package com.monospace.app.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ClockDateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ClockDateWidget()
}
