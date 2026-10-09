# MotoTaller PRO — Aplicación Nativa Android para Taller de Motos

Aplicación 100% nativa para Android desarrollada con **Kotlin** y **Jetpack Compose**, diseñada específicamente para talleres mecánicos de motocicletas.

### Módulos y Funcionalidades:
* **Tablero Kanban en Tiempo Real:** Seguimiento ágil del ciclo de vida de cada motocicleta (*Recibidas*, *En Diagnóstico*, *Esperando Repuestos*, *En Reparación*, *Listas para Entrega*, *Entregadas*).
* **Recepción con Wizard de 4 Pasos:** Registro rápido de cliente, motocicleta (placa, marca, kilometraje, nivel de combustible), chequeo express de 10 puntos, marcas de daños en diagrama táctil y firma digital en pantalla.
* **Detalle de Orden & Cotizaciones:** Aprobación interactiva de repuestos, cálculo de mano de obra y generación de mensajes directos para **WhatsApp**.
* **Modo Mecánico de Patio:** Cronómetro de trabajo en tiempo real, registro de tiempos trabajados y fotos/notas de piezas dañadas.
* **Control de Inventario y Bodega:** Catálogo de repuestos con ubicación en estantería, niveles de stock mínimo y alertas automáticas de reposición.
* **CRM de Clientes & Motos:** Historial de visitas por cliente y alertas de vencimiento de SOAT y Tecnomecánica.
* **Caja Registradora & Auditoría:** Registro de anticipos, liquidación de saldos en efectivo/transferencia y bitácora de auditoría.
* **Base de Datos & Seguridad:** Persistencia local con **Room Database (SQLite)** y sincronización en la nube con **Firebase Firestore** y autenticación Google vía Jetpack Credential Manager.
