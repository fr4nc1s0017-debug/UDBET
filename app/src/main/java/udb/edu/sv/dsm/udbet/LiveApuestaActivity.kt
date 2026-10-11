package udb.edu.sv.dsm.udbet

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import udb.edu.sv.dsm.udbet.data.EXTRA_SELECTED_TAB
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class LiveApuestaActivity : AppCompatActivity() {

    private lateinit var txtPartido: TextView
    private lateinit var txtSeleccion: TextView
    private lateinit var txtReloj: TextView
    private lateinit var txtValorRetiro: TextView
    private lateinit var btnRetirarse: Button
    private lateinit var btnSeguir: Button

    private val handler = Handler(Looper.getMainLooper())
    private var tick = 0
    private var resuelto = false

    private var betId = ""
    private var teamA = ""
    private var teamB = ""
    private var league = ""
    private var selectionLabel = ""
    private var selectionPercent = 0
    private var odd = 0.0
    private var stake = 0.0

    private var liveProb = 0.0
    private var secretWin = false

    companion object {
        private const val TOTAL_TICKS = 20
        private const val TICK_MS = 1200L
        private const val CASH_OUT_MARGIN = 0.08
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_apuesta)

        betId = intent.getStringExtra("betId") ?: ""
        teamA = intent.getStringExtra("teamA") ?: ""
        teamB = intent.getStringExtra("teamB") ?: ""
        league = intent.getStringExtra("league") ?: ""
        selectionLabel = intent.getStringExtra("selectionLabel") ?: ""
        selectionPercent = intent.getIntExtra("selectionPercent", 50)
        odd = intent.getDoubleExtra("odd", 1.0)
        stake = intent.getDoubleExtra("stake", 0.0)

        txtPartido = findViewById(R.id.txtPartidoVivo)
        txtSeleccion = findViewById(R.id.txtSeleccionVivo)
        txtReloj = findViewById(R.id.txtRelojVivo)
        txtValorRetiro = findViewById(R.id.txtValorRetiro)
        btnRetirarse = findViewById(R.id.btnRetirarse)
        btnSeguir = findViewById(R.id.btnSeguir)

        txtPartido.text = "$teamA vs $teamB\n$league"
        txtSeleccion.text = "Tu apuesta: $selectionLabel @ $odd  ·  $${"%.2f".format(stake)}"

        liveProb = selectionPercent / 100.0
        secretWin = Random.nextDouble() < liveProb

        btnRetirarse.setOnClickListener { if (!resuelto) cashOut() }
        btnSeguir.setOnClickListener {
            if (!resuelto) {
                Toast.makeText(this, "Sigues en juego...", Toast.LENGTH_SHORT).show()
            }
        }

        updateCashOutText()
        handler.postDelayed(::tickMatch, TICK_MS)
    }

    private fun tickMatch() {
        if (resuelto) return
        tick++

        val target = if (secretWin) 1.0 else 0.0
        val progress = tick.toDouble() / TOTAL_TICKS
        val convergence = 0.15 + 0.5 * progress
        val noiseAmplitude = 0.15 * (1 - progress)
        val noise = (Random.nextDouble() - 0.5) * 2 * noiseAmplitude

        liveProb += (target - liveProb) * convergence + noise
        liveProb = min(0.97, max(0.03, liveProb))

        val minuto = (tick.toDouble() / TOTAL_TICKS * 90).toInt()
        txtReloj.text = "Minuto $minuto'"

        updateCashOutText()

        if (tick >= TOTAL_TICKS) {
            resolverFinal()
        } else {
            handler.postDelayed(::tickMatch, TICK_MS)
        }
    }

    private fun currentCashOutValue(): Double {
        val multiplier = liveProb * odd * (1 - CASH_OUT_MARGIN)
        return stake * multiplier
    }

    private fun updateCashOutText() {
        txtValorRetiro.text = "$%.2f".format(currentCashOutValue())
    }

    private fun cashOut() {
        resuelto = true
        handler.removeCallbacksAndMessages(null)
        finalizarApuesta(currentCashOutValue(), "cashed_out")
    }

    private fun resolverFinal() {
        resuelto = true
        val payout = if (secretWin) stake * odd else 0.0
        val status = if (secretWin) "won" else "lost"
        txtReloj.text = "Minuto 90' · Final"
        finalizarApuesta(payout, status)
    }

    private fun finalizarApuesta(payout: Double, status: String) {
        btnRetirarse.isEnabled = false
        btnSeguir.isEnabled = false

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val userRef = FirebaseDatabase.getInstance().reference.child("users").child(uid)

            if (betId.isNotEmpty()) {
                userRef.child("bets").child(betId).child("status").setValue(status)
                userRef.child("bets").child(betId).child("payout").setValue(payout)
            }

            userRef.child("balance").get().addOnSuccessListener { snapshot ->
                val balanceActual = snapshot.getValue(Double::class.java) ?: 0.0
                userRef.child("balance").setValue(balanceActual + payout)
            }

            val ganado = payout
            val perdido = max(stake - payout, 0.0)
            userRef.child("history").push().setValue(
                mapOf(
                    "gameName" to "$teamA vs $teamB",
                    "won" to ganado,
                    "lost" to perdido,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }

        txtValorRetiro.text = when (status) {
            "won" -> "¡Ganaste $%.2f!".format(payout)
            "cashed_out" -> "Te retiraste con $%.2f".format(payout)
            else -> "Perdiste la apuesta"
        }

        handler.postDelayed({
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra(EXTRA_SELECTED_TAB, R.id.nav_history)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }, 2200)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}