# Checklist de recette manuelle — 1.0.0-rc1

Appareil : Galaxy Z Flip3. Version testée : `1.0.0-rc1` (À propos). Testeur : ________ Date : ________
Installer la variante `.release` (voir `release.md` §3) : `./gradlew assembleRelease -Pdmgym.signReleaseWithDebug=true`, puis `adb install -r`. **Ne jamais désinstaller `io.github.qnicoud.dmgym`** (historique réel).
Chaque ligne : cocher OK ou KO, noter une remarque. Un KO = une ligne dans `rapport-rc1.md`.

Légende : [ ] OK  [ ] KO

## 1. Les 7 exercices (prêt, en cours, bilan)

| # | Critère (SPEC) | OK | KO | Remarque |
|---|---|---|---|---|
| 1.1 | Accueil : 7 cartes avec nom, discipline d'origine, durée ; One-Page marqué « Préparation » | [ ] | [ ] | |
| 1.2 | Crazy Eights : problème tiré, 8 champs numérotés, indicateur x/8, 8 min par défaut | [ ] | [ ] | |
| 1.3 | Crazy Eights : rappel de rythme (1 impulsion par minute) si activé dans les Réglages | [ ] | [ ] | |
| 1.4 | Constraint Box : lieu + 3 mots sensoriels, 5 min, mots surlignés et soulignés quand ils apparaissent (accents, casse, pluriel) | [ ] | [ ] | |
| 1.5 | Constraint Box : cochage manuel d'un mot fait foi (décocher un mot détecté reste décoché en continuant à écrire) | [ ] | [ ] | |
| 1.6 | Core Image : image centrale, questions numérotées une à la fois, « Précédente/Suivante », liste complète consultable, 10 min | [ ] | [ ] | |
| 1.7 | Core Image : le champ de saisie reste visible clavier ouvert | [ ] | [ ] | |
| 1.8 | Status Numbers : carte 1-10 + rôle PNJ, rappel « statut ≠ rang » repliable, options contre-stéréotype et bascule (explication claire) | [ ] | [ ] | |
| 1.9 | Status Numbers : bascule à mi-parcours annoncée, « Scène terminée », notes facultatives | [ ] | [ ] | |
| 1.10 | Decision Point : 30 s, 3 réponses, case « jamais fait », mode situation (tirage) et mode vidéo (« Pause faite ») | [ ] | [ ] | |
| 1.11 | Decision Point : bilan « Laquelle aurais-tu jouée en premier ? » sans jugement | [ ] | [ ] | |
| 1.12 | Connect the Story : 5 éléments (lieu, objet, trait, détail sensoriel, réplique) pré-cochés pendant la frappe, cochage manuel prioritaire | [ ] | [ ] | |
| 1.13 | Connect the Story : liste repliée = pas d'énumération, champ de saisie pleine hauteur sur le Flip3 | [ ] | [ ] | |
| 1.14 | One-Page : situation tirée (mode sans campagne) ou libre, formulaire PNJ / motivation / liens (5 PNJ max), sans minuteur par défaut | [ ] | [ ] | |
| 1.15 | « Relancer le tirage » : nouveau tirage différent du précédent, compteur de relances | [ ] | [ ] | |
| 1.16 | Durée réglable sur l'écran Prêt et option « sans minuteur » sur chaque exercice | [ ] | [ ] | |

## 2. Minuteur : les 6 états

| # | État / critère | OK | KO | Remarque |
|---|---|---|---|---|
| 2.1 | Idle (prêt) : durée affichée, rien ne tourne | [ ] | [ ] | |
| 2.2 | Running : décompte, barre à bouts arrondis, écran maintenu allumé | [ ] | [ ] | |
| 2.3 | Dernière minute : icône + libellé + motif rayé (pas la couleur seule) | [ ] | [ ] | |
| 2.4 | Paused : texte « En pause » + icône, saisie toujours possible | [ ] | [ ] | |
| 2.5 | Overtime (temps additionnel) : aucun dialogue, saisie possible après la fin, libellé neutre (pas « échec ») | [ ] | [ ] | |
| 2.6 | Untimed (sans minuteur) : puce « Sans minuteur », temps réel enregistré, « Lancer un minuteur » possible | [ ] | [ ] | |
| 2.7 | Pause puis reprise : le temps n'avance pas pendant la pause | [ ] | [ ] | |
| 2.8 | Pause pendant le temps additionnel | [ ] | [ ] | |
| 2.9 | Arrière-plan 30 s puis retour : minuteur juste (horodatage) | [ ] | [ ] | |
| 2.10 | Arrière-plan jusqu'après l'échéance : retour en temps additionnel, **pas de signal tardif** | [ ] | [ ] | |
| 2.11 | Écran verrouillé puis déverrouillé en cours d'exercice | [ ] | [ ] | |

## 3. Signal de fin (matrice complète : `l3-signal-fin.md`)

