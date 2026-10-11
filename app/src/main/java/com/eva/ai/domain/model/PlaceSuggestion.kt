package com.eva.ai.domain.model

data class PlaceSuggestion(
    val id: String,
    val label: String,
    val countryCode: String,
    val billingAdministrativeArea: String? = null
)
