# Licences et provenance des ressources

## Audio

| Fichier | Provenance | Licence |
|---|---|---|
| `app/src/main/res/raw/end_beep.ogg` | Son **original**, synthétisé par `tools/generate_end_beep.py` : sinusoïde de 880 Hz (harmonique à 1760 Hz très atténué), 0,45 s, attaque de 30 ms, fondu de sortie de 250 ms, encodage Vorbis mono via ffmpeg. Aucun échantillon tiers. | CC0 1.0 (domaine public) : libre de réutilisation sans condition. |

Pour régénérer : `python3 -I tools/generate_end_beep.py app/src/main/res/raw/end_beep.ogg` (ffmpeg avec libvorbis requis).
