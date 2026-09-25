package com.naampath.colorpath3d.theme

import com.naampath.colorpath3d.model.Quality

data class GameTheme(
    val id: String,
    val displayName: String,
    val coinPrice: Int,
    val premiumOnly: Boolean,
    val background: Int,
    val fog: Int,
    val tile: Int,
    val tileEdge: Int,
    val accent: Int,
    val metal: Int
)

object Themes {
    val all: List<GameTheme> = listOf(
        theme("neon", "Neon", 0, false, 0xFF070814, 0xFF12162A, 0xFF1A2038, 0xFF3DFFF2, 0xFF3DFFF2, 0xFF8D93B5),
        theme("cyber", "Cyber", 180, false, 0xFF04060C, 0xFF101820, 0xFF142028, 0xFFFF3D8A, 0xFFFF3D8A, 0xFF9AA4B8),
        theme("ocean", "Ocean", 180, false, 0xFF041018, 0xFF0C2430, 0xFF123040, 0xFF3DE0FF, 0xFF3DE0FF, 0xFF7FB4C4),
        theme("galaxy", "Galaxy", 240, false, 0xFF0A0614, 0xFF1A1030, 0xFF241848, 0xFFB45CFF, 0xFFB45CFF, 0xFFB7A4D6),
        theme("volcano", "Volcano", 240, false, 0xFF140806, 0xFF2A120C, 0xFF3A1A10, 0xFFFF6A3D, 0xFFFF6A3D, 0xFFC4A090),
        theme("crystal", "Crystal", 300, true, 0xFF081018, 0xFF143040, 0xFF1C3C4C, 0xFF9AF7FF, 0xFF9AF7FF, 0xFFD0F4F8),
        theme("forest", "Forest", 300, true, 0xFF06110C, 0xFF102018, 0xFF163024, 0xFF3DDC97, 0xFF3DDC97, 0xFF9CC8B0),
        theme("darkmatter", "Dark Matter", 0, true, 0xFF050508, 0xFF101018, 0xFF16161E, 0xFFE8E8F2, 0xFFE8E8F2, 0xFF8A8A98)
    )

    fun byId(id: String): GameTheme = all.firstOrNull { it.id == id } ?: all.first()

    private fun theme(
        id: String,
        name: String,
        price: Int,
        premium: Boolean,
        background: Long,
        fog: Long,
        tile: Long,
        edge: Long,
        accent: Long,
        metal: Long
    ) = GameTheme(id, name, price, premium, background.toInt(), fog.toInt(), tile.toInt(), edge.toInt(), accent.toInt(), metal.toInt())
}

data class Presentation(
    val theme: GameTheme = Themes.all.first(),
    val quality: Quality = Quality.HIGH,
    val colorBlind: Boolean = false,
    val highContrast: Boolean = false,
    val reducedEffects: Boolean = false,
    val cameraRotation: Boolean = true
)
