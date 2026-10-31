package com.sukisu.ultra.ui.component.liquid

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.colorControls
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.runtimeShaderEffect

@Immutable
data class InnerShadow(
    val radius: Dp = 24.dp,
    val offset: DpOffset = DpOffset(0.dp, radius),
    val color: Color = Color.Black.copy(alpha = 0.15f),
    val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode,
) {
    companion object {
        @Stable val Default: InnerShadow = InnerShadow()
    }
}

fun Modifier.innerShadow(shape: Shape, shadow: () -> InnerShadow?): Modifier =
    this then InnerShadowElement(shape, shadow)

private class InnerShadowElement(val shape: Shape, val shadow: () -> InnerShadow?) :
    ModifierNodeElement<InnerShadowNode>() {
    override fun create(): InnerShadowNode = InnerShadowNode(shape, shadow)
    override fun update(node: InnerShadowNode) {
        node.shape = shape; node.shadow = shadow; node.invalidateDraw()
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "innerShadow"; properties["shape"] = shape; properties["shadow"] = shadow
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InnerShadowElement) return false
        return shape == other.shape && shadow == other.shadow
    }
    override fun hashCode(): Int = 31 * shape.hashCode() + shadow.hashCode()
}

private class InnerShadowNode(var shape: Shape, var shadow: () -> InnerShadow?) :
    Modifier.Node(), DrawModifierNode {
    override val shouldAutoInvalidate: Boolean = false
    private var shadowLayer: GraphicsLayer? = null
    private val paint = Paint()
    private val clipPath = Path()
    private var prevRadius = Float.NaN

    override fun ContentDrawScope.draw() {
        drawContent()
        val s = shadow() ?: return
        val layer = shadowLayer ?: return
        val radius = s.radius.toPx()
        val ox = s.offset.x.toPx(); val oy = s.offset.y.toPx()
        val outline = shape.createOutline(size, layoutDirection, this)
        clipPath.reset()
        when (outline) {
            is Outline.Rectangle -> clipPath.addRect(outline.rect)
            is Outline.Rounded -> clipPath.addRoundRect(outline.roundRect)
            is Outline.Generic -> clipPath.addPath(outline.path)
        }
        paint.color = s.color
        layer.alpha = s.alpha
        layer.blendMode = s.blendMode
        if (prevRadius != radius) {
            layer.renderEffect = if (radius > 0f) BlurEffect(radius, radius, TileMode.Decal) else null
            prevRadius = radius
        }
        layer.record {
            drawContext.canvas.let { c ->
                c.save(); c.clipPath(clipPath); c.drawOutline(outline, paint)
                c.translate(ox, oy); c.drawOutline(outline, ShadowMaskPaint)
                c.translate(-ox, -oy); c.restore()
            }
        }
        drawContext.canvas.let { c -> c.save(); c.clipPath(clipPath); drawLayer(layer); c.restore() }
    }
    override fun onAttach() {
        shadowLayer = requireGraphicsContext().createGraphicsLayer().apply {
            compositingStrategy = CompositingStrategy.Offscreen
        }
    }
    override fun onDetach() {
        shadowLayer?.let { requireGraphicsContext().releaseGraphicsLayer(it); shadowLayer = null }
    }
}

private val ShadowMaskPaint: Paint = Paint().apply { blendMode = BlendMode.Clear }

// ===== Lens & Vibrancy =====

fun BackdropEffectScope.lens(
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Float = 0f,
) {
    if (!isRuntimeShaderSupported()) return
    if (refractionHeight <= 0f || refractionAmount <= 0f) return
    if (padding < refractionAmount) padding = refractionAmount
    val radii = roundedRectCornerRadii() ?: return
    val disp = chromaticAberration > 0f
    val shader = if (disp) LENS_SHADER_DISPERSION else LENS_SHADER
    val key = if (disp) "LensDisp" else "Lens"
    val sf = downscaleFactor.coerceAtLeast(1).toFloat()
    val scaledRadii = FloatArray(radii.size) { radii[it] / sf }
    runtimeShaderEffect(key = key, shaderString = shader, uniformShaderName = "content") {
        setFloatUniform("size", size.width / sf, size.height / sf)
        setFloatUniform("offset", -padding / sf, -padding / sf)
        setFloatUniform("cornerRadii", scaledRadii)
        setFloatUniform("refractionHeight", refractionHeight / sf)
        setFloatUniform("refractionAmount", -refractionAmount / sf)
        setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
        if (disp) setFloatUniform("chromaticAberration", chromaticAberration)
    }
}

