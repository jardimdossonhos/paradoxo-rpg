package com.example.rpggame.data.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.Path
import com.example.rpggame.data.RoomItem
import com.example.rpggame.data.CharacterItem
import com.example.rpggame.data.LoreItem

data class PlayerAction(
    val session_id: String,
    val player_id: String,
    val action: String
)

data class ActionResponse(
    val status: String,
    val message: String
)

data class ImageGenerationRequest(
    val session_id: String,
    val prompt_description: String,
    val aspect_ratio: String = "1:1"
)

data class ImageGenerationResponse(
    val status: String,
    val image_url: String,
    val is_mock: Boolean,
    val error: String? = null
)


data class GenericResponse(val status: String)

data class RoomCreate(val name: String, val description: String, val maxPlayers: Int = 4)
data class RoomResponse(val status: String, val room: RoomItem)

data class CharacterCreate(val name: String, val clazz: String, val backstory: String, val avatarUrl: String, val attributes: Map<String, Int>)
data class CharacterResponse(val status: String, val character: CharacterItem)

data class LoreCreate(val title: String, val summary: String, val fullText: String)
data class LoreResponse(val status: String, val lore: LoreItem)

interface RpgApiService {
    @POST("/action")
    suspend fun sendAction(@Body action: PlayerAction): ActionResponse

    @POST("/generate-image")
    suspend fun generateImage(@Body request: ImageGenerationRequest): ImageGenerationResponse

    // Rooms
    @GET("/rooms")
    suspend fun getRooms(): Map<String, RoomItem>

    @POST("/rooms")
    suspend fun createRoom(@Body room: RoomCreate): RoomResponse

    @DELETE("/rooms/{roomId}")
    suspend fun deleteRoom(@Path("roomId") roomId: String): GenericResponse

    // Characters
    @GET("/characters")
    suspend fun getCharacters(): Map<String, CharacterItem>

    @POST("/characters")
    suspend fun createCharacter(@Body character: CharacterCreate): CharacterResponse

    @DELETE("/characters/{charId}")
    suspend fun deleteCharacter(@Path("charId") charId: String): GenericResponse

    // Lores
    @GET("/lores")
    suspend fun getLores(): Map<String, LoreItem>

    @POST("/lores")
    suspend fun createLore(@Body lore: LoreCreate): LoreResponse
}

