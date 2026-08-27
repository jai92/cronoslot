# CronoSlot 1.0.0

Proyecto Android Studio.

Incluye:
- Menú: Carrera, Registros, Récords, Estadísticas, Datos, Calibración y Exportar.
- SQLite local para pilotos, coches, circuitos, sesiones y vueltas.
- Selección de piloto/coche/pista antes de carrera.
- Cámara CameraX con línea de detección movible.
- Detección de movimiento y bloqueo contra dobles detecciones.
- Confirmación al terminar y notas de sesión.
- Récords por pista y piloto+coche.
- Estadísticas básicas de distancia y tiempos.
- Exportación XLSX; el selector de Android permite elegir Google Drive si está instalado/conectado.

Limitaciones de esta 1.0:
- La foto de pilotos/coches/circuitos y la edición/eliminación avanzada de fichas todavía son la siguiente iteración.
- La detección de cámara es un detector de movimiento inicial y debe probarse en la pista real; no se presenta como un sistema de visión industrial.
- Los pitidos de récord están preparados de forma básica; la lógica de récord y doble pitido se puede endurecer tras probar el detector real.
