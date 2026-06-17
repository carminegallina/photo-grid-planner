# Photo Grid Planner

App Android nativa per pianificare una griglia Instagram fotografica, con griglia a 3 colonne, import multiplo tramite Android Photo Picker, riordino drag & drop, nascondi/mostra post, preview fullscreen, salvataggio locale DataStore, export testuale, collegamento opzionale Instagram e sezione Grid Cutter.

Non usa backend e non modifica il profilo Instagram reale.

## Stack

- Kotlin con Android Gradle Plugin 9 e Kotlin integrato.
- Jetpack Compose + Material 3.
- DataStore Preferences per salvataggio locale automatico.
- Android Photo Picker per importare immagini dalla galleria.
- Min SDK 26, target/compile SDK 36.

## Struttura file

```text
.
|-- settings.gradle.kts
|-- build.gradle.kts
|-- gradle.properties
|-- gradlew
|-- gradlew.bat
|-- docs/
|   |-- .well-known/
|   |   `-- assetlinks.json
|   |-- data-deletion.html
|   |-- deauthorize.html
|   |-- instagram-auth.html
|   `-- privacy.html
|-- gradle/
|   |-- libs.versions.toml
|   `-- wrapper/
|       |-- gradle-wrapper.jar
|       `-- gradle-wrapper.properties
`-- app/
    |-- build.gradle.kts
    `-- src/main/
        |-- AndroidManifest.xml
        |-- java/com/photogridplanner/
        |   |-- MainActivity.kt
        |   |-- cutter/
        |   |   |-- GridCutterModels.kt
        |   |   `-- MosaicCutter.kt
        |   |-- data/
        |   |   |-- GridModels.kt
        |   |   `-- PlannerRepository.kt
        |   |-- image/
        |   |   `-- ImageLoader.kt
        |   |-- ui/
        |   |   |-- PhotoGridPlannerApp.kt
        |   |   |-- components/
        |   |   |-- cutter/
        |   |   |-- grid/
        |   |   |-- settings/
        |   |   `-- theme/
        |   `-- viewmodel/
        |       `-- PlannerViewModel.kt
        `-- res/
            |-- drawable/
            |-- mipmap-anydpi-v26/
            `-- values/
```

## Aprire in Android Studio

1. Apri Android Studio.
2. Seleziona `Open`.
3. Scegli questa cartella: `C:\Users\carmy\Documents\app`.
4. Attendi il Gradle Sync.
5. Se Android Studio chiede di installare SDK, Build Tools o JDK, accetta. Il progetto richiede JDK 17.

Qui nel terminale di sistema risulta disponibile Java 8. Per compilare da riga di comando imposta `JAVA_HOME` su un JDK 17, oppure usa il JDK integrato di Android Studio.

## Collegare il telefono via USB

1. Sul telefono apri `Impostazioni > Info sul telefono`.
2. Tocca `Numero build` 7 volte per abilitare le Opzioni sviluppatore.
3. Vai in `Impostazioni > Sistema > Opzioni sviluppatore`.
4. Attiva `Debug USB`.
5. Collega il telefono via USB.
6. Accetta il popup di autorizzazione RSA sul telefono.
7. In Android Studio scegli il dispositivo dalla barra in alto e premi Run.

## Comandi Gradle utili

Da PowerShell nella cartella del progetto:

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

Su macOS/Linux:

```bash
./gradlew --version
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Se il wrapper deve scaricare Gradle alla prima esecuzione, serve connessione internet. In Android Studio questo avviene durante il Sync.

## Generare APK debug

Metodo Android Studio:

1. Apri il progetto.
2. Vai su `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
3. Al termine clicca `locate`.
4. L'APK sara in:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Metodo terminale:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Installare APK manualmente

Con ADB:

```powershell
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Oppure copia `app-debug.apk` sul telefono, aprilo dal file manager e consenti l'installazione da origini sconosciute quando Android lo chiede.

## Funzioni implementate

- Preview griglia Instagram moderna a 3 colonne.
- Preview verticale 4:5.
- Import multiplo immagini dalla galleria.
- Riordino post con long press + drag.
- Eliminazione post.
- Nascondi/mostra post dalla preview.
- Preview singolo post fullscreen.
- Salvataggio automatico locale con DataStore.
- Export ordine come testo copiabile/condivisibile.
- Reset progetto da pulsante circolare nella griglia.
- Collegamento Instagram via token Graph API per visualizzare i post del profilo.
- Riordino locale della griglia Instagram senza modificare il profilo reale.
- Salvataggio e applicazione di layout locali del profilo Instagram.
- Bottom navigation: Griglia, Cutter, Impostazioni.
- Tema scuro moderno.

## Grid Cutter

Il Cutter usa tasselli verticali `1080x1350`.

La sezione Cutter permette di:

- scegliere una foto dall'anteprima;
- impostare colonne e righe con slider da `1` a `6`;
- vedere subito le linee di taglio;
- generare mosaici personalizzati;
- salvare automaticamente in `Pictures/PhotoGridPlanner` su Android 10+ oppure nella cartella immagini dell'app come fallback.

## Collegamento Instagram

La sezione `Impostazioni > Instagram` accetta l'`Instagram App Client ID`, l'`Instagram App Secret` e mostra il redirect URI:

```text
https://carminegallina.github.io/photo-grid-planner/instagram-auth.html
```

Questo URI HTTPS va configurato nell'app Meta/Instagram. La pagina `docs/instagram-auth.html`, pubblicata con GitHub Pages, fa da ponte e riapre l'app Android tramite:

```text
photogridplanner://instagram-auth
```

L'app supporta anche Android App Links per aprire direttamente l'URL HTTPS nell'app. Il file di verifica principale e pubblicato nel repository root `carminegallina.github.io`:

```text
https://carminegallina.github.io/.well-known/assetlinks.json
```

Il pulsante `Accedi con Instagram` apre il login ufficiale nel browser. Instagram restituisce un codice OAuth, poi l'app lo scambia con un access token usando Client ID e App Secret salvati solo sul telefono.

La sincronizzazione scarica i post disponibili tramite API ufficiale e li mostra nella schermata Griglia. Il riordino resta locale: Instagram non permette di cambiare l'ordine reale dei post pubblicati. Il pulsante reset nella Griglia riporta l'anteprima all'ordine originale del profilo, mentre il pulsante salva memorizza layout alternativi sul telefono.

Per un'app pubblica sarebbe meglio spostare lo scambio del codice OAuth su un backend sicuro. In questa versione personale di test, l'App Secret viene inserito manualmente nell'app e resta salvato localmente sul telefono.

## URL Meta utili

```text
Redirect OAuth:
https://carminegallina.github.io/photo-grid-planner/instagram-auth.html

Privacy Policy:
https://carminegallina.github.io/photo-grid-planner/privacy.html

Callback rimozione autorizzazione:
https://carminegallina.github.io/photo-grid-planner/deauthorize.html

Richiesta eliminazione dati:
https://carminegallina.github.io/photo-grid-planner/data-deletion.html
```
