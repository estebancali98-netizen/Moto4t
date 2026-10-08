package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.AppUser
import com.example.data.model.CheckStatus
import com.example.data.model.Client
import com.example.data.model.ClientSegment
import com.example.data.model.ExpressCheckItem
import com.example.data.model.FuelLevel
import com.example.data.model.Motorcycle
import com.example.ui.components.SignatureCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderWizardScreen(
    mechanics: List<AppUser>,
    onOrderCreated: (
        client: Client,
        moto: Motorcycle,
        issue: String,
        mileage: Int,
        fuel: FuelLevel,
        damages: List<String>,
        signature: String,
        mechanic: AppUser?,
        checks: List<ExpressCheckItem>
    ) -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Recepción, 2: Chequeo Express, 3: Inspección Visual y Firma, 4: Asignación

    // Form states
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientDoc by remember { mutableStateOf("") }

    var plate by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("AKT") }
    var model by remember { mutableStateOf("AK125 NKD") }
    var year by remember { mutableStateOf("2024") }
    var displacement by remember { mutableStateOf("125") }
    var color by remember { mutableStateOf("Negro") }
    var mileageStr by remember { mutableStateOf("15000") }
    var reportedIssue by remember { mutableStateOf("") }

    // Fuel level
    var selectedFuel by remember { mutableStateOf(FuelLevel.HALF) }

    // Damages list
    val damagesList = remember { mutableStateListOf<String>() }
    var newDamageInput by remember { mutableStateOf("") }

    // Digital signature token
    var clientSignatureToken by remember { mutableStateOf("") }

    // Selected Mechanic
    var selectedMechanic by remember { mutableStateOf<AppUser?>(mechanics.firstOrNull()) }

    // 10 Express check items
    val expressChecks = remember {
        mutableStateListOf(
            ExpressCheckItem("chk_1", "1. Luces y Direccionales", CheckStatus.OK),
            ExpressCheckItem("chk_2", "2. Frenos Delantero / Trasero", CheckStatus.OK),
            ExpressCheckItem("chk_3", "3. Nivel de Aceite y Fugas", CheckStatus.OK),
            ExpressCheckItem("chk_4", "4. Presión y Estado de Llantas", CheckStatus.OK),
            ExpressCheckItem("chk_5", "5. Kit de Arrastre (Tensión/Dientes)", CheckStatus.OK),
            ExpressCheckItem("chk_6", "6. Suspensión y Retenedores", CheckStatus.OK),
            ExpressCheckItem("chk_7", "7. Batería y Encendido", CheckStatus.OK),
            ExpressCheckItem("chk_8", "8. Dirección y Cunas", CheckStatus.OK),
            ExpressCheckItem("chk_9", "9. Mandos, Guayas y Acelerador", CheckStatus.OK),
            ExpressCheckItem("chk_10", "10. Espejos y Bocina / Pito", CheckStatus.OK)
        )
    }

    // High rotation bike models presets
    val presetModels = listOf(
        Triple("AKT", "AK125 NKD", 125),
        Triple("Suzuki", "GN 125", 125),
        Triple("Bajaj", "Boxer CT100", 100),
        Triple("Yamaha", "NMAX 155", 155),
        Triple("Yamaha", "XTZ 150", 150),
        Triple("Bajaj", "Pulsar NS 200", 200)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Wizard Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar")
                        }
                        Column {
                            Text(
                                text = "Apertura de Orden de Trabajo",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Paso $step de 4: ${
                                    when (step) {
                                        1 -> "Recepción y Vehículo"
                                        2 -> "Chequeo Express (10 Puntos)"
                                        3 -> "Inspección y Firma Digital"
                                        else -> "Asignación de Técnico"
                                    }
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Step Progress Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (step >= i) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
            }
        }

        // Step Content
        Box(modifier = Modifier.weight(1f)) {
            when (step) {
                1 -> {
                    // Paso 1: Recepción y Vehículo
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Datos del Propietario",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = clientName,
                                onValueChange = { clientName = it },
                                label = { Text("Nombre Completo del Cliente *") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_client_name_input"),
                                singleLine = true
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = clientPhone,
                                    onValueChange = { clientPhone = it },
                                    label = { Text("Teléfono / WhatsApp *") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wizard_client_phone_input"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = clientDoc,
                                    onValueChange = { clientDoc = it },
                                    label = { Text("Cédula / Documento") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Datos de la Motocicleta",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Selección rápida para motos de alta rotación:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quick Model Presets
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presetModels.forEach { (pBrand, pModel, pCc) ->
                                    val isSelected = brand == pBrand && model == pModel
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable {
                                            brand = pBrand
                                            model = pModel
                                            displacement = pCc.toString()
                                        }
                                    ) {
                                        Text(
                                            text = "$pBrand $pModel",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = plate,
                                    onValueChange = { plate = it.uppercase() },
                                    label = { Text("Placa (Ej: ABC-12D) *") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wizard_plate_input"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = mileageStr,
                                    onValueChange = { mileageStr = it },
                                    label = { Text("Kilometraje Actual *") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = brand,
                                    onValueChange = { brand = it },
                                    label = { Text("Marca") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = model,
                                    onValueChange = { model = it },
                                    label = { Text("Línea / Modelo") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = color,
                                    onValueChange = { color = it },
                                    label = { Text("Color") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = year,
                                    onValueChange = { year = it },
                                    label = { Text("Año") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = reportedIssue,
                                onValueChange = { reportedIssue = it },
                                label = { Text("Problema Reportado por el Cliente *") },
                                placeholder = { Text("Ej: Ruido en transmisión al frenar, cambio de aceite y filtro") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_reported_issue_input"),
                                minLines = 3
                            )
                        }
                    }
                }

                2 -> {
                    // Paso 2: Chequeo Express de 10 Puntos
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Chequeo Express Gratis (10 Puntos)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Diagnóstico visual rápido para detectar necesidades preventivas adicionales:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(expressChecks) { check ->
                            val index = expressChecks.indexOf(check)
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = check.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // OK button
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (check.status == CheckStatus.OK) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    expressChecks[index] = check.copy(status = CheckStatus.OK)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = if (check.status == CheckStatus.OK) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Bueno",
                                                    color = if (check.status == CheckStatus.OK) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }

                                        // Regular button
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (check.status == CheckStatus.REGULAR) Color(0xFFF59E0B) else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    expressChecks[index] = check.copy(status = CheckStatus.REGULAR)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Regular",
                                                    color = if (check.status == CheckStatus.REGULAR) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }

                                        // Bad button
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (check.status == CheckStatus.BAD) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    expressChecks[index] = check.copy(status = CheckStatus.BAD)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = if (check.status == CheckStatus.BAD) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Malo",
                                                    color = if (check.status == CheckStatus.BAD) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Paso 3: Inspección Visual, Nivel de Gasolina y Firma Digital
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "Nivel de Combustible Ingresado",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FuelLevel.values().forEach { fuel ->
                                    val isSelected = selectedFuel == fuel
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedFuel = fuel }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.LocalGasStation,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = fuel.label.split(" ").firstOrNull() ?: fuel.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Inventario de Rayones / Daños Previos",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newDamageInput,
                                    onValueChange = { newDamageInput = it },
                                    label = { Text("Registrar rayón o golpe (Ej: Tapa lateral)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newDamageInput.isNotBlank()) {
                                            damagesList.add(newDamageInput.trim())
                                            newDamageInput = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar")
                                }
                            }

                            if (damagesList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                damagesList.forEachIndexed { i, d ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "• $d", style = MaterialTheme.typography.bodySmall)
                                        IconButton(onClick = { damagesList.removeAt(i) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Quitar", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Digital Signature Canvas
                            SignatureCanvas(
                                initialSigned = clientSignatureToken.isNotBlank(),
                                onSignatureConfirmed = { token ->
                                    clientSignatureToken = token
                                }
                            )
                        }
                    }
                }

                4 -> {
                    // Paso 4: Asignación de Técnico y Resumen
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "Asignación del Mecánico en Patio",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Seleccione el técnico responsable de este vehículo:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(mechanics) { mech ->
                            val isSelected = selectedMechanic?.id == mech.id
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMechanic = mech }
                                    .testTag("select_mechanic_${mech.id}"),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mech.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = mech.role.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Resumen de la Orden",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "Cliente: $clientName • Tel: $clientPhone", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Moto: $plate ($brand $model $year)", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Kilometraje: $mileageStr km • Gasolina: ${selectedFuel.label}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Problema: $reportedIssue", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = if (clientSignatureToken.isNotBlank()) "✓ Firma digital registrada" else "⚠️ Sin firma digital",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (clientSignatureToken.isNotBlank()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Wizard Actions
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.testTag("wizard_prev_button")
                    ) {
                        Text("Anterior")
                    }
                } else {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("wizard_cancel_button")
                    ) {
                        Text("Cancelar")
                    }
                }

                if (step < 4) {
                    Button(
                        onClick = {
                            if (step == 1 && (clientName.isBlank() || plate.isBlank() || reportedIssue.isBlank())) {
                                // Simple fallback fill if empty
                                if (clientName.isBlank()) clientName = "Cliente Mostrador"
                                if (plate.isBlank()) plate = "MTO-001"
                                if (reportedIssue.isBlank()) reportedIssue = "Revisión general y diagnóstico"
                            }
                            step++
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("wizard_next_button")
                    ) {
                        Text("Siguiente")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                } else {
                    Button(
                        onClick = {
                            val client = Client(
                                id = "cli_${System.currentTimeMillis()}",
                                name = if (clientName.isBlank()) "Cliente Mostrador" else clientName.trim(),
                                phone = if (clientPhone.isBlank()) "3001234567" else clientPhone.trim(),
                                documentNumber = if (clientDoc.isBlank()) "1000000000" else clientDoc.trim(),
                                segment = ClientSegment.OCCASIONAL
                            )
                            val moto = Motorcycle(
                                id = "moto_${System.currentTimeMillis()}",
                                plate = if (plate.isBlank()) "MTO-001" else plate.trim().uppercase(),
                                brand = brand,
                                model = model,
                                year = year.toIntOrNull() ?: 2023,
                                displacementCc = displacement.toIntOrNull() ?: 125,
                                color = color,
                                ownerClientId = client.id,
                                currentMileage = mileageStr.toIntOrNull() ?: 10000,
                                soatExpiryDate = "2027-01-01",
                                tecnoExpiryDate = "2027-01-01"
                            )
                            onOrderCreated(
                                client,
                                moto,
                                if (reportedIssue.isBlank()) "Revisión periódica" else reportedIssue.trim(),
                                mileageStr.toIntOrNull() ?: 10000,
                                selectedFuel,
                                damagesList.toList(),
                                if (clientSignatureToken.isBlank()) "FIRMA_DEFAULT_OK" else clientSignatureToken,
                                selectedMechanic,
                                expressChecks.toList()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("wizard_finish_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Crear y Abrir Orden")
                    }
                }
            }
        }
    }
}
