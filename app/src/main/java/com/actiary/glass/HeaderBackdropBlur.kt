package com.actiary.glass

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor

private const val MAX_BLUR_RADIUS_PX = 40

/**
 * Applies to a viewport starting at the top of the window, with the pinned header and ambient
 * background drawn separately. [headerHeight] includes the top system inset.
 */
@Composable
internal fun Modifier.headerBackdropBlur(headerHeight: Dp, scrollOffset: () -> Int): Modifier {
    if (headerHeight <= 0.dp) return this
    val density = LocalDensity.current
    val headerPx = with(density) { headerHeight.toPx() }
    val extensionPx = with(density) { 34.dp.toPx() }
    val insetPx = WindowInsets.statusBars.getTop(density).toFloat().coerceAtMost(headerPx)
    val blur = if (Build.VERSION.SDK_INT >= 33 && !rememberGlassDegraded()) {
        val radiusPx = with(density) { 10.dp.toPx() }.coerceAtMost(MAX_BLUR_RADIUS_PX.toFloat())
        rememberHeaderBlurEffect(radiusPx, insetPx, headerPx + extensionPx)
    } else {
        null
    }

    return graphicsLayer {
        val scrolled = scrollOffset() > 0
        renderEffect = if (scrolled) blur else null
        compositingStrategy = if (scrolled) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }.drawWithCache {
        // Remove foreground alpha before it is blurred, so system icons sit over the ambient
        // background even when high-contrast foreground content passes underneath.
        val mask = Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.70f), Color.Transparent),
            startY = insetPx,
            endY = headerPx,
        )
        val maskSize = Size(size.width, minOf(size.height, headerPx))
        onDrawWithContent {
            drawContent()
            val reveal = (scrollOffset() / extensionPx).coerceIn(0f, 1f)
            if (reveal > 0f) {
                drawRect(mask, size = maskSize, alpha = reveal, blendMode = BlendMode.DstOut)
            }
        }
    }
}

/** Soft vertical neutral scrim behind the pinned header while content scrolls underneath. */
@Composable
internal fun Modifier.headerBackdropScrim(scrollOffset: () -> Int): Modifier {
    val bg = LocalThemeTokens.current.neutrals.background.asColor()
    val extensionPx = with(LocalDensity.current) { 34.dp.toPx() }
    return drawWithCache {
        val scrim = Brush.verticalGradient(
            0f to bg.copy(alpha = 0.72f),
            0.72f to bg.copy(alpha = 0.42f),
            1f to Color.Transparent,
            startY = 0f,
            endY = size.height,
        )
        onDrawBehind {
            val reveal = (scrollOffset() / extensionPx).coerceIn(0f, 1f)
            if (reveal > 0f) {
                drawRect(scrim, alpha = reveal)
            }
        }
    }
}

@RequiresApi(33)
@Composable
private fun rememberHeaderBlurEffect(radius: Float, start: Float, end: Float): RenderEffect {
    val horizontal = remember { RuntimeShader(BlurPass).apply { setFloatUniform("axis", 1f, 0f) } }
    val vertical = remember { RuntimeShader(BlurPass).apply { setFloatUniform("axis", 0f, 1f) } }
    return remember(radius, start, end) {
        fun pass(shader: RuntimeShader): AndroidRenderEffect {
            shader.setFloatUniform("maxRadius", radius)
            shader.setFloatUniform("blurStartY", start)
            shader.setFloatUniform("blurEndY", end)
            return AndroidRenderEffect.createRuntimeShaderEffect(shader, "source")
        }
        AndroidRenderEffect.createChainEffect(pass(vertical), pass(horizontal)).asComposeRenderEffect()
    }
}

// Physical-pixel taps avoid repeated sharp copies of text at the largest blur radius.
private val BlurPass = """
    uniform shader source;
    uniform float2 axis;
    uniform float maxRadius;
    uniform float blurStartY;
    uniform float blurEndY;

    half4 sampleAt(float2 p) {
        return source.eval(float2(p.x, max(p.y, 0.5)));
    }

    half4 main(float2 p) {
        half4 center = source.eval(p);
        if (p.y >= blurEndY) {
            return center;
        }

        float radius = maxRadius * (1.0 - smoothstep(blurStartY, blurEndY, p.y));
        float sigma = max(radius * 0.5, 0.25);
        float exponent = -0.5 / (sigma * sigma);
        float ratio = exp(exponent);
        float ratioStep = exp(2.0 * exponent);
        float weight = 1.0;
        float weightSum = 1.0;
        float4 sum = float4(center);
        for (int i = 1; i <= $MAX_BLUR_RADIUS_PX; i++) {
            float pixel = float(i);
            float coverage = min(radius + 1.0 - pixel, 1.0);
            if (coverage <= 0.0) { break; }
            weight *= ratio;
            ratio *= ratioStep;
            float coveredWeight = weight * coverage;
            float2 tapOffset = axis * pixel;
            sum += float4(sampleAt(p - tapOffset) + sampleAt(p + tapOffset)) * coveredWeight;
            weightSum += 2.0 * coveredWeight;
        }
        return half4(sum / weightSum);
    }
"""
