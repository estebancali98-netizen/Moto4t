package com.example.data.local

import com.example.data.model.CheckStatus
import com.example.data.model.ExpressCheckItem
import com.example.data.model.FuelLevel
import com.example.data.model.QualityChecklist
import com.example.data.model.QuoteItem
import com.example.data.model.WorkOrder
import com.example.data.model.WorkOrderStatus
import org.json.JSONArray
import org.json.JSONObject

object WorkshopConverters {

    fun expressChecksToJson(items: List<ExpressCheckItem>): String {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("label", item.label)
            obj.put("status", item.status.name)
            obj.put("notes", item.notes)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun jsonToExpressChecks(json: String): List<ExpressCheckItem> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<ExpressCheckItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ExpressCheckItem(
                        id = obj.optString("id", "check_$i"),
                        label = obj.optString("label", ""),
                        status = try { CheckStatus.valueOf(obj.optString("status", "OK")) } catch (_: Exception) { CheckStatus.OK },
                        notes = obj.optString("notes", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun quoteItemsToJson(items: List<QuoteItem>): String {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("description", item.description)
            obj.put("type", item.type)
            obj.put("quantity", item.quantity)
            obj.put("unitPrice", item.unitPrice)
            obj.put("isApproved", item.isApproved)
            obj.put("isRouteAddition", item.isRouteAddition)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun jsonToQuoteItems(json: String): List<QuoteItem> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<QuoteItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    QuoteItem(
                        id = obj.optString("id", "quote_$i"),
                        description = obj.optString("description", ""),
                        type = obj.optString("type", "REPUESTO"),
                        quantity = obj.optInt("quantity", 1),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        isApproved = obj.optBoolean("isApproved", true),
                        isRouteAddition = obj.optBoolean("isRouteAddition", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun qualityChecklistToJson(q: QualityChecklist): String {
        val obj = JSONObject()
        obj.put("roadTestOk", q.roadTestOk)
        obj.put("boltsTorqued", q.boltsTorqued)
        obj.put("cleaned", q.cleaned)
        obj.put("tirePressureOk", q.tirePressureOk)
        obj.put("oldPartsReturned", q.oldPartsReturned)
        return obj.toString()
    }

    fun jsonToQualityChecklist(json: String): QualityChecklist {
        if (json.isBlank()) return QualityChecklist()
        return try {
            val obj = JSONObject(json)
            QualityChecklist(
                roadTestOk = obj.optBoolean("roadTestOk", false),
                boltsTorqued = obj.optBoolean("boltsTorqued", false),
                cleaned = obj.optBoolean("cleaned", false),
                tirePressureOk = obj.optBoolean("tirePressureOk", false),
                oldPartsReturned = obj.optBoolean("oldPartsReturned", false)
            )
        } catch (_: Exception) {
            QualityChecklist()
        }
    }

    fun stringListToJson(list: List<String>): String {
        val arr = JSONArray()
        for (s in list) {
            arr.put(s)
        }
        return arr.toString()
    }

    fun jsonToStringList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }

    fun workOrderToEntity(w: WorkOrder): WorkOrderEntity {
        return WorkOrderEntity(
            id = w.id,
            motorcycleId = w.motorcycleId,
            plate = w.plate,
            motorcycleSummary = w.motorcycleSummary,
            clientName = w.clientName,
            clientPhone = w.clientPhone,
            reportedIssue = w.reportedIssue,
            initialMileage = w.initialMileage,
            fuelLevel = w.fuelLevel.name,
            visualDamagesJson = stringListToJson(w.visualDamages),
            clientSignatureBase64 = w.clientSignatureBase64,
            expressChecksJson = expressChecksToJson(w.expressChecks),
            status = w.status.name,
            assignedMechanicId = w.assignedMechanicId,
            assignedMechanicName = w.assignedMechanicName,
            entryTimestamp = w.entryTimestamp,
            estimatedDeliveryTimestamp = w.estimatedDeliveryTimestamp,
            actualDeliveryTimestamp = w.actualDeliveryTimestamp,
            workStartTime = w.workStartTime,
            workEndTime = w.workEndTime,
            minutesWorked = w.minutesWorked,
            isTimerRunning = w.isTimerRunning,
            quoteItemsJson = quoteItemsToJson(w.quoteItems),
            advancePayment = w.advancePayment,
            paidAmount = w.paidAmount,
            paymentMethod = w.paymentMethod,
            qualityChecklistJson = qualityChecklistToJson(w.qualityChecklist),
            warrantyDays = w.warrantyDays,
            warrantyKm = w.warrantyKm,
            photoNotesJson = stringListToJson(w.photoNotes)
        )
    }

    fun entityToWorkOrder(e: WorkOrderEntity): WorkOrder {
        return WorkOrder(
            id = e.id,
            motorcycleId = e.motorcycleId,
            plate = e.plate,
            motorcycleSummary = e.motorcycleSummary,
            clientName = e.clientName,
            clientPhone = e.clientPhone,
            reportedIssue = e.reportedIssue,
            initialMileage = e.initialMileage,
            fuelLevel = try { FuelLevel.valueOf(e.fuelLevel) } catch (_: Exception) { FuelLevel.HALF },
            visualDamages = jsonToStringList(e.visualDamagesJson),
            clientSignatureBase64 = e.clientSignatureBase64,
            expressChecks = jsonToExpressChecks(e.expressChecksJson),
            status = try { WorkOrderStatus.valueOf(e.status) } catch (_: Exception) { WorkOrderStatus.RECEIVED },
            assignedMechanicId = e.assignedMechanicId,
            assignedMechanicName = e.assignedMechanicName,
            entryTimestamp = e.entryTimestamp,
            estimatedDeliveryTimestamp = e.estimatedDeliveryTimestamp,
            actualDeliveryTimestamp = e.actualDeliveryTimestamp,
            workStartTime = e.workStartTime,
            workEndTime = e.workEndTime,
            minutesWorked = e.minutesWorked,
            isTimerRunning = e.isTimerRunning,
            quoteItems = jsonToQuoteItems(e.quoteItemsJson),
            advancePayment = e.advancePayment,
            paidAmount = e.paidAmount,
            paymentMethod = e.paymentMethod,
            qualityChecklist = jsonToQualityChecklist(e.qualityChecklistJson),
            warrantyDays = e.warrantyDays,
            warrantyKm = e.warrantyKm,
            photoNotes = jsonToStringList(e.photoNotesJson)
        )
    }
}
