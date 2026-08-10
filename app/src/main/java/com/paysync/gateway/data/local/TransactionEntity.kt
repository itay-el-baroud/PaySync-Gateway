package com.paysync.gateway.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: String,
    val sender: String,
    val messageBody: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val webhookResponse: String? = null
)
