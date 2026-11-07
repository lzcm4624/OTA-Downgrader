package com.chumor.downgrader.presentation.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 卡片入场：淡入 + 上移 + 轻微放大。
 * LazyColumn 里滑到哪里、哪一项就播一次；切换通道时整块重新入场。
 */
@Composable
fun Reveal(
    modifier: Modifier = Modifier,
    order: Int = 0,
    play: Boolean = true,
    content: @Composable () -> Unit,
) {
    var played by rememberSaveable { mutableStateOf(!play) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(play) {
        if (played || !play) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        val stagger = order.coerceIn(0, 8) * 42L
        if (stagger > 0L) delay(stagger)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        )
        played = true
    }
    Box(
        modifier = modifier.graphicsLayer {
            val value = progress.value
            alpha = value
            translationY = (1f - value) * 24.dp.toPx()
            val scale = 0.985f + 0.015f * value
            scaleX = scale
            scaleY = scale
        },
        content = { content() },
    )
}

/** 列表项 + 入场动画，一行搞定。 */
fun LazyListScope.revealItem(
    key: Any,
    order: Int = 0,
    play: Boolean = true,
    content: @Composable () -> Unit,
) {
    item(key = key) {
        Reveal(order = order, play = play, content = content)
    }
}

/**
 * 不确定进度条：一条光带循环扫过，给「正在查 / 正在读」这类没有百分比的动作做反馈。
 */
@Composable
fun IndeterminateBar(
    modifier: Modifier = Modifier,
    height: Dp = 2.dp,
    color: Color = Color.Unspecified,
) {
    val p = Glass.palette
    val tint = if (color == Color.Unspecified) p.accent else color
    val transition = rememberInfiniteTransition(label = "indeterminate")
    val travel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "indeterminateTravel",
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val radius = size.height / 2f
        drawRoundRect(
            color = tint.copy(alpha = 0.16f),
            cornerRadius = CornerRadius(radius, radius),
            size = size,
        )
        val bandWidth = size.width * 0.46f
        val x = (size.width + bandWidth) * travel - bandWidth
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    tint.copy(alpha = 0.55f),
                    tint,
                    tint.copy(alpha = 0.55f),
                    Color.Transparent,
                ),
                startX = x,
                endX = x + bandWidth,
            ),
            topLeft = Offset(x, 0f),
            size = Size(bandWidth, size.height),
            cornerRadius = CornerRadius(radius, radius),
        )
    }
}

/**
 * 骨架屏上扫过的一道高光，让「正在读取」看着是在动。
 */
@Composable
fun SkeletonSweep(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val p = Glass.palette
    val tint = if (color == Color.Unspecified) p.text else color
    val transition = rememberInfiniteTransition(label = "sweep")
    val travel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweepTravel",
    )
    Canvas(modifier = modifier) {
        val bandWidth = size.width * 0.36f
        val x = (size.width + bandWidth) * travel - bandWidth
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, tint.copy(alpha = 0.16f), Color.Transparent),
                startX = x,
                endX = x + bandWidth,
            ),
            topLeft = Offset(x, 0f),
            size = Size(bandWidth, size.height),
        )
    }
}
