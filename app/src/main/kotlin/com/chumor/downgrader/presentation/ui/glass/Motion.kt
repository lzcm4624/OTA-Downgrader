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
import androidx.compose.runtime.remember
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
 *
 * 每次进入组合都会播一次（滑出屏幕再滑回来、切换通道、重新查询后刷出新结果都算），
 * 所以这里刻意不用 rememberSaveable —— 那会把「已播放」记进快照，
 * 导致只有第一次进页面有动画，后面全是干巴巴直接出现。
 */
@Composable
fun Reveal(
    modifier: Modifier = Modifier,
    order: Int = 0,
    replayKey: Any? = Unit,
    durationMillis: Int = 420,
    content: @Composable () -> Unit,
) {
    val progress = remember(replayKey) { Animatable(0f) }
    LaunchedEffect(replayKey) {
        progress.snapTo(0f)
        val stagger = order.coerceIn(0, 8) * 34L
        if (stagger > 0L) delay(stagger)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
        )
    }
    Box(
        modifier = modifier.graphicsLayer {
            val value = progress.value
            alpha = value
            translationY = (1f - value) * 26.dp.toPx()
            val scale = 0.982f + 0.018f * value
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
    replayKey: Any? = Unit,
    content: @Composable () -> Unit,
) {
    item(key = key) {
        Reveal(order = order, replayKey = replayKey, content = content)
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
