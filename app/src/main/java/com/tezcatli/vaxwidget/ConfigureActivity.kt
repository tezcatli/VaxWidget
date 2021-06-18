package com.tezcatli.vaxwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.beust.klaxon.Klaxon
import java.lang.Exception

data class ConfigurationData (
    val period : Int = defaultPeriod,
    val charts : List<String> = listOf<String>("DailyJabs")
        ) {
    companion object {
        const val defaultPeriod: Int = 60
    }
}

class ConfigurationManager(val context : Context) {


    val sharedPref: SharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)

    fun getConf() : ConfigurationData {
        val confJson : String = sharedPref.getString(context.getString(R.string.configuration_json_key), "{}") ?: ""
        Log.e("JSON",  "Loading: " + confJson)
        return Klaxon().parse<ConfigurationData>(confJson) ?: ConfigurationData()
    }

    fun setConf(configuration : ConfigurationData) {
        val json = Klaxon().toJsonString(configuration)

        Log.e("JSON",  "Saving:" + json)

        with(sharedPref.edit()) {
            putString(context.getString(R.string.configuration_json_key), json)
            apply()
        }
    }
}

class ConfigureActivity : Activity() {
    var checkBoxes = mutableListOf<CheckBox>()


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("Config",  "Starting configuration activity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.configuration_layout)

        setResult(RESULT_CANCELED)


        val configurationManager = ConfigurationManager(applicationContext)

        val configuration = configurationManager.getConf()

        val layout : LinearLayout = findViewById(R.id.configCheckboxesLayout)

        enumValues<VaxChart.Type>().forEach { enum ->
            val checkBox : CheckBox = CheckBox(applicationContext).apply {
                text = enum.long
                tag = enum.name

                configuration.charts.forEach { chart ->
                    if (chart == enum.name) {
                        isChecked = true
                    }
                }
            }
            checkBoxes.add(checkBox)
            layout.addView(checkBox)
        }

        val periodTextView : EditText = findViewById(R.id.editTextNumber)

        periodTextView.setText(configuration.period.toString())

        val button : Button = findViewById(R.id.button)

        button.setOnClickListener {

            // Validation

            val period: Int

            try {
                period = periodTextView.text.toString().toInt()
            } catch (exception: Exception) {
                periodTextView.setError("Valeur invalide")
                return@setOnClickListener
            }

            val newConfiguration = ConfigurationData(
                period,
                checkBoxes.mapNotNull{
                    if (it.isChecked) it.tag as String else null
                }
            )

            configurationManager.setConf(newConfiguration)

            val extras = intent.extras
            if (extras != null) {
                val appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )

                val result = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(RESULT_OK, result)
                finish()
            }

        }

    }
}