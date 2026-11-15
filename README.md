# Comisiones OPPO · Android

Aplicación Android WebView independiente de PWABuilder, que utiliza el sitio de producción existente. No modifica Vercel, Supabase, sesiones o ventas existentes. Android 8.0+; requiere conexión. Iniciar sesión dentro de la app con la cuenta existente (los datos locales del navegador no se migran). La pantalla de registro y las tarifas proceden del sitio publicado. Si el registro requiere confirmar correo, abrir el enlace del correo y luego volver a iniciar sesión dentro de la app.

## Compilación en GitHub Actions

Subir todos los archivos a un repositorio privado, incluyendo `.github/workflows/android.yml`. Ejecutar el workflow «Compilar APK Android». Descarga el artefacto desde la ejecución, descomprime y abre `ComisionesOPPO-prueba.apk` en el teléfono. Autorizar instalación desde la fuente si Android lo solicita. El APK de prueba usa la firma de depuración, que puede cambiar en cada ejecución; no distribuirlo como versión final. Para distribución estable, usar una clave privada de firma conservada fuera del repositorio y configurar la firma release (variables OPPO_KEYSTORE_PATH, OPPO_KEYSTORE_PASSWORD, OPPO_KEY_ALIAS, OPPO_KEY_PASSWORD). Nunca subir claves privadas al repositorio.

## Pruebas pendientes en un dispositivo Android

Instalación, apertura, login (válido e inválido), registro y confirmación de correo, guardar y recargar ventas, cierre de sesión, separación entre cuentas, exportación CSV, navegación atrás y recuperación sin red. No usar datos reales de otros vendedores en pruebas. La exportación CSV depende de la versión de Android System WebView y debe verificarse; Chrome puede seguir usándose si el exportador nativo no funciona. La app no funciona sin internet y no requiere permisos de contactos, cámara ni ubicación.
