package udb.edu.sv.dsm.udbet.data

data class Outcome(
    val label: String,   // "Win" | "Draw" | "Lose"
    val percent: Int,    // probabilidad estimada (ej. 45)
    val odd: Double       // cuota (ej. 1.8)
)

data class Match(
    val id: String,
    val sport: String,   // "futbol" | "basquetball"
    val league: String,
    val teamA: String,
    val teamB: String,
    val schedule: String, // "Hoy 14:00"
    val isLive: Boolean = false, // NUEVO CAMPO: Para saber si está en vivo
    val outcomes: List<Outcome> // 3 para fútbol (Win/Draw/Lose), 2 para basquetball (Win/Lose)
)

object MatchRepository {

    val matches: List<Match> = listOf(
        Match(
            id = "m1", sport = "futbol", league = "Premier League",
            teamA = "Manchester United", teamB = "Liverpool", schedule = "Hoy 14:00",
            isLive = true, // Ejemplo: Este partido está en vivo
            outcomes = listOf(
                Outcome("Win", 40, 2.34),
                Outcome("Draw", 28, 3.34),
                Outcome("Lose", 32, 2.92)
            )
        ),
        Match(
            id = "m2", sport = "futbol", league = "Premier League",
            teamA = "Arsenal", teamB = "Chelsea", schedule = "Hoy 16:30",
            outcomes = listOf(
                Outcome("Win", 45, 2.08),
                Outcome("Draw", 27, 3.46),
                Outcome("Lose", 28, 3.34)
            )
        ),
        Match(
            id = "m3", sport = "futbol", league = "La Liga",
            teamA = "Real Madrid", teamB = "Barcelona", schedule = "Mañana 13:00",
            outcomes = listOf(
                Outcome("Win", 38, 2.46),
                Outcome("Draw", 26, 3.59),
                Outcome("Lose", 36, 2.60)
            )
        ),
        Match(
            id = "m4", sport = "futbol", league = "La Liga",
            teamA = "Atletico Madrid", teamB = "Sevilla", schedule = "Mañana 15:00",
            outcomes = listOf(
                Outcome("Win", 52, 1.80),
                Outcome("Draw", 26, 3.59),
                Outcome("Lose", 22, 4.25)
            )
        ),
        Match(
            id = "m5", sport = "basquetball", league = "NBA",
            teamA = "Lakers", teamB = "Celtics", schedule = "Hoy 20:00",
            outcomes = listOf(
                Outcome("Win", 55, 1.72),
                Outcome("Lose", 45, 2.10)
            )
        ),
        Match(
            id = "m6", sport = "basquetball", league = "NBA",
            teamA = "Warriors", teamB = "Nets", schedule = "Hoy 22:30",
            outcomes = listOf(
                Outcome("Win", 62, 1.52),
                Outcome("Lose", 38, 2.48)
            )
        )
    )

    fun search(query: String): List<Match> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return matches.filter {
            it.teamA.lowercase().contains(q) ||
                    it.teamB.lowercase().contains(q) ||
                    it.league.lowercase().contains(q)
        }
    }

    fun byId(id: String): Match? = matches.find { it.id == id }
}