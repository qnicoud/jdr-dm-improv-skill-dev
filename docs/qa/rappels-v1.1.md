# Rappels (V3) — recette manuelle

Appareil : Z Flip3. Les cases se cochent à l'essai ; les résultats sont notés ici.

- [ ] Réglages > Rappels : interrupteur désactivé par défaut, aucune demande de permission à l'ouverture.
- [ ] Premier passage à « activé » : dialogue système ; autorisé, les lignes Heure et Jours apparaissent.
- [ ] Refus : l'interrupteur revient à « désactivé », message neutre et lien vers les réglages Android.
- [ ] « Envoyer un test » : notification silencieuse (importance basse) ; un appui ouvre la séance du jour, retour mène à l'accueil.
- [ ] Rappel réel à l'heure approximative, un jour choisi.
- [ ] Aucun rappel si une session a été faite le jour même.
- [ ] Doze forcé (`adb shell dumpsys deviceidle force-idle`) : l'alarme arrive avec un retard toléré.
- [ ] Redémarrage du téléphone : l'alarme est réarmée (`adb shell dumpsys alarm | grep dmgym`).
- [ ] Mise à jour par-dessus (`installDebug`) : l'alarme est réarmée.
- [ ] Changement de fuseau : l'alarme suit l'heure murale.
- [ ] 14 jours sans session : « Rappel en pause » ; « Reprendre » relance.
- [ ] Samsung « applis en veille prolongée » : noter si l'alarme est retardée ou supprimée.
