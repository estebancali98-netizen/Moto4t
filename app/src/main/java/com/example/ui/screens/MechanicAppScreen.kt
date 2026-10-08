package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUser
import com.example.data.model.CheckStatus
import com.example.data.model.WorkOrder
import com.example.data.model.WorkOrderStatus
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MechanicAppScreen(
    currentMechanic: AppUser,
    orders: List<WorkOrder>,
    onToggleTimer: (String) -> Unit,
    onAddPhotoDamagedPart: (orderId: String, note: String) -> Unit,
    onAdvanceOrder: (orderId: String, newStatus: WorkOrderStatus) -> Unit,
    onOrderClick: (String) -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }

    // Filter orders assigned to this mechanic or show all active if mechanic has none
    val myOrders = orders.filter {
        it.assignedMechanicId == currentMechanic.id || it.assignedMechanicName.contains(currentMechanic.name.split(" ").first(), ignoreCase = true)
    }.ifEmpty {
        orders.filter { it.status != WorkOrderStatus.DELIVERED }
    }

    // Productivity metrics
    val totalMinutes = myOrders.sumOf { it.minutesWorked }
    val totalHours = String.format(Locale.US, "%.1f", totalMinutes / 60.0)
    val estimatedCommission = myOrders.sumOf { order ->
        val laborTotal = order.quoteItems.filter { it.type == "MANO_OBRA" && it.isApproved }.sumOf { it.subtotal }
        laborTotal * 0.40 // 40% labor commission standard
    }

    var selectedOrderForPhoto by remember { mutableStateOf<WorkOrder?>(null) }
    var photoNoteInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Mechanic Top Badge Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "MODO TALLER",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Manos en Obra",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentMechanic.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }

                    // Productivity Chip
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$totalHours hrs",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currencyFormat.format(estimatedCommission),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }

        // List of Assigned Motorcycles
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("mechanic_orders_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Motos en mi Banco de Trabajo (${myOrders.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(myOrders, key = { it.id }) { order ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mechanic_card_${order.id}"),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (order.isTimerRunning) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Big Plate & ID banner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = order.plate,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${order.id} • ${order.motorcycleSummary}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            // Big status pill
                            val stColor = when (order.status) {
                                WorkOrderStatus.IN_REPAIR -> MaterialTheme.colorScheme.primary
                                WorkOrderStatus.WAITING_PARTS -> Color(0xFF8B5CF6)
                                WorkOrderStatus.READY -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.secondary
                            }
                            Surface(
                                color = stColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = order.status.label,
                                    color = stColor,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // High contrast reported problem box
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Falla Reportada por Cliente:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = order.reportedIssue,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Tasks to do list (Express Check Bad items or Quote tasks)
                        val badChecks = order.expressChecks.filter { it.status == CheckStatus.BAD || it.status == CheckStatus.REGULAR }
                        if (badChecks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Puntos críticos identificados en chequeo express:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            badChecks.forEach { chk ->
                                Text(
                                    text = "• ${chk.label} (${if (chk.status == CheckStatus.BAD) "Malo" else "Regular"})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (chk.status == CheckStatus.BAD) MaterialTheme.colorScheme.error else Color(0xFFF59E0B)
                                )
                            }
                        }

                        // Damaged parts documented
                        if (order.photoNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Evidencias y piezas documentadas:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            order.photoNotes.forEach { note ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = note, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // LARGE ACTION BUTTONS (Designed for dirty hands/gloves)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Start/Pause Timer Giant Button
                            Button(
                                onClick = { onToggleTimer(order.id) },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(54.dp)
                                    .testTag("mechanic_timer_btn_${order.id}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (order.isTimerRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    if (order.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (order.isTimerRunning) "Pausar (${order.minutesWorked}m)" else "Trabajar (${order.minutesWorked}m)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Document Damage / Camera Button
                            Button(
                                onClick = {
                                    selectedOrderForPhoto = order
                                    photoNoteInput = ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("mechanic_photo_btn_${order.id}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Evidencia",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Secondary action: Mark ready or open full sheet
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { onOrderClick(order.id) }) {
                                Text("Ver Ficha Completa")
                            }

                            if (order.status == WorkOrderStatus.IN_REPAIR) {
                                Button(
                                    onClick = { onAdvanceOrder(order.id, WorkOrderStatus.READY) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Terminada (Lista)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Document Damaged Part
    if (selectedOrderForPhoto != null) {
        AlertDialog(
            onDismissRequest = { selectedOrderForPhoto = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Documentar Pieza Dañada")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Orden: ${selectedOrderForPhoto?.id} (${selectedOrderForPhoto?.plate})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Describe el estado de la pieza averiada encontrada en el desarme:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = photoNoteInput,
                        onValueChange = { photoNoteInput = it },
                        label = { Text("Ej: Corona de arrastre con dientes afilados y cadena gripada") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = selectedOrderForPhoto
                        if (current != null && photoNoteInput.isNotBlank()) {
                            onAddPhotoDamagedPart(current.id, photoNoteInput.trim())
                            selectedOrderForPhoto = null
                        }
                    }
                ) {
                    Text("Guardar Evidencia")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedOrderForPhoto = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
