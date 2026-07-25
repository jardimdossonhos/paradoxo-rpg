package com.example.rpggame.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import com.example.rpggame.data.network.RpgApiService
import com.example.rpggame.data.network.RoomCreate
import com.example.rpggame.data.network.CharacterCreate

data class RoomItem(
    val id: String,
    val name: String,
    val description: String,
    val players: Int,
    val maxPlayers: Int = 4,
    val hostName: String = "Sistema",
    val adminUid: String = ""
)

data class CharacterItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val clazz: String,
    val level: Int = 1,
    val avatarUrl: String = "",
    val xp: Int = 0,
    val attributes: Map<String, Int> = emptyMap(),
    val backstory: String = ""
)

data class LoreItem(
    val id: String,
    val title: String,
    val summary: String,
    val fullText: String
)

object RpgRepository {
    private var api: RpgApiService? = null

    fun initialize(apiService: RpgApiService) {
        api = apiService
    }

    private val _lores = MutableStateFlow<List<LoreItem>>(emptyList())
    val lores: StateFlow<List<LoreItem>> = _lores.asStateFlow()

    private val _rooms = MutableStateFlow<List<RoomItem>>(emptyList())
    val rooms: StateFlow<List<RoomItem>> = _rooms.asStateFlow()

    private val _characters = MutableStateFlow<List<CharacterItem>>(emptyList())
    val characters: StateFlow<List<CharacterItem>> = _characters.asStateFlow()

    suspend fun refreshData() {
        api?.let {
            try {
                val roomsMap = it.getRooms()
                _rooms.value = roomsMap.values.toList()
                
                val charsMap = it.getCharacters()
                _characters.value = charsMap.values.toList()

                val loresMap = it.getLores()
                _lores.value = loresMap.values.toList()
            } catch (e: Exception) {
                // Silently fail for now, keep existing data or empty
            }
        }
    }

    suspend fun addRoom(name: String, description: String, maxPlayers: Int = 4): RoomItem? {
        return api?.let {
            val response = it.createRoom(RoomCreate(name, description, maxPlayers))
            refreshData()
            response.room
        }
    }

    suspend fun addCharacter(name: String, clazz: String, backstory: String, avatarUrl: String, attributes: Map<String, Int>): CharacterItem? {
        return api?.let {
            val response = it.createCharacter(CharacterCreate(name, clazz, backstory, avatarUrl, attributes))
            refreshData()
            response.character
        }
    }

    suspend fun deleteRoom(roomId: String) {
        api?.let {
            try {
                it.deleteRoom(roomId)
                refreshData()
            } catch (e: Exception) {
                // Silently fail or log
            }
        }
    }

    suspend fun addLore(title: String, summary: String, fullText: String): LoreItem? {
        return api?.let {
            val response = it.createLore(com.example.rpggame.data.network.LoreCreate(title, summary, fullText))
            refreshData()
            response.lore
        }
    }
}
