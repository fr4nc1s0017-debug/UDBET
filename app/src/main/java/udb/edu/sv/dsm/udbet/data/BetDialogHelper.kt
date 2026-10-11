package udb.edu.sv.dsm.udbet.data

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import udb.edu.sv.dsm.udbet.LiveApuestaActivity

object BetDialogHelper {

    private const val MIN_STAKE = 5.0

    fun showStakeDialog(context: Context, match: Match, outcome: Outcome) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "Debes iniciar sesión para apostar", Toast.LENGTH_SHORT).show()
            return
        }

        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(48, 24, 48, 0)

        val txtInfo = TextView(context)
        txtInfo.text = "${match.teamA} vs ${match.teamB}\n${outcome.label} @ ${outcome.odd}"
        txtInfo.setPadding(0, 0, 0, 24)
        container.addView(txtInfo)

        val editStake = EditText(context)
        editStake.hint = "Monto a apostar (mín. $${MIN_STAKE.toInt()})"
        editStake.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        container.addView(editStake)

        AlertDialog.Builder(context)
            .setTitle("Confirmar apuesta")
            .setView(container)
            .setPositiveButton("Apostar") { _, _ ->
                val stake = editStake.text.toString().toDoubleOrNull()
                if (stake == null || stake < MIN_STAKE) {
                    Toast.makeText(context, "El monto mínimo es $${MIN_STAKE.toInt()}", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                placeBet(context, uid, match, outcome, stake)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun placeBet(context: Context, uid: String, match: Match, outcome: Outcome, stake: Double) {
        val balanceRef = FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("balance")

        balanceRef.get().addOnSuccessListener { snapshot ->
            val balance = snapshot.getValue(Double::class.java) ?: 0.0
            if (balance < stake) {
                Toast.makeText(context, "Saldo insuficiente", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            balanceRef.setValue(balance - stake)

            val betsRef = FirebaseDatabase.getInstance().reference
                .child("users").child(uid).child("bets")
            val betId = betsRef.push().key ?: return@addOnSuccessListener

            val betData = mapOf(
                "matchId" to match.id,
                "teamA" to match.teamA,
                "teamB" to match.teamB,
                "league" to match.league,
                "selectionLabel" to outcome.label,
                "selectionPercent" to outcome.percent,
                "odd" to outcome.odd,
                "stake" to stake,
                "status" to "live",
                "placedAt" to System.currentTimeMillis()
            )
            betsRef.child(betId).setValue(betData)

            val intent = Intent(context, LiveApuestaActivity::class.java).apply {
                putExtra("betId", betId)
                putExtra("teamA", match.teamA)
                putExtra("teamB", match.teamB)
                putExtra("league", match.league)
                putExtra("selectionLabel", outcome.label)
                putExtra("selectionPercent", outcome.percent)
                putExtra("odd", outcome.odd)
                putExtra("stake", stake)
            }
            context.startActivity(intent)
        }
    }
}