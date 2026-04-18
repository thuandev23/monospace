package com.monospace.app.feature.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.monospace.app.R
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.WallpaperAlignment
import com.monospace.app.core.domain.model.WallpaperConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
}
