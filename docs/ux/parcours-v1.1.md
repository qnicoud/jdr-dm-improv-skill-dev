# DM Gym v1.1 — Parcours et interface

Date : 2026-10-18. Auteur : ux-designer. Entrées : `docs/pilotage.md` (v1.1 — périmètre), `SPEC.md`, `parcours.md` et `design-system.md` (références, non modifiées), `docs/research/02` (exercices A, B, C), `docs/qa/tests-utilisateur.md`, `docs/qa/recette-rc1-resultats.md`, `docs/mockups/`.
Les principes de `parcours.md` §1 restent valables : aucune fenêtre pendant un exercice, jamais de couleur seule, jamais de jugement, cibles ≥ 48 dp. Les IDs d'écran continuent la numérotation (S13 et suivants). Les décisions ouvertes sont en section 8 (`V11-n`, à convertir en D54+).

## 0. Leçons des retours utilisateur appliquées à toute la v1.1

Chaque choix de ce document en découle ; ils sont rappelés ici une seule fois.

| Retour (source) | Règle v1.1 |
|---|---|
| Petit écran Z Flip : liste d'éléments repliée qui mange le champ (tests, Connect the Story) ; consigne trop haute en paysage (recette 4.4) | **Budget de la zone épinglée : minuteur ≤ 56 dp + consigne ≤ 2 lignes.** Aucune liste dans la zone épinglée des 3 nouveaux exercices. Le détail (types, registres) vit sur l'étiquette de chaque champ, qui défile avec lui. |
| « Préciser à quoi sert la bascule » (Status Numbers) | **Toute option a une phrase d'explication visible** sur l'écran Prêt (pas d'infobulle). Vaut pour « Bloquer d'abord ». |
| Pas de cochage sur l'écran Prêt (recette 1.5, 1.12) | Prêt = lecture seule, hors options explicitement nommées. |
| Question « jouée en premier » sans sélection visible (recette 1.11) | Le choix « ma préférée » d'Échec qui avance réutilise **le même composant**, avec une sélection lisible (coche + contour + libellé « Choisie »). |
| Balise « Préparation » à harmoniser avec la puce de durée (tests) | L'étiquette « Nouveau » reprend ce style : même forme que la puce de durée, fond plus discret. |
| Feedback de succès près du bouton (recette 8.5) | Export, import, rappel : icône de validation **à côté du bouton**, pas un texte plus bas. |
| Réglage direct depuis l'exercice (recette 1.16) | Les 3 nouveaux exercices utilisent la feuille de durée de l'écran Prêt telle quelle. |

Cible d'écran de référence : Galaxy Z Flip3 déplié (≈ 412 × 1000 dp ; clavier ouvert ≈ 400 dp de hauteur visible, la zone épinglée vaut donc ≈ 140 dp) et 360 × 640 dp à 200 % comme pire cas.

## 1. Les trois nouveaux exercices

Tous utilisent l'écran générique **Prêt → En cours → Bilan** (S04 / S05 / S06) et le moteur d'exercice existant (ADR 0012). Aucun nouvel écran, seulement des variantes de contenu. Vocabulaire inchangé : « tirage », « relance », « temps additionnel », « bijou ».

### 1.1 Fiches d'identité (carte d'accueil, composant `ExerciseCard`)

| | Échec qui avance | Règle des 3 indices | Oui, et… |
|---|---|---|---|
| Nom affiché | Échec qui avance | Règle des 3 indices | Oui, et… |
| Discipline (ligne secondaire) | Game design narratif | Enquête | Théâtre d'impro |
| Durée (puce) | 4 min | 5 min | 5 min |
| Icône (Material Symbols Rounded, à vérifier) | `moving` | `search` (loupe) | `forum` (bulles de dialogue) |
| Étiquette | « Nouveau » tant qu'aucune session de cet exercice n'existe | idem | idem |
| Description TalkBack | « Échec qui avance, game design narratif, 4 minutes, nouveau. » | idem avec « enquête » | idem avec « théâtre d'impro » |
| Justification de l'icône | Flèche vers l'avant = « avancer malgré l'échec » ; distincte de `alt_route` (Decision Point) | Évite `hub` (Core Image) | `forum` ≠ `chat_bubble` (réplique de Connect the Story) |

L'étiquette « Nouveau » est **déduite de l'historique** (aucune session de cet exercice), donc rien à stocker et elle disparaît toute seule. Elle porte un texte, jamais un point de couleur seul.

### 1.2 Échec qui avance

**Consigne (Prêt, 2 phrases)** : « Le jet est raté. Ne dis pas non : fais avancer l'histoire. Écris 3 complications, chacune d'un type différent. »

**Prêt (S04)**
- Carte de tirage : la **situation d'échec** (« Le voleur rate son jet pour crocheter la porte du temple. »).
- Sous la carte, **3 étiquettes de type tirées** (icône + mot) : par exemple « Coût », « Temps », « Information ». Elles sont **imposées et toujours distinctes** : la contrainte de l'exercice est ainsi garantie sans que l'utilisateur ait à classer ses propres idées (ce qui l'obligerait à s'auto-juger).
- Types proposés (6 ; liste finale au rédacteur de contenu) : **Coût** (`payments`), **Temps** (`schedule`), **Information** (`info`), **Relation** (`group`), **Position** (`move_down`), **Danger** (`bolt`).
- « Relancer le tirage » retire situation et types ; compteur de relances inchangé. Puce « 4 min », feuille de durée existante.
- Phrase d'aide sous les étiquettes : « Un type par complication. Il t'oblige à ne pas répéter la même idée. »

**En cours (S05)**
- Zone épinglée : minuteur + situation (2 lignes, « Voir tout »). Rien d'autre.
- 3 `NumberedField` (1 à 3 lignes). Chaque champ a **au-dessus** une étiquette « Complication 1 · Coût » (icône + texte) et un placeholder propre au type, pour démarrer sans réfléchir : Coût « Ça coûte quoi ? », Temps « Qu'est-ce qui presse ? », Information « Qu'apprend-on, ou que perd-on de vue ? », Relation « Qui est touché ? », Position « Où se retrouve-t-on ? », Danger « Qu'est-ce qui menace ? ». Ils disparaissent à la frappe.
- Compteur : puce « 2/3 » + 3 segments.
- Clavier : IME **Suivant** passe au champ suivant, **Terminé** sur le 3e (ferme le clavier, ne termine pas l'exercice).
- Dernière minute : palier actif (durée 4 min ≥ 3 min), comme les autres.

**Bilan (S06)**, ordre : récap (« 3 complications · 3 min 50 s ») → **Ta préférée** → niveau → grille repliée → bijou → actions.
- **Ta préférée** : titre « Laquelle ferait le plus avancer l'histoire ? », 3 lignes sélectionnables (groupe de choix unique, 56 dp min), chacune = étiquette de type + début du texte (3 lignes max). Sélection = coche + contour 2 dp + libellé « Choisie ». Seconde touche = désélection. Facultatif, sautable. Si un champ est vide, sa ligne est absente.
- Grille (2 questions Oui/Non) : « Chaque complication change-t-elle la situation ? » ; « Une complication m'a-t-elle surpris ? »
- Justification : choisir sa préférée force une comparaison sans note ; c'est le geste de relecture que la recherche valorise (§4 point 4 de `02`).

**Détail (S09)** : situation, puis 3 complications avec leur type ; la préférée porte l'icône `check_circle` et le texte « Ma préférée ».

**Séance du jour** : durée courte et accessible → éligible à l'échauffement.

### 1.3 Règle des 3 indices

**Consigne** : « Tes joueurs doivent découvrir cette conclusion. Écris 3 indices : un dans un objet, un chez un PNJ, un dans un lieu. Un seul suffit pour avancer. »

**Prêt**
- Carte de tirage : la **conclusion** (« Le bourgmestre est un imposteur. »).
- Trois étiquettes **fixes** (pas de tirage) : Objet (`inventory_2`), PNJ (`face`), Lieu (`place`). Elles reprennent les icônes de catégorie de Connect the Story (cohérence, déjà validées par l'utilisateur en recette).
- Aide : « Trois registres différents : si les joueurs ratent l'un, un autre les attend. »
- Relance : retire la conclusion.

**En cours**
- Zone épinglée : minuteur + conclusion (2 lignes). Pas de rappel de la règle (elle est sur les étiquettes).
- 3 champs, étiquette « Indice 1 · Objet », placeholders : Objet « Qu'est-ce qu'on trouve ? », PNJ « Que dit ou laisse échapper quelqu'un ? », Lieu « Que remarque-t-on sur place ? ».
- Ordre libre : on peut commencer par le lieu. Le compteur compte les champs non vides (« 2/3 »), sans ordre imposé.
- Clavier : Suivant / Terminé comme ci-dessus.

**Bilan**
- Récap : « 3 indices · 4 min 40 s ».
- Grille : « Chaque indice pointe-t-il vers la conclusion sans la dire ? » ; « Pourrais-je les glisser dans ma prochaine séance ? »
- Pas de bloc « préférée ».

**Détail** : conclusion + 3 indices avec registre. Pas de surlignage de mots.
**Séance du jour** : durée 5 min, rôle d'exercice « standard ». Complémentaire de One-Page et de Decision Point (recherche §3).

### 1.4 Oui, et…

C'est l'exercice le plus différent : 5 mini-tâches séquentielles de ~60 s. Il reprend la structure de **Core Image Threading** (une question à la fois, « Précédente / Suivante »), déjà validée (« c'est parfait », tests utilisateur), au lieu d'inventer un nouveau patron.

**Consigne** : « Un joueur fait une proposition. Réponds « Oui, et… » et ajoute quelque chose. Compte environ une minute par offre. »

**Prêt**
- Pas de carte de tirage unique : une carte de description « 5 offres de joueurs, tirées au fur et à mesure » (les offres ne sont pas révélées d'avance : l'effet de surprise fait partie de l'exercice). Relancer n'a donc pas de sens avant de commencer ; le bouton est masqué. DÉCISION V11-4.
- **Option « Bloquer d'abord »** (interrupteur, désactivé par défaut) avec explication visible : « Pour chaque offre, écris d'abord comment tu la bloquerais (ton premier réflexe), puis ta réponse « Oui, et… ». Cela t'aide à repérer ta façon de bloquer. » Le titre de l'option est neutre : on observe, on ne culpabilise pas (« réflexe », pas « défaut »).
- Durée 5 min (feuille de durée). Si le mode bloquer est actif, la durée conseillée affichée est inchangée ; l'utilisateur peut la rallonger. Justification : ne pas changer silencieusement un réglage choisi.

**En cours**
- Zone épinglée : minuteur (56 dp) + **offre en cours** (« Un joueur dit : « Je veux séduire le dragon. » », 2 lignes) + « Offre 3 sur 5 » avec 5 points (courant = point plein plus grand). Le minuteur est **global** (5:00). Pas de second minuteur : deux comptes à rebours simultanés surchargent et le minuteur est un composant unique.
- **Rythme de 60 s** : discret et optionnel. Un fin repère visuel (segment du point de l'offre qui se remplit sur 60 s) et, si « vibration chaque minute » est activée dans Réglages (réglage existant, non par défaut), une vibration brève. **Aucun passage automatique à l'offre suivante** : une avance forcée ferait perdre une phrase en cours d'écriture. L'utilisateur passe quand il veut (« Offre suivante »).
- Champ(s) :
  - Mode normal : 1 champ « Oui, et… » avec un préfixe visuel non modifiable « Oui, et » à gauche (étiquette, pas du texte saisi) pour que l'utilisateur écrive seulement l'ajout. Placeholder : « …ajoute quelque chose ».
  - Mode « Bloquer d'abord » : 2 champs superposés, A « Mon réflexe : je bloque… » (1 ligne, ~80 caractères) puis B « Oui, et… ». Le focus commence en A.
- Boutons en bas, au-dessus du clavier (comme Core Image) : « Précédente » (texte), « Offre suivante » (tonal) ; sur l'offre 5 : « Terminer ». Lien discret « Passer cette offre ». Les réponses précédentes restent atteignables avec « Précédente ».
- Clavier : IME **Suivant** = A → B puis offre suivante ; en mode normal, Suivant = offre suivante ; sur l'offre 5 : **Terminé** ferme le clavier.
- Temps additionnel : même traitement. Si le temps passe avant la 5e offre, on continue, rien n'est coupé.

**Bilan**
- Récap : « 5 offres · 4 min 40 s » (ou « 4 offres, 1 passée »).
- Grille : « Mon « et » ajoute-t-il quelque chose de nouveau ? » ; « Ai-je construit sur l'offre sans la corriger ? » ; **si le mode bloquer était actif** : « Ai-je repéré mon réflexe de blocage ? » (3e question, jamais affichée sinon).
- Pas de bloc « préférée ».

**Détail** : liste des 5 offres, pour chacune la réponse ; en mode bloquer, le réflexe est affiché avant, étiqueté « Réflexe » (icône `block` + texte).
**Séance du jour** : exercice court sans préparation, idéal en échauffement.

### 1.5 Impact sur les écrans existants

| Écran | Changement |
|---|---|
| S02 Accueil | La liste passe de 7 à 10 cartes. **Pas de regroupement** (aucune catégorie évidente, un titre de plus coûte de la hauteur) ; ordre : les 7 existants, puis les 3 nouveaux avec l'étiquette « Nouveau ». Le bandeau de séance du jour reste au-dessus, donc visible sans défiler. |
| S02 Accueil, étiquette « Nouveau » | Elle disparaît après la première session **comptée** de l'exercice (terminée, ou inachevée avec du contenu), comme le compteur de sessions (ADR 0011). Une session inachevée sans contenu la laisse en place. |
| S03 Séance du jour | Aucun changement de forme. Les nouveaux exercices entrent dans le tirage (pool de 10, One-Page toujours exclu). Un exercice jamais pratiqué compte 0 session, donc la règle « zone faible » (ADR 0011) le sélectionne naturellement : pas de règle de promotion à ajouter. DÉCISION V11-13 pour la séance déjà proposée le jour de la mise à jour. |
| S08 Historique | La rangée de filtres passe de 9 à 12 puces (défilement horizontal déjà prévu). Filtre « Bijoux » reste en 2e position, avant les exercices, pour rester atteignable. |
| S10 Progrès | La répartition par exercice passe à 10 lignes. Inchangé en logique : on ne pointe pas le moins pratiqué. Alternative textuelle déjà en liste. |
| S11 Réglages | Durées par défaut : 10 lignes au lieu de 7 (sous-écran Durées déjà défilant). |

## 2. Rappels (désactivés par défaut)

### 2.1 Emplacement et réglage (S11, nouvelle section « Rappels »)

Position : entre « Minuteur » et « Apparence ». Justification : c'est un comportement de l'entraînement, pas un détail de données.

| Élément | Contenu |
|---|---|
| Ligne principale | Interrupteur « Rappel d'entraînement ». Sous-titre : désactivé → « Désactivé » ; activé → « Lun, mer, ven · vers 18 h 30 ». Le sous-titre sert de résumé, évitant d'ouvrir le détail. |
| Texte d'aide permanent | « Une notification discrète, aux jours choisis. Rien ne quitte ton téléphone. » |
| Heure (visible si activé) | Ligne « Heure » → sélecteur d'heure Material 3 (mode cadran ou saisie au clavier, au choix de l'utilisateur ; la saisie clavier est le mode accessible). Défaut : 18 h 30 (fin de journée, avant une soirée de jeu). Le format 12/24 h suit le téléphone. |
| Jours (visible si activé) | 7 boutons à bascule, 48 dp, étiquette lettre « L M M J V S D » **et** nom complet en description. Sélectionné = fond + coche + contour ; non sélectionné = contour seul. Défaut : lundi, mercredi, vendredi (3 jours par semaine : rythme tenable, cohérent avec un jour de grâce par semaine). À 200 %, les 7 boutons passent sur 2 rangées. |
| Dernier jour désélectionné | La désélection du dernier jour est ignorée ; texte d'aide neutre sous la rangée : « Garde au moins un jour, ou coupe le rappel avec l'interrupteur. » Pas de rouge. |
| « Envoyer un test » | Bouton texte qui envoie la notification tout de suite. Utile pour vérifier permission et canal, et pour voir le rendu. Visible si activé. |
| « Réglages Android de cette notification » | Lien texte vers le canal « Rappels d'entraînement » (son, importance, écran de verrouillage). Visible si activé. |

Les changements s'appliquent immédiatement, sans bouton « Enregistrer » (principe existant). Quand le rappel est programmé, une icône `check` apparaît près de l'interrupteur et la ligne de résumé se met à jour ; TalkBack annonce « Rappel activé, lundi, mercredi, vendredi, vers 18 h 30 » (annonce polie unique).

L'heure est « vers » parce que la programmation approximative évite la permission d'alarme exacte (`SCHEDULE_EXACT_ALARM`) : un retard de quelques minutes est sans conséquence ici. À noter pour l'architecte (ADR amendant 0010) : c'est un choix qui découle de l'UX, pas une contrainte subie.

### 2.2 Demande de permission (Android 13+ seulement)

Principe : **au moment où l'utilisateur active l'interrupteur pour la première fois, jamais avant** (ni au premier lancement, ni à l'ouverture de Réglages). Une demande contextuelle est comprise et acceptée plus souvent, et l'aide permanente a déjà expliqué la raison.

1. L'utilisateur touche l'interrupteur (pour la première fois).
2. Le dialogue système apparaît (aucune fenêtre maison avant lui ; le texte d'aide au-dessus du commutateur fait office d'explication).
3. **Autorisé** : l'interrupteur passe à activé, les lignes Heure / Jours apparaissent, le rappel est programmé, annonce « Rappel activé ».
4. **Refusé** : l'interrupteur **revient à désactivé** (l'état reflète la réalité), et sous la ligne apparaît un message neutre avec icône `info` : « Les notifications sont refusées pour DM Gym. Tu peux les autoriser dans les réglages Android. » + bouton texte « Ouvrir les réglages Android ». Pas de rouge, pas de répétition de la demande.
5. Au retour dans l'app depuis les réglages Android, l'écran revérifie la permission : si elle est accordée, le message disparaît (l'interrupteur reste désactivé, c'est à l'utilisateur de l'activer, pour ne pas créer d'effet de bord).
6. **Refus définitif** (le système ne montre plus le dialogue, deux refus) : le toucher de l'interrupteur affiche directement le message de l'étape 4.
7. Android ≤ 12 : pas de permission, étapes 1 et 3 seulement.

**Permission retirée plus tard alors que le rappel est activé** : à l'ouverture de Réglages, la ligne affiche « Rappel en pause : les notifications sont désactivées dans Android. » + « Ouvrir les réglages Android ». L'interrupteur reste actif (il traduit l'intention) mais l'état « en pause » est écrit en texte. Aucune notification d'avertissement.

### 2.3 Texte de la notification

Règles de rédaction, vérifiables :
- Jamais de mention de perte (« ne perds pas ta série », « tu n'es pas venu »), de reproche, de compte de jours d'absence, d'urgence, de point d'exclamation, d'émoji.
- La série et le jour de grâce **ne sont jamais cités dans la notification**. Justification : la seule façon d'être toujours cohérent avec un jour de grâce (qui absorbe un jour manqué) est de ne rien affirmer de la série ; citer « série de 5 jours » dans un rappel transforme l'outil en menace.
- Titre « DM Gym » ; un seul texte, court, parmi un petit jeu choisi par la date (déterministe, comme la séance du jour), pour éviter la lassitude.
- Un seul rappel par jour, pas de relance ni de report (« Plus tard »).

| Situation | Texte du corps (exemples, à valider par le rédacteur) |
|---|---|
| Cas général (séance du jour pas faite) | « La séance du jour t'attend : 3 exercices, environ 14 min. » / « 5 minutes pour échauffer ton imagination ? » / « Un petit exercice avant la partie ? » |
| Aucune session encore (0 session) | « Un premier exercice ? Cinq minutes suffisent. » |
| Série en cours, jour de grâce disponible ou utilisé, série rompue | Même jeu de textes que le cas général (volontairement identique). |
| Séance du jour déjà terminée, ou ≥ 1 session aujourd'hui | **Aucune notification** (vérifié au moment de l'envoi). |

- Appui sur la notification : ouvre **S03 Aperçu de la séance du jour** (le texte promet une séance). Retour → Accueil. S'il y a un brouillon, l'Accueil l'affiche de toute façon.
- Pas de bouton d'action dans la notification (Android 13 a déjà un geste de rejet ; chaque action ajoute du bruit).
- Importance recommandée : **basse** (pas de son, pas de bandeau flottant) ; l'utilisateur peut la relever dans les réglages du canal. DÉCISION V11-7.
- Visible sur l'écran verrouillé : le texte est anodin, donc oui.
- **Mise en pause automatique** après 14 jours sans session : le rappel cesse d'être envoyé, sans notification d'adieu ; dans Réglages la ligne affiche « Rappel en pause : aucune session depuis 14 jours. Reprendre. » DÉCISION V11-8.
- Survie au redémarrage du téléphone : le rappel reste programmé (à signaler à l'architecte : cela relève de l'ADR 0010).

### 2.4 Accessibilité des rappels
- Ligne d'interrupteur = un élément TalkBack : « Rappel d'entraînement, activé, lundi, mercredi, vendredi, vers 18 h 30 ». Les jours : groupe nommé « Jours du rappel », chaque bouton « Lundi, sélectionné ».
- Messages d'erreur/permission : texte + icône `info`, jamais la couleur seule ; focus non volé.
- À 200 % : l'aide permanente passe sur plusieurs lignes, rien n'est tronqué, les jours sur 2 rangées.
- Notification : texte lu tel quel par TalkBack ; aucune information portée par une icône seule.

## 3. Import des sessions

### 3.1 Où et comment (S11 Réglages > section Données)

Ordre des lignes (la destructive reste en dernier, séparée par un filet) :
1. **Exporter pour lire** (le texte lisible actuel, inchangé)
2. **Sauvegarder mes sessions** (nouveau : fichier réimportable, `.json`)
3. **Importer des sessions** (nouveau → ouvre S13)
4. *(filet)* **Tout effacer**

Deux lignes d'export distinctes plutôt qu'un choix de format : le but de chacune se lit dans son intitulé et son sous-titre (« Pour lire ou partager » / « Pour changer de téléphone ou d'installation »). DÉCISION V11-9.

La sauvegarde indique ce qu'elle contient : « Tes sessions, tirages, évaluations et bijoux. Les réglages ne sont pas inclus. » Après la sauvegarde : icône `check` près du bouton et **nom du fichier + nombre de sessions** (« dm-gym-sauvegarde-2026-10-18.json · 42 sessions »), car c'est la preuve dont l'utilisateur a besoin avant de désinstaller quoi que ce soit. Nom de fichier ASCII, daté.

### 3.2 Parcours d'import (S13, écran plein, sans barre de navigation)

États : **A** choix, **B** lecture, **C** aperçu, **D** résultat, **E** erreur.

| État | Contenu | Actions |
|---|---|---|
| **A Choix** | Titre « Importer des sessions ». Texte : « Retrouve tes sessions depuis un fichier créé par « Sauvegarder mes sessions ». Elles sont ajoutées à celles que tu as déjà : rien n'est supprimé ni remplacé. » Pliable « Je change d'installation de DM Gym » (voir 3.4). | Primaire « Choisir un fichier » (sélecteur système, aucune permission de stockage). |
| **B Lecture** | Barre de progression indéterminée + « Lecture du fichier… ». S'affiche seulement si la lecture dépasse 300 ms. | Aucune (pas de bouton Annuler utile ; le retour système abandonne). |
| **C Aperçu** | Carte récap, de haut en bas : **« 42 sessions trouvées »** (titleLarge) ; « du 3 au 17 octobre 2026 » ; deux lignes avec icône : « 39 seront ajoutées » (`add_circle`), « 3 sont déjà présentes » (`check_circle`, non modifiées). S'il y en a : « 2 d'un exercice inconnu seront ignorées ». Phrase : « Rien n'est supprimé. Les sessions déjà présentes ne changent pas. » | Primaire « Ajouter 39 sessions » ; texte « Choisir un autre fichier ». |
| **C′ Rien à ajouter** | « Toutes les sessions de ce fichier sont déjà dans ton carnet. » | « Choisir un autre fichier », « Fermer ». Pas d'action primaire. |
| **D Résultat** | Icône `check` + « 39 sessions ajoutées. » ; « 3 étaient déjà présentes. » ; « Ton historique et tes statistiques sont à jour. » | Primaire « Voir l'historique » ; texte « Annuler cet import » (tant que l'écran est ouvert) ; « Terminé ». |
| **E Erreur** | Voir 3.3. | « Choisir un autre fichier ». |

- L'import est **atomique** : tout est ajouté ou rien ; le message d'erreur le dit (« Rien n'a été ajouté »).
- « Annuler cet import » supprime exactement les sessions ajoutées, sans dialogue (retour par snackbar « Import annulé »). Disponible seulement sur S13 ; une fois quitté, les sessions s'effacent une par une depuis S09. DÉCISION V11-11.
- Sessions importées : conservent leur bijou, évaluation, texte, date d'origine. Elles sont indistinguables des autres dans l'historique (pas d'étiquette « importée »), pour ne pas créer deux classes de sessions.
- Série et « meilleure série » : calculées depuis l'historique, donc recalculées par l'import. Si le moteur les stocke, l'import doit les recalculer (point à confirmer par l'architecte).
- Conflit (même session présente des deux côtés mais avec une évaluation différente) : **l'existante est conservée**. DÉCISION V11-10.

### 3.3 Erreurs (carte d'erreur existante `ErrorCard`, icône `error`, texte neutre ; c'est une vraie erreur technique donc le conteneur d'erreur est permis)

| Cas | Titre | Texte | Action |
|---|---|---|---|
| Fichier qui n'est pas une sauvegarde (JSON invalide, autre format) | « Ce fichier n'est pas une sauvegarde DM Gym » | « Choisis un fichier créé avec « Sauvegarder mes sessions ». » | Choisir un autre fichier |
| Export texte lisible choisi par erreur (`.txt` ou en-tête reconnu) | « Ce fichier est un export à lire » | « Il ne peut pas être réimporté. Utilise « Sauvegarder mes sessions » pour créer un fichier réimportable. » | Choisir un autre fichier |
| Fichier tronqué ou abîmé | « Le fichier semble incomplet » | « Rien n'a été ajouté. Refais une sauvegarde depuis l'installation d'origine si tu peux. » | Choisir un autre fichier |
| Version trop récente | « Ce fichier vient d'une version plus récente de DM Gym » | « Mets l'app à jour pour l'importer. Rien n'a été ajouté. » (pas de lien de mise à jour : l'app est hors ligne et installée à la main) | Choisir un autre fichier |
| Fichier illisible (fournisseur de fichiers, droit retiré) | « Impossible de lire ce fichier » | « Copie-le sur le téléphone, puis choisis-le de nouveau. » | Choisir un autre fichier |
| Fichier trop volumineux (plafond défini par l'architecte) | « Ce fichier est trop gros pour une sauvegarde DM Gym » | « Vérifie que c'est le bon fichier. » | Choisir un autre fichier |

Version ancienne du format : importable silencieusement (migration), jamais d'erreur. À l'arrivée en état E, le focus TalkBack va sur le titre de l'erreur, annonce polie.

### 3.4 Passage de l'app de développement à l'app signée

Risque central : une APK signée avec une autre clé ne peut pas remplacer l'installation de développement si l'identifiant d'application est le même ; il faut alors désinstaller, ce qui **efface les données**. Et l'app rc1 actuelle **n'a pas d'export réimportable** (seulement le texte lisible, non réimportable). Procédure à documenter (à vérifier par l'architecte, notamment l'identifiant de l'APK signée : un suffixe `.release` permettrait la cohabitation sans désinstaller) :

1. Installer la build v1.1 **par-dessus** l'installation de développement actuelle (même signature, les données sont conservées).
2. Dans cette app : Réglages > Données > **Sauvegarder mes sessions**. Enregistrer le fichier dans « Documents » ou « Téléchargements », **pas** dans un dossier de l'application. Vérifier le nom et le nombre de sessions affichés.
3. Installer l'app signée (en désinstallant l'ancienne si l'identifiant est identique).
4. Dans la nouvelle app : Réglages > Données > **Importer des sessions** > choisir le fichier.

L'écran S13 état A contient cette procédure dans un bloc pliable « Je change d'installation de DM Gym », en 4 lignes numérotées, avec l'avertissement en tête : « Sauvegarde avant de désinstaller : désinstaller efface les sessions. » Le bloc est replié par défaut, le cas étant rare. DÉCISION V11-12 : prévoir, à défaut, un import du texte lisible de la rc1 ?

### 3.5 Accessibilité de l'import
- Chaque état est un écran simple à un seul bouton primaire ; ordre de focus : titre, texte, bouton.
- Le récap de l'aperçu est un bloc lu en une phrase : « 42 sessions trouvées, 39 seront ajoutées, 3 sont déjà présentes. »
- Résultat : annonce polie « 39 sessions ajoutées ».
- 200 % : aucune troncature ; les deux lignes de récap passent sur 2 lignes chacune, le bouton primaire reste visible en bas (barre d'action fixée).

## 4. Gestion de la langue

### 4.1 Mon avis sur une 2e langue pour ce public

- **Public principal** : MJ francophones qui jouent à leur table. Le contenu est surtout du **texte créatif** (tirages, offres de joueurs, mots sensoriels) : il ne se traduit pas, il se réécrit. Chaque exercice a ≥ 30 entrées, 10 exercices : 300 entrées ou plus, plus la détection de mots-clés et son corpus de test par langue.
- **Le seul candidat sérieux est l'anglais** : la communauté du JdR et les sources pédagogiques (Johnstone, Alexandrian, Crazy Eights) sont anglophones, et l'audience est bien plus large. Les autres langues (allemand, espagnol) ne se justifient pas sans demande identifiée.
- **Valeur pour l'utilisateur actuel** : faible à court terme. Elle devient réelle si l'app est publiée, ou si l'utilisateur joue en anglais.
- **Piège principal** : une langue à moitié livrée (interface traduite, tirages en français) est pire qu'une seule langue. La « langue » doit donc rester **un seul commutateur** : interface + tirages + détection ensemble, ou pas du tout.
- **Recommandation** : en v1.1, **architecture seule** (ressources par langue, tables par langue, détection paramétrée, formats via la locale, langue de session stockée) et **aucun sélecteur affiché tant qu'une 2e langue complète n'est pas livrée**. Un sélecteur à un seul choix est du bruit. Le français sert d'étalon de longueur (cf. 4.4). DÉCISION V11-1.

### 4.2 Réglage (si 2 langues ou plus sont livrées)

- **Emplacement** : Réglages > Apparence, ligne « Langue » dont le sous-titre affiche la langue courante.
- **Contrôle** : feuille basse avec liste à choix unique : « Langue du téléphone » (défaut), « Français », « English ». **Chaque langue est écrite dans sa propre langue** (autonymes) : on doit pouvoir retrouver sa langue si l'interface est devenue illisible. Chaque option porte son attribut de langue pour que TalkBack la prononce correctement.
- **Android 13+** : l'app déclare ses langues pour apparaître dans Réglages Android > Applications > DM Gym > Langue. Les deux entrées (ligne in-app et sélecteur système) sont **synchronisées** : un seul réglage, deux portes. L'ouverture de la ligne in-app peut renvoyer vers le sélecteur système sur 13+, mais une feuille in-app est préférable (cohérente sur toutes les versions, pas de sortie de l'app). DÉCISION V11-2.
- **Application** : immédiate. L'écran se recrée dans la nouvelle langue, l'utilisateur reste sur Réglages avec le focus sur la ligne « Langue », annonce polie « Langue : English ». Aucune confirmation (réversible en un geste).
- **Langue du téléphone non gérée** : repli sur le français, sans message.

### 4.3 Comportement des sessions passées et des brouillons

| Élément | Règle | Justification |
|---|---|---|
| Texte saisi et tirages enregistrés | **Jamais traduits** : ce sont des données, conservées dans leur langue d'origine. | Traduire du contenu de l'utilisateur est faux et irréversible. |
| Noms d'exercices, libellés de champs, questions de grille, étiquettes | Suivent la langue de l'interface (clés), y compris dans l'historique. | Ce sont des textes d'interface ; le texte reste lisible en tout cas. |
| Langue de chaque session | **Enregistrée avec la session** (ajout au schéma, migration testée, ADR 0004). | Nécessaire pour la détection de mots surlignés dans le détail et la prononciation TalkBack. |
| Détail d'une session passée | Le contenu en langue d'origine est balisé avec sa langue (TalkBack lit « Auberge de relais » avec la voix française même si l'interface est en anglais). Le surlignage utilise la langue de la session. | Lecture correcte ; cohérence des surlignages. |
| Brouillon à reprendre | Reprise dans sa langue d'origine (tirage conservé), interface dans la nouvelle langue. | Évite de perdre ou de dénaturer un brouillon. |
| Séance du jour | La graine reste la date ; la suggestion est la même, les exercices sont ceux de la langue courante. | Pas de règle supplémentaire. |
| Statistiques et série | Indépendantes de la langue. | |
| Sauvegarde `.json` | Neutre en langue (identifiants + textes d'origine + langue de session). L'export lisible suit la langue de l'interface. | Une sauvegarde faite en français doit s'importer en anglais. |
| Dates, nombres, durées | Via la locale (« 17 octobre 2026 » / « October 17, 2026 »). Aucun format en dur. | |

### 4.4 Longueur des textes sur petit écran

- Le français est l'une des langues les plus longues parmi celles visées : l'anglais est en général 10 à 20 % plus court. Le français sert de **cas défavorable**. Si une 3e langue plus longue (allemand, +30 %) arrive un jour, elle impose un nouveau test.
- Règles : aucun libellé tronqué par « … » ; les boutons passent sur 2 lignes (hauteur min 48 dp, pas hauteur fixe) ; les **onglets restent sur une ligne** (noms courts : « S'entraîner / Historique / Progrès », « Train / History / Progress ») ; le sélecteur de niveau à 3 boutons (« Prêt pour la table » est le plus long) retourne à la ligne dans le bouton ; la rangée des 7 jours du rappel passe sur 2 rangées.
- **Test de pseudo-localisation** (+40 %, caractères accentués) à 200 % sur 360 × 640 dp, pour les 10 exercices et les écrans S11, S13. Critère : minuteur et « Terminer » visibles avec clavier ouvert.
- Les placeholders de champ sont courts (≤ 30 caractères) : ils disparaissent à la frappe de toute façon.
- La traduction des textes de l'interface passe par les ressources (`strings.xml` par langue), jamais dans le code Compose (règle existante).

## 5. Accessibilité (récapitulatif v1.1)

| Sujet | Règle |
|---|---|
| Cibles | ≥ 48 dp partout : jours du rappel, lignes de choix « préférée », boutons Précédente/Suivante, options de langue. Écarts ≥ 8 dp. |
| Lignes sélectionnables | Chaque ligne « Ta préférée » est un élément radio : « Complication 2, coût : …, sélectionnable » ; l'état « Choisie » est annoncé et écrit. |
| Couleur seule | Type de complication, registre d'indice, jour sélectionné, option choisie : icône ou coche + texte à chaque fois. |
| TalkBack, exercice | Le minuteur reste un élément unique (« Minuteur : 4 minutes 10 restantes ») ; aucune annonce par seconde. Annonces ponctuelles : début, dernière minute, fin. Oui, et… : annonce polie « Offre 3 sur 5 : … » au passage à l'offre suivante. Le préfixe visuel « Oui, et » est exposé au champ comme étiquette (« Oui, et… réponse »), pas comme texte saisi. |
| Focus clavier/commutateur | Ordre : barre du haut, minuteur, offre/situation, champs, boutons. |
| 200 % | Zone épinglée plafonnée à 35 % ; à 200 % la situation/offre se replie à 2 lignes avec « Voir tout » ; au moins 3 lignes de saisie visibles (D47). Les étiquettes de champ (« Complication 1 · Coût ») peuvent passer sur 2 lignes. |
| Animations réduites | Le segment de 60 s de Oui, et… ne s'anime pas : il passe d'un état à l'autre par paliers. |
| Notification | Texte seul porteur de sens ; réglage du canal accessible depuis l'app. |
| Aucune fenêtre pendant un exercice | Aucun des trois exercices n'ouvre de dialogue ; la feuille de durée est limitée à l'écran Prêt. Les erreurs d'import et la permission de notification n'interviennent jamais pendant une session. |

## 6. États récapitulatifs par exercice

| État | Échec qui avance | Règle des 3 indices | Oui, et… |
|---|---|---|---|
| Vide (Prêt) | Situation + 3 types | Conclusion + 3 registres | Description + option |
| En cours | 3 champs étiquetés, « 2/3 » | 3 champs étiquetés, « 2/3 » | Offre « 3 sur 5 », 1 ou 2 champs |
| Temps additionnel | Standard (§4.4 de `parcours.md`) | idem | idem ; les offres restantes restent accessibles |
| Erreur de table (X) | Carte d'erreur + « Écrire ma propre situation » | Carte d'erreur + « Écrire ma propre conclusion » | Carte d'erreur ; pas de mode libre (les offres sont l'exercice) |
| Sans minuteur | Autorisé | Autorisé | Autorisé (le repère de 60 s est masqué) |

## 7. Textes d'interface nouveaux (clés à créer dans `strings.xml`)

Exemples de libellés sont donnés dans les sections ci-dessus ; la liste exhaustive est à tirer de ce document par le développeur. Les textes d'aide et de rappel sont sans point d'exclamation.

## 8. Décisions (tranchées le 2026-10-18 : toutes les recommandations retenues, D54–D70 dans l'artefact ; V11-2 → sélecteur in-app AppCompat)

DÉCISION V11-1: Langue en v1.1 — (a) architecture seule, sélecteur masqué, (b) architecture + interface anglaise + tables anglaises complètes, (c) interface seule traduite — recommandation (a) : une langue à moitié livrée dégrade l'expérience ; (b) en v1.2 si la publication est confirmée.
DÉCISION V11-2: Sélecteur de langue — (a) feuille in-app synchronisée avec le sélecteur système Android 13, (b) renvoi vers le sélecteur système sur 13+ et feuille in-app en dessous — recommandation (a) : même comportement sur toutes les versions.
DÉCISION V11-3: Type des 3 complications — (a) tirés et imposés distincts, (b) choisis par l'utilisateur après saisie, (c) libre — recommandation (a) : garantit la contrainte, évite l'auto-jugement.
DÉCISION V11-4: Oui, et… — offres révélées une à une, pas de relance avant de commencer — (a) oui, (b) les 5 offres visibles dès l'écran Prêt avec relance — recommandation (a), mais (b) si l'utilisateur préfère préparer.
DÉCISION V11-5: Rythme de 60 s de Oui, et… — (a) repère visuel + vibration optionnelle, avance manuelle, (b) avance automatique à 60 s — recommandation (a) : ne jamais couper une phrase en cours.
DÉCISION V11-6: Jours et heure par défaut du rappel — (a) lun/mer/ven à 18 h 30, (b) tous les jours, (c) aucun jour présélectionné — recommandation (a).
DÉCISION V11-7: Importance de la notification — (a) basse (silencieuse), (b) par défaut (son) — recommandation (a) : usage en mobilité et avant une partie ; modifiable dans le canal.
DÉCISION V11-8: Pause automatique du rappel après 14 jours sans session — (a) oui, état visible dans Réglages, (b) non — recommandation (a) : un rappel ignoré qui insiste est le contraire de l'esprit « sans culpabilisation ».
DÉCISION V11-9: Export — (a) deux lignes (« Exporter pour lire », « Sauvegarder mes sessions »), (b) une ligne avec choix du format — recommandation (a).
DÉCISION V11-10: Conflit à l'import (session déjà présente, contenu différent) — (a) l'existante gagne, (b) la plus récente gagne — recommandation (a) : « rien n'est remplacé » est une promesse simple.
DÉCISION V11-11: Annuler un import — (a) bouton sur le résultat, valable tant que l'écran est ouvert, (b) aucun — recommandation (a) : supprimer 39 sessions une à une serait pénible.
DÉCISION V11-12: Rattrapage de la rc1 (aucun export réimportable) — (a) imposer la procédure « installer la v1.1 par-dessus, sauvegarder, puis changer d'app », (b) importer aussi le texte lisible de la rc1 — recommandation (a) : le texte lisible ne contient pas tout et l'analyser est fragile. Vérifier l'identifiant d'application de l'APK signée (cohabitation possible ou non).
DÉCISION V11-13: Séance du jour déjà générée le jour de la mise à jour — (a) la figer jusqu'au lendemain, (b) la recalculer avec le nouveau pool — recommandation (a) : ne pas changer une séance sous les yeux de l'utilisateur.
DÉCISION V11-14: Réglages inclus dans la sauvegarde (durées, thème, rappel) — (a) non, sessions seulement, (b) oui — recommandation (a) pour la v1.1 (périmètre), dit clairement dans l'interface.
DÉCISION V11-15: Citer la série dans la notification (« 5 jours de suite ») — (a) jamais, (b) variante positive — recommandation (a) : la cohérence avec le jour de grâce est garantie par l'absence d'affirmation.
