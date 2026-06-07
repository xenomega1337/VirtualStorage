package com.example.virtualstorage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.virtualstorage.data.Category
import com.example.virtualstorage.data.FirebaseWarehouseRepository
import com.example.virtualstorage.data.InventoryItem
import com.example.virtualstorage.data.InventoryRepository
import com.example.virtualstorage.data.InventoryState
import com.example.virtualstorage.data.InventoryTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InventoryRepository(application)
    private val firebaseRepository = FirebaseWarehouseRepository(application)
    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state

    init {
        refresh()
    }

    fun refresh() {
        _state.update {
            it.copy(
                categories = repository.getCategories(),
                items = repository.getItems(),
                history = repository.getHistory(),
                firebaseConfigured = firebaseRepository.isConfigured,
                userEmail = firebaseRepository.currentUserEmail()
            )
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(authMessage = "Введите email и пароль") }
            return
        }
        firebaseRepository.signIn(email.trim(), password) { _, message ->
            _state.update { it.copy(authMessage = message) }
            refresh()
        }
    }

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(authMessage = "Введите email и пароль") }
            return
        }
        firebaseRepository.register(email.trim(), password) { _, message ->
            _state.update { it.copy(authMessage = message) }
            refresh()
        }
    }

    fun signOut() {
        firebaseRepository.signOut()
        _state.update {
            it.copy(
                authMessage = "Вы вышли из аккаунта",
                userEmail = firebaseRepository.currentUserEmail(),
                categories = emptyList(),
                items = emptyList(),
                history = emptyList(),
                selectedCategoryId = null,
                isItemDialogOpen = false,
                isCategoryDialogOpen = false,
                editingItem = null,
                editingCategory = null
            )
        }
    }

    fun selectTab(tab: InventoryTab) {
        _state.update {
            it.copy(
                selectedTab = tab,
                isItemDialogOpen = false,
                isCategoryDialogOpen = false,
                editingItem = null,
                editingCategory = null
            )
        }
    }

    fun search(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun filterByCategory(categoryId: Long?) {
        _state.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun startItemEdit(item: InventoryItem? = null) {
        if (_state.value.userEmail == null) {
            _state.update { it.copy(authMessage = "Сначала войдите в аккаунт") }
            return
        }
        _state.update { it.copy(isItemDialogOpen = true, editingItem = item) }
    }

    fun closeItemEdit() {
        _state.update { it.copy(isItemDialogOpen = false, editingItem = null) }
    }

    fun startCategoryEdit(category: Category? = null) {
        if (_state.value.userEmail == null) {
            _state.update { it.copy(authMessage = "Сначала войдите в аккаунт") }
            return
        }
        _state.update { it.copy(isCategoryDialogOpen = true, editingCategory = category) }
    }

    fun closeCategoryEdit() {
        _state.update { it.copy(isCategoryDialogOpen = false, editingCategory = null) }
    }

    fun saveItem(
        name: String,
        description: String,
        quantity: Int,
        minQuantity: Int,
        categoryId: Long?,
        imageUri: String?,
        itemId: Long?
    ) {
        if (name.isBlank()) return
        repository.saveItem(name, description, quantity, minQuantity, categoryId, imageUri, itemId)
        closeItemEdit()
        refresh()
    }

    fun deleteItem(itemId: Long) {
        repository.deleteItem(itemId)
        refresh()
    }

    fun changeQuantity(item: InventoryItem, delta: Int) {
        repository.changeQuantity(item, delta)
        refresh()
    }

    fun saveCategory(name: String, parentId: Long?, categoryId: Long?) {
        if (name.isBlank()) return
        repository.saveCategory(name, parentId, categoryId)
        closeCategoryEdit()
        refresh()
    }

    fun deleteCategory(categoryId: Long) {
        repository.deleteCategory(categoryId)
        _state.update { it.copy(selectedCategoryId = null) }
        refresh()
    }
}
