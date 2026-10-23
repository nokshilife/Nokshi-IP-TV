package com.example.player

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerView(
    videoId: String,
    modifier: Modifier = Modifier,
    onPlaybackStarted: () -> Unit = {},
    onPlaybackError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val webView = remember(context) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true
                useWideViewPort = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                allowFileAccess = true
                allowContentAccess = true
                setSupportMultipleWindows(false)
                // Use standard Android Chrome user-agent for optimal HTML5 live stream support
                userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 7 Build/UQ1A.240205.002) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.64 Mobile Safari/537.36"
            }

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    // Prevent external browser launches for internal youtube frame navigation
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    mainHandler.postDelayed({
                        onPlaybackStarted()
                    }, 1500)
                }
            }

            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onPlaying() {
                        mainHandler.post { onPlaybackStarted() }
                    }

                    @JavascriptInterface
                    fun onBuffering() {
                        mainHandler.post { onPlaybackStarted() }
                    }

                    @JavascriptInterface
                    fun onError(errorCode: Int) {
                        mainHandler.post { onPlaybackError("YouTube error code: $errorCode") }
                    }
                },
                "AndroidInterface"
            )
        }
    }

    LaunchedEffect(videoId) {
        if (videoId.isNotEmpty()) {
            val html = buildYouTubeHtml(videoId)
            // CRITICAL FIX FOR ERROR 152-4:
            // YouTube blocks loadDataWithBaseURL if baseUrl is "https://www.youtube.com" (detects clickjacking/spoofing).
            // Using "https://bcs24admin.org.bd" supplies a valid Referer header satisfying YouTube's embed requirements.
            webView.loadDataWithBaseURL(
                "https://bcs24admin.org.bd",
                html,
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webView.stopLoading()
                webView.destroy()
            } catch (e: Exception) {}
        }
    }

    Box(modifier = modifier.background(androidx.compose.ui.graphics.Color.Black)) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun buildYouTubeHtml(videoId: String): String {
    val originDomain = "https://bcs24admin.org.bd"
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>
                * { margin:0; padding:0; box-sizing:border-box; background-color:#000000; overflow:hidden; }
                html, body { width:100%; height:100%; }
                #player-container { width:100%; height:100%; position:relative; }
                iframe { width:100%; height:100%; border:none; }
            </style>
        </head>
        <body>
            <div id="player-container">
                <iframe id="ytplayer"
                    src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&enablejsapi=1&playsinline=1&rel=0&modestbranding=1&origin=$originDomain&widget_referrer=$originDomain"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                    allowfullscreen>
                </iframe>
            </div>
            <script>
                // Listen to postMessage from YouTube embed API
                window.addEventListener('message', function(event) {
                    try {
                        var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                        if (data && data.event === 'onStateChange') {
                            // 1 = PLAYING, 3 = BUFFERING
                            if (data.info === 1 || data.info === 3) {
                                if (window.AndroidInterface) {
                                    window.AndroidInterface.onPlaying();
                                }
                            }
                        } else if (data && data.event === 'onError') {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onError(data.info);
                            }
                        }
                    } catch (e) {}
                });

                // Fallback: Notify Android that buffer is initiating
                setTimeout(function() {
                    if (window.AndroidInterface) {
                        window.AndroidInterface.onBuffering();
                    }
                }, 1800);
            </script>
        </body>
        </html>
    """.trimIndent()
}
