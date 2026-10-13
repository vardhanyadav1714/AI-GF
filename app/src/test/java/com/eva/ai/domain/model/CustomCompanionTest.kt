package com.eva.ai.domain.model

import org.junit.Assert.*
import org.junit.Test

class CustomCompanionTest {
    @Test fun keepsPrivateIdentityAndPortraitSeparate() {
        val character = CustomCompanion(id = "custom_507f1f77bcf86cd799439011", name = "Asha", avatarId = "mira",
            age = 28, traits = listOf("Calm", "Creative"), backstory = "Enjoys books")
        val profile = character.profile()
        assertEquals(character.id, profile.id)
        assertEquals("Asha", profile.name)
        assertEquals(companionById("mira").imageRes, profile.imageRes)
        assertEquals("28", profile.age)
        assertEquals("Enjoys books", profile.about)
        assertEquals("Mira", companionById("mira").name)
    }
}
