# VisionRT

Aplicacion Android de asistencia visual para personas con discapacidad
visual. Detecta obstaculos y personas en tiempo real usando la camara
del dispositivo, y emite alertas por voz y vibracion.

Todo el procesamiento se ejecuta localmente en el dispositivo con modelos de imagen a clasificación. No se
envia informacion a servidores externos.

## Estado del proyecto

| Componente | Estado |
|------------|--------|
| Deteccion de obstaculos | Funcional |
| Deteccion de personas a 8 m | Funcional (YOLOv11n @ 640) |
| Alertas por voz (TTS) | Funcional |
| Alertas por vibracion | Funcional |
| Lectura de texto (OCR) | Funcional |
| Resumen de objetos | Funcional |
| Modo degradado | Funcional |
| Publicacion en Play Store | Pendiente |

Version actual: `0.8.0-beta.1`

## Arquitectura

El proyecto esta organizado como un conjunto de modulos Gradle con
responsabilidades separadas.

```
app/         Shell Android, inyeccion de dependencias (Hilt), UI
core/        Dominio, orquestacion, politicas de alerta, OCR, speech
data/        Persistencia (DataStore)
perception/  Captura de camara, frame gate, preprocesado de tensores
inference/   LiteRT/TFLite, ML Kit OCR, postprocesado
feature/     UI por feature (home, onboarding, settings, help, ...)
feedback/    Dispatcher de TTS, vibracion y earcons
benchmark/   Medicion de latencia y matriz de dispositivos
```

Para el detalle completo, consulta `docs/ARCHITECTURE.md`.

## Stack tecnologico

- Kotlin, Android SDK 36 (minSdk 30).
- Hilt para inyeccion de dependencias.
- LiteRT (TensorFlow Lite) para inferencia on-device.
- ML Kit para OCR.
- DataStore Preferences para ajustes.
- YOLOv11n @ 640 int8 como modelo de deteccion.

## Retos encontrados durante el desarrollo

| Retos | Solución propuesta |
| Gestión de batería y recursos | Se implementaron 3 modos de consumo y sistema automático que reduce la potencia del modelo a un mínimo funcional |
| Detección de falsos positivos | Detectaba objetos que no estaban allí, se decidió implementar un modelo más pesado de YOLO que tuviera una precisión dentro del umbral de >60% |
| Accesibilidad | Se implementó un sistema OCR tipo narrador que lee los botones en voz alta, la UI es simple y con botones grandes también asistidos por narrador. El hardware mínimo de funcionalidad es un móvil con 3GB de ram libres |

## Requisitos de compilacion

- JDK 17 o superior.
- Android SDK 36.
- Gradle 8.x (incluido en el wrapper).

## Compilar

```bash
# Debug
./gradlew :app:assembleDebug

# Release (requiere keystore configurado, ver mas abajo)
./gradlew :app:assembleRelease
```

## Ejecutar tests

```bash
# Tests unitarios
./gradlew :core:test

# Tests instrumentados (requiere dispositivo conectado)
./gradlew :app:connectedAndroidTest
```

## Instalar en un dispositivo

```bash
# Debug
./gradlew :app:installDebug

# Release
adb install app/build/outputs/apk/release/app-release.apk
```

> [!NOTE]
> El APK de release se firma con un keystore configurado en
> `~/.gradle/gradle.properties`. Las contrasenas nunca se almacenan en
> el repositorio.

## Configuracion del keystore para release

Crea el keystore una sola vez:

```bash
keytool -genkeypair -v \
  -keystore visionrt-release.jks \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -alias visionrt \
  -storetype JKS
```

Anade las contrasenas a `~/.gradle/gradle.properties` (fuera del repositorio):

```properties
visionrt.storePassword=TU_PASSWORD
visionrt.keyPassword=TU_PASSWORD
```

## Modelo de deteccion

El modelo activo es `yolo11n_640_int8.tflite` (YOLOv11n, entrada 640x640,
cuantizacion int8). Se encuentra en `app/src/main/assets/models/`.

Para reemplazarlo:

1. Exporta el modelo con `tools/model_conversion/`.
2. Actualiza `app/src/main/assets/models/manifest.json` con el nuevo
   nombre de archivo, dimensiones de entrada y checksum SHA-256.
3. Recompila e instala.

Consulta `tools/model_conversion/README.md` para el procedimiento
completo.

## Estructura de la documentacion

- `docs/ARCHITECTURE.md`: diseno del sistema y decisiones.
- `docs/REQUIREMENTS.md`: requisitos funcionales y no funcionales.
- `docs/ROADMAP.md`: plan de desarrollo por hitos.
- `docs/DATASET_SCHEMA.md`: esquema de datos para validacion.
- `docs/DEVICE_PROCUREMENT.md`: dispositivos de referencia.
- `docs/M7_REPORTS.md`: reportes de rendimiento y estabilidad.

## Estado de la version beta

La version `0.8.0-beta.1` esta publicada como pre-release en GitHub.
Consulta la seccion de Releases para descargar el APK.

> [!WARNING]
> Esta aplicacion es una ayuda complementaria. No reemplaza el uso de
> baston, perro guia u otras ayudas certificadas. No debe usarse como
> unico medio de orientacion en entornos desconocidos o peligrosos.

## Contribuir

1. Haz fork del repositorio.
2. Crea una rama con un nombre descriptivo (`fix/deteccion-intermitente`,
   `feat/nuevo-modelo`, etc.).
3. Ejecuta `./gradlew :core:test` antes de hacer commit.
4. Envia un pull request describiendo el cambio y su motivacion.

> [!IMPORTANT]
> Antes de proponer un cambio en el pipeline de deteccion, ejecuta el
> benchmark con `DetectorBenchmarkRunner` y adjunta los resultados
> (p50, p95, p99) al pull request.

## Licencia

Ver el archivo LICENSE en la raiz del repositorio.

## Contacto

Repositorio: https://github.com/beoasaver-boop/Vision-RT

Issues: https://github.com/beoasaver-boop/Vision-RT/issues
