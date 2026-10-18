# Recette v1.1 — résultats (2026-10-18)

Source : section « Recette v1.1 » de l'artefact de pilotage (collection `recette_v11`), saisie par l'utilisateur sur son Galaxy Z Flip3.

**Bilan : 125 OK, 1 KO sur 126 critères.** Section 11 (passage à l'APK signé) validée : le téléphone tourne désormais avec l'APK signé par la clé de l'utilisateur.

## KO et remarques, avec la correction (branche `fix/v11-acceptance`)

| # | Remarque de l'utilisateur | Correction |
|---|---|---|
| 3.10 (KO) | « Je ne vois pas les questions de remplacement ? » | La grille était correcte mais repliée sans indication : le bouton affiche « Quelques questions (N) ». |
| 3.3 | « Oui, et… » devrait être le titre du champ ; rendu bizarre. | Libellé du champ « Oui, et… », placeholder « …ajoute quelque chose » ; `FieldSpec.prefix` retiré. |
| 3.5 | L'impulsion reste toutes les minutes quand on réduit la durée. | N étapes sur une durée D → N−1 impulsions à intervalles D/N (Crazy Eights, Oui, et…), aucune sans minuteur. |
| — | Logo du démarrage peu lisible en thème clair. | Disque brique plein derrière le d20 ; contraste ≥ 3:1 testé dans les 8 variantes. |
