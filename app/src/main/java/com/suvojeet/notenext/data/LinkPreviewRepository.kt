package com.suvojeet.notenext.data

import com.suvojeet.notenext.data.LinkPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class LinkPreviewRepository {

    suspend fun getLinkPreview(url: String): LinkPreview {
        return withContext(Dispatchers.IO) {
            try {
                val document = Jsoup.connect(url).get()
                val title = document.select("meta[property=og:title]").attr("content").ifEmpty {
                    document.title()
                }
                val description = document.select("meta[property=og:description]").attr("content").ifEmpty {
                    document.select("meta[name=description]").attr("content")
                }
                val imageUrl = document.select("meta[property=og:image]").attr("content")

                LinkPreview(url, title, description, imageUrl)
            } catch (e: Exception) {
                LinkPreview(url, null, null, null)
            }
        }
    }
}
