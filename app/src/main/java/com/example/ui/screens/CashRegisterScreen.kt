package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CashEntry
import com.example.data.model.CashEntryType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashRegisterScreen(
    cashEntries: List<CashEntry>,
    onAddExpense: (Double, String, String, String) -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }
    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    var showExpenseDialog by remember { mutableStateOf(false) }
    var showCloseRegisterDialog by remember { mutableStateOf(false) }

    var expenseAmountStr by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("Insumos de Taller") }
    var expenseDesc by remember { mutableStateOf("") }
    var expenseMethod by remember { mutableStateOf("Efectivo") }

    val totalIncome = cashEntries.filter { it.type == CashEntryType.INCOME }.sumOf { it.amount }
    val totalExpense = cashEntries.filter { it.type == CashEntryType.EXPENSE }.sumOf { it.amount }
    val netCashBalance = totalIncome - totalExpense

    val cashByMethod = cashEntries.groupBy { it.paymentMethod }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cash_register_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title & Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gestión de Caja Diaria",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Control de ingresos, gastos menores y conciliación",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showCloseRegisterDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cierre de Caja")
                }
            }
        }

        // Summary Balance Cards
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Saldo Actual en Caja",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currencyFormat.format(netCashBalance),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(6.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "Total Ingresos", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = currencyFormat.format(totalIncome),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(6.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "Gastos Menores", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = currencyFormat.format(totalExpense),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showExpenseDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Registrar Gasto de Caja Menor (Insumos/Gasolina)")
                    }
                }
            }
        }

        // Breakdown by Payment Method
        item {
            Text(
                text = "Conciliación por Método de Pago",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Efectivo", "Nequi", "Daviplata", "Tarjeta", "Transferencia").forEach { method ->
                    val methodEntries = cashByMethod[method] ?: emptyList()
                    val totalForMethod = methodEntries.sumOf { if (it.type == CashEntryType.INCOME) it.amount else -it.amount }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = method,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currencyFormat.format(totalForMethod),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${methodEntries.size} transacciones",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Live Cash Movement Log
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Movimientos de la Jornada",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(cashEntries, key = { it.id }) { entry ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isIncome = entry.type == CashEntryType.INCOME
                    Surface(
                        shape = CircleShape,
                        color = if (isIncome) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.category,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = entry.paymentMethod,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${entry.description} • Por: ${entry.registeredBy}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(Date(entry.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Text(
                        text = "${if (isIncome) "+" else "-"} ${currencyFormat.format(entry.amount)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    }

    // Dialog: Add Minor Workshop Expense
    if (showExpenseDialog) {
        AlertDialog(
            onDismissRequest = { showExpenseDialog = false },
            title = { Text("Registrar Gasto de Caja Menor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = expenseAmountStr,
                        onValueChange = { expenseAmountStr = it },
                        label = { Text("Monto del Gasto ($) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expenseDesc,
                        onValueChange = { expenseDesc = it },
                        label = { Text("Descripción (Ej: Desengrasante, Gasolina pruebas) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Insumos de Taller", "Gasolina Pruebas", "Repuesto Urgente", "Refrigerio/Agua").forEach { cat ->
                            FilterChip(
                                selected = expenseCategory == cat,
                                onClick = { expenseCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = expenseAmountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0 && expenseDesc.isNotBlank()) {
                            onAddExpense(amount, expenseCategory, expenseDesc.trim(), expenseMethod)
                            expenseAmountStr = ""
                            expenseDesc = ""
                            showExpenseDialog = false
                        }
                    }
                ) {
                    Text("Registrar Egreso")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpenseDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Close Cash Register
    if (showCloseRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showCloseRegisterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Arqueo y Cierre de Caja")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Resumen del Arqueo de Caja:", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Total Ingresos Registrados: ${currencyFormat.format(totalIncome)}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Total Gastos Menores: ${currencyFormat.format(totalExpense)}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Dinero Esperado en Caja: ${currencyFormat.format(netCashBalance)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Al confirmar, se genera el reporte de cierre cuadrado y se archivan las operaciones del turno.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showCloseRegisterDialog = false }) {
                    Text("Confirmar Cierre Cuadrado")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseRegisterDialog = false }) {
                    Text("Volver")
                }
            }
        )
    }
}
