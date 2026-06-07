package com.example.virtualstorage.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val name: String,
    @ColumnInfo(name = "parent_id")
    val parentId: Long?
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("category_id"), Index("user_id")]
)
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val name: String,
    val description: String,
    val quantity: Int,
    @ColumnInfo(name = "min_quantity")
    val minQuantity: Int,
    @ColumnInfo(name = "category_id")
    val categoryId: Long?,
    @ColumnInfo(name = "image_uri")
    val imageUri: String?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    val needsRestock: Boolean
        get() = quantity <= minQuantity
}

@Entity(
    tableName = "stock_history",
    foreignKeys = [
        ForeignKey(
            entity = InventoryItem::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("item_id"), Index("user_id")]
)
data class StockHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "item_id")
    val itemId: Long,
    val action: String,
    val details: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

data class InventoryState(
    val categories: List<Category> = emptyList(),
    val items: List<InventoryItem> = emptyList(),
    val history: List<StockHistory> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val selectedTab: InventoryTab = InventoryTab.Dashboard,
    val isItemDialogOpen: Boolean = false,
    val isCategoryDialogOpen: Boolean = false,
    val editingItem: InventoryItem? = null,
    val editingCategory: Category? = null,
    val firebaseConfigured: Boolean = false,
    val userEmail: String? = null,
    val authMessage: String = ""
) {
    val filteredItems: List<InventoryItem>
        get() = items.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryId == null ||
                item.categoryId == selectedCategoryId ||
                categories.isDescendantOf(item.categoryId, selectedCategoryId)
            matchesSearch && matchesCategory
        }

    val restockItems: List<InventoryItem>
        get() = items.filter { it.needsRestock }.sortedBy { it.quantity - it.minQuantity }
}

enum class InventoryTab(val title: String) {
    Dashboard("Главная"),
    Items("Товары"),
    Categories("Категории"),
    Restock("Докупить")
}

fun List<Category>.categoryPath(categoryId: Long?): String {
    if (categoryId == null) return "Без категории"
    val byId = associateBy { it.id }
    val names = mutableListOf<String>()
    var current = byId[categoryId]
    while (current != null) {
        names.add(current.name)
        current = current.parentId?.let(byId::get)
    }
    return names.asReversed().joinToString(" / ").ifBlank { "Без категории" }
}

private fun List<Category>.isDescendantOf(categoryId: Long?, parentId: Long?): Boolean {
    if (categoryId == null || parentId == null) return false
    val byId = associateBy { it.id }
    var current = byId[categoryId]
    while (current != null) {
        if (current.parentId == parentId) return true
        current = current.parentId?.let(byId::get)
    }
    return false
}
