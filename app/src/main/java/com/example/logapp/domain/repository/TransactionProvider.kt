package com.example.logapp.domain.repository

interface TransactionProvider {
    suspend fun <T> runAsTransaction(block: suspend () -> T): T
}
