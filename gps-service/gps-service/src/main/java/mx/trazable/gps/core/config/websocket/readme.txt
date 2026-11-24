MICROSERVICIO GPS - TRAZABLE

DESCRIPCIÓN

Microservicio desarrollado con Java 21, Spring Boot y PostgreSQL para recibir ubicaciones GPS mediante WebSocket y almacenarlas únicamente cuando estén asociadas a viajes activos.

1. INICIALIZACIÓN

Al iniciar Spring Boot, GpsActiveTrackingService consulta PostgreSQL y carga los viajes activos en un ConcurrentHashMap.

Cada viaje contiene:
- appointmentId
- orgId
- plate
- carrierId
- lat y lng del destino
- radius
- status

Ejemplo de clave en memoria:
3:RTW4RF

2. CONEXIÓN DEL PROVEEDOR

Endpoint local:
ws://localhost:8080/api/ws/gps

El proveedor se autentica mediante JWT.

GpsHandshakeInterceptor y GpsTokenValidator validan el token y construyen un GpsIdentity.

Ejemplo de identidad:
orgId = 3
supplierGpsId = 12
carrierId = 45

SessionManager registra la conexión.

3. ENVÍO DE UBICACIONES

Ejemplo de JSON enviado por el proveedor:

```json
{
  "imei": "123456789012345",
  "plate": "RTW-4RF",
  "latitude": 20.1234567,
  "longitude": -98.1234567,
  "instant": "2026-11-24T01:05:00Z",
  "isIgnition": true,
  "type": "LOCATION",
  "additionalData": {}
}
```

El appointmentId no se envía, ya que el servidor lo asigna automáticamente.

4. VALIDACIÓN DE PLACA Y VIAJE

GpsWebSocketHandler valida el mensaje y normaliza la placa mediante PlateUtils.

Ejemplos:
RTW-4RF -> RTW4RF
rtw 4rf -> RTW4RF
RTW4RF -> RTW4RF

Posteriormente:
- Busca la placa en los viajes activos de la organización.
- Verifica que coincida el carrierId del transportista.
- Obtiene el appointmentId correspondiente.
- Ignora los pulsos de placas sin viajes activos.

5. CONTROL DE FRECUENCIA

Se acepta como máximo un pulso cada 3 segundos por organización y placa.

Ejemplo:
12:00:00 -> Aceptado
12:00:01 -> Descartado
12:00:03 -> Aceptado

Se utiliza Caffeine para controlar la frecuencia sin crecimiento indefinido de memoria.

6. PROCESAMIENTO Y FILTRADO

GpsIngestionService recibe los pulsos y los incorpora a una cola.

GpsProcessingWorker consume la cola y utiliza GpsGeoFilterService para filtrar ubicaciones.

Reglas:
- Primera ubicación: guardar.
- Movimiento >= 15 metros: guardar.
- Intervalo >= 60 segundos: guardar.
- Cambio de appointmentId: guardar.
- Cambio de encendido: guardar.
- Eventos diferentes de LOCATION: guardar.
- Ubicaciones repetidas o redundantes: descartar.

7. PERSISTENCIA

GpsPersistenceService guarda los pulsos aceptados en PostgreSQL.

Tabla:
bem_gps_tracking

Ejemplo de consulta:

```sql
SELECT
    id,
    plate,
    appointment_id,
    latitude,
    longitude,
    instant
FROM bem_gps_tracking
WHERE plate = 'RTW4RF'
ORDER BY id DESC
LIMIT 10;
```

La última posición confirmada se actualiza en memoria después de guardar correctamente el lote.

8. EJEMPLOS DE LOGS

Conexión exitosa:
GPS CONNECTED | supplier=12 | carrier=45 | active=1

Carga inicial:
GPS TRACKING INITIALIZED | total=1

Pulso recibido:
GPS RECEIVED | plate=RTW4RF | appointment=150 | org=3 | queueSize=1

Persistencia:
GPS SAVED | records=1

Transportista incorrecto:
GPS CARRIER MISMATCH | plate=RTW4RF | carrier=45

Placa sin viaje activo:
GPS INACTIVE PLATE | plate=ABC123 | org=3

9. COMPONENTES PRINCIPALES

GpsWebSocketConfig: configura el WebSocket.
GpsHandshakeInterceptor: autentica conexiones.
GpsTokenValidator: valida JWT.
SessionManager: administra sesiones.
GpsWebSocketHandler: recibe y valida pulsos.
GpsTrackingRepository: consulta viajes activos.
GpsActiveTrackingService: administra viajes en memoria.
PlateUtils: normaliza placas.
GpsIngestionService: administra la cola.
GpsProcessingWorker: procesa pulsos.
GpsGeoFilterService: filtra ubicaciones.
GpsPersistenceService: guarda en PostgreSQL.

10. FUNCIONALIDADES PENDIENTES

- WebSocket administrativo para activar y finalizar viajes.
- Actualización de viajes en memoria sin reiniciar Spring Boot.
- Detección automática de llegada al destino mediante latitud, longitud y radio.
- Soporte de coordenadas para destinos CLIENTE.
- Pruebas de integración y rendimiento.

OBJETIVO FINAL

Recibir ubicaciones GPS de múltiples proveedores, identificar automáticamente la cita correspondiente y almacenar únicamente las posiciones relevantes de unidades con viajes activos.