# Checklist de recette manuelle — 1.1.0

Appareil : Galaxy Z Flip3. Version testée : `1.1.0` (À propos). Testeur : ________ Date : ________
Installer la variante `.release` pour les contrôles de publication (voir `release.md` §3) ; pour la mise à jour réelle (§9), utiliser `adb install -r` par-dessus l'app installée. **Ne jamais désinstaller `io.github.qnicoud.dmgym`** (historique réel).
Chaque ligne : cocher OK ou KO, noter une remarque. Un KO = une ligne dans `rapport-v1.1.md`.
Prérequis : avoir au moins une sauvegarde `.json` valide de la version précédente (voir §5) et noter le nombre de sessions avant la mise à jour.

Légende : [ ] OK  [ ] KO

## 1. Échec qui avance

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 1.1 | Accueil : carte « Échec qui avance », discipline « game design narratif », durée 4 min | [ ] | [ ] | |
| 1.2 | Prêt : consigne claire (« Le jet est raté… »), 3 types de complication tirés et affichés, durée réglable, option « sans minuteur » | [ ] | [ ] | |
| 1.3 | En cours : 3 champs « Complication » numérotés, chacun étiqueté de son type (Coût, Temps, Information, Relation, Position, Danger) avec l'aide du type | [ ] | [ ] | |
| 1.4 | « Relancer le tirage » : de nouveaux types, jamais deux fois le même type dans un tirage, le texte déjà saisi n'est pas perdu | [ ] | [ ] | |
| 1.5 | Pas de liste d'éléments dans la zone épinglée : le champ de saisie garde sa hauteur sur le Flip3 (ouvert et replié), clavier ouvert | [ ] | [ ] | |
| 1.6 | Bilan : « Laquelle ferait le plus avancer l'histoire ? », « Ma préférée » sélectionnable, grille de 2 questions, niveau et bijou facultatifs | [ ] | [ ] | |
| 1.7 | Historique et détail : types et complications lus correctement, « Refaire avec un nouveau tirage » | [ ] | [ ] | |

## 2. Règle des 3 indices

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 2.1 | Accueil : carte « Règle des 3 indices », discipline « enquête », durée 5 min | [ ] | [ ] | |
| 2.2 | Prêt : une conclusion tirée à faire découvrir, consigne « 3 indices : objet, PNJ, lieu » | [ ] | [ ] | |
| 2.3 | En cours : 3 champs « Indice 1 · Objet », « Indice 2 · PNJ », « Indice 3 · Lieu » avec leurs indications | [ ] | [ ] | |
| 2.4 | « Relancer le tirage » : nouvelle conclusion différente de la précédente | [ ] | [ ] | |
| 2.5 | Bilan : 2 questions (« pointe vers la conclusion sans la dire », « réutilisable »), icône PNJ visible dans le détail de session | [ ] | [ ] | |
| 2.6 | Minuteur : pause, arrière-plan 30 s, temps additionnel, saisie après la fin | [ ] | [ ] | |

## 3. Oui, et…

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 3.1 | Accueil : carte « Oui, et… », discipline « théâtre d'impro », durée 5 min | [ ] | [ ] | |
| 3.2 | Prêt : aucune offre n'est révélée à l'avance ; option « Bloquer d'abord » avec son aide, désactivée par défaut | [ ] | [ ] | |
| 3.3 | En cours : une seule offre visible à la fois (« Offre 1 sur 5 »), l'offre courante reste épinglée, le préfixe « Oui, et » est affiché sans faire partie du texte saisi | [ ] | [ ] | |
| 3.4 | Offres tirées dans l'ordre des catégories (social, action, exploration, imprévu), 5 offres au total | [ ] | [ ] | |
| 3.5 | Repère par minute : une impulsion par minute (vibration activée) sans passage automatique à l'offre suivante | [ ] | [ ] | |
| 3.6 | Passage à l'offre suivante manuel : « Suivante » / « Précédente », retour possible, réponses conservées | [ ] | [ ] | |
| 3.7 | Touche **Suivant** du clavier : passe à l'offre suivante ; sur la dernière offre, la touche devient **Terminé** et ferme le clavier | [ ] | [ ] | |
| 3.8 | Mode « Bloquer d'abord » : un champ « Mon réflexe : je bloque… » apparaît avant « Ma réponse » ; touche Suivant passe du blocage à la réponse puis à l'offre suivante | [ ] | [ ] | |
| 3.9 | Mode bloquer : le champ de blocage a une limite souple d'environ 80 caractères, sans perte de texte | [ ] | [ ] | |
| 3.10 | Sans le mode bloquer : pas de champ de blocage, grille de 2 questions ; avec : 3 questions dont « Ai-je repéré mon réflexe de blocage ? » | [ ] | [ ] | |
| 3.11 | Reprise après arrêt brutal : retour sur la dernière offre répondue, texte retrouvé | [ ] | [ ] | |
| 3.12 | Historique et export : seules les offres atteintes sont listées, numérotées par offre | [ ] | [ ] | |

## 4. Étiquette « Nouveau »

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 4.1 | Les 3 nouveaux exercices portent l'étiquette « Nouveau » (texte, pas seulement un point de couleur) tant qu'aucune session de l'exercice n'existe | [ ] | [ ] | |
| 4.2 | Les 7 exercices existants ne portent pas l'étiquette | [ ] | [ ] | |
| 4.3 | Après une première session terminée d'un nouvel exercice, l'étiquette disparaît ; les deux autres la gardent | [ ] | [ ] | |
| 4.4 | Une session abandonnée ou en brouillon ne fait pas disparaître l'étiquette | [ ] | [ ] | |
| 4.5 | TalkBack : la carte est lue « …, nouveau » | [ ] | [ ] | |
| 4.6 | Après import d'une sauvegarde contenant une session d'un nouvel exercice, l'étiquette disparaît | [ ] | [ ] | |

## 5. Sauvegarde et import

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 5.1 | Réglages > Données : « Sauvegarder mes sessions » crée un fichier `.json` ; message « Sauvegarde terminée » avec nombre de sessions ; « Exporter mes sessions » reste un fichier texte lisible | [ ] | [ ] | |
| 5.2 | « Importer des sessions » : écran de choix avec « Choisir un fichier » et procédure repliable « Je change d'installation de DM Gym » | [ ] | [ ] | |
| 5.3 | Fichier valide : aperçu avec nombre de sessions trouvées, à ajouter, déjà présentes, période couverte ; « Rien n'est supprimé » | [ ] | [ ] | |
| 5.4 | Confirmer : « Ajouter N sessions » ; résultat « Import terminé », compteur de l'historique et des Progrès mis à jour | [ ] | [ ] | |
| 5.5 | Doublons : réimporter le même fichier = « Toutes les sessions de ce fichier sont déjà dans ton carnet », aucun bouton « Ajouter » | [ ] | [ ] | |
| 5.6 | Fichier partiel : ne s'ajoutent que les sessions absentes (identité exercice + heure de début), les autres sont comptées « déjà présentes » | [ ] | [ ] | |
| 5.7 | Annuler cet import : retire exactement les sessions ajoutées, snackbar « Import annulé », retour au choix de fichier ; l'historique retrouve son état d'avant | [ ] | [ ] | |
| 5.8 | Annulation après avoir tué l'app juste après l'import : le bouton d'annulation est toujours proposé à la réouverture | [ ] | [ ] | |
| 5.9 | Fichier invalide (photo, PDF renommé en `.json`) : « Ce fichier n'est pas une sauvegarde DM Gym », rien ajouté, « Choisir un autre fichier » | [ ] | [ ] | |
| 5.10 | Fichier tronqué (couper la fin d'une sauvegarde) : « Le fichier semble incomplet », rien ajouté | [ ] | [ ] | |
| 5.11 | Export texte lisible choisi par erreur : « Ce fichier est un export à lire », non réimportable | [ ] | [ ] | |
| 5.12 | Fichier trop gros (créer un fichier de plus de 8 Mio, ex. `head -c 9000000 /dev/zero > gros.json`) : « Ce fichier est trop gros… », pas de plantage, pas de lenteur notable | [ ] | [ ] | |
| 5.13 | Fichier d'une version plus récente (champ de version modifié) : message « version plus récente », rien ajouté | [ ] | [ ] | |
| 5.14 | Sessions d'un exercice inconnu dans le fichier : ignorées et comptées dans l'aperçu, le reste est importé | [ ] | [ ] | |
| 5.15 | Rotation et retour système pendant l'aperçu et le résultat : l'état est conservé, aucun doublon d'ajout par double appui | [ ] | [ ] | |
| 5.16 | Aller-retour : sauvegarde, « Tout effacer » (variante `.release` seulement), import : historique identique (textes, tirages, niveaux, bijoux, durées) | [ ] | [ ] | |

## 6. Rappels

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 6.1 | Réglages > Rappels : interrupteur désactivé par défaut, aucune demande de permission à l'ouverture de l'app ni des Réglages | [ ] | [ ] | |
| 6.2 | Premier passage à « activé » : dialogue système de permission ; autorisé, les lignes Heure et Jours apparaissent | [ ] | [ ] | |
| 6.3 | Refus de la permission : l'interrupteur revient à « désactivé », message neutre et lien « Ouvrir les réglages Android » | [ ] | [ ] | |
| 6.4 | Notifications coupées dans Android alors que le rappel est activé : « Rappel en pause : les notifications sont désactivées dans Android » avec lien | [ ] | [ ] | |
| 6.5 | Jours : 7 pastilles avec lettre et coche (pas la couleur seule) ; impossible de désélectionner le dernier jour, aide affichée | [ ] | [ ] | |
| 6.6 | Heure : cadran et saisie au clavier, format 12 h / 24 h selon le téléphone ; la valeur est conservée après fermeture | [ ] | [ ] | |
| 6.7 | « Envoyer un test » : notification discrète (importance basse, sans son) ; un appui ouvre la séance du jour, retour mène à l'accueil | [ ] | [ ] | |
| 6.8 | Rappel réel à l'heure approximative (régler à +3 min), un jour coché ; texte neutre, jamais culpabilisant | [ ] | [ ] | |
| 6.9 | Jour non coché : aucune notification | [ ] | [ ] | |
| 6.10 | Aucun rappel si une session a déjà eu lieu le jour même (faire une session, régler l'heure à +3 min, attendre) | [ ] | [ ] | |
| 6.11 | 14 jours sans session (simuler avec `adb shell date`/fuseau ou un historique ancien sur la variante `.release`) : « Rappel en pause : aucune session depuis 14 jours » ; « Reprendre » relance | [ ] | [ ] | |
| 6.12 | Redémarrage du téléphone avec rappel actif : l'alarme est réarmée (`adb shell dumpsys alarm \| grep dmgym`) ; avec rappel désactivé, rien n'est armé | [ ] | [ ] | |
| 6.13 | Mise à jour par-dessus (`adb install -r`) : l'alarme est réarmée | [ ] | [ ] | |
| 6.14 | Changement de fuseau : l'alarme suit l'heure murale | [ ] | [ ] | |
| 6.15 | Désactiver l'interrupteur : alarme annulée (plus d'entrée `dmgym` dans `dumpsys alarm`) | [ ] | [ ] | |
| 6.16 | Samsung « applis en veille prolongée » : noter si l'alarme est retardée ou supprimée (non bloquant) | [ ] | [ ] | |
| 6.17 | Une session en cours d'exercice n'est pas interrompue par la notification | [ ] | [ ] | |

## 7. Séance du jour figée

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 7.1 | Avant la mise à jour : noter les 3 exercices de la séance du jour affichés sur l'accueil | [ ] | [ ] | |
| 7.2 | Après la mise à jour le même jour : la séance est strictement la même (aucun des 3 nouveaux exercices n'y apparaît) ; les remplacements déjà faits sont conservés | [ ] | [ ] | |
| 7.3 | Quitter et rouvrir l'app, redémarrer le téléphone : même séance | [ ] | [ ] | |
| 7.4 | Le lendemain (ou après avance de la date) : nouvelle séance, tirée parmi les 9 exercices (One-Page absent par défaut) | [ ] | [ ] | |
| 7.5 | « Remplacer » propose les nouveaux exercices en alternative | [ ] | [ ] | |

## 8. Non-régression des 7 exercices (rapide)

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 8.1 | Accueil : 10 cartes (7 + 3), One-Page marqué « Préparation » | [ ] | [ ] | |
| 8.2 | Crazy Eights : tirage, 8 champs, indicateur x/8, rappel de rythme | [ ] | [ ] | |
| 8.3 | Constraint Box : mots surlignés (accents, casse, pluriel), cochage manuel prioritaire | [ ] | [ ] | |
| 8.4 | Core Image : questions une à une, Précédente/Suivante, champ visible clavier ouvert | [ ] | [ ] | |
| 8.5 | Status Numbers : carte, rôle PNJ, bascule à mi-parcours | [ ] | [ ] | |
| 8.6 | Decision Point : 30 s, 3 réponses, bilan sans jugement | [ ] | [ ] | |
| 8.7 | Connect the Story : 5 éléments pré-cochés, champ pleine hauteur | [ ] | [ ] | |
| 8.8 | One-Page : situation tirée ou libre, formulaire PNJ | [ ] | [ ] | |
| 8.9 | Minuteur : pause/reprise, arrière-plan, fin sans dialogue, temps additionnel | [ ] | [ ] | |
| 8.10 | Signal de fin : bip + vibration, une seule fois par exercice (matrice : `l3-signal-fin.md`) | [ ] | [ ] | |
| 8.11 | Historique, détail, partage, suppression avec confirmation | [ ] | [ ] | |
| 8.12 | Progrès : compteur, série, sessions par exercice (10 lignes) | [ ] | [ ] | |
| 8.13 | Séance du jour : enchaînement, « Passer cet exercice », écran de fin | [ ] | [ ] | |
| 8.14 | Réglages : durées des 10 exercices, thème, vibration/son, réduire les animations | [ ] | [ ] | |
| 8.15 | Rotation, replier/déplier le Flip3, « Ne pas conserver les activités » : rien n'est perdu | [ ] | [ ] | |

## 9. Correctif de la frappe juste avant « Terminer »

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 9.1 | Taper un mot puis appuyer aussitôt sur « Terminer » (sans quitter le champ) : le dernier mot est dans la session enregistrée (détail, partage) | [ ] | [ ] | |
| 9.2 | Idem avec la touche **Terminé** du clavier sur le dernier champ | [ ] | [ ] | |
| 9.3 | Idem sur un exercice à champs multiples (Crazy Eights, dernier champ) et sur une réponse de Core Image | [ ] | [ ] | |
| 9.4 | Idem sur les 3 nouveaux exercices, et dans le champ de blocage de « Oui, et… » | [ ] | [ ] | |
| 9.5 | Fin de temps puis frappe en temps additionnel puis « Terminer » : rien n'est perdu | [ ] | [ ] | |
| 9.6 | Retour système juste après une frappe : le brouillon contient la dernière lettre | [ ] | [ ] | |

## 10. Mise à jour depuis la rc1 par-dessus

À faire une seule fois, sur l'app réelle, **sans désinstaller**.

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 10.1 | Avant : noter version (À propos), nombre de sessions, série, durées réglées, thème, 1 session de chaque type reconnaissable | [ ] | [ ] | |
| 10.2 | Avant : faire une sauvegarde `.json` de sécurité hors de l'app (si la rc1 la propose) ou un export texte | [ ] | [ ] | |
| 10.3 | `adb install -r` (ou `installDebug`) réussit sans « INSTALL_FAILED_UPDATE_INCOMPATIBLE » (même clé de signature) | [ ] | [ ] | |
| 10.4 | Premier lancement : pas de crash (`adb logcat -d -b crash`), pas de perte visible, écran de démarrage normal | [ ] | [ ] | |
| 10.5 | Migration Room v2 → v3 : toutes les sessions de la rc1 sont présentes, même nombre qu'avant, détails lisibles | [ ] | [ ] | |
| 10.6 | Sessions rc1 ouvertes dans le détail : textes, tirages, niveau, bijou, durées inchangés | [ ] | [ ] | |
| 10.7 | Série et Progrès identiques à ceux d'avant la mise à jour | [ ] | [ ] | |
| 10.8 | Réglages conservés (durées, thème, vibration, son, « inclure One-Page ») | [ ] | [ ] | |
| 10.9 | Un brouillon laissé « à reprendre » avant la mise à jour est toujours repris (carte « Reprendre ») | [ ] | [ ] | |
| 10.10 | Nouvelle session après mise à jour : enregistrée et visible, l'historique mêle sessions rc1 et v1.1 | [ ] | [ ] | |
| 10.11 | Sauvegarde puis import sur l'historique mixte : sessions rc1 (sans langue enregistrée) acceptées, aucune erreur | [ ] | [ ] | |
| 10.12 | À propos : version `1.1.0` | [ ] | [ ] | |

## 11. Passage à l'APK signé

Procédure décrite dans `release.md` (section signature de publication, rédigée à part) ; cette section vérifie qu'elle fonctionne. Renvoyer vers la section correspondante de `release.md` et noter ici les écarts.

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 11.1 | `release.md` : la procédure de signature est complète et suivable sans aide (clé, `zipalign`, `apksigner`) ; la clé n'est pas dans le dépôt | [ ] | [ ] | |
| 11.2 | Build non signé : `./gradlew check koverVerify assembleRelease` vert | [ ] | [ ] | |
| 11.3 | `apksigner verify --print-certs` sur l'APK signé : un seul certificat, empreinte notée | [ ] | [ ] | |
| 11.4 | L'APK signé a `versionName` `1.1.0` et un `versionCode` supérieur à celui de la rc1 (`aapt dump badging`) | [ ] | [ ] | |
| 11.5 | Permissions de l'APK signé : seulement `VIBRATE`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` (+ la permission interne de récepteur dynamique) ; aucune permission réseau | [ ] | [ ] | |
| 11.6 | Installation de l'APK signé par-dessus une app signée **avec la même clé** : réussie, données conservées | [ ] | [ ] | |
| 11.7 | Installation par-dessus une app signée **autrement** (par exemple la clé debug) : refus attendu. Ne **pas** désinstaller l'app réelle : sauvegarder d'abord (§5), puis voir la procédure « Je change d'installation » | [ ] | [ ] | |
| 11.8 | Après installation de l'APK signé : mêmes contrôles rapides que le §8 (ouverture des 10 exercices, enregistrement, historique) et aucun crash `logcat -b crash` | [ ] | [ ] | |
| 11.9 | Sauvegarde de la clé de signature faite hors du dépôt, mot de passe noté ailleurs | [ ] | [ ] | |

## 12. Version de publication

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 12.1 | `versionName` `1.1.0` et `versionCode` incrémenté dans `app/build.gradle.kts` ; À propos affiche `1.1.0` | [ ] | [ ] | |
| 12.2 | `SPEC.md` et `release.md` mentionnent 1.1.0 (10 exercices, rappels, sauvegarde et import) | [ ] | [ ] | |
| 12.3 | Variante `.release` : s'installe à côté de l'app réelle, les 10 exercices s'ouvrent après minification (tables JSON chargées) | [ ] | [ ] | |
| 12.4 | Aucun crash (`adb logcat -d -b crash`) : SerializationException, ClassNotFound, NoSuchMethod, y compris à l'import d'un `.json` et au déclenchement d'un rappel | [ ] | [ ] | |
| 12.5 | Récepteurs de rappel présents après minification (`adb shell dumpsys package io.github.qnicoud.dmgym.release \| grep -i receiver`) ; « Envoyer un test » fonctionne sur la release | [ ] | [ ] | |
| 12.6 | Mode avion : parcours complet des 10 exercices, sauvegarde et import (le sélecteur de fichiers n'a pas besoin du réseau) | [ ] | [ ] | |
| 12.7 | Paramètres Android > DM Gym > Autorisations : seulement « Notifications » ; aucune autre | [ ] | [ ] | |
| 12.8 | Taille de l'APK et démarrage à froid raisonnables, comparés à la rc1 | [ ] | [ ] | |
| 12.9 | Notes de version rédigées (ce qui change, rappel que les réglages ne sont pas dans la sauvegarde) | [ ] | [ ] | |

## 13. Accessibilité et affichage (compléments v1.1)

L'audit automatisé couvre déjà cibles, libellés, titres et textes coupés à 200 % (voir `rapport-v1.1.md`) ; ici, seulement ce qu'il ne voit pas.

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 13.1 | TalkBack sur les 3 nouveaux exercices : « Offre 2 sur 5 », « Indice 2 · PNJ », type de complication annoncé avec le champ | [ ] | [ ] | |
| 13.2 | TalkBack sur Rappels : interrupteur, jours (sélectionné / non sélectionné), heure, « Rappel programmé » annoncé à l'activation | [ ] | [ ] | |
| 13.3 | TalkBack sur l'import : aperçu lu en une phrase, résultat annoncé, annulation atteignable | [ ] | [ ] | |
| 13.4 | Police maximale : aucun texte coupé sur les 3 nouveaux exercices, Rappels et Import | [ ] | [ ] | |
| 13.5 | Exercice « Oui, et… » à 200 % avec clavier ouvert, écran replié : offre courante et « Terminer » accessibles | [ ] | [ ] | |
| 13.6 | Thème sombre : contraste lisible sur l'étiquette « Nouveau », les pastilles de jours et l'écran d'import | [ ] | [ ] | |

## Verdict

[ ] Prêt pour la publication  [ ] Corrections nécessaires — KO listés : ________________
