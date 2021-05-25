package com.tezcatli.vaxwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

class MyXAxisFormatter : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        ///val calendar = Calendar.getInstance()
        val time = Date(value.toLong() * 86400).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        return time.dayOfMonth.toString() + "/" + time.monthValue.toString() + "/" + (time.year - 2000).toString()

    }
}


class VaccineWidget : AppWidgetProvider() {

    companion object {
        public val DISPLAY_DATA: String = "com.tezcatli.vaxwidget.DISPLAY_DATA"
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

        //if (context != null) {
        //    LocalBroadcastManager.getInstance(context)
        //        .registerReceiver(this, IntentFilter(DISPLAY_DATA))
        //}

    }

    override fun onRestored(context: Context?, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        Log.e("WIDGET", "onRestored")

        super.onRestored(context, oldWidgetIds, newWidgetIds)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.e("WIDGET", "onUpdate --> " + appWidgetIds.toString())

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun updateAppWidget(
        context: Context, appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {

        Log.e("WIDGET", "updateWidget class = " + DataService::class.java.name)

        val i = Intent(context, DataService::class.java)
        // potentially add data to the intent
        i.putExtra("appWidgetId", appWidgetId)
        //context.startService(i)

        DataService.enqueueWork(context, i)

    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.e("WIDGET", "Received intent: " + intent.toString())
        //if (intent?.component != null) {
        //    Log.e("COMPONENT---->", intent!!.component.className)
        //}
        if (intent?.component != null && intent!!.component!!.className == VaccineWidget::class.java.name && intent!!.action == VaccineWidget.DISPLAY_DATA) {
        //if (intent?.component != null && intent!!.component!!.className == SimpleAppWidget::class.java.name) {

            Log.e("WIDGET", "Processing intent")

            assert(intent.extras!!.containsKey("appWidgetId") == true)

            val appWidgetManager = AppWidgetManager.getInstance(
                context!!.applicationContext
            )

            val thisWidget = ComponentName(
                context.applicationContext,
                VaccineWidget::class.java
            )

            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            //val idArray = intArrayOf(appWidgetId)

            val appWidgetId = intent.getIntExtra("appWidgetId", 0)
            val views = RemoteViews(context.packageName, R.layout.simple_app_widget)

            //val vaccineData : ArrayList<Int>? = intent.getIntegerArrayListExtra("VaccineData")

            val vaccineData: VaccineData? = intent.getParcelableExtra<VaccineData>("VaccineData")
            //val dataSets: List<ILineDataSet> = ArrayList()


            if (vaccineData != null) {
                //Log.e("YOUPI", msg)

                val chart = LineChart(context)
                val lineData = LineData()

                val colors = arrayOf<Int>(
                    R.color.black,
                    R.color.red,
                    R.color.brown,
                    R.color.green,
                    R.color.orange
                )

                for (vaccineIdx: Int in 1..VaccineData.vaccineLabel.size - 1) {

                    val entries: MutableList<Entry> = ArrayList<Entry>()

                    //var entry : String;
                    for (vaccineDataEntry in vaccineData.data) {
                        entries.add(
                            Entry(
                                (vaccineDataEntry.date / 86400).toFloat(),
                                vaccineDataEntry.jabs[vaccineIdx].toFloat()
                            )
                        )
                    }

                    val dataSet =
                        LineDataSet(entries.toList(), VaccineData.vaccineLabel[vaccineIdx])
                    dataSet.setColors(intArrayOf(colors[vaccineIdx]), context)
                    dataSet.setDrawCircles(false)

                    lineData.addDataSet(dataSet)
                }


                chart.data = lineData
                chart.axisRight.isEnabled = false
                chart.axisLeft.valueFormatter = LargeValueFormatter()
                chart.xAxis.setLabelCount(6, true)
                chart.xAxis.labelRotationAngle = 45.0f
                chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                chart.xAxis.valueFormatter = MyXAxisFormatter()
                chart.xAxis.setDrawGridLines(true)
                chart.description.isEnabled = false

                // get a layout defined in xml
                //RelativeLayout rl = (RelativeLayout) findViewById(R.id.relativeLayout);
                //rl.add(chart); // add the programmatically created chart
                chart.measure(1000, 1000)
                chart.layout(0, 0, 1000, 1000)
                chart.isDrawingCacheEnabled = true
                chart.invalidate()
                val bitmap = chart.drawingCache
                views.setImageViewBitmap(R.id.imageView, bitmap)

                var totalLastDay = 0
                for (vaccineIdx: Int in 1 until VaccineData.vaccineLabel.size) {
                    totalLastDay += vaccineData.data.last().jabs[vaccineIdx]
                }

                val lastTime = Date(vaccineData.data.last().date.toLong() ).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                val lastTimeStr = lastTime.dayOfMonth.toString() + "/" + lastTime.monthValue.toString() + "/" + (lastTime.year - 2000).toString()

                views.setTextViewText(R.id.textView, "Dernier jour (" + lastTimeStr + "): " + totalLastDay)


                val intentUpdate = Intent(context, VaccineWidget::class.java)
                intentUpdate.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
//Update the current widget instance only, by creating an array that contains the widget’s unique ID//
                intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

//Wrap the intent as a PendingIntent, using PendingIntent.getBroadcast()//
                val pendingUpdate = PendingIntent.getBroadcast(
                    context, appWidgetId, intentUpdate,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

//Send the pending intent in response to the user tapping the ‘Update’ TextView//
                views.setOnClickPendingIntent(R.id.imageView, pendingUpdate)
                views.setOnClickPendingIntent(R.id.textView, pendingUpdate)


                appWidgetManager.updateAppWidget(appWidgetId, views)


            } else {

                val intentUpdate2 = Intent(context, VaccineWidget::class.java)
                intentUpdate2.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
//Update the current widget instance only, by creating an array that contains the widget’s unique ID//
                intentUpdate2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                intentUpdate2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                //Wrap the intent as a PendingIntent, using PendingIntent.getBroadcast()//
                val pendingUpdate2 = PendingIntent.getBroadcast(
                    context, appWidgetId, intentUpdate2,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.textView, pendingUpdate2)
                appWidgetManager.updateAppWidget(appWidgetId, views)

            }
        }
        super.onReceive(context, intent)
    }

}