// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import helium314.keyboard.keyboard.Key
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Colors
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.common.isEmoji

/**
 * Authentic 3D Mechanical Keycap Renderer (MT3 / SA / OEM Profile).
 * Features:
 * 1. Deep concave spherical scooped dish (MT3 Susuwatari profile)
 * 2. Tapered 4-sided 3D keycap bevel skirt with realistic shadows
 * 3. Diagonal corner chamfers connecting outer housing to inner scooped dish
 * 4. Generous emoji grid spacing (no collisions or cramped emoji keys)
 * 5. Optional RGB Underglow mode with rainbow hue spectrum
 * 6. Tactile homing bars on F and J keys
 * 7. Real-time physical switch plunge on press
 *
 * [drawKeycapShape] is shared by the real keyboard and the Mech Studio preview, so both look identical.
 */
object MechanicalKeycapRenderer {

    const val PREF_MECH_3D_KEYCAPS = "pref_mech_3d_keycaps"
    const val PREF_MECH_ALWAYS_3D = "pref_mech_always_3d"
    const val PREF_MECH_RGB_ENABLED = "pref_mech_rgb_enabled"

    private val socketPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rgbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val skirtPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val topDishPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dishBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val chamferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val homingBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val rectSocket = RectF()
    private val rectSkirt = RectF()
    private val rectDish = RectF()
    private val rectHighlight = RectF()
    private val rectHoming = RectF()

    fun shouldDrawMechanicalKey(context: Context): Boolean {
        val prefs = context.prefs()
        return prefs.getBoolean(PREF_MECH_3D_KEYCAPS, true)
    }

    /**
     * Vertical offset for the key label/icon so it sits centered on the raised 3D top dish
     * when resting, and plunges down with the keycap when pressed.
     * At rest, 0f avoids pushing icons/labels beyond the key boundaries.
     */
    fun getLabelVerticalOffsetPx(key: Key, density: Float): Float {
        return if (key.isPressed) {
            1.2f * density
        } else {
            0f
        }
    }

    fun draw3DKeycap(key: Key, canvas: Canvas, colors: Colors, context: Context) {
        val width = key.getDrawWidth().toFloat()
        val height = key.getHeight().toFloat()
        if (width <= 0f || height <= 0f) return

        val density = context.resources.displayMetrics.density
        val code = key.getCode()
        val isSpacebar = code == Constants.CODE_SPACE

        // Detect emoji or grid key to apply clean padding/spacing
        val isEmoji = (key.label?.let { isEmoji(it) } == true) ||
                      (key.outputText?.let { isEmoji(it) } == true) ||
                      (width < 46f * density && height < 50f * density && !isSpacebar)

        // Base color by key role (Alphas, Modifiers, Artisan Accent, Spacebar)
        val rawBaseColor = when {
            key.hasActionKeyBackground() -> colors.get(ColorType.ACTION_KEY_BACKGROUND)
            isSpacebar -> colors.get(ColorType.SPACE_BAR_BACKGROUND)
            key.hasFunctionalBackground() -> colors.get(ColorType.FUNCTIONAL_KEY_BACKGROUND)
            else -> colors.get(ColorType.KEY_BACKGROUND)
        }

        val prefs = context.prefs()
        val rgbHue = if (prefs.getBoolean(PREF_MECH_RGB_ENABLED, false)) {
            val screenW = context.resources.displayMetrics.widthPixels.coerceAtLeast(1).toFloat()
            ((key.x.toFloat() / screenW) * 360f) % 360f
        } else null

        val lowerCode = Character.toLowerCase(code)
        val hasHomingBar = lowerCode == 'f'.code || lowerCode == 'j'.code

        drawKeycapShape(
            canvas, width, height, rawBaseColor, density,
            key.isPressed, isSpacebar, isEmoji, hasHomingBar, rgbHue
        )
    }

    /**
     * Draws a complete 3D keycap into the rectangle (0, 0, [width], [height]) of [canvas].
     * Everything stays strictly inside that rectangle, whatever the key size or gap setting.
     * @param rgbHue if not null, an RGB underglow ring with this hue (0..360) is drawn
     */
    @JvmStatic
    fun drawKeycapShape(
        canvas: Canvas, width: Float, height: Float, rawBaseColor: Int, density: Float,
        isPressed: Boolean, isSpacebar: Boolean, isEmoji: Boolean, hasHomingBar: Boolean, rgbHue: Float?
    ) {
        if (width <= 0f || height <= 0f) return
        val minSide = minOf(width, height)
        // scale all details down for very small keys (large key gaps) so nothing overlaps or leaves the key
        val detail = (minSide / (40f * density)).coerceIn(0.55f, 1f)

        val hGap = when {
            isEmoji -> 2.2f * density
            isSpacebar -> 0.8f * density
            else -> 0.4f * density
        } * detail
        val vGap = (if (isEmoji) 2.2f * density else 0.6f * density) * detail

        val left = hGap
        val right = width - hGap
        val top = vGap
        val bottom = height - vGap - 1.0f * density * detail // room for the socket shadow below the key
        if (right <= left || bottom <= top) return

        val outerRadius = (if (isEmoji) 3.6f else 4.2f) * density * detail
        val innerRadius = (if (isEmoji) 2.8f else 3.4f) * density * detail

        // Ensure opaque keycap body
        val baseColor = rawBaseColor or -0x1000000
        val isDarkKeycap = ColorUtils.calculateLuminance(baseColor) < 0.28

        // 1. Switch Plate Socket Cutout & Drop Shadow (with optional RGB Underglow)
        if (rgbHue != null) {
            val rgbColor = Color.HSVToColor(floatArrayOf(rgbHue, 0.85f, 1.0f))
            rgbPaint.color = ColorUtils.setAlphaComponent(rgbColor, 190)
            rgbPaint.strokeWidth = 1.2f * density * detail
            val grow = 0.3f * density
            rectSocket.set(
                (left - grow).coerceAtLeast(0f),
                (top - grow).coerceAtLeast(0f),
                (right + grow).coerceAtMost(width),
                (bottom + 1.0f * density * detail).coerceAtMost(height)
            )
            canvas.drawRoundRect(rectSocket, outerRadius + 0.6f * density, outerRadius + 0.6f * density, rgbPaint)
        } else {
            socketPaint.color = 0x38000000
            rectSocket.set(
                left,
                top + 0.4f * density * detail,
                right,
                (bottom + 1.0f * density * detail).coerceAtMost(height)
            )
            canvas.drawRoundRect(rectSocket, outerRadius + 0.4f * density, outerRadius + 0.4f * density, socketPaint)
        }

        // 2. Tapered 3D Bevel / Skirt Geometry
        val pressDrop = if (isPressed) 1.8f * density * detail else 0f
        val skirtTop = top + pressDrop
        val skirtBottom = bottom

        val sideBevelColor = ColorUtils.blendARGB(baseColor, Color.BLACK, if (isDarkKeycap) 0.22f else 0.16f)
        val bottomSkirtColor = ColorUtils.blendARGB(baseColor, Color.BLACK, if (isDarkKeycap) 0.46f else 0.32f)

        skirtPaint.shader = LinearGradient(
            0f, skirtTop, 0f, skirtBottom,
            sideBevelColor, bottomSkirtColor,
            Shader.TileMode.CLAMP
        )
        rectSkirt.set(left, skirtTop, right, skirtBottom)
        canvas.drawRoundRect(rectSkirt, outerRadius, outerRadius, skirtPaint)
        skirtPaint.shader = null

        // 3. MT3 / SA Profile Spherical Inset Top Dish Dimensions
        val sideInset = (if (isPressed) 0.8f else 1.2f) * density * detail
        val topInset = (if (isPressed) 0.6f else 0.8f) * density * detail
        val bottomLipDepth = (if (isPressed) 1.0f else 2.2f) * density * detail

        val dishLeft = left + sideInset
        val dishRight = (right - sideInset).coerceAtLeast(dishLeft + 1f)
        val dishTop = skirtTop + topInset
        val dishBottom = (skirtBottom - bottomLipDepth).coerceAtLeast(dishTop + 3f * density * detail)

        // 4. Diagonal Corner Chamfer Ridges
        if (!isPressed && !isEmoji) {
            chamferPaint.strokeWidth = 1.0f * density * detail
            val insetCornerOuter = outerRadius * 0.45f
            val insetCornerInner = innerRadius * 0.45f

            // Top-left & Top-right chamfers (catch light)
            chamferPaint.color = if (isDarkKeycap) 0x26FFFFFF else 0x45FFFFFF
            canvas.drawLine(
                left + insetCornerOuter, skirtTop + insetCornerOuter,
                dishLeft + insetCornerInner, dishTop + insetCornerInner,
                chamferPaint
            )
            canvas.drawLine(
                right - insetCornerOuter, skirtTop + insetCornerOuter,
                dishRight - insetCornerInner, dishTop + insetCornerInner,
                chamferPaint
            )

            // Bottom-left & Bottom-right chamfers (bevel ridge)
            chamferPaint.color = 0x38000000
            canvas.drawLine(
                left + insetCornerOuter, skirtBottom - insetCornerOuter,
                dishLeft + insetCornerInner, dishBottom - insetCornerInner,
                chamferPaint
            )
            canvas.drawLine(
                right - insetCornerOuter, skirtBottom - insetCornerOuter,
                dishRight - insetCornerInner, dishBottom - insetCornerInner,
                chamferPaint
            )
        }

        // 5. Authentic MT3 Deep Spherical Concave Scoop (Bowl Gradient)
        val activeColor = if (isPressed) {
            ColorUtils.blendARGB(baseColor, Color.BLACK, 0.08f)
        } else {
            baseColor
        }

        rectDish.set(dishLeft, dishTop, dishRight, dishBottom)

        if (isSpacebar) {
            // Convex rounded spacebar saddle
            val dishTopColor = ColorUtils.blendARGB(activeColor, Color.WHITE, if (isDarkKeycap) 0.10f else 0.16f)
            val dishBottomColor = ColorUtils.blendARGB(activeColor, Color.BLACK, 0.06f)
            topDishPaint.shader = LinearGradient(
                0f, dishTop, 0f, dishBottom,
                dishTopColor, dishBottomColor,
                Shader.TileMode.CLAMP
            )
        } else {
            // MT3 spherical bowl depression
            val dishCenterX = (dishLeft + dishRight) * 0.5f
            val dishCenterY = (dishTop + dishBottom) * 0.46f
            val dishRadius = ((dishRight - dishLeft) * 0.65f).coerceAtLeast(1f)

            val centerScoopColor = ColorUtils.blendARGB(activeColor, Color.BLACK, if (isDarkKeycap) 0.15f else 0.08f)
            val rimHighlightColor = ColorUtils.blendARGB(activeColor, Color.WHITE, if (isDarkKeycap) 0.14f else 0.20f)

            topDishPaint.shader = RadialGradient(
                dishCenterX, dishCenterY, dishRadius,
                intArrayOf(centerScoopColor, activeColor, rimHighlightColor),
                floatArrayOf(0.0f, 0.60f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }

        canvas.drawRoundRect(rectDish, innerRadius, innerRadius, topDishPaint)
        topDishPaint.shader = null

        // 6. Crisp Top-Dish Rim Border & Specular Edge Highlight
        dishBorderPaint.strokeWidth = 0.9f * density * detail
        dishBorderPaint.color = if (isDarkKeycap) 0x2AFFFFFF else 0x55FFFFFF
        canvas.drawRoundRect(rectDish, innerRadius, innerRadius, dishBorderPaint)

        // Top specular sheen bar
        if (dishRight - dishLeft > 8f * density) {
            highlightPaint.color = if (isDarkKeycap) 0x20FFFFFF else 0x48FFFFFF
            rectHighlight.set(
                dishLeft + 2.5f * density * detail,
                dishTop + 0.6f * density * detail,
                dishRight - 2.5f * density * detail,
                dishTop + 1.8f * density * detail
            )
            canvas.drawRoundRect(rectHighlight, 1f * density, 1f * density, highlightPaint)
        }

        // 7. Tactile Homing Bar on F and J keys
        if (hasHomingBar) {
            val barWidth = (dishRight - dishLeft) * 0.34f
            val barCenterX = (dishLeft + dishRight) * 0.5f
            val barBottom = dishBottom - 2.2f * density * detail
            val barTop = barBottom - 1.5f * density * detail
            homingBarPaint.color = if (isDarkKeycap) 0x55FFFFFF else 0x45000000
            rectHoming.set(barCenterX - barWidth * 0.5f, barTop, barCenterX + barWidth * 0.5f, barBottom)
            canvas.drawRoundRect(rectHoming, 1f * density, 1f * density, homingBarPaint)
        }
    }
}
