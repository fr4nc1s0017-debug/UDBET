package udb.edu.sv.dsm.udbet.fragment

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import udb.edu.sv.dsm.udbet.R
import udb.edu.sv.dsm.udbet.data.BetDialogHelper
import udb.edu.sv.dsm.udbet.data.Match
import udb.edu.sv.dsm.udbet.data.MatchCardHelper
import udb.edu.sv.dsm.udbet.data.MatchRepository

class BusquedaFragment : Fragment(R.layout.fragment_busqueda) {

    // Usamos los nombres exactos de tus archivos XML en drawable
    private val accesosRapidos = listOf(
        Pair("Premier League", R.drawable.premierleague),
        Pair("La Liga", R.drawable.laliga),
        Pair("NBA", R.drawable.nba)
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editBuscar = view.findViewById<EditText>(R.id.editBuscar)
        val accesosContainer = view.findViewById<LinearLayout>(R.id.accesosRapidosContainer)
        val resultadosContainer = view.findViewById<LinearLayout>(R.id.resultadosContainer)

        mostrarAccesosRapidos(accesosContainer, editBuscar)

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

    private fun mostrarAccesosRapidos(container: LinearLayout, editBuscar: EditText) {
        container.removeAllViews()

        val titulo = TextView(requireContext())
        titulo.text = "Ligas Populares"
        titulo.setTextColor(resources.getColor(R.color.white, null))
        titulo.textSize = 16f
        titulo.setTypeface(null, Typeface.BOLD)
        titulo.setPadding(0, 0, 0, dp(12))
        container.addView(titulo)

        accesosRapidos.forEach { (nombre, imagenRes) ->
            val row = crearTarjetaAcceso(nombre, imagenRes)
            row.setOnClickListener {
                editBuscar.setText(nombre)
                editBuscar.setSelection(nombre.length)
            }
            container.addView(row)
        }
    }

    private fun crearTarjetaAcceso(nombre: String, imagenRes: Int): LinearLayout {
        val row = LinearLayout(requireContext())
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(16), dp(12), dp(16), dp(12))

        val bg = GradientDrawable()
        bg.cornerRadius = dp(12).toFloat()
        bg.setColor(Color.parseColor("#252535"))
        row.background = bg

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = dp(12)
        row.layoutParams = params

        // Contenedor blanco para el logo
        val iconFrame = android.widget.FrameLayout(requireContext())
        val iconParams = LinearLayout.LayoutParams(dp(48), dp(48))
        iconParams.marginEnd = dp(16)
        iconFrame.layoutParams = iconParams

        val iconBg = GradientDrawable()
        iconBg.cornerRadius = dp(8).toFloat()
        iconBg.setColor(Color.WHITE) // Fondo blanco para que resalten
        iconFrame.background = iconBg

        // Cargamos tu Vector Asset
        val imgLogo = ImageView(requireContext())
        imgLogo.setImageResource(imagenRes)
        imgLogo.scaleType = ImageView.ScaleType.FIT_CENTER
        imgLogo.setPadding(dp(8), dp(8), dp(8), dp(8)) // Margen interno para que no se pegue a los bordes
        iconFrame.addView(imgLogo, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ))

        row.addView(iconFrame)

        val txt = TextView(requireContext())
        txt.text = nombre
        txt.textSize = 16f
        txt.setTextColor(resources.getColor(R.color.white, null))
        txt.setTypeface(null, Typeface.BOLD)
        row.addView(txt)

        return row
    }

    private fun mostrarResultados(container: LinearLayout, matches: List<Match>) {
        container.removeAllViews()
        if (matches.isEmpty()) {
            val txt = TextView(requireContext())
            txt.text = "No se encontraron partidos"
            txt.setTextColor(resources.getColor(R.color.TextHint, null))
            txt.gravity = Gravity.CENTER
            txt.setPadding(0, 32, 0, 32)
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