package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.TopRoleBar
import com.example.ui.components.WorkshopBottomNav
import com.example.ui.screens.CashRegisterScreen
import com.example.ui.screens.CreateOrderWizardScreen
import com.example.ui.screens.CrmClientsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.MechanicAppScreen
import com.example.ui.screens.OrderDetailScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ReportsAuditScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.WorkshopViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MotoTallerApp()
            }
        }
    }
}

@Composable
fun MotoTallerApp(
    viewModel: WorkshopViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val orders by viewModel.workOrders.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val motorcycles by viewModel.motorcycles.collectAsStateWithLifecycle()
    val cashEntries by viewModel.cashEntries.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    val canGoBack = currentScreen is AppScreen.OrderDetail ||
            currentScreen is AppScreen.CreateOrder ||
            currentScreen is AppScreen.ReportsAudit

    BackHandler(enabled = canGoBack) {
        viewModel.handleBack()
    }

    Scaffold(
        topBar = {
            TopRoleBar(
                currentUser = currentUser,
                availableUsers = viewModel.availableUsers,
                onUserSelected = { viewModel.switchUser(it) },
                canNavigateBack = canGoBack,
                onBackClicked = { viewModel.handleBack() }
            )
        },
        bottomBar = {
            // Hide bottom nav in full wizard
            if (currentScreen !is AppScreen.CreateOrder) {
                WorkshopBottomNav(
                    currentScreen = currentScreen,
                    onTabSelected = { screen ->
                        viewModel.navigateTo(screen)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is AppScreen.Dashboard -> {
                    DashboardScreen(
                        orders = orders,
                        inventory = inventory,
                        motorcycles = motorcycles,
                        onOrderClick = { orderId ->
                            viewModel.navigateTo(AppScreen.OrderDetail(orderId))
                        },
                        onCreateOrderClick = {
                            viewModel.navigateTo(AppScreen.CreateOrder)
                        },
                        onAdvanceStatusClick = { orderId, newStatus ->
                            viewModel.updateOrderStatus(orderId, newStatus)
                        }
                    )
                }

                is AppScreen.Orders -> {
                    OrdersScreen(
                        orders = orders,
                        onOrderClick = { orderId ->
                            viewModel.navigateTo(AppScreen.OrderDetail(orderId))
                        },
                        onCreateOrderClick = {
                            viewModel.navigateTo(AppScreen.CreateOrder)
                        }
                    )
                }

                is AppScreen.OrderDetail -> {
                    val order = orders.find { it.id == screen.orderId }
                    if (order != null) {
                        OrderDetailScreen(
                            order = order,
                            onBackClick = { viewModel.handleBack() },
                            onStatusChange = { newStatus ->
                                viewModel.updateOrderStatus(order.id, newStatus)
                            },
                            onToggleItemApproval = { itemId, approved ->
                                viewModel.toggleQuoteItemApproval(order.id, itemId, approved)
                            },
                            onAddQuoteItem = { item ->
                                viewModel.addQuoteItem(order.id, item)
                            },
                            onAddPresetKit = { kitCode ->
                                viewModel.addPresetKit(order.id, kitCode)
                            },
                            onToggleTimer = {
                                viewModel.toggleMechanicTimer(order.id)
                            },
                            onUpdateQualityCheck = { qc, warrantyDays ->
                                viewModel.updateQualityCheck(order.id, qc, warrantyDays)
                            },
                            onRecordPayment = { amount, method, isAdvance ->
                                viewModel.recordPayment(order.id, amount, method, isAdvance)
                            },
                            onGenerateWhatsApp = { type ->
                                viewModel.buildWhatsAppMessage(type, order)
                            }
                        )
                    } else {
                        // Fallback
                        DashboardScreen(
                            orders = orders,
                            inventory = inventory,
                            motorcycles = motorcycles,
                            onOrderClick = { viewModel.navigateTo(AppScreen.OrderDetail(it)) },
                            onCreateOrderClick = { viewModel.navigateTo(AppScreen.CreateOrder) },
                            onAdvanceStatusClick = { id, st -> viewModel.updateOrderStatus(id, st) }
                        )
                    }
                }

                is AppScreen.CreateOrder -> {
                    CreateOrderWizardScreen(
                        mechanics = viewModel.availableUsers.filter { it.role == com.example.data.model.UserRole.MECHANIC },
                        onOrderCreated = { client, moto, issue, mileage, fuel, damages, signature, mech, checks ->
                            viewModel.createNewOrder(
                                client = client,
                                moto = moto,
                                reportedIssue = issue,
                                mileage = mileage,
                                fuelLevel = fuel,
                                damages = damages,
                                signature = signature,
                                assignedMechanic = mech,
                                initialChecks = checks
                            )
                        },
                        onCancel = { viewModel.handleBack() }
                    )
                }

                is AppScreen.MechanicMode -> {
                    MechanicAppScreen(
                        currentMechanic = currentUser,
                        orders = orders,
                        onToggleTimer = { orderId ->
                            viewModel.toggleMechanicTimer(orderId)
                        },
                        onAddPhotoDamagedPart = { orderId, note ->
                            viewModel.addDamagedPhotoTag(orderId, note)
                        },
                        onAdvanceOrder = { orderId, newStatus ->
                            viewModel.updateOrderStatus(orderId, newStatus)
                        },
                        onOrderClick = { orderId ->
                            viewModel.navigateTo(AppScreen.OrderDetail(orderId))
                        }
                    )
                }

                is AppScreen.Inventory -> {
                    InventoryScreen(
                        inventory = inventory,
                        onAdjustStock = { itemId, delta ->
                            viewModel.adjustStock(itemId, delta)
                        },
                        onSaveItem = { item ->
                            viewModel.saveInventoryItem(item)
                        }
                    )
                }

                is AppScreen.CrmClients -> {
                    CrmClientsScreen(
                        clients = clients,
                        motorcycles = motorcycles,
                        orders = orders,
                        onSaveClient = { viewModel.saveClient(it) },
                        onSaveMotorcycle = { viewModel.saveMotorcycle(it) },
                        onOpenOrder = { orderId ->
                            viewModel.navigateTo(AppScreen.OrderDetail(orderId))
                        }
                    )
                }

                is AppScreen.CashRegister -> {
                    CashRegisterScreen(
                        cashEntries = cashEntries,
                        onAddExpense = { amount, category, desc, method ->
                            viewModel.addCashExpense(amount, category, desc, method)
                        }
                    )
                }

                is AppScreen.ReportsAudit -> {
                    ReportsAuditScreen(
                        orders = orders,
                        inventory = inventory,
                        cashEntries = cashEntries,
                        auditLogs = auditLogs
                    )
                }
            }
        }
    }
}
