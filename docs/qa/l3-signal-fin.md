# L3 — Test manuel du signal de fin (appareil réel)

Cible : `platform/AndroidEndSignal` (ADR 0006, architecture §7). Appareil : Samsung R5CTA23GG8K (branché), à faire avec l'utilisateur.

## Préparation

- [ ] `./gradlew installDebug` ; noter la version Android et le modèle : __________
- [ ] Volume « sonneries » / « notifications » à mi-course au moins.
- [ ] Pas d'écran de test de l'exercice avant L5 : utiliser l'écran de démonstration du lot L5, ou un point d'entrée de debug appelant `AndroidEndSignal.prepare()` puis `play(Settings())`.

## Matrice (cocher le résultat observé)

| # | Mode sonnerie | Son (réglage) | Vibration (réglage) | Attendu | Observé |
|---|---|---|---|---|---|
| 1 | Sonnerie | oui | oui | bip + deux impulsions | |
| 2 | Sonnerie | oui | non | bip seul | |
| 3 | Sonnerie | non | oui | deux impulsions seules | |
| 4 | Sonnerie | non | non | rien | |
| 5 | Vibreur | oui | oui | **pas de bip**, deux impulsions | |
| 6 | Vibreur | oui | non | rien | |
| 7 | Silencieux | oui | oui | **pas de bip** ; vibration selon fabricant (noter) | |
| 8 | Ne pas déranger (alarmes seules) | oui | oui | pas de bip ; noter la vibration | |

## Points transverses

- [ ] Pendant une vidéo ou de la musique (Decision Point) : le bip se superpose sans couper ni baisser le média (pas de focus audio).
- [ ] Bip court, doux, non agressif (< 1 s), sans clic au début ni à la fin.
- [ ] Deux impulsions courtes distinctes (120 ms, pause 100 ms).
- [ ] Appareil sans vibreur / tablette : `canVibrate == false`, aucune exception.
- [ ] Après `release()` puis `prepare()` (sortie et retour sur l'écran) : le bip rejoue.
- [ ] `play()` avant la fin du chargement du bip : pas d'exception (le son est ignoré).
- [ ] Écran allumé (`KeepScreenOn`) pendant Running et Overtime, éteint au retour à l'accueil.

## Comportement à noter, hors périmètre

- Retour d'arrière-plan après l'échéance : aucun signal (événement périmé > 2 s), géré par le ViewModel (L5).

## Résultat

Date : ________  Testeur : ________  Verdict : ________  Remarques :
