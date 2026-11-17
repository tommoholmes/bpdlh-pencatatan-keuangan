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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.bpdlh.pencatatankeuangan.AppContainer
import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.ui.components.FieldHint
import com.bpdlh.pencatatankeuangan.ui.components.LoadingBlock
import com.bpdlh.pencatatankeuangan.ui.components.PrimaryAction
import com.bpdlh.pencatatankeuangan.ui.components.SectionLabel
import com.bpdlh.pencatatankeuangan.ui.asString
import com.bpdlh.pencatatankeuangan.ui.components.StatusText
import com.bpdlh.pencatatankeuangan.ui.formatCoordinate
import com.bpdlh.pencatatankeuangan.ui.formatNumber
import com.bpdlh.pencatatankeuangan.ui.parsePrice
import com.bpdlh.pencatatankeuangan.ui.priceDigits
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun EditorRoot(
    container: AppContainer,
    recordId: String?,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: EditorViewModel = viewModel(key = recordId ?: "new") {
        EditorViewModel(
            records = container.records,
            photos = container.photos,
            location = container.location,
            prefs = container.prefs,
            recordId = recordId,
            savedStateHandle = createSavedStateHandle(),
        )
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
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(if (state.existing) R.string.editor_title_edit else R.string.editor_title_new),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (state.isLoading) {
            LoadingBlock(stringResource(R.string.editor_loading))
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
                label = { Text(stringResource(R.string.editor_name_label)) },
                placeholder = { Text(stringResource(R.string.editor_name_placeholder)) },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = { state.nameError?.let { Text(it.asString()) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.priceDigits,
                onValueChange = { onAction(EditorAction.OnPriceChange(it)) },
                label = { Text(stringResource(R.string.editor_price_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.priceError != null,
                supportingText = {
                    Text(
                        text = state.priceError?.asString()
                            ?: state.priceDigits.takeIf { it.isNotEmpty() }?.let {
                                stringResource(R.string.currency_amount, formatNumber(parsePrice(it) ?: 0))
                            }
                            ?: stringResource(R.string.editor_price_hint),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.editor_section_photo))
            Spacer(Modifier.height(4.dp))
            val photoPath = state.photoPath
            if (photoPath != null) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = File(photoPath),
                    contentDescription = stringResource(R.string.editor_cd_photo),
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
                    }) { Text(stringResource(R.string.editor_photo_change)) }
                    TextButton(onClick = { onAction(EditorAction.OnRemovePhoto) }) {
                        Text(stringResource(R.string.editor_photo_remove))
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val uri = newCameraUri(context)
                        cameraUri = uri
                        camera.launch(uri)
                    }) { Text(stringResource(R.string.editor_photo_take)) }
                    TextButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(stringResource(R.string.editor_photo_pick)) }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel(stringResource(R.string.editor_section_location))
            Spacer(Modifier.height(4.dp))
            LocationBlock(
                state = state,
                onRequestPermission = { showLocationRationale = true },
                onOpenSettings = onOpenSettingsClick,
            )

            Spacer(Modifier.height(16.dp))
            if (state.existing) {
                FieldHint(stringResource(R.string.editor_hint_existing))
            }
            Spacer(Modifier.height(16.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            PrimaryAction(
                text = stringResource(R.string.action_save),
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
            title = { Text(stringResource(R.string.editor_location_rationale_title)) },
            text = {
                Text(stringResource(R.string.editor_location_rationale_body))
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
                }) { Text(stringResource(R.string.action_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showLocationRationale = false }) { Text(stringResource(R.string.action_later)) }
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
            text = place.accuracyM?.let { accuracy ->
                stringResource(
                    R.string.editor_location_coordinate,
                    formatCoordinate(place.latitude ?: 0.0, place.longitude ?: 0.0),
                    stringResource(R.string.accuracy_meters, Math.round(accuracy).toInt()),
                )
            } ?: formatCoordinate(place.latitude ?: 0.0, place.longitude ?: 0.0),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LocationState.Requesting -> StatusText(stringResource(R.string.editor_location_requesting), muted)
        LocationState.Denied -> LocationLine(
            text = stringResource(R.string.editor_location_denied),
            actionLabel = stringResource(R.string.action_retry),
            onAction = onRequestPermission,
        )
        LocationState.Unavailable -> StatusText(stringResource(R.string.editor_location_unavailable), muted)
        LocationState.Disabled -> LocationLine(
            text = stringResource(R.string.editor_location_disabled),
            actionLabel = stringResource(R.string.action_settings),
            onAction = onOpenSettings,
        )
        LocationState.Idle -> LocationLine(
            text = stringResource(R.string.editor_location_idle),
            actionLabel = stringResource(R.string.action_allow),
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
