package com.fitmate.app.tutorial

import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun TutorialWebPlayer(
    tutorial: Tutorial,
    modifier: Modifier = Modifier
) {
    val headers = mapOf(
        "Referer" to ("https://" + "www.youtube.com/")
    )

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false

                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()

                setBackgroundColor(Color.BLACK)
                loadUrl(tutorial.embedUrl, headers)
            }
        },
        update = { webView ->
            if (webView.url != tutorial.embedUrl) {
                webView.loadUrl(tutorial.embedUrl, headers)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        }
    )
}
