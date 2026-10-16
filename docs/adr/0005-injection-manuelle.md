# ADR 0005 — Injection de dépendances manuelle (`AppContainer`)

- Statut : acceptée (2026-10-16)
- Décision de pilotage : D9
- Source : `docs/research/01-stack-technique.md` §3

## Contexte

L'app a une dizaine de dépendances (base Room, repositories, réglages, tables, horloges, signal de fin, registre d'exercices) et un seul module. Hilt ajouterait un second processeur d'annotations à côté de KSP/Room 3, et aucune source ne le valide avec Navigation 3 ni avec Room 3.

## Options

1. Hilt : recommandé par Google pour les grosses apps, mais lourd ici.
2. Koin : non étudié sur une source primaire.
3. **Manuelle** : un conteneur créé dans `Application`, avec des fabriques de ViewModel.

## Décision

- `DmGymApplication` crée **un** `AppContainer` (paresseux, champs `by lazy`).
- Les dépendances passent **par le constructeur**. Le service locator est interdit : aucune classe métier n'appelle le conteneur.
- Les ViewModels sont créés avec `viewModelFactory { initializer { … } }`, exposé par le conteneur, et `createSavedStateHandle()`.
- Les horloges et l'aléatoire sont injectés pour garder les tests déterministes :

```kotlin
interface AppContainer {
    val wallClock: java.time.Clock               // Clock.systemDefaultZone()
    val monotonicClock: MonotonicClock           // SystemClock.elapsedRealtime()
    val randomFactory: RandomFactory             // (seed?) -> kotlin.random.Random
    val tableRepository: TableRepository
    val historyRepository: HistoryRepository
    val settingsRepository: SettingsRepository
    val exerciseRegistry: ExerciseRegistry
    val endSignal: EndSignal                     // son + vibration
    val keywordDetector: KeywordDetector
    val ioDispatcher: CoroutineDispatcher
}
```

## Conséquences

- Les tests construisent les classes avec des doublures (`FakeHistoryRepository`, `Clock.fixed`, `FakeMonotonicClock`, `Random(42)`), sans framework de mock obligatoire.
- Chaque nouvelle dépendance demande une ligne dans le conteneur, ce qui reste acceptable à cette taille.
- On réévaluera Hilt au-delà d'environ 15 dépendances ou en cas de passage au multi-module (nouvel ADR).
