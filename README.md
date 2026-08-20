# Training Now!

App Android (Kotlin, Jetpack Compose, MVVM) de entrenamiento físico. Conecta tres tipos de usuario en un mismo ecosistema:

- **Usuarios** siguen rutinas de ejercicio, consultan la biblioteca de ejercicios, registran series/repeticiones, ven su calendario de progreso y avance mensual, y chatean con su entrenador o soporte.
- **Entrenadores** gestionan clientes, crean y comparten rutinas, editan su perfil público y chatean con sus usuarios.
- **Administradores** gestionan usuarios (crear, banear, suspender), la biblioteca global de ejercicios, rutinas globales, auditoría de acciones y comunicación interna.

Se conecta a los 4 microservicios Spring Boot del repo hermano `TrainingNow-Microservicios` (usuarios, biblioteca, rutinas/workouts, comunicaciones) vía Retrofit, con autenticación JWT.

## Puesta en marcha

1. Levantar los 4 microservicios (ver README de `TrainingNow-Microservicios`).
2. Abrir este proyecto en Android Studio, sincronizar Gradle.
3. `RemoteModule.kt` detecta el host automáticamente: `10.0.2.2` si corre en emulador, `127.0.0.1`/IP local si es dispositivo físico en la misma red que el backend.
4. Ejecutar `app` en un emulador o dispositivo.

## Estructura del proyecto

Proyecto Gradle de módulo único (`:app`), paquete base `com.shagox.apptrainingnow`.

```
app/src/main/java/com/shagox/apptrainingnow/
├── ui/screen/            pantallas Compose comunes + subcarpetas admin/ y coach/
├── ui/components/        componentes reutilizables (headers, diálogos, reproductor de video, etc.)
├── ui/theme/             design tokens, tema claro/oscuro
├── ui/viewmodel/         AuthViewModel, CoachViewModel (+ factories)
├── navigation/           Routes.kt (rutas), AppNavGraph.kt (NavHost único)
├── data/remote/          Retrofit: RemoteModule.kt + *Api.kt + dto/
├── data/repository/      repositorios (API y/o Room según el módulo)
├── data/local/           Room: entidades y DAOs por dominio (user, routine, workout, chat, trainer, exercise, notification, progress, database/)
├── data/domain/          modelos de dominio (RoutineModels.kt)
├── domain/validation/    Validators.kt — únicos validadores de formularios del proyecto
├── utils/                compresión de imágenes, recordatorios, unidades de medida, etc.
├── MainActivity.kt       decide pantalla inicial según rol de sesión
└── TrainingNowApplication.kt   restaura token/sesión al abrir la app
```

## Pantallas por rol

Todas las rutas viven en un único `NavHost` (`navigation/AppNavGraph.kt`); el filtrado por rol ocurre en tiempo de composición leyendo `loggedUser?.role` (`"ADMIN"`, `"TRAINER"`/`"COACH"`, o usuario normal), tanto para elegir la barra de navegación inferior (`Routes.kt → getBottomNavRoutes(role)`) como el destino inicial (`MainActivity.kt`).

### Comunes a los 3 roles (`ui/screen/`)
- `WelcomeScreen.kt` — carrusel de bienvenida (primer uso).
- `ProfileScreen.kt` — perfil, editar datos, login/registro, borrar cuenta.
- `SettingsScreen.kt` — tema claro/oscuro, cambiar contraseña, cerrar sesión.
- `ChatScreen.kt` — conversación 1 a 1.
- `NotificationsScreen.kt` — notificaciones/alertas.
- `LibraryScreen.kt`, `LibraryCategoryScreen.kt`, `ExerciseDetailScreen.kt` — biblioteca de ejercicios (consulta).
- `MonthlyReportScreen.kt` — reporte de avance mensual.

### Usuario (`ui/screen/`)
- `UserRoutinesScreen.kt` — mis rutinas: carrusel de recomendadas + creadas por el usuario.
- `RoutineActiveScreen.kt` — rutina activa por día, registro de series/repeticiones.
- `CreateRoutineScreen.kt` — crear/editar rutina propia (también reutilizada por entrenador/admin).
- `UserChatsScreen.kt` — chats con entrenadores + soporte TrainingNow.

### Entrenador (`ui/screen/coach/`)
- `CoachClientsScreen.kt` — clientes activos.
- `ClientDetailScreen.kt` — detalle/progreso de un cliente.
- `CoachRoutinesScreen.kt` — rutinas creadas por el entrenador.
- `CoachUsersScreen.kt` — listado de todos los usuarios normales (solo lectura).
- `CoachPublicProfileScreen.kt` — editor de perfil público.
- `CoachChatsScreen.kt` — mensajes del entrenador.

