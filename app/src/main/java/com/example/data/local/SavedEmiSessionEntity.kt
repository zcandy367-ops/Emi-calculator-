package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_emi_sessions")
data class SavedEmiSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val presetName: String,
    val currencyCode: String,
    val principal: Double,
    val annualRatePercent: Double,
    val tenureValue: Int,
    val tenureUnitName: String,
    val extraPayment: Double,
    val prepaymentFrequencyName: String,
    val monthlyEmi: Double,
    val totalInterest: Double,
    val totalPayable: Double,
    val interestSaved: Double,
    val monthsSaved: Int,
    val createdAtMillis: Long = System.currentTimeMillis()
)
