package udb.edu.sv.dsm.udbet.data

import android.app.Activity
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

/**
 * Puente entre el JS de los juegos (tragamonedas, blackjack, cross road) y Firebase.
 * Cada juego ya trae su propio "saldo" interno (state.balance / G.balance), que
 * GameActivity sincroniza con el saldo real al cargar la página. Este puente solo
 * refleja en Firebase cada movimiento real de dinero que ocurre dentro del juego:
 *
 *  - placeBet(monto): se llama justo cuando el juego descuenta una apuesta
 *  - reportResult(gano, pago): se llama al resolver la ronda, con lo que el
 *    juego le devuelve al jugador (0 si perdió todo)
 *
 * Se usa runTransaction (igual que en MontoActivity) para evitar que dos
 * escrituras casi simultáneas dejen el saldo en un estado inconsistente.
 */
class GameBridge(
    private val activity: Activity,
    private val gameName: String
) {

    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    private val balanceRef
        get() = uid?.let {
            FirebaseDatabase.getInstance().reference.child("users").child(it).child("balance")
        }

    @JavascriptInterface
    fun placeBet(amount: Double) {
        val ref = balanceRef ?: return
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Double::class.java) ?: 0.0
                currentData.value = current - amount
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
        })
    }

    @JavascriptInterface
    fun reportResult(won: Boolean, payout: Double) {
        val currentUid = uid ?: return
        val ref = balanceRef ?: return

        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Double::class.java) ?: 0.0
                currentData.value = current + payout
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
        })

        FirebaseDatabase.getInstance().reference
            .child("users").child(currentUid).child("history").push()
            .setValue(
                mapOf(
                    "gameName" to gameName,
                    "won" to payout,
                    "lost" to 0.0,
                    "timestamp" to System.currentTimeMillis()
                )
            )

        activity.runOnUiThread {
            val msg = if (won) "Ganaste $%.2f".format(payout) else "Perdiste la ronda"
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        }
    }
}