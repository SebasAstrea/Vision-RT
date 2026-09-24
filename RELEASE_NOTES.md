# VisionRT 0.8.0-beta.1 — Long-range detection

Primera beta pública de VisionRT con detección de personas y obstáculos
a larga distancia (aproximadamente 8 metros) en dispositivos Android de
gama baja.

## Novedades

- **YOLOv11n @ 640 int8** (3.8 MB): detección de personas y obstáculos
  a 8 metros. Reemplaza el modelo anterior YOLOv8n @ 320, que no
  alcanzaba esa distancia.
- **Interrupción selectiva de TTS**: una alerta critica ya no corta a
  otra alerta critica a mitad de palabra. La politica de feedback
  compara la severidad de la alerta entrante con la que esta en curso.
- **AlertPolicy estable**: el seguimiento de objetos ya no se reinicia
  cuando la persona cruza los limites laterales del encuadre.
- **Icono de aplicacion** con adaptive icon para Android 8 y superior.
- **APK optimizado**: 32 MB. Se limitaron las ABIs a arm64-v8a y
  armeabi-v7a, y se activo el shrinker de recursos.

## Rendimiento medido

Dispositivo de referencia: Samsung SM-A226BR (MediaTek MT6833, 3 GB RAM).

| Metrica | Valor |
|---------|-------|
| Latencia p95 de inferencia | 314 ms (presupuesto 450 ms) |
| Pico de memoria | 177.9 MB |
| Nivel de recursos | NORMAL (sin degradacion) |
| maxScore mediano | 0.80 - 0.86 |
| FPS efectivos | ~2.4 |

> [!NOTE]
> El modelo se ejecuta completamente on-device con LiteRT (XNNPACK). No
> se envia ninguna imagen ni dato de sensor a servidores externos.

## Instalacion

1. Descarga el archivo `VisionRT-0.8.0-beta.1.apk` adjunto a esta release.
2. En el dispositivo, activa la instalacion desde fuentes desconocidas:
   Ajustes > Seguridad > Instalar apps desconocidas.
3. Abre el APK descargado y acepta la instalacion.
4. Al abrir la aplicacion, concede los permisos de camara y vibracion.

> [!IMPORTANT]
> Este es un APK de tipo beta. Puede contener errores no detectados en
> pruebas internas. Reporta cualquier problema en la seccion de Issues
> del repositorio.

## Requisitos

- Android 11 o superior (API 30+).
- Procesador ARM64 (arm64-v8a) o ARMv7 (armeabi-v7a).
- Camara trasera funcional.
- Espacio libre: aproximadamente 32 MB.

## Cambios incompatibles

Ninguno. Es la primera release publica.

## Feedback

Reporta errores y sugerencias en:
https://github.com/beoasaver-boop/Vision-RT/issues

Al reportar un error, incluye si es posible:

- Modelo del dispositivo y version de Android.
- Salida de `adb logcat` filtrada por la etiqueta `VisionRT`.
- Descripcion del escenario (persona a 8 metros, obstaculo lateral,
  lectura de texto, etc.).

> [!WARNING]
> La aplicacion no reemplaza el uso de baston, perro guia o cualquier
> otra ayuda tecnica certificada. Es una herramienta complementaria en
> fase de desarrollo.

## Privacidad

Todo el procesamiento de vision ocurre localmente en el dispositivo. La
aplicacion no requiere conexion a internet para funcionar y no envia
imagenes, audio ni datos de sensores a ningun servidor.

## Licencia

Ver el archivo LICENSE en la raiz del repositorio.

---

Artefacto: `VisionRT-0.8.0-beta.1.apk`

Modelo: `yolo11n_640_int8.tflite`

SHA-256 del modelo: `3d1e489fe1ad2e2071dc3422a6ddb6b482049ed4b44195c92254aae9c991bcd9`