### Administrador (`ui/screen/admin/`)
- `AdminPanelScreen.kt` — panel principal.
- `AdminChatsScreen.kt` — bandeja de soporte (pestañas Admins/Entrenadores/Chats abiertos).
- `AdminUserManagementScreen.kt`, `AdminUserListScreen.kt` — gestión y listado de usuarios con búsqueda.
- `AdminCreateUserScreen.kt` — creación de staff (dominio `@trainingnow.com` automático).
- `AdminSanctionScreen.kt` — suspender/banear/eliminar cuenta.
- `AdminLibraryScreen.kt`, `AdminLibraryCategoryScreen.kt`, `AdminCreateCategoryScreen.kt` — gestión de biblioteca global (con compresión de imágenes).
- `AdminGlobalRoutinesScreen.kt` — editor de rutinas globales publicadas.
- `AdminSendMessagesScreen.kt` — mensajería interna segmentada a usuarios.
- `AdminActivityLogScreen.kt` — registro de auditoría (quién hizo qué, con filtros).

> Nota: `AdminReportsScreen.kt` y `AdminSendNotificationScreen.kt` siguen presentes en el código pero ya no tienen acceso desde el panel admin (funcionalidad de "Reportes" y notificaciones push retirada de la UI en una iteración posterior).

## Seguridad y sesión

- **Token JWT**: `data/remote/RemoteModule.kt` mantiene `authToken` en memoria y lo agrega automáticamente como header `Authorization: Bearer $token` en cada request vía interceptor OkHttp — ninguna pantalla arma el header a mano.
- **Persistencia de sesión**: `data/local/user/SessionManager.kt` guarda el token JWT y el usuario (JSON) en `SharedPreferences`. Al abrir la app, `TrainingNowApplication.onCreate()` restaura `RemoteModule.authToken` desde ahí antes de cualquier llamada a la API, así la sesión sobrevive cerrar/reabrir la app.
- **Enforcement por rol**: el rol viaja en el JWT y el backend lo exige a nivel Spring Security en endpoints admin-only (ver README de microservicios); en la app, el rol (`loggedUser.role`) decide qué tabs/pantallas se muestran (`Routes.kt`, `AppNavGraph.kt`) — es control de UI, la autorización real ocurre en el backend.
- **Bloqueo por sanción**: `AppNavGraph.kt` verifica `isBanned`/`suspendedUntil` del usuario logueado y muestra un diálogo modal bloqueante si corresponde.

## Validaciones

Todo centralizado en `domain/validation/Validators.kt`:

- `validateNameLettersOnly` — no vacío, solo letras/tildes/ñ/espacios.
- `validateEmail` — no vacío, formato válido (`Patterns.EMAIL_ADDRESS`).
- `validatePhoneDigitsOnly` — solo dígitos (8–15), debe iniciar con un código de país americano reconocido (19 países soportados), mínimo 7 dígitos tras el código.
- `validateStringPassword` — mínimo 8 caracteres, al menos 1 mayúscula, 1 minúscula, 1 número.
- `validateConfirm` — coincidencia exacta con la contraseña.

## Base de datos local (Room)

`data/local/database/AppDatabase.kt`. Room funciona como caché/local-first: **Usuarios, Notificaciones y Ejercicios** usan repositorios de API directamente (`USE_API = true` en `TrainingNowApplication`), mientras que **Rutinas, Chat, Trainer-Client y Progreso/Workouts** usan Room con sincronización puntual desde el backend (`syncRoutinesFromBackend`, `syncConversation`, etc.). También guarda preferencias de contacto del chat (bloqueo/silencio) y la sesión de invitado (`GuestSession`).

## Tests

`app/src/test/java/com/shagox/apptrainingnow/` (JUnit4 + MockK + Robolectric + kotlinx-coroutines-test):
- `ui/viewmodel/AuthViewModelTest.kt` — login/registro/logout con `IUserRepository` mockeado.
- `ui/viewmodel/CoachViewModelTest.kt` — lógica de clientes/progreso del entrenador.
- `domain/validation/ValidatorsTest.kt` — cobertura de los 5 validadores (nombre, email, teléfono, password, confirmación).
- `data/repository/ExerciseApiRepositoryTest.kt` — repositorio de ejercicios contra la API mockeada.
- `ExampleUnitTest.kt` — plantilla por defecto de Android Studio, sin lógica.

`app/src/androidTest/java/com/shagox/apptrainingnow/`:
- `ExampleInstrumentedTest.kt` — plantilla por defecto; no hay tests de UI Compose reales aún (las dependencias de `ComposeTestRule` están declaradas pero sin usar).

Ejecutar: panel *Run* de Android Studio sobre la carpeta `test` (unitarios, corren en JVM local) o clic derecho → *Run Tests* sobre cada archivo; `androidTest` requiere emulador/dispositivo conectado.

## Stack técnico

Jetpack Compose (BOM gestionado), Navigation Compose 2.9.7, Retrofit 3.0.0 + OkHttp 5.3.2, Room 2.8.4 (KSP), Coil 2.7.0, DataStore Preferences, Lifecycle ViewModel/Compose 2.10.0, Coroutines 1.10.2. `compileSdk`/`targetSdk` 36, `minSdk` 24.
