# Música

Música es un reproductor de audio local para Android (Android 13 o superior). Escanea los archivos MP3 almacenados en el dispositivo y crea automáticamente listas de reproducción basadas en la organización de carpetas, además de una lista combinada **Toda la música**.

## Características

- **Nombre de la aplicación**: Música (en español).
- **Control de reproducción**: Botones de reproducción/pausa, canción anterior/siguiente, modo aleatorio y repetir canción actual.
- **Listas por carpetas**: Genera listas de reproducción basadas en la carpeta donde se encuentra cada archivo MP3.
- **Lista global**: Incluye la lista de reproducción "Toda la música" con todas las canciones MP3 del dispositivo.
- **Búsqueda**: Barra de búsqueda en la parte superior central para buscar canciones dentro de la lista actual.
- **Botón de actualización**: Ubicado en la esquina superior derecha para reescanear la música del dispositivo y actualizar las listas.
- **Botón de selección de lista**: Ubicado en la esquina superior izquierda para seleccionar la lista activa.
- **Compatibilidad**: Diseñado para Android 13 (API 33) o superior.

## Requisitos

- Android Studio con SDK Platform 35 y JDK 17
- Dispositivo físico o emulador con Android 13 (API 33) o superior

## Compilación y ejecución

1. Abre este proyecto en Android Studio y permite que Gradle sincronice la configuración.
2. Selecciona y ejecuta el módulo `app` en un emulador o teléfono con Android 13 o superior.
3. Al iniciar la app, concede el permiso de acceso a música y audio cuando la pantalla lo solicite.

## Crear un APK

En Windows, ejecuta `.\gradlew.bat assembleDebug` desde la carpeta del proyecto. El APK instalable se genera en `musica.apk` en la raíz del proyecto (también queda disponible en `app\build\outputs\apk\debug\app-debug.apk`). Esta compilación de depuración está firmada automáticamente con la clave de depuración local; para distribuir versiones de lanzamiento o publicar en Google Play, configura una clave de firma privada y persistente.
