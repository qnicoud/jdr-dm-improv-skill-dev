# DM Gym — Maquettes HTML (index)

Date : 2026-10-16. Auteur : maquettiste. Sources : `docs/ux/brief-maquettes.md`, `design-system.md`, `parcours.md`.

Chaque fichier est autonome (CSS et JS en ligne, polices Google Fonts avec repli `Georgia` / `system-ui`, icônes en SVG en ligne, aucune image ni script externe). Cadre téléphone 360 × 800 dp (640 dp pour les variantes 200 %), jetons `--md-sys-color-*` identiques au design system §2, clair et sombre (classe `.dark`).

Barre d'outils de chaque page : **Clair / Sombre / Côte à côte**, case **Annotations** (contours pointillés sur les cibles tactiles), panneau d'annotations à droite (sous les maquettes à 360 px de large). Le JS clone chaque cadre en version sombre au chargement : sans JavaScript, seule la version claire s'affiche.

## Fichiers

| Fichier | Écran | États montrés | Thèmes | 200 % |
|---|---|---|---|---|
| `01-accueil.html` | Accueil (S02) | (a) normal avec brouillon à reprendre, séance entamée, 7 cartes d'exercice ; (b) vide, 0 session | clair (défaut), sombre | non demandé |
| `02-seance-du-jour.html` | Séance du jour (S03, S03b) | (a) aperçu Fait / En cours / À faire ; (b) feuille « Remplacer par… » sur Decision Point | clair (défaut), sombre | oui (c) |
| `03-constraint-box.html` | Constraint Box en cours (S05-2) | (a) en cours avec clavier, tiède et grésillement cochés, moisi non cochée (les puces se touchent) ; (b) Prêt avec tirage, « 0 relance », « 5 min » | clair + sombre | oui (c), avec clavier |
| `04-crazy-eights.html` | Crazy Eights en cours (S05-1) | (a) 5/8 avec clavier « Suivant », champ 6 en focus ; (b) vide sans clavier, « Ta première idée, même mauvaise. » | clair (défaut), sombre | non demandé |
| `05-minuteur-etats.html` | Planche du composant minuteur | six états étiquetés ; transition 3 vers 4 en 200 ms avec bouton, interrupteur « Animations réduites », annonce TalkBack ; copie en niveaux de gris | clair + sombre | non |
| `06-connect-the-story.html` | Connect the Story (S05-6) | (a) panneau déployé, 3 sur 5 reliés ; (b) compact avec clavier ; chevron plie/déplie, cases cliquables | clair (défaut), sombre | oui (c), avec clavier |
| `07-status-numbers.html` | Status Numbers (S05-4) | (a) avant bascule ; (b) bandeau de bascule | clair + sombre | non |
| `08-bilan.html` | Bilan (S06) | (a) initial ; (b) Solide choisi, questions ouvertes, bijou actif | clair + sombre | oui (c), pile verticale, variante « Exercice suivant (2 sur 3) » |
| `09-historique.html` | Historique et détail (S08, S09) | (a) liste, filtre Tous ; (b) vide ; (c) détail Constraint Box, menu ⋮ ouvert | clair + sombre | non |
| `10-progres.html` | Progrès (S10) | (a) normal ; (b) série rompue ; (c) vide | clair (défaut), sombre | non |

Les pages « clair + sombre » s'ouvrent en mode « Côte à côte ». Les autres s'ouvrent en clair ; le bouton « Sombre » ou « Côte à côte » affiche la variante sombre.

Interactions : bascule de thème ; minuteur (05) ; chevron et cases de Connect the Story ; puces de mots (03) ; sélecteur de niveau, Oui/Non et bijou (08, 09) ; repliables natifs.

## Écarts au brief

1. **Variantes 200 %** : le brief les attribue aux « priorités 2, 3 et 5 ». Faites sur les écrans 2 (Séance du jour), 3 (Constraint Box) et 6 (Connect the Story), qui sont les seuls à montrer minuteur, consigne et « Terminer ». Une quatrième est ajoutée pour le Bilan (sélecteur en pile). Format 360 × 640 dp, clavier 280 dp pour 3 et 6.
2. **Barre du haut à 200 %** : le titre est plafonné à 100 % et « Enregistré » se réduit à son icône, sinon « Terminer » ne tient pas sur 360 dp. « Terminer » double bien de taille.
3. **Consigne à 200 %** : en ligne unique « minuteur · pause · chevron » (étiquette « Consigne » portée par l'accessibilité, pas affichée).
4. **Rangée de 7 jours (Progrès)** : fenêtre glissante qui finit aujourd'hui (S D L M M J V), conforme au design system §5.11, et non L M M J V S D du brief. Avec le 16 octobre un vendredi, la semaine calendaire ne peut pas contenir 5 disques + 1 jour de grâce + aujourd'hui entouré.
5. **Connect the Story compact** : le minuteur perd sa barre de progression (le compteur « 3 sur 5 reliés » prend la place) ; texte de puce à 12 sp sur 2 lignes.
6. **Connect the Story déployé** : environ 56 % de la fenêtre avant l'ouverture du clavier, au-delà des 35 % ; le panneau se plie à l'ouverture du clavier.
7. **Zone épinglée** (fenêtre 520 dp) : Constraint Box ≈ 180 dp (34 %), Crazy Eights ≈ 162 dp (31 %), Connect the Story compact ≈ 164 dp (32 %).
8. **Ajouts mineurs** : bouton « Choisir un exercice » sur l'état vide de Progrès (règle des états vides §5.13) ; bouton « Compris » du bandeau de bascule (facultatif §5.12) ; phrase finale du texte du détail (09) pour le « texte complet ».
9. **Non produits** : les bonus (Premier lancement, Decision Point, Core Image, erreur de table, Réglages, Fin de séance).
10. **Non vérifié visuellement** : aucun navigateur n'était disponible dans cette session. Les mises en page et les largeurs de puces (surtout Constraint Box compact et Connect the Story compact) sont calculées, à contrôler à l'ouverture.

## Variantes à arbitrer

Aucune variante visuelle alternative n'a été jugée nécessaire : les deux directions n'ont pas été produites.

DÉCISION: Jour de grâce « rechargé chaque lundi » et affiché « disponible » alors que la rangée montre un jour de grâce utilisé le lundi 12 — options : (A) montrer « Jour de grâce utilisé cette semaine » dans le cas normal, (B) placer le jour utilisé la semaine précédente et allonger la série, (C) garder le brief tel quel — recommandation : A, plus cohérent avec la règle du lundi.

DÉCISION: À 200 % avec clavier sur 360 × 640 dp, il ne reste que 3 lignes de saisie au-dessus du clavier — options : (A) accepter, (B) masquer aussi le titre d'écran en barre du haut, (C) passer en saisie plein écran au-delà de 150 % — recommandation : A, à valider sur appareil par le testeur.

DÉCISION: Panneau de Connect the Story déployé hors règle des 35 % avant clavier — options : (A) exception documentée, (B) le limiter à 35 % avec défilement interne — recommandation : A, car les cinq éléments doivent rester visibles pour planifier l'écriture.
