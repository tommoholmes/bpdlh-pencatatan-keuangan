package com.bpdlh.pencatatankeuangan.ui.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.ui.UiText
import androidx.lifecycle.viewModelScope
import com.bpdlh.pencatatankeuangan.data.LocationProvider
import com.bpdlh.pencatatankeuangan.data.PhotoLibrary
import com.bpdlh.pencatatankeuangan.data.PlaceSnapshot
import com.bpdlh.pencatatankeuangan.data.RecordEditor
import com.bpdlh.pencatatankeuangan.data.SettingsStore
import com.bpdlh.pencatatankeuangan.ui.parsePrice
import com.bpdlh.pencatatankeuangan.ui.priceDigits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val nameError: UiText? = null,
    val priceError: UiText? = null,
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
    private val records: RecordEditor,
    private val photos: PhotoLibrary,
    private val location: LocationProvider,
    private val prefs: SettingsStore,
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
            val existing = recordId?.let { records.find(it) }
            if (existing == null) {
                _state.update { it.copy(isLoading = false, existing = false) }
                when {
                    !prefs.saveLocationEnabled ->
                        _state.update { it.copy(locationState = LocationState.Disabled) }
                    location.hasPermission() -> onAction(EditorAction.OnRequestLocation)
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
                photos.delete(_state.value.photoPath)
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
            runCatching { photos.import(uri) }.onSuccess { path ->
                val old = _state.value.photoPath
                if (old != null && old != path) photos.delete(old)
                _state.update { it.copy(photoPath = path) }
            }
        }
    }

    private fun requestLocation() {
        val current = _state.value
        if (current.existing || current.locationState == LocationState.Requesting) return
        _state.update { it.copy(locationState = LocationState.Requesting) }
        viewModelScope.launch {
            val place = location.requestOnce()
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
        val nameError = if (name.isEmpty()) UiText.Res(R.string.editor_name_required) else null
        val priceError = if (price == null) UiText.Res(R.string.editor_price_required) else null
        if (nameError != null || priceError != null) {
            _state.update { it.copy(nameError = nameError, priceError = priceError) }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            records.save(
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
