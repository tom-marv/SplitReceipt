package com.tommarv.splitreceipt.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.tommarv.splitreceipt.data.Person
import com.tommarv.splitreceipt.data.ReceiptItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SplitViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("split_receipt_prefs", Context.MODE_PRIVATE)

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

    private fun loadThemePreference(): Boolean = sharedPrefs.getBoolean("dark_mode", false)

    fun toggleTheme() {
        _isDarkMode.update { !it }
        sharedPrefs.edit().putBoolean("dark_mode", _isDarkMode.value).apply()
    }

    private fun loadSavedNames(): List<String> {
        val set = sharedPrefs.getStringSet("saved_names", emptySet()) ?: emptySet()
        // SharedPreferences stores as Unordered Set, we need a list to maintain our custom order
        // We will store the order in a separate string to persist it correctly
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

    fun addPerson(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        
        // Prevent adding duplicate people in the current session
        if (_people.value.any { it.name.equals(trimmedName, ignoreCase = true) }) return

        val newPerson = Person(name = trimmedName)
        _people.update { it + newPerson }
        
        // Save to history if new
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
        _items.update { it + ReceiptItem(name = name, price = price) }
    }

    fun removeItem(itemId: String) {
        _items.update { it.filterNot { item -> item.id == itemId } }
    }

    fun updateItem(itemId: String, name: String, price: Double) {
        _items.update { currentItems ->
            currentItems.map { item ->
                if (item.id == itemId) item.copy(name = name, price = price) else item
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

    fun getItemsForPerson(personId: String): List<Pair<ReceiptItem, Double>> {
        return _items.value.filter { it.assignedPersonIds.contains(personId) }
            .map { it to (it.price / it.assignedPersonIds.size) }
    }

    fun setScannedItems(scannedItems: List<Pair<String, Double>>) {
        _items.update { current ->
            current + scannedItems.map { ReceiptItem(name = it.first, price = it.second) }
        }
    }
}
