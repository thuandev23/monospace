package com.monospace.app.feature.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.monospace.app.R
import com.monospace.app.core.domain.model.AppShortcut
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.WallpaperAlignment
import com.monospace.app.core.domain.model.WallpaperConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TaskHit(val topY: Float, val bottomY: Float)
data class ShortcutHit(val rect: RectF, val packageName: String)
data class HitMap(val taskHits: List<TaskHit>, val shortcutHits: List<ShortcutHit>)

object WallpaperRenderer {

    fun render(
        context: Context,
        config: WallpaperConfig,
        tasks: List<Task>,
        width: Int,
        height: Int,
        now: LocalDateTime = LocalDateTime.now()
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val scaledDensity = metrics.scaledDensity

        val bgColor = parseColor(config.backgroundColorHex, android.graphics.Color.BLACK)
        val textColor = parseColor(config.textColorHex, android.graphics.Color.WHITE)
        val typeface = ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.DEFAULT

        canvas.drawColor(bgColor)

        val paddingH = 28f * density
        val paddingTop = 64f * density
        val paddingBottom = 80f * density
        val maxTextWidth = width - paddingH * 2

        val timeSizePx = 56f * scaledDensity
        val dateSizePx = 15f * scaledDensity
        val taskSizePx = 14f * scaledDensity
        val taskSpacerPx = 28f * density

        val limitedTasks = tasks.take(config.taskLimit)

        // Measure total content height for vertical alignment
        val contentHeight = measureContentHeight(
            config, limitedTasks,
            timeSizePx, dateSizePx, taskSizePx, taskSpacerPx, typeface
        )

        var currentY = when (config.contentAlignment) {
            WallpaperAlignment.TOP -> paddingTop
            WallpaperAlignment.BOTTOM -> height - paddingBottom - contentHeight
            WallpaperAlignment.CENTER -> (height - contentHeight) / 2f
        }

        if (config.showTime) {
            val paint = textPaint(typeface, timeSizePx, textColor, alpha = 255)
            val fm = paint.fontMetrics
            currentY -= fm.ascent
            canvas.drawText(
                DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(now),
                paddingH, currentY, paint
            )
            currentY += fm.descent + timeSizePx * 0.05f
        }

        if (config.showDate) {
            val paint = textPaint(typeface, dateSizePx, textColor, alpha = (255 * 0.65f).toInt())
            val fm = paint.fontMetrics
            currentY -= fm.ascent
            canvas.drawText(
                DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()).format(now),
                paddingH, currentY, paint
            )
            currentY += fm.descent
        }

        if (config.showTasks && limitedTasks.isNotEmpty()) {
            if (config.showTime || config.showDate) currentY += taskSpacerPx
            val paint = textPaint(typeface, taskSizePx, textColor, alpha = (255 * 0.80f).toInt())
            val textPaintForEllipsize = TextPaint(paint)
            val fm = paint.fontMetrics
            limitedTasks.forEach { task ->
                currentY -= fm.ascent
                val label = TextUtils.ellipsize(
                    "— ${task.title}", textPaintForEllipsize, maxTextWidth, TextUtils.TruncateAt.END
                ).toString()
                canvas.drawText(label, paddingH, currentY, paint)
                currentY += fm.descent + taskSizePx * 0.4f
            }
        }

        return bitmap
    }

    private fun measureContentHeight(
        config: WallpaperConfig,
        tasks: List<Task>,
        timeSizePx: Float,
        dateSizePx: Float,
        taskSizePx: Float,
        taskSpacerPx: Float,
        typeface: Typeface
    ): Float {
        var height = 0f
        if (config.showTime) {
            val fm = textPaint(typeface, timeSizePx, 0, 255).fontMetrics
            height += (-fm.ascent + fm.descent) + timeSizePx * 0.05f
        }
        if (config.showDate) {
            val fm = textPaint(typeface, dateSizePx, 0, 255).fontMetrics
            height += (-fm.ascent + fm.descent)
        }
        if (config.showTasks && tasks.isNotEmpty()) {
            if (config.showTime || config.showDate) height += taskSpacerPx
            val fm = textPaint(typeface, taskSizePx, 0, 255).fontMetrics
            val rowHeight = (-fm.ascent + fm.descent) + taskSizePx * 0.4f
            height += rowHeight * tasks.size
        }
        return height
    }

    private fun textPaint(typeface: Typeface, sizePx: Float, color: Int, alpha: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = sizePx
            this.color = color
            this.alpha = alpha
        }

    private fun parseColor(hex: String, fallback: Int): Int =
        runCatching { android.graphics.Color.parseColor(hex) }.getOrDefault(fallback)

    fun renderToCanvas(
        canvas: Canvas,
        context: Context,
        config: WallpaperConfig,
        tasks: List<Task>,
        shortcuts: List<AppShortcut>,
        iconCache: Map<String, Bitmap>,
        width: Int,
        height: Int,
        now: LocalDateTime = LocalDateTime.now()
    ): HitMap {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val scaledDensity = metrics.scaledDensity

        val bgColor = parseColor(config.backgroundColorHex, android.graphics.Color.BLACK)
        val textColor = parseColor(config.textColorHex, android.graphics.Color.WHITE)
        val typeface = ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.DEFAULT

        canvas.drawColor(bgColor)

        val paddingH = 28f * density
        val paddingTop = 64f * density
        val paddingBottom = 80f * density
        val maxTextWidth = width - paddingH * 2

        val timeSizePx = 56f * scaledDensity
        val dateSizePx = 15f * scaledDensity
        val taskSizePx = 14f * scaledDensity
        val taskSpacerPx = 28f * density

        val iconSizePx = (44f * density).toInt()
        val labelSizePx = 11f * scaledDensity
        val shortcutRowHeight =
            if (shortcuts.isNotEmpty()) iconSizePx + labelSizePx * 2.5f + 8f * density else 0f

        val limitedTasks = tasks.take(config.taskLimit)

        val contentHeight = measureContentHeight(
            config, limitedTasks,
            timeSizePx, dateSizePx, taskSizePx, taskSpacerPx, typeface
        )

        var currentY = when (config.contentAlignment) {
            WallpaperAlignment.TOP -> paddingTop
            WallpaperAlignment.BOTTOM -> height - paddingBottom - shortcutRowHeight - 24f * density - contentHeight
            WallpaperAlignment.CENTER -> (height - contentHeight - shortcutRowHeight - 24f * density) / 2f
        }

        if (config.showTime) {
            val paint = textPaint(typeface, timeSizePx, textColor, alpha = 255)
            val fm = paint.fontMetrics
            currentY -= fm.ascent
            canvas.drawText(
                DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(now),
                paddingH, currentY, paint
            )
            currentY += fm.descent + timeSizePx * 0.05f
        }

        if (config.showDate) {
            val paint = textPaint(typeface, dateSizePx, textColor, alpha = (255 * 0.65f).toInt())
            val fm = paint.fontMetrics
            currentY -= fm.ascent
            canvas.drawText(
                DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()).format(now),
                paddingH, currentY, paint
            )
            currentY += fm.descent
        }

        val taskHits = mutableListOf<TaskHit>()
        if (config.showTasks && limitedTasks.isNotEmpty()) {
            if (config.showTime || config.showDate) currentY += taskSpacerPx
            val paint = textPaint(typeface, taskSizePx, textColor, alpha = (255 * 0.80f).toInt())
            val textPaintForEllipsize = TextPaint(paint)
            val fm = paint.fontMetrics
            limitedTasks.forEach { task ->
                val topY = currentY + fm.ascent
                currentY -= fm.ascent
                val label = TextUtils.ellipsize(
                    "— ${task.title}", textPaintForEllipsize, maxTextWidth, TextUtils.TruncateAt.END
                ).toString()
                canvas.drawText(label, paddingH, currentY, paint)
                currentY += fm.descent + taskSizePx * 0.4f
                taskHits += TaskHit(topY, currentY)
            }
        }

        val shortcutHits = mutableListOf<ShortcutHit>()
        if (shortcuts.isNotEmpty()) {
            val rowTop = height - paddingBottom - shortcutRowHeight
            val slotWidth = width.toFloat() / shortcuts.size.coerceAtLeast(1)
            val labelPaint =
                textPaint(typeface, labelSizePx, textColor, alpha = (255 * 0.70f).toInt())
            val labelFm = labelPaint.fontMetrics
            val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(60, 255, 255, 255)
            }

            shortcuts.forEachIndexed { index, shortcut ->
                val slotLeft = index * slotWidth
                val slotCenter = slotLeft + slotWidth / 2f
                val iconLeft = slotCenter - iconSizePx / 2f
                val iconTop = rowTop
                val iconRight = iconLeft + iconSizePx
                val iconBottom = iconTop + iconSizePx

                val icon = iconCache[shortcut.packageName]
                if (icon != null) {
                    val src = android.graphics.Rect(0, 0, icon.width, icon.height)
                    val dst = RectF(iconLeft, iconTop, iconRight, iconBottom)
                    canvas.drawBitmap(icon, src, dst, null)
                } else {
                    canvas.drawRoundRect(
                        RectF(iconLeft, iconTop, iconRight, iconBottom),
                        8f * density, 8f * density,
                        placeholderPaint
                    )
                }

                val labelY = iconBottom + (-labelFm.ascent) + 4f * density
                val labelText = TextUtils.ellipsize(
                    shortcut.label, TextPaint(labelPaint),
                    slotWidth - 8f * density, TextUtils.TruncateAt.END
                ).toString()
                val labelX = slotCenter - labelPaint.measureText(labelText) / 2f
                canvas.drawText(labelText, labelX, labelY, labelPaint)

                val hitBottom = labelY + labelFm.descent
                shortcutHits += ShortcutHit(
                    RectF(slotLeft, iconTop, slotLeft + slotWidth, hitBottom),
                    shortcut.packageName
                )
            }
        }

        return HitMap(taskHits, shortcutHits)
    }

    fun rasterizeIcon(context: Context, packageName: String): Bitmap? = runCatching {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        if (drawable is BitmapDrawable) return drawable.bitmap
        val size = (48f * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(c)
        } else {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(c)
        }
        bitmap
    }.getOrNull()
}
