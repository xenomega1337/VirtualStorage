package com.example.virtualstorage.data

import android.content.Context

class InventoryRepository(context: Context) {
    private val dao = InventoryRoomDatabase.getInstance(context).inventoryDao()
    private val firebaseRepository = FirebaseWarehouseRepository(context.applicationContext)

    fun getCategories(): List<Category> {
        val userId = currentUserId() ?: return emptyList()
        ensureDefaultCategories(userId)
        return dao.getCategories(userId)
    }

    fun getItems(): List<InventoryItem> {
        val userId = currentUserId() ?: return emptyList()
        return dao.getItems(userId)
    }

    fun getHistory(): List<StockHistory> {
        val userId = currentUserId() ?: return emptyList()
        return dao.getHistory(userId)
    }

    fun saveCategory(name: String, parentId: Long?, categoryId: Long? = null) {
        val userId = currentUserId() ?: return
        val category = Category(
            id = categoryId ?: 0,
            userId = userId,
            name = name.trim(),
            parentId = parentId
        )
        if (categoryId == null) {
            dao.insertCategory(category)
        } else {
            dao.updateCategory(category)
        }
        firebaseRepository.pushInventoryEvent("Категория", null, "Сохранена категория: ${name.trim()}")
    }

    fun deleteCategory(categoryId: Long) {
        val userId = currentUserId() ?: return
        dao.getCategory(categoryId, userId)?.let {
            dao.deleteCategory(it)
            firebaseRepository.pushInventoryEvent("Категория", null, "Удалена категория: ${it.name}")
        }
    }

    fun saveItem(
        name: String,
        description: String,
        quantity: Int,
        minQuantity: Int,
        categoryId: Long?,
        imageUri: String?,
        itemId: Long? = null
    ) {
        val userId = currentUserId() ?: return // вот тут берем userId
        val now = System.currentTimeMillis()
        val item = InventoryItem(
            id = itemId ?: 0,
            userId = userId,
            name = name.trim(),
            description = description.trim(),
            quantity = quantity,
            minQuantity = minQuantity,
            categoryId = categoryId,
            imageUri = imageUri?.takeIf { it.isNotBlank() },
            updatedAt = now
        )
        val savedId = if (itemId == null) {
            dao.insertItem(item)
        } else {
            dao.updateItem(item)
            itemId
        } //обновляем Room
        val savedItem = item.copy(id = savedId)
        addHistory(
            userId = userId,
            itemId = savedId,
            action = "Сохранение",
            details = "Название: ${name.trim()}, количество: $quantity, минимум: $minQuantity"
        )
        firebaseRepository.pushInventoryEvent("Товар", savedItem, "Сохранен товар ${savedItem.name}") // отправляем все в FireBase
    }

    fun deleteItem(itemId: Long) {
        val userId = currentUserId() ?: return
        val item = dao.getItem(itemId, userId) ?: return
        addHistory(userId, itemId, "Удаление", "Товар удален со склада")
        dao.deleteItem(item)
        firebaseRepository.pushInventoryEvent("Удаление", item, "Удален товар ${item.name}")
    }

    fun changeQuantity(item: InventoryItem, delta: Int) {
        val userId = currentUserId() ?: return
        if (item.userId != userId) return
        val newQuantity = (item.quantity + delta).coerceAtLeast(0)
        dao.updateQuantity(item.id, userId, newQuantity, System.currentTimeMillis())
        val action = if (delta > 0) "Приход" else "Списание"
        addHistory(userId, item.id, action, "Было ${item.quantity}, стало $newQuantity")
        firebaseRepository.pushInventoryEvent(action, item.copy(quantity = newQuantity), "Количество изменено")
    }

    private fun addHistory(userId: String, itemId: Long, action: String, details: String) {
        dao.insertHistory(
            StockHistory(
                userId = userId,
                itemId = itemId,
                action = action,
                details = details,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private fun ensureDefaultCategories(userId: String) {
        if (dao.getCategories(userId).isNotEmpty()) return
        val saleId = dao.insertCategory(Category(userId = userId, name = "На продажу", parentId = null))
        dao.insertCategory(Category(userId = userId, name = "Личное", parentId = null))
        dao.insertCategory(Category(userId = userId, name = "Расходники", parentId = saleId))
    }

    private fun currentUserId(): String? = firebaseRepository.currentUserId()
}
