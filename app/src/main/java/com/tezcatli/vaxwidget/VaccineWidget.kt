package com.tezcatli.vaxwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.ZoneId
import java.util.*


class VaccineWidget : AppWidgetProvider() {


    companion object {
        val DISPLAY_DATA: String = "com.tezcatli.vaxwidget.DISPLAY_DATA"
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        Log.e("WIDGET", "onDeleted")
        super.onDisabled(context)
    }

    override fun onDisabled(context: Context?) {
        Log.e("WIDGET", "onDisabled")
        super.onDisabled(context)
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)

        Log.e("WIDGET", "onEnabled")

    }

    override fun onRestored(context: Context?, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        Log.e("WIDGET", "onRestored")

        super.onRestored(context, oldWidgetIds, newWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager,
        appWidgetId: Int, newOptions: Bundle
    ) {
        Log.e("WIDGET", "onAppWidgetOptionsChanged")
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.e("WIDGET", "onUpdate")

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetId)
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun updateAppWidget(
        context: Context, appWidgetId: Int
    ) {
        Log.e("VaccineWidget", "updateWidget " + appWidgetId)
        VaxWidgetController.fetch(context, VaxChart.Type.DailyJabs, appWidgetId)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.e("WIDGET ", "Received intent: " + intent.toString())

        if (intent == null || context == null) {
            super.onReceive(context, intent)
            return
        }

        when (intent.action) {
            DISPLAY_DATA -> {
                if (intent.component != null && intent.component!!.className == VaccineWidget::class.java.name) {
                    VaxWidgetController.paint(context, intent)
                }
            }
            else -> {

            }
        }
        super.onReceive(context, intent)
    }

}