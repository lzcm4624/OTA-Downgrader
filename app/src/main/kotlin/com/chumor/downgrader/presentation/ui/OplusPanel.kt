package com.chumor.downgrader.presentation.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chumor.downgrader.domain.model.OplusBrand
import com.chumor.downgrader.domain.model.OplusFirmware
import com.chumor.downgrader.domain.model.OplusQueryError
import com.chumor.downgrader.domain.model.OplusSource
import com.chumor.downgrader.presentation.DowngraderUiState
import com.chumor.downgrader.presentation.FirmwareChannel
import com.chumor.downgrader.presentation.ui.glass.DataRow
import com.chumor.downgrader.presentation.ui.glass.Glass
import com.chumor.downgrader.presentation.ui.glass.GlassButton
import com.chumor.downgrader.presentation.ui.glass.GlassCard
import com.chumor.downgrader.presentation.ui.glass.GlassErrorNote
import com.chumor.downgrader.presentation.ui.glass.LocalLiquidPhase
import com.chumor.downgrader.presentation.ui.glass.SectionLabel
import com.chumor.downgrader.presentation.ui.glass.liquidGlassEdge
import com.chumor.downgrader.presentation.ui.glass.SmallGlassButton
import com.chumor.downgrader.presentation.ui.glass.Spinner
import com.chumor.downgrader.presentation.ui.glass.StatusPill
import com.chumor.downgrader.presentation.ui.glass.Text
import com.chumor.downgrader.presentation.ui.glass.revealItem
import java.util.Locale

/** 通道切换胶囊：高亮块滑动过去，也可以直接左右滑动手势切。 */
@Composable
fun ChannelSwitcher(
    current: FirmwareChannel,
    onSelect: (FirmwareChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = Glass.palette
    val channels = FirmwareChannel.entries
    val phase = LocalLiquidPhase.current
    val selectedIndex = channels.indexOf(current).coerceAtLeast(0)
    val dragAccumulator = remember { floatArrayOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(if (p.dark) Color(0x1FFFFFFF) else Color(0x0F000000))
            .liquidGlassEdge(
                cornerRadius = 24.dp,
                palette = p,
                phase = phase,
                width = 1.3.dp,
            )
            .padding(4.dp)
            .pointerInput(selectedIndex) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator[0] = 0f },
                    onDragCancel = { dragAccumulator[0] = 0f },
                    onDragEnd = {
                        val threshold = 44.dp.toPx()
                        val travelled = dragAccumulator[0]
                        dragAccumulator[0] = 0f
                        when {
                            travelled <= -threshold ->
                                onSelect(channels[(selectedIndex + 1).coerceAtMost(channels.lastIndex)])
                            travelled >= threshold ->
                                onSelect(channels[(selectedIndex - 1).coerceAtLeast(0)])
                        }
                    },
                ) { _, delta -> dragAccumulator[0] += delta }
            },
    ) {
        val gap = 4.dp
        val itemWidth = (maxWidth - gap * (channels.size - 1)) / channels.size
        val highlightOffset by animateDpAsState(
            targetValue = (itemWidth + gap) * selectedIndex,
            animationSpec = tween(340, easing = FastOutSlowInEasing),
            label = "channelHighlight",
        )
        Box(
            modifier = Modifier
                .offset(x = highlightOffset)
                .width(itemWidth)
                .height(40.dp)
                .clip(CircleShape)
                .background(p.accent.copy(alpha = if (p.dark) 0.26f else 0.16f))
                .liquidGlassEdge(
                    cornerRadius = 20.dp,
                    palette = p,
                    phase = phase,
                    width = 1.3.dp,
                    glow = false,
                    strength = 0.9f,
                ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            channels.forEach { channel ->
                val selected = channel == current
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(channel) },
                    contentAlignment = Alignment.Center,
                ) {
                    val contentAlpha by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.72f,
                        animationSpec = tween(260),
                        label = "channelLabel",
                    )
                    Text(
                        text = channel.label,
                        style = Glass.label(
                            size = 13f,
                            color = (if (selected) p.text else p.textMute).copy(alpha = contentAlpha),
                            weight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

/** 欧加真整块内容：设备、机型输入、查询、结果列表。 */
fun LazyListScope.oplusContent(
    state: DowngraderUiState,
    replayKey: Any? = Unit,
    onModelChange: (String) -> Unit,
    onQuery: () -> Unit,
    onCopyUrl: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    revealItem(key = "oplus.device.label", order = 0, replayKey = replayKey) {
        SectionLabel(
            title = "设备信息",
            trailing = state.oplusDevice?.brand?.label ?: "未识别",
        )
    }
    revealItem(key = "oplus.device.card", order = 1, replayKey = replayKey) {
        OplusDeviceCard(state = state)
    }

    revealItem(key = "oplus.model.label", order = 2, replayKey = replayKey) {
        SectionLabel(title = "机型代号", trailing = "可手动输入")
    }
    revealItem(key = "oplus.model.card", order = 3, replayKey = replayKey) {
        GlassCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
            ModelInputField(
                value = state.oplusModelInput,
                placeholder = "例如 RMX3888 / PJZ110 / CPH2661",
                onValueChange = onModelChange,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "品牌不同代号前缀不同：真我 RMX/CPH、OPPO Pxx、一加 Pxx/CPH。代号可在「关于手机」里看到。",
                style = Glass.label(11.5f, Glass.palette.textMute),
            )
        }
    }

    revealItem(key = "oplus.query.action", order = 4, replayKey = replayKey) {
        GlassButton(
            title = when {
                state.isQueryingOplus -> state.oplusStage.ifBlank { "正在查询" }
                else -> "查询欧加真降级包"
            },
            subtitle = "官方 OTA 版本链 + 售后降级服务器",
            enabled = state.canQueryOplus,
            loading = state.isQueryingOplus,
            onClick = onQuery,
        )
    }

    state.oplusQueryError?.let { error ->
        revealItem(key = "oplus.error", order = 4, replayKey = replayKey) {
            GlassErrorNote(title = "查询失败", message = oplusErrorMessage(error))
        }
    }

    val afterSale = state.oplusAfterSalePackages
    if (afterSale.isNotEmpty()) {
        revealItem(key = "oplus.aftersale.label", order = 5, replayKey = replayKey) {
            SectionLabel(title = "售后降级包", trailing = "${afterSale.size} 个")
        }
        afterSale.forEachIndexed { index, firmware ->
            revealItem(key = "oplus.aftersale.$index", order = 6 + index) {
                OplusPackageCard(
                    firmware = firmware,
                    onCopyUrl = { onCopyUrl(firmware.downloadUrl) },
                    onOpenUrl = { onOpenUrl(firmware.downloadUrl) },
                )
            }
        }
    }

    val official = state.oplusOfficialPackages
    if (official.isNotEmpty()) {
        revealItem(key = "oplus.official.label", order = 5, replayKey = replayKey) {
            SectionLabel(title = "官方版本链", trailing = "${official.size} 个版本")
        }
        official.forEachIndexed { index, firmware ->
            revealItem(key = "oplus.official.$index", order = 6 + index) {
                OplusPackageCard(
                    firmware = firmware,
                    highlight = index == 0,
                    onCopyUrl = { onCopyUrl(firmware.downloadUrl) },
                    onOpenUrl = { onOpenUrl(firmware.downloadUrl) },
                )
            }
        }
    }

    if (state.oplusQueried && state.oplusQueryError == null && state.oplusPackages.isEmpty()) {
        revealItem(key = "oplus.empty", order = 5, replayKey = replayKey) {
            GlassCard {
                Text(
                    text = "这个代号没查到可用的包。确认代号写对了（不是市场名），或者换一个分支再试。",
                    style = Glass.body(14f, Glass.palette.textDim),
                )
            }
        }
    }

    if (state.oplusPackages.isNotEmpty()) {
        revealItem(key = "oplus.tip", order = 6, replayKey = replayKey) {
            GlassCard(tint = null, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp)) {
                Text(
                    text = "整包动辄 8-11 GB，手机上下载容易断，建议复制直链到电脑用下载工具拉。官方 OTA 直链有时效，过期重查一次即可；售后降级包直链长期有效。跨 Android 大版本往回刷会触发防回滚（ARB），刷之前确认目标版本的 ARB 等级不高于当前，否则可能变砖。",
                    style = Glass.label(11.5f, Glass.palette.textMute),
                )
            }
        }
    }
}

@Composable
private fun OplusDeviceCard(state: DowngraderUiState) {
    val p = Glass.palette
    val device = state.oplusDevice
    GlassCard {
        if (device == null || !device.isRecognized) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(text = "未识别", active = false)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "当前不是欧加真机型，或读不到代号。手动输入代号也能查。",
                    style = Glass.body(13f, p.textDim),
                )
            }
            return@GlassCard
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = device.brand.label,
                style = Glass.title(24f, p.text),
            )
            Spacer(Modifier.width(10.dp))
            StatusPill(text = "已识别", active = true)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = device.model.ifBlank { device.code },
            style = Glass.headline(15.5f, p.textDim),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        DataRow(label = "机型代号", value = device.code.ifBlank { "—" }, showDivider = true)
        DataRow(
            label = "系统版本",
            value = device.colorosVersion.ifBlank { "—" },
            showDivider = true,
        )
        DataRow(
            label = "Android",
            value = device.androidVersion.ifBlank { "—" },
            showDivider = device.otaVersion.isNotBlank(),
        )
        if (device.otaVersion.isNotBlank()) {
            DataRow(
                label = "内部版本",
                value = device.otaVersion,
                showDivider = false,
                valueStyle = Glass.mono(12.5f, p.textDim),
            )
        }
    }
}

@Composable
private fun ModelInputField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    val p = Glass.palette
    val phase = LocalLiquidPhase.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (p.dark) Color(0x1AFFFFFF) else Color(0x0D000000))
            .liquidGlassEdge(
                cornerRadius = 16.dp,
                palette = p,
                phase = phase,
                width = 1.3.dp,
                glow = false,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(text = placeholder, style = Glass.mono(14f, p.textMute))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = Glass.mono(14f, p.text),
            cursorBrush = SolidColor(p.accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OplusPackageCard(
    firmware: OplusFirmware,
    onCopyUrl: () -> Unit,
    onOpenUrl: () -> Unit,
    highlight: Boolean = false,
) {
    val p = Glass.palette
    val accent = when (firmware.source) {
        OplusSource.AFTER_SALE -> p.green
        OplusSource.OFFICIAL -> p.accent
    }
    GlassCard(tint = if (highlight) accent else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(accent, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = firmware.version,
                style = Glass.headline(16.5f, p.text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildString {
                    append(firmware.androidVersion.ifBlank { "Android ?" })
                    if (firmware.branch.isNotBlank()) append(" · 分支 ").append(firmware.branch)
                    append(" · ").append(formatSize(firmware.sizeBytes))
                },
                style = Glass.label(12f, p.textDim),
            )
        }
        Spacer(Modifier.height(10.dp))

        if (firmware.securityPatch.isNotBlank()) {
            DataRow(label = "安全补丁", value = firmware.securityPatch, showDivider = true)
        }
        if (firmware.published.isNotBlank()) {
            DataRow(label = "发布日期", value = formatStamp(firmware.published), showDivider = true)
        }
        if (firmware.otaVersion.isNotBlank()) {
            DataRow(
                label = "内部版本",
                value = firmware.otaVersion,
                showDivider = firmware.md5.isNotBlank(),
                valueStyle = Glass.mono(12f, p.textDim),
            )
        }
        if (firmware.md5.isNotBlank()) {
            DataRow(
                label = "MD5",
                value = firmware.md5,
                showDivider = false,
                valueStyle = Glass.mono(11.5f, p.textDim),
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallGlassButton(
                title = "复制直链",
                enabled = firmware.downloadUrl.isNotBlank(),
                tint = accent,
                onClick = onCopyUrl,
            )
            SmallGlassButton(
                title = "打开下载",
                enabled = firmware.downloadUrl.isNotBlank(),
                onClick = onOpenUrl,
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "大小未知"
    val gb = bytes / 1024.0 / 1024.0 / 1024.0
    return if (gb >= 1.0) {
        String.format(Locale.US, "%.2f GB", gb)
    } else {
        String.format(Locale.US, "%.0f MB", bytes / 1024.0 / 1024.0)
    }
}

/** 服务器的 publishedTime 是毫秒时间戳；能格式化就格式化，不能就原样显示。 */
private fun formatStamp(raw: String): String {
    val millis = raw.toLongOrNull() ?: return raw
    return try {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        format.format(java.util.Date(millis))
    } catch (_: Exception) {
        raw
    }
}

private fun oplusErrorMessage(error: OplusQueryError): String = when (error) {
    OplusQueryError.Network -> "连不上服务器，检查网络后重试"
    is OplusQueryError.HttpStatus -> "服务器拒绝了请求（${error.code}），稍后重试"
    OplusQueryError.NotFound -> "这个代号没查到可用的包，确认代号写对了"
    OplusQueryError.InvalidResponse -> "返回的数据看不懂，可能是服务器改了接口"
    OplusQueryError.Unexpected -> "出了点意外，重试一次看看"
}
