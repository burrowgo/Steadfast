package com.example.steadfast.domain

data class Quote(
    val text: String,
    val author: String? = null,
    val type: String = "general"
)
