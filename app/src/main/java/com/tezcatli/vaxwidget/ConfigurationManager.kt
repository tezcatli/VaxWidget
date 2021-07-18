package com.tezcatli.vaxwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.beust.klaxon.Klaxon



class ConfigurationManager(val context : Context) {
    data class ConfigurationEntry (
//        val widgetId : Int? = null,
        val period : Int = defaultPeriod,
        val charts : List<String> = listOf<String>("DailyJabs")
    ) {
        companion object {
            const val defaultPeriod: Int = 60
        }
    }

    data class Configuration (
        var entries : MutableMap<String,ConfigurationEntry> = mutableMapOf<String, ConfigurationEntry>()
        )


    val sharedPref: SharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key),
        Context.MODE_PRIVATE
    )

    var configuration = Configuration()

    fun loadConf() {
        val confJson : String = sharedPref.getString(context.getString(R.string.configuration_json_key), "{}") ?: ""
        Log.e("JSON", "Loading: " + confJson)
        configuration = Klaxon().parse<Configuration>(confJson) ?: Configuration()

    }

    init {
        loadConf()

        /*
        context.sendBroadcast(Intent(context, VaccineWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, configuration.entries.keys.toIntArray())})
            */

    }

    fun setEntry(appWidgetId: Int, entry : ConfigurationEntry) {
        configuration.entries[appWidgetId.toString()] = entry
        saveConf()
    }

    fun getEntry(appWidgetId: Int) : ConfigurationEntry? {
        val entry = configuration.entries.get(appWidgetId.toString())


        return entry
    }

    fun deleteEntry(appWidgetId: Int) {
        configuration.entries.remove(appWidgetId.toString())
        saveConf()
    }

    fun saveConf() {
        val json = Klaxon().toJsonString(configuration)

        Log.e("JSON", "Saving:" + json)

        with(sharedPref.edit()) {
            putString(context.getString(R.string.configuration_json_key), json)
            apply()
        }
    }
}