package com.example.virtualstorage.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FirebaseWarehouseRepository(private val context: Context) {
    val isConfigured: Boolean
        get() = FirebaseApp.getApps(context).isNotEmpty()

    fun currentUserEmail(): String? = if (isConfigured) {
        FirebaseAuth.getInstance().currentUser?.email
    } else {
        null
    }

    fun currentUserId(): String? = if (isConfigured) {
        FirebaseAuth.getInstance().currentUser?.uid
    } else {
        null
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        if (!isConfigured) {
            onResult(false, FIREBASE_NOT_CONFIGURED)
            return
        }
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult(true, "Вход выполнен") }
            .addOnFailureListener { onResult(false, it.localizedMessage ?: "Ошибка входа") }
    }

    fun register(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        if (!isConfigured) {
            onResult(false, FIREBASE_NOT_CONFIGURED)
            return
        }
        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult(true, "Аккаунт создан") }
            .addOnFailureListener { onResult(false, it.localizedMessage ?: "Ошибка регистрации") }
    }

    fun signOut() {
        if (isConfigured) FirebaseAuth.getInstance().signOut()
    }

    fun pushInventoryEvent(action: String, item: InventoryItem?, details: String) {
        val uid = currentUserId() ?: return
        val payload = mapOf(
            "action" to action,
            "itemId" to (item?.id ?: 0L),
            "itemName" to (item?.name ?: ""),
            "details" to details,
            "createdAt" to System.currentTimeMillis()
        )
        FirebaseDatabase.getInstance()
            .reference
            .child("users")
            .child(uid)
            .child("inventory_events")
            .push()
            .setValue(payload)
    }

    companion object {
        const val FIREBASE_NOT_CONFIGURED =
            "Firebase еще не настроен: добавьте google-services.json из Firebase Console."
    }
}
