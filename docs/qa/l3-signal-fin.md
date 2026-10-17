# L3 — Test manuel du signal de fin (appareil réel)

Cible : `platform/AndroidEndSignal` (ADR 0006, architecture §7). Appareil : Samsung R5CTA23GG8K (branché), à faire avec l'utilisateur.

## Préparation

- [ ] `./gradlew installDebug` ; noter la version Android et le modèle : __________
- [ ] Volume « sonneries » / « notifications » à mi-course au moins.
- [ ] Pas d'écran de test de l'exercice avant L5 : utiliser l'écran de démonstration du lot L5, ou un point d'entrée de debug appelant `AndroidEndSignal.prepare()` puis `play(Settings())`.

## Diagnostic « ni bip ni vibration » (test utilisateur, 17 oct.)

- La chaîne expiration, `TimerAnnouncer`, `EndSignal.play()` est correcte (test JVM `the end signal is played once when the timer runs out`, réglages son et vibration actifs par défaut). Journaux sur le Z Flip3 : `play()` appelé une fois, SoundPool chargé, flux de lecture valide, vibration enregistrée par `vibrator_manager` (usage ALARM, 370 ms).
- **Cause du bip muet** : `USAGE_ASSISTANCE_SONIFICATION` est routé par Samsung vers `STREAM_SYSTEM`, dont le volume était à 0 (haut-parleur). Correctif : `USAGE_MEDIA` (volume média, sans focus audio).
- **Vibration** : l'effet atteignait bien le moteur, mais deux impulsions de 120 ms à amplitude par défaut sont presque imperceptibles sur ce moteur. Correctif : 2 x 250 ms à pleine amplitude (si contrôle d'amplitude).
- Comportement attendu : mode sonnerie, bip au volume média et vibration ; vibreur, vibration seule ; silencieux, vibration seule (usage ALARM). Un volume média à 0 rend le bip muet (réglage utilisateur).

## Matrice (cocher le résultat observé)

| # | Mode sonnerie | Son (réglage) | Vibration (réglage) | Attendu | Observé |
|---|---|---|---|---|---|
| 1 | Sonnerie | oui | oui | bip + deux impulsions | |
| 2 | Sonnerie | oui | non | bip seul | |
| 3 | Sonnerie | non | oui | deux impulsions seules | |
| 4 | Sonnerie | non | non | rien | |
| 5 | Vibreur | oui | oui | **pas de bip**, deux impulsions (usage ALARM) | |
| 6 | Vibreur | oui | non | rien | |
| 7 | Silencieux | oui | oui | **pas de bip** ; vibration attendue (usage ALARM, non filtré par le retour haptique) : noter si le fabricant la bloque | |
| 8 | Ne pas déranger (alarmes seules) | oui | oui | pas de bip ; noter la vibration | |

## Points transverses

- [ ] Vibration en usage ALARM (`VibrationAttributes` dès Android 13, `AudioAttributes` avant) : elle doit jouer même si le retour haptique système est désactivé.

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

## Force des vibrations et rappel de rythme (L12a)

- Réglage Douce / Normale / Forte (défaut Normale) : Forte = 2 x 250 ms à pleine amplitude (signal d'origine). Sans contrôle d'amplitude, seule la durée change. Bouton « Essayer » dans les Réglages (anti-rebond 500 ms).
- Le rappel de rythme de Crazy Eights (une impulsion par minute, `AndroidEndSignal.tick`) est lui aussi en **usage ALARM**, comme le signal de fin : l'utilisateur l'active volontairement pendant l'exercice et le retour haptique système ne doit pas le couper. Il suit la même force.
- [ ] Les trois forces se distinguent au toucher sur le Z Flip3 (la Douce reste perceptible).
- [ ] Rappel de rythme actif, retour haptique système coupé : l'impulsion se sent quand même.
