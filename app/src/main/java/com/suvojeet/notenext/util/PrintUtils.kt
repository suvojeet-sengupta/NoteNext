package com.suvojeet.notenext.util

import android.content.Context
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

fun printNote(context: Context, htmlContent: String) {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = webView.createPrintDocumentAdapter("Note Document")
            printManager.print("Note Document", printAdapter, null)
        }
    }
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
}
