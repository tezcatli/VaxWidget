package com.tezcatli.vaxwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import java.lang.Exception


class ConfigureActivity : Activity() {
    var checkBoxes = mutableListOf<CheckBox>()


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("Config", "Starting configuration activity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.configuration_layout)

        val appWidgetId = intent!!.extras!!.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        setResult(RESULT_CANCELED)


        val configurationManager =
            (this.applicationContext as VaxApplication).serviceLocator.configurationManager

        val configurationEntry =
            configurationManager.getEntry(appWidgetId) ?: ConfigurationManager.ConfigurationEntry()

        val layout: LinearLayout = findViewById(R.id.configCheckboxesLayout)

        enumValues<VaxChart.Type>().forEach { enum ->
            val checkBox: CheckBox = CheckBox(applicationContext).apply {
                text = enum.long
                tag = enum.name

                configurationEntry.charts.forEach { chart ->
                    if (chart == enum.name) {
                        isChecked = true
                    }
                }
            }
            checkBoxes.add(checkBox)
            layout.addView(checkBox)
        }

        val periodTextView: EditText = findViewById(R.id.editTextNumber)

        periodTextView.setText(configurationEntry.period.toString())

        val button: Button = findViewById(R.id.button)

        button.setOnClickListener {

            // Validation

            val period: Int

            try {
                period = periodTextView.text.toString().toInt()
            } catch (exception: Exception) {
                periodTextView.setError("Valeur invalide")
                return@setOnClickListener
            }

            val newConfigurationEntry = ConfigurationManager.ConfigurationEntry(
                period,
                checkBoxes.mapNotNull {
                    if (it.isChecked) it.tag as String else null
                }
            )

            configurationManager.setEntry(appWidgetId, newConfigurationEntry)

            configurationManager.saveConf()


            /*
            val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(applicationContext)

            RemoteViews(applicationContext.packageName, R.layout.chart_widget_layout).also { views->
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
*/

            val result = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }

            setResult(RESULT_OK, result)

            applicationContext.sendBroadcast(Intent(applicationContext, VaccineWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))})

            finish()
        }

    }
}