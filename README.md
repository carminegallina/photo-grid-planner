# NiwLayr - Creator Studio

App Android nativa per creator e fotografi che vogliono **pianificare il proprio feed**: griglia in stile profilo, mosaici, caroselli, calendario editoriale e analisi estetica del feed. Tutto avviene **localmente sul dispositivo**.

- Package / applicationId: `com.niwlayr.app`
- **Nessun backend, nessuna API o login Instagram, nessun tracking o upload esterno.** Le immagini restano sul telefono e vengono elaborate in locale.
- L'organizzazione della griglia è solo nell'app: non modifica nulla su Instagram.

## Stack

- Kotlin + Jetpack Compose + Material 3.
- DataStore Preferences per il salvataggio locale automatico.
- MediaStore / Android Photo Picker per le immagini della galleria.
- Identità tipografica inclusa (Fraunces, Inter, JetBrains Mono) come font variabili in `res/font`.
- Min SDK 26, target/compile SDK 36.

## Funzioni principali

- **Griglia** in stile profilo a 3 colonne con anteprima 4:5, drag & drop, post/caroselli/mosaici e placeholder modificabili (colore e label).
- Menu del singolo post (oscura dalla preview / elimina) e menu placeholder, apribili con **tap o long-press**.
- **Import dalla galleria** e import tramite il menù **"Condividi"** di Android (immagini condivise verso l'app): singolo → post, multiple → scelta mosaico/carosello. Le foto condivise vengono copiate in locale, quindi restano disponibili offline.
- **Analisi feed**: Feed Score (coerenza cromatica, luce, saturazione, armonia) e suggerimento sul prossimo post.
- **Agenda**: calendario editoriale con orari e **notifiche di pubblicazione anche ad app chiusa**.
- **Cutter**: mosaico, post singolo e carosello, con cornice bianca opzionale ed export.
- Backup/ripristino, export ordine/ZIP, tutorial iniziale, lingue IT/EN, tema scuro.

## Build e installazione

Il progetto si apre direttamente in Android Studio (richiede JDK 17). Da riga di comando, impostare `JAVA_HOME` su un JDK 17 e `ANDROID_HOME` sull'SDK.

```powershell
.\gradlew.bat :app:assembleDebug    # APK debug
.\gradlew.bat :app:assembleRelease  # APK release (firma da local.properties)
.\gradlew.bat :app:bundleRelease    # AAB per Google Play
```

Output:

```text
APK debug:   app/build/outputs/apk/debug/app-debug.apk
APK release: app/build/outputs/apk/release/app-release.apk
AAB release: app/build/outputs/bundle/release/app-release.aab
```

Installazione via ADB:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

La firma release usa file locali non versionati (`local.properties` / keystore); nessuna credenziale è inclusa nel repository.

## Note

- `docs/` contiene pagine HTML statiche (privacy, eliminazione dati) usate per la pubblicazione su store. Non è codice né integrazione esterna.
- Le notifiche di pubblicazione usano l'allarme esatto quando disponibile e ripiegano automaticamente su un allarme inesatto se il permesso non è concesso.
