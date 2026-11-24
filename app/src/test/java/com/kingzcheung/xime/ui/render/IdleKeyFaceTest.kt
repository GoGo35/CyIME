package com.kingzcheung.xime.ui.render

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingzcheung.xime.ui.keyboard.KeyFlickFade
import com.kingzcheung.xime.ui.keyboard.KeyFlickOrigin
import com.kingzcheung.xime.ui.keyboard.KeyFlickPreview
import com.kingzcheung.xime.ui.keyboard.rememberKeyFlickFade
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class IdleKeyFaceTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun untouchedLettersShareIdleFaceAndReleaseDropsThePreviousGesture() {
        var gesture by mutableStateOf<KeyFlickPreview?>(null)
        lateinit var faces: List<KeyFlickFade>
        rule.setContent {
            val row = (0 until 26).map { rememberKeyFlickFade(if (it == 0) gesture else null) }
            SideEffect { faces = row }
        }
        fun checkIdle() {
            assertTrue(faces.all { it === KeyFlickFade.Idle })
            assertEquals(0f, faces.first().strongest, 0f)
            assertTrue(faces.first().labels().isEmpty())
        }
        rule.runOnIdle { checkIdle(); gesture = KeyFlickPreview("1", .6f, KeyFlickOrigin.TOP) }
        rule.runOnIdle {
            assertEquals(listOf(gesture), faces.first().labels()); assertTrue(faces.drop(1).all { it === KeyFlickFade.Idle })
            gesture = KeyFlickPreview("!", .8f, KeyFlickOrigin.BOTTOM)
        }
        rule.runOnIdle { assertEquals(listOf(gesture), faces.first().labels()); gesture = null }
        rule.runOnIdle { checkIdle(); gesture = KeyFlickPreview("?", .4f, KeyFlickOrigin.LEFT) }
        rule.runOnIdle { assertEquals(listOf(gesture), faces.first().labels()) }
    }
}
