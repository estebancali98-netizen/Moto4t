package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CheckStatus
import com.example.data.model.QualityChecklist
import com.example.data.model.QuoteItem
import com.example.data.model.WorkOrder
import com.example.data.model.WorkOrderStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    order: WorkOrder,
    onBackClick: () -> Unit,
    onStatusChange: (WorkOrderStatus) -> Unit,
    onToggleItemApproval: (String, Boolean) -> Unit,
    onAddQuoteItem: (QuoteItem) -> Unit,
    onAddPresetKit: (String) -> Unit,
    onToggleTimer: () -> Unit,
    onUpdateQualityCheck: (QualityChecklist, Int) -> Unit,
    onRecordPayment: (Double, String, Boolean) -> Unit,
    onGenerateWhatsApp: (String) -> String
) {
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Resumen & Tiempos, 1: Cotización, 2: Chequeo 10 Pts, 3: Control Calidad, 4: Pagos & Recibo

    // Add Item Dialog state
    var showAddItemDialog by remember { mutableStateOf(false) }
    var newItemDesc by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("1") }
    var newItemType by remember { mutableStateOf("REPUESTO") }
    var newItemIsRouteAddition by remember { mutableStateOf(false) }

    // Payment Dialog state
    var showPaymentDialog by remember { mutableStateOf(false) }
    var payAmountStr by remember { mutableStateOf(order.balanceDue.toInt().toString()) }
    var payMethod by remember { mutableStateOf("Efectivo") }
    var isAdvancePayment by remember { mutableStateOf(false) }

    // WhatsApp Message preview dialog
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var whatsAppText by remember { mutableStateOf("") }

    val tabs = listOf("Resumen", "Cotización", "Chequeo Express", "Control Calidad", "Caja & Recibo")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = order.id,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${order.plate}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                                )
                            }
                            Text(
                                text = order.motorcycleSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Share WhatsApp Icon
                    IconButton(
                        onClick = {
                            val msgType = when (order.status) {
                                WorkOrderStatus.RECEIVED, WorkOrderStatus.DIAGNOSIS -> "RECEIVED"
                                WorkOrderStatus.READY, WorkOrderStatus.DELIVERED -> "READY"
                                else -> "QUOTE"
                            }
                            whatsAppText = onGenerateWhatsApp(msgType)
                            showWhatsAppDialog = true
                        }
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "WhatsApp",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: RESUMEN Y CRONÓMETRO DE TIEMPOS
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Status Controller Row
                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Estado Actual de la Orden",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        WorkOrderStatus.values().forEach { st ->
                                            val isCurrent = order.status == st
                                            FilterChip(
                                                selected = isCurrent,
                                                onClick = { onStatusChange(st) },
                                                label = { Text(st.label) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Labor Productivity Timer Card
                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = if (order.isTimerRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Cronómetro de Labor de Mecánico",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        if (order.isTimerRunning) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = "EN CURSO",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Técnico: ${order.assignedMechanicName}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Tiempo Registrado: ${order.minutesWorked} minutos trabajados",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = onToggleTimer,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (order.isTimerRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("toggle_order_timer_button")
                                    ) {
                                        Icon(
                                            if (order.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (order.isTimerRunning) "Pausar Trabajo" else "Iniciar Trabajo")
                                    }
                                }
                            }
                        }

                        // Vehicle & Reception Details
                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Detalles de Recepción",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(text = "Propietario: ${order.clientName}", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Teléfono: ${order.clientPhone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Kilometraje inicial: ${order.initialMileage} km", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Nivel de gasolina: ${order.fuelLevel.label}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Fecha de ingreso: ${dateFormat.format(Date(order.entryTimestamp))}", style = MaterialTheme.typography.bodySmall)

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Falla Reportada:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = order.reportedIssue,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    if (order.visualDamages.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Inventario de Daños Previos:",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        order.visualDamages.forEach { d ->
                                            Text(text = "• $d", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (order.clientSignatureBase64.isNotBlank()) "✓ Firma digital del cliente archivada" else "Sin firma",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: COTIZACIÓN Y AUTORIZACIÓN (Módulo 5)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quick Presets Combos Row
                        item {
                            Column {
                                Text(
                                    text = "Agregar Kits y Combos Frecuentes (1 Clic)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(onClick = { onAddPresetKit("ARRAS") }) {
                                        Text("+ Kit Arrastre ($115.000)")
                                    }
                                    OutlinedButton(onClick = { onAddPresetKit("AFINA") }) {
                                        Text("+ Afinación Mayor ($85.000)")
                                    }
                                    OutlinedButton(onClick = { onAddPresetKit("ACEITE") }) {
                                        Text("+ Cambio Aceite ($65.000)")
                                    }
                                    OutlinedButton(onClick = { onAddPresetKit("FRENOS") }) {
                                        Text("+ Frenos Integral ($72.000)")
                                    }
                                }
                            }
                        }

                        // Add Custom Item / Part Button
                        item {
                            Button(
                                onClick = { showAddItemDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_quote_item_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Agregar Repuesto / Mano de Obra")
                            }
                        }

                        // Quote Total Card
                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Total Presupuesto Aprobado:", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = currencyFormat.format(order.totalApprovedQuote),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Anticipos Abonados:", style = MaterialTheme.typography.bodySmall)
                                        Text(text = currencyFormat.format(order.advancePayment), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Saldo Pendiente:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(
                                            text = currencyFormat.format(order.balanceDue),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (order.balanceDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }

                        // List of Quote Items
                        if (order.quoteItems.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No hay ítems en esta cotización.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(order.quoteItems) { item ->
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Customer Approval Toggle Checkbox
                                        Checkbox(
                                            checked = item.isApproved,
                                            onCheckedChange = { onToggleItemApproval(item.id, it) }
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.description,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                                if (item.isRouteAddition) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Adición en Ruta",
                                                            color = Color(0xFFD97706),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${item.type} • Cantidad: ${item.quantity} • Unitario: ${currencyFormat.format(item.unitPrice)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = currencyFormat.format(item.subtotal),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.isApproved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: CHEQUEO EXPRESS 10 PUNTOS
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Chequeo Preventivo de 10 Puntos",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        items(order.expressChecks) { chk ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (badgeColor, statusText) = when (chk.status) {
                                        CheckStatus.OK -> MaterialTheme.colorScheme.tertiary to "Bueno"
                                        CheckStatus.REGULAR -> Color(0xFFF59E0B) to "Regular"
                                        CheckStatus.BAD -> MaterialTheme.colorScheme.error to "Requiere Cambio"
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = badgeColor,
                                        modifier = Modifier.size(12.dp)
                                    ) {}

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = chk.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        if (chk.notes.isNotBlank()) {
                                            Text(
                                                text = chk.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: CONTROL DE CALIDAD Y GARANTÍA (Paso 5)
                    var qcRoadTest by remember { mutableStateOf(order.qualityChecklist.roadTestOk) }
                    var qcBolts by remember { mutableStateOf(order.qualityChecklist.boltsTorqued) }
                    var qcCleaned by remember { mutableStateOf(order.qualityChecklist.cleaned) }
                    var qcTires by remember { mutableStateOf(order.qualityChecklist.tirePressureOk) }
                    var qcOldParts by remember { mutableStateOf(order.qualityChecklist.oldPartsReturned) }
                    var warrantyDaysStr by remember { mutableStateOf(order.warrantyDays.toString()) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Control de Calidad Pre-Entrega (5 Puntos)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Verificación estricta antes de marcar la motocicleta como 'Lista':",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = qcRoadTest, onCheckedChange = { qcRoadTest = it })
                                        Text("1. Prueba de ruta realizada con encendido y frenado satisfactorio")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = qcBolts, onCheckedChange = { qcBolts = it })
                                        Text("2. Tornillería y ejes calibrados con torquímetro")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = qcCleaned, onCheckedChange = { qcCleaned = it })
                                        Text("3. Limpieza y desengrase de cadena / partes intervenidas")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = qcTires, onCheckedChange = { qcTires = it })
                                        Text("4. Calibración de presión de llantas según manual")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = qcOldParts, onCheckedChange = { qcOldParts = it })
                                        Text("5. Repuestos viejos empacados en bolsa para entrega al cliente")
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = warrantyDaysStr,
                                onValueChange = { warrantyDaysStr = it },
                                label = { Text("Días de Garantía Otorgada") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        item {
                            Button(
                                onClick = {
                                    val updatedQc = QualityChecklist(
                                        roadTestOk = qcRoadTest,
                                        boltsTorqued = qcBolts,
                                        cleaned = qcCleaned,
                                        tirePressureOk = qcTires,
                                        oldPartsReturned = qcOldParts
                                    )
                                    onUpdateQualityCheck(updatedQc, warrantyDaysStr.toIntOrNull() ?: 30)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_quality_check_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Guardar Control de Calidad y Marcar Lista")
                            }
                        }
                    }
                }

                4 -> {
                    // TAB 4: FACTURACIÓN, CAJA Y RECIBO (Módulo 8)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Estado Financiero de la Orden",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Total Cotizado:", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = currencyFormat.format(order.totalApprovedQuote),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Pagado / Abonado:", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = currencyFormat.format(order.paidAmount),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Saldo por Cobrar:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(
                                            text = currencyFormat.format(order.balanceDue),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                            color = if (order.balanceDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    Text(
                                        text = "Método registrado: ${order.paymentMethod}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (order.balanceDue > 0) {
                                        Button(
                                            onClick = {
                                                payAmountStr = order.balanceDue.toInt().toString()
                                                isAdvancePayment = false
                                                showPaymentDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("record_payment_button")
                                        ) {
                                            Icon(Icons.Default.LocalAtm, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Registrar Pago / Liquidar Saldo")
                                        }
                                    } else {
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Orden completamente liquidada",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Generated Receipt Card
                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Comprobante de Entrega",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Icon(Icons.Default.Receipt, contentDescription = null)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "Taller: MotoTaller Pro", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Orden: #${order.id} • Placa: ${order.plate}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Cliente: ${order.clientName}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Garantía: ${order.warrantyDays} días o ${order.warrantyKm} km", style = MaterialTheme.typography.bodySmall)

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    order.quoteItems.filter { it.isApproved }.forEach {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "${it.description} x${it.quantity}", style = MaterialTheme.typography.bodySmall)
                                            Text(text = currencyFormat.format(it.subtotal), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "TOTAL CANCELADO:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(text = currencyFormat.format(order.paidAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Quote Item
    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Agregar Ítem a Cotización") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newItemDesc,
                        onValueChange = { newItemDesc = it },
                        label = { Text("Descripción del Repuesto o Servicio *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newItemPrice,
                            onValueChange = { newItemPrice = it },
                            label = { Text("Precio Unitario ($) *") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newItemQty,
                            onValueChange = { newItemQty = it },
                            label = { Text("Cantidad") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("REPUESTO", "MANO_OBRA").forEach { type ->
                            FilterChip(
                                selected = newItemType == type,
                                onClick = { newItemType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = newItemIsRouteAddition,
                            onCheckedChange = { newItemIsRouteAddition = it }
                        )
                        Text("Adición en ruta (descubierto durante desarme)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = newItemPrice.toDoubleOrNull() ?: 0.0
                        val qty = newItemQty.toIntOrNull() ?: 1
                        if (newItemDesc.isNotBlank() && price > 0) {
                            onAddQuoteItem(
                                QuoteItem(
                                    id = "item_${System.currentTimeMillis()}",
                                    description = newItemDesc.trim(),
                                    type = newItemType,
                                    quantity = qty,
                                    unitPrice = price,
                                    isApproved = true,
                                    isRouteAddition = newItemIsRouteAddition
                                )
                            )
                            newItemDesc = ""
                            newItemPrice = ""
                            showAddItemDialog = false
                        }
                    }
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Record Payment
    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Registrar Recaudo en Caja") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = payAmountStr,
                        onValueChange = { payAmountStr = it },
                        label = { Text("Monto a Pagar ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Método de Pago:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Efectivo", "Nequi", "Daviplata", "Tarjeta", "Transferencia").forEach { method ->
                            FilterChip(
                                selected = payMethod == method,
                                onClick = { payMethod = method },
                                label = { Text(method) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payAmountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            onRecordPayment(amount, payMethod, isAdvancePayment)
                            showPaymentDialog = false
                        }
                    }
                ) {
                    Text("Confirmar Recaudo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: WhatsApp Share Message Preview
    if (showWhatsAppDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsAppDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mensaje para WhatsApp")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Mensaje generado automáticamente:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = whatsAppText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = Uri.parse("https://wa.me/57${order.clientPhone.trim()}?text=${Uri.encode(whatsAppText)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                        showWhatsAppDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enviar por WhatsApp")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
