package udb.edu.sv.dsm.udbet.fragment

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editBuscar = view.findViewById<EditText>(R.id.editBuscarDeportes)
        val container = view.findViewById<LinearLayout>(R.id.partidosContainer)

        renderMatches(container, MatchRepository.matches)

        editBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty()
                val lista = if (query.isBlank()) MatchRepository.matches else MatchRepository.search(query)
                renderMatches(container, lista)
            }
        })
    }

    private fun renderMatches(container: LinearLayout, matches: List<Match>) {
        container.removeAllViews()

        if (matches.isEmpty()) {
            val txt = TextView(requireContext())
            txt.text = "Sin partidos para esa búsqueda"
            txt.setTextColor(resources.getColor(R.color.TextHint, null))
            container.addView(txt)
            return
        }

        val grupos = LinkedHashMap<String, MutableList<Match>>()
        matches.forEach { match ->
            val titulo = "${tituloDeporte(match.sport)} - Liga de ${tituloDeporte(match.sport).lowercase()}"
            grupos.getOrPut(titulo) { mutableListOf() }.add(match)
        }

        grupos.forEach { (titulo, lista) ->
            val header = TextView(requireContext())
            header.text = titulo
            header.setTextColor(resources.getColor(R.color.white, null))
            header.textSize = 13f
            header.setTypeface(null, Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dp(16)
            params.bottomMargin = dp(10)
            header.layoutParams = params
            container.addView(header)

            lista.forEach { match ->
                val card = MatchCardHelper.buildCard(requireContext(), container, match) { m, outcome ->
                    BetDialogHelper.showStakeDialog(requireContext(), m, outcome)
                }
                container.addView(card)
            }
        }
    }

    private fun tituloDeporte(sport: String) = when (sport) {
        "futbol" -> "Futbol"
        "basquetball" -> "Basquetball"
        else -> sport
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}