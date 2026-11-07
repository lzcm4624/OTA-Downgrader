package com.chumor.downgrader.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chumor.downgrader.BuildConfig
import com.chumor.downgrader.domain.model.DeviceInfo
import com.chumor.downgrader.domain.model.DeviceReadError
import com.chumor.downgrader.domain.model.FirmwareDownloadStatus
import com.chumor.downgrader.domain.model.FirmwarePackage
import com.chumor.downgrader.domain.model.QueryError
import com.chumor.downgrader.domain.model.downloadFileName
import com.chumor.downgrader.domain.model.isSha256Digest
import com.chumor.downgrader.presentation.DowngraderUiState
import com.chumor.downgrader.presentation.FirmwareChannel
import com.chumor.downgrader.presentation.ui.glass.DataRow
import com.chumor.downgrader.presentation.ui.glass.Glass
import com.chumor.downgrader.presentation.ui.glass.GlassAboutDialog
import com.chumor.downgrader.presentation.ui.glass.GlassBackdrop
import com.chumor.downgrader.presentation.ui.glass.GlassButton
import com.chumor.downgrader.presentation.ui.glass.GlassCard
import com.chumor.downgrader.presentation.ui.glass.GlassErrorNote
import com.chumor.downgrader.presentation.ui.glass.GlassProgress
import com.chumor.downgrader.presentation.ui.glass.GlassTheme
import com.chumor.downgrader.presentation.ui.glass.IndeterminateBar
import com.chumor.downgrader.presentation.ui.glass.SectionLabel
import com.chumor.downgrader.presentation.ui.glass.SkeletonLines
import com.chumor.downgrader.presentation.ui.glass.SmallGlassButton
import com.chumor.downgrader.presentation.ui.glass.Spinner
import com.chumor.downgrader.presentation.ui.glass.Text
import com.chumor.downgrader.presentation.ui.glass.StatusPill
import com.chumor.downgrader.presentation.ui.glass.revealItem
import java.util.Locale

private val ContentMaxWidth = 760.dp

@Composable
fun DowngraderScreen(
    state: DowngraderUiState,
    onSelectChannel: (FirmwareChannel) -> Unit,
    onOplusModelChange: (String) -> Unit,
    onQueryOplus: () -> Unit,
    onCopyOplusUrl: (String) -> Unit,
    onOpenOplusUrl: (String) -> Unit,
    onQueryPackage: () -> Unit,
    onSaveCertificate: () -> Unit,
    onDownloadFirmware: () -> Unit,
    onCancelFirmwareDownload: () -> Unit,
    onCopyFirmwareUrl: (String) -> Unit,
    onCopyCertificate: (String) -> Unit,
    onOpenSystemUpdater: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var guideExpanded by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val busy = state.isLoadingDevice || state.isQuerying ||
        state.isDownloadingFirmware || state.isQueryingOplus
    val collapseTarget by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 240f).coerceIn(0f, 1f)
            }
        }
    }
    val collapse by animateFloatAsState(
        targetValue = collapseTarget,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "headerCollapse",
    )

    GlassTheme {
        GlassBackdrop(modifier = modifier) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val sidePadding = if (maxWidth > ContentMaxWidth) {
                    (maxWidth - ContentMaxWidth) / 2
                } else {
                    0.dp
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    contentPadding = PaddingValues(
                        start = 18.dp + sidePadding,
                        end = 18.dp + sidePadding,
                        top = 12.dp,
                        bottom = 40.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    revealItem(key = "header", order = 0) {
                        Header(
                            busy = busy,
                            channel = state.channel,
                            onAbout = { showAbout = true },
                        )
                    }

                    if (state.channel == FirmwareChannel.OPLUS) {
                        oplusContent(
                            state = state,
                            onModelChange = onOplusModelChange,
                            onQuery = onQueryOplus,
                            onCopyUrl = onCopyOplusUrl,
                            onOpenUrl = onOpenOplusUrl,
                        )
                    } else {

                    revealItem(key = "device.label", order = 1) {
                        SectionLabel(title = "设备信息", trailing = "本机")
                    }

                    revealItem(key = "device.card", order = 1) {
                        DeviceCard(state = state)
                    }

                    revealItem(key = "credential.label", order = 2) {
                        SectionLabel(
                            title = "降级凭据",
                            trailing = if (state.credentialAvailable) "已就绪" else "缺失",
                            accent = Color.Unspecified,
                        )
                    }

                    revealItem(key = "credential.card", order = 2) {
                        CredentialCard(state = state)
                    }

                    revealItem(key = "query.action", order = 3) {
                        GlassButton(
                            title = if (state.isQuerying) "正在查询降级包" else "查询可用降级包",
                            subtitle = "vivo 官方升级服务",
                            enabled = state.canQueryPackage,
                            loading = state.isQuerying,
                            onClick = onQueryPackage,
                        )
                    }

                    state.queryError?.let { error ->
                        revealItem(key = "query.error", order = 3) {
                            GlassErrorNote(
                                title = "查询失败",
                                message = queryErrorMessage(error),
                            )
                        }
                    }

                    state.firmware?.let { firmware ->
                        revealItem(key = "firmware.label", order = 4) {
                            SectionLabel(title = "可用降级包", trailing = "已获取")
                        }
                        revealItem(key = "firmware.card", order = 4) {
                            FirmwareCard(
                                state = state,
                                firmware = firmware,
                                onCopyFirmwareUrl = onCopyFirmwareUrl,
                                onCopyCertificate = onCopyCertificate,
                                onSaveCertificate = onSaveCertificate,
                                onDownloadFirmware = onDownloadFirmware,
                                onCancelFirmwareDownload = onCancelFirmwareDownload,
                            )
                        }
                    }

                    if (state.isDownloadingFirmware && state.firmware == null) {
                        revealItem(key = "download.standalone", order = 5) {
                            GlassCard {
                                DownloadProgressBlock(
                                    state = state,
                                    onCancel = onCancelFirmwareDownload,
                                )
                            }
                        }
                    }

                    if (state.firmware != null) {
                        revealItem(key = "guide.card", order = 5) {
                            GuideCard(
                                expanded = guideExpanded,
                                onToggle = { guideExpanded = !guideExpanded },
                                onOpenSystemUpdater = onOpenSystemUpdater,
                            )
                        }
                    }

                    }

                    revealItem(key = "channel.label", order = 6) {
                        SectionLabel(title = "查询通道", trailing = state.channel.label)
                    }

                    revealItem(key = "channel.switcher", order = 7) {
                        ChannelBlock(
                            current = state.channel,
                            onSelect = onSelectChannel,
                        )
                    }

                    revealItem(key = "footer", order = 8) {
                        Footer(channel = state.channel)
                    }
                }

                MiniTopBar(
                    alpha = collapse,
                    busy = busy,
                    channel = state.channel,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }

        GlassAboutDialog(show = showAbout, onDismissRequest = { showAbout = false })
    }
}

@Composable
private fun MiniTopBar(
    alpha: Float,
    busy: Boolean,
    channel: FirmwareChannel,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0.02f) return
    val p = Glass.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .background(
                Brush.verticalGradient(
                    listOf(
                        p.glassTop.copy(alpha = 0.94f),
                        p.glassBottom.copy(alpha = 0.82f),
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 22.dp)
            .padding(top = 10.dp, bottom = if (busy) 0.dp else 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("降级助手", style = Glass.headline(15f, p.text, FontWeight.Bold))
            Spacer(Modifier.width(9.dp))
            Text(channel.label, style = Glass.label(11.5f, p.accent, FontWeight.SemiBold))
            Spacer(Modifier.weight(1f))
            StatusPill(text = if (busy) "处理中" else "就绪", active = !busy)
        }
        if (busy) {
            Spacer(Modifier.height(9.dp))
            IndeterminateBar(
                height = 2.dp,
                modifier = Modifier.padding(bottom = 9.dp),
            )
        }
    }
}

@Composable
private fun ChannelBlock(
    current: FirmwareChannel,
    onSelect: (FirmwareChannel) -> Unit,
) {
    val p = Glass.palette
    GlassCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)) {
        ChannelSwitcher(current = current, onSelect = onSelect)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(p.accent, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(channelHint(current), style = Glass.label(12f, p.textDim))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "左右滑动胶囊也能切换",
            style = Glass.label(11.5f, p.textMute),
        )
    }
}

private fun channelHint(channel: FirmwareChannel): String = when (channel) {
    FirmwareChannel.VIVO -> "vivo / iQOO 官方升级服务，需要本机降级凭据"
    FirmwareChannel.OPLUS -> "OPPO · 一加 · 真我 官方 OTA 版本链 + 售后降级服务器"
}

@Composable
private fun Header(busy: Boolean, channel: FirmwareChannel, onAbout: () -> Unit) {
    val p = Glass.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 2.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "降级助手",
                style = Glass.title(30f, p.text, FontWeight.Bold),
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${channel.label} 通道 · 查询 · 下载 · 校验",
                style = Glass.label(12.5f, p.textMute, FontWeight.Medium),
                maxLines = 1,
            )
        }
        StatusPill(text = if (busy) "处理中" else "就绪", active = !busy)
        Spacer(Modifier.width(8.dp))
        AboutButton(onClick = onAbout)
    }
}

@Composable
private fun AboutButton(onClick: () -> Unit) {
    val p = Glass.palette
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(p.glassTint, CircleShape)
            .border(1.dp, p.edgeTop.copy(alpha = 0.42f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
            val stroke = 1.7.dp.toPx()
            drawCircle(
                color = p.text.copy(alpha = 0.82f),
                radius = size.minDimension / 2f - stroke / 2f,
                style = Stroke(width = stroke),
            )
            drawCircle(
                color = p.text.copy(alpha = 0.9f),
                radius = stroke * 0.62f,
                center = Offset(size.width / 2f, size.height * 0.30f),
            )
            drawLine(
                color = p.text.copy(alpha = 0.9f),
                start = Offset(size.width / 2f, size.height * 0.46f),
                end = Offset(size.width / 2f, size.height * 0.72f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun DeviceCard(state: DowngraderUiState) {
    val p = Glass.palette
    GlassCard {
        when {
            state.device != null -> DeviceRows(state.device)
            state.isLoadingDevice -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spinner()
                    Spacer(Modifier.width(10.dp))
                    Text("正在读取系统属性…", style = Glass.body(14f, p.textDim))
                }
                Spacer(Modifier.height(14.dp))
                SkeletonLines(count = 4)
            }
            state.deviceError != null -> {
                Text(
                    text = deviceErrorMessage(state.deviceError!!),
                    style = Glass.body(13.5f, p.red),
                )
            }
            else -> {
                Text("暂无设备数据", style = Glass.body(13.5f, p.textMute))
            }
        }
    }
}

@Composable
private fun DeviceRows(device: DeviceInfo) {
    val p = Glass.palette
    Text(
        text = device.model.asDisplayValue(),
        style = Glass.title(23f, p.text, FontWeight.Bold),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(3.dp))
    Text(
        text = device.productName.asDisplayValue(),
        style = Glass.label(12.5f, p.textMute),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(14.dp))
    DataRow(
        label = "系统版本",
        value = device.softwareVersion.asDisplayValue(),
        valueStyle = Glass.body(14f, p.accent, FontWeight.SemiBold),
    )
    DataRow(label = "Android 版本", value = device.androidVersion.asDisplayValue())
    DataRow(label = "序列号", value = device.serial.asMaskedSerial(), showDivider = false)
}

@Composable
private fun CredentialCard(state: DowngraderUiState) {
    val p = Glass.palette
    val ready = state.credentialAvailable
    val accent = if (ready) p.green else p.amber
    GlassCard(tint = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(
                text = if (ready) "凭据已就绪" else "未检测到凭据",
                active = ready,
                activeColor = accent,
                inactiveColor = accent,
            )
            Spacer(Modifier.weight(1f))
            Text("dota_sig", style = Glass.mono(11.5f, p.textMute))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (ready) {
                "本机已开放降级凭据，可以直接向 vivo 升级服务发起查询。"
            } else {
                "本机没有可用的降级凭据，服务端不会返回降级包。换一个系统版本或机型再试。"
            },
            style = Glass.body(13.5f, if (ready) p.textDim else p.textDim),
        )
    }
}

@Composable
private fun FirmwareCard(
    state: DowngraderUiState,
    firmware: FirmwarePackage,
    onCopyFirmwareUrl: (String) -> Unit,
    onCopyCertificate: (String) -> Unit,
    onSaveCertificate: () -> Unit,
    onDownloadFirmware: () -> Unit,
    onCancelFirmwareDownload: () -> Unit,
) {
    val p = Glass.palette
    GlassCard {
        Text("目标系统版本", style = Glass.label(12f, p.textMute, FontWeight.Medium))
        Spacer(Modifier.height(5.dp))
        Text(
            text = firmware.systemVersion.asDisplayValue(),
            style = Glass.title(26f, p.text, FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(text = "已获取固件包", active = true, activeColor = p.accent)
            Spacer(Modifier.weight(1f))
            Text(
                text = formatFileSize(firmware.fileSizeBytes),
                style = Glass.label(13f, p.textDim, FontWeight.SemiBold),
            )
        }
        Spacer(Modifier.height(12.dp))

        DataRow(label = "Android 版本", value = firmware.androidVersion.asDisplayValue())
        DataRow(label = "ROM 版本", value = firmware.romVersion.asDisplayValue())
        DataRow(label = "发布时间", value = firmware.releaseTime.asDisplayValue())
        DataRow(
            label = "SHA-256",
            value = firmware.sha256.asDisplayValue(),
            valueStyle = Glass.mono(11.5f, p.textDim),
        )
        DataRow(
            label = "文件名",
            value = firmware.downloadFileName(state.device?.model ?: "device"),
            valueStyle = Glass.mono(11.5f, p.textDim),
        )
        DataRow(
            label = "下载地址",
            value = firmware.downloadUrl.asDisplayValue(),
            valueStyle = Glass.mono(11.5f, p.textDim),
            showDivider = false,
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallGlassButton(
                title = "复制地址",
                modifier = Modifier.weight(1f),
                enabled = firmware.downloadUrl.isNotBlank(),
                onClick = { onCopyFirmwareUrl(firmware.downloadUrl) },
            )
            SmallGlassButton(
                title = "复制凭据",
                modifier = Modifier.weight(1f),
                enabled = firmware.certificate.isNotBlank(),
                onClick = { onCopyCertificate(firmware.certificate) },
            )
            SmallGlassButton(
                title = if (state.isCertificateSaved) "已保存" else "保存凭据",
                modifier = Modifier.weight(1f),
                enabled = state.canSaveCertificate,
                tint = if (state.isCertificateSaved) p.green else Color.Unspecified,
                onClick = onSaveCertificate,
            )
        }

        if (state.firmwareDownload.status != FirmwareDownloadStatus.Idle) {
            Spacer(Modifier.height(16.dp))
            DownloadProgressBlock(state = state, onCancel = onCancelFirmwareDownload)
        }

        Spacer(Modifier.height(16.dp))

        if (state.isDownloadingFirmware) {
            GlassButton(
                title = "取消下载",
                enabled = true,
                prominent = false,
                onClick = onCancelFirmwareDownload,
            )
        } else {
            GlassButton(
                title = if (state.firmwareDownload.status == FirmwareDownloadStatus.Completed) {
                    "重新下载固件"
                } else {
                    "下载固件包"
                },
                subtitle = "自动校验大小与 SHA-256",
                enabled = state.canDownloadFirmware,
                onClick = onDownloadFirmware,
            )
            if (!state.canDownloadFirmware && state.firmwareDownload.status != FirmwareDownloadStatus.Completed) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (!firmware.sha256.isSha256Digest()) {
                        "缺少有效的文件大小或 SHA-256，无法校验下载完整性"
                    } else {
                        "当前状态不可下载，请先完成设备读取"
                    },
                    style = Glass.label(12f, p.textMute),
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressBlock(
    state: DowngraderUiState,
    onCancel: () -> Unit,
) {
    val p = Glass.palette
    val download = state.firmwareDownload
    val total = download.totalBytes.takeIf { it > 0 } ?: state.firmware?.fileSizeBytes ?: 0L
    val progress = when {
        total > 0L -> (download.downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        download.status == FirmwareDownloadStatus.Completed -> 1f
        else -> 0f
    }
    val statusColor = when (download.status) {
        FirmwareDownloadStatus.Completed -> p.green
        FirmwareDownloadStatus.Failed -> p.red
        FirmwareDownloadStatus.Cancelled -> p.amber
        else -> p.accent
    }
    val shownProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(420),
        label = "countUp",
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = downloadStatusText(download.status),
                style = Glass.body(13.5f, statusColor, FontWeight.SemiBold),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = String.format(Locale.US, "%d%%", (shownProgress * 100).toInt()),
                style = Glass.headline(15f, statusColor, FontWeight.Bold),
            )
        }
        Spacer(Modifier.height(9.dp))
        GlassProgress(progress = progress, tint = statusColor)
        Spacer(Modifier.height(8.dp))
        Row {
            Text(
                text = "${formatFileSize(download.downloadedBytes)} / ${formatFileSize(total)}",
                style = Glass.label(11.5f, p.textMute),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "SHA-256 校验",
                style = Glass.label(11.5f, p.textMute),
            )
        }
    }
}

@Composable
private fun GuideCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenSystemUpdater: (() -> Unit)?,
) {
    val p = Glass.palette
    val interaction = remember { MutableInteractionSource() }
    GlassCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = interaction, indication = null, onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("降级包使用指南", style = Glass.headline(14.5f, p.text, FontWeight.SemiBold))
            Spacer(Modifier.weight(1f))
            Text(
                text = if (expanded) "收起" else "展开",
                style = Glass.label(12.5f, p.accent, FontWeight.SemiBold),
            )
            Spacer(Modifier.width(4.dp))
            Chevron(expanded = expanded, color = p.accent)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(240)) +
                expandVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top,
                ),
            exit = fadeOut(animationSpec = tween(160)) +
                shrinkVertically(
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top,
                ),
        ) {
            Column {
                Spacer(Modifier.height(14.dp))
                GuideStep(index = "1", text = "点击下载固件包，同时保存 dota_sig.txt，按提示授予存储权限。")
                GuideStep(index = "2", text = "两个文件都会直接落到内部存储根目录，不需要解压。")
                GuideStep(index = "3", text = "进入系统更新 → 菜单 → 升级设置 → 本地升级，选中刚下载的包开始降级。")
                if (onOpenSystemUpdater != null) {
                    Spacer(Modifier.height(14.dp))
                    GlassButton(
                        title = "打开系统更新",
                        prominent = false,
                        onClick = onOpenSystemUpdater,
                    )
                }
            }
        }
    }
}

@Composable
private fun Chevron(expanded: Boolean, color: Color) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "chevronRotation",
    )
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .size(10.dp)
            .graphicsLayer { rotationZ = rotation },
    ) {
        val path = Path().apply {
            moveTo(0f, size.height * 0.34f)
            lineTo(size.width / 2f, size.height * 0.70f)
            lineTo(size.width, size.height * 0.34f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun GuideStep(index: String, text: String) {
    val p = Glass.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(p.accent.copy(alpha = 0.16f), CircleShape)
                .border(1.dp, p.accent.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(index, style = Glass.label(11f, p.accent, FontWeight.Bold))
        }
        Spacer(Modifier.width(11.dp))
        Text(
            text = text,
            style = Glass.body(13f, p.textDim),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun Footer(channel: FirmwareChannel) {
    val p = Glass.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · GPL-3.0",
            style = Glass.label(11.5f, p.textMute),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = when (channel) {
                FirmwareChannel.VIVO -> "数据来自 vivo 官方升级服务"
                FirmwareChannel.OPLUS -> "数据来自 OPPO · 一加 · 真我 官方服务"
            },
            style = Glass.label(11f, p.textMute.copy(alpha = 0.75f)),
        )
    }
}

private fun downloadStatusText(status: FirmwareDownloadStatus): String = when (status) {
    FirmwareDownloadStatus.Idle -> "待命"
    FirmwareDownloadStatus.Preparing -> "准备下载"
    FirmwareDownloadStatus.Downloading -> "正在下载"
    FirmwareDownloadStatus.Verifying -> "校验完整性"
    FirmwareDownloadStatus.Completed -> "下载完成"
    FirmwareDownloadStatus.Failed -> "下载失败"
    FirmwareDownloadStatus.Cancelled -> "已取消"
}

private fun String?.asDisplayValue(): String =
    this?.takeIf(String::isNotBlank) ?: "—"

private fun String?.asMaskedSerial(): String {
    val serial = this?.takeIf(String::isNotBlank) ?: return "—"
    return if (serial.length <= 4) {
        "••••"
    } else {
        "•".repeat(serial.length - 4) + serial.takeLast(4)
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (index == 0) {
        "${bytes} ${units[index]}"
    } else {
        String.format(Locale.US, "%.2f %s", value, units[index])
    }
}

private fun deviceErrorMessage(error: DeviceReadError): String = when (error) {
    DeviceReadError.Unavailable -> "读取不到设备属性，确认本机 getprop 可用。"
    is DeviceReadError.Failed -> "getprop 退出码 ${error.exitCode}，设备属性读取失败。"
    DeviceReadError.Interrupted -> "设备属性读取被中断，请重试。"
}

private fun queryErrorMessage(error: QueryError): String = when (error) {
    QueryError.Network -> "请求没有到达 vivo 升级服务，检查网络后重试。"
    is QueryError.HttpStatus -> "vivo 服务返回 HTTP ${error.code}。"
    is QueryError.ServerRejected -> if (error.serverMessage.isBlank()) {
        "vivo 服务拒绝了本次请求（错误码 ${error.code}）。"
    } else {
        "vivo 服务拒绝了本次请求（错误码 ${error.code}）：${error.serverMessage}"
    }
    QueryError.InvalidResponse -> "vivo 服务返回了无法解析的内容。"
    QueryError.Unexpected -> "请求过程中发生未知错误。"
}
