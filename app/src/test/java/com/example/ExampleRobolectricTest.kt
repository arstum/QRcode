package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.QrErrorCorrection
import com.example.qr.QrCodeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name matches QRcode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("QRcode", appName)
    }

    @Test
    fun `qr encode and decode roundtrip succeeds`() {
        val payload = "https://example.org/qrcode-test?v=15"
        val bitmap = QrCodeEngine.renderExportBitmap(payload, QrErrorCorrection.M, targetPixels = 512)
        assertNotNull(bitmap)
        val decoded = QrCodeEngine.decodeBitmap(bitmap!!)
        assertEquals(payload, decoded)
        bitmap.recycle()
    }

    @Test
    fun `main tabs and qr generation UI render and switch cleanly`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("generate_input_field").assertIsDisplayed()
        composeRule.onNodeWithTag("generate_input_field").performTextInput("https://qrcode.app")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("长度：18").assertIsDisplayed()
        composeRule.onNodeWithTag("qr_code_canvas").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_tab_scan").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("scan_camera_button").assertIsDisplayed()
        composeRule.onNodeWithTag("scan_gallery_button").assertIsDisplayed()
        composeRule.onNodeWithTag("scan_clipboard_button").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_tab_history").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("history_clear_button").assertIsDisplayed()
        composeRule.onNodeWithTag("history_pause_button").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_dark_inverted_checkbox").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_sound_checkbox").assertIsDisplayed()
    }
}
