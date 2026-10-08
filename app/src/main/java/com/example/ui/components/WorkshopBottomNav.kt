package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.viewmodel.AppScreen

@Composable
fun WorkshopBottomNav(
    currentScreen: AppScreen,
    onTabSelected: (AppScreen) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("main_bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // Tab 1: Dashboard
        NavigationBarItem(
            selected = currentScreen is AppScreen.Dashboard,
            onClick = { onTabSelected(AppScreen.Dashboard) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Panel") },
            label = { Text("Panel") },
            modifier = Modifier.testTag("nav_tab_dashboard"),
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Tab 2: Órdenes
        NavigationBarItem(
            selected = currentScreen is AppScreen.Orders || currentScreen is AppScreen.OrderDetail || currentScreen is AppScreen.CreateOrder,
            onClick = { onTabSelected(AppScreen.Orders) },
            icon = { Icon(Icons.Default.TwoWheeler, contentDescription = "Órdenes") },
            label = { Text("Órdenes") },
            modifier = Modifier.testTag("nav_tab_orders"),
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Tab 3: Modo Mecánico
        NavigationBarItem(
            selected = currentScreen is AppScreen.MechanicMode,
            onClick = { onTabSelected(AppScreen.MechanicMode) },
            icon = { Icon(Icons.Default.Build, contentDescription = "Taller") },
            label = { Text("Taller") },
            modifier = Modifier.testTag("nav_tab_mechanic"),
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Tab 4: Inventario
        NavigationBarItem(
            selected = currentScreen is AppScreen.Inventory,
            onClick = { onTabSelected(AppScreen.Inventory) },
            icon = { Icon(Icons.Default.Inventory, contentDescription = "Bodega") },
            label = { Text("Bodega") },
            modifier = Modifier.testTag("nav_tab_inventory"),
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Tab 5: Clientes CRM
        NavigationBarItem(
            selected = currentScreen is AppScreen.CrmClients,
            onClick = { onTabSelected(AppScreen.CrmClients) },
            icon = { Icon(Icons.Default.People, contentDescription = "Clientes") },
            label = { Text("Clientes") },
            modifier = Modifier.testTag("nav_tab_crm"),
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Tab 6: Caja & Reportes
        NavigationBarItem(
            selected = currentScreen is AppScreen.CashRegister || currentScreen is AppScreen.ReportsAudit,
            onClick = { onTabSelected(AppScreen.CashRegister) },
            icon = { Icon(Icons.Default.LocalAtm, contentDescription = "Caja") },
            label = { Text("Caja") },
            modifier = Modifier.testTag("nav_tab_cash"),
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
