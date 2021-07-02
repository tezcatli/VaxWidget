package com.tezcatli.vaxwidget

import android.content.Context
import android.util.Log
import okhttp3.Cache
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.net.URL



class HttpFetcher constructor(
    private val context : Context)
{

    val cache : Cache = Cache(File(context.cacheDir,"http-cache"), 10*1024*1024)
    val client : OkHttpClient = OkHttpClient().newBuilder().cache(cache).build()


    //val client : OkHttpClient = OkHttpClient().newBuilder().build()

    fun requestGet(url : URL) : InputStream {

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        return response.body!!.byteStream()
    }

}
