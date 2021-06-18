package com.tezcatli.vaxwidget

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.util.Log

abstract class VaxChart {


    enum class Type(val short: String, val long: String) {
        DailyJabs("Daily Jabs", "Nombre d'injections quotidiennes"),
        ImmunizationCoverage("Immunization Coverage", "Couverture d'immunisation")
    }

    abstract val type: Type

    abstract fun serialize(): Parcelable
    abstract fun deserialize(intent: Intent, name: String)

    abstract fun fetch()

    abstract fun paint(context: Context, appWidgetId : Int)

    companion object {
        fun build(type: Type): VaxChart? {
            when (type) {
                Type.DailyJabs -> {
                    return VaxChartDailyJabs()
                }
                else -> {
                    Log.e("VaxWidget", "Unknown type: " + type.name)
                    return null
                }
            }

        }
    }
}