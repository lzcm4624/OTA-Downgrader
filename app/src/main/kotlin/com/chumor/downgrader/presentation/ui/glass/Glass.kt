package com.chumor.downgrader.presentation.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Visual language: iOS-style liquid glass. Soft drifting light behind frosted,
 * translucent panes with specular edges, generous radii and gentle depth.
 */
data class GlassPalette(
    val dark: Boolean,
    val backdropTop: Color,
    val backdropBottom: Color,
    val blobA: Color,
    val blobB: Color,
    val blobC: Color,
    val blobD: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassTint: Color,
    val edgeTop: Color,
    val edgeBottom: Color,
    val innerGlow: Color,
    val text: Color,
    val textDim: Color,
    val textMute: Color,
    val accent: Color,
    val accentSoft: Color,
    val green: Color,
    val amber: Color,
    val red: Color,
    val hairline: Color,
    val shadow: Color,
) {
    companion object {
        val Dark = GlassPalette(
            dark = true,
            backdropTop = Color(0xFF0B1224),
            backdropBottom = Color(0xFF04060D),
            blobA = Color(0xFF2F6BFF),
            blobB = Color(0xFF9B4BFF),
            blobC = Color(0xFF00C2D1),
            blobD = Color(0xFFFF4FA3),
            glassTop = Color(0x2BFFFFFF),
            glassBottom = Color(0x0EFFFFFF),
            glassTint = Color(0x14FFFFFF),
            edgeTop = Color(0x8CFFFFFF),
            edgeBottom = Color(0x1FFFFFFF),
            innerGlow = Color(0x33FFFFFF),
            text = Color(0xFFF6F8FC),
            textDim = Color(0xFFAFB9CC),
            textMute = Color(0xFF6F7B90),
            accent = Color(0xFF5AA2FF),
            accentSoft = Color(0xFF8FC4FF),
            green = Color(0xFF46E0A8),
            amber = Color(0xFFFFC15E),
            red = Color(0xFFFF7A8C),
            hairline = Color(0x1FFFFFFF),
            shadow = Color(0xFF000000),
        )

        val Light = GlassPalette(
            dark = false,
            backdropTop = Color(0xFFE8EEFA),
            backdropBottom = Color(0xFFF7F9FF),
            blobA = Color(0xFF5B8DFF),
            blobB = Color(0xFFB07CFF),
            blobC = Color(0xFF3FD4E0),
            blobD = Color(0xFFFF8FC4),
            glassTop = Color(0xCCFFFFFF),
            glassBottom = Color(0x8CFFFFFF),
            glassTint = Color(0x66FFFFFF),
            edgeTop = Color(0xFFFFFFFF),
            edgeBottom = Color(0x33FFFFFF),
            innerGlow = Color(0x66FFFFFF),
            text = Color(0xFF0E1420),
            textDim = Color(0xFF4B5568),
            textMute = Color(0xFF8B94A6),
            accent = Color(0xFF0A72F0),
            accentSoft = Color(0xFF3D8BFF),
            green = Color(0xFF0FA47A),
            amber = Color(0xFFC8801A),
            red = Color(0xFFD6455C),
            hairline = Color(0x14000000),
            shadow = Color(0xFF3B4C6E),
        )
    }
}

val LocalGlassPalette = compositionLocalOf { GlassPalette.Dark }

object Glass {
    val palette: GlassPalette
        @Composable get() = LocalGlassPalette.current

    fun title(size: Float = 28f, color: Color = Color.Unspecified, weight: FontWeight = FontWeight.Bold): TextStyle =
        TextStyle(fontSize = size.sp, fontWeight = weight, letterSpacing = (-0.4).sp, lineHeight = (size * 1.18f).sp)

    fun headline(size: Float = 17f, color: Color = Color.Unspecified, weight: FontWeight = FontWeight.SemiBold): TextStyle =
        TextStyle(fontSize = size.sp, fontWeight = weight, letterSpacing = (-0.2).sp, lineHeight = (size * 1.32f).sp)

    fun body(size: Float = 14.5f, color: Color = Color.Unspecified, weight: FontWeight = FontWeight.Normal): TextStyle =
        TextStyle(fontSize = size.sp, fontWeight = weight, letterSpacing = 0.sp, lineHeight = (size * 1.45f).sp)

    fun label(size: Float = 12f, color: Color = Color.Unspecified, weight: FontWeight = FontWeight.Medium): TextStyle =
        TextStyle(fontSize = size.sp, fontWeight = weight, letterSpacing = 0.1.sp, lineHeight = (size * 1.35f).sp)

    fun mono(size: Float = 12f, color: Color = Color.Unspecified, weight: FontWeight = FontWeight.Normal): TextStyle =
        TextStyle(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontSize = size.sp,
            fontWeight = weight,
            letterSpacing = 0.2.sp,
            lineHeight = (size * 1.45f).sp,
        )
}


/**
 * Minimal text primitive (the project does not depend on material3).
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = if (textAlign != null) style.copy(textAlign = textAlign) else style,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun GlassTheme(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) GlassPalette.Dark else GlassPalette.Light
    CompositionLocalProvider(LocalGlassPalette provides palette) {
        Box(modifier = modifier.fillMaxSize()) { content() }
    }
}

/**
 * Backdrop: a deep gradient with slow drifting colour blooms, the light source
 * that every glass pane refracts.
 */
@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val p = Glass.palette
    val transition = rememberInfiniteTransition(label = "backdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val breathe by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.backdropTop, p.backdropBottom)))
            .drawBehind {
                val w = size.width
                val h = size.height
                val intensity = if (p.dark) 1f else 0.75f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(p.blobA.copy(alpha = 0.55f * intensity * breathe), Color.Transparent),
                        center = Offset(w * (0.16f + 0.06f * drift), h * 0.10f),
                        radius = w * 0.95f,
                    ),
                    radius = w * 0.95f,
                    center = Offset(w * (0.16f + 0.06f * drift), h * 0.10f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(p.blobB.copy(alpha = 0.48f * intensity * breathe), Color.Transparent),
                        center = Offset(w * (0.92f - 0.05f * drift), h * 0.26f),
                        radius = w * 0.85f,
                    ),
                    radius = w * 0.85f,
                    center = Offset(w * (0.92f - 0.05f * drift), h * 0.26f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(p.blobC.copy(alpha = 0.30f * intensity), Color.Transparent),
                        center = Offset(w * (0.10f + 0.10f * drift), h * 0.72f),
                        radius = w * 0.80f,
                    ),
                    radius = w * 0.80f,
                    center = Offset(w * (0.10f + 0.10f * drift), h * 0.72f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(p.blobD.copy(alpha = 0.20f * intensity), Color.Transparent),
                        center = Offset(w * (0.86f - 0.08f * drift), h * 0.94f),
                        radius = w * 0.70f,
                    ),
                    radius = w * 0.70f,
                    center = Offset(w * (0.86f - 0.08f * drift), h * 0.94f),
                )
            },
        content = content,
    )
}

private fun Modifier.glassSurface(
    shape: Shape,
    palette: GlassPalette,
    borderWidth: Dp = 1.dp,
    tint: Color? = null,
): Modifier = this
    .shadow(
        elevation = if (palette.dark) 22.dp else 16.dp,
        shape = shape,
        ambientColor = palette.shadow.copy(alpha = if (palette.dark) 0.55f else 0.22f),
        spotColor = palette.shadow.copy(alpha = if (palette.dark) 0.45f else 0.28f),
    )
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                tint?.copy(alpha = if (palette.dark) 0.22f else 0.55f) ?: palette.glassTop,
                tint?.copy(alpha = if (palette.dark) 0.07f else 0.22f) ?: palette.glassBottom,
            ),
        ),
    )
    .drawBehind {
        // soft light pooling in from the top-left corner
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(palette.innerGlow.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(size.width * 0.12f, 0f),
                radius = size.width * 0.85f,
            ),
        )
        // specular sheen along the very top edge
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = if (palette.dark) 0.45f else 0.9f),
                    Color.Transparent,
                ),
            ),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, 1.2.dp.toPx()),
        )
    }
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(palette.edgeTop, palette.edgeBottom, palette.edgeTop.copy(alpha = 0.35f)),
            start = Offset.Zero,
            end = Offset.Infinite,
        ),
        shape = shape,
    )

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    tint: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = Glass.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(shape = shape, palette = p, tint = tint)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun SectionLabel(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    accent: Color = Color.Unspecified,
) {
    val p = Glass.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = Glass.label(12.5f, if (accent == Color.Unspecified) p.textDim else accent, FontWeight.SemiBold),
        )
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            Text(text = trailing, style = Glass.label(11.5f, p.textMute, FontWeight.Medium))
        }
    }
}

@Composable
fun DataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle? = null,
    showDivider: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val p = Glass.palette
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = Glass.body(14f, p.textDim))
            Spacer(Modifier.weight(1f))
            if (trailing != null) {
                trailing()
            } else {
                Text(
                    text = value,
                    style = valueStyle ?: Glass.body(14f, p.text, FontWeight.Medium),
                    textAlign = TextAlign.End,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(p.hairline),
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.Unspecified,
    inactiveColor: Color = Color.Unspecified,
) {
    val p = Glass.palette
    val color = when {
        active && activeColor != Color.Unspecified -> activeColor
        !active && inactiveColor != Color.Unspecified -> inactiveColor
        active -> p.green
        else -> p.amber
    }
    val transition = rememberInfiniteTransition(label = "pill")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "pillPulse",
    )
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = if (p.dark) 0.16f else 0.14f))
            .border(0.8.dp, color.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .graphicsLayer { alpha = pulse }
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(7.dp))
        Text(text = text, style = Glass.label(12f, color, FontWeight.SemiBold))
    }
}

@Composable
fun GlassButton(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    prominent: Boolean = true,
    accent: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    val p = Glass.palette
    val tint = if (accent == Color.Unspecified) p.accent else accent
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, tween(120), label = "btnScale")
    val alpha = if (enabled) 1f else 0.42f
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .then(
                if (prominent) {
                    Modifier
                        .shadow(
                            elevation = if (p.dark) 16.dp else 12.dp,
                            shape = shape,
                            ambientColor = tint.copy(alpha = 0.55f),
                            spotColor = tint.copy(alpha = 0.65f),
                        )
                        .clip(shape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    tint.copy(alpha = if (p.dark) 0.92f else 0.95f),
                                    tint.copy(alpha = if (p.dark) 0.62f else 0.78f),
                                ),
                            ),
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.62f), Color.White.copy(alpha = 0.10f)),
                            ),
                            shape,
                        )
                } else {
                    Modifier
                        .clip(shape)
                        .background(p.glassTint)
                        .border(1.dp, p.edgeTop.copy(alpha = 0.35f), shape)
                }
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                Spinner(color = Color.White.copy(alpha = 0.9f), size = 16.dp)
                Spacer(Modifier.width(10.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = Glass.headline(16f, if (prominent) Color.White else p.text, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = Glass.label(11.5f, if (prominent) Color.White.copy(alpha = 0.72f) else p.textMute),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun SmallGlassButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    val p = Glass.palette
    val color = if (tint == Color.Unspecified) p.text else tint
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(110), label = "smallBtnScale")
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .clip(shape)
            .background(p.glassTint)
            .border(1.dp, p.edgeTop.copy(alpha = 0.4f), shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = Glass.label(12.5f, color, FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun Spinner(color: Color = Color.Unspecified, size: Dp = 16.dp) {
    val p = Glass.palette
    val c = if (color == Color.Unspecified) p.accent else color
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spinAngle",
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = angle }
            .drawBehind {
                val stroke = 1.8.dp.toPx()
                drawArc(
                    color = c.copy(alpha = 0.22f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = c,
                    startAngle = -90f,
                    sweepAngle = 110f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            },
    )
}

@Composable
fun GlassProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val p = Glass.palette
    val color = if (tint == Color.Unspecified) p.accent else tint
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(420), label = "progress")
    val live = animated > 0.02f && animated < 0.999f
    val sweep = rememberInfiniteTransition(label = "progressSweep")
    val sweepTravel by sweep.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "progressSweepTravel",
    )
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(shape)
            .background(if (p.dark) Color(0x33000000) else Color(0x12000000))
            .border(0.8.dp, p.edgeTop.copy(alpha = 0.28f), shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(color.copy(alpha = 0.78f), color),
                    ),
                )
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, size.height * 0.55f),
                    )
                    if (live) {
                        val bandWidth = size.width * 0.34f
                        val x = (size.width + bandWidth) * sweepTravel - bandWidth
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.42f),
                                    Color.Transparent,
                                ),
                                startX = x,
                                endX = x + bandWidth,
                            ),
                            topLeft = Offset(x, 0f),
                            size = Size(bandWidth, size.height),
                        )
                    }
                },
        )
    }
}

@Composable
fun GlassErrorNote(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    val p = Glass.palette
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        tint = p.red,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(p.red.copy(alpha = 0.22f), CircleShape)
                    .border(1.dp, p.red.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("!", style = Glass.label(13f, p.red, FontWeight.Bold))
            }
            Spacer(Modifier.width(10.dp))
            Text(title, style = Glass.headline(14.5f, p.text, FontWeight.SemiBold))
        }
        Spacer(Modifier.height(7.dp))
        Text(message, style = Glass.body(13.5f, p.textDim))
    }
}

@Composable
fun SkeletonLines(
    modifier: Modifier = Modifier,
    count: Int = 4,
) {
    val p = Glass.palette
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmer by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmer",
    )
    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            repeat(count) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (index % 2 == 0) 0.62f else 0.85f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(p.textMute.copy(alpha = 0.16f * shimmer * 2f)),
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        SkeletonSweep(modifier = Modifier.matchParentSize())
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    val p = Glass.palette
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, p.hairline, Color.Transparent),
                ),
            ),
    )
}
