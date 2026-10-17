# DM Gym — Spécifications

Application Android pour s'entraîner aux compétences de Maître du Jeu (D&D / JdR) via 7 exercices courts inspirés d'autres disciplines créatives.

Source d'inspiration : vidéo d'Edariad « How to Get Better at DMing Crazy Fast » (https://www.youtube.com/watch?v=RhmlT8crkG4), résumée dans https://scipio202.bearblog.dev/growing-edges-and-gming-exercises/.

**Positionnement** : « la salle de sport du MJ » — des séances courtes, hors partie, avec une contrainte, un chrono et une trace. Ni un générateur de contenu pour la table, ni un outil de campagne.

> Révision du 2026-10-16 : décisions D1–D24 tranchées (voir `docs/pilotage.md` et les ADR de `docs/adr/`) et corrections issues de la recherche (`docs/research/`).

## Principes de conception

Chaque exercice s'appuie sur trois idées :
- **Les contraintes stimulent la créativité** → chaque exercice impose une règle stricte.
- **La pression du temps fait taire l'autocritique** → chaque exercice a un minuteur visible.
- **Pratique délibérée** → l'utilisateur garde une trace de ses productions et peut les relire.

L'app doit fonctionner **entièrement hors ligne**. Pas de compte, pas de backend.

## Stack technique (décidée — détails dans les ADR)

- Kotlin 2.4.21 + Jetpack Compose (BOM 2026.09.00, Material 3 1.4.0)
- AGP 9.4.0 (Kotlin intégré) + Gradle 9.7.x, exécutés sous **JDK 21** (repli AGP 9.3.1 si le build de fumée échoue)
- Architecture MVVM, une seule Activity, **Navigation 3** (1.2.0)
- **Room 3** (3.0.3, KSP) pour l'historique des sessions (repli Room 2.8.5)
- Injection de dépendances **manuelle** (`AppContainer`)
- Contenu des exercices (tables aléatoires) dans des fichiers JSON dans `assets/`, décodés en strict par kotlinx.serialization et validés par tests JVM
- minSdk 26, targetSdk 36, compileSdk 37
- Tests : JUnit Jupiter 6 + Vintage (Robolectric, Compose), JUnit 4 en instrumenté

## Fonctionnalités communes

- Écran d'accueil : liste des 7 exercices (nom, discipline d'origine, durée).
- Écran d'exercice : consigne, tirage aléatoire (si pertinent), minuteur, zone de saisie texte (ou plusieurs champs).
- Bouton « Relancer le tirage ».
- Minuteur par horodatage (survit à la rotation et à l'arrière-plan, sans service ni notification en v1) ; écran maintenu allumé pendant l'exercice.
- **Durée réglable** pour chaque exercice et **option « sans minuteur »** (WCAG 2.2.1).
- À la fin du minuteur : vibration (réglable) + bip discret via SoundPool, joué seulement en mode sonnerie normal ; la saisie reste possible (passage en « temps additionnel », jamais de blocage ni de dialogue).
- Sauvegarde automatique de chaque session (exercice, date, tirage, réponses).
- **Auto-évaluation optionnelle** en fin d'exercice : 3 niveaux, plus possibilité de marquer un « bijou » (production à garder).
- Écran Historique : liste filtrable par exercice, détail d'une session.
- Statistiques simples : nombre de sessions (affiché en premier), puis série de jours consécutifs **avec jour de grâce, sans remise à zéro punitive** ; le jour bascule à minuit local.
- **Thème** (D53) : réglage « Parchemin » (défaut, identité de l'app) / « Couleurs du téléphone » (Material You, Android 12+) / 3-4 palettes Material conformes.
- **Saisie vocale** (D52) : la v1 s'appuie sur la dictée du clavier Android ; aucun bouton micro intégré, aucune permission audio.
- **Séance du jour** suggérée (3 exercices, graine = date, chaque exercice remplaçable ; One-Page exclu par défaut).

## Les 7 exercices

### 1. Crazy Eights (design thinking)
- **But** : dépasser ses premières idées (les plus accessibles en mémoire) pour trouver les plus intéressantes.
- **Déroulé** : un problème est tiré au hasard ; l'utilisateur a **8 minutes pour écrire 8 solutions** (8 champs numérotés).
- **Exemples de problèmes** : trouver une accroche d'aventure, réagir à un choix inattendu des joueurs, faire découvrir une information importante aux PJ.
- Indicateur de progression : x/8 champs remplis.

### 2. Constraint Box (jazz)
- **But** : sortir des automatismes.
- **Déroulé** : l'utilisateur choisit un domaine (ex. description de lieu), l'app tire un lieu + **3 mots sensoriels imposés**. Il doit écrire la description en utilisant uniquement ces trois mots comme ancrages sensoriels.
- Durée : 5 min.
- Les mots imposés sont surlignés quand ils apparaissent dans le texte (détection approximative, voir « Détection des mots »), et cochables à la main.

### 3. Core Image Threading (développement motivique, musique)
- **But** : développer des réflexes de connexion entre les éléments d'un monde.
- **Déroulé** : une image centrale est tirée (ex. « une clé rouillée à laquelle il manque une dent »). L'app pose une série de questions de worldbuilding (que ouvre-t-elle ? comment a-t-elle rouillé ? quels PNJ y sont liés ? …). Une question à la fois, l'utilisateur répond puis passe à la suivante.
- Durée : 10 min.

### 4. Status Numbers (théâtre d'improvisation, d'après la théorie du statut de Keith Johnstone)
- Le jeu avec cartes 1-10 est une adaptation courante de la communauté impro, pas un exercice d'*Impro* (1979) : ne pas l'attribuer à Johnstone.
- **But** : élargir la palette de personnalités de PNJ.
- **Déroulé** : tirage d'une carte virtuelle (As à 10 = statut bas → haut) + un rôle de PNJ. L'utilisateur joue le PNJ à voix haute en communiquant le statut **par le comportement uniquement**.
- Rappel affiché : le statut ≠ le rang social, c'est la façon dont le personnage s'attend à être traité. Statut haut : contrôle la conversation, regard direct, suppose la coopération. Statut bas : hésite, se coupe, évite le regard.
- Option « contre-stéréotype » (ex. prince de statut bas).
- Option « bascule » : à mi-parcours, l'app annonce un événement qui change le statut.
- Saisie : notes facultatives après la scène. Enregistrement audio facultatif (v2).

### 5. Decision Point Drill (science du sport)
- **But** : reconnaissance de situations et éventail de réponses.
- **Déroulé** : l'utilisateur regarde un actual play de son côté et met en pause quand un joueur surprend le MJ. Dans l'app : **30 secondes pour écrire 3 réponses possibles**, dont au moins une qu'il ne ferait jamais d'habitude (case à cocher pour la marquer).
- Mode alternatif sans vidéo : l'app propose des situations surprises tirées d'une table.
- Le but n'est pas de deviner ce qu'a fait le MJ, mais d'élargir son répertoire : **t'entraîner à ce que ta première idée soit meilleure** (cf. heuristique *take-the-first*, Johnson & Raab), pas « plus d'options = mieux ».

### 6. Connect the Story (écriture)
- **But** : donner l'impression que des éléments disparates étaient prévus.
- **Déroulé** : tirage de **5 éléments** : un lieu, un objet, un trait de personnage, un détail sensoriel, une courte réplique. **10 minutes** pour écrire une scène qui les relie tous.
- Les 5 éléments sont pré-cochés automatiquement quand ils sont mentionnés (détection approximative par mots-clés, recalculée pendant la frappe) ; l'utilisateur peut cocher/décocher à la main et **le cochage manuel fait foi**.

### 7. One-Page Design (game design)
- **But** : préparation — condenser une situation complexe à l'essentiel.
- **Déroulé** : l'utilisateur décrit une situation de sa campagne (ex. intrigue politique) puis la résume en une page : liste des PNJ clés, leurs motivations, leurs relations.
- v1 : formulaire structuré (PNJ, motivation, liens). v2 : schéma de relations éditable.
- Pas de minuteur imposé (optionnel).
- Mode sans campagne : une situation de départ est tirée dans `one_page_situations.json`.

> Durées : celles de Constraint Box, Core Image, Status et One-Page sont des choix du projet (non données par la source).

## Détection des mots (Constraint Box, Connect the Story)

- Normalisation par jeton (minuscules, accents retirés, œ/æ, apostrophes) ; comparaison avec les `keywords` fournis par le rédacteur pour chaque entrée.
- Racinisation Snowball (recopiée dans le dépôt) seulement si le corpus de test montre un rappel insuffisant.
- Tolérance aux fautes de frappe désactivée par défaut, activable par entrée (mots ≥ 8 lettres).
- La détection ne fait que pré-cocher ; le cochage manuel fait foi. Corpus de test doré obligatoire.

## Contenu (tables aléatoires)

Fichiers JSON dans `assets/tables/` :
- `problems.json` (Crazy Eights)
- `places.json`, `sensory_words.json` (Constraint Box)
- `core_images.json`, `threading_questions.json` (Core Image)
- `npc_roles.json`, `status_shifts.json` (Status Numbers)
- `surprise_situations.json` (Decision Point)
- `story_elements.json` avec 5 catégories (Connect the Story)
- `one_page_situations.json` (One-Page, mode sans campagne)

Au moins 30 entrées par table pour la v1. Contenu original, en français.
Chaque entrée porte un `id` stable et un champ `difficulty` (1-3) ; le champ `keywords` (1 à 4 mots-clés/synonymes) est **obligatoire** dans `story_elements.json` et `sensory_words.json`.

## Hors périmètre v1

- Synchronisation cloud, comptes
- IA générative intégrée (prévue après la v1, en option : voir la feuille de route)
- Enregistrement audio
- Thèmes entièrement personnalisés (la v1 propose seulement un choix de palette, voir « Fonctionnalités communes »)
- Notifications (rappels, fin de minuteur hors app) → v1.1, optionnelles, désactivées par défaut

## Feuille de route après la v1

- **v1.1** : exercices « Échec qui avance », « Règle des 3 indices », « Oui, et… » ; rappels par notification locale optionnels.
- **v1.2** : « PNJ éclair ».
- **Après la v1 — LLM optionnel** (D51) : exercices interactifs (dialogue) et génération de situations. Désactivé par défaut ; service distant (ChatGPT, Claude, Mistral…) avec la clé d'API de l'utilisateur, ou modèle local. L'app reste entièrement utilisable sans. Demandera la permission réseau, donc un nouvel ADR qui amende l'ADR 0010.
- **Plus tard — saisie vocale intégrée** (D52) : bouton micro avec reconnaissance sur l'appareil.

## Identité

- Logo et icône (D49, D50) : **d20 dont la face est un sablier**, sur fond **brique** (`#8A2E1C`) ; source `docs/ux/logo/03-d20-sablier.svg`.

## Ordre de développement proposé

1. Squelette du projet, navigation, écran d'accueil
2. Composant minuteur réutilisable + moteur de tirage depuis JSON
3. Exercices 1, 2 et 6 (les plus simples)
4. Room + historique
5. Exercices 3, 4, 5, 7
6. Statistiques, finitions
