# Diseño del módulo Rutinas

## Jerarquía de componentes de UI

- **RoutineActiveScreen**: pantalla principal al abrir una rutina.
  - **Cabecera**: nombre global de la rutina + botón atrás.
  - **Lista de días** (selector): chips horizontales; cada día tiene `dayLabel` y `activityName` ("Actividad de hoy").
  - **Tarjeta día seleccionado**: día + "Actividad de hoy: {nombre}".
  - **Tabs**: Calendario | Notificaciones | Seguimiento.
  - **Vista de detalle de ejercicios**:
    - Si el día tiene 0 ejercicios: placeholder "No hay rutina definida para hoy."
    - Si tiene 1–10 ejercicios: lista de ejercicios + texto "X ejercicios" y lógica de límite (deshabilitar añadir cuando hay 10).

Flujo: **Lista de días** → selección de día → **Vista de detalle de ejercicios** (lista o placeholder).

## Modelo de datos (dominio y Room)

- **RoutineHeader**: id, name, ownerId, creatorId (cabecera global).
- **RoutineDayView**: routineId (id del día en BD), dayLabel, activityName, exercises, exerciseCount.
- **RoutineWithDays**: header + days (lista de RoutineDayView).
- **Room**: `RoutineEntity` (una fila por día: name, dayInfo = "DayLabel - ActivityName"), `RoutineExerciseEntity` (routineId, exerciseId, order). Una rutina lógica = varias filas `RoutineEntity` con el mismo `name`.

Permisos: **Admin/Entrenador** → rutinas globales (`ownerId == null`). **Usuario** → rutinas privadas (`ownerId == userId`).

## Interfaz del repositorio (RoutineRepositoryContract)

Preparada para conectar con Spring Boot:

- **Lectura**: `getMyRoutines`, `getGlobalRoutines`, `observeRoutine`, `getRoutineWithDays`, `getRoutineById`, `getExercisesForDay`.
- **CRUD rutinas**: `insertRoutine`, `updateRoutine`, `deleteRoutine`.
- **CRUD por día**: `addExerciseToDay`, `removeExerciseFromDay`, `updateDayActivity`.
- **Límite 10**: `canAddExerciseToDay(routineId)` (usa `countExercisesInRoutine` < 10).

## Límite de 10 ejercicios por día

- Constante: `MAX_EXERCISES_PER_DAY = 10` en `RoutineModels.kt`.
- En UI: mensaje "Límite alcanzado (10 ejercicios por día)" y deshabilitar añadir cuando `exerciseCount >= 10`.
- En repositorio: `addExerciseToDay` comprueba `canAddExerciseToDay` antes de insertar; `savePersonalRoutine` usa `.take(MAX_EXERCISES_PER_DAY)` por día.
