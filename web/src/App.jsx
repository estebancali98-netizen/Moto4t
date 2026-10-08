import React, { useState, useEffect } from 'react';
import { collection, onSnapshot, doc, updateDoc, setDoc } from 'firebase/firestore';
import { db } from './firebase';
import { 
  Wrench, 
  Bike, 
  CheckCircle2, 
  Clock, 
  AlertCircle, 
  DollarSign, 
  Search, 
  Send, 
  RefreshCw, 
  ShieldCheck, 
  Fuel, 
  User, 
  Phone,
  Layers,
  ChevronRight,
  Menu,
  X
} from 'lucide-react';

const KANBAN_COLUMNS = [
  { id: 'RECEIVED', label: 'Recibidas', color: '#f59e0b', bg: '#78350f20' },
  { id: 'DIAGNOSIS', label: 'En Diagnóstico', color: '#38bdf8', bg: '#0369a120' },
  { id: 'WAITING_PARTS', label: 'Esperando Repuestos', color: '#a855f7', bg: '#6b21a820' },
  { id: 'IN_REPAIR', label: 'En Reparación', color: '#f97316', bg: '#c2410c20' },
  { id: 'READY', label: 'Listas para Entrega', color: '#22c55e', bg: '#15803d20' },
  { id: 'DELIVERED', label: 'Entregadas', color: '#94a3b8', bg: '#33415520' }
];

