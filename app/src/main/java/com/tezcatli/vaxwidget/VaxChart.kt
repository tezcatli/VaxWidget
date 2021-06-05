package com.tezcatli.vaxwidget

import android.graphics.Bitmap
import android.util.Log


interface IndexedEnum {
    val name: String

    companion object {
        inline fun <reified T : IndexedEnum> valueOf(name: String) =
            T::class.java.takeIf { it.isEnum }?.enumConstants?.find {
                Log.e("name ---->", name)
                it.name == name
            }
    }
}

abstract class VaxChart  {

    abstract fun draw(vaxData: VaxData): Bitmap

    enum class Type(name: String) : IndexedEnum {
        DailyJabs("Daily Jabs")
    }

    companion object {
        fun name2Type(name : String) : Type? {
            return IndexedEnum.valueOf<Type>(name)
        }
    }

}

