package com.paysync.gateway.data.local
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawMessage: String,
    val sender: String,
    val amount: String?,
    val phone: String?,
    val timestamp: Long,
    @ColumnInfo(name = "txn_status") val status: String,
    val response: String? = null
)
