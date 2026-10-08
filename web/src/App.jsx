import React, { useState, useEffect } from 'react';
import { collection, onSnapshot, doc, updateDoc } from 'firebase/firestore';
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
  ChevronRight
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

  const filteredOrders = orders.filter(o => 
    (o.plate || '').toLowerCase().includes(search.toLowerCase()) ||
    (o.clientName || '').toLowerCase().includes(search.toLowerCase()) ||
    (o.motorcycleSummary || '').toLowerCase().includes(search.toLowerCase())
  );

  const totalRevenue = orders.reduce((acc, curr) => acc + (Number(curr.paidAmount) || 0), 0);
  const activeCount = orders.filter(o => o.status !== 'DELIVERED').length;
  const readyCount = orders.filter(o => o.status === 'READY').length;

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: '#0b1120' }}>
      {/* Top Header */}
      <header style={{ 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'space-between', 
        padding: '16px 28px', 
        backgroundColor: '#0f172a', 
        borderBottom: '1px solid #1e293b' 
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{ 
            width: '42px', 
            height: '42px', 
            borderRadius: '12px', 
            backgroundColor: '#ea580c', 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            color: '#fff' 
          }}>
            <Bike size={24} />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '20px', fontWeight: '800', color: '#f8fafc', letterSpacing: '-0.5px' }}>
                MotoTaller
              </span>
              <span style={{ 
                backgroundColor: '#ea580c', 
                color: '#fff', 
                fontSize: '11px', 
                fontWeight: '900', 
                padding: '2px 6px', 
                borderRadius: '4px' 
              }}>
                PRO
              </span>
              <span style={{ 
                backgroundColor: '#0284c720', 
                color: '#38bdf8', 
                border: '1px solid #0284c740',
                fontSize: '11px', 
                fontWeight: '600', 
                padding: '2px 8px', 
                borderRadius: '20px' 
              }}>
                Vercel Web Reception
              </span>
            </div>
            <p style={{ fontSize: '13px', color: '#94a3b8' }}>
              Mostrador de Recepción sincronizado con App Móvil Android (Firebase Firestore)
            </p>
          </div>
        </div>

        {/* Search */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{ 
            position: 'relative', 
            display: 'flex', 
            alignItems: 'center', 
            backgroundColor: '#1e293b', 
            borderRadius: '10px', 
            padding: '8px 14px',
            border: '1px solid #334155'
          }}>
            <Search size={18} color="#94a3b8" style={{ marginRight: '8px' }} />
            <input 
              type="text" 
              placeholder="Buscar placa, cliente..." 
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ 
                background: 'transparent', 
                border: 'none', 
                outline: 'none', 
                color: '#f8fafc', 
                fontSize: '14px',
                width: '240px' 
              }}
            />
          </div>
          <div style={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: '8px', 
            fontSize: '12px', 
            color: '#22c55e', 
            backgroundColor: '#15803d20',
            border: '1px solid #22c55e40',
            padding: '6px 12px',
            borderRadius: '20px'
          }}>
            <div style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#22c55e' }} />
            Firestore en Vivo
          </div>
        </div>
      </header>

      {/* KPI Bar */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', 
        gap: '16px', 
        padding: '20px 28px',
        backgroundColor: '#0b1120'
      }}>
        <div style={{ backgroundColor: '#0f172a', padding: '16px', borderRadius: '12px', border: '1px solid #1e293b' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', color: '#94a3b8', fontSize: '13px' }}>
            <span>Órdenes Activas</span>
            <Wrench size={18} color="#ea580c" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: '800', color: '#f8fafc', marginTop: '6px' }}>
            {activeCount}
          </div>
        </div>

        <div style={{ backgroundColor: '#0f172a', padding: '16px', borderRadius: '12px', border: '1px solid #1e293b' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', color: '#94a3b8', fontSize: '13px' }}>
            <span>Listas para Entrega</span>
            <CheckCircle2 size={18} color="#22c55e" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: '800', color: '#22c55e', marginTop: '6px' }}>
            {readyCount}
          </div>
        </div>

        <div style={{ backgroundColor: '#0f172a', padding: '16px', borderRadius: '12px', border: '1px solid #1e293b' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', color: '#94a3b8', fontSize: '13px' }}>
            <span>Recaudado Total</span>
            <DollarSign size={18} color="#38bdf8" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: '800', color: '#38bdf8', marginTop: '6px' }}>
            ${totalRevenue.toLocaleString('es-CO')}
          </div>
        </div>
      </div>

      {/* Kanban Board */}
      <div style={{ 
        flex: 1, 
        padding: '0 28px 28px', 
        display: 'grid', 
        gridTemplateColumns: 'repeat(6, minmax(260px, 1fr))', 
        gap: '16px', 
        overflowX: 'auto' 
      }}>
        {KANBAN_COLUMNS.map(column => {
          const colOrders = filteredOrders.filter(o => (o.status || 'RECEIVED') === column.id);
          return (
            <div 
              key={column.id} 
              style={{ 
                backgroundColor: '#0f172a', 
                borderRadius: '14px', 
                border: '1px solid #1e293b', 
                display: 'flex', 
                flexDirection: 'column',
                height: 'calc(100vh - 230px)' 
              }}
            >
              {/* Column Header */}
              <div style={{ 
                padding: '14px 16px', 
                borderBottom: '1px solid #1e293b', 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'space-between' 
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div style={{ width: '10px', height: '10px', borderRadius: '50%', backgroundColor: column.color }} />
                  <span style={{ fontSize: '14px', fontWeight: '700', color: '#f8fafc' }}>
                    {column.label}
                  </span>
                </div>
                <span style={{ 
                  backgroundColor: '#1e293b', 
                  color: '#94a3b8', 
                  fontSize: '12px', 
                  fontWeight: '700', 
                  padding: '2px 8px', 
                  borderRadius: '12px' 
                }}>
                  {colOrders.length}
                </span>
              </div>

              {/* Cards Container */}
              <div style={{ flex: 1, overflowY: 'auto', padding: '12px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {colOrders.map(order => (
                  <div 
                    key={order.id}
                    onClick={() => setSelectedOrder(order)}
                    style={{ 
                      backgroundColor: '#1e293b', 
                      borderRadius: '10px', 
                      padding: '14px', 
                      border: '1px solid #334155',
                      cursor: 'pointer',
                      transition: 'transform 0.15s, border-color 0.15s'
                    }}
                    onMouseEnter={(e) => e.currentTarget.style.borderColor = column.color}
                    onMouseLeave={(e) => e.currentTarget.style.borderColor = '#334155'}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                      <span style={{ 
                        backgroundColor: '#ea580c20', 
                        color: '#f97316', 
                        fontWeight: '800', 
                        fontSize: '13px', 
                        padding: '2px 8px', 
                        borderRadius: '6px' 
                      }}>
                        {order.plate || 'SIN PLACA'}
                      </span>
                      <span style={{ fontSize: '11px', color: '#94a3b8' }}>
                        #{order.id}
                      </span>
                    </div>

                    <div style={{ fontSize: '14px', fontWeight: '600', color: '#f8fafc', marginBottom: '4px' }}>
                      {order.motorcycleSummary || 'Motocicleta'}
                    </div>

                    <div style={{ fontSize: '12px', color: '#94a3b8', display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
                      <User size={13} />
                      {order.clientName || 'Cliente'}
                    </div>

                    <div style={{ 
                      fontSize: '12px', 
                      color: '#cbd5e1', 
                      backgroundColor: '#0f172a', 
                      padding: '8px', 
                      borderRadius: '6px', 
                      marginBottom: '10px',
                      fontStyle: 'italic'
                    }}>
                      "{order.reportedIssue || 'Mantenimiento preventivo general'}"
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '8px', borderTop: '1px solid #334155' }}>
                      <span style={{ fontSize: '12px', color: '#94a3b8', display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Wrench size={13} color="#ea580c" />
                        {order.assignedMechanicName || 'Sin asignar'}
                      </span>

                      {column.id !== 'DELIVERED' && (
                        <button 
                          onClick={(e) => {
                            e.stopPropagation();
                            handleAdvanceStatus(order.id, column.id);
                          }}
                          style={{ 
                            background: '#ea580c', 
                            color: '#fff', 
                            border: 'none', 
                            borderRadius: '6px', 
                            padding: '4px 8px', 
                            fontSize: '11px', 
                            fontWeight: '700', 
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '2px'
                          }}
                        >
                          Avanzar <ChevronRight size={12} />
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

      {/* Order Detail Modal */}
      {selectedOrder && (
        <div style={{ 
          position: 'fixed', 
          top: 0, 
          left: 0, 
          right: 0, 
          bottom: 0, 
          backgroundColor: '#000000bb', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center', 
          zIndex: 50,
          padding: '20px'
        }}>
          <div style={{ 
            backgroundColor: '#0f172a', 
            borderRadius: '16px', 
            border: '1px solid #334155', 
            width: '100%', 
            maxWidth: '620px', 
            maxHeight: '90vh', 
            overflowY: 'auto', 
            padding: '24px' 
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px' }}>
              <div>
                <span style={{ 
                  backgroundColor: '#ea580c20', 
                  color: '#f97316', 
                  fontWeight: '800', 
                  fontSize: '14px', 
                  padding: '4px 10px', 
                  borderRadius: '6px' 
                }}>
                  {selectedOrder.plate}
                </span>
                <h2 style={{ fontSize: '20px', fontWeight: '800', color: '#f8fafc', marginTop: '8px' }}>
                  {selectedOrder.motorcycleSummary}
                </h2>
                <p style={{ fontSize: '13px', color: '#94a3b8' }}>
                  Orden #{selectedOrder.id} • Estado: {selectedOrder.status}
                </p>
              </div>
              <button 
                onClick={() => setSelectedOrder(null)}
                style={{ background: 'transparent', border: 'none', color: '#94a3b8', fontSize: '20px', cursor: 'pointer' }}
              >
                ✕
              </button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '16px' }}>
              <div style={{ backgroundColor: '#1e293b', padding: '12px', borderRadius: '10px' }}>
                <span style={{ fontSize: '12px', color: '#94a3b8' }}>Propietario / Cliente</span>
                <p style={{ fontSize: '14px', fontWeight: '700', color: '#f8fafc', marginTop: '2px' }}>
                  {selectedOrder.clientName}
                </p>
                <p style={{ fontSize: '12px', color: '#38bdf8', marginTop: '2px' }}>
                  📞 {selectedOrder.clientPhone || 'Sin teléfono'}
                </p>
              </div>

              <div style={{ backgroundColor: '#1e293b', padding: '12px', borderRadius: '10px' }}>
                <span style={{ fontSize: '12px', color: '#94a3b8' }}>Mecánico de Patio</span>
                <p style={{ fontSize: '14px', fontWeight: '700', color: '#f8fafc', marginTop: '2px' }}>
                  {selectedOrder.assignedMechanicName || 'Sin asignar'}
                </p>
                <p style={{ fontSize: '12px', color: '#94a3b8', marginTop: '2px' }}>
                  ⛽ Combustible: {selectedOrder.fuelLevel || '1/2'}
                </p>
              </div>
            </div>

            <div style={{ backgroundColor: '#1e293b', padding: '14px', borderRadius: '10px', marginBottom: '20px' }}>
              <span style={{ fontSize: '12px', color: '#94a3b8' }}>Falla Reportada por Cliente:</span>
              <p style={{ fontSize: '14px', color: '#f8fafc', marginTop: '4px', lineHeight: '1.4' }}>
                {selectedOrder.reportedIssue}
              </p>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button 
                onClick={() => {
                  const msg = `Hola ${selectedOrder.clientName}, tu moto ${selectedOrder.plate} está siendo atendida en MotoTaller Pro. Estado actual: ${selectedOrder.status}.`;
                  window.open(`https://wa.me/?text=${encodeURIComponent(msg)}`, '_blank');
                }}
                style={{ 
                  backgroundColor: '#15803d', 
                  color: '#fff', 
                  border: 'none', 
                  padding: '10px 16px', 
                  borderRadius: '8px', 
                  fontWeight: '700', 
                  fontSize: '13px', 
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px'
                }}
              >
                <Send size={15} /> WhatsApp Cliente
              </button>
              <button 
                onClick={() => setSelectedOrder(null)}
                style={{ 
                  backgroundColor: '#334155', 
                  color: '#f8fafc', 
                  border: 'none', 
                  padding: '10px 18px', 
                  borderRadius: '8px', 
                  fontWeight: '600', 
                  fontSize: '13px', 
                  cursor: 'pointer' 
                }}
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
