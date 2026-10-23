package com.example.player

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
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

            // CRITICAL FOR ANDROID TV:
            // Disable D-pad/keyboard focus on the WebView so it does NOT hijack TV remote navigation
            // from the Compose UI (channel cards, tabs, and buttons). Mobile touch gestures still function.
            isFocusable = false
            isFocusableInTouchMode = false

            // CRITICAL FOR YOUTUBE VERIFICATION & SESSION PERSISTENCE:
            // Enable first-party and third-party cookies so user authentication / human checks persist
            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)
            } catch (e: Exception) {}

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
                // Modern Chrome user-agent
                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.113 Mobile Safari/537.36"
            }

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    // Stay within WebView for embedded stream navigation
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    try {
                        CookieManager.getInstance().flush()
                    } catch (e: Exception) {}

                    // Tell app playback initiated after page finishes
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

    LaunchedEffect(videoId) {
        if (videoId.isNotEmpty()) {
            val html = buildYouTubeHtml(videoId)
            // Use domain as baseUrl to provide authorized Referer and Origin headers
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
                    src="https://www.youtube.com/embed/$videoId?autoplay=1&mute=0&enablejsapi=1&playsinline=1&rel=0&modestbranding=1&origin=$originDomain&widget_referrer=$originDomain"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                    allowfullscreen>
                </iframe>
            </div>
            <script>
                // Load YouTube IFrame API
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player;
                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('ytplayer', {
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange,
                            'onError': onPlayerError
                        }
                    });
                }

                function onPlayerReady(event) {
                    try {
                        event.target.playVideo();
                    } catch(e) {}
                    if (window.AndroidInterface) {
                        window.AndroidInterface.onPlaying();
                    }
                }

                function onPlayerStateChange(event) {
                    // 1 = PLAYING, 3 = BUFFERING
                    if (event.data === 1 || event.data === 3) {
                        if (window.AndroidInterface) {
                            window.AndroidInterface.onPlaying();
                        }
                    }
                }

                function onPlayerError(event) {
                    if (window.AndroidInterface) {
                        window.AndroidInterface.onError(event.data);
                    }
                }

                // Window message listener fallback
                window.addEventListener('message', function(event) {
                    try {
                        var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                        if (data && data.event === 'onStateChange') {
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

                // Fallback: Notify Android that playback started after 1.8 seconds
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
