#!/usr/bin/env python3
"""Generate Spanish interview answers for IDs 223-236."""

import os

BASE = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app/src/main/assets/answers",
)


def fmt(concepto, detalle, cuando, ejemplo, consejo):
    return (
        f"Concepto:\n{concepto}\n\n"
        f"Explicación detallada:\n{detalle}\n\n"
        f"Cuándo usarlo:\n{cuando}\n\n"
        f"Ejemplo:\n{ejemplo}\n\n"
        f"Consejo para entrevista:\n{consejo}\n"
    )


ANSWERS = {
    223: fmt(
        "ADB (Android Debug Bridge) es la herramienta de línea de comandos del Android SDK que permite comunicarse con un dispositivo o emulador por USB o red. Actúa como puente entre el PC y el daemon adbd que corre en el dispositivo (modo Android normal, con depuración USB activada).",
        """El cliente adb en el PC envía órdenes al servidor adb local, que las reenvía al dispositivo. Permite instalar y desinstalar APKs, ver logs, ejecutar shell, reenviar puertos (port forwarding), copiar archivos, capturar pantalla, reiniciar en bootloader/recovery y depurar apps.

Para un desarrollador: depuración con Android Studio, logcat, profiling, tests instrumentados. Para un técnico: diagnóstico de fallos, extracción de logs tras un crash, desbloqueo de pantalla en laboratorio autorizado, sideload de actualizaciones oficiales, reset de caché de app, comprobar si el dispositivo es reconocido.

Requisitos: drivers USB del fabricante (o Google USB Driver), depuración USB activada, y en muchos equipos confirmar la huella RSA del PC. adb kill-server / adb start-server resuelve problemas de conexión frecuentes.

No confundir con Fastboot: ADB opera con Android arrancado (o recovery que expone adbd); Fastboot habla con el bootloader.""",
        "Desarrollo diario, QA, soporte técnico y laboratorios de reparación cuando el sistema arranca y acepta depuración.",
        "adb devices\nadb install app-debug.apk\nadb logcat | grep MyApp\nadb shell pm clear com.example.app\nadb reboot bootloader",
        "Define ADB como puente PC-dispositivo con adbd. Menciona depuración USB y diferencia con Fastboot.",
    ),
    224: fmt(
        "Los comandos ADB más usados cubren instalación, logs, shell, archivos, reinicios y información del dispositivo.",
        """Instalación: adb install [-r reemplazar] [-d permitir downgrade] ruta.apk. adb uninstall <paquete>.

Diagnóstico: adb devices -l (lista), adb get-state, adb shell getprop ro.build.version.release, adb shell dumpsys battery|meminfo|package <pkg>.

Logs: adb logcat, filtros por tag/prioridad, adb bugreport (informe completo, pesado).

Shell: adb shell entra al dispositivo; pm list packages, am start -n para lanzar Activity, settings put global development_settings_enabled 1 (con cuidado).

Archivos: adb push / adb pull entre PC y /sdcard/ o rutas accesibles.

Reinicios: adb reboot, adb reboot bootloader, adb reboot recovery.

Red: adb tcpip 5555 y adb connect IP:5555 para inalámbrico en la misma LAN.

Técnico: adb sideload update.zip en recovery oficial para OTA locales; adb backup (legacy) casi obsoleto.""",
        "Cualquier sesión de depuración, soporte remoto o taller con PC autorizado.",
        "adb shell pm list packages -3\nadb logcat -s AndroidRuntime:E\nadb pull /sdcard/screenshot.png .",
        "Demuestra dominio con logcat, install, shell pm/am y reboot bootloader.",
    ),
    225: fmt(
        "Fastboot es un protocolo y modo de arranque del bootloader que permite flashear particiones, desbloquear OEM y ejecutar comandos de bajo nivel cuando Android no está cargado.",
        """Se accede con adb reboot bootloader o combinación de teclas (varía por marca). fastboot devices lista equipos. Comandos típicos: fastboot flash boot/recovery/system/vbmeta imagen.img, fastboot erase, fastboot oem unlock (según fabricante), fastboot reboot.

A diferencia de ADB, no necesita sistema Android completo ni depuración USB en sentido de app debugging; necesita bootloader desbloqueado o comandos permitidos por el OEM.

Riesgos: flashear imagen incorrecta puede dejar el teléfono en bootloop (brick soft, a veces recuperable). Algunos fabricantes usan herramientas propietarias (Samsung Odin, MediaTek SP Flash, Qualcomm EDL) además o en lugar de fastboot estándar.""",
        "Flasheo de ROM oficial, desbloqueo, reparación de particiones boot/recovery, desarrollo de kernels.",
        "fastboot devices\nfastboot flash boot boot.img\nfastboot reboot",
        "Contrasta con ADB: Fastboot = bootloader, particiones. Menciona riesgo de brick y herramientas OEM.",
    ),
    226: fmt(
        "El bootloader es el primer software que ejecuta el teléfono al encender; carga el kernel y arranca Android o recovery. Suele estar bloqueado por el fabricante.",
        """Bloqueado: solo arranques firmados (verified boot). Desbloquear (OEM unlock) permite flashear particiones custom, instalar recovery alternativo y rootear, pero suele borrar datos (factory reset warning) y anular garantía.

Bootloader bloqueado + verificación activa impide ROMs no firmadas. Algunos fabricantes (Google Pixel, algunos Xiaomi con cuenta) facilitan unlock; otros lo restringen.

Implicaciones legales y de seguridad: dispositivo más vulnerable a malware con privilegios si se compromete el sistema. Empresas MDM pueden detectar bootloader desbloqueado.

Para desarrolladores normales no hace falta desbloquear; para técnico que restaura software o instala ROM oficial vía fastboot a veces sí.""",
        "Flasheo oficial, desarrollo de sistema, modding avanzado. No necesario para apps de Play Store estándar.",
        "Ajustes > Opciones desarrollador > Desbloqueo OEM (si existe)\nfastboot oem unlock",
        "Explica verified boot, wipe de datos al unlock y política de garantía.",
    ),
    227: fmt(
        "Rootear consiste en obtener acceso de superusuario (uid 0) en Android, tradicionalmente con su o Magisk, permitiendo modificar /system o interceptar el arranque con módulos.",
        """Magisk (actual estándar) parchea la imagen boot e instala un manager que concede root por app. Permite módulos sin tocar system (systemless), ocultar root de algunas apps (Zygisk, denylist) aunque bancos y juegos con Play Integrity pueden detectarlo.

Ventajas: backups completos, bloqueadores sistema, automatización avanzada, temas profundos. Riesgos: malware con root, brick si se borran particiones críticas, violación de políticas bancarias/empresa, actualizaciones OTA fallidas.

Root no es lo mismo que bootloader unlock: normalmente se requiere unlock primero. No confundir con jailbreak de iOS.

Desarrollador de apps: debe asumir que en dispositivos rooteados las protecciones locales fallan; usar servidor para datos críticos y Play Integrity donde aplique.""",
        "Modding personal, investigación, herramientas de backup (Titanium-style). Evitar en dispositivos de trabajo con datos sensibles.",
        "Magisk Manager > Instalar > Parchear boot.img > Flashear vía fastboot",
        "Menciona Magisk vs su antiguo, Play Integrity, y riesgos de seguridad.",
    ),
    228: fmt(
        "Flashear es escribir una imagen de firmware en particiones del dispositivo (boot, system, vendor, super, recovery, etc.) para instalar, actualizar o reparar el sistema operativo.",
        """Tipos: OTA oficial (incremental o full), fastboot flash de imágenes sueltas, herramientas OEM (Odin AP/BL/CP/CSC, Mi Flash, QPST), recovery sideload (adb sideload), EDL/QPST en Qualcomm cuando el dispositivo está en modo emergencia.

ROM custom (LineageOS, etc.): requiere unlock, recovery custom, zip de la ROM y a menudo GApps por separado. Siempre usar builds para el modelo exacto (codename).

Precauciones: batería cargada, cable de calidad, backup de datos, verificar checksums, no interrumpir el proceso. Brick duro si se corrompe partición crítica sin modo recuperación.

Técnico restaura software tras fallo de actualización, pantalla rota con ADB activo, o cambio de región en equipos permitidos por el fabricante.""",
        "Reparación, downgrade/upgrade oficial, instalación de ROM cuando el usuario lo solicita y es legal.",
        "fastboot flashall -w   # script del fabricante, usar solo paquete correcto\nadb sideload ota.zip   # en recovery oficial",
        "Diferencia OTA vs fastboot vs Odin. Insiste en modelo exacto y riesgo de brick.",
    ),
    229: fmt(
        "Recovery es una partición o modo de arranque mínimo para mantenimiento: factory reset, aplicar OTA, sideload, montar /data y a veces backups.",
        """Stock recovery: limitado, firmado por OEM, wipe data/cache, OTA oficial. Custom recovery (TWRP, OrangeFox): interfaz táctil, backups Nandroid, flash zips, explorador de archivos, adb en recovery.

Se entra con adb reboot recovery o teclas (Vol+ + Power según marca). Sin unlock muchos equipos no permiten recovery custom.

Wipe data/factory reset borra apps y datos de usuario; wipe cache dalvik/ART menos drástico. Formatear /data en TWRP equivale a reset completo.

Técnico usa recovery para desbloquear patrón olvidado solo en dispositivos propios o autorizados; en la práctica legal depende de la jurisdicción y política de la empresa.""",
        "Actualizaciones manuales, wipes, flash de Magisk/ROM en entornos controlados.",
        "adb reboot recovery\n# En TWRP: Install > zip de ROM o Magisk",
        "Stock vs TWRP, wipe data vs cache, sideload en recovery oficial.",
    ),
    230: fmt(
        "OEM unlock es la opción del fabricante que permite desbloquear el bootloader. Verified boot (AVB) comprueba la firma de cada partición en el arranque; dm-verity protege particiones montadas contra modificación.",
        """Con bootloader bloqueado, solo imágenes firmadas arrancan. Tras unlock, el estado pasa a orange (Google) o advertencia en pantalla de inicio. Magisk puede parchear vbmeta para desactivar verificación en algunos casos (riesgo de seguridad).

Play Integrity / SafetyNet evalúan bootloader, firma y root; apps bancarias pueden negar servicio.

Para técnico: restaurar stock completo y volver a bloquear bootloader (si el OEM lo permite) puede ser necesario para devolver un equipo a garantía o cumplir política corporativa.""",
        "Entender por qué root/ROM rompe apps con integridad; soporte avanzado de flasheo.",
        "fastboot flashing unlock\nfastboot getvar unlocked",
        "Orange state, AVB, relación con Magisk y apps bancarias.",
    ),
    231: fmt(
        "Un técnico Android en taller combina diagnóstico de hardware/software, restauración de firmware, gestión de cuenta (FRP), limpieza de almacenamiento y orientación al usuario.",
        """Tareas habituales: comprobar puerto USB y modo (MTP, PTP, solo carga), activar depuración si es posible, extraer logs con adb bugreport, reinstalar ROM oficial, reset de fábrica, sustitución de batería/pantalla, test de sensores en menú secreto (*#*#4636#*#* en algunos), verificar IMEI y bandas.

FRP (Factory Reset Protection): tras reset, pide cuenta Google previa; no se “salta” legalmente sin credenciales del dueño; técnico debe explicar al cliente.

Herramientas por fabricante: Odin (Samsung), Mi Flash (Xiaomi), MSM Download Tool (OnePlus/OPPO), SP Flash Tool (MediaTek), HiSuite/Honor, EDL cables en Qualcomm.

Modo download/EDL: arranque de emergencia cuando no enciende Android; requiere autorización y firmware firmado. Distinto de fastboot visible.

Ética: solo operar en dispositivos de propiedad demostrada; respetar privacidad de datos.""",
        "Centros de servicio, reparación de móviles, soporte IT corporativo con dispositivos Android.",
        "Diagnóstico: adb devices, cargador original, recovery wipe cache, flash ROM oficial del sitio del OEM",
        "Menciona FRP, Odin/Mi Flash, y límites legales/éticos.",
    ),
    232: fmt(
        "Android clasifica permisos en normal, peligroso (dangerous/runtime), firma (signature) y especial (special/appop). La protección depende del nivel de riesgo para privacidad e integridad.",
        """Normal (INTERNET, VIBRATE): concedidos en instalación sin diálogo. Dangerous (CAMERA, LOCATION, READ_CONTACTS): declarados en manifest y solicitados en runtime; el usuario puede denegar. Signature: solo apps firmadas con la misma clave que el sistema o el OEM. Special: permisos de alto impacto gestionados en Ajustes (WRITE_SETTINGS, MANAGE_EXTERNAL_STORAGE, SYSTEM_ALERT_WINDOW).

Desde Android 6 (API 23) los dangerous requieren requestPermissions o Activity Result API. Android 13+ granularidad: POST_NOTIFICATIONS, READ_MEDIA_* en lugar de READ_EXTERNAL_STORAGE único.

Principio de mínimo privilegio: declarar solo lo necesario; usar maxSdkVersion para permisos legacy que ya no aplican. Revisar declaraciones fusionadas de librerías con merged manifest.""",
        "Diseño de manifest, code review de privacidad, preparación para Play Console (Data safety).",
        "AndroidManifest.xml uses-permission\nregisterForActivityResult(RequestPermission())",
        "Cuatro tipos + runtime desde API 23. Relaciona con pregunta de permisos peligrosos (id 103).",
    ),
    233: fmt(
        "Los permisos de ubicación controlan acceso a GPS, red y Bluetooth (beacons). Incluyen ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION y desde Android 10 ACCESS_BACKGROUND_LOCATION por separado.",
        """Foreground: suficiente para mapas en uso. Background: tracking continuo; Google Play exige justificación fuerte y formulario; el usuario debe conceder en dos pasos (primero foreground, luego background en Ajustes).

Android 12+: aproximada vs precisa (COARSE vs FINE). Sin permisos, usar solo ubicación aproximada por IP en servidor si la política lo permite.

Seguridad: no loguear coordenadas en producción; almacenar mínimo necesario; informar en política de privacidad; ofrecer desactivar tracking; fuzzing de ubicación en emulador para tests.

FusedLocationProviderClient pausa actualizaciones en onStop si no necesitas background. Geofencing y Activity Recognition tienen APIs propias con reglas similares.""",
        "Apps de mapas, delivery, fitness, tiendas cercanas. Evitar background si no es core de la app.",
        "if (hasFineLocation()) fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token)",
        "Foreground vs background, doble permiso Play, FusedLocationProvider.",
    ),
    234: fmt(
        "El acceso a archivos evolucionó de almacenamiento amplio (READ/WRITE_EXTERNAL_STORAGE) al modelo de sandbox y MediaStore con permisos granulares por tipo de medio.",
        """Android 10+: scoped storage; apps acceden a su directorio y a URIs compartidos vía SAF (ACTION_OPEN_DOCUMENT, CREATE_DOCUMENT). Android 13+: READ_MEDIA_IMAGES/VIDEO/AUDIO sustituyen lectura amplia de imágenes.

MANAGE_EXTERNAL_STORAGE (All files access): solo para gestores de archivos, backup, antivirus; revisión estricta en Play; pedir en Ajustes, no runtime clásico.

Seguridad: no exponer file:// en Intents (usar FileProvider); validar MIME y tamaño; escanear malware en servidor si suben archivos; no guardar PII en Downloads público.

WRITE_EXTERNAL_STORAGE deprecated en target altos. Para compartir entre apps: content:// con permisos temporales FLAG_GRANT_READ_URI_PERMISSION.""",
        "Galería, editor, backup, exportar PDF. Usar SAF para archivos arbitrarios del usuario.",
        "val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)",
        "Scoped storage, SAF, READ_MEDIA_* y por qué MANAGE_EXTERNAL_STORAGE es excepcional.",
    ),
    235: fmt(
        "Cámara, micrófono y contactos son permisos dangerous de alto impacto en privacidad; requieren justificación clara y manejo de denegación.",
        """CAMERA / RECORD_AUDIO: solicitar just-in-time cuando el usuario pulsa grabar o escanear QR. Indicador verde/naranja en barra de estado (Android 12+) muestra uso activo. No grabar en background sin consentimiento explícito (ilegal en muchas regiones).

READ_CONTACTS / WRITE_CONTACTS: minimizar campos leídos; no subir libreta completa al servidor sin necesidad. GET_ACCOUNTS restringido.

READ_SMS / SEND_SMS / CALL_PHONE: muy sensibles; Play limita casos de uso (apps por defecto SMS/teléfono). Riesgo de fraude si se abusa.

Buenas prácticas: rationale antes del segundo request, degradar funcionalidad si deniegan, enlace a Ajustes si denegación permanente. Revisar que librerías de terceros no declaren permisos extra.

POST_NOTIFICATIONS (Android 13+): dangerous; pedir al activar notificaciones, no al instalar.""",
        "Apps de mensajería, banca con KYC, escáneres, VoIP. Auditar manifest de SDKs de ads y analytics.",
        "ActivityResultContracts.RequestMultiplePermissions() para CAMERA + RECORD_AUDIO",
        "Just-in-time, indicadores de privacidad OS, POST_NOTIFICATIONS separado.",
    ),
    236: fmt(
        "Además de los dangerous clásicos existen permisos especiales que se conceden desde Ajustes del sistema o mediante AppOps, por su alto poder sobre el dispositivo.",
        """SYSTEM_ALERT_WINDOW (dibujar sobre otras apps): overlays; riesgo de phishing; solicitar con Settings.canDrawOverlays y ACTION_MANAGE_OVERLAY_PERMISSION. PACKAGE_USAGE_STATS: estadísticas de uso. REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: exención Doze (justificar en Play). SCHEDULE_EXACT_ALARM (Android 12+): alarmas exactas. NEARBY_WIFI_DEVICES (Android 13+): sin ubicación para WiFi scan en algunos casos.

WRITE_SETTINGS: modificar ajustes globales. MANAGE_EXTERNAL_STORAGE: todos los archivos. BIND_ACCESSIBILITY_SERVICE: automatización potente, vector de malware si el usuario activa servicios maliciosos.

Seguridad: explicar al usuario por qué abre Ajustes; nunca engañar para obtener permisos; auditar que solo servicios de accesibilidad necesarios estén en el manifest. Play Console declara usos sensibles.

AppOps puede revocar permisos en runtime aunque el usuario aceptó antes (modo “permitir solo esta vez”).""",
        "Funciones avanzadas (burbujas, VPN propia, automatización). Evitar pedir overlay si no es esencial.",
        "if (!Settings.canDrawOverlays(ctx)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri))",
        "Lista 3-4 especiales con riesgo (overlay, accessibility, all files). Mencionar AppOps.",
    ),
}

os.makedirs(BASE, exist_ok=True)
for qid, text in ANSWERS.items():
    path = os.path.join(BASE, f"{qid}.txt")
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"Wrote {path}")

print(f"Done: {len(ANSWERS)} files")
