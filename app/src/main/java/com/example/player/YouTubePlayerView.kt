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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    val currentOnStarted by rememberUpdatedState(onPlaybackStarted)
    val currentOnError by rememberUpdatedState(onPlaybackError)

    val webView = remember(context) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)

            // Enable hardware acceleration for smooth video rendering on Android TV
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

            // CRITICAL FOR ANDROID TV:
            // Disable D-pad/keyboard focus on the WebView so it does NOT hijack TV remote navigation
            // from the Compose UI (channel cards, tabs, and buttons). Mobile touch gestures still function.
            isFocusable = false
            isFocusableInTouchMode = false

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
                // Critical: allows unmuted autoplay without user gesture in Android WebView
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true
                useWideViewPort = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                allowFileAccess = true
                allowContentAccess = true
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = true
                try {
                    userAgentString = WebSettings.getDefaultUserAgent(context)
                } catch (e: Exception) {}
            }

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        val fallbackHtml = buildYouTubeHtml(videoId)
                        view?.loadDataWithBaseURL(
                            "https://bcs24admin.org.bd",
                            fallbackHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: android.webkit.WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                        val fallbackHtml = buildYouTubeHtml(videoId)
                        view?.loadDataWithBaseURL(
                            "https://bcs24admin.org.bd",
                            fallbackHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    try {
                        CookieManager.getInstance().flush()
                    } catch (e: Exception) {}

                    // Inject runtime audio & true-PLAYING monitor:
                    // 1. Overrides any server-cached player.mute() call so channels NEVER play muted.
                    // 2. Forces unMute() + setVolume(100).
                    // 3. Signals onActualVideoPlaying() ONLY when YT.PlayerState.PLAYING (state === 1)
                    //    is reached and video frames are actively rendering (eliminating black-screen flash).
                    val monitorScript = """
                        (function() {
                            if (window.__nokshiMonitorInstalled) return;
                            window.__nokshiMonitorInstalled = true;
                            var actualNotified = false;
                            var ticks = 0;
                            var timer = setInterval(function() {
                                ticks++;
                                try {
                                    if (window.YT && window.YT.Player && window.YT.Player.prototype) {
                                        window.YT.Player.prototype.mute = function() {
                                            try { this.unMute(); this.setVolume(100); } catch(e) {}
                                        };
                                    }
                                    if (typeof player !== 'undefined' && player) {
                                        if (typeof player.isMuted === 'function' && player.isMuted()) {
                                            player.unMute();
                                        }
                                        if (typeof player.setVolume === 'function') {
                                            player.setVolume(100);
                                        }
                                        if (typeof player.getPlayerState === 'function') {
                                            var st = player.getPlayerState();
                                            if (st === 1 && !actualNotified) {
                                                actualNotified = true;
                                                setTimeout(function() {
                                                    try {
                                                        if (player.isMuted && player.isMuted()) player.unMute();
                                                        if (player.setVolume) player.setVolume(100);
                                                    } catch(e) {}
                                                    if (window.AndroidBridge && window.AndroidBridge.onActualVideoPlaying) {
                                                        window.AndroidBridge.onActualVideoPlaying();
                                                    }
                                                }, 320);
                                            }
                                        }
                                    }
                                } catch(e) {}
                                if (actualNotified && ticks > 40) {
                                    clearInterval(timer);
                                }
                            }, 200);
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(monitorScript, null)
                }
            }

            val bridgeObject = object {
                @JavascriptInterface
                fun onActualVideoPlaying() {
                    mainHandler.post { currentOnStarted() }
                }

                @JavascriptInterface
                fun onYoutubeStarted() {
                    // Server's older youtube_embed.html may fire onYoutubeStarted on BUFFERING (3).
                    // Verify via JS whether state === 1 (PLAYING) before dismissing splash!
                    mainHandler.post {
                        evaluateJavascript(
                            "(function(){ try { if (typeof player !== 'undefined' && player) { if (player.unMute) player.unMute(); if (player.setVolume) player.setVolume(100); return player.getPlayerState(); } } catch(e){} return -1; })()"
                        ) { result ->
                            if (result?.trim() == "1") {
                                mainHandler.postDelayed({ currentOnStarted() }, 280)
                            }
                        }
                    }
                }

                @JavascriptInterface
                fun onYoutubeError(errorCode: Int) {
                    mainHandler.post { currentOnError("YouTube error code: $errorCode") }
                }

                @JavascriptInterface
                fun onPlaying() {
                    mainHandler.post {
                        evaluateJavascript(
                            "(function(){ try { if (typeof player !== 'undefined' && player) { if (player.unMute) player.unMute(); if (player.setVolume) player.setVolume(100); return player.getPlayerState(); } } catch(e){} return 1; })()"
                        ) { result ->
                            if (result?.trim() == "1") {
                                mainHandler.postDelayed({ currentOnStarted() }, 280)
                            }
                        }
                    }
                }

                @JavascriptInterface
                fun onBuffering() {
                    // Keep splash visible while buffering so no black screen is shown!
                }

                @JavascriptInterface
                fun onError(errorCode: Int) {
                    mainHandler.post { currentOnError("YouTube error code: $errorCode") }
                }
            }

            addJavascriptInterface(bridgeObject, "AndroidBridge")
            addJavascriptInterface(bridgeObject, "AndroidInterface")
        }
    }

    LaunchedEffect(videoId) {
        if (videoId.isNotEmpty()) {
            val serverEmbedUrl = "https://bcs24admin.org.bd/tv/youtube_embed.html?id=$videoId"
            webView.loadUrl(serverEmbedUrl)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mainHandler.removeCallbacksAndMessages(null)
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
                    src="https://www.youtube.com/embed/$videoId?autoplay=1&mute=0&enablejsapi=1&playsinline=1&controls=1&rel=0&modestbranding=1&origin=$originDomain&widget_referrer=$originDomain"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share; fullscreen"
                    allowfullscreen>
                </iframe>
            </div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player;
                var notified = false;
                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('ytplayer', {
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange,
                            'onError': onPlayerError
                        }
                    });
                }

                function ensureSound(p) {
                    try {
                        if (p && p.unMute) p.unMute();
                        if (p && p.setVolume) p.setVolume(100);
                    } catch(e) {}
                }

                function onPlayerReady(event) {
                    ensureSound(event.target);
                    try {
                        event.target.playVideo();
                    } catch(e) {}

                    setTimeout(function() {
                        if (player && !notified) {
                            try {
                                ensureSound(player);
                                if (player.getPlayerState() !== 1) {
                                    player.playVideo();
                                }
                            } catch(e) {}
                        }
                    }, 2000);
                }

                function onPlayerStateChange(event) {
                    // Strictly 1 = PLAYING (never dismiss splash on 3 = BUFFERING)
                    if (event.data === 1) {
                        ensureSound(event.target);
                        if (!notified) {
                            notified = true;
                            setTimeout(function() {
                                ensureSound(event.target);
                                if (window.AndroidBridge && window.AndroidBridge.onActualVideoPlaying) {
                                    window.AndroidBridge.onActualVideoPlaying();
                                }
                            }, 300);
                        }
                    }
                }

                function onPlayerError(event) {
                    if (window.AndroidBridge && window.AndroidBridge.onYoutubeError) {
                        window.AndroidBridge.onYoutubeError(event.data);
                    }
                }

                window.addEventListener('message', function(event) {
                    try {
                        var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                        if (data && data.event === 'onStateChange' && data.info === 1 && !notified) {
                            notified = true;
                            setTimeout(function() {
                                if (window.AndroidBridge && window.AndroidBridge.onActualVideoPlaying) {
                                    window.AndroidBridge.onActualVideoPlaying();
                                }
                            }, 300);
                        } else if (data && data.event === 'onError') {
                            if (window.AndroidBridge && window.AndroidBridge.onYoutubeError) {
                                window.AndroidBridge.onYoutubeError(data.info);
                            }
                        }
                    } catch (e) {}
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}
