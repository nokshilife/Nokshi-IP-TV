package com.example.player

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            }

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Page loaded: Notify that playback / buffering is active
                    mainHandler.postDelayed({
                        onPlaybackStarted()
                    }, 1200)
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

    androidx.compose.runtime.LaunchedEffect(videoId) {
        if (videoId.isNotEmpty()) {
            val html = buildYouTubeHtml(videoId)
            webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
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
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin:0; padding:0; box-sizing:border-box; background-color:#000000; overflow:hidden; }
                html, body { width:100%; height:100%; }
                #player-container { width:100%; height:100%; position:relative; }
                iframe { width:100%; height:100%; border:0; }
            </style>
        </head>
        <body>
            <div id="player-container">
                <iframe id="ytplayer"
                    src="https://www.youtube.com/embed/$videoId?autoplay=1&enablejsapi=1&playsinline=1&rel=0&modestbranding=1"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                    allowfullscreen>
                </iframe>
            </div>
            <script>
                // Listen to postMessage events from YouTube embed
                window.addEventListener('message', function(event) {
                    try {
                        var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                        if (data && data.event === 'onStateChange') {
                            if (data.info === 1 || data.info === 3) { // PLAYING or BUFFERING
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

                // Fallback: Notify Android after DOM is ready
                document.addEventListener('DOMContentLoaded', function() {
                    setTimeout(function() {
                        if (window.AndroidInterface) {
                            window.AndroidInterface.onBuffering();
                        }
                    }, 1000);
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}
