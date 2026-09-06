# Tsk.R Agent Instructions

## Project conventions

- Tsk.R is a single-module Android app (`:app`) written in Kotlin, targeting package `com.brixavier.tskr`.
- Use Jetpack Compose and Material 3 for UI. Reuse `ReminderAppTheme`, Material theme tokens, existing UI helpers, and the app's concise/sarcastic product tone.
- Keep the existing package layout: `ui`, `bloc`, `data`, `model`, `scheduler`, `receiver`, `widget`, and `engine`.
- Follow Kotlin conventions already present: `PascalCase` types/composables, `camelCase` members, immutable `data class` models, coroutines for asynchronous work, and `StateFlow` for screen state.

## Architecture and data flow

- Preserve the current BLoC/MVVM-style flow for user-driven reminder changes: Compose UI dispatches `ReminderEvent` -> `ReminderBloc` coordinates Room, alarms, notifications, streaks, and widget refresh -> Room `Flow` updates `ReminderState` -> Compose renders state.
- Keep Room as the source of truth for reminders. The `Reminder` entity stores date/time as `yyyy-MM-dd` and `HH:mm`; retain these formats unless the task explicitly includes a data migration.
- Keep `ReminderScheduler` as the single owner of AlarmManager request-code and three-stage scheduling behavior (7 days, 24 hours, due now).
- Platform entry points retain their established responsibilities: `AlarmReceiver` displays alerts, `BootReceiver` restores active reminder alarms, `AlarmActivity` handles mission-mode interactions, and the Glance widget reads upcoming reminders and opens `MainActivity`.
- When changing reminder lifecycle behavior, assess all affected paths: BLoC actions, scheduled alarms, active notifications, reboot restoration, mission mode, and widget content.

## Change discipline

- Make the smallest focused change required by the task. Do not refactor toward repositories, dependency injection, navigation libraries, or a new architecture unless explicitly requested.
- Do not modify unrelated files, app-wide styling, manifest permissions/components, build configuration, Gradle/plugin versions, or dependencies for a scoped feature or fix.
- Do not add libraries when AndroidX/Kotlin APIs already in the project are sufficient.
- Preserve existing public intent extras and alarm stages: `EXTRA_OPEN_ADD_SHEET`, `EXTRA_OPEN_REMINDER_ID`, and the three `ReminderScheduler` stage constants.
- Use `Dispatchers.IO` for Room/alarm/widget work and lifecycle-aware collection (`collectAsStateWithLifecycle`) for UI state.

## Database and safety

- The current Room database is `reminder_database`, at schema version 6. Before making any Room schema change, inspect the current version, existing migrations, and the real upgrade path for installed users.
- Never assume a missing migration is a bug without verifying compatibility. Do not change the database schema or migrations as part of creating or updating this `AGENTS.md`.
- Do not change the Room schema version, entity fields, table name, DAO queries, or database name without an explicit migration and upgrade plan.
- Never use destructive Room options such as `fallbackToDestructiveMigration`, and do not clear/reset user data to solve schema or test issues.
- Preserve reminder IDs and scheduler request-code semantics so existing alarms and notifications can be cancelled reliably.
- Do not run destructive Git commands, delete project files, or overwrite user changes.

## Verification

- For Kotlin/Compose or resource changes, run the narrowest relevant Gradle check; use `./gradlew :app:assembleDebug` when a full compile is appropriate.
- For reminder behavior changes, verify add, edit, complete/deactivate, delete, boot restoration, all three alarm stages, and widget refresh as applicable.
- Report tests run and any validation that could not be performed.
