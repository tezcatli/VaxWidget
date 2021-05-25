package com.tezcatli.vaxwidget

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import androidx.core.app.JobIntentService
import com.opencsv.CSVReader
import kotlinx.parcelize.Parcelize
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


@Parcelize
data class VaccineRow(val date: Long, val jabs: IntArray) : Parcelable

@Parcelize
data class VaccineData(val data : MutableList<VaccineRow>) : Parcelable {
    companion object {
        val vaccineLabel = arrayOf<String>("Tous", "Pfizer", "Moderna", "AstraZeneca", "Janssen")
    }
}

class DataService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        // We have received work to do.  The system or framework is already
        // holding a wake lock for us at this point, so we can just go.
        Log.e("DATASERVICE", "Executing work: $intent")

        var vaccineData = mutableMapOf<Long, IntArray>()

        try {
            val url =
                URL("https://www.data.gouv.fr/fr/datasets/r/b273cf3b-e9de-437c-af55-eda5979e92fc")
            val urlConnection = url.openConnection() as HttpURLConnection

            val `in`: InputStream = BufferedInputStream(urlConnection.inputStream)
            //readStream(in);

            val r = BufferedReader(InputStreamReader(`in`), 1000)
            val reader = CSVReader(r, ';')
            var line: Array<String>?

            var rowCtr: Int = 0

            while (reader.readNext().also { line = it } != null) {
                try {
                    if (line != null && rowCtr != 0) {

                        val dateFields: List<String> = line!![2].split("-")
                        val calendar = Calendar.getInstance()
                        calendar.set(dateFields[0].toInt(),dateFields[1].toInt() - 1, dateFields[2].toInt(),0,0,0)
                        calendar.set(Calendar.MILLISECOND, 0);
                        val time: Long = calendar.time.time

                        if (! vaccineData.containsKey(time)) {
                            //vaccineData[calendar.time] = mutableMapOf<Int, Int>()
                            vaccineData[time] = IntArray(VaccineData.vaccineLabel.size)

                        }

                        vaccineData[time]!![line!![1].toInt()] = line!![3].toInt() + line!![4].toInt()

                    }
                } catch (e: NumberFormatException) {
                    Log.e("onUpdate", "Uncaught exception: " + e.toString(), e)
                }

                rowCtr++
            }

        } catch (e: Exception) {
            Log.e("onUpdate", "Uncaught exception : " + e.toString(), e)
        } finally {

            val window : Int = 7



            var parcel = VaccineData(mutableListOf())
            val sorted = vaccineData.toSortedMap()


            var register = Array(window) {
                IntArray(VaccineData.vaccineLabel.size)
            }


            var ctr : Int = 0
            for ((k,v) in sorted) {
                register[ctr.rem(window)] = v
                if (ctr >= (window-1)) {

                    var sum = IntArray(VaccineData.vaccineLabel.size)

                    for (idx in 0..(VaccineData.vaccineLabel.size-1)) {

                        sum[idx] = 0
                        for (i in 0..(window-1)) {
                            sum[idx] += register[i][idx]
                        }
                        sum[idx] = sum[idx] / window
                    }

                    parcel.data.add(VaccineRow(k, sum))
                }
                ctr++
            }

            var broadcastIntent = Intent(this, VaccineWidget::class.java)
            Log.e("DATASERVICE", "Sending intent to " + VaccineWidget::class.java)

            //var broadcastIntent = Intent(SimpleAppWidget.javaClass.name)
            broadcastIntent.setAction(VaccineWidget.DISPLAY_DATA)
            //broadcastIntent.setAction("com.erenutku.com.tezcatli.vaxwidget.PROCESS_RESPONSE")
            //broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT)
            //broadcastIntent.setAction("PLEASE_PLAY")
            //broadcastIntent.putIntegerArrayListExtra("VaccineData", ArrayList(data));

            assert(intent.extras!!.containsKey("appWidgetId") == true)

            broadcastIntent.putExtra("VaccineData", parcel)
            broadcastIntent.putExtra("appWidgetId", intent!!.getIntExtra("appWidgetId", 0))
            //broadcastIntent.putExtra("myMessage", "message string");
            broadcastIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            sendBroadcast(broadcastIntent)
            Log.e("DATASERVICE", "Intent sent")

        }


        Log.i("DataService", "Completed service @ " + SystemClock.elapsedRealtime())
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    companion object {
        /**
         * Unique job ID for this service.
         */
        const val JOB_ID = 1000

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