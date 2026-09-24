package com.yourhistory.app.domain.model

data class FinancialSummary(
    val totalExpense: Long = 0L,
    val totalIncome: Long = 0L,
    val balance: Long = totalIncome - totalExpense
)
