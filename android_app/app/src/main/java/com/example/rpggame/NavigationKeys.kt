package com.example.rpggame

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Login : NavKey
@Serializable data object Rooms : NavKey
@Serializable data object Characters : NavKey
@Serializable data class CharacterDetail(val characterId: String) : NavKey
@Serializable data object LoreCampaigns : NavKey
@Serializable data class LoreDetail(val loreId: String) : NavKey
@Serializable data object Profile : NavKey
@Serializable data class GameSession(val sessionId: String) : NavKey

// Character Creation Wizard Keys
@Serializable data object CharacterWizardGenerateArt : NavKey
@Serializable data class CharacterWizardApproveArt(val prompt: String, val tempImageUrl: String) : NavKey
@Serializable data class CharacterWizardSheet(val approvedImageUrl: String) : NavKey
