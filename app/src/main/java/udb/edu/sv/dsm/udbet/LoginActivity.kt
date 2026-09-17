package udb.edu.sv.dsm.udbet

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val rootView = findViewById<android.view.View>(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()
        // Elementos de la interfaz
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            // Validar campos vacíos
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.error_empty_fields),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Validar formato del correo
            if (!android.util.Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()
            ) {
                Toast.makeText(
                    this,
                    "Ingresa un correo electrónico válido",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Iniciar sesión con Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            getString(R.string.welcome_msg),
                            Toast.LENGTH_SHORT
                        ).show()
                        // Mantiene exactamente tu navegación:
                        // Login -> MainActivity
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(
                            this,
                            "Correo o contraseña incorrectos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // Login -> Register
        tvGoToRegister.setOnClickListener {

            val intent =
                Intent(this, RegisterActivity::class.java)

            startActivity(intent)
        }
    }
}