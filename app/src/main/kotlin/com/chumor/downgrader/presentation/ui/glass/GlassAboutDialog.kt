package com.chumor.downgrader.presentation.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chumor.downgrader.BuildConfig

private const val SOURCE_URL = "https://github.com/Chumor/Downgrader"

@Composable
fun GlassAboutDialog(show: Boolean, onDismissRequest: () -> Unit) {
    if (!show) return
    val uriHandler = LocalUriHandler.current
    val p = Glass.palette

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp),
        ) {
            GlassCard(
                shape = RoundedCornerShape(30.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 22.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF4C8DFF), Color(0xFF9B6BFF))),
                                RoundedCornerShape(13.dp),
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        GlyphArrow(color = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(13.dp))
                    Column {
                        Text("降级助手", style = Glass.headline(18f, p.text, FontWeight.Bold))
                        Text("vivo 系统降级查询", style = Glass.label(12f, p.textMute))
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "读取本机降级凭据，向 vivo 官方升级服务查询可用的降级包，并支持完整性校验下载。",
                    style = Glass.body(13.5f, p.textDim),
                )

                Spacer(Modifier.height(16.dp))
                DataRow(label = "版本", value = BuildConfig.VERSION_NAME)
                DataRow(label = "构建号", value = BuildConfig.VERSION_CODE.toString())
                DataRow(label = "开源协议", value = "GPL-3.0")
                DataRow(label = "原项目", value = "github.com/Chumor", showDivider = false)

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SmallGlassButton(
                        title = "查看源码",
                        modifier = Modifier.weight(1f),
                        tint = p.accent,
                        onClick = {
                            try {
                                uriHandler.openUri(SOURCE_URL)
                            } catch (_: Exception) {
                                // No browser available.
                            }
                        },
                    )
                    SmallGlassButton(
                        title = "关闭",
                        modifier = Modifier.weight(1f),
                        onClick = onDismissRequest,
                    )
                }
            }
        }
    }
}

/** Simple downward glyph: a shaft, a rounded chevron and a landing line. */
@Composable
fun GlyphArrow(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.13f
        val cx = size.width / 2f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(cx, size.height * 0.06f),
            end = androidx.compose.ui.geometry.Offset(cx, size.height * 0.60f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - size.width * 0.27f, size.height * 0.42f)
            lineTo(cx, size.height * 0.70f)
            lineTo(cx + size.width * 0.27f, size.height * 0.42f)
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
        drawLine(
            color = color.copy(alpha = 0.7f),
            start = androidx.compose.ui.geometry.Offset(cx - size.width * 0.34f, size.height * 0.94f),
            end = androidx.compose.ui.geometry.Offset(cx + size.width * 0.34f, size.height * 0.94f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}
