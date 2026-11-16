1) El proveedor se conecta
GpsWebSocketConfig dirige la conexión al interceptor.

2) Se valida el JWT
GpsTokenValidator obtiene orgId=3, supplierGpsId=12 y carrierId=45, y se construye un GpsIdentity.

3) Se registra la sesión
GpsWebSocketHandler llama a SessionManager para registrar la conexión.

4) Llega una ubicación
El proveedor envía un JSON con IMEI, latitud, longitud y fecha. Jackson lo transforma en GpsPulseDTO.

5) Se valida y encola
El handler revisa los campos y GpsIngestionService crea un GpsQueuedPulse con el pulso y la identidad autenticada.

6) Se guarda en PostgreSQL
Un worker deberá consumir la cola, comprobar las relaciones y guardar en bem_gps_tracking.