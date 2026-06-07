package com.example.virtualstorage.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface InventoryDao {
    @Query("SELECT * FROM categories WHERE user_id = :userId ORDER BY parent_id IS NOT NULL, name")
    fun getCategories(userId: String): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategory(category: Category): Long

    @Update
    fun updateCategory(category: Category)

    @Delete
    fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories WHERE id = :categoryId AND user_id = :userId LIMIT 1")
    fun getCategory(categoryId: Long, userId: String): Category?

    @Query("SELECT * FROM items WHERE user_id = :userId ORDER BY updated_at DESC")
    fun getItems(userId: String): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItem(item: InventoryItem): Long

    @Update
    fun updateItem(item: InventoryItem)

    @Delete
    fun deleteItem(item: InventoryItem)

    @Query("SELECT * FROM items WHERE id = :itemId AND user_id = :userId LIMIT 1")
    fun getItem(itemId: Long, userId: String): InventoryItem?

    @Query("UPDATE items SET quantity = :quantity, updated_at = :updatedAt WHERE id = :itemId AND user_id = :userId")
    fun updateQuantity(itemId: Long, userId: String, quantity: Int, updatedAt: Long)

    @Insert
    fun insertHistory(history: StockHistory): Long

    @Query("SELECT * FROM stock_history WHERE user_id = :userId ORDER BY created_at DESC LIMIT 20")
    fun getHistory(userId: String): List<StockHistory>
}
