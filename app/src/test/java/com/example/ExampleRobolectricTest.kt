package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
    fun `qr yuv camera frame decode succeeds`() {
        val payload = "https://play.google.com/store/apps"
        val bitmap = QrCodeEngine.renderExportBitmap(payload, QrErrorCorrection.M, targetPixels = 400)!!
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()

        val yPlane = ByteArray(w * h) { idx ->
            val c = pixels[idx]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            ((0.299 * r + 0.587 * g + 0.114 * b).toInt()).coerceIn(0, 255).toByte()
        }
        val decoded = QrCodeEngine.decodeYuvFrame(yPlane, w, h)
        assertEquals(payload, decoded)
    }

    @Test
    fun `main tabs and live language switching work cleanly`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("scan_camera_button").assertIsDisplayed()
        composeRule.onNodeWithTag("scan_gallery_button").assertIsDisplayed()
        composeRule.onNodeWithTag("scan_clipboard_button").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_tab_generate").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("generate_input_field").assertIsDisplayed()
        composeRule.onNodeWithTag("generate_input_field").performTextInput("https://qrcode.app")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Length: 18").assertIsDisplayed()
        composeRule.onNodeWithTag("qr_code_canvas").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_tab_history").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("history_clear_button").assertIsDisplayed()
        composeRule.onNodeWithTag("history_pause_button").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()

        // Switch language to Simplified Chinese in-app and verify immediate update
        composeRule.onNodeWithTag("settings_lang_zh_cn").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("界面语言").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        composeRule.onNodeWithTag("nav_tab_generate").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("长度：18").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // Switch language to Japanese and verify immediate update
        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_lang_ja").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("表示言語").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }
}
