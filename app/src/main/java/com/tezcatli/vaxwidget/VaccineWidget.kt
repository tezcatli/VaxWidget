package com.tezcatli.vaxwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log


class VaccineWidget : AppWidgetProvider() {

    //lateinit var httpFetcher : HttpFetcher


    companion object {
        val DISPLAY_DATA = "com.tezcatli.vaxwidget.DISPLAY_DATA"
        val NEXT_SLIDE_PLEASE = "com.tezcatli.vaxwidget.NEXT_SLIDE_PLEASE"
    }

    //var application : Application = Application()


    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        Log.e("WIDGET", "onDeleted")

        if (context != null && appWidgetIds != null) {

            for (appWidgetId in appWidgetIds) {
                (context.applicationContext as VaxApplication).serviceLocator.vaxWidgetController.deleteWidget(appWidgetId)
            }
        }

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

        updateAppWidget(context, appWidgetId)

        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
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

        (context.applicationContext as VaxApplication).serviceLocator.vaxWidgetController.update(appWidgetId)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.e("WIDGET ", "Received intent: " + intent.toString())

        if (context != null && intent != null) {
            (context.applicationContext as VaxApplication).serviceLocator.vaxWidgetController.handleIntent(intent)
        }

        super.onReceive(context, intent)
    }

}