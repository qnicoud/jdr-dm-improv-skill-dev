# Recette 1.0.0-rc1 — résultats (2026-10-17)

Source : section « Recette rc1 » de l'artefact de pilotage (collection `recette`), saisie par l'utilisateur sur son Galaxy Z Flip3.

**Bilan : 96 OK, 4 KO sur 100 critères. Verdict : corrections nécessaires.**

## KO

| # | Critère | Remarque de l'utilisateur | Lot |
|---|---|---|---|
| 1.11 | Decision Point : bilan « Laquelle aurais-tu jouée en premier ? » | Pas de case cochable ni de sélection sur cette question. | R1a |
| 1.16 | Durée réglable sur l'écran Prêt + « sans minuteur » | Seulement dans les Réglages ; pouvoir le régler directement depuis l'exercice. | R1b |
| 7.3 | Progrès : liens « Voir mes bijoux » et par exercice | Tout OK sauf : pas de lien « Voir mes bijoux ». | R1c |
| 8.6 | Tout effacer | Petit bug visuel : la page défile un peu vers le haut et le bas pendant la saisie. | R1c |

## Remarques sur des critères OK

| # | Remarque de l'utilisateur | Lot |
|---|---|---|
| 1.5 | Constraint Box : quand un mot décoché à la main réapparaît plus loin dans le texte, le recocher. Retirer la possibilité de cocher les mots sur l'écran Prêt. | R1a |
| 1.12 | Connect the Story : idem, pas de cochage sur l'écran Prêt. | R1a |
| 1.8 | Status Numbers : la carte n'a pas l'air corrélée au type de personnage (ex. un 9 pour un pêcheur). | R1a |
| 4.4 | Rotation : réduire la consigne, le champ de saisie n'est pas visible en mode paysage. | R1b |
| 8.1 | L'icône des Réglages n'est que sur l'onglet Exercices ; la mettre aussi sur Historique et Progrès. | R1c |
| 8.5 | Export réussi : une icône de validation près du bouton plutôt qu'un texte plus bas. | R1c |
| 9.1 | Écran de démarrage : fond brique même avec un autre thème ; affiché ~0,2 s, ce qui fait bizarre : au moins une seconde. | R1c |

## Arbitrages de l'orchestrateur

- **1.5 (amende D13)** : le cochage manuel fait toujours foi, mais une **nouvelle occurrence** du mot (plus d'occurrences qu'au moment du décochage) le recoche automatiquement.
- **1.8** : par défaut, le statut tiré suit le rang social du rôle (`rank:high` → 7-10, `rank:mid` → 4-7, `rank:low` → 1-4) ; l'option « contre-stéréotype » prend la plage opposée. Le rappel « statut ≠ rang » reste affiché.
- **1.16** : réglage depuis l'écran Prêt par une feuille du bas (pas pendant l'exercice en cours, où aucune fenêtre ne s'ouvre).
- **9.1** : l'écran de démarrage prend la couleur de la palette choisie (Android 13+, `setSplashScreenTheme`), avec repli brique en dessous, et reste affiché au moins 1 s.
