package udb.edu.sv.dsm.udbet.data

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import udb.edu.sv.dsm.udbet.R

object MatchCardHelper {

    private fun colorForOutcome(label: String): Int = when (label) {
        "Win" -> Color.parseColor("#4CD964")  // Verde
        "Draw" -> Color.parseColor("#8E8E93") // Gris
        else -> Color.parseColor("#E53935")   // Rojo
    }

    // Color de fondo oscuro para los botones
    private val darkButtonBg = Color.parseColor("#2A2A35")

    fun buildCard(
        context: Context,
        parent: ViewGroup,
        match: Match,
        onOutcomeSelected: (Match, Outcome) -> Unit
    ): View {
        val card = LayoutInflater.from(context).inflate(R.layout.item_match_card, parent, false)

        // Configurar textos de cabecera
        card.findViewById<TextView>(R.id.txtMatchSchedule).text =
            "${match.league} · ${match.schedule}"

        // Configurar indicador en vivo
        val liveIndicator = card.findViewById<TextView>(R.id.txtLiveIndicator)
        if (match.isLive) {
            liveIndicator.visibility = View.VISIBLE
        } else {
            liveIndicator.visibility = View.GONE
        }

        card.findViewById<TextView>(R.id.txtTeamA).text = match.teamA
        card.findViewById<TextView>(R.id.txtTeamB).text = match.teamB

        val outcomesContainer = card.findViewById<LinearLayout>(R.id.outcomesContainer)
        outcomesContainer.removeAllViews()

        match.outcomes.forEachIndexed { index, outcome ->
            val button = buildOutcomeView(context, outcome)
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            params.marginStart = if (index == 0) 0 else dp(context, 8)
            button.layoutParams = params
            button.setOnClickListener { onOutcomeSelected(match, outcome) }
            outcomesContainer.addView(button)
        }

        return card
    }

    private fun buildOutcomeView(context: Context, outcome: Outcome): LinearLayout {
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.gravity = Gravity.CENTER

        // Fondo oscuro con esquinas redondeadas
        val bg = GradientDrawable()
        bg.cornerRadius = dp(context, 10).toFloat()
        bg.setColor(darkButtonBg)
        // Borde sutil del color del resultado
        bg.setStroke(dp(context, 1), colorForOutcome(outcome.label))

        container.background = bg
        container.setPadding(dp(context, 6), dp(context, 10), dp(context, 6), dp(context, 10))

        // Texto del porcentaje (ej. 40%)
        val txtPercent = TextView(context)
        txtPercent.text = "${outcome.percent}%"
        txtPercent.textSize = 13f
        txtPercent.setTextColor(Color.WHITE)
        txtPercent.gravity = Gravity.CENTER
        txtPercent.setTypeface(null, Typeface.BOLD)

        // Texto de la etiqueta (Win/Draw/Lose)
        val txtLabel = TextView(context)
        txtLabel.text = outcome.label
        txtLabel.textSize = 10f
        txtLabel.setTextColor(colorForOutcome(outcome.label)) // Texto de color
        txtLabel.gravity = Gravity.CENTER
        txtLabel.setTypeface(null, Typeface.BOLD)

        // Texto de la cuota (ej. 2.34)
        val txtOdd = TextView(context)
        txtOdd.text = outcome.odd.toString()
        txtOdd.textSize = 12f
        txtOdd.setTextColor(Color.WHITE)
        txtOdd.gravity = Gravity.CENTER
        txtOdd.setTypeface(null, Typeface.BOLD)

        container.addView(txtPercent)
        container.addView(txtLabel)
        container.addView(txtOdd)

        return container
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}