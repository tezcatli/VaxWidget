package com.tezcatli.vaxwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService


class VaxWidgetController : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        // We have received work to do.  The system or framework is already
        // holding a wake lock for us at this point, so we can just go.
        Log.e("DATASERVICE", "Executing work: $intent")

        //assert(intent.extras!!.containsKey("vaxDataName") == true)
        //assert(intent.extras!!.containsKey("appWidgetId") == true)

        val vaxDataName = intent.getStringExtra("VaxType")

        if (vaxDataName != null) {

            //val vaxChart = VaxChart.build(VaxChart.Type.valueOf(vaxDataName))
            val vaxChart = VaxChart.build(VaxChart.Type.valueOf(vaxDataName))


            if (vaxChart != null) {

                vaxChart.fetch()

                val broadcastIntent = Intent(this, VaccineWidget::class.java)
                Log.e("DATASERVICE", "Sending intent to " + VaccineWidget::class.java)

                broadcastIntent.setAction(VaccineWidget.DISPLAY_DATA)


                broadcastIntent.putExtra("VaxType", vaxChart.type.name)
                broadcastIntent.putExtra("VaxData", vaxChart.serialize())
                broadcastIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0))
                //broadcastIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent)
                sendBroadcast(broadcastIntent)
                Log.e("DATASERVICE", "Intent sent")

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    companion object {
        /**
         * Unique job ID for this service.
         */
        const val JOB_ID = 1000


        fun fetch(context: Context, type: VaxChart.Type, appWidgetId: Int) {

            Log.e("VaxWidgetController","Enqueuing")

            val intent = Intent(context, VaxWidgetController::class.java)
            // potentially add data to the intent
            intent.putExtra("appWidgetId", appWidgetId)
            intent.putExtra("VaxType", type.name)

            enqueueWork(
                context,
                VaxWidgetController::class.java, JOB_ID, intent
            )
        }


        fun paint(context: Context, intent: Intent) {

            val vaxDataName = intent.getStringExtra("VaxType")!!
            val appWidgetId = intent.getIntExtra("appWidgetId", 0)

            val vaxChart = VaxChart.build(VaxChart.Type.valueOf(vaxDataName))!!
            vaxChart.deserialize(intent, "VaxData")
            vaxChart.paint(context, appWidgetId)
        }

    }
}

