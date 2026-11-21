# OPTIMUS — Assistant Android

OPTIMUS est un projet d'assistant personnel Android en français. Cette première version pose les bases de l'application native.

## Fonctionnalités de démarrage
- Interface de conversation.
- Saisie de messages.
- Reconnaissance vocale Android après autorisation du microphone.
- Réponse vocale avec la synthèse vocale du téléphone.
- Commandes simples : heure, date, ouverture des paramètres et du navigateur.

## Limites actuelles
Cette version est un prototype local : elle n'est pas encore connectée à un modèle d'intelligence artificielle en ligne et ne remplace pas complètement l'assistant système du téléphone. Les fonctions sensibles nécessitent des autorisations Android explicites.

## Construire l'APK
Chaque push sur `main` lance le workflow GitHub Actions **Build Optimus APK**. À la fin du workflow, télécharge l'artefact `optimus-debug-apk` depuis l'onglet Actions.

## Identifiant Android
`com.optimus.assistant`
