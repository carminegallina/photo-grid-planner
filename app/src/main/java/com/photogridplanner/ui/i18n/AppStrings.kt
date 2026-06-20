package com.photogridplanner.ui.i18n

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.photogridplanner.data.AppLanguage

val LocalAppStrings = staticCompositionLocalOf { appStringsFor(AppLanguage.English) }

fun appStringsFor(language: AppLanguage): AppStrings = AppStrings(language)

class AppStrings(private val language: AppLanguage) {
    fun t(raw: String): String {
        if (language == AppLanguage.Italian) return raw
        return EnglishStrings[raw] ?: translateDynamic(raw)
    }

    private fun translateDynamic(raw: String): String {
        Regex("""^(\d+) di (\d+)$""").matchEntire(raw)?.let {
            return "${it.groupValues[1]} of ${it.groupValues[2]}"
        }
        Regex("""^(\d+) elementi$""").matchEntire(raw)?.let {
            return "${it.groupValues[1]} items"
        }
        Regex("""^(\d+) unita visive da (\d+) post$""").matchEntire(raw)?.let {
            return "${it.groupValues[1]} visual units from ${it.groupValues[2]} posts"
        }
        Regex("""^(\d+) mosaici rilevati automaticamente$""").matchEntire(raw)?.let {
            return "${it.groupValues[1]} mosaics detected automatically"
        }
        Regex("""^Riga (\d+)$""").matchEntire(raw)?.let {
            return "Row ${it.groupValues[1]}"
        }
        Regex("""^Post (\d+)$""").matchEntire(raw)?.let {
            return "Post ${it.groupValues[1]}"
        }
        Regex("""^Placeholder (\d+)$""").matchEntire(raw)?.let {
            return "Placeholder ${it.groupValues[1]}"
        }
        Regex("""^Mosaico (\d+)x(\d+) \((\d+) post\)$""").matchEntire(raw)?.let {
            return "Mosaic ${it.groupValues[1]}x${it.groupValues[2]} (${it.groupValues[3]} posts)"
        }
        Regex("""^Foto (\d+)$""").matchEntire(raw)?.let {
            return "Photo ${it.groupValues[1]}"
        }
        Regex("""^Carosello (\d+)/(\d+)$""").matchEntire(raw)?.let {
            return "Carousel ${it.groupValues[1]}/${it.groupValues[2]}"
        }
        Regex("""^(\d+) post pianificati\.$""").matchEntire(raw)?.let {
            return "${it.groupValues[1]} scheduled posts."
        }
        Regex("""^riga (\d+), colonna (\d+)$""").matchEntire(raw)?.let {
            return "row ${it.groupValues[1]}, column ${it.groupValues[2]}"
        }
        Regex("""^Luce (\d+)% - Sat (\d+)%$""").matchEntire(raw)?.let {
            return "Brightness ${it.groupValues[1]}% - Sat ${it.groupValues[2]}%"
        }
        Regex("""^Temperatura (.+)$""").matchEntire(raw)?.let {
            return "Temperature ${t(it.groupValues[1]).lowercase()}"
        }
        Regex("""^(.+) del campione$""").matchEntire(raw)?.let {
            return "${it.groupValues[1]} of the sample"
        }
        Regex("""^Ultimi (\d+)$""").matchEntire(raw)?.let {
            return "Last ${it.groupValues[1]}"
        }
        Regex("""^Importa (\d+)$""").matchEntire(raw)?.let {
            return "Import ${it.groupValues[1]}"
        }
        Regex("""^Importa (\d+) immagini$""").matchEntire(raw)?.let {
            return "Import ${it.groupValues[1]} images"
        }
        Regex("""^Selezionate (\d+)/(\d+)$""").matchEntire(raw)?.let {
            return "Selected ${it.groupValues[1]}/${it.groupValues[2]}"
        }
        Regex("""^(\d+) foto$""").matchEntire(raw)?.let {
            return "${it.groupValues[1]} photos"
        }
        Regex("""^Layout (\d+)$""").matchEntire(raw)?.let {
            return "Layout ${it.groupValues[1]}"
        }
        Regex("""^Pianificati (\d+)$""").matchEntire(raw)?.let {
            return "Scheduled ${it.groupValues[1]}"
        }
        Regex("""^Selezionati (\d+)$""").matchEntire(raw)?.let {
            return "Selected ${it.groupValues[1]}"
        }
        Regex("""^(\d+) file$""").matchEntire(raw)?.let {
            return "${it.groupValues[1]} files"
        }
        Regex("""^Suggerito (.+)$""").matchEntire(raw)?.let {
            return "Suggested ${it.groupValues[1]}"
        }
        Regex("""^Ora impostata: (.+)$""").matchEntire(raw)?.let {
            return "Set time: ${it.groupValues[1]}"
        }
        Regex("""^Orario consigliato: (.+)$""").matchEntire(raw)?.let {
            return "Suggested time: ${it.groupValues[1]}"
        }
        Regex("""^ZIP esportato: (\d+) immagini$""").matchEntire(raw)?.let {
            return "ZIP exported: ${it.groupValues[1]} images"
        }
        Regex("""^Cartella esportata: (\d+) immagini$""").matchEntire(raw)?.let {
            return "Folder exported: ${it.groupValues[1]} images"
        }
        Regex("""^ZIP giornata esportato: (\d+) immagini$""").matchEntire(raw)?.let {
            return "Day ZIP exported: ${it.groupValues[1]} images"
        }
        Regex("""^Cartella giornata esportata: (\d+) immagini$""").matchEntire(raw)?.let {
            return "Day folder exported: ${it.groupValues[1]} images"
        }
        Regex("""^Creati (\d+) tasselli\.$""").matchEntire(raw)?.let {
            return "Created ${it.groupValues[1]} tiles."
        }
        Regex("""^Create (\d+) slide 4:5\.$""").matchEntire(raw)?.let {
            return "Created ${it.groupValues[1]} 4:5 slides."
        }
        Regex("""^Create (\d+) slide template\.$""").matchEntire(raw)?.let {
            return "Created ${it.groupValues[1]} template slides."
        }
        Regex("""^Orario consigliato: (.+)$""").matchEntire(raw)?.let {
            return "Suggested time: ${it.groupValues[1]}"
        }
        Regex("""^Luminosita (\d+)%$""").matchEntire(raw)?.let {
            return "Brightness ${it.groupValues[1]}%"
        }
        Regex("""^Saturazione (\d+)%$""").matchEntire(raw)?.let {
            return "Saturation ${it.groupValues[1]}%"
        }
        Regex("""^Sono disponibili (\d+) elementi: l'analisi usa solo quelli presenti\.$""").matchEntire(raw)?.let {
            return "${it.groupValues[1]} items are available: the analysis only uses the ones currently present."
        }
        Regex("""^(\d+) immagini non sono leggibili dal dispositivo e sono state escluse\.$""").matchEntire(raw)?.let {
            return "${it.groupValues[1]} images cannot be read by the device and were excluded."
        }
        Regex("""^Rilevati (\d+) mosaici: (\d+) post vengono valutati come immagini ricomposte\.$""").matchEntire(raw)?.let {
            return "Detected ${it.groupValues[1]} mosaics: ${it.groupValues[2]} posts are evaluated as recomposed images."
        }
        Regex("""^La (riga|riga superiore|riga inferiore|riga \d+) e molto (piu chiara|piu scura) rispetto al resto della griglia\.$""").matchEntire(raw)?.let {
            return "The ${translateRowName(it.groupValues[1])} is much ${translateBrightnessDirection(it.groupValues[2])} than the rest of the grid."
        }
        Regex("""^La colonna (sinistra|centrale|destra) risulta molto (piu chiara|piu scura) rispetto alla media\.$""").matchEntire(raw)?.let {
            return "The ${translateColumnName(it.groupValues[1])} column is much ${translateBrightnessDirection(it.groupValues[2])} than average."
        }
        Regex("""^Continua con una foto (.+) e dominanza (.+), oppure con colori analoghi\. Evita cambi di luce estremi nel post immediatamente successivo\.$""").matchEntire(raw)?.let {
            return "Continue with a ${translateBrightnessLabel(it.groupValues[1])} photo with ${translateColorFamily(it.groupValues[2])} dominance, or with analogous colors. Avoid extreme light changes in the very next post."
        }
        Regex("""^Vuoi eliminare "(.+)"\? La griglia attuale non verra modificata\.$""").matchEntire(raw)?.let {
            return "Delete \"${it.groupValues[1]}\"? The current grid will not be changed."
        }
        Regex("""^(.+) immagini$""").matchEntire(raw)?.let {
            return "${it.groupValues[1]} images"
        }
        Regex("""^Riduci il mosaico: massimo (\d+) tasselli supportati\.$""").matchEntire(raw)?.let {
            return "Reduce the mosaic: maximum ${it.groupValues[1]} supported tiles."
        }

        return raw
            .replace("Impostazioni", "Settings")
            .replace("Griglia", "Grid")
            .replace("Agenda", "Calendar")
            .replace("Analisi", "Analysis")
            .replace("Cutter", "Cutter")
            .replace("Mosaico", "Mosaic")
            .replace("Carosello", "Carousel")
            .replace("Placeholder", "Placeholder")
            .replace("Luminosita", "Brightness")
            .replace("Saturazione", "Saturation")
            .replace("Fotografia", "Photography")
            .replace("composizioni", "compositions")
            .replace("anteprima", "preview")
            .replace("Mosaici", "Mosaics")
            .replace("Bozze", "Drafts")
            .replace("Righe", "Rows")
            .replace("Colonne", "Columns")
            .replace("pianificati", "scheduled")
            .replace("pianificato", "scheduled")
            .replace("libreria", "library")
            .replace("giorno", "day")
            .replace("Avanti", "Next")
            .replace("Indietro", "Back")
            .replace("Annulla", "Cancel")
            .replace("Chiudi", "Close")
            .replace("Salva", "Save")
            .replace("Elimina", "Delete")
            .replace("Cartella", "Folder")
    }

    private fun translateRowName(value: String): String {
        return when (value) {
            "riga" -> "row"
            "riga superiore" -> "top row"
            "riga inferiore" -> "bottom row"
            else -> value.replace("riga", "row")
        }
    }

    private fun translateColumnName(value: String): String {
        return when (value) {
            "sinistra" -> "left"
            "centrale" -> "center"
            "destra" -> "right"
            else -> value
        }
    }

    private fun translateBrightnessDirection(value: String): String {
        return when (value) {
            "piu chiara" -> "lighter"
            "piu scura" -> "darker"
            else -> value
        }
    }

    private fun translateBrightnessLabel(value: String): String {
        return when (value) {
            "scuro/cupo" -> "dark/moody"
            "medio-scuro" -> "medium-dark"
            "medio" -> "medium-bright"
            "luminoso" -> "bright"
            else -> value
        }
    }

    private fun translateColorFamily(value: String): String {
        return when (value) {
            "neutra" -> "neutral"
            "rossa" -> "red"
            "arancio" -> "orange"
            "gialla" -> "yellow"
            "verde" -> "green"
            "ciano" -> "cyan"
            "blu" -> "blue"
            "viola" -> "purple"
            "magenta" -> "magenta"
            else -> value
        }
    }
}

private val EnglishStrings = mapOf(
    "Griglia" to "Grid",
    "Agenda" to "Calendar",
    "Agenda feed" to "Feed calendar",
    "Analisi" to "Analysis",
    "Cutter" to "Cutter",
    "Impost." to "Settings",
    "Impostazioni" to "Settings",
    "Italiano" to "Italian",
    "English" to "English",
    "Profilo" to "Profile",
    "Profilo verticale moderno" to "Modern vertical profile",
    "Preview" to "Preview",
    "Mostra post oscurati" to "Show hidden posts",
    "Tutorial" to "Tutorial",
    "Mostra tutorial all'avvio" to "Show tutorial at launch",
    "Apri tutorial" to "Open tutorial",
    "Privacy" to "Privacy",
    "Lingua" to "Language",
    "Scegli la lingua dell'app." to "Choose the app language.",
    "Rilevata automaticamente al primo avvio: italiano per dispositivi in italiano, inglese per tutte le altre lingue." to
        "Automatically detected on first launch: Italian for devices set to Italian, English for every other language.",
    "Export pacchetto" to "Package export",
    "Esporta ordine, manifest e immagini originali senza ricodificarle." to "Export order, manifest and original images without re-encoding them.",
    "File ZIP" to "ZIP file",
    "Cartella" to "Folder",
    "Progetto" to "Project",
    "Salvataggio locale DataStore. L'app lavora solo con foto importate dal dispositivo e placeholder." to
        "Local DataStore saving. The app only works with photos imported from the device and placeholders.",
    "Copyright" to "Copyright",
    "Photo Grid Planner (c) 2026 Carmine Gallina. Tutti i diritti riservati." to
        "Photo Grid Planner (c) 2026 Carmine Gallina. All rights reserved.",
    "Versione beta privata concessa solo per test. Vietata la redistribuzione, modifica, vendita o ripubblicazione dell'app o dell'APK senza autorizzazione." to
        "Private beta version for testing only. Redistribution, modification, sale or republishing of the app or APK without permission is prohibited.",
    "L'app richiede l'accesso alla libreria fotografica per permetterti di visualizzare, selezionare, organizzare, tagliare e pianificare le immagini nella griglia. Le foto restano sul dispositivo e vengono elaborate localmente. Nessuna immagine viene caricata online o condivisa con terze parti senza una tua azione esplicita." to
        "The app needs photo library access so you can view, select, organize, cut and plan images in the grid. Photos stay on your device and are processed locally. No image is uploaded online or shared with third parties without an explicit action from you.",

    "Guida rapida" to "Quick guide",
    "Scorri oppure usa i pulsanti" to "Swipe or use the buttons",
    "Scorri oppure usa Avanti" to "Swipe or use Next",
    "Non mostrare piu all'avvio" to "Do not show at launch",
    "Avanti" to "Next",
    "Fine" to "Done",
    "Salta" to "Skip",
    "Indietro" to "Back",
    "Componi il feed" to "Compose the feed",
    "Importa foto dalla libreria, aggiungi placeholder, riordina i post e salva layout diversi da confrontare." to
        "Import photos from the library, add placeholders, reorder posts and save different layouts to compare.",
    "Prepara post e mosaici" to "Prepare posts and mosaics",
    "Taglia mosaici, crea post 4:5, caroselli e template. Le immagini restano sempre sul dispositivo." to
        "Cut mosaics, create 4:5 posts, carousels and templates. Images always stay on your device.",
    "Bilancia colori e luce" to "Balance color and light",
    "Leggi palette, luminosita e suggerimenti estetici per scegliere cosa pubblicare dopo." to
        "Read palettes, brightness and visual suggestions to choose what to publish next.",
    "Pianifica con calma" to "Plan calmly",
    "Organizza i post nel calendario e scarica le immagini del giorno quando sei pronto a pubblicare." to
        "Organize posts in the calendar and download the day's images when you are ready to publish.",
    "Tutto rimane locale" to "Everything stays local",
    "Nessun upload, tracking o login. L'app lavora con le foto autorizzate e le elabora offline." to
        "No uploads, tracking or login. The app works with authorized photos and processes them offline.",
    "GRIGLIA" to "GRID",
    "CUTTER" to "CUTTER",
    "ANALISI" to "ANALYSIS",
    "AGENDA" to "CALENDAR",
    "PRIVACY" to "PRIVACY",

    "Elimina layout" to "Delete layout",
    "Elimina" to "Delete",
    "Annulla" to "Cancel",
    "Salva" to "Save",
    "Salva layout" to "Save layout",
    "Aggiungi placeholder" to "Add placeholder",
    "Importa immagini" to "Import images",
    "Attuale" to "Current",
    "Svuota griglia" to "Clear grid",
    "Nascosto" to "Hidden",
    "Tutte le foto e i placeholder nella griglia verranno rimossi. I layout salvati restano disponibili." to
        "All photos and placeholders in the grid will be removed. Saved layouts remain available.",
    "Svuota" to "Clear",
    "Scegli come inserirle nella griglia." to "Choose how to add them to the grid.",
    "Carosello" to "Carousel",
    "Mosaico" to "Mosaic",
    "Layout salvati" to "Saved layouts",
    "Nessun layout salvato." to "No saved layouts.",
    "Apri" to "Open",
    "Confronta" to "Compare",
    "Chiudi" to "Close",
    "Rinomina layout" to "Rename layout",
    "Nome layout" to "Layout name",
    "Placeholder" to "Placeholder",
    "Nome personalizzato" to "Custom name",
    "Tipo" to "Type",
    "Preset neutri" to "Neutral presets",
    "Confronto layout" to "Layout comparison",
    "Modifica placeholder" to "Edit placeholder",
    "Colore placeholder" to "Placeholder color",
    "Mostra nella preview" to "Show in preview",
    "Oscura dalla preview" to "Hide from preview",
    "Post" to "Post",
    "post" to "posts",
    "follower" to "followers",
    "seguiti" to "following",
    "Photo Grid Planner" to "Photo Grid Planner",
    "Fotografia, palette e composizioni in anteprima." to "Photography, palettes and compositions in preview.",
    "Modifica profilo" to "Edit profile",
    "Condividi profilo" to "Share profile",
    "Portfolio" to "Portfolio",
    "Mosaici" to "Mosaics",
    "Palette" to "Palette",
    "Bozze" to "Drafts",
    "POST" to "POSTS",
    "Tocca + per importare immagini o aggiungere placeholder" to "Tap + to import images or add placeholders",
    "Se inserisci un nome, il tipo viene deselezionato. Se scegli un tipo, il nome viene cancellato." to
        "If you enter a name, the type is deselected. If you choose a type, the custom name is cleared.",

    "Libreria foto" to "Photo library",
    "Notifiche" to "Notifications",
    "Backup progetto" to "Project backup",
    "Salva griglia, layout, calendario e impostazioni in un file locale. Le foto non vengono duplicate e restano sul dispositivo." to
        "Save the grid, layouts, calendar and settings to a local file. Photos are not duplicated and stay on your device.",
    "Crea backup" to "Create backup",
    "Ripristina backup" to "Restore backup",
    "Backup progetto creato" to "Project backup created",
    "Creazione backup non riuscita" to "Could not create the backup",
    "File di backup non valido" to "Invalid backup file",
    "Ripristinare il progetto?" to "Restore this project?",
    "La griglia, i layout e il calendario attuali verranno sostituiti dal contenuto del backup." to
        "The current grid, layouts and calendar will be replaced with the backup contents.",
    "Backup ripristinato" to "Backup restored",
    "Ripristino backup non riuscito" to "Could not restore the backup",
    "Promemoria pubblicazione" to "Publishing reminders",
    "Consenti gli allarmi esatti per ricevere il promemoria anche con l'app chiusa." to
        "Allow exact alarms to receive reminders even when the app is closed.",
    "Consenti promemoria puntuali" to "Allow precise reminders",
    "Ricevi un avviso all'orario scelto nel calendario quando ci sono post pianificati per la giornata." to
        "Receive a reminder at the time selected in the calendar when posts are planned for the day.",
    "Permesso notifiche non concesso" to "Notification permission not granted",
    "Importa nella griglia" to "Import to grid",
    "Tieni premuto e trascina per selezionare piu foto" to "Press and drag to select multiple photos",
    "Tutte le foto" to "All photos",
    "Album" to "Albums",
    "Tutti gli album" to "All albums",
    "Accesso parziale" to "Partial access",
    "Stai vedendo solo le immagini che hai autorizzato. Puoi modificarle quando vuoi." to
        "You are only seeing the images you allowed. You can change them at any time.",
    "Nessuna immagine disponibile" to "No images available",
    "Se hai concesso accesso parziale, usa Gestisci foto consentite per aggiungere altre immagini." to
        "If you granted partial access, use Manage allowed photos to add more images.",
    "Accesso alla libreria negato" to "Library access denied",
    "Accesso alla libreria richiesto" to "Library access required",
    "La libreria fotografica serve per visualizzare, selezionare, organizzare, tagliare e pianificare le immagini nella griglia. Le foto restano sul dispositivo e vengono elaborate localmente." to
        "The photo library is needed to view, select, organize, cut and plan images in the grid. Photos stay on your device and are processed locally.",
    "Gestisci foto consentite" to "Manage allowed photos",
    "Consenti accesso" to "Allow access",
    "La libreria e vuota o non contiene immagini accessibili." to "The library is empty or contains no accessible images.",
    "Accesso alle foto necessario" to "Photo access required",
    "Per importare, organizzare, tagliare e pianificare immagini nella griglia devi consentire l'accesso alla libreria fotografica. Le foto restano sul dispositivo e vengono elaborate localmente." to
        "To import, organize, cut and plan images in the grid, allow access to the photo library. Photos stay on your device and are processed locally.",

    "Cornice" to "Frame",
    "Bianca" to "White",
    "Spessore" to "Thickness",
    "Verticale 1080x1350" to "Vertical 1080x1350",
    "Post singolo" to "Single post",
    "Foto" to "Photo",
    "Template" to "Templates",
    "2 orizzontali" to "2 horizontal",
    "3 orizzontali" to "3 horizontal",
    "2 verticali" to "2 vertical",
    "3 verticali" to "3 vertical",
    "4 quadrati" to "4 squares",
    "Hero + 3" to "Hero + 3",
    "Stack + panorama" to "Stack + panorama",
    "Doppia riga" to "Double row",
    "Hero + griglia" to "Hero + grid",
    "Sfondo" to "Background",
    "Codice colore" to "Color code",
    "Colonne" to "Columns",
    "Righe" to "Rows",
    "Slide" to "Slides",
    "Cambia foto" to "Change photo",
    "Centra" to "Center",
    "Esporto..." to "Exporting...",
    "File creati" to "Created files",
    "Taglia mosaico" to "Cut mosaic",
    "Esporta post 4:5" to "Export 4:5 post",
    "Crea carosello" to "Create carousel",
    "Tocca per inserire" to "Tap to add",
    "Tocca" to "Tap",
    "per inserire" to "to add",
    "Tocca lo slot per inserire una foto" to "Tap the slot to add a photo",
    "Seleziona una foto" to "Select a photo",
    "Seleziona foto" to "Select photo",
    "Esporta mosaico" to "Export mosaic",
    "Esporta post" to "Export post",
    "Esporta carosello" to "Export carousel",
    "Esporta template" to "Export template",
    "Esportazione non riuscita." to "Export failed.",
    "Esportazione template non riuscita." to "Template export failed.",
    "Post 4:5 salvato." to "4:5 post saved.",
    "Template post salvato." to "Post template saved.",
    "Inserisci almeno una foto nel template." to "Add at least one photo to the template.",
    "Impossibile leggere l'immagine selezionata." to "Could not read the selected image.",
    "La griglia deve avere almeno una riga e una colonna." to "The grid must have at least one row and one column.",

    "Oggi" to "Today",
    "Mese" to "Month",
    "Settimana" to "Week",
    "Post da inserire" to "Posts to schedule",
    "Solo non pianificati" to "Unscheduled only",
    "Giornata" to "Day",
    "Piano giornata" to "Day plan",
    "Orario di pubblicazione" to "Publishing time",
    "Imposta ora" to "Set time",
    "Ora attuale" to "Current time",
    "Personalizzato" to "Custom",
    "Suggerito" to "Suggested",
    "Orario consigliato" to "Suggested time",
    "Cambia orario" to "Change time",
    "Usa orario consigliato" to "Use suggested time",
    "Applica" to "Apply",
    "Cambia" to "Change",
    "Aggiungi nota" to "Add note",
    "Note" to "Notes",
    "Salva piano" to "Save plan",
    "Svuota giornata" to "Clear day",
    "ZIP" to "ZIP",
    "Scegli orario" to "Choose time",
    "Conferma" to "Confirm",
    "Nessun post pianificato per questa giornata." to "No posts scheduled for this day.",
    "Nessun post pianificato per questo giorno." to "No posts scheduled for this day.",
    "1 post pianificato." to "1 scheduled post.",
    "Pianificati" to "Scheduled",
    "Selezionati" to "Selected",
    "Organizza i post e apri ogni giorno per anteprime, piano e download." to
        "Organize posts and open each day for previews, plans and downloads.",
    "Organizza i post e apri ogni giorno per anteprima ed export." to
        "Organize posts and open each day for previews and export.",
    "Importa immagini nella griglia prima di pianificare il calendario." to
        "Import images into the grid before planning the calendar.",
    "Importa immagini nella griglia prima di pianificare il feed." to
        "Import images into the grid before planning the feed.",
    "Non ci sono post senza data." to "There are no unscheduled posts.",
    "tap per selezionare" to "tap to select",
    "Tocca un giorno per aprirlo. Seleziona post e poi tocca un giorno per assegnarli." to
        "Tap a day to open it. Select posts, then tap a day to assign them.",
    "Tocca un giorno per assegnare i post selezionati." to
        "Tap a day to assign the selected posts.",
    "Tocca un giorno per aprirlo. Seleziona post sopra per pianificarli." to
        "Tap a day to open it. Select posts above to schedule them.",
    "L'anteprima usa lo stesso ordine della griglia." to
        "The preview uses the same order as the grid.",
    "L'anteprima usa lo stesso ordine della griglia, cosi i mosaici restano leggibili come nel profilo." to
        "The preview uses the same order as the grid, so mosaics remain readable as they are on the profile.",
    "Periodo precedente" to "Previous period",
    "Periodo successivo" to "Next period",
    "Rimuovi data" to "Remove date",
    "Lun" to "Mon",
    "Mar" to "Tue",
    "Mer" to "Wed",
    "Gio" to "Thu",
    "Ven" to "Fri",
    "Sab" to "Sat",
    "Dom" to "Sun",

    "Analisi Feed" to "Feed Analysis",
    "Analisi non riuscita." to "Analysis failed.",
    "Analisi non disponibile" to "Analysis unavailable",
    "Feed Score" to "Feed Score",
    "Aggiungi almeno un'immagine per ricevere suggerimenti cromatici e di luminosita." to
        "Add at least one image to receive color and brightness suggestions.",
    "Prossimo post consigliato" to "Recommended next post",
    "Transizione morbida" to "Smooth transition",
    "Cambio colore controllato" to "Controlled color shift",
    "Se vuoi cambiare atmosfera, usa prima un contenuto ponte: luce intermedia, saturazione piu calma e una tinta vicina alla palette attuale." to
        "If you want to change mood, use a bridge post first: mid brightness, calmer saturation and a hue close to the current palette.",
    "Per passare a un colore diverso senza far stonare il feed, usa complementari attenuati o split complementari. Meglio toni smorzati che colori troppo pieni." to
        "To move to a different color without clashing with the feed, use muted complementary or split-complementary tones. Softer tones work better than overly saturated colors.",
    "Luce consigliata: bassa, circa 20-38%." to "Recommended light: low, around 20-38%.",
    "Luce consigliata: medio-bassa, circa 32-50%." to "Recommended light: medium-low, around 32-50%.",
    "Luce consigliata: media, circa 45-68%." to "Recommended light: medium, around 45-68%.",
    "Luce consigliata: alta ma controllata, circa 62-82%." to "Recommended light: high but controlled, around 62-82%.",
    "Transizione: sali verso 38-55%, prima di passare a foto molto luminose." to
        "Transition: move up toward 38-55% before switching to very bright photos.",
    "Transizione: scendi verso 48-62%, prima di passare a foto molto scure." to
        "Transition: move down toward 48-62% before switching to very dark photos.",
    "Transizione: resta tra 42-65%, poi cambia gradualmente atmosfera." to
        "Transition: stay between 42-65%, then gradually change the mood.",
    "Colori compatibili" to "Compatible colors",
    "Cosa pubblicare dopo" to "What to publish next",
    "Palette colori" to "Color palette",
    "Nessun colore dominante disponibile." to "No dominant color available.",
    "Heatmap luminosita" to "Brightness heatmap",
    "Medie per riga" to "Row averages",
    "Medie per colonna" to "Column averages",
    "Colonna sinistra" to "Left column",
    "Colonna centrale" to "Center column",
    "Colonna destra" to "Right column",
    "Nessun elemento analizzabile." to "No analyzable item.",
    "Avvisi e suggerimenti" to "Alerts and suggestions",
    "Metriche per unita visiva" to "Metrics by visual unit",
    "Nessun post analizzabile." to "No analyzable post.",
    "Nessun post da analizzare" to "No posts to analyze",
    "Importa immagini nella griglia o aggiungi placeholder per calcolare le metriche del feed." to
        "Import images into the grid or add placeholders to calculate feed metrics.",
    "Analisi in corso" to "Analysis in progress",
    "Coerenza cromatica" to "Color consistency",
    "Bilanciamento luminosita" to "Brightness balance",
    "Bilanciamento saturazione" to "Saturation balance",
    "Armonia generale" to "Overall harmony",
    "Calda" to "Warm",
    "Fredda" to "Cool",
    "Neutra" to "Neutral",
    "Aggiungi immagini o placeholder alla griglia per avviare l'analisi." to
        "Add images or placeholders to the grid to start the analysis.",
    "Con meno di 3 unita visive il bilanciamento del feed e solo indicativo." to
        "With fewer than 3 visual units, the feed balance is only indicative.",
    "La palette e molto desaturata: funziona bene per feed minimal, ma puo apparire piatta." to
        "The palette is very desaturated: it works well for minimal feeds, but may look flat.",
    "La saturazione media e alta: valuta alternanza con immagini piu calme." to
        "Average saturation is high: consider alternating with calmer images.",
    "Il feed e visivamente bilanciato: non emergono squilibri forti." to
        "The feed is visually balanced: no strong imbalances stand out.",
    "photo.grid  Anteprima post del layout. Qui controlli continuita, tagli e caroselli prima di pubblicare." to
        "photo.grid  Layout post preview. Check continuity, cuts and carousels before publishing.",
    "Visualizzazione locale, nessuna connessione a Instagram." to
        "Local preview, no connection to Instagram.",
)

@Composable
fun LocalizedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    Text(
        text = LocalAppStrings.current.t(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}

@Composable
fun LocalizedText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}
