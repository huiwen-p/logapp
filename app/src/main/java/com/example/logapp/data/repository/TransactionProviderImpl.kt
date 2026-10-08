package com.example.logapp.data.repository

import androidx.room.withTransaction
import com.example.logapp.data.local.database.TimeLensDatabase
import com.example.logapp.domain.repository.TransactionProvider

class TransactionProviderImpl(
    private val db: TimeLensDatabase
) : TransactionProvider {
    override suspend fun <T> runAsTransaction(block: suspend () -> T): T {
        return db.withTransaction {
            block()
        }
    }
}
