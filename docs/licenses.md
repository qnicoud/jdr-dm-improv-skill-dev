# Licences et provenance des ressources

## Audio

| Fichier | Provenance | Licence |
|---|---|---|
| `app/src/main/res/raw/end_beep.ogg` | Son **original**, synthétisé par `tools/generate_end_beep.py` : sinusoïde de 880 Hz (harmonique à 1760 Hz très atténué), 0,45 s, attaque de 30 ms, fondu de sortie de 250 ms, encodage Vorbis mono via ffmpeg. Aucun échantillon tiers. | CC0 1.0 (domaine public) : libre de réutilisation sans condition. |

Pour régénérer : `python3 -I tools/generate_end_beep.py app/src/main/res/raw/end_beep.ogg` (ffmpeg avec libvorbis requis).

## Polices (lot L7, décision D38)

Les deux polices sont des polices variables (axe de graisse `wght`), sous **SIL Open Font License 1.1** (OFL). Elles sont embarquées dans l'APK : l'OFL autorise la redistribution avec l'application, à condition de conserver la mention de copyright et le texte de la licence.

| Police | Usage | Fichier | Source | Copyright |
|---|---|---|---|---|
| Alegreya | Titres (serif) | `app/src/main/res/font/alegreya.ttf` | dépôt Google Fonts, `ofl/alegreya` | © 2011 The Alegreya Project Authors (https://github.com/huertatipografica/Alegreya) |
| Atkinson Hyperlegible Next | Corps et interface | `app/src/main/res/font/atkinson_hyperlegible_next.ttf` | dépôt Google Fonts, `ofl/atkinsonhyperlegiblenext` | © 2020-2024 The Atkinson Hyperlegible Next Project Authors (https://github.com/googlefonts/atkinson-hyperlegible-next) |

Texte intégral de la licence : https://openfontlicense.org (fichier `OFL.txt` de chaque famille dans le dépôt Google Fonts). Il sera reproduit dans l'écran « À propos / Licences » (S12).

Repli : si une police ne se charge pas, Compose retombe sur la police système.
