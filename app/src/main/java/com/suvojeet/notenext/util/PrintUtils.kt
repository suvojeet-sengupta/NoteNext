package com.suvojeet.notenext.util

import android.content.Context
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

fun printNote(context: Context, htmlContent: String, title: String = "Note Document") {
    val webView = WebView(context)
    webView.settings.allowFileAccess = true
    webView.settings.allowContentAccess = true
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = webView.createPrintDocumentAdapter(title)
            printManager.print(title, printAdapter, null)
        }
    }
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
}