| # | Mode sonnerie | Attendu | OK | KO | Remarque |
|---|---|---|---|---|---|
| 3.1 | Sonnerie, son + vibration | bip + 2 impulsions | [ ] | [ ] | |
| 3.2 | Sonnerie, son seul | bip seul | [ ] | [ ] | |
| 3.3 | Sonnerie, vibration seule | 2 impulsions | [ ] | [ ] | |
| 3.4 | Sonnerie, ni son ni vibration | rien | [ ] | [ ] | |
| 3.5 | Vibreur, son + vibration | pas de bip, vibration | [ ] | [ ] | |
| 3.6 | Silencieux | pas de bip, noter la vibration | [ ] | [ ] | |
| 3.7 | Ne pas déranger | pas de bip, noter la vibration | [ ] | [ ] | |
| 3.8 | Force Douce / Normale / Forte + « Essayer » | trois forces distinctes | [ ] | [ ] | |
| 3.9 | Pendant une vidéo ou de la musique | bip superposé, média non coupé | [ ] | [ ] | |
| 3.10 | Signal joué une seule fois par exercice | [ ] | [ ] | |

## 4. Sauvegarde, reprise, rotation, mort du processus

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 4.1 | Sauvegarde automatique : écrire, quitter brutalement (balayer l'app), rouvrir : carte « Reprendre » avec extrait et temps | [ ] | [ ] | |
| 4.2 | Reprendre : retrouve texte, tirage, case cochées ; si le temps est passé, reprise en temps additionnel | [ ] | [ ] | |
| 4.3 | Retour système en cours d'exercice : brouillon enregistré, snackbar, aucune confirmation | [ ] | [ ] | |
| 4.4 | Rotation (Flip3 ouvert / replié) sur chaque écran : texte, minuteur, cases, défilement conservés | [ ] | [ ] | |
| 4.5 | Option développeur « Ne pas conserver les activités » : quitter l'app en plein exercice, revenir : état restauré, pas de crash | [ ] | [ ] | |
| 4.6 | Idem sur Historique filtré, Détail, Séance du jour, Réglages | [ ] | [ ] | |
| 4.7 | Fermer/replier le Flip3 en cours d'exercice : aucune perte | [ ] | [ ] | |

## 5. Historique et détail de session

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 5.1 | Liste groupée par jour (Aujourd'hui, Hier, date), extrait, bijou, niveau | [ ] | [ ] | |
| 5.2 | Filtre par exercice et filtre « Bijoux » ; état et défilement conservés en changeant d'onglet | [ ] | [ ] | |
| 5.3 | État vide encourageant (aucune session) | [ ] | [ ] | |
| 5.4 | Détail : tirage (mots utilisés en texte), réponses, durées (active + additionnelle), grille | [ ] | [ ] | |
| 5.5 | Détail : changer le niveau (3 niveaux), marquer/démarquer un bijou | [ ] | [ ] | |
| 5.6 | Détail : partager le texte, « Refaire avec un nouveau tirage » | [ ] | [ ] | |
| 5.7 | Détail : supprimer avec confirmation, annulation possible | [ ] | [ ] | |
| 5.8 | Bilan facultatif après « Terminer » : 3 niveaux, bijou, jamais obligatoire | [ ] | [ ] | |

## 6. Séance du jour et S07

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 6.1 | Carte sur l'accueil : 3 exercices, durée totale, One-Page absent par défaut | [ ] | [ ] | |
| 6.2 | Aperçu : « Remplacer » ouvre la feuille, le choix est mémorisé pour la journée | [ ] | [ ] | |
| 6.3 | Même séance si on quitte et revient le même jour ; nouvelle séance le lendemain | [ ] | [ ] | |
| 6.4 | « Commencer » enchaîne : fil « Séance du jour · 1 sur 3 », « Passer cet exercice », « Exercice suivant » | [ ] | [ ] | |
| 6.5 | Retour en cours de séance : retour à l'aperçu avec Fait / En cours / À faire | [ ] | [ ] | |
| 6.6 | S07 Fin de séance : récap, niveaux, bijoux, compteur de sessions, série ; « Retour à l'accueil », « Refaire un exercice » | [ ] | [ ] | |

## 7. Progrès et série

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 7.1 | Nombre de sessions affiché en premier et en grand | [ ] | [ ] | |
| 7.2 | Série de jours avec jour de grâce, sans remise à zéro punitive ; jours récents (disque / pointillé / vide + lettre) | [ ] | [ ] | |
| 7.3 | Sessions par exercice, liens « Voir mes bijoux » et par exercice vers l'Historique filtré | [ ] | [ ] | |
| 7.4 | Passage de minuit : une session à 23 h 59 puis une à 00 h 01 comptent pour deux jours | [ ] | [ ] | |
| 7.5 | Changement de fuseau horaire (Réglages Android) : pas de doublon ni de trou incohérent | [ ] | [ ] | |
| 7.6 | État vide (0 session) | [ ] | [ ] | |

## 8. Réglages et À propos

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 8.1 | Durée par exercice : − / + dans les bornes, « sans minuteur » par exercice | [ ] | [ ] | |
| 8.2 | Thème : Parchemin (défaut), Couleurs du téléphone (Android 12+), palettes ; clair / sombre / système | [ ] | [ ] | |
| 8.3 | Vibration, son, force, « Essayer », rappel de rythme, inclure One-Page dans la séance | [ ] | [ ] | |
| 8.4 | Réduire les animations (suit le système ou forcé) | [ ] | [ ] | |
| 8.5 | Exporter mes sessions : fichier texte partagé, contenu lisible | [ ] | [ ] | |
| 8.6 | Tout effacer : confirmation, historique vide ensuite, réglages conservés (tester sur la variante `.release` uniquement) | [ ] | [ ] | |
| 8.7 | À propos : version `1.0.0-rc1`, licences Alegreya, Atkinson Hyperlegible Next, bip | [ ] | [ ] | |

## 9. Écran de démarrage et premier lancement

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 9.1 | Écran de démarrage (icône d20 sablier sur brique), pas de flash blanc, sans clignotement | [ ] | [ ] | |
| 9.2 | Icône adaptative correcte sur le lanceur (formes rondes et carrées) | [ ] | [ ] | |
| 9.3 | Premier lancement (variante `.release` neuve) : accueil avec « 0 session », ligne « Tout reste sur ton téléphone » | [ ] | [ ] | |
| 9.4 | Aucune permission demandée à l'exécution | [ ] | [ ] | |

## 10. Hors ligne

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 10.1 | Mode avion (Wi-Fi et données coupés) : parcourir les 7 exercices, enregistrer, historique, export | [ ] | [ ] | |
| 10.2 | Paramètres Android > Applications > DM Gym > Autorisations : aucune autorisation listée | [ ] | [ ] | |

## 11. Version de publication `.release`

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 11.1 | S'installe à côté de l'app réelle sans l'écraser | [ ] | [ ] | |
| 11.2 | Aucun crash (`adb logcat -d -b crash`) : SerializationException, ClassNotFound, NoSuchMethod | [ ] | [ ] | |
| 11.3 | Les 7 exercices s'ouvrent, tirent, enregistrent (tables JSON chargées après minification) | [ ] | [ ] | |
| 11.4 | Navigation et restauration (routes sérialisées) après « Ne pas conserver les activités » | [ ] | [ ] | |
| 11.5 | Historique (Room) et réglages (DataStore) persistent après fermeture et redémarrage du téléphone | [ ] | [ ] | |
| 11.6 | Taille de l'APK et temps de démarrage à froid raisonnables | [ ] | [ ] | |

## 12. Accessibilité : TalkBack

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 12.1 | Accueil : chaque carte lue en un élément (nom, discipline, durée) ; titres navigables par en-têtes | [ ] | [ ] | |
| 12.2 | Chaque bouton a un libellé (Retour, Réglages, Plus d'actions, − / + des durées) | [ ] | [ ] | |
| 12.3 | Minuteur : un seul élément, pas d'annonce seconde par seconde ; jalons début, mi-parcours, dernière minute, fin | [ ] | [ ] | |
| 12.4 | Champs : « Idée 3 sur 8 » ; le focus reste dans le champ ; le surlignage n'est pas lu | [ ] | [ ] | |
| 12.5 | Cases, boutons radio, interrupteurs : type et état annoncés | [ ] | [ ] | |
| 12.6 | Ordre de focus : barre du haut, minuteur, consigne, champs, bouton ; « Terminer » atteignable en 2-3 balayages | [ ] | [ ] | |
| 12.7 | Après relance ou changement de question : annonce unique, focus sur le nouvel élément | [ ] | [ ] | |
| 12.8 | Progrès : équivalent textuel des graphiques (« Crazy Eights : 12 sessions ») | [ ] | [ ] | |
| 12.9 | Détail de session, Séance du jour, S07 : lecture cohérente, états (Fait / À faire) annoncés | [ ] | [ ] | |
| 12.10 | Aucun dialogue bloquant en fin de temps ; fin annoncée « Temps écoulé. Tu peux continuer à écrire. » | [ ] | [ ] | |
| 12.11 | Clavier physique / commutateur : Tab, Entrée, Échap | [ ] | [ ] | |

## 13. Accessibilité : police 200 % et affichage

| # | Critère | OK | KO | Remarque |
|---|---|---|---|---|
| 13.1 | Taille de police maximale (Réglages Android) : aucun texte coupé sur les 10 écrans principaux | [ ] | [ ] | |
| 13.2 | Exercice à 200 % avec clavier ouvert, écran replié : minuteur et « Terminer » visibles | [ ] | [ ] | |
| 13.3 | Zoom d'affichage maximal | [ ] | [ ] | |
| 13.4 | Thème sombre : contraste lisible partout | [ ] | [ ] | |
| 13.5 | Aucune information portée par la couleur seule (mots utilisés, élément relié, niveau, série, bijou) | [ ] | [ ] | |
| 13.6 | Animations réduites système : pas de retournement de carte ni de transitions | [ ] | [ ] | |

## Verdict

[ ] Prêt pour la publication  [ ] Corrections nécessaires — KO listés : ________________
