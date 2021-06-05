package com.tezcatli.vaxwidget

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import androidx.core.app.JobIntentService
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import kotlinx.parcelize.Parcelize
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


abstract class VaxData : Parcelable {
    abstract var type: VaxChart.Type
}

@Parcelize
data class VaxDataDailyJabsRow(val date: Long, val jabs: IntArray) : Parcelable

@Parcelize
data class VaxDataDailyJabs(
    val data: MutableList<VaxDataDailyJabsRow>,
    override var type : VaxChart.Type = VaxChart.Type.DailyJabs
) : VaxData() {
    companion object {
        val vaccineLabel = arrayOf<String>("Tous", "Pfizer", "Moderna", "AstraZeneca", "Janssen")
    }
}

abstract class VaxFetcher {
    abstract fun fetch() : VaxData

    abstract fun getDataFromIntent(intent : Intent) : VaxData?

    companion object {
        fun build(vaxType: VaxChart.Type): VaxFetcher? {
            when (vaxType) {
                VaxChart.Type.DailyJabs -> {
                    return VaxFetcherDailyJabs()
                }
                else -> {
                    Log.e("VaxDataService",  "Unknown data set: " + vaxType.name)
                    return null
                }
            }
        }
    }
}

class VaxFetcherDailyJabs : VaxFetcher() {
    override fun fetch() : VaxData {
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
                    Log.e("onUpdate", "-----------------" + line!![0])

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

                for (idx in 0..(VaxDataDailyJabs.vaccineLabel.size - 1)) {

                    sum[idx] = 0
                    for (i in 0..(window - 1)) {
                        sum[idx] += register[i][idx]
                    }
                    sum[idx] = sum[idx] / window
                }

                vaxDataDailyJabs.data.add(VaxDataDailyJabsRow(k, sum))
            }
            ctr++
        }

        return vaxDataDailyJabs
    }

    override fun getDataFromIntent(intent : Intent): VaxData? {
        return intent.getParcelableExtra<VaxDataDailyJabs>("VaccineData")
    }
}

class DataService : JobIntentService() {



    override fun onHandleWork(intent: Intent) {
        // We have received work to do.  The system or framework is already
        // holding a wake lock for us at this point, so we can just go.
        Log.e("DATASERVICE", "Executing work: $intent")

        //assert(intent.extras!!.containsKey("vaxDataName") == true)
        assert(intent.extras!!.containsKey("appWidgetId") == true)

        val vaxDataName = intent.getStringExtra("vaxDataName")

        if (vaxDataName != null) {

            val vaxData = fetchData(VaxChart.name2Type(vaxDataName)!!)

            if (vaxData != null) {

                val broadcastIntent = Intent(this, VaccineWidget::class.java)
                Log.e("DATASERVICE", "Sending intent to " + VaccineWidget::class.java)

                broadcastIntent.setAction(VaccineWidget.DISPLAY_DATA)


                broadcastIntent.putExtra("VaccineData", vaxData)
                broadcastIntent.putExtra("appWidgetId", intent.getIntExtra("appWidgetId", 0))
                broadcastIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                sendBroadcast(broadcastIntent)
                Log.e("DATASERVICE", "Intent sent")


                Log.i("DataService", "Completed service @ " + SystemClock.elapsedRealtime())
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


        fun requestData(context: Context, vaxType : VaxChart.Type, appWidgetId: Int) {

            val i = Intent(context, DataService::class.java)
            // potentially add data to the intent
            i.putExtra("appWidgetId", appWidgetId)
            i.putExtra("vaxDataName", vaxType.name)

            enqueueWork(context, i)
        }

        fun fetchData(vaxDataType: VaxChart.Type) : VaxData? {
            val fetcher = VaxFetcher.build(vaxDataType)

            if (fetcher != null) {
                return fetcher.fetch()
            } else {
                return null
            }

        }

        /**
         * Convenience method for enqueuing work in to this service.
         */
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(
                context,
                DataService::class.java, JOB_ID, work
            )
        }
    }
}