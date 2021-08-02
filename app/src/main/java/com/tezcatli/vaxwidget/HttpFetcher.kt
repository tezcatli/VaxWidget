package com.tezcatli.vaxwidget

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat.getSystemService
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.net.URL


class HttpFetcher constructor(
    private val context : Context)
{

    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isConnected() : Boolean {
        val network = cm.activeNetworkInfo
        return network?.isConnectedOrConnecting == true
    }

    var onlineInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        val maxAge = 60 // read from cache for 60 seconds even if there is internet connection
        response.newBuilder()
            .header("Cache-Control", "public, max-age=$maxAge")
            .removeHeader("Pragma")
            .build()
    }

    var offlineInterceptor = Interceptor { chain ->
        var request = chain.request()
        if (!isConnected()) {
            val maxStale = 60 * 60 * 24 * 30 // Offline cache available for 30 days
            request = request.newBuilder()
                .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                .removeHeader("Pragma")
                .build()
        }
        chain.proceed(request)
    }

    val cache : Cache = Cache(File(context.cacheDir,"http-cache"), 10*1024*1024)
    val client : OkHttpClient = OkHttpClient().newBuilder()
        .addInterceptor(offlineInterceptor)
        .addNetworkInterceptor(onlineInterceptor)
        .cache(cache).build()


    //val client : OkHttpClient = OkHttpClient().newBuilder().build()

    fun requestGet(url : URL) : InputStream {

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        return response.body!!.byteStream()
    }

}
