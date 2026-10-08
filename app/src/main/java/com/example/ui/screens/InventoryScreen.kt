package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.InventoryItem
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    inventory: List<InventoryItem>,
    onAdjustStock: (String, Int) -> Unit,
    onSaveItem: (InventoryItem) -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }
    var searchQuery by remember { mutableStateOf("") }
    var filterLowStockOnly by remember { mutableStateOf(false) }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showPurchaseOrderDialog by remember { mutableStateOf(false) }

    // New item inputs
    var newSku by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newBrand by remember { mutableStateOf("") }
    var newCost by remember { mutableStateOf("") }
    var newSalePrice by remember { mutableStateOf("") }
    var newStock by remember { mutableStateOf("10") }
    var newMinStock by remember { mutableStateOf("3") }
    var newLocation by remember { mutableStateOf("Estante A-1") }
    var newSupplier by remember { mutableStateOf("Distribuidora Motos") }
    var newCrossCompat by remember { mutableStateOf("AK125 NKD, Boxer CT100, GN 125") }

    val filteredList = inventory.filter {
        val matchesQuery = it.description.contains(searchQuery, ignoreCase = true) ||
                it.sku.contains(searchQuery, ignoreCase = true) ||
                it.crossCompatibility.contains(searchQuery, ignoreCase = true)
        val matchesLow = if (filterLowStockOnly) it.isLowStock else true
        matchesQuery && matchesLow
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("inventory_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Inventario y Proveedores",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${inventory.size} referencias • Descuento automático en tiempo real",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showPurchaseOrderDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reabastecer")
                    }
                }
            }

            // Search Bar & Filter Chips
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar por repuesto, código o compatibilidad...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inventory_search_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !filterLowStockOnly,
                        onClick = { filterLowStockOnly = false },
                        label = { Text("Todos (${inventory.size})") }
                    )
                    val lowCount = inventory.count { it.isLowStock }
                    FilterChip(
                        selected = filterLowStockOnly,
                        onClick = { filterLowStockOnly = true },
                        label = { Text("Stock Bajo ($lowCount)") },
                        leadingIcon = if (lowCount > 0) {
                            { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Inventory List
            if (filteredList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No se encontraron piezas en bodega", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_card_${item.id}"),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (item.isLowStock) MaterialTheme.colorScheme.error.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // SKU & Brand Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = item.sku,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.brand,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (item.isLowStock) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "STOCK BAJO",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            // Cross Compatibility Banner
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🔄 Compatibilidad: ${item.crossCompatibility}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ubicación: ${item.warehouseLocation} • Proveedor: ${item.supplier}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Pricing and Stock Controls Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Venta: ${currencyFormat.format(item.salePrice)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Costo: ${currencyFormat.format(item.costPrice)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Quick Stock Adjuster (+ / - buttons)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = { onAdjustStock(item.id, -1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                                    }

                                    Text(
                                        text = "${item.stockQuantity} un.",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (item.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )

                                    IconButton(
                                        onClick = { onAdjustStock(item.id, 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB to add new product
        FloatingActionButton(
            onClick = { showAddItemDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_inventory_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nuevo Repuesto")
        }
    }

    // Dialog: Add Item
    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Registrar Nuevo Repuesto") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSku,
                        onValueChange = { newSku = it.uppercase() },
                        label = { Text("Código SKU (Ej: PAS-ICH-NKD) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Descripción del Repuesto *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newBrand,
                            onValueChange = { newBrand = it },
                            label = { Text("Marca") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newLocation,
                            onValueChange = { newLocation = it },
                            label = { Text("Ubicación Bodega") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newCost,
                            onValueChange = { newCost = it },
                            label = { Text("Costo ($)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newSalePrice,
                            onValueChange = { newSalePrice = it },
                            label = { Text("Precio Venta ($) *") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newStock,
                            onValueChange = { newStock = it },
                            label = { Text("Stock Inicial") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newMinStock,
                            onValueChange = { newMinStock = it },
                            label = { Text("Stock Mínimo") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = newCrossCompat,
                        onValueChange = { newCrossCompat = it },
                        label = { Text("Compatibilidad Cruzada (Motos)") },
                        placeholder = { Text("Ej: AK125 NKD, Suzuki GN 125, Boxer CT100") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSupplier,
                        onValueChange = { newSupplier = it },
                        label = { Text("Proveedor Principal") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val salePriceVal = newSalePrice.toDoubleOrNull() ?: 0.0
                        if (newDesc.isNotBlank() && salePriceVal > 0) {
                            val item = InventoryItem(
                                id = "inv_${System.currentTimeMillis()}",
                                sku = if (newSku.isBlank()) "REP-${System.currentTimeMillis() % 1000}" else newSku,
                                description = newDesc.trim(),
                                brand = if (newBrand.isBlank()) "Genérico" else newBrand.trim(),
                                costPrice = newCost.toDoubleOrNull() ?: (salePriceVal * 0.6),
                                salePrice = salePriceVal,
                                stockQuantity = newStock.toIntOrNull() ?: 5,
                                minStock = newMinStock.toIntOrNull() ?: 2,
                                warehouseLocation = if (newLocation.isBlank()) "Estante General" else newLocation.trim(),
                                supplier = if (newSupplier.isBlank()) "Distribuidora Local" else newSupplier.trim(),
                                crossCompatibility = if (newCrossCompat.isBlank()) "Universal" else newCrossCompat.trim()
                            )
                            onSaveItem(item)
                            newSku = ""
                            newDesc = ""
                            newCost = ""
                            newSalePrice = ""
                            showAddItemDialog = false
                        }
                    }
                ) {
                    Text("Guardar Repuesto")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Purchase Order Reorder
    if (showPurchaseOrderDialog) {
        val lowItems = inventory.filter { it.isLowStock }
        AlertDialog(
            onDismissRequest = { showPurchaseOrderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Orden de Compra a Proveedores")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Lista de piezas que requieren reabastecimiento inmediato:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (lowItems.isEmpty()) {
                        Text(text = "¡Excelente! Todo el inventario está por encima del stock mínimo.")
                    } else {
                        lowItems.forEach { i ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "• ${i.description}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "Pedir: ${i.minStock * 2 - i.stockQuantity} un.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPurchaseOrderDialog = false }) {
                    Text("Generar y Enviar Pedido")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseOrderDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
