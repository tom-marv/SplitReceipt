package com.tommarv.splitreceipt.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tommarv.splitreceipt.data.Person
import com.tommarv.splitreceipt.data.ReceiptItem
import com.tommarv.splitreceipt.data.SavedSplit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

enum class AppLanguage(val code: String, val label: String) {
    ITALIAN("it", "🇮🇹 Italiano"),
    ENGLISH("en", "🇬🇧 English")
}

class SplitViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("split_receipt_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _people = MutableStateFlow<List<Person>>(emptyList())
    val people: StateFlow<List<Person>> = _people.asStateFlow()

    private val _items = MutableStateFlow<List<ReceiptItem>>(emptyList())
    val items: StateFlow<List<ReceiptItem>> = _items.asStateFlow()

    private val _discount = MutableStateFlow(0.0)
    val discount: StateFlow<Double> = _discount.asStateFlow()

    private val _isDarkMode = MutableStateFlow(loadThemePreference())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _language = MutableStateFlow(loadLanguagePreference())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _saveImagesEnabled = MutableStateFlow(loadSaveImagesPreference())
    val saveImagesEnabled: StateFlow<Boolean> = _saveImagesEnabled.asStateFlow()

    private val _lastScannedImageUri = MutableStateFlow<Uri?>(null)
    val lastScannedImageUri: StateFlow<Uri?> = _lastScannedImageUri.asStateFlow()

    private val _savedNames = MutableStateFlow<List<String>>(loadSavedNames())
    val savedNames: StateFlow<List<String>> = _savedNames.asStateFlow()

    private val _savedSplits = MutableStateFlow<List<SavedSplit>>(loadSavedSplits())
    val savedSplits: StateFlow<List<SavedSplit>> = _savedSplits.asStateFlow()

    private fun loadThemePreference(): Boolean = sharedPrefs.getBoolean("dark_mode", false)
    
    private fun loadLanguagePreference(): AppLanguage {
        val code = sharedPrefs.getString("language", AppLanguage.ITALIAN.code) ?: AppLanguage.ITALIAN.code
        return AppLanguage.values().find { it.code == code } ?: AppLanguage.ITALIAN
    }
    
    private fun loadSaveImagesPreference(): Boolean = sharedPrefs.getBoolean("save_images", false)

    private fun loadSavedSplits(): List<SavedSplit> {
        val json = sharedPrefs.getString("saved_splits", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedSplit>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveSavedSplits(splits: List<SavedSplit>) {
        val json = gson.toJson(splits)
        sharedPrefs.edit().putString("saved_splits", json).apply()
    }

    fun saveCurrentSplit(name: String, place: String) {
        var imagePath: String? = null
        if (_saveImagesEnabled.value) {
            _lastScannedImageUri.value?.let { uri ->
                imagePath = saveImageToInternalStorage(uri)
            }
        }

        val newSplit = SavedSplit(
            name = name,
            place = place,
            people = _people.value,
            items = _items.value,
            discount = _discount.value,
            receiptImagePath = imagePath
        )
        _savedSplits.update { (listOf(newSplit) + it).take(50) } // Keep last 50
        saveSavedSplits(_savedSplits.value)
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val context = getApplication<Application>().applicationContext
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            val fileName = "receipt_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.flush()
            out.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteSplit(id: String) {
        val splitToDelete = _savedSplits.value.find { it.id == id }
        splitToDelete?.receiptImagePath?.let { path ->
            File(path).delete()
        }
        
        _savedSplits.update { it.filterNot { split -> split.id == id } }
        saveSavedSplits(_savedSplits.value)
    }

    fun deleteAllSplits() {
        _savedSplits.value.forEach { split ->
            split.receiptImagePath?.let { path ->
                File(path).delete()
            }
        }
        _savedSplits.value = emptyList()
        saveSavedSplits(emptyList())
    }

    fun setLastScannedImage(uri: Uri?) {
        _lastScannedImageUri.value = uri
    }

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        sharedPrefs.edit().putString("language", language.code).apply()
    }

    fun setSaveImagesEnabled(enabled: Boolean) {
        _saveImagesEnabled.value = enabled
        sharedPrefs.edit().putBoolean("save_images", enabled).apply()
    }

    fun t(key: String): String {
        val isEn = _language.value == AppLanguage.ENGLISH
        return when (key) {
            "home" -> if (isEn) "Home" else "Home"
            "history" -> if (isEn) "History" else "Storico"
            "settings" -> if (isEn) "Settings" else "Impostazioni"
            "info" -> if (isEn) "App Info" else "Info App"
            "start_scan" -> if (isEn) "Start Scan" else "Avvia Scansione"
            "extract_ai" -> if (isEn) "Extract data with AI" else "Estrai dati con AI"
            "who_participated" -> if (isEn) "Who participated?" else "Chi ha partecipato?"
            "people" -> if (isEn) "People" else "Persone"
            "check_prices" -> if (isEn) "Check prices" else "Controlla i prezzi"
            "items" -> if (isEn) "Items" else "Voci"
            "processing" -> if (isEn) "PROCESSING" else "ELABORAZIONE"
            "split_assign" -> if (isEn) "Split and Assign" else "Dividi e Assegna"
            "select_who_pays" -> if (isEn) "Select who pays what" else "Seleziona chi paga cosa"
            "view_result" -> if (isEn) "View Result" else "Visualizza Risultato"
            "final_bills" -> if (isEn) "Final bills per person" else "Conti finali per persona"
            "history_title" -> if (isEn) "History" else "Storico Conti"
            "delete_all" -> if (isEn) "Delete All" else "Elimina Tutto"
            "no_saved_splits" -> if (isEn) "No saved splits" else "Nessun conto salvato"
            "app_description" -> if (isEn) "A modern app to manage and split bills quickly and precisely, designed to simplify your evenings." else "Un'applicazione moderna per gestire e dividere i conti in modo rapido e preciso, pensata per semplificare le tue serate."
            "developed_by" -> if (isEn) "Developed by" else "Sviluppato da"
            "app_info_title" -> if (isEn) "Information" else "Informazioni"
            "version" -> if (isEn) "Version" else "Versione"
            "copyright_by" -> if (isEn) "Copyright by" else "Copyright di"
            "all_rights_reserved" -> if (isEn) "All rights reserved" else "Tutti i diritti riservati"
            "language" -> if (isEn) "Language" else "Lingua"
            "save_scans" -> if (isEn) "Save scans" else "Salva scansioni"
            "save_scans_desc" -> if (isEn) "Save receipt images in history" else "Salva le foto degli scontrini nello storico"
            "total" -> if (isEn) "Total" else "Totale"
            "discount" -> if (isEn) "Discount" else "Sconto"
            "details" -> if (isEn) "Details" else "Dettagli"
            "restore" -> if (isEn) "Restore" else "Ripristina"
            "delete_confirm_title" -> if (isEn) "Delete this split?" else "Eliminare questo conto?"
            "delete_confirm_desc" -> if (isEn) "This action is final and cannot be undone." else "L'azione è definitiva e non può essere annullata."
            "reset_all" -> if (isEn) "Reset All?" else "Resetta Tutto?"
            "reset_desc" -> if (isEn) "All current items, participants, and assignments will be deleted. Name history will remain." else "Verranno eliminate tutte le voci, i partecipanti e le assegnazioni correnti. Lo storico nomi rimarrà salvato."
            "delete" -> if (isEn) "DELETE" else "ELIMINA"
            "cancel" -> if (isEn) "CANCEL" else "ANNULLA"
            "reset" -> if (isEn) "RESET" else "RESETTA"
            "close" -> if (isEn) "CLOSE" else "CHIUDI"
            "scanning" -> if (isEn) "Scanning..." else "Scansione..."
            "review_scan" -> if (isEn) "Review Scan" else "Revisione Scansione"
            "confirm" -> if (isEn) "CONFIRM" else "CONFERMA"
            "analyzing_receipt" -> if (isEn) "Analyzing receipt..." else "Analisi scontrino..."
            "detected_items_desc" -> if (isEn) "Detected items (tap to delete)" else "Voci rilevate (tocca per eliminare)"
            "edit_items_title" -> if (isEn) "Edit Items" else "Modifica Voci"
            "add_item" -> if (isEn) "Add Item" else "Aggiungi Voce"
            "item_name" -> if (isEn) "Item Name" else "Nome Voce"
            "price" -> if (isEn) "Price" else "Prezzo"
            "participants_title" -> if (isEn) "Participants" else "Partecipanti"
            "add_person" -> if (isEn) "Add Person" else "Aggiungi Persona"
            "person_name" -> if (isEn) "Person Name" else "Nome Persona"
            "assignment_title" -> if (isEn) "Assign Items" else "Assegna Voci"
            "report_title" -> if (isEn) "Final Report" else "Report Finale"
            "share" -> if (isEn) "Share" else "Condividi"
            "synthetic_report" -> if (isEn) "Synthetic Report" else "Report Sintetico"
            "full_report" -> if (isEn) "Full Report" else "Report Completo"
            "no_data" -> if (isEn) "No data available" else "Nessun dato presente"
            "back" -> if (isEn) "Back" else "Indietro"
            "clear_history_title" -> if (isEn) "Clear History?" else "Cancella Cronologia?"
            "clear_history_desc" -> if (isEn) "All suggested names will be deleted permanently." else "Tutti i nomi suggeriti verranno eliminati definitivamente."
            "num_people" -> if (isEn) "Num. People" else "Num. Persone"
            "generate" -> if (isEn) "Generate" else "Genera"
            "add_by_name" -> if (isEn) "Add by name" else "Aggiungi per nome"
            "suggested_drag" -> if (isEn) "SUGGESTED (Drag or tap X)" else "SUGGERITI (Trascina o premi X)"
            "current_list" -> if (isEn) "CURRENT LIST" else "LISTA CORRENTE"
            "no_name" -> if (isEn) "Untitled" else "Senza Nome"
            "place_not_specified" -> if (isEn) "Place not specified" else "Luogo non specificato"
            "synthetic_chooser" -> if (isEn) "Share synthetic report" else "Condividi sintetico"
            "full_chooser" -> if (isEn) "Share full report" else "Condividi completo"
            "prs" -> if (isEn) "prs" else "prs"
            "items_count" -> if (isEn) "items" else "voci"
            "cleaning_history" -> if (isEn) "Cleaning history" else "Pulisci cronologia"
            "add" -> if (isEn) "ADD" else "AGGIUNGI"
            "clear_all_items_title" -> if (isEn) "Clear all items?" else "Svuota tutto?"
            "clear_all_items_desc" -> if (isEn) "Are you sure you want to delete all items? This action cannot be undone." else "Sei sicuro di voler eliminare tutte le voci inserite? Questa azione non può essere annullata."
            "add_new_item" -> if (isEn) "ADD NEW ITEM" else "AGGIUNGI NUOVA PIETANZA"
            "item_name_hint" -> if (isEn) "Name (e.g. Pizza)" else "Nome (es. Pizza)"
            "items_list" -> if (isEn) "ITEMS LIST" else "ELENCO PIATTI"
            "clear_all" -> if (isEn) "CLEAR ALL" else "SVUOTA TUTTO"
            "no_participants" -> if (isEn) "No participants found" else "Nessun partecipante trovato"
            "add_friends_desc" -> if (isEn) "Add your friends in the 'People' section to start splitting the bill." else "Aggiungi i tuoi amici nella sezione 'Persone' per iniziare a dividere il conto."
            "split_all" -> if (isEn) "Split all" else "Dividi tutto"
            "remove_all" -> if (isEn) "Remove all" else "Rimuovi tutto"
            "all" -> if (isEn) "ALL" else "TUTTI"
            "share_synthetic" -> if (isEn) "Synthetic report" else "Report Sintetico"
            "share_full" -> if (isEn) "Full report" else "Report Completo"
            "person_total" -> if (isEn) "Total for person" else "Quota per persona"
            "share_quota" -> if (isEn) "Share" else "Quota"
            "save_to_log" -> if (isEn) "Save to log" else "Salva nel registro"
            "event_name" -> if (isEn) "Event Name" else "Nome Evento"
            "event_name_hint" -> if (isEn) "e.g. Dinner with friends" else "es: Cena con amici"
            "place_hint" -> if (isEn) "e.g. Mario's Pizzeria" else "es: Pizzeria da Mario"
            "save" -> if (isEn) "SAVE" else "SALVA"
            "final_summary_title" -> if (isEn) "FINAL SUMMARY" else "RIEPILOGO FINALE"
            "no_data_to_show" -> if (isEn) "No data to show" else "Nessun dato da mostrare"
            "assign_desc" -> if (isEn) "Assign receipt items to participants to see totals here." else "Assegna le voci dello scontrino ai partecipanti per vedere i totali qui."
            "total_due" -> if (isEn) "Total due" else "Totale dovuto"
            "detail" -> if (isEn) "DETAIL" else "DETTAGLIO"
            "only_total" -> if (isEn) "Total only" else "Solo Totale"
            "total_with_details" -> if (isEn) "Total with details" else "Totale con Dettagli"
            "send_total_to" -> if (isEn) "Send total to" else "Invia totale a"
            "send_detail_to" -> if (isEn) "Send detail to" else "Invia dettaglio a"
            "cost_detail" -> if (isEn) "COST DETAIL" else "DETTAGLIO COSTI"
            "divided_between" -> if (isEn) "Divided among" else "Diviso tra"
            "people_count" -> if (isEn) "people" else "persone"
            "full_share" -> if (isEn) "Full share" else "Quota intera"
            "discount_applied" -> if (isEn) "Discount applied" else "Sconto applicato"
            "divided_equally" -> if (isEn) "Divided equally" else "Suddiviso equamente"
            "theme" -> if (isEn) "Theme" else "Tema"
            "light" -> if (isEn) "Light" else "Chiaro"
            "dark" -> if (isEn) "Dark" else "Scuro"
            "photo" -> if (isEn) "Photo" else "Foto"
            else -> key
        }
    }

    fun loadSplitIntoSession(split: SavedSplit) {
        _people.value = split.people
        _items.value = split.items
        _discount.value = split.discount
    }

    fun calculateTotalForPersonInSplit(split: SavedSplit, personId: String): Double {
        val itemsTotal = split.items.sumOf { item ->
            if (item.assignedPersonIds.contains(personId)) {
                item.price / item.assignedPersonIds.size
            } else {
                0.0
            }
        }
        val peopleCount = split.people.size
        val discountPerPerson = if (peopleCount > 0) split.discount / peopleCount else 0.0
        return (itemsTotal - discountPerPerson).coerceAtLeast(0.0)
    }

    fun getItemsForPersonInSplit(split: SavedSplit, personId: String): List<Pair<ReceiptItem, Double>> {
        return split.items.filter { it.assignedPersonIds.contains(personId) }
            .map { it to (it.price / it.assignedPersonIds.size) }
    }

    fun generateFullSummaryForSplit(split: SavedSplit): String {
        if (split.people.isEmpty()) return "Nessun dato presente."

        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        val dateStr = sdf.format(java.util.Date(split.date))

        val sb = StringBuilder()
        sb.append("🧾 RIEPILOGO COMPLETO: ${split.name.uppercase()}\n")
        sb.append("📅 Data: $dateStr\n")
        if (split.place.isNotBlank()) sb.append("📍 Luogo: ${split.place}\n")
        sb.append("----------------------------\n")
        
        split.people.forEach { person ->
            val total = calculateTotalForPersonInSplit(split, person.id)
            sb.append("👤 ${person.name}: € ${String.format("%.2f", total)}\n")
            val items = getItemsForPersonInSplit(split, person.id)
            items.forEach { (item, share) ->
                sb.append("  - ${item.name}: € ${String.format("%.2f", share)}")
                if (item.assignedPersonIds.size > 1) {
                    sb.append(" (diviso ${item.assignedPersonIds.size})")
                }
                sb.append("\n")
            }
            sb.append("\n")
        }
        
        if (split.discount > 0) {
            sb.append("💰 Sconto totale: - € ${String.format("%.2f", split.discount)}\n")
        }
        sb.append("----------------------------\n")
        sb.append("Inviato tramite SplitReceipt App")
        
        return sb.toString()
    }

    fun generateSyntheticSummaryForSplit(split: SavedSplit): String {
        if (split.people.isEmpty()) return "Nessun dato presente."

        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        val dateStr = sdf.format(java.util.Date(split.date))

        val sb = StringBuilder()
        sb.append("🧾 RIEPILOGO SINTETICO: ${split.name.uppercase()}\n")
        sb.append("📅 Data: $dateStr\n")
        if (split.place.isNotBlank()) sb.append("📍 Luogo: ${split.place}\n")
        sb.append("----------------------------\n")
        
        var grandTotal = 0.0
        split.people.forEach { person ->
            val total = calculateTotalForPersonInSplit(split, person.id)
            grandTotal += total
            sb.append("👤 ${person.name}: € ${String.format("%.2f", total)}\n")
        }
        
        sb.append("----------------------------\n")
        sb.append("💰 TOTALE GENERALE: € ${String.format("%.2f", grandTotal)}\n")
        sb.append("Inviato tramite SplitReceipt App")
        
        return sb.toString()
    }

    fun toggleTheme() {
        _isDarkMode.update { !it }
        sharedPrefs.edit().putBoolean("dark_mode", _isDarkMode.value).apply()
    }

    private fun loadSavedNames(): List<String> {
        val set = sharedPrefs.getStringSet("saved_names", emptySet()) ?: emptySet()
        val orderString = sharedPrefs.getString("names_order", "") ?: ""
        if (orderString.isEmpty()) return set.toList()
        
        val orderedList = orderString.split("|").filter { it.isNotEmpty() && set.contains(it) }
        val missing = set.filterNot { orderedList.contains(it) }
        return orderedList + missing
    }

    private fun saveNames(names: List<String>) {
        sharedPrefs.edit()
            .putStringSet("saved_names", names.toSet())
            .putString("names_order", names.joinToString("|"))
            .apply()
    }

    private fun normalizeText(text: String): String {
        val lowercaseWords = setOf(
            "di", "a", "da", "in", "con", "su", "per", "tra", "fra",
            "del", "dello", "della", "dei", "degli", "delle",
            "al", "allo", "alla", "ai", "agli", "alle",
            "dal", "dallo", "dalla", "dai", "dagli", "dalle",
            "nel", "nello", "nella", "nei", "negli", "nelle",
            "sul", "sullo", "sulla", "sui", "sugli", "sulle",
            "e", "o", "ma", "se", "che", "né", "x", "con"
        )

        return text.lowercase().split(" ")
            .filter { it.isNotBlank() }
            .mapIndexed { index, word ->
                if (index == 0 || !lowercaseWords.contains(word)) {
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } else {
                    word
                }
            }.joinToString(" ")
    }

    fun addPerson(name: String) {
        val trimmedName = normalizeText(name.trim())
        if (trimmedName.isBlank()) return
        
        if (_people.value.any { it.name.equals(trimmedName, ignoreCase = true) }) return

        val newPerson = Person(name = trimmedName)
        _people.update { it + newPerson }
        
        val currentHistory = _savedNames.value.toMutableList()
        if (!currentHistory.any { it.equals(trimmedName, ignoreCase = true) }) {
            currentHistory.add(0, trimmedName)
            _savedNames.value = currentHistory
            saveNames(currentHistory)
        }
    }

    fun addMultiplePeople(count: Int) {
        val currentNames = _people.value.map { it.name.lowercase() }.toSet()
        val newPeople = mutableListOf<Person>()
        var i = 1
        var added = 0
        while (added < count) {
            val name = "Persona ${i + _people.value.size}"
            if (!currentNames.contains(name.lowercase())) {
                newPeople.add(Person(name = name))
                added++
            }
            i++
        }
        _people.update { it + newPeople }
    }

    fun removePerson(personId: String) {
        _people.update { it.filterNot { p -> p.id == personId } }
        _items.update { currentItems ->
            currentItems.map { item ->
                item.copy(assignedPersonIds = item.assignedPersonIds.filterNot { it == personId })
            }
        }
    }

    fun clearAllData() {
        _items.value = emptyList()
        _people.value = emptyList()
        _discount.value = 0.0
    }

    fun clearHistory() {
        _savedNames.value = emptyList()
        saveNames(emptyList())
    }

    fun deleteSavedName(name: String) {
        val currentNames = _savedNames.value.toMutableList()
        currentNames.remove(name)
        _savedNames.value = currentNames
        saveNames(currentNames)
    }

    fun promoteName(name: String) {
        val currentNames = _savedNames.value.toMutableList()
        val index = currentNames.indexOfFirst { it.equals(name, ignoreCase = true) }
        if (index != -1) {
            val foundName = currentNames.removeAt(index)
            currentNames.add(0, foundName)
            _savedNames.value = currentNames
            saveNames(currentNames)
        }
    }

    fun moveName(fromIndex: Int, toIndex: Int) {
        val currentNames = _savedNames.value.toMutableList()
        if (fromIndex in currentNames.indices && toIndex in currentNames.indices) {
            val item = currentNames.removeAt(fromIndex)
            currentNames.add(toIndex, item)
            _savedNames.value = currentNames
            saveNames(currentNames)
        }
    }

    fun addItem(name: String, price: Double) {
        _items.update { it + ReceiptItem(name = normalizeText(name), price = price) }
    }

    fun removeItem(itemId: String) {
        _items.update { it.filterNot { item -> item.id == itemId } }
    }

    fun updateItem(itemId: String, name: String, price: Double) {
        _items.update { currentItems ->
            currentItems.map { item ->
                if (item.id == itemId) item.copy(name = normalizeText(name), price = price) else item
            }
        }
    }

    fun toggleAssignment(itemId: String, personId: String) {
        _items.update { currentItems ->
            currentItems.map { item ->
                if (item.id == itemId) {
                    val newList = if (item.assignedPersonIds.contains(personId)) {
                        item.assignedPersonIds - personId
                    } else {
                        item.assignedPersonIds + personId
                    }
                    item.copy(assignedPersonIds = newList)
                } else {
                    item
                }
            }
        }
    }

    fun assignToAll(itemId: String) {
        val allPersonIds = _people.value.map { it.id }
        _items.update { currentItems ->
            currentItems.map { item ->
                if (item.id == itemId) {
                    val newList = if (item.assignedPersonIds.size == allPersonIds.size) {
                        emptyList()
                    } else {
                        allPersonIds
                    }
                    item.copy(assignedPersonIds = newList)
                } else {
                    item
                }
            }
        }
    }

    fun assignAllToAll() {
        val allPersonIds = _people.value.map { it.id }
        _items.update { currentItems ->
            currentItems.map { item ->
                item.copy(assignedPersonIds = allPersonIds)
            }
        }
    }

    fun clearAllAssignments() {
        _items.update { currentItems ->
            currentItems.map { item ->
                item.copy(assignedPersonIds = emptyList())
            }
        }
    }

    fun updateDiscount(amount: Double) {
        _discount.value = amount
    }

    fun calculateTotalForPerson(personId: String): Double {
        val itemsTotal = _items.value.sumOf { item ->
            if (item.assignedPersonIds.contains(personId)) {
                item.price / item.assignedPersonIds.size
            } else {
                0.0
            }
        }
        val peopleCount = _people.value.size
        val discountPerPerson = if (peopleCount > 0) _discount.value / peopleCount else 0.0
        return (itemsTotal - discountPerPerson).coerceAtLeast(0.0)
    }

    fun getDiscountPerPerson(): Double {
        val peopleCount = _people.value.size
        return if (peopleCount > 0) _discount.value / peopleCount else 0.0
    }

    fun generateFullSummary(): String {
        val peopleList = _people.value
        if (peopleList.isEmpty()) return "Nessun dato presente."

        val sb = StringBuilder()
        sb.append("🧾 RIEPILOGO COMPLETO SPLIT RECEIPT\n")
        sb.append("----------------------------\n")
        
        peopleList.forEach { person ->
            val total = calculateTotalForPerson(person.id)
            sb.append("👤 ${person.name}: € ${String.format("%.2f", total)}\n")
            val items = getItemsForPerson(person.id)
            items.forEach { (item, share) ->
                sb.append("  - ${item.name}: € ${String.format("%.2f", share)}")
                if (item.assignedPersonIds.size > 1) {
                    sb.append(" (diviso ${item.assignedPersonIds.size})")
                }
                sb.append("\n")
            }
            sb.append("\n")
        }
        
        if (_discount.value > 0) {
            sb.append("💰 Sconto totale: - € ${String.format("%.2f", _discount.value)}\n")
        }
        sb.append("----------------------------\n")
        sb.append("Inviato tramite SplitReceipt App")
        
        return sb.toString()
    }

    fun generateSyntheticSummary(): String {
        val peopleList = _people.value
        if (peopleList.isEmpty()) return "Nessun dato presente."

        val sb = StringBuilder()
        sb.append("🧾 RIEPILOGO SINTETICO SPLIT RECEIPT\n")
        sb.append("----------------------------\n")
        
        var grandTotal = 0.0
        peopleList.forEach { person ->
            val total = calculateTotalForPerson(person.id)
            grandTotal += total
            sb.append("👤 ${person.name}: € ${String.format("%.2f", total)}\n")
        }
        
        sb.append("----------------------------\n")
        sb.append("💰 TOTALE GENERALE: € ${String.format("%.2f", grandTotal)}\n")
        sb.append("Inviato tramite SplitReceipt App")
        
        return sb.toString()
    }

    fun generatePersonSyntheticSummary(personId: String): String {
        val person = _people.value.find { it.id == personId } ?: return ""
        val total = calculateTotalForPerson(personId)
        return "🧾 QUOTA PER ${person.name.uppercase()}\n💰 TOTALE: € ${String.format("%.2f", total)}\n\nInviato tramite SplitReceipt App"
    }

    fun generatePersonFullSummary(personId: String): String {
        val person = _people.value.find { it.id == personId } ?: return ""
        val total = calculateTotalForPerson(personId)
        val assignedItems = getItemsForPerson(personId)
        val discountPerPerson = getDiscountPerPerson()

        val sb = StringBuilder()
        sb.append("🧾 DETTAGLIO QUOTA: ${person.name.uppercase()}\n")
        sb.append("----------------------------\n")
        assignedItems.forEach { (item, share) ->
            sb.append("• ${item.name}: € ${String.format("%.2f", share)}\n")
        }
        if (discountPerPerson > 0) {
            sb.append("🎁 Sconto: - € ${String.format("%.2f", discountPerPerson)}\n")
        }
        sb.append("----------------------------\n")
        sb.append("💰 TOTALE: € ${String.format("%.2f", total)}\n\n")
        sb.append("Inviato tramite SplitReceipt App")
        return sb.toString()
    }

    fun getItemsForPerson(personId: String): List<Pair<ReceiptItem, Double>> {
        return _items.value.filter { it.assignedPersonIds.contains(personId) }
            .map { it to (it.price / it.assignedPersonIds.size) }
    }

    fun setScannedItemsWithReset(scannedItems: List<Pair<String, Double>>) {
        _items.value = scannedItems.map { ReceiptItem(name = normalizeText(it.first), price = it.second) }
    }

    fun clearAllItems() {
        _items.value = emptyList()
    }
}
