# CronoSlot 4.0.2

Correcciones del build 4.0.1:
- Restaurada la dependencia Apache POI requerida por `WorkbookBuilder`.
- Adaptado `WorkbookBuilder` al modelo de un mando por piloto.
- Añadido `Db.bestTrack(long)` requerido por `CameraActivity`.
- La pantalla Exportar usa ahora `WorkbookBuilder` para crear el XLSX.
- Se mantienen AndroidX, Java 17, CameraX y la lógica de detección.
