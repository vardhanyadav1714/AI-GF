package com.eva.ai.domain.model

val customCharacterTraits = listOf("Caring", "Playful", "Calm", "Witty", "Creative", "Confident", "Curious", "Supportive")

data class CompanionPortrait(val id: String, val name: String, val imageRes: Int)
val companionPortraits = companionProfiles.map { CompanionPortrait(it.id, it.name, it.imageRes) } + listOf(
    CompanionPortrait("portrait_anaya", "Anaya", com.eva.ai.R.drawable.portrait_anaya),
    CompanionPortrait("portrait_meera", "Meera", com.eva.ai.R.drawable.portrait_meera),
    CompanionPortrait("portrait_zoya", "Zoya", com.eva.ai.R.drawable.portrait_zoya),
    CompanionPortrait("portrait_tara", "Tara", com.eva.ai.R.drawable.portrait_tara),
    CompanionPortrait("portrait_isha", "Isha", com.eva.ai.R.drawable.portrait_isha),
    CompanionPortrait("portrait_leena", "Leena", com.eva.ai.R.drawable.portrait_leena),
    CompanionPortrait("portrait_noor", "Noor", com.eva.ai.R.drawable.portrait_noor),
    CompanionPortrait("portrait_sana", "Sana", com.eva.ai.R.drawable.portrait_sana),
    CompanionPortrait("portrait_maya", "Maya", com.eva.ai.R.drawable.portrait_maya),
    CompanionPortrait("portrait_kiara", "Kiara", com.eva.ai.R.drawable.portrait_kiara),
    CompanionPortrait("portrait_aditi", "Aditi", com.eva.ai.R.drawable.portrait_aditi),
    CompanionPortrait("portrait_rhea", "Rhea", com.eva.ai.R.drawable.portrait_rhea)
)
fun companionPortraitById(id: String) = companionPortraits.firstOrNull { it.id == id } ?: companionPortraits.first()

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
        tags = traits, imageRes = companionPortraitById(avatarId).imageRes,
        homeLine = backstory.ifBlank { "A little space for your conversations." },
        openingLine = "Hi, I'm $name. How is your day going?"
    )
}
