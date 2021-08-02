package com.tezcatli.vaxwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Parcelable
import android.util.Log
import android.util.TypedValue
import android.widget.RemoteViews


abstract class VaxChart {


    enum class Type(val short: String, val long: String) {
        DailyJabs("Daily Jabs", "Nombre d'injections quotidiennes"),
        DailyFullImmunization("Daily full immunization Coverage", "Evolution couverture vaccinale complète"),
        Immunization("Immunization", "Couverture vaccinale")
    }

    abstract val type: Type

    abstract fun fetch()
    abstract fun serialize(): Parcelable


    abstract fun deserialize(intent: Intent, name: String)
    abstract fun paint2(appWidgetId: Int, width: Int, height: Int): RemoteViews
    abstract fun isDataValid(): Boolean

    fun paint(context: Context, appWidgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(
            context.applicationContext
        )


        val orientation = context.resources.configuration.orientation
        val metrics = context.resources.displayMetrics

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

        val width: Int
        val height: Int

        if (orientation != Configuration.ORIENTATION_PORTRAIT) {
            width = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).toFloat(),
                metrics
            ).toInt()
            height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).toFloat(),
                metrics
            ).toInt()
        } else {
            width = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).toFloat(),
                metrics
            ).toInt()
            height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).toFloat(),
                metrics
            ).toInt()
        }

        Log.e(this.javaClass.name, "Width = ${width}, height ${height}")


        if (width != 0 && height != 0) {
            val views = paint2(appWidgetId, width, height)

            val intentUpdate = Intent(context, VaccineWidget::class.java)
            intentUpdate.action = VaccineWidget.NEXT_SLIDE_PLEASE
            intentUpdate.putExtra("appWidgetId", appWidgetId)
            val pendingUpdate = PendingIntent.getBroadcast(
                context, appWidgetId, intentUpdate,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.imageView, pendingUpdate)
            //views.setOnClickPendingIntent(R.id.textView, pendingUpdate)

            //val alarmIntent = Intent(context, VaccineWidget

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } else {
            // sometime width or height gets null ... try to refresh again
            context.sendBroadcast(Intent(context, VaccineWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))})
        }
    }

    companion object {
        fun build(context: Context, type: Type): VaxChart? {
            when (type) {
                Type.DailyJabs -> {
                    return VaxChartDailyJabs(context)
                }
                Type.DailyFullImmunization -> {
                    return VaxChartImmunizationCoverage(context)
                }
                Type.Immunization -> {
                    return VaxChartImmunization(context)
                }
                else -> {
                    Log.e("VaxWidget", "Unknown type: " + type.name)
                    return null
                }
            }
        }
    }
}