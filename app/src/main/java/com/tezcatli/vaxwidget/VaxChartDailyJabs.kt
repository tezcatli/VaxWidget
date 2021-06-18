package com.tezcatli.vaxwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Parcelable
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
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import kotlinx.parcelize.Parcelize
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneId
import java.util.*

class VaxChartDailyJabs : VaxChart() {

    @Parcelize
    data class VaxDataDailyJabs(
        val data: MutableList<Row>
    ) : Parcelable {

        @Parcelize
        data class Row(val date: Long, val jabs: IntArray) : Parcelable

        companion object {
            val vaccineLabel = arrayOf<String>("Tous", "Pfizer", "Moderna", "AstraZeneca", "Janssen")
        }
    }

    class MyXAxisFormatter : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            ///val calendar = Calendar.getInstance()
            val time = Date(value.toLong() * 86400).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            return time.dayOfMonth.toString() + "/" + time.monthValue.toString() + "/" + (time.year - 2000).toString()

        }
    }

    override val type = Type.DailyJabs
    var vaxData = VaxDataDailyJabs(mutableListOf())

    override fun serialize(): Parcelable {
        return vaxData
    }

    override fun deserialize(intent: Intent, name: String) {
        vaxData = intent.getParcelableExtra(name)!!
    }

    override fun fetch() {
        val vaxData = mutableMapOf<Long, IntArray>()

        try {
            val url =
                URL("https://www.data.gouv.fr/fr/datasets/r/b273cf3b-e9de-437c-af55-eda5979e92fc")
            val urlConnection = url.openConnection() as HttpURLConnection

            val `in`: InputStream = BufferedInputStream(urlConnection.inputStream)
            //readStream(in);

            val r = BufferedReader(InputStreamReader(`in`), 1000)
            //val reader = CSVReader(r, ';')

            val parser = CSVParserBuilder().withSeparator(';').build()

            val reader = CSVReaderBuilder(r).withCSVParser(parser).withSkipLines(1).build()
            var line: Array<String>?

            while (reader.readNext().also { line = it } != null) {
                try {

                    val dateFields: List<String> = line!![2].split("-")
                    val calendar = Calendar.getInstance()
                    calendar.set(
                        dateFields[0].toInt(),
                        dateFields[1].toInt() - 1,
                        dateFields[2].toInt(),
                        0,
                        0,
                        0
                    )
                    calendar.set(Calendar.MILLISECOND, 0)
                    val time: Long = calendar.time.time

                    if (!vaxData.containsKey(time)) {
                        //vaccineData[calendar.time] = mutableMapOf<Int, Int>()
                        vaxData[time] = IntArray(VaxDataDailyJabs.vaccineLabel.size)

                    }

                    vaxData[time]!![line!![1].toInt()] = line!![3].toInt() + line!![4].toInt()

                } catch (e: NumberFormatException) {
                    Log.e("onUpdate", "Uncaught exception: " + e.toString(), e)
                }
            }

        } catch (e: Exception) {
            Log.e("onUpdate", "Uncaught exception : " + e.toString(), e)
            throw(e)
        }

        val window = 7

        val vaxDataDailyJabs = VaxDataDailyJabs(mutableListOf())
        val sorted = vaxData.toSortedMap()


        val register = Array(window) {
            IntArray(VaxDataDailyJabs.vaccineLabel.size)
        }

        var ctr = 0
        for ((k, v) in sorted) {
            register[ctr.rem(window)] = v
            if (ctr >= (window - 1)) {

                val sum = IntArray(VaxDataDailyJabs.vaccineLabel.size)

                for (idx in 0 until VaxDataDailyJabs.vaccineLabel.size) {

                    sum[idx] = 0
                    for (i in 0 until window) {
                        sum[idx] += register[i][idx]
                    }
                    sum[idx] = sum[idx] / window
                }

                vaxDataDailyJabs.data.add(VaxDataDailyJabs.Row(k, sum))
            }
            ctr++
        }

        this.vaxData = vaxDataDailyJabs
    }

    override fun paint(context: Context, appWidgetId: Int) {


        Log.e("VaxChartDailyJabs", "Paint starting")


        val chart = LineChart(context)
        val lineData = LineData()

        val colors = arrayOf<Int>(
            R.color.black,
            R.color.red,
            R.color.brown,
            R.color.green,
            R.color.orange
        )

        for (vaccineIdx: Int in 1 until VaxDataDailyJabs.vaccineLabel.size) {

            val entries: MutableList<Entry> = ArrayList<Entry>()

            //var entry : String;
            for (vaccineDataEntry in vaxData.data) {
                entries.add(
                    Entry(
                        (vaccineDataEntry.date / 86400).toFloat(),
                        vaccineDataEntry.jabs[vaccineIdx].toFloat()
                    )
                )
            }

            val dataSet =
                LineDataSet(
                    entries.toList(),
                    VaxDataDailyJabs.vaccineLabel[vaccineIdx]
                )
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

        chart.measure(1000, 1000)
        chart.layout(0, 0, 1000, 1000)

        chart.invalidate()

        val bitmap =
            Bitmap.createBitmap(chart.width, chart.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        chart.draw(canvas)

        var totalLastDay = 0
        for (vaccineIdx: Int in 1 until VaxDataDailyJabs.vaccineLabel.size) {
            totalLastDay += vaxData.data.last().jabs[vaccineIdx]
        }

        val lastTime = Date(vaxData.data.last().date).toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val lastTimeStr =
            lastTime.dayOfMonth.toString() + "/" + lastTime.monthValue.toString() + "/" + (lastTime.year - 2000).toString()

        val appWidgetManager = AppWidgetManager.getInstance(
            context.applicationContext
        )

        val views = RemoteViews(context.packageName, R.layout.chart_widget_layout)

        views.setImageViewBitmap(R.id.imageView, bitmap)
        views.setTextViewText(
            R.id.textView,
            "Dernier jour (" + VaccineWidget.testCounter + " " + lastTimeStr + "): " + totalLastDay
        )

        val intentUpdate = Intent(context, VaccineWidget::class.java)
        intentUpdate.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, arrayOf(appWidgetId))
        //intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        val pendingUpdate = PendingIntent.getBroadcast(
            context, appWidgetId, intentUpdate,
            PendingIntent.FLAG_UPDATE_CURRENT
        )


        views.setOnClickPendingIntent(R.id.imageView, pendingUpdate)
        views.setOnClickPendingIntent(R.id.textView, pendingUpdate)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}