package com.bpdlh.pencatatankeuangan.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.bpdlh.pencatatankeuangan.AppContainer
import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.ui.asString
import com.bpdlh.pencatatankeuangan.ui.components.Hairline
import com.bpdlh.pencatatankeuangan.ui.components.KeyValueLine
import com.bpdlh.pencatatankeuangan.ui.components.LoadingBlock
import com.bpdlh.pencatatankeuangan.ui.components.MessageBlock
import com.bpdlh.pencatatankeuangan.ui.components.SecondaryAction
import com.bpdlh.pencatatankeuangan.ui.formatCoordinate
import com.bpdlh.pencatatankeuangan.ui.formatDateTime
import com.bpdlh.pencatatankeuangan.ui.formatNumber
import com.bpdlh.pencatatankeuangan.ui.theme.AmountTextStyle
import java.io.File

@Composable
fun DetailRoot(
    container: AppContainer,
    recordId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: DetailViewModel = viewModel(key = recordId) { DetailViewModel(container.records, recordId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    DetailScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBack,
        onEditClick = onEdit,
    )
}

@Composable
fun DetailScreen(
    state: DetailState,
    onAction: (DetailAction) -> Unit,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    val context = LocalContext.current
    var mapError by remember { mutableStateOf<String?>(null) }
    val noMapApp = stringResource(R.string.detail_no_map_app)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.detail != null) {
                TextButton(onClick = onEditClick, modifier = Modifier.height(48.dp)) {
                    Text(stringResource(R.string.action_edit))
                }
                TextButton(onClick = { onAction(DetailAction.OnDeleteClick) }, modifier = Modifier.height(48.dp)) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }

        when {
            state.isLoading -> LoadingBlock(stringResource(R.string.detail_loading))
            state.detail == null -> MessageBlock(
                title = stringResource(R.string.detail_error_title),
                body = state.error?.asString().orEmpty(),
                actionLabel = stringResource(R.string.cd_back),
                onAction = onBackClick,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            else -> {
                val detail = state.detail
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = detail.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.currency_amount, formatNumber(detail.price)),
                        style = AmountTextStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    val photoPath = state.photoPath
                    if (photoPath != null) {
                        Spacer(Modifier.height(16.dp))
                        AsyncImage(
                            model = File(photoPath),
                            contentDescription = stringResource(R.string.detail_cd_photo, detail.name),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    } else if (state.photoFailed) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.detail_photo_failed),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SecondaryAction(
                            stringResource(R.string.detail_photo_reload),
                            onClick = { onAction(DetailAction.OnRetryClick) },
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Hairline()

                    KeyValueLine(stringResource(R.string.detail_label_created), formatDateTime(detail.createdAt))
                    KeyValueLine(stringResource(R.string.detail_label_updated), formatDateTime(detail.updatedAt))
                    KeyValueLine(stringResource(R.string.detail_label_status), stringResource(detail.statusRes))

                    val latitude = detail.latitude
                    val longitude = detail.longitude
                    if (latitude != null && longitude != null) {
                        KeyValueLine(
                            stringResource(R.string.detail_label_location),
                            formatCoordinate(latitude, longitude),
                        )
                        detail.accuracyM?.let {
                            KeyValueLine(
                                stringResource(R.string.detail_label_accuracy),
                                stringResource(R.string.accuracy_meters, Math.round(it).toInt()),
                            )
                        }
                        TextButton(
                            onClick = {
                                val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                                mapError = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                    .exceptionOrNull()
                                    ?.let { noMapApp }
                            },
                        ) { Text(stringResource(R.string.detail_open_map)) }
                        mapError?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        KeyValueLine(
                            stringResource(R.string.detail_label_location),
                            stringResource(R.string.detail_location_missing),
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.detail_device_line,
                            detail.deviceModel ?: stringResource(R.string.detail_device_unknown),
                            detail.osVersion ?: stringResource(R.string.detail_device_unknown),
                            detail.appVersion ?: stringResource(R.string.detail_device_unknown),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { onAction(DetailAction.OnDeleteDismiss) },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = { Text(stringResource(R.string.detail_delete_body)) },
            confirmButton = {
                TextButton(onClick = { onAction(DetailAction.OnDeleteConfirm) }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(DetailAction.OnDeleteDismiss) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
