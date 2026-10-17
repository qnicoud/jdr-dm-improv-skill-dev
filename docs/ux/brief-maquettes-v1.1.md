# Brief maquettes v1.1

Pour le `maquettiste`. Source : `docs/ux/parcours-v1.1.md` (textes, règles, états). Référence visuelle : `docs/ux/design-system.md` et `docs/mockups/` existantes (même cadre téléphone, mêmes tokens, clair et sombre). Format cible : 412 × 900 dp ; pour les écrans avec clavier, afficher un clavier factice de ≈ 300 dp de haut (hauteur visible ≈ 400 dp).

Critère de fin : chaque maquette montre les états listés, sans couleur seule pour porter un sens, et la zone épinglée respecte le budget (minuteur + 2 lignes).

| N° | Maquette | États à montrer |
|---|---|---|
| 11 | Accueil, 10 exercices | Les 3 cartes « Nouveau » (style proche de la puce de durée, fond plus discret) ; carte « Préparation » alignée dans le même style |
| 12 | Échec qui avance | Prêt (situation + 3 types) ; En cours clavier ouvert, champ 2 actif ; Bilan avec « Ta préférée » sélectionnée |
| 13 | Règle des 3 indices | Prêt ; En cours clavier ouvert (étiquettes Objet / PNJ / Lieu) ; Bilan |
| 14 | Oui, et… | Prêt avec option « Bloquer d'abord » activée et son explication ; En cours mode normal clavier ouvert ; En cours mode bloquer (2 champs) ; Bilan avec 3e question |
| 15 | Réglages, rappels | Désactivé ; activé (heure, jours, test) ; permission refusée ; en pause (permission retirée) ; en pause automatique |
| 16 | Notification | Écran verrouillé et volet, texte du cas général, clair et sombre |
| 17 | Réglages, Données | Les quatre lignes, confirmation de sauvegarde (icône près du bouton, nom du fichier, nombre de sessions) |
| 18 | Import | A choix (bloc pliable ouvert) ; C aperçu « 42 trouvées, 3 déjà présentes » ; C′ rien à ajouter ; D résultat avec « Annuler cet import » ; E erreurs (fichier invalide, version trop récente) |
| 19 | Langue | Ligne « Langue » dans Apparence ; feuille de choix avec autonymes ; un écran en anglais (Accueil) pour vérifier les longueurs ; pseudo-localisation +40 % à 200 % |
| 20 | Détail de session | Échec (préférée marquée), Oui, et… (mode bloquer avec « Réflexe ») |

Contraintes : aucune fenêtre pendant l'exercice ; texte du clavier non masqué ; état de focus visible 2 dp ; ne pas inventer de nouveaux tokens de couleur.
