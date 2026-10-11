package udb.edu.sv.dsm.udbet.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import udb.edu.sv.dsm.udbet.MainActivity
import udb.edu.sv.dsm.udbet.R
import udb.edu.sv.dsm.udbet.data.BetDialogHelper
import udb.edu.sv.dsm.udbet.data.Match
import udb.edu.sv.dsm.udbet.data.MatchCardHelper
import udb.edu.sv.dsm.udbet.data.MatchRepository

class BusquedaFragment : Fragment(R.layout.fragment_busqueda) {

    private val accesosRapidos = listOf("Premier League", "La Liga", "NBA", "Casino")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editBuscar = view.findViewById<EditText>(R.id.editBuscar)
        val accesosContainer = view.findViewById<LinearLayout>(R.id.accesosRapidosContainer)
        val resultadosContainer = view.findViewById<LinearLayout>(R.id.resultadosContainer)

        mostrarAccesosRapidos(accesosContainer)

        editBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty()
                if (query.isBlank()) {
                    accesosContainer.visibility = View.VISIBLE
                    resultadosContainer.removeAllViews()
                } else {
                    accesosContainer.visibility = View.GONE
                    mostrarResultados(resultadosContainer, MatchRepository.search(query))
                }
            }
        })
    }

    private fun mostrarAccesosRapidos(container: LinearLayout) {
        container.removeAllViews()
        accesosRapidos.forEach { nombre ->
            val row = crearFilaAcceso(nombre)
            row.setOnClickListener {
                if (nombre == "Casino") {
                    (activity as? MainActivity)?.navigateToTab(R.id.nav_casino)
                } else {
                    (activity as? MainActivity)?.navigateToTab(R.id.nav_sports)
                }
            }
            container.addView(row)
        }
    }

    private fun crearFilaAcceso(nombre: String): LinearLayout {
        val row = LinearLayout(requireContext())
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setBackgroundResource(R.drawable.bg_search_bar)
        row.setPadding(dp(16), dp(14), dp(16), dp(14))

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = dp(10)
        row.layoutParams = params

        val txt = TextView(requireContext())
        txt.text = "$nombre >"
        txt.textSize = 14f
        txt.setTextColor(resources.getColor(R.color.white, null))
        row.addView(txt)

        return row
    }

    private fun mostrarResultados(container: LinearLayout, matches: List<Match>) {
        container.removeAllViews()
        if (matches.isEmpty()) {
            val txt = TextView(requireContext())
            txt.text = "Sin resultados"
            txt.setTextColor(resources.getColor(R.color.TextHint, null))
            container.addView(txt)
            return
        }
        matches.forEach { match ->
            val card = MatchCardHelper.buildCard(requireContext(), container, match) { m, outcome ->
                BetDialogHelper.showStakeDialog(requireContext(), m, outcome)
            }
            container.addView(card)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}