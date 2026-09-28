package udb.edu.sv.dsm.udbet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

class MontoActivity : AppCompatActivity() {

    private var montoActual = 0.0
    private lateinit var modo: String
    private lateinit var txtMonto: TextView

    private val auth = FirebaseAuth.getInstance()
    private val balanceRef by lazy {
        FirebaseDatabase.getInstance().reference
            .child("users").child(auth.currentUser?.uid ?: "unknown").child("balance")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monto)

        modo = intent.getStringExtra(SaldoActivity.EXTRA_MODO) ?: SaldoActivity.MODO_DEPOSITO

        txtMonto = findViewById(R.id.txtMonto)
        findViewById<TextView>(R.id.txtMontoTitulo).text =
            if (modo == SaldoActivity.MODO_RETIRO) "Retiro" else "Depósito"

        actualizarTextoMonto()

        findViewById<View>(R.id.btnMas20).setOnClickListener { agregarMonto(20.0) }
        findViewById<View>(R.id.btnMas40).setOnClickListener { agregarMonto(40.0) }
        findViewById<View>(R.id.btnMas100).setOnClickListener { agregarMonto(100.0) }

        findViewById<MaterialButton>(R.id.btnContinuarMonto).setOnClickListener {
            procesarTransaccion()
        }

        setupBottomNavigation(findViewById<BottomNavigationView>(R.id.bottomNavigationMonto))
    }

    private fun agregarMonto(valor: Double) {
        montoActual += valor
        actualizarTextoMonto()
    }

    private fun actualizarTextoMonto() {
        txtMonto.text = String.format("$%.0f", montoActual)
    }

    private fun procesarTransaccion() {
        if (montoActual < 20.0) {
            Toast.makeText(this, "El monto mínimo es $20", Toast.LENGTH_SHORT).show()
            return
        }

        // runTransaction evita que dos operaciones simultáneas (ej. dos retiros a la
        // vez) dejen el saldo en un estado inconsistente: Firebase reintenta la
        // transacción si el valor cambió entre la lectura y la escritura.
        balanceRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val saldoActual = currentData.getValue(Double::class.java) ?: 0.0

                if (modo == SaldoActivity.MODO_RETIRO) {
                    if (saldoActual < montoActual) {
                        return Transaction.abort()
                    }
                    currentData.value = saldoActual - montoActual
                } else {
                    currentData.value = saldoActual + montoActual
                }
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (committed) {
                    startActivity(Intent(this@MontoActivity, ConfirmacionActivity::class.java))
                    finish()
                } else {
                    val mensaje = if (modo == SaldoActivity.MODO_RETIRO) {
                        "Saldo insuficiente para retirar esa cantidad"
                    } else {
                        "No se pudo completar la operación, intenta de nuevo"
                    }
                    Toast.makeText(this@MontoActivity, mensaje, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}