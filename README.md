# 🎓 MoodleSync

Aplicación Android para sincronizar automáticamente las tareas de Moodle y recibir notificaciones inteligentes según la urgencia de entrega.

Desarrollada por **Renzo Povis** — Estudiante de DAM (Desarrollo de Aplicaciones Multiplataforma)

---

## 📱 ¿Qué hace la app?

- Se conecta a cualquier Moodle mediante calendario iCal
- Sincroniza automáticamente las tareas cada 30 minutos en segundo plano
- Muestra las tareas ordenadas por urgencia con colores:
  - 🔴 Rojo — menos de 24 horas
  - 🟠 Naranja — menos de 3 días
  - 🟡 Amarillo — menos de 1 semana
  - 🟢 Verde — más de 1 semana
- Envía notificaciones inteligentes cuando una entrega se acerca
- Permite marcar tareas como completadas
- Organiza las tareas por asignatura mediante pestañas
- Modo oscuro y claro
- Cualquier estudiante puede usarla con su propia URL de Moodle

---

## 🌐 Versión Web (iPhone/iPad)

También disponible como Progressive Web App para dispositivos Apple:

👉 **[Abrir MoodleSync Web](https://renzopovisc-cmd.github.io/moodlesync-web)**

Para instalarla en iPhone/iPad:
1. Abre el enlace en Safari
2. Toca el botón compartir ↑
3. Toca "Añadir a pantalla de inicio"
4. Ya aparece como una app normal 🎓

---

## 🛠️ Tecnologías utilizadas

- **Kotlin** — lenguaje principal
- **Jetpack Compose** — interfaz de usuario
- **Room Database** — almacenamiento local
- **WorkManager** — sincronización en segundo plano
- **iCal (.ics)** — conexión con Moodle
- **MVVM** — arquitectura del proyecto

---

## 🚀 Cómo instalar

1. Clona el repositorio
2. Ábrelo con Android Studio
3. Conecta un móvil Android
4. Dale a Run ▶️
5. Al abrir la app, genera tu URL de calendario en Moodle y pégala

---

## 📋 Cómo obtener tu URL de Moodle

1. Entra a tu Moodle
2. Ve a Calendario → Exportar
3. Selecciona "Cursos" y "60 días"
4. Dale a "Genera URL del calendario"
5. Copia la URL y pégala en la app

---

## 📁 Estructura del proyecto
app/src/main/java/com/renzo/moodlesync/

├── data/

│   ├── Task.kt           # Modelo de datos

│   ├── TaskDao.kt        # Acceso a base de datos

│   └── AppDatabase.kt    # Base de datos Room

├── ui/

│   ├── TaskScreen.kt     # Pantalla principal

│   ├── SplashScreen.kt   # Pantalla de carga

│   ├── NotificationHelper.kt  # Notificaciones

│   └── theme/

│       └── Theme.kt      # Tema de la app

├── worker/

│   └── SyncWorker.kt     # Sincronización en segundo plano

└── MainActivity.kt       # Actividad principal
---

## 👨‍💻 Autor

**Renzo Povis** — DAM1B  
Proyecto personal desarrollado como app de uso real para estudiantes de FP
