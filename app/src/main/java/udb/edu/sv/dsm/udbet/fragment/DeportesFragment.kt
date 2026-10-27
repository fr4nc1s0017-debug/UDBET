package udb.edu.sv.dsm.udbet.fragment

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import udb.edu.sv.dsm.udbet.R
import udb.edu.sv.dsm.udbet.data.BetDialogHelper
import udb.edu.sv.dsm.udbet.data.Match
import udb.edu.sv.dsm.udbet.data.MatchCardHelper
import udb.edu.sv.dsm.udbet.data.MatchRepository

class DeportesFragment : Fragment(R.layout.fragment_deportes) {

    private var contenedorPrincipal: LinearLayout? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Asegúrate de que este ID exista en fragment_deportes.xml
        contenedorPrincipal = view.findViewById(R.id.deportesContainer)
        val editBuscar = view.findViewById<EditText>(R.id.editBuscarDeportes)

        cargarPartidosAgrupados(MatchRepository.matches)

        // Buscador dentro de Deportes
        editBuscar?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty()
                if (query.isBlank()) {
                    cargarPartidosAgrupados(MatchRepository.matches)
                } else {
                    cargarPartidosAgrupados(MatchRepository.search(query))
                }
            }
        })
    }

    private fun cargarPartidosAgrupados(matches: List<Match>) {
        val container = contenedorPrincipal ?: return
        container.removeAllViews()

        if (matches.isEmpty()) {
            val txt = TextView(requireContext())
            txt.text = "No hay partidos disponibles"
            txt.setTextColor(resources.getColor(R.color.TextHint, null))
            txt.gravity = Gravity.CENTER
            txt.setPadding(0, 50, 0, 0)
            container.addView(txt)
            return
        }

        // Agrupamos los partidos por su propiedad 'league'
        val partidosPorLiga = matches.groupBy { it.league }

        partidosPorLiga.forEach { (liga, partidos) ->
            // 1. Agregamos el Título de la Liga
            val tituloLiga = TextView(requireContext())
            tituloLiga.text = liga
            tituloLiga.setTextColor(resources.getColor(R.color.white, null))
            tituloLiga.textSize = 16f
            tituloLiga.setTypeface(null, Typeface.BOLD)
            tituloLiga.setPadding(0, dp(20), 0, dp(10))
            container.addView(tituloLiga)

            // 2. Agregamos cada partido de esa liga
            partidos.forEach { match ->
                val card = MatchCardHelper.buildCard(requireContext(), container, match) { m, outcome ->
                    BetDialogHelper.showStakeDialog(requireContext(), m, outcome)
                }
                container.addView(card)
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}