package com.tezcatli.vaxwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit


class VaxWidgetController(val context: Context) {

    class ControllerWorker(val appContext: Context, workerParams: WorkerParameters) :
        Worker(appContext, workerParams) {

        override fun doWork(): Result {
            // We have received work to do.  The system or framework is already
            // holding a wake lock for us at this point, so we can just go.
            Log.e("DATASERVICE", "Executing work")

            //return Result.success()

            val vaxTypeName = inputData.getString("VaxType")

            if (vaxTypeName != null) {

                val vaxType = VaxChart.Type.valueOf(vaxTypeName)

                //val vaxChart = VaxChart.build(VaxChart.Type.valueOf(vaxDataName))
                val vaxChart = VaxChart.build(appContext, vaxType)

                if (vaxChart != null) {

                    //httpFetcher.requestGet(URL("https://www.google.fr/"))

                    vaxChart.fetch()

                    val broadcastIntent = Intent(applicationContext, VaccineWidget::class.java)
                    Log.e("DATASERVICE", "Sending intent to " + VaccineWidget::class.java)

                    broadcastIntent.setAction(VaccineWidget.DISPLAY_DATA)

                    broadcastIntent.putExtra("VaxType", vaxType.name)
                    broadcastIntent.putExtra("appWidgetId", inputData.getInt("appWidgetId", 0))
                    broadcastIntent.putExtra("VaxData", vaxChart.serialize())

                    applicationContext.sendBroadcast(broadcastIntent)
                    Log.e("DATASERVICE", "Intent sent")
                }
            }

            return Result.success()

        }
    }

    data class WidgetState(
        val configuration: ConfigurationManager.ConfigurationEntry,
        var chartCurrentIdx: Int = 0
    )

    val widgets = mutableMapOf<Int, WidgetState>()

    /**
     * Unique job ID for this service.
     */


    fun update(appWidgetId: Int) {

        val newWidget = !widgets.containsKey(appWidgetId)

        addWidget(appWidgetId)

        val state = widgets.get(appWidgetId)

        if (state != null) {

            if (newWidget)
                nextSlidePlease(appWidgetId)
            else {
                Log.e(
                    "VaxWidgetController",
                    "Enqueuing ${state.configuration.charts.get(state.chartCurrentIdx)}"
                )

                val uploadWorkRequest: WorkRequest =
                    OneTimeWorkRequestBuilder<ControllerWorker>().setInputData(
                        workDataOf(
                            "appWidgetId" to appWidgetId,
                            "VaxType" to state.configuration.charts.get(state.chartCurrentIdx)
                        )
                    ).build()

                WorkManager.getInstance(context).enqueue(uploadWorkRequest)
            }

        }
    }


    fun paint(appWidgetId: Int, type: VaxChart.Type, intent: Intent) {

        val vaxChart = VaxChart.build(context, type)!!
        vaxChart.deserialize(intent, "VaxData")
        vaxChart.paint(context, appWidgetId)
    }

    fun nextSlidePlease(appWidgetId: Int) {

        Log.e("VaxWidgetController", "nextSlidePlease ${appWidgetId}")

        update(appWidgetId)

        val state = widgets.get(appWidgetId)
        if (state != null) {
            Log.e("VaxWidgetController", "got state")


            val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            manager.setExact(
                AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() +
                state.configuration.period * 1000L,
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId, Intent(context, VaccineWidget::class.java).apply {
                        action = VaccineWidget.NEXT_SLIDE_PLEASE
                        putExtra("appWidgetId", appWidgetId)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

            state.chartCurrentIdx =
                (state.chartCurrentIdx + 1) % state.configuration.charts.size

        }
    }

    fun addWidget(appWidgetId: Int) {
        if (!widgets.containsKey(appWidgetId)) {

            val configuration =
                (context.applicationContext as VaxApplication).serviceLocator.configurationManager.getEntry(
                    appWidgetId
                )
            if (configuration != null)
                widgets.put(appWidgetId, WidgetState(configuration))

        }
    }

    fun deleteWidget(appWidgetId: Int) {
        if (!widgets.containsKey(appWidgetId)) {
            widgets.remove(appWidgetId)
        }
    }


    fun handleIntent(intent: Intent) {
        val extras = intent.extras

        if (extras != null) {
            if (extras.containsKey("appWidgetId")) {

                val appWidgetId = intent.getIntExtra("appWidgetId", 0)

                when (intent.action) {
                    VaccineWidget.DISPLAY_DATA -> {
                        val vaxTypeName = intent.getStringExtra("VaxType")
                        //val bundle = intent.getBundleExtra("VaxData")

                        if (vaxTypeName != null) {
                            if (intent.component != null && intent.component!!.className == VaccineWidget::class.java.name) {
                                (context.applicationContext as VaxApplication).serviceLocator.vaxWidgetController.paint(
                                    appWidgetId,
                                    VaxChart.Type.valueOf(vaxTypeName),
                                    intent
                                )
                            }
                        }
                    }
                    VaccineWidget.NEXT_SLIDE_PLEASE -> {
                        if (intent.component != null && intent.component!!.className == VaccineWidget::class.java.name) {
                            (context.applicationContext as VaxApplication).serviceLocator.vaxWidgetController.nextSlidePlease(
                                appWidgetId
                            )
                        }
                    }
                    else -> {

                    }
                }
            }
        }
    }

    init {
        val save = OneTimeWorkRequestBuilder<ControllerWorker>()
            .setInitialDelay(10000, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(context).enqueue(save)
    }

}

