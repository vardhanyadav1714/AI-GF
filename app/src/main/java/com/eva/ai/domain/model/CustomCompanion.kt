package com.eva.ai.domain.model

val customCharacterTraits = listOf("Caring", "Playful", "Calm", "Witty", "Creative", "Confident", "Curious", "Supportive")

data class CustomCompanion(
    val id: String = "",
    val name: String = "",
    val age: Int = 24,
    val avatarId: String = "eva",
    val traits: List<String> = listOf("Caring"),
    val interests: String = "",
    val backstory: String = "",
    val language: String = "Automatic",
    val conversationStyle: String = "natural",
    val archived: Boolean = false
) {
    fun profile(): CompanionProfile = companionById(avatarId).copy(
        id = id, name = name, age = age.toString(), subtitle = "Your private companion",
        personality = traits.joinToString(", "), interests = interests, about = backstory,
        tags = traits, homeLine = backstory.ifBlank { "A little space for your conversations." },
        openingLine = "Hi, I'm $name. How is your day going?"
    )
}
