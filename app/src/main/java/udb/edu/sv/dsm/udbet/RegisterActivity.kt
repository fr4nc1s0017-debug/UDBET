package udb.edu.sv.dsm.udbet

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        val rootView = findViewById<android.view.View>(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Elementos de la interfaz
        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        btnRegister.setOnClickListener {

            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword =
                etConfirmPassword.text.toString()
            // Validar campos vacíos
            if (name.isEmpty() ||
                email.isEmpty() ||
                password.isEmpty() ||
                confirmPassword.isEmpty()
            ) {
                Toast.makeText(
                    this,
                    getString(R.string.error_empty_fields),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Validar correo
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(
                    this,
                    "Ingresa un correo electrónico válido",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Validar longitud de contraseña
            if (password.length < 6) {
                Toast.makeText(
                    this,
                    "La contraseña debe tener al menos 6 caracteres",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Confirmar contraseña
            if (password != confirmPassword) {
                Toast.makeText(
                    this,
                    getString(R.string.error_password_mismatch),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Crear usuario en Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val uid = user.uid

                            // Datos que sí se guardan en Realtime Database
                            val userData = HashMap<String, Any>()

                            userData["name"] = name
                            userData["email"] = email

                            // Crear usuario en Realtime Database
                            database.reference
                                .child("users")
                                .child(uid)
                                .setValue(userData)
                                .addOnSuccessListener {

                                    Toast.makeText(
                                        this,
                                        getString(R.string.register_success),
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Mantiene tu lógica original:
                                    // regresar al Login
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this,
                                        "Error al guardar los datos del usuario",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }

                    } else {

                        val errorMessage =
                            task.exception?.message
                                ?: "No se pudo crear la cuenta"
                        Toast.makeText(
                            this,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // Mantiene tu navegación original
        tvGoToLogin.setOnClickListener {
            finish()
        }
    }
}