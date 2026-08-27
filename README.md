# CronoSlot 3.0.4

Corrección puntual de compilación:
- En `MainActivity.java`, la lambda del menú principal usaba el índice `i` del bucle.
- Ahora captura un `final int menuIndex`, válido para Java.
- No se modifica la cámara ni la lógica funcional.

Error corregido:
`local variables referenced from a lambda expression must be final or effectively final`
