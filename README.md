# CronoSlot 4.4.1

Corrección del build 4.4:
- Restaurado `copyPhotoToInternalStorage(Uri, int, long)` que era referenciado por el selector de fotos pero había quedado fuera de `MainActivity`.
- Mantiene tiempo mínimo de vuelta y separadores.
- No se modifica la detección de cámara.
