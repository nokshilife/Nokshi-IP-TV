package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Channel
import com.example.data.model.ChannelType
import com.example.data.remote.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nokshi IP TV", appName)
    }

    @Test
    fun `extract youtube video id test`() {
        val ch = Channel(
            id = 1,
            type = ChannelType.YOUTUBE,
            name = "Channel 24",
            streamUrl = "https://www.youtube.com/watch?v=p6EO0TuILpw"
        )
        assertEquals("p6EO0TuILpw", ch.getYouTubeVideoId())
    }

    @Test
    fun `parse m3u content test`() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="btv" tvg-name="BTV" tvg-logo="https://example.com/btv.png" group-title="Bangla",BTV World
            https://example.com/live/btv.m3u8
        """.trimIndent()

        val parser = M3uParser()
        val channels = parser.parseM3uContent(sampleM3u)

        assertEquals(1, channels.size)
        val channel = channels.first()
        assertEquals("BTV World", channel.name)
        assertEquals("Bangla", channel.groupTitle)
        assertEquals("https://example.com/live/btv.m3u8", channel.streamUrl)
        assertEquals("https://example.com/btv.png", channel.poster)
    }
}
