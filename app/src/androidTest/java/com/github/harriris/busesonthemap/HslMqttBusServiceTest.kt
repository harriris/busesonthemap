package com.github.harriris.busesonthemap

import android.content.Intent
import android.os.IBinder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import com.github.harriris.busesonthemap.service.HslMqttBusService
import org.hamcrest.CoreMatchers.isA
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Units tests for the HslMqttBusService Service.
 */
@RunWith(AndroidJUnit4::class)
class HslMqttBusServiceTest {
    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun serviceBinding() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val serviceIntent = Intent(appContext, HslMqttBusService::class.java)
        val binder: IBinder = serviceRule.bindService(serviceIntent)
        val service: HslMqttBusService = (binder as HslMqttBusService.LocalBinder).getService()
        assertThat(service.getBusLines, isA(ArrayList::class.java))
        assertTrue(service.getBusLines.isEmpty())
    }
}
