package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Client
import com.example.data.model.ClientSegment
import com.example.data.model.Motorcycle
import com.example.data.model.WorkOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrmClientsScreen(
    clients: List<Client>,
    motorcycles: List<Motorcycle>,
    orders: List<WorkOrder>,
    onSaveClient: (Client) -> Unit,
    onSaveMotorcycle: (Motorcycle) -> Unit,
    onOpenOrder: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Clientes CRM, 1: Fichas de Motos
    var searchQuery by remember { mutableStateOf("") }

    var showAddClientDialog by remember { mutableStateOf(false) }
    var showAddMotoDialog by remember { mutableStateOf(false) }

    // Client form state
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientDoc by remember { mutableStateOf("") }
    var clientAddress by remember { mutableStateOf("") }
    var clientSegment by remember { mutableStateOf(ClientSegment.FREQUENT) }

    // Moto form state
    var motoPlate by remember { mutableStateOf("") }
    var motoBrand by remember { mutableStateOf("AKT") }
    var motoModel by remember { mutableStateOf("AK125 NKD") }
    var motoYear by remember { mutableStateOf("2024") }
    var motoCc by remember { mutableStateOf("125") }
    var motoColor by remember { mutableStateOf("Negro") }
    var motoMileage by remember { mutableStateOf("15000") }
    var motoOwnerId by remember { mutableStateOf(clients.firstOrNull()?.id ?: "") }

    val filteredClients = clients.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true) ||
                it.documentNumber.contains(searchQuery, ignoreCase = true)
    }

    val filteredMotos = motorcycles.filter {
        it.plate.contains(searchQuery, ignoreCase = true) ||
                it.brand.contains(searchQuery, ignoreCase = true) ||
                it.model.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)) {
                    Text(
                        text = "Clientes y Parque Automotor",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Fidelización CRM • Historial clínico de motocicletas • Alertas normativas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Propietarios CRM (${clients.size})") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Fichas de Motos (${motorcycles.size})") }
                        )
                    }
                }
            }

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(if (selectedTab == 0) "Buscar por nombre, teléfono o cédula..." else "Buscar por placa, modelo o marca...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Content
            if (selectedTab == 0) {
                // TAB 0: CLIENTES CRM
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("clients_crm_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        val clientMotos = motorcycles.filter { it.ownerClientId == client.id }
                        val clientOrders = orders.filter { it.clientPhone == client.phone || it.clientName == client.name }

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = client.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Doc: ${client.documentNumber} • ${client.totalVisits} visitas",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // WhatsApp direct launch button
                                    IconButton(
                                        onClick = {
                                            val text = "Hola ${client.name}, te saludamos desde MotoTaller Pro para consultar por el estado de tu motocicleta y programar tu próximo mantenimiento preventivo."
                                            val uri = Uri.parse("https://wa.me/57${client.phone.trim()}?text=${Uri.encode(text)}")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Chat,
                                            contentDescription = "WhatsApp",
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Segment Pill
                                val segmentColor = when (client.segment) {
                                    ClientSegment.VIP -> Color(0xFFF59E0B)
                                    ClientSegment.FREQUENT -> MaterialTheme.colorScheme.primary
                                    ClientSegment.OCCASIONAL -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Surface(
                                    color = segmentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = segmentColor, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = client.segment.label,
                                            color = segmentColor,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Associated Motorcycles chips
                                Text(
                                    text = "Motos asociadas (${clientMotos.size}):",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (clientMotos.isEmpty()) {
                                    Text(text = "Sin motos registradas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        clientMotos.forEach { m ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${m.plate} (${m.brand} ${m.model})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Preventative recommendation calculation
                                val lastMileage = clientMotos.maxOfOrNull { it.currentMileage } ?: 0
                                if (lastMileage > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "🔔 Plan Preventivo: Próximo cambio de aceite recomendado a los ${lastMileage + 3000} km",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 1: FICHAS DE MOTOS (Módulo 3)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("motorcycles_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMotos, key = { it.id }) { moto ->
                        val owner = clients.find { it.id == moto.ownerClientId }
                        val motoOrders = orders.filter { it.motorcycleId == moto.id || it.plate == moto.plate }

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = moto.plate,
                                                color = Color.White,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "${moto.brand} ${moto.model} (${moto.displacementCc}cc)",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Año ${moto.year} • Color: ${moto.color}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${moto.currentMileage} km",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Propietario: ${owner?.name ?: "Sin asignar"} (${owner?.phone ?: ""})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "VIN: ${if (moto.vin.isNotBlank()) moto.vin else "N/A"} • Motor: ${if (moto.engineNumber.isNotBlank()) moto.engineNumber else "N/A"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Alertas Normativas SOAT y Tecnomecánica
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isSoatAlert = moto.soatExpiryDate.contains("2026-10")
                                    Surface(
                                        color = if (isSoatAlert) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "SOAT",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSoatAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = moto.soatExpiryDate,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (isSoatAlert) {
                                                Text(
                                                    text = "⚠️ Próximo a vencer",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "Tecnomecánica",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = moto.tecnoExpiryDate,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                if (motoOrders.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Historial de Órdenes en Taller (${motoOrders.size}):",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    motoOrders.forEach { ord ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onOpenOrder(ord.id) }
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "• #${ord.id}: ${ord.status.label}", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "Ver Orden >", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                if (selectedTab == 0) showAddClientDialog = true else showAddMotoDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Agregar")
        }
    }

    // Dialog: Add Client
    if (showAddClientDialog) {
        AlertDialog(
            onDismissRequest = { showAddClientDialog = false },
            title = { Text("Registrar Nuevo Propietario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nombre Completo *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Teléfono / WhatsApp *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clientDoc,
                        onValueChange = { clientDoc = it },
                        label = { Text("Cédula / Documento") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clientAddress,
                        onValueChange = { clientAddress = it },
                        label = { Text("Dirección") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clientName.isNotBlank() && clientPhone.isNotBlank()) {
                            onSaveClient(
                                Client(
                                    id = "cli_${System.currentTimeMillis()}",
                                    name = clientName.trim(),
                                    phone = clientPhone.trim(),
                                    documentNumber = clientDoc.trim(),
                                    address = clientAddress.trim(),
                                    segment = ClientSegment.OCCASIONAL
                                )
                            )
                            clientName = ""
                            clientPhone = ""
                            showAddClientDialog = false
                        }
                    }
                ) {
                    Text("Guardar Cliente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClientDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Add Motorcycle
    if (showAddMotoDialog) {
        AlertDialog(
            onDismissRequest = { showAddMotoDialog = false },
            title = { Text("Registrar Nueva Moto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = motoPlate,
                        onValueChange = { motoPlate = it.uppercase() },
                        label = { Text("Placa (Ej: ABC-12D) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = motoBrand,
                            onValueChange = { motoBrand = it },
                            label = { Text("Marca") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = motoModel,
                            onValueChange = { motoModel = it },
                            label = { Text("Modelo") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = motoYear,
                            onValueChange = { motoYear = it },
                            label = { Text("Año") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = motoMileage,
                            onValueChange = { motoMileage = it },
                            label = { Text("Kilometraje") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (motoPlate.isNotBlank()) {
                            onSaveMotorcycle(
                                Motorcycle(
                                    id = "moto_${System.currentTimeMillis()}",
                                    plate = motoPlate.trim().uppercase(),
                                    brand = motoBrand.trim(),
                                    model = motoModel.trim(),
                                    year = motoYear.toIntOrNull() ?: 2024,
                                    displacementCc = motoCc.toIntOrNull() ?: 125,
                                    color = motoColor.trim(),
                                    ownerClientId = motoOwnerId,
                                    currentMileage = motoMileage.toIntOrNull() ?: 10000,
                                    soatExpiryDate = "2027-01-01",
                                    tecnoExpiryDate = "2027-01-01"
                                )
                            )
                            motoPlate = ""
                            showAddMotoDialog = false
                        }
                    }
                ) {
                    Text("Guardar Moto")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMotoDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
