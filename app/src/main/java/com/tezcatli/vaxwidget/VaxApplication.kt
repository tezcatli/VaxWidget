package com.tezcatli.vaxwidget

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.util.Log


class VaxApplication : Application() {

    lateinit var serviceLocator : AppServiceLocator


    override fun onCreate() {

        /*
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork() // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )

*/

        serviceLocator = AppServiceLocator(applicationContext)
        Log.d(VaxApplication::class.simpleName, "onCreate: Application")

        super.onCreate()


    }
}