# ADR 0014 — Rappels d'entraînement par notification locale

- Statut : proposée (2026-10-18), à valider avec les décisions D54+ de la v1.1
- Amende : ADR 0010 (liste blanche de permissions), ADR 0013 (nouvelles clés de réglage)
- Décisions de pilotage : D19 (rappels optionnels, désactivés par défaut, v1.1)
- Entrées : `docs/ux/parcours-v1.1.md` §2 (réglage, permission, textes, pause après 14 jours), `docs/research/02-pedagogie-exercices.md` §4

## Contexte

La v1.1 ajoute un rappel facultatif, **désactivé par défaut** : une notification locale, une fois par jour au plus, aux jours et à l'heure choisis (défaut UX : lundi, mercredi, vendredi, « vers 18 h 30 »). Contraintes :

- 100 % hors ligne : aucune permission réseau, aucun service tiers (ADR 0010).
- L'heure est **approximative** (« vers 18 h 30 ») : un retard de quelques minutes est sans conséquence. On n'a donc pas besoin d'alarme exacte (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`).
- Android 13+ (API 33) : `POST_NOTIFICATIONS` est une permission d'exécution. Elle est demandée **seulement** quand l'utilisateur active le rappel, depuis Réglages, jamais pendant un exercice ni au premier lancement.
- Le rappel doit survivre au redémarrage du téléphone, à la mise à jour de l'app et au changement de fuseau horaire.
- Pas de culpabilisation (UX §2.3) : pas de notification si une session a déjà été faite aujourd'hui, mise en pause automatique après 14 jours sans session, et la série n'est jamais citée.
- Mode Doze et mise en veille des applis : l'alarme peut être retardée, ce qui est acceptable.

## Options

### Planification

1. **WorkManager** (`PeriodicWorkRequest` de 24 h ou `OneTimeWorkRequest` replanifié).
   - Pour : il est replanifié automatiquement après un redémarrage, et son API est connue.
   - Contre : **son manifeste ajoute `ACCESS_NETWORK_STATE`**, ainsi que `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` et `FOREGROUND_SERVICE`. `ACCESS_NETWORK_STATE` contredit l'esprit de l'ADR 0010 : la retirer par `tools:node="remove"` expose à des plantages du suivi de connectivité. Il ajoute aussi une base Room interne, une initialisation App Startup et une nouvelle dépendance. Il ne garantit pas une heure de la journée : une tâche périodique dérive, et une tâche unique demande de toute façon un calcul de délai.
2. **AlarmManager inexact** (`setAndAllowWhileIdle(RTC_WAKEUP, …)`), réarmé à chaque déclenchement, avec un récepteur de redémarrage.
   - Pour : c'est du code du framework, sans dépendance. L'heure murale est respectée, avec un retard toléré en Doze (au plus une alarme par fenêtre de quelques minutes pour l'app, sans conséquence ici). Aucune permission d'alarme exacte.
   - Contre : les alarmes sont effacées au redémarrage et après un arrêt forcé. Il faut un récepteur `BOOT_COMPLETED` (permission normale `RECEIVE_BOOT_COMPLETED`) et un réarmement à chaque démarrage de l'app.
3. `setInexactRepeating(INTERVAL_DAY)` : écartée. Il dérive aux changements d'heure, et les jours de la semaine choisis ne s'y expriment pas.
4. Service de premier plan : écartée. C'est disproportionné et cela demande une permission supplémentaire.

### Redémarrage

- (a) **Récepteur `BOOT_COMPLETED`**, désactivé dans le manifeste et activé seulement quand le rappel est actif (`PackageManager.setComponentEnabledSetting`).
- (b) Réarmer seulement au prochain lancement de l'app : un utilisateur qui n'ouvre plus l'app ne serait plus rappelé, ce qui retire tout l'intérêt du rappel. Écartée.

## Décision

### 1. AlarmManager inexact, un déclenchement à la fois

- `core/reminder` (Kotlin pur, testé en JVM) :
  - `ReminderSchedule(enabled, time: LocalTime, days: Set<DayOfWeek>)` est le modèle des réglages.
  - `nextTrigger(now: ZonedDateTime, schedule): Instant?` calcule la prochaine occurrence **strictement future** d'un jour choisi à l'heure choisie dans le fuseau courant. Un trou de changement d'heure (02:30 inexistant) est décalé vers l'avant par `ZonedDateTime.of`. Une heure ambiguë prend le premier instant. Si aucun jour n'est choisi, la fonction renvoie `null`, un cas interdit par l'UX mais traité.
  - `ReminderPolicy.decide(today, practicedDays, sessionCount, schedule, resumedOnEpochDay): ReminderDecision` renvoie `Skip(AlreadyPracticed | Paused14Days)` ou `Notify(textIndex)`. Le texte est choisi par la date (graine = `localEpochDay`, déterministe comme la séance du jour). Le texte « premier exercice » est choisi si `sessionCount == 0`.
  - Pause après 14 jours (UX V11-8) : `Paused14Days` vaut si `today − max(dernier jour pratiqué, resumedOnEpochDay) ≥ 14`. « Reprendre » écrit `resumedOnEpochDay = today`. Rien n'est stocké au moment de la pause : elle est **calculée**.
- `platform/reminder` :
  - `AlarmReminderScheduler` implémente le port `ReminderScheduler { suspend fun sync(schedule) }`. Il annule la `PendingIntent` existante, puis appelle `setAndAllowWhileIdle(RTC_WAKEUP, nextTrigger, pi)` si le rappel est actif. La `PendingIntent` est `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`, avec un code de requête fixe, et vise un récepteur explicite non exporté.
  - `ReminderAlarmReceiver` (`exported="false"`) : il appelle `goAsync()`, lit les jours pratiqués et le compte de sessions par `HistoryRepository` (une lecture, moins de 10 s), applique `ReminderPolicy`, publie ou non, puis **réarme toujours** l'occurrence suivante.
  - `ReminderRescheduleReceiver` (désactivé par défaut, activé seulement si le rappel est actif) écoute `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED` et `TIME_SET`. Ces diffusions font partie des exceptions aux diffusions implicites et sont donc délivrées aux récepteurs du manifeste. Il appelle `sync`. La valeur d'`exported` sera vérifiée au lot sur l'appareil : `false` suffit si le système délivre ces diffusions aux composants non exportés ; sinon `true`, avec un filtrage strict de l'action reçue (aucune donnée n'est lue dans l'intent).
- **Réarmement à chaque démarrage** : `DmGymApplication.onCreate` lance `sync(schedule)` dans le scope de l'app. Cela couvre l'arrêt forcé, qui efface les alarmes et bloque les diffusions jusqu'à la prochaine ouverture, et la restauration par transfert d'appareil (D26). `sync` est aussi appelée à chaque changement des réglages du rappel. Elle est idempotente.

### 2. Notification et canal

- Un seul canal `reminders` (« Rappels d'entraînement »), créé à chaque `Application.onCreate` par `NotificationManagerCompat.createNotificationChannel`. L'appel est idempotent et met à jour le nom traduit, puisque le nom suit la langue (ADR 0016).
- **L'importance d'un canal ne peut plus être changée par l'app une fois créé** : seul l'utilisateur peut la modifier. La valeur doit donc être tranchée **avant** la publication de la v1.1 (UX V11-7 : basse). Pour en changer plus tard, il faudrait un nouvel identifiant de canal.
- La notification n'a qu'un titre (« DM Gym ») et un texte tiré de `strings.xml` (jeu de textes, UX §2.3). Elle a `setAutoCancel(true)`, aucun bouton d'action et une petite icône monochrome dédiée `ic_stat_reminder` (le d20, en vecteur). Sa visibilité est publique, puisque le texte est anodin.
- Un appui ouvre `MainActivity` avec un extra `destination=daily`, par une `PendingIntent` immuable. `MainActivity` le traduit en route `Daily`, au démarrage comme dans `onNewIntent`. Le retour mène à l'accueil.
- « Envoyer un test » (UX §2.1) publie la notification tout de suite, par le même code, sans `ReminderPolicy`.

### 3. Permission `POST_NOTIFICATIONS`

- Elle est déclarée dans le manifeste. En dessous d'Android 13, elle est sans effet.
- L'écran Réglages passe par un `NotificationPermissionGate` (interface de `feature/settings`, implémentée côté plateforme), qui donne `areNotificationsEnabled()`, `needsRuntimePermission` (API ≥ 33) et `shouldShowRationale`. La demande passe par `rememberLauncherForActivityResult(RequestPermission())`, au premier appui sur l'interrupteur.
- L'état affiché se calcule ainsi : `enabled` (l'intention, stockée) et `deliverable = areNotificationsEnabled() && canal non bloqué` (relu à chaque `ON_RESUME`). Si `enabled` est vrai mais `deliverable` faux, l'écran affiche « Rappel en pause » (UX §2.2). Il n'y a **jamais** de dialogue ni de demande hors de l'écran Réglages.
- Aucun appel `requestPermissions` ni dialogue dans un écran d'exercice. La règle « sans dialogue pendant l'exercice » reste respectée.

### 4. Réglages (complément à l'ADR 0013)

| Clé DataStore | Type | Défaut |
|---|---|---|
| `reminder_enabled` | Boolean | `false` |
| `reminder_minute_of_day` | Int (0..1439) | 1110 (18 h 30, UX V11-6) |
| `reminder_days` | String (`"1,3,5"`, ISO 1 = lundi) | `"1,3,5"` |
| `reminder_resumed_epoch_day` | Long | absent |

Les valeurs sont bornées à la lecture : une valeur corrompue est remplacée par le défaut. Un ensemble de jours vide est relu comme le défaut.

### 5. Liste blanche (amende l'ADR 0010)

La liste blanche de `verifyPermissions` devient :

- `android.permission.VIBRATE` ;
- `android.permission.POST_NOTIFICATIONS` (nouvelle, permission d'exécution API 33+, sans rapport avec le réseau) ;
- `android.permission.RECEIVE_BOOT_COMPLETED` (nouvelle, permission normale accordée à l'installation) ;
- `${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.

Restent interdites, entre autres : `INTERNET`, `ACCESS_NETWORK_STATE`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Le test de la tâche est complété : un manifeste qui contient `SCHEDULE_EXACT_ALARM` est rejeté.

### 6. Hors périmètre

- La **notification de fin de minuteur** quand l'app est en arrière-plan (piste citée par l'ADR 0006) reste hors de la v1.1. D6 est maintenue : la fin n'est signalée qu'au premier plan.
- Aucun rappel de relance ni de report (« Plus tard »).
- Aucune demande d'exemption d'optimisation de batterie.

## Conséquences

- Aucune nouvelle dépendance : `AlarmManager` vient du framework, et `NotificationManagerCompat` d'`androidx.core`, déjà présent.
- Sur certains fabricants (Samsung : « applis en veille prolongée »), le système peut retarder ou supprimer l'alarme d'une app peu utilisée. On l'accepte (exactitude non requise) et on le documente dans `docs/qa/`. Le bouton « Envoyer un test » sert de diagnostic.
- Tests :
  - JVM : `nextTrigger` (jours choisis, passage de semaine, heure déjà passée aujourd'hui, trou et doublon de changement d'heure, changement de fuseau) et `ReminderPolicy` (déjà pratiqué, 0 session, pause à 14 jours, reprise, texte déterministe).
  - Robolectric : `ShadowAlarmManager` (alarme posée, remplacée, annulée à la désactivation), `ShadowNotificationManager` (canal, notification publiée ou absente), récepteurs (redémarrage → `sync`, alarme → publication puis réarmement), activation et désactivation du composant de redémarrage, réglages DataStore.
  - Compose : l'interrupteur et ses états (refusé, refus définitif, en pause), avec une doublure de `NotificationPermissionGate`.
  - `verifyPermissions` : nouvelle liste blanche, et refus de `SCHEDULE_EXACT_ALARM`.
  - Manuel (Z Flip3) : rappel réel, Doze forcé (`adb shell dumpsys deviceidle force-idle`), redémarrage, mise à jour par-dessus, changement de fuseau. Les résultats vont dans `docs/qa/`.

## Décisions de l'utilisateur (2026-10-18)

- D59 : lundi, mercredi, vendredi vers 18 h 30 par défaut. D60 : canal d'importance **basse** (silencieux). D61 : **AlarmManager inexact** + reprogrammation au redémarrage. D62 : pause automatique après 14 jours sans session. D63 : la notification ne cite jamais la série.
