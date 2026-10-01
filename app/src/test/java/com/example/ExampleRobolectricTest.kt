package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.gemini.MahiPersona
import com.example.data.model.SassLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mahi AI", appName)
  }

  @Test
  fun `mahi persona prompt generates properly for all sass levels`() {
    for (level in SassLevel.values()) {
      val prompt = MahiPersona.getSystemPrompt(level)
      assertTrue(prompt.contains("Mahi"))
      assertTrue(prompt.contains("sassy"))
    }
  }

  @Test
  fun `mahi handles local quick tool requests`() {
    val (reply, tool) = MahiPersona.getLocalResponse("Mahi open YouTube", SassLevel.SASSY)
    assertNotNull(tool)
    assertTrue(tool!!.contains("openWebsite"))
    assertTrue(reply.contains("YouTube"))
  }
}
