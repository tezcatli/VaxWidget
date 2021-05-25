package com.tezcatli.vaxwidget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

//import android.support.v7.app.AppCompatActivity;
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //val context = applicationContext
        //val i = Intent(context, DataService::class.java)
        // potentially add data to the intent
        //i.putExtra("KEY1", "Value to be used by the service")
        //startService(i)
    }
}