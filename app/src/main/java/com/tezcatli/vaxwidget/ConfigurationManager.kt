package com.tezcatli.vaxwidget

import android.content.Context
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
        var entries : MutableMap<Int,ConfigurationEntry> = mutableMapOf<Int, ConfigurationEntry>()
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
    }

    fun setEntry(appWidgetId: Int, entry : ConfigurationEntry) {
        configuration.entries[appWidgetId] = entry
    }

    fun getEntry(appWidgetId: Int) : ConfigurationEntry? {
        return configuration.entries.get(appWidgetId)
    }

    fun deleteEntry(appWidgetId: Int) {
        configuration.entries.remove(appWidgetId)
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