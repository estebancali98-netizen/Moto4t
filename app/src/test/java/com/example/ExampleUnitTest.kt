package com.example

import com.example.data.local.WorkshopConverters
import com.example.data.model.CheckStatus
import com.example.data.model.ExpressCheckItem
import com.example.data.model.QualityChecklist
import com.example.data.model.QuoteItem
import com.example.data.model.WorkOrder
import com.example.data.model.WorkOrderStatus
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testQuoteCalculations() {
    val items = listOf(
      QuoteItem("1", "Aceite Motul 5100", "REPUESTO", 1, 48000.0, isApproved = true),
      QuoteItem("2", "Kit Arrastre Choho", "REPUESTO", 1, 75000.0, isApproved = true),
      QuoteItem("3", "Filtro Aire", "REPUESTO", 1, 16000.0, isApproved = false)
    )

    val order = WorkOrder(
      id = "OT-101",
      motorcycleId = "moto_1",
      plate = "ABC-12D",
      motorcycleSummary = "AK125 NKD",
      clientName = "Carlos",
      clientPhone = "3124567890",
      reportedIssue = "Mantenimiento",
      initialMileage = 18500,
      quoteItems = items,
      paidAmount = 50000.0
    )

    assertEquals(123000.0, order.totalApprovedQuote, 0.01)
    assertEquals(73000.0, order.balanceDue, 0.01)
    assertFalse(order.isFullyPaid)
  }

  @Test
  fun testQualityChecklist() {
    val incomplete = QualityChecklist(roadTestOk = true, boltsTorqued = true)
    assertFalse(incomplete.isFullyCompleted)

    val complete = QualityChecklist(
      roadTestOk = true,
      boltsTorqued = true,
      cleaned = true,
      tirePressureOk = true,
      oldPartsReturned = true
    )
    assertTrue(complete.isFullyCompleted)
  }
}