private fun BackdropEffectScope.roundedRectCornerRadii(): FloatArray? {
    val cs = shape as? CornerBasedShape ?: return null
    val s = size; val max = s.minDimension / 2f
    val ltr = layoutDirection == LayoutDirection.Ltr
    val tl = if (ltr) cs.topStart.toPx(s, this) else cs.topEnd.toPx(s, this)
    val tr = if (ltr) cs.topEnd.toPx(s, this) else cs.topStart.toPx(s, this)
    val br = if (ltr) cs.bottomEnd.toPx(s, this) else cs.bottomStart.toPx(s, this)
    val bl = if (ltr) cs.bottomStart.toPx(s, this) else cs.bottomEnd.toPx(s, this)
    return floatArrayOf(
        tl.fastCoerceAtMost(max), tr.fastCoerceAtMost(max),
        br.fastCoerceAtMost(max), bl.fastCoerceAtMost(max),
    )
}

fun BackdropEffectScope.vibrancy() {
    colorControls(brightness = 0f, contrast = 1f, saturation = 1.5f)
}

private const val SDF = """
float radiusAt(float2 c, float4 r) {
  if (c.x >= 0.0) { if (c.y <= 0.0) return r.y; else return r.z; }
  else { if (c.y <= 0.0) return r.x; else return r.w; }
}
float sdRoundedRect(float2 c, float2 h, float radius) {
  float2 cc = abs(c) - (h - float2(radius));
  return length(max(cc, 0.0)) - radius + min(max(cc.x, cc.y), 0.0);
}
float2 gradSdRoundedRect(float2 c, float2 h, float r) {
  float2 cc = abs(c) - (h - float2(r));
  if (cc.x >= 0.0 || cc.y >= 0.0) return sign(c) * normalize(max(cc, 0.0));
  float g = step(cc.y, cc.x); return sign(c) * float2(g, 1.0 - g);
}
"""

private const val LENS_SHADER = """
uniform shader content;
uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;
$SDF
float cm(float x){return 1.0-sqrt(1.0-x*x);}
half4 main(float2 coord){
  float2 hs = size*0.5; float2 cc = (coord+offset)-hs;
  float r = radiusAt(cc, cornerRadii);
  float sd = sdRoundedRect(cc, hs, r);
  if (-sd >= refractionHeight) return content.eval(coord);
  sd = min(sd, 0.0);
  float d = cm(1.0 - -sd/refractionHeight) * refractionAmount;
  float gr = min(r*1.5, min(hs.x, hs.y));
  float2 g = normalize(gradSdRoundedRect(cc, hs, gr) + depthEffect*normalize(cc));
  return content.eval(coord + d*g);
}
"""

private const val LENS_SHADER_DISPERSION = """
uniform shader content;
uniform float2 size; uniform float2 offset; uniform float4 cornerRadii;
uniform float refractionHeight; uniform float refractionAmount; uniform float depthEffect;
uniform float chromaticAberration;
$SDF
float cm(float x){return 1.0-sqrt(1.0-x*x);}
half4 main(float2 coord){
  float2 hs = size*0.5; float2 cc = (coord+offset)-hs;
  float r = radiusAt(cc, cornerRadii);
  float sd = sdRoundedRect(cc, hs, r);
  if (-sd >= refractionHeight) return content.eval(coord);
  sd = min(sd, 0.0);
  float d = cm(1.0 - -sd/refractionHeight) * refractionAmount;
  float gr = min(r*1.5, min(hs.x, hs.y));
  float2 g = normalize(gradSdRoundedRect(cc, hs, gr) + depthEffect*normalize(cc));
  float2 rc = coord + d*g;
  float di = chromaticAberration * ((cc.x*cc.y)/(hs.x*hs.y));
  float2 dc = d*g*di;
  half4 col = half4(0.0);
  half4 a = content.eval(rc+dc); col.r+=a.r/3.5; col.a+=a.a/7.0;
  half4 b = content.eval(rc+dc*(2.0/3.0)); col.r+=b.r/3.5; col.g+=b.g/7.0; col.a+=b.a/7.0;
  half4 c = content.eval(rc+dc*(1.0/3.0)); col.r+=c.r/3.5; col.g+=c.g/3.5; col.a+=c.a/7.0;
  half4 e = content.eval(rc); col.g+=e.g/3.5; col.a+=e.a/7.0;
  half4 f = content.eval(rc-dc*(1.0/3.0)); col.g+=f.g/3.5; col.b+=f.b/3.0; col.a+=f.a/7.0;
  half4 h = content.eval(rc-dc*(2.0/3.0)); col.b+=h.b/3.0; col.a+=h.a/7.0;
  half4 i = content.eval(rc-dc); col.r+=i.r/7.0; col.b+=i.b/3.0; col.a+=i.a/7.0;
  return col;
}
"""
