# CronoSlot 2.0

Aplicación Android completa basada en la especificación acordada:
- Carrera con piloto, mando, coche y circuito.
- Cámara y detección de cruce existente preservadas.
- Calibración de línea por circuito.
- Registros con filtros de fecha y filtros dinámicos.
- Récords absolutos por pista y récords por piloto.
- Estadísticas de distancia, vueltas, mejor tiempo y velocidad media.
- Datos: pilotos, varios mandos, coches y circuitos; CRUD y fotos con cámara/galería.
- Notas de sesión al terminar.
- Pitido para récord de pista y doble pitido cuando la vuelta también es récord del conjunto piloto+coche en esa pista.
- Excel ordenado por circuito; el selector Android permite elegir Google Drive si está disponible.

El workflow de GitHub Actions instala Gradle 8.9 y no necesita gradlew.


## 2.1.1 build fixes
- Added missing MainActivity label/find helper methods.
- Added android.database.Cursor import to Db.java.
- Corrected CameraActivity ImageProxy API to `getPlanes()`.


## 2.1.2
- Restaurado el pipeline de cámara basado en la implementación que ya estaba funcionando.
- Solicitud explícita de permiso de cámara al entrar.
- PreviewView configurado para rendimiento.
- Gestión de errores de apertura de cámara visible en pantalla.
- Mantiene detección de movimiento, línea calibrable, cronometraje, récords, sonidos y guardado de sesión.
