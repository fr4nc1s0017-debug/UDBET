package udb.edu.sv.dsm.udbet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import udb.edu.sv.dsm.udbet.data.setupBottomNavigation
import java.util.Locale

class MontoActivity : AppCompatActivity() {

    companion object {
        const val MONTO_MINIMO = 20.0
        const val EXTRA_MONTO = "extra_monto"
    }

    private lateinit var modo: String
    private lateinit var editMonto: EditText
    private lateinit var txtErrorMonto: TextView
    private lateinit var btnContinuar: MaterialButton

    private var procesando = false

    private val auth = FirebaseAuth.getInstance()
    private val balanceRef by lazy {
        FirebaseDatabase.getInstance().reference
            .child("users").child(auth.currentUser?.uid ?: "unknown").child("balance")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monto)

        modo = intent.getStringExtra(SaldoActivity.EXTRA_MODO) ?: SaldoActivity.MODO_DEPOSITO

        editMonto = findViewById(R.id.editMonto)
        txtErrorMonto = findViewById(R.id.txtErrorMonto)
        btnContinuar = findViewById(R.id.btnContinuarMonto)

        findViewById<TextView>(R.id.txtMontoTitulo).text =
            if (modo == SaldoActivity.MODO_RETIRO) "Retiro" else "Depósito"

        configurarCampoMonto()

        findViewById<View>(R.id.btnMas20).setOnClickListener { agregarMonto(20.0) }
        findViewById<View>(R.id.btnMas40).setOnClickListener { agregarMonto(40.0) }
        findViewById<View>(R.id.btnMas100).setOnClickListener { agregarMonto(100.0) }
        findViewById<View>(R.id.btnLimpiarMonto).setOnClickListener {
            editMonto.setText("")
        }

        btnContinuar.setOnClickListener { procesarTransaccion() }

        setupBottomNavigation(findViewById<BottomNavigationView>(R.id.bottomNavigationMonto))

        validarMonto()
    }

    // ---------- Campo de monto (teclado) ----------

    private fun configurarCampoMonto() {
        // Máximo 2 decimales
        editMonto.filters = arrayOf(
            object : InputFilter {
                override fun filter(
                    source: CharSequence, start: Int, end: Int,
                    dest: Spanned, dstart: Int, dend: Int
                ): CharSequence? {
                    val resultado = StringBuilder(dest)
                        .replace(dstart, dend, source.subSequence(start, end).toString())
                        .toString()
                    return if (Regex("^\\d{0,7}([.,]\\d{0,2})?$").matches(resultado)) null else ""
                }
            }
        )

        editMonto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = validarMonto()
        })

        // Tocar la tarjeta abre el teclado
        findViewById<MaterialCardView>(R.id.cardMonto).setOnClickListener { mostrarTeclado() }

        // "Listo" en el teclado lo cierra
        editMonto.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                ocultarTeclado()
                true
            } else false
        }
    }

    private fun mostrarTeclado() {
        editMonto.requestFocus()
        editMonto.setSelection(editMonto.text.length)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editMonto, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun ocultarTeclado() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editMonto.windowToken, 0)
        editMonto.clearFocus()
    }

    // ---------- Lógica del monto ----------

    private fun obtenerMonto(): Double =
        editMonto.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0

    private fun agregarMonto(valor: Double) {
        val nuevo = obtenerMonto() + valor
        val texto = if (nuevo % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", nuevo)
        } else {
            String.format(Locale.US, "%.2f", nuevo)
        }
        editMonto.setText(texto)
        editMonto.setSelection(editMonto.text.length)
    }

    private fun validarMonto() {
        val monto = obtenerMonto()
        val valido = monto >= MONTO_MINIMO

        if (editMonto.text.isNullOrEmpty() || valido) {
            txtErrorMonto.text = "Monto mínimo: $${MONTO_MINIMO.toInt()}"
            txtErrorMonto.setTextColor(getColor(R.color.GreenAccent))
        } else {
            txtErrorMonto.text = "El monto mínimo es $${MONTO_MINIMO.toInt()}"
            txtErrorMonto.setTextColor(0xFFE53935.toInt())
        }

        btnContinuar.isEnabled = valido && !procesando
        btnContinuar.alpha = if (btnContinuar.isEnabled) 1f else 0.5f
    }

    // ---------- Transacción ----------

    private fun procesarTransaccion() {
        val monto = obtenerMonto()

        if (monto < MONTO_MINIMO) {
            Toast.makeText(this, "El monto mínimo es $${MONTO_MINIMO.toInt()}", Toast.LENGTH_SHORT).show()
            return
        }
        if (procesando) return

        ocultarTeclado()
        procesando = true
        validarMonto()

        balanceRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val saldoActual = currentData.getValue(Double::class.java) ?: 0.0

                if (modo == SaldoActivity.MODO_RETIRO) {
                    if (saldoActual < monto) {
                        return Transaction.abort()
                    }
                    currentData.value = saldoActual - monto
                } else {
                    currentData.value = saldoActual + monto
                }
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                procesando = false

                if (committed) {
                    val intent = Intent(this@MontoActivity, ConfirmacionActivity::class.java).apply {
                        putExtra(SaldoActivity.EXTRA_MODO, modo)
                        putExtra(EXTRA_MONTO, monto)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    validarMonto()
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