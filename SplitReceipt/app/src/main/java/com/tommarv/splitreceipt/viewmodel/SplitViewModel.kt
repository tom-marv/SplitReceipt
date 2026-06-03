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

    private val _savedNames = MutableStateFlow<List<String>>(loadSavedNames())
    val savedNames: StateFlow<List<String>> = _savedNames.asStateFlow()

    private val _savedSplits = MutableStateFlow<List<SavedSplit>>(loadSavedSplits())
    val savedSplits: StateFlow<List<SavedSplit>> = _savedSplits.asStateFlow()

    private fun loadThemePreference(): Boolean = sharedPrefs.getBoolean("dark_mode", false)

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
        val newSplit = SavedSplit(
            name = name,
            place = place,
            people = _people.value,
            items = _items.value,
            discount = _discount.value
        )
        _savedSplits.update { (listOf(newSplit) + it).take(50) } // Keep last 50
        saveSavedSplits(_savedSplits.value)
    }

    fun deleteSplit(id: String) {
        _savedSplits.update { it.filterNot { split -> split.id == id } }
        saveSavedSplits(_savedSplits.value)
    }

    fun deleteAllSplits() {
        _savedSplits.value = emptyList()
        saveSavedSplits(emptyList())
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
