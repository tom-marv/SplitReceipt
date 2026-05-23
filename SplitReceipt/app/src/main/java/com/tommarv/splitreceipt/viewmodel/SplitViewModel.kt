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

    private fun loadThemePreference(): Boolean {
        return sharedPrefs.getBoolean("dark_mode", false)
    }

    fun toggleTheme() {
        _isDarkMode.update { !it }
        sharedPrefs.edit().putBoolean("dark_mode", _isDarkMode.value).apply()
    }

    private val _savedNames = MutableStateFlow<Set<String>>(loadSavedNames())
    val savedNames: StateFlow<Set<String>> = _savedNames.asStateFlow()

    private fun loadSavedNames(): Set<String> {
        return sharedPrefs.getStringSet("saved_names", emptySet()) ?: emptySet()
    }

    private fun saveNames(names: Set<String>) {
        sharedPrefs.edit().putStringSet("saved_names", names).apply()
    }

    fun addPerson(name: String) {
        if (name.isBlank()) return
        val newPerson = Person(name = name)
        _people.update { it + newPerson }
        
        val updatedNames = _savedNames.value + name
        _savedNames.value = updatedNames
        saveNames(updatedNames)
    }

    fun addMultiplePeople(count: Int) {
        val currentSize = _people.value.size
        val newPeople = (1..count).map { i ->
            Person(name = "Persona ${currentSize + i}")
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

    fun clearPeople() {
        _people.value = emptyList()
    }

    fun deleteSavedName(name: String) {
        val updatedNames = _savedNames.value - name
        _savedNames.value = updatedNames
        saveNames(updatedNames)
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
