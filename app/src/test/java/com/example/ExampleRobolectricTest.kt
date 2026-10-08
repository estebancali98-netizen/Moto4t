package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WorkshopConverters
import com.example.data.model.CheckStatus
import com.example.data.model.ExpressCheckItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MotoTaller Pro", appName)
  }

  @Test
  fun testWorkshopConverters() {
    val checks = listOf(
      ExpressCheckItem("chk_1", "Luces", CheckStatus.OK, "Buen estado"),
      ExpressCheckItem("chk_2", "Frenos", CheckStatus.BAD, "Pastillas gastadas")
    )

    val json = WorkshopConverters.expressChecksToJson(checks)
    val parsed = WorkshopConverters.jsonToExpressChecks(json)

    assertEquals(2, parsed.size)
    assertEquals(CheckStatus.OK, parsed[0].status)
    assertEquals(CheckStatus.BAD, parsed[1].status)
  }
}

