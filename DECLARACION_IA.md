# Declaración de uso de IA

## Para qué la usé

- Usé Codex con el modelo GPT-6.1 Sol para detectar fallos y ayudarme con la implementación. La ayuda incluyó sugerencias y generación y edición de código.
- Pedí ayuda con Firebase, ubicación, acelerómetro, mapa y revisión de errores. También usé el asistente de Android Studio para revisar advertencias y ordenar textos.


## Qué hice y revisé yo

### Estructura del proyecto

- Creé el proyecto en Android Studio usando Java.
- Creé las cuatro pantallas de la app y sus archivos de diseño: inicio, registro, listado y mapa.
- Ajusté los valores de minSdk y targetSdk en el archivo Gradle para asegurar la compatibilidad con el sistema.

### Diseño de las pantallas

- Agregué los botones del inicio y la navegación entre las pantallas.
- Elegí mantener una interfaz sencilla, con los campos del formulario y los mensajes a la vista. Los textos y las categorías del proyecto están en `strings.xml`.
- Ajusté los márgenes, tamaños de letra y la alineación de los elementos en los XML para que se vean ordenados.

### Configuración de Firebase

- Creé el proyecto en Firebase, habilité el acceso anónimo y descargué el archivo de configuración de Android.
- Creé la base de datos y publiqué las reglas para limitar el acceso a los registros de cada usuario.
- Ubiqué manualmente el archivo `google-services.json` dentro de la carpeta `app` y revisé que la conexión no diera errores.

### Pruebas en un dispositivo Android

- Activé la depuración inalámbrica y conecté el dispositivo a Android Studio por Wi-Fi.
- Ejecuté la app inicial y probé la navegación entre las pantallas.
- Probé ingresar datos vacíos en el formulario y mover el teléfono para verificar la lectura del acelerómetro.

### Versiones y documentación

- Hice los commits y pushes a GitHub para ir guardando los avances.
- Revisé el README y esta declaración, y fui cambiando el texto y la presentación para dejarlos más simples.
- Configuré el `.gitignore` para no subir archivos locales con claves privadas al repositorio.

## Ejemplos de consultas

Estos ejemplos resumen algunas consultas que hice durante el trabajo:

- Cómo revisar las advertencias del proyecto y distinguirlas de los errores que impiden ejecutar la app.
- Cómo guardar y consultar las incidencias en Firebase y obtener la ubicación con permiso del usuario.
- Cómo mostrar los valores del acelerómetro y mantener el código sencillo.
