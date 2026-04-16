package com.monospace.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.monospace.app", appContext.packageName)
    }
}

//Quy tắc:
//- src/test/ — pure JVM, không dùng Android API, chạy bằng ./gradlew testDebugUnitTest
//- src/androidTest/ — cần Android runtime (Room, Context...), chạy bằng ./gradlew connectedDebugAndroidTest