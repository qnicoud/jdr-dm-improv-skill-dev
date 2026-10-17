# Rapport QA — 1.0.0-rc1

Date : 2026-10-17. Branche `test/rc1-qa`. Recette manuelle : `checklist-rc1.md` (à dérouler sur le Galaxy Z Flip3).

## 1. Audit d'accessibilité automatisé

Fichiers : `app/src/test/java/io/github/qnicoud/dmgym/accessibility/` (`AccessibilityAudit.kt`, `AccessibilityAuditTest.kt`, `AccessibilityAuditSelfTest.kt`). Robolectric (rendu graphique `NATIVE` pour des mesures de texte réelles) + APIs de test Compose, sans nouvelle dépendance.

Écrans audités, chacun à `fontScale` 1 puis 2, largeur 360 dp : accueil (avec barre d'onglets), les 7 exercices en Prêt, En cours et Bilan (21 états), historique, détail de session, progrès, séance du jour, fin de séance (S07), réglages, à propos. 15 tests.

Contrôles :
- zone tactile effective (`touchBoundsInRoot`) d'au moins 48 dp sur tout nœud cliquable ;
- nœud cliquable avec texte ou description ;
- champ de saisie avec libellé fusionné (« Idée 3 sur 8 ») ;
- éléments à état (case, radio, interrupteur, onglet) avec rôle ;
- au moins un `heading()` par écran ;
- à `fontScale = 2` : aucun texte coupé en hauteur ni tronqué par une ellipse.

Fiabilité de l'audit : 6 tests d'auto-vérification prouvent que chaque contrôle détecte bien son défaut (cible 24 dp, texte sans libellé, ellipse, absence de titre, champ sans libellé, case sans rôle). Un premier essai en rendu `LEGACY` avait produit des faux positifs (largeur de texte approximative) ; le dépassement en largeur est donc exclu et le rendu `NATIVE` utilisé.

**Résultat : 0 défaut.** Aucun correctif de code de production n'était nécessaire.

Information (non bloquante) : les boutons Material 3 et les icônes de barre font 40 dp de côté visuellement (Retour, Réglages, Terminer, Commencer de l'accueil, boutons − / + des durées, Remplacer, Continuer, Voir mes bijoux). Leur zone tactile est étendue à 48 dp par Compose (convention M3, prévue au design system §7.1). Le scanner d'accessibilité Android, qui lit les bornes visuelles, pourrait les signaler.

Limites de l'audit (à couvrir à la main) :
- contraste des couleurs (clair et sombre) et minimum 12 sp ;
- zone épinglée plafonnée à 35 % sur 640 dp de haut avec clavier (fenêtre de 5000 dp utilisée pour placer tous les nœuds) ;
- débordement en largeur non mesurable de façon fiable sous Robolectric ;
- ordre de focus, annonces TalkBack, jalons du minuteur ;
- feuilles et dialogues (remplacement de séance, confirmation d'effacement) ;
- listes paresseuses : seuls les éléments composés sont audités.

## 2. TalkBack sur appareil

Non réalisé : `timeout 10 adb devices` ne liste aucun appareil (téléphone non branché ou verrouillé). À faire via la section 12 de la checklist. Rien n'a été installé, désinstallé ni écrit sur l'appareil.

## 3. Défauts

| Gravité | Défaut | État |
|---|---|---|
| Corrigés | aucun trouvé par l'audit automatisé | |
| Ouverts | aucun défaut confirmé | |
| À confirmer sur appareil | voir risques ci-dessous | |

## 4. Suite de tests et couverture

`./gradlew check koverVerify` : vert (tests JVM, ktlint, lint, `verifyPermissions`, seuil Kover 80 %). Couverture d'instructions du périmètre Kover (logique métier, données, ViewModels) : **98,0 %** (16 672 / 17 020).

| Paquet | Couverture |
|---|---|
| core/timer | 99,0 % |
| core/exercise | 98,8 % |
| core/exercise/definitions | 100 % |
| core/detection | 93,9 % |
| core/daily | 98,9 % |
| core/streak | 100 % |
| core/settings | 100 % |
| core/history | 100 % |
| core/time | 100 % |
| core/tables | 90,1 % |
| core/smoke | 88,2 % |
| data/history | 93,1 % |
| data/history/db | 100 % |
| data/settings | 92,3 % |
| data/tables | 97,1 % |
| feature/exercise | 99,8 % |
| feature/history | 98,9 % |
| feature/settings | 99,9 % |
| feature/daily | 98,7 % |
| feature/home | 98,3 % |
| feature/stats | 99,2 % |

Paquets les plus faibles : `core/smoke` (code de fumée), `core/tables` et `data/settings` (chemins d'erreur d'E/S). Les écrans Compose sont exclus de la mesure (filtres Kover) mais couverts par les tests d'interface.

## 5. Risques avant publication

1. **Variante `.release` non parcourue** : le téléphone était verrouillé lors du dernier essai (`release.md`). Le minificateur pourrait casser un chemin d'exécution (tables JSON, routes sérialisées, Room) que les tests JVM ne voient pas. Moyenne-haute : dérouler la section 11 de la checklist.
2. **TalkBack et police 200 % non vérifiés sur appareil** (section 12 et 13). Moyenne.
3. **Signal de fin** : bip et vibration ont déjà été difficiles sur Samsung ; matrice des modes de sonnerie à valider (section 3). Moyenne.
4. **Contraste** (surtout thème sombre et palettes) non automatisé. Faible-moyenne.
5. **Signature de publication** : clé de l'utilisateur, une mise à jour exige la même clé. Procédure dans `release.md` §2.
6. **Mort du processus** : couvert par tests JVM de restauration, mais à confirmer avec « Ne pas conserver les activités » (section 4.5).
7. **Pas de test instrumenté** exécuté (aucun appareil).
