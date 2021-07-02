package com.tezcatli.vaxwidget

import android.content.Context


interface VaxServiceLocator {
    val httpFetcher : HttpFetcher
    val configurationManager : ConfigurationManager
    val widgetController : VaxWidgetController
}

class AppServiceLocator(val context : Context) {
    val httpFetcher : HttpFetcher by lazy { HttpFetcher(context) }
    val configurationManager : ConfigurationManager by lazy { ConfigurationManager(context) }
    val vaxWidgetController : VaxWidgetController by lazy { VaxWidgetController(context)}
}