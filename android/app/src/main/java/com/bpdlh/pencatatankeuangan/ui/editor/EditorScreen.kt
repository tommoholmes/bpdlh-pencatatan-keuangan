package com.bpdlh.pencatatankeuangan.ui.editor

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.bpdlh.pencatatankeuangan.AppContainer
import com.bpdlh.pencatatankeuangan.data.PlaceSnapshot
import com.bpdlh.pencatatankeuangan.ui.components.FieldHint
import com.bpdlh.pencatatankeuangan.ui.components.LoadingBlock
import com.bpdlh.pencatatankeuangan.ui.components.PrimaryAction
import com.bpdlh.pencatatankeuangan.ui.components.SectionLabel
import com.bpdlh.pencatatankeuangan.ui.components.StatusText
import com.bpdlh.pencatatankeuangan.ui.formatAccuracy
import com.bpdlh.pencatatankeuangan.ui.formatCoordinate
import com.bpdlh.pencatatankeuangan.ui.formatRupiah
import com.bpdlh.pencatatankeuangan.ui.parsePrice
import com.bpdlh.pencatatankeuangan.ui.priceDigits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class LocationState { Idle, Requesting, Filled, Unavailable, Denied, Disabled }

data class EditorState(
    val isLoading: Boolean = true,
    val existing: Boolean = false,
    val name: String = "",
    val priceDigits: String = "",
    val photoPath: String? = null,
    val place: PlaceSnapshot = PlaceSnapshot(null, null, null),
    val locationState: LocationState = LocationState.Idle,
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val priceError: String? = null,
    val saved: Boolean = false,
)

sealed interface EditorAction {
    data class OnNameChange(val value: String) : EditorAction
    data class OnPriceChange(val value: String) : EditorAction
    data class OnPhotoPicked(val uri: Uri) : EditorAction
    data object OnRemovePhoto : EditorAction
    data object OnRequestLocation : EditorAction
    data object OnLocationDenied : EditorAction
    data object OnSaveClick : EditorAction
}

class EditorViewModel(
    private val container: AppContainer,
    private val recordId: String?,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                name = savedStateHandle[KEY_NAME] ?: "",
                priceDigits = savedStateHandle[KEY_PRICE] ?: "",
            )
        }
        viewModelScope.launch {
            val existing = recordId?.let { container.records.find(it) }
            if (existing == null) {
                _state.update { it.copy(isLoading = false, existing = false) }
                when {
                    !container.prefs.saveLocationEnabled ->
                        _state.update { it.copy(locationState = LocationState.Disabled) }
                    container.location.hasPermission() -> onAction(EditorAction.OnRequestLocation)
                }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        existing = true,
                        name = existing.name,
                        priceDigits = existing.price.toString(),
                        photoPath = existing.photoPath,
                        place = PlaceSnapshot(existing.latitude, existing.longitude, existing.accuracyM),
                        locationState = if (existing.latitude != null) LocationState.Filled else LocationState.Unavailable,
                    )
                }
            }
        }
    }

    fun onAction(action: EditorAction) {
        when (action) {
            is EditorAction.OnNameChange -> {
                val value = action.value.take(200)
                savedStateHandle[KEY_NAME] = value
                _state.update { it.copy(name = value, nameError = null) }
            }
            is EditorAction.OnPriceChange -> {
                val value = priceDigits(action.value)
                savedStateHandle[KEY_PRICE] = value
                _state.update { it.copy(priceDigits = value, priceError = null) }
            }
            is EditorAction.OnPhotoPicked -> importPhoto(action.uri)
            EditorAction.OnRemovePhoto -> {
                container.photos.delete(_state.value.photoPath)
                _state.update { it.copy(photoPath = null) }
            }
            EditorAction.OnRequestLocation -> requestLocation()
            EditorAction.OnLocationDenied ->
                _state.update { it.copy(locationState = LocationState.Denied) }
            EditorAction.OnSaveClick -> save()
        }
    }

    private fun importPhoto(uri: Uri) {
        viewModelScope.launch {
            runCatching { container.photos.import(uri) }.onSuccess { path ->
                val old = _state.value.photoPath
                if (old != null && old != path) container.photos.delete(old)
                _state.update { it.copy(photoPath = path) }
            }
        }
    }

    private fun requestLocation() {
        val current = _state.value
        if (current.existing || current.locationState == LocationState.Requesting) return
        _state.update { it.copy(locationState = LocationState.Requesting) }
        viewModelScope.launch {
            val place = container.location.requestOnce()
            _state.update {
                if (place.latitude != null) {
                    it.copy(place = place, locationState = LocationState.Filled)
                } else {
                    it.copy(place = PlaceSnapshot(null, null, null), locationState = LocationState.Unavailable)
                }
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving) return
        val name = current.name.trim()
        val price = parsePrice(current.priceDigits)
        val nameError = if (name.isEmpty()) "Nama belum diisi." else null
        val priceError = if (price == null) "Harga belum diisi." else null
        if (nameError != null || priceError != null) {
            _state.update { it.copy(nameError = nameError, priceError = priceError) }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            container.records.save(
                existingId = recordId,
                name = name,
                price = price ?: 0,
                photoPath = current.photoPath,
                place = current.place,
            )
            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_PRICE = "price"
    }
}

@Composable
fun EditorRoot(
    container: AppContainer,
    recordId: String?,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: EditorViewModel = viewModel(key = recordId ?: "new") {
        EditorViewModel(container, recordId, createSavedStateHandle())
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    EditorScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBack,
        onOpenSettingsClick = onOpenSettings,
    )
}

@Composable
fun EditorScreen(
    state: EditorState,
    onAction: (EditorAction) -> Unit,
    onBackClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
) {
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var showLocationRationale by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onAction(EditorAction.OnPhotoPicked(uri))
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) onAction(EditorAction.OnPhotoPicked(uri))
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            onAction(EditorAction.OnRequestLocation)
        } else {
            onAction(EditorAction.OnLocationDenied)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Kembali",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (state.existing) "Ubah catatan" else "Catat baru",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (state.isLoading) {
            LoadingBlock("Membuka catatan")
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = { onAction(EditorAction.OnNameChange(it)) },
                label = { Text("Nama") },
                placeholder = { Text("Kopi susu") },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = { state.nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.priceDigits,
                onValueChange = { onAction(EditorAction.OnPriceChange(it)) },
                label = { Text("Harga (Rp)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.priceError != null,
                supportingText = {
                    Text(
                        text = state.priceError
                            ?: state.priceDigits.takeIf { it.isNotEmpty() }?.let { formatRupiah(parsePrice(it) ?: 0) }
                            ?: "Tanpa titik atau koma.",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Foto")
            Spacer(Modifier.height(4.dp))
            val photoPath = state.photoPath
            if (photoPath != null) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = File(photoPath),
                    contentDescription = "Foto catatan",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val uri = newCameraUri(context)
                        cameraUri = uri
                        camera.launch(uri)
                    }) { Text("Ganti foto") }
                    TextButton(onClick = { onAction(EditorAction.OnRemovePhoto) }) { Text("Hapus foto") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val uri = newCameraUri(context)
                        cameraUri = uri
                        camera.launch(uri)
                    }) { Text("Ambil foto") }
                    TextButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Pilih dari galeri") }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Lokasi")
            Spacer(Modifier.height(4.dp))
            LocationBlock(
                state = state,
                onRequestPermission = { showLocationRationale = true },
                onOpenSettings = onOpenSettingsClick,
            )

            Spacer(Modifier.height(16.dp))
            if (state.existing) {
                FieldHint("Lokasi dan info perangkat tidak berubah saat catatan diubah.")
            }
            Spacer(Modifier.height(16.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            PrimaryAction(
                text = "Simpan",
                onClick = { onAction(EditorAction.OnSaveClick) },
                loading = state.isSaving,
                enabled = !state.isSaving,
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showLocationRationale) {
        AlertDialog(
            onDismissRequest = { showLocationRationale = false },
            title = { Text("Izin lokasi") },
            text = {
                Text(
                    "Lokasi presisi diambil sekali saat catatan disimpan, supaya catatan tahu di mana ia dibuat. " +
                        "Tanpa izin, catatan tetap tersimpan tanpa lokasi.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLocationRationale = false
                    permission.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }) { Text("Lanjutkan") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationRationale = false }) { Text("Nanti") }
            },
        )
    }
}

@Composable
private fun LocationBlock(
    state: EditorState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val place = state.place
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when (state.locationState) {
        LocationState.Filled -> Text(
            text = formatCoordinate(place.latitude ?: 0.0, place.longitude ?: 0.0) +
                (place.accuracyM?.let { " ±${formatAccuracy(it)}" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LocationState.Requesting -> StatusText("Mengambil lokasi…", muted)
        LocationState.Denied -> LocationLine(
            text = "Izin ditolak. Catatan tersimpan tanpa lokasi.",
            actionLabel = "Coba lagi",
            onAction = onRequestPermission,
        )
        LocationState.Unavailable -> StatusText("Lokasi tidak tersedia. Catatan tetap tersimpan.", muted)
        LocationState.Disabled -> LocationLine(
            text = "Lokasi otomatis dimatikan.",
            actionLabel = "Pengaturan",
            onAction = onOpenSettings,
        )
        LocationState.Idle -> LocationLine(
            text = "Belum diambil.",
            actionLabel = "Izinkan",
            onAction = onRequestPermission,
        )
    }
}

@Composable
private fun LocationLine(text: String, actionLabel: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusText(text, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

private fun newCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "foto-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}
