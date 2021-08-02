package com.tezcatli.vaxwidget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.icu.number.IntegerWidth
import android.os.Parcelable
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.opencsv.CSVParser
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import kotlinx.parcelize.Parcelize
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.time.ZoneId
import java.util.*

class VaxChartImmunization constructor(var context: Context) : VaxChart() {

    @Parcelize
    data class Data(
        val row: MutableMap<Int, Row>,
        var date: Long
    ) : Parcelable {

        @Parcelize
        data class Row(val dose1: Float, val complete: Float) : Parcelable

        companion object {
            val dataClass =
                mapOf(
                    0 to "0-100",
                    4 to "0-4",
                    9 to "5-9",
                    11 to "10-11",
                    17 to "12-17",
                    24 to "18-24",
                    29 to "25-29",
                    39 to "30-39",
                    49 to "40-49",
                    59 to "50-59",
                    64 to "60-64",
                    69 to "60-69",
                    74 to "70-74",
                    79 to "75-79",
                    80 to "80-100"
                )

        }
    }


    override val type = Type.DailyJabs

    var vaxData = Data(mutableMapOf(), 0)

    override fun serialize(): Parcelable {
        return vaxData
    }

    override fun deserialize(intent: Intent, name: String) {
        vaxData = intent.getParcelableExtra(name)!!
    }

    override fun isDataValid(): Boolean {
        return !vaxData.row.isEmpty()
    }


    override fun fetch() {

        Log.e("VaxChartDailyJabs", "Fetch starting")


        lateinit var stream: InputStream
        lateinit var inputStreamReader: InputStreamReader
        lateinit var bufferedStream: BufferedReader
        lateinit var parser: CSVParser
        lateinit var reader: CSVReader

        try {
            val httpFetcher =
                (context.applicationContext as VaxApplication).serviceLocator.httpFetcher


            stream =
                httpFetcher.requestGet(URL("https://www.data.gouv.fr/fr/datasets/r/dc103057-d933-4e4b-bdbf-36d312af9ca9"))
            inputStreamReader = InputStreamReader(stream)
            bufferedStream = BufferedReader(inputStreamReader, 1000)
            parser = CSVParserBuilder().withSeparator(';').build()
            reader = CSVReaderBuilder(bufferedStream).withCSVParser(parser).withSkipLines(1).build()


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

                    vaxData.date = time

                    vaxData.row[line!![1].toInt()] =
                        Data.Row(line!![6].toFloat(), line!![7].toFloat())


                } catch (e: NumberFormatException) {
                    Log.e("onUpdate", "Uncaught exception: " + e.toString(), e)
                }
            }

        } catch (e: Exception) {
            Log.e("onUpdate", "Uncaught exception : " + e.toString(), e)

            this.vaxData = Data(mutableMapOf(), 0)

            throw(e)
        } finally {

            try {
                stream.close()
                inputStreamReader.close()
                bufferedStream.close()
                reader.close()
            } catch (e: Exception) {

            }
        }

    }

    override fun paint2(appWidgetId: Int, width: Int, height: Int): RemoteViews {


        Log.e("VaxChartDailyJabs", "Paint starting")

        val views = RemoteViews(context.packageName, R.layout.chart_widget_layout)


        if (isDataValid()) {
            val chart = LineChart(context)
            val lineData = LineData()

            val entries: MutableList<Entry> = ArrayList<Entry>()

            for (vaccineIdx in vaxData.row.keys.sorted()) {

                if (vaccineIdx == 0)
                    continue

                entries.add(
                    Entry(
                        Data.dataClass[vaccineIdx]!!.split('-').let {
                            it[0].toFloat() + (it[1].toFloat() - it[0].toFloat()) / 2
                        },
                        vaxData.row[vaccineIdx]!!.dose1
                    )
                )
            }

            LineDataSet(entries.toList(), "Partielle").let {
                it.setColors(intArrayOf(R.color.green), context)
                lineData.addDataSet(it)
                it.setDrawFilled(true)
                it.setDrawCircles(false)
                it.fillColor = ContextCompat.getColor(context, R.color.green)

            }

            entries.clear()

            for (vaccineIdx in vaxData.row.keys.sorted()) {

                if (vaccineIdx == 0)
                    continue

                entries.add(
                    Entry(
                        //getClassIndex(vaccineIdx.toInt()).toFloat(),
                        Data.dataClass[vaccineIdx]!!.split('-').let {
                            it[0].toFloat() + (it[1].toFloat() - it[0].toFloat()) / 2
                        },
                        //vaccineIdx.toFloat(),
                        vaxData.row[vaccineIdx]!!.complete
                    )
                )
            }

            LineDataSet(entries.toList(), "Totale").let {
                it.setColors(intArrayOf(R.color.red), context)
                lineData.addDataSet(it)
                it.setDrawFilled(true)
                it.setDrawCircles(false)
                it.fillColor = ContextCompat.getColor(context, R.color.red)
            }

            chart.data = lineData
            chart.axisRight.isEnabled = false
            chart.axisLeft.valueFormatter = LargeValueFormatter()
            chart.xAxis.setLabelCount(6, true)
            chart.xAxis.labelRotationAngle = 45.0f
            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            // chart.xAxis.valueFormatter = MyXAxisFormatter()
            chart.xAxis.setDrawGridLines(true)
            chart.xAxis.setTextColor(Color.WHITE)
            chart.axisLeft.setTextColor(Color.WHITE)
            chart.legend.setTextColor(Color.WHITE)
            chart.legend.isWordWrapEnabled = true

            chart.description.isEnabled = false

            chart.measure(width, height)
            chart.layout(0, 0, width, height)

            chart.invalidate()

            val bitmap =
                Bitmap.createBitmap(chart.width, chart.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            chart.draw(canvas)

            var totalLastDay = 0

            val lastTime = Date(vaxData.date).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate()
            val lastTimeStr =
                lastTime.dayOfMonth.toString() + "/" + lastTime.monthValue.toString() + "/" + (lastTime.year - 2000).toString()


            views.setImageViewBitmap(R.id.imageView, bitmap)


            views.setTextViewText(
                R.id.textView,
                "Couverture vaccinale (${lastTimeStr})"
            )

        }

        return views

    }
}
