package com.example

data class FormTemplate(
    val id: String,
    val name: String,
    val valuesJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
