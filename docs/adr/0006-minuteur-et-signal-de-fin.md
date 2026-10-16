# ADR 0006 — Minuteur par horodatage et signal de fin

- Statut : acceptée (2026-10-16)
- Décisions de pilotage : D6 (minuteur en arrière-plan), D7 (son de fin), D17 (durée réglable, sans minuteur)
- Sources : `docs/research/01-stack-technique.md` §4, `docs/research/03-existant-ux.md` §2.2

## Contexte

Les exercices durent de 30 s à 10 min. Le minuteur doit survivre à la rotation, à la mise en arrière-plan et à la mort du processus, sans service ni notification en v1. À la fin, il émet un signal discret (vibration réglable + bip SoundPool en mode sonnerie normal), puis passe en « temps additionnel » sans jamais bloquer la saisie. WCAG 2.2.1 impose une durée réglable et une option « sans minuteur ».

## Options

- Décompte : compteur décrémenté chaque seconde (dérive, perdu en arrière-plan) ou **horodatage de fin** (calculé à la demande).
- Arrière-plan : **horodatage seul** ; service de premier plan (type `specialUse` relu par Google Play, `shortService` limité à environ 3 min) ; alarme avec notification.
- Son : **SoundPool** (bip court dans `res/raw`) ; ToneGenerator (son « téléphone », peut échouer à l'initialisation) ; vibration seule.

## Décision

1. **Moteur pur** `core/timer/TimerEngine` (sans Android). Il calcule l'état à partir d'un instant fourni :
   - `TimerState` = `Idle(duration)` | `Running(endAtElapsed, duration)` | `Overtime(endAtElapsed, duration)` | `Untimed(startedAtElapsed)`.
   - Temps restant = `endAtElapsed − now`, recalculé à chaque tic (environ 250 ms, affichage à la seconde). On ne décrémente jamais de compteur.
   - L'horloge est `MonotonicClock` (`SystemClock.elapsedRealtime()`, monotone, compte aussi la veille).
2. **Persistance de l'état** dans le `SavedStateHandle` du ViewModel : `endAtWallMillis`, `durationMs`, `signalDelivered`, `sessionId`. Après une mort du processus ou un redémarrage, on reconstruit `endAtElapsed = elapsedNow + (endAtWall − wallNow)`.
3. **Pas de service ni de notification en v1.** Les tics ne sont produits que pendant que l'écran est collecté (`collectAsStateWithLifecycle`). En arrière-plan, rien ne tourne.
4. **Signal de fin unique** : quand un tic constate `now ≥ endAt` pour la première fois, le moteur renvoie l'événement `Expired(endAtElapsed)`. Le ViewModel le publie dans un `Channel`, et l'écran le consomme pendant l'état `STARTED`. **L'événement est ignoré s'il est périmé** (`now − endAt > 2 s`) : un retour d'arrière-plan après l'échéance affiche « temps écoulé » sans jouer de son. Le drapeau `signalDelivered` empêche de rejouer le signal après une rotation.
5. **`EndSignal`** (interface du domaine, implémentation `platform/AndroidEndSignal`) :
   - vibration : `VibrationEffect.createWaveform` (deux impulsions courtes), seulement si le réglage est actif et `hasVibrator()` ; permission `VIBRATE` (normale) ;
   - son : `SoundPool` chargé à l'entrée de l'écran d'exercice et libéré à la sortie, `AudioAttributes.USAGE_ASSISTANCE_SONIFICATION`, joué **seulement si `ringerMode == RINGER_MODE_NORMAL`** et si le réglage est actif ; **pas de demande de focus audio** (pour ne pas couper une vidéo pendant Decision Point) ;
   - le bip (< 1 s) est original ou sous licence CC0, dans `res/raw/end_beep.ogg`, et sa licence est notée dans `docs/licenses.md`.
6. **Temps additionnel** : après l'échéance, l'état `Overtime` affiche un compteur positif discret. La saisie reste ouverte, sans dialogue ni blocage.
7. **Écran allumé** : `view.keepScreenOn = true` dans un `DisposableEffect` tant que l'état est `Running` ou `Overtime`, puis on remet `false`.
8. **Durée réglable / sans minuteur** (réglages, ADR 0013) : bornes `[max(15 s, défaut/2) ; 10 × défaut]`, pas de 15 s sous 2 min et de 1 min au-delà. « Sans minuteur » donne l'état `Untimed` : chronomètre croissant discret, sans signal.
9. **Événements programmés** (rappel toutes les 60 s de Crazy Eights, bascule de Status Numbers entre 40 et 60 %) : ce sont des instants relatifs au début, évalués par le moteur au même tic et à la même règle de péremption. Un rappel manqué en arrière-plan n'est jamais rattrapé, mais la bascule de Status est affichée au retour, sans vibration.

## Conséquences

- Tout le minuteur se teste en JVM avec `FakeMonotonicClock` et `runTest` : expiration, rotation (même instance), mort du processus (restauration depuis le `SavedStateHandle`), péremption du signal, temps additionnel.
- Si l'app est en arrière-plan à l'échéance, l'utilisateur n'est pas prévenu en v1. C'est assumé (D6). La v1.1 pourra ajouter une notification de fin optionnelle (`POST_NOTIFICATIONS` + alarme, avec son propre ADR).
- Le comportement en silencieux ou en Ne pas déranger se vérifie sur un appareil réel (test manuel au lot 12).
- Passer à targetSdk 37 ne change rien tant qu'aucun son n'est joué en arrière-plan.