export default function App() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [activeTabStatus, setActiveTabStatus] = useState('ALL'); // Para vista móvil de columnas

  useEffect(() => {
    try {
      const unsubscribe = onSnapshot(collection(db, 'work_orders'), (snapshot) => {
        const docs = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
        setOrders(docs);
        setLoading(false);
      }, (error) => {
        console.warn("Firestore snapshot listener error:", error);
        setLoading(false);
      });
      return () => unsubscribe();
    } catch (e) {
      console.warn("Error subscribing to work_orders:", e);
      setLoading(false);
    }
  }, []);

  const handleAdvanceStatus = async (orderId, currentStatus) => {
    const statusOrder = ['RECEIVED', 'DIAGNOSIS', 'WAITING_PARTS', 'IN_REPAIR', 'READY', 'DELIVERED'];
    const currentIndex = statusOrder.indexOf(currentStatus);
    if (currentIndex < statusOrder.length - 1) {
      const nextStatus = statusOrder[currentIndex + 1];
      try {
        await updateDoc(doc(db, 'work_orders', orderId), {
          status: nextStatus,
          updatedAt: new Date()
        });
      } catch (err) {
        console.error("Error updating status:", err);
      }
    }
  };

  const seedInitialOrders = async () => {
    try {
      const sampleOrders = [
        {
          id: 'ord_1',
          plate: 'ABC-12D',
          motorcycleSummary: 'AKT AK125 NKD (2023)',
          clientName: 'Carlos Andrés Gómez',
          clientPhone: '3124567890',
          reportedIssue: 'Mantenimiento preventivo 15,000km, cambio de aceite y kit de arrastre',
          status: 'RECEIVED',
          assignedMechanicName: 'Carlos Mecánico',
          initialMileage: 18500,
          fuelLevel: 'HALF',
          totalApprovedQuote: 125000,
          advancePayment: 50000,
          paidAmount: 50000,
          balanceDue: 75000,
          minutesWorked: 0,
          entryTimestamp: Date.now() - 3600000,
          userId: 'demo_user'
        },
        {
          id: 'ord_2',
          plate: 'XYZ-89E',
          motorcycleSummary: 'Yamaha NMAX 155 (2024)',
          clientName: 'Mariana Valencia',
          clientPhone: '3159876543',
          reportedIssue: 'Revisión de pastillas de freno delanteras y ruido en variador',
          status: 'DIAGNOSIS',
          assignedMechanicName: 'Mateo Especialista',
          initialMileage: 8200,
          fuelLevel: 'FULL',
          totalApprovedQuote: 180000,
          advancePayment: 80000,
          paidAmount: 80000,
          balanceDue: 100000,
          minutesWorked: 25,
          entryTimestamp: Date.now() - 7200000,
          userId: 'demo_user'
        },
        {
          id: 'ord_3',
          plate: 'KJH-45F',
          motorcycleSummary: 'Suzuki GN 125 (2022)',
          clientName: 'Sebastián Roa',
          clientPhone: '3201234567',
          reportedIssue: 'Sincronización de carburador y cambio de bujía NGK',
          status: 'READY',
          assignedMechanicName: 'Carlos Mecánico',
          initialMileage: 34200,
          fuelLevel: 'QUARTER',
          totalApprovedQuote: 95000,
          advancePayment: 95000,
          paidAmount: 95000,
          balanceDue: 0,
          minutesWorked: 65,
          entryTimestamp: Date.now() - 14400000,
          userId: 'demo_user'
        }
      ];

      for (const order of sampleOrders) {
        await setDoc(doc(db, 'work_orders', order.id), order);
      }
      alert('¡Órdenes de prueba sincronizadas con éxito en Firestore!');
    } catch (e) {
      console.error("Error seeding orders:", e);
      alert('Error: ' + e.message);
    }
  };

  const filteredOrders = orders.filter(o => 
    (o.plate || '').toLowerCase().includes(search.toLowerCase()) ||
    (o.clientName || '').toLowerCase().includes(search.toLowerCase()) ||
    (o.motorcycleSummary || '').toLowerCase().includes(search.toLowerCase())
  );

  const totalRevenue = orders.reduce((acc, curr) => acc + (Number(curr.paidAmount) || 0), 0);
  const activeCount = orders.filter(o => o.status !== 'DELIVERED').length;
  const readyCount = orders.filter(o => o.status === 'READY').length;

  return (
    <div className="min-h-screen flex flex-col bg-[#0b1120] text-slate-100">
      {/* Top Header - Mobile friendly wrap */}
      <header className="bg-[#0f172a] border-b border-slate-800 px-4 py-3 sm:px-7 sm:py-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-orange-600 flex items-center justify-center text-white shrink-0 shadow-md">
            <Bike size={22} />
          </div>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-lg sm:text-xl font-extrabold text-slate-50 tracking-tight">
                MotoTaller
              </span>
              <span className="bg-orange-600 text-white text-[10px] sm:text-xs font-black px-1.5 py-0.5 rounded">
                PRO
              </span>
              <span className="bg-sky-500/10 text-sky-400 border border-sky-500/30 text-[10px] sm:text-xs font-medium px-2 py-0.5 rounded-full">
                Móvil & Web
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Sincronizado en tiempo real con Android & Firestore
            </p>
          </div>
        </div>

        {/* Search & Cloud Status */}
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <div className="relative flex items-center bg-slate-800/90 rounded-lg px-3 py-2 border border-slate-700 flex-1 sm:w-64">
            <Search size={16} className="text-slate-400 mr-2 shrink-0" />
            <input 
              type="text" 
              placeholder="Buscar placa, cliente..." 
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="bg-transparent border-none outline-none text-slate-100 text-xs sm:text-sm w-full placeholder:text-slate-500"
            />
          </div>
          {orders.length === 0 && (
            <button 
              onClick={seedInitialOrders}
              className="bg-orange-600 hover:bg-orange-500 text-white text-xs font-bold px-3 py-2 rounded-lg flex items-center gap-1.5 shadow transition-all animate-bounce whitespace-nowrap"
            >
              <RefreshCw size={13} /> Cargar Órdenes Ejemplo
            </button>
          )}
          <div className="hidden sm:flex items-center gap-2 text-xs text-emerald-400 bg-emerald-500/10 border border-emerald-500/30 px-3 py-1.5 rounded-full whitespace-nowrap">
            <div className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>Firestore en Vivo</span>
          </div>
        </div>
      </header>

      {/* KPI Bar - Mobile grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5 sm:gap-4 p-3 sm:px-7 sm:py-4 bg-[#0b1120]">
        <div className="bg-[#0f172a] p-3 sm:p-4 rounded-xl border border-slate-800">
          <div className="flex justify-between text-slate-400 text-xs">
            <span>En Patio</span>
            <Wrench size={16} className="text-orange-500" />
          </div>
          <div className="text-xl sm:text-2xl font-black text-slate-50 mt-1">
            {activeCount}
          </div>
        </div>

        <div className="bg-[#0f172a] p-3 sm:p-4 rounded-xl border border-slate-800">
          <div className="flex justify-between text-slate-400 text-xs">
            <span>Listas Entrega</span>
            <CheckCircle2 size={16} className="text-emerald-400" />
          </div>
          <div className="text-xl sm:text-2xl font-black text-emerald-400 mt-1">
            {readyCount}
          </div>
        </div>

        <div className="col-span-2 sm:col-span-1 bg-[#0f172a] p-3 sm:p-4 rounded-xl border border-slate-800">
          <div className="flex justify-between text-slate-400 text-xs">
            <span>Recaudado Total</span>
            <DollarSign size={16} className="text-sky-400" />
          </div>
          <div className="text-xl sm:text-2xl font-black text-sky-400 mt-1">
            ${totalRevenue.toLocaleString('es-CO')}
          </div>
        </div>
      </div>

      {/* Mobile Kanban Tab Selector (Horizontal scroll for small screens) */}
      <div className="px-3 sm:px-7 mb-2 flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none sm:hidden">
        <button
          onClick={() => setActiveTabStatus('ALL')}
          className={`px-3 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-colors ${
            activeTabStatus === 'ALL'
              ? 'bg-orange-600 text-white shadow-sm'
              : 'bg-slate-800 text-slate-300 border border-slate-700'
          }`}
        >
          Todas ({filteredOrders.length})
        </button>
        {KANBAN_COLUMNS.map(col => {
          const count = filteredOrders.filter(o => (o.status || 'RECEIVED') === col.id).length;
          return (
            <button
              key={col.id}
              onClick={() => setActiveTabStatus(col.id)}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap flex items-center gap-1.5 transition-colors ${
                activeTabStatus === col.id
                  ? 'bg-orange-600 text-white shadow-sm'
                  : 'bg-slate-800 text-slate-300 border border-slate-700'
              }`}
            >
              <div className="w-2 h-2 rounded-full" style={{ backgroundColor: col.color }} />
              {col.label} ({count})
            </button>
          );
        })}
      </div>

      {/* Kanban Board Container: Responsive Multi-Column on desktop, filtered cards or side-scroller on mobile */}
      <div className="flex-1 px-3 sm:px-7 pb-6 overflow-x-auto">
        <div className="hidden sm:grid sm:grid-cols-6 gap-3.5 min-w-[1200px]">
          {KANBAN_COLUMNS.map(column => {
            const colOrders = filteredOrders.filter(o => (o.status || 'RECEIVED') === column.id);
            return (
              <div 
                key={column.id} 
                className="bg-[#0f172a] rounded-xl border border-slate-800 flex flex-col h-[calc(100vh-220px)]"
              >
                {/* Column Header */}
                <div className="p-3 border-b border-slate-800 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: column.color }} />
                    <span className="text-xs font-bold text-slate-100">
                      {column.label}
                    </span>
                  </div>
                  <span className="bg-slate-800 text-slate-400 text-[11px] font-bold px-2 py-0.5 rounded-full">
                    {colOrders.length}
                  </span>
                </div>

                {/* Cards Container */}
                <div className="flex-1 overflow-y-auto p-2.5 flex flex-col gap-2.5">
                  {colOrders.map(order => (
                    <div 
                      key={order.id}
                      onClick={() => setSelectedOrder(order)}
                      className="bg-slate-800/90 hover:border-orange-500 rounded-lg p-3 border border-slate-700/80 cursor-pointer transition-all shadow-sm"
                    >
                      <div className="flex justify-between items-center mb-1.5">
                        <span className="bg-orange-500/20 text-orange-400 font-extrabold text-xs px-2 py-0.5 rounded">
                          {order.plate || 'SIN PLACA'}
                        </span>
                        <span className="text-[10px] text-slate-400">
                          #{order.id}
                        </span>
                      </div>

                      <div className="text-xs font-bold text-slate-100 mb-1 line-clamp-1">
                        {order.motorcycleSummary || 'Motocicleta'}
                      </div>

                      <div className="text-[11px] text-slate-400 flex items-center gap-1.5 mb-1.5">
                        <User size={12} />
                        <span className="truncate">{order.clientName || 'Cliente'}</span>
                      </div>

                      <div className="text-[11px] text-slate-300 bg-slate-900/80 p-2 rounded mb-2 italic line-clamp-2">
                        "{order.reportedIssue || 'Revisión general'}"
                      </div>

                      <div className="flex justify-between items-center pt-2 border-t border-slate-700/60">
                        <span className="text-[11px] text-slate-400 flex items-center gap-1 truncate max-w-[100px]">
                          <Wrench size={12} className="text-orange-500 shrink-0" />
                          <span className="truncate">{order.assignedMechanicName || 'Sin asignar'}</span>
                        </span>

                        {column.id !== 'DELIVERED' && (
                          <button 
                            onClick={(e) => {
                              e.stopPropagation();
                              handleAdvanceStatus(order.id, column.id);
                            }}
                            className="bg-orange-600 hover:bg-orange-500 text-white rounded px-2 py-1 text-[10px] font-bold flex items-center gap-1 transition-colors"
                          >
                            Avanzar <ChevronRight size={11} />
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>

        {/* Mobile View: Vertical list filtered by selected status pill */}
        <div className="sm:hidden flex flex-col gap-2.5">
          {(() => {
            const currentOrders = activeTabStatus === 'ALL'
              ? filteredOrders
              : filteredOrders.filter(o => (o.status || 'RECEIVED') === activeTabStatus);

            if (currentOrders.length === 0) {
              return (
                <div className="bg-[#0f172a] rounded-xl border border-slate-800 p-8 text-center text-slate-400">
                  <Bike size={36} className="mx-auto text-slate-600 mb-2" />
                  <p className="text-xs">No hay órdenes en este estado</p>
                </div>
              );
            }

            return currentOrders.map(order => {
              const col = KANBAN_COLUMNS.find(c => c.id === (order.status || 'RECEIVED')) || KANBAN_COLUMNS[0];
              return (
                <div 
                  key={order.id}
                  onClick={() => setSelectedOrder(order)}
                  className="bg-[#0f172a] rounded-xl p-3.5 border border-slate-800 active:scale-[0.99] transition-transform"
                >
                  <div className="flex justify-between items-center mb-2">
                    <div className="flex items-center gap-2">
                      <span className="bg-orange-500/20 text-orange-400 font-extrabold text-xs px-2.5 py-0.5 rounded">
                        {order.plate || 'SIN PLACA'}
                      </span>
                      <span 
                        className="text-[10px] font-bold px-2 py-0.5 rounded-full" 
                        style={{ backgroundColor: `${col.color}25`, color: col.color }}
                      >
                        {col.label}
                      </span>
                    </div>
                    <span className="text-[11px] text-slate-500 font-mono">
                      #{order.id}
                    </span>
                  </div>

                  <div className="text-sm font-bold text-slate-100 mb-1">
                    {order.motorcycleSummary || 'Motocicleta'}
                  </div>

                  <div className="flex items-center gap-3 text-xs text-slate-400 mb-2">
                    <span className="flex items-center gap-1 truncate">
                      <User size={13} /> {order.clientName || 'Cliente'}
                    </span>
                    {order.clientPhone && (
                      <span className="flex items-center gap-1 text-sky-400">
                        <Phone size={12} /> {order.clientPhone}
                      </span>
                    )}
                  </div>

                  <div className="text-xs text-slate-300 bg-slate-900/90 p-2.5 rounded-lg mb-2.5 italic">
                    "{order.reportedIssue || 'Revisión general y diagnóstico'}"
                  </div>

                  <div className="flex justify-between items-center pt-2 border-t border-slate-800 text-xs">
                    <span className="text-slate-400 flex items-center gap-1">
                      <Wrench size={13} className="text-orange-500" />
                      {order.assignedMechanicName || 'Mecánico asignado'}
                    </span>

                    {order.status !== 'DELIVERED' && (
                      <button 
                        onClick={(e) => {
                          e.stopPropagation();
                          handleAdvanceStatus(order.id, order.status || 'RECEIVED');
                        }}
                        className="bg-orange-600 active:bg-orange-700 text-white rounded-lg px-3 py-1.5 text-xs font-bold flex items-center gap-1 shadow"
                      >
                        Avanzar <ChevronRight size={13} />
                      </button>
                    )}
                  </div>
                </div>
              );
            });
          })()}
        </div>
      </div>

      {/* Order Detail Modal - Mobile responsive drawer/dialog */}
      {selectedOrder && (
        <div className="fixed inset-0 bg-black/80 flex items-center justify-center z-50 p-3 sm:p-5">
          <div className="bg-[#0f172a] rounded-2xl border border-slate-700 w-full max-w-lg max-h-[90vh] overflow-y-auto p-4 sm:p-6 shadow-2xl">
            <div className="flex justify-between items-start mb-4">
              <div>
                <span className="bg-orange-500/20 text-orange-400 font-extrabold text-sm px-2.5 py-1 rounded">
                  {selectedOrder.plate}
                </span>
                <h2 className="text-base sm:text-lg font-black text-slate-50 mt-2">
                  {selectedOrder.motorcycleSummary}
                </h2>
                <p className="text-xs text-slate-400">
                  Orden #{selectedOrder.id} • Estado: {selectedOrder.status}
                </p>
              </div>
              <button 
                onClick={() => setSelectedOrder(null)}
                className="text-slate-400 hover:text-white p-1 rounded-full text-lg"
              >
                ✕
              </button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 mb-3">
              <div className="bg-slate-800/80 p-3 rounded-xl border border-slate-700/60">
                <span className="text-[11px] text-slate-400">Cliente / Propietario</span>
                <p className="text-xs sm:text-sm font-bold text-slate-100 mt-0.5">
                  {selectedOrder.clientName}
                </p>
                <p className="text-xs text-sky-400 mt-0.5">
                  📞 {selectedOrder.clientPhone || 'Sin teléfono'}
                </p>
              </div>

              <div className="bg-slate-800/80 p-3 rounded-xl border border-slate-700/60">
                <span className="text-[11px] text-slate-400">Mecánico de Patio</span>
                <p className="text-xs sm:text-sm font-bold text-slate-100 mt-0.5">
                  {selectedOrder.assignedMechanicName || 'Sin asignar'}
                </p>
                <p className="text-xs text-slate-400 mt-0.5">
                  ⛽ Gasolina: {selectedOrder.fuelLevel || '1/2'}
                </p>
              </div>
            </div>

            <div className="bg-slate-800/80 p-3 rounded-xl border border-slate-700/60 mb-4">
              <span className="text-[11px] text-slate-400">Falla Reportada:</span>
              <p className="text-xs sm:text-sm text-slate-100 mt-1 leading-relaxed">
                {selectedOrder.reportedIssue}
              </p>
            </div>

            <div className="flex flex-col sm:flex-row justify-end gap-2">
              <button 
                onClick={() => {
                  const msg = `Hola ${selectedOrder.clientName}, tu moto ${selectedOrder.plate} está en servicio en MotoTaller Pro. Estado: ${selectedOrder.status}.`;
                  window.open(`https://wa.me/?text=${encodeURIComponent(msg)}`, '_blank');
                }}
                className="bg-emerald-600 hover:bg-emerald-500 text-white py-2.5 px-4 rounded-xl font-bold text-xs flex items-center justify-center gap-1.5 shadow"
              >
                <Send size={14} /> WhatsApp Cliente
              </button>
              <button 
                onClick={() => setSelectedOrder(null)}
                className="bg-slate-700 hover:bg-slate-600 text-slate-100 py-2.5 px-5 rounded-xl font-semibold text-xs text-center"
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
