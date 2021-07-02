package com.tezcatli.vaxwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.TypedArray
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.util.TypedValue
import android.widget.RemoteViews


abstract class VaxChart  {


    enum class Type(val short: String, val long: String) {
        DailyJabs("Daily Jabs", "Nombre d'injections quotidiennes"),
        ImmunizationCoverage("Immunization Coverage", "Couverture d'immunisation")
    }

    abstract val type: Type

    abstract fun fetch()
    abstract fun serialize(): Parcelable


    abstract fun deserialize(intent : Intent, name : String)
    abstract fun paint2(appWidgetId : Int, width: Int, height : Int) : RemoteViews
    abstract fun isDataValid() : Boolean

    fun paint(context: Context, appWidgetId : Int) {
        val appWidgetManager = AppWidgetManager.getInstance(
            context.applicationContext
        )


        val orientation = context.resources.configuration.orientation
        val metrics = context.resources.displayMetrics

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

        val width : Int
        val height : Int

        if (orientation != Configuration.ORIENTATION_PORTRAIT) {
            width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).toFloat(), metrics).toInt()
            height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).toFloat(), metrics).toInt()
        } else {
            width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).toFloat(), metrics).toInt()
            height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).toFloat(), metrics).toInt()
        }


        Log.e(this.javaClass.name, "Width = ${width}, height ${height}")

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
    }

    companion object {
        fun build(context : Context, type: Type): VaxChart? {
            when (type) {
                Type.DailyJabs -> {
                    return VaxChartDailyJabs(context)
                }
                Type.ImmunizationCoverage -> {
                    return VaxChartImmunizationCoverage(context)
                }
                else -> {
                    Log.e("VaxWidget", "Unknown type: " + type.name)
                    return null
                }
            }
        }
    }
}