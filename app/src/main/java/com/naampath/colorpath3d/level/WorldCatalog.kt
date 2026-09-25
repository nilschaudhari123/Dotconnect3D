package com.naampath.colorpath3d.level

data class World(
    val id: Int,
    val name: String,
    val subtitle: String,
    val firstLevel: Int,
    val lastLevel: Int
)

object WorldCatalog {
    const val CAMPAIGN_SIZE = 1000

    val worlds: List<World> = listOf(
        World(1, "Neon Garden", "First light on the grid", 1, 100),
        World(2, "Cyber Grid", "Signals learn to turn", 101, 200),
        World(3, "Quantum Core", "Gates open one way", 201, 300),
        World(4, "Crystal Network", "Ice carries the current", 301, 400),
        World(5, "Shadow Circuit", "Barriers wait their turn", 401, 500),
        World(6, "Plasma City", "Dense neon traffic", 501, 600),
        World(7, "Void Matrix", "Portals fold the board", 601, 700),
        World(8, "Cosmic Grid", "Wide orbits, tight paths", 701, 800),
        World(9, "Infinity Network", "Every cell is a choice", 801, 900),
        World(10, "Ultimate Dimension", "The longest constellations", 901, 1000)
    )

    fun worldFor(levelId: Int): World = worlds.first { levelId in it.firstLevel..it.lastLevel }

    fun worldById(id: Int): World? = worlds.find { it.id == id }
}
