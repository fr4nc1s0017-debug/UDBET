package udb.edu.sv.dsm.udbet.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import udb.edu.sv.dsm.udbet.GameActivity
import udb.edu.sv.dsm.udbet.R

data class CasinoGame(
    val folder: String,
    val title: String,
    val description: String,
    val coverRes: Int
)

class CasinoFragment : Fragment(R.layout.fragment_casino) {

    // Reemplaza coverRes por tus propias imágenes: agrégalas a res/drawable/
    // con estos mismos nombres (cover_tragamonedas.png, cover_blackjack.png,
    // cover_cross_road.png). Mientras no existan, usa R.drawable.bg_placeholder.
    private val games = listOf(
        CasinoGame(
            folder = "traga_monedas",
            title = "Tragamonedas",
            description = "Clásica máquina de 3 rodillos y 5 líneas de pago. Junta símbolos iguales para ganar.",
            coverRes = R.drawable.tragamoedas   // res/drawable/cover_tragamonedas.png
        ),
        CasinoGame(
            folder = "blackjack",
            title = "Blackjack",
            description = "El clásico 21: pide carta, plántate, dobla o divide contra el crupier.",
            coverRes = R.drawable.blackjack     // res/drawable/cover_blackjack.png
        ),
        CasinoGame(
            folder = "cross_road",
            title = "Cross Road",
            description = "Cruza la carretera evitando los carros: entre más avances, mayor el multiplicador.",
            coverRes = R.drawable.crossroad      // res/drawable/cover_cross_road.png
        )
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = view.findViewById<LinearLayout>(R.id.casinoGamesContainer)
        container.removeAllViews()

        games.forEach { game ->
            val card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_game_card, container, false)

            card.findViewById<ImageView>(R.id.imgGameCover).setImageResource(game.coverRes)
            card.findViewById<TextView>(R.id.txtGameTitle).text = game.title
            card.findViewById<TextView>(R.id.txtGameDescription).text = game.description

            card.setOnClickListener {
                val intent = Intent(requireContext(), GameActivity::class.java).apply {
                    putExtra(GameActivity.EXTRA_GAME_FOLDER, game.folder)
                    putExtra(GameActivity.EXTRA_GAME_TITLE, game.title)
                }
                startActivity(intent)
            }

            container.addView(card)
        }
    }
}