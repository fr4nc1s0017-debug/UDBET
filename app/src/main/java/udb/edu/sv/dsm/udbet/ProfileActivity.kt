package udb.edu.sv.dsm.udbet

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var imgProfileCard: com.google.android.material.card.MaterialCardView
    private lateinit var imgProfilePicture: ImageView
    private lateinit var progressUpload: ProgressBar
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var editPhone: EditText
    private lateinit var btnModificarPassword: TextView
    private lateinit var btnModificarPhone: TextView
    private lateinit var btnActualizar: MaterialButton
    private lateinit var btnCancelar: MaterialButton
    private lateinit var btnClose: ImageView

    private val auth = FirebaseAuth.getInstance()
    // Nodo en Realtime Database: users/{uid}/{username, phone, profileImage}
    private val dbRef by lazy {
        FirebaseDatabase.getInstance().reference.child("users").child(auth.currentUser?.uid ?: "unknown")
    }

    // Nueva contraseña pendiente de guardar (se pide junto con la actual para reautenticar)
    private var pendingNewPassword: String? = null

    // Selector de imagen de galería
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uploadProfileImage(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        imgProfileCard = findViewById(R.id.imgProfileCard)
        imgProfilePicture = findViewById(R.id.imgProfilePicture)
        progressUpload = findViewById(R.id.progressUpload)
        editEmail = findViewById(R.id.editEmail)
        editPassword = findViewById(R.id.editPassword)
        editPhone = findViewById(R.id.editPhone)
        btnModificarPassword = findViewById(R.id.btnModificarPassword)
        btnModificarPhone = findViewById(R.id.btnModificarPhone)
        btnActualizar = findViewById(R.id.btnActualizar)
        btnCancelar = findViewById(R.id.btnCancelar)
        btnClose = findViewById(R.id.btnClose)

        // Tocar la foto (todo el círculo) = elegir una nueva imagen de la galería
        imgProfileCard.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnClose.setOnClickListener { finish() }
        btnCancelar.setOnClickListener { finish() }

        btnModificarPassword.setOnClickListener { showChangePasswordDialog() }
        btnModificarPhone.setOnClickListener {
            editPhone.isEnabled = true
            editPhone.requestFocus()
        }

        btnActualizar.setOnClickListener { saveChanges() }

        loadUserData()
    }

    /** Carga correo actual, teléfono y foto guardados */
    private fun loadUserData() {
        editEmail.setText(auth.currentUser?.email ?: "")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val phone = snapshot.child("phone").getValue(String::class.java)
                if (!phone.isNullOrEmpty()) editPhone.setText(phone)

                val base64Image = snapshot.child("profileImage").getValue(String::class.java)
                if (!base64Image.isNullOrEmpty()) {
                    setImageFromBase64(base64Image)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, "Error al cargar datos: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setImageFromBase64(base64Image: String) {
        try {
            val bytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imgProfilePicture.setImageBitmap(bitmap)
        } catch (e: Exception) {
            // Imagen corrupta o no decodificable; se ignora y se deja el placeholder
        }
    }

    /**
     * Redimensiona la imagen elegida y la sube como Base64 a Realtime Database.
     * NOTA: para producción, lo recomendable es usar Firebase Storage para el archivo
     * y guardar solo la URL en Realtime Database. Aquí se guarda directamente en RTDB
     * porque así se pidió, comprimiendo la imagen para que el nodo no pese demasiado.
     */
    private fun uploadProfileImage(uri: Uri) {
        progressUpload.visibility = ProgressBar.VISIBLE
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val resized = Bitmap.createScaledBitmap(originalBitmap, 300, 300, true)

            val outputStream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val imageBytes = outputStream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)

            dbRef.child("profileImage").setValue(base64Image)
                .addOnSuccessListener {
                    progressUpload.visibility = ProgressBar.GONE
                    imgProfilePicture.setImageBitmap(resized)
                    Toast.makeText(this, "Foto actualizada", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    progressUpload.visibility = ProgressBar.GONE
                    Toast.makeText(this, "Error al guardar la foto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            progressUpload.visibility = ProgressBar.GONE
            Toast.makeText(this, "No se pudo procesar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    /** Diálogo para pedir contraseña actual + nueva contraseña (requerido para reautenticar) */
    private fun showChangePasswordDialog() {
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(48, 24, 48, 0)

        val currentPassInput = EditText(this)
        currentPassInput.hint = "Contraseña actual"
        currentPassInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        val newPassInput = EditText(this)
        newPassInput.hint = "Nueva contraseña"
        newPassInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        container.addView(currentPassInput)
        container.addView(newPassInput)

        AlertDialog.Builder(this)
            .setTitle("Cambiar contraseña")
            .setView(container)
            .setPositiveButton("Confirmar") { _, _ ->
                val currentPass = currentPassInput.text.toString()
                val newPass = newPassInput.text.toString()
                if (currentPass.isBlank() || newPass.length < 6) {
                    Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                reauthenticateAndChangePassword(currentPass, newPass)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun reauthenticateAndChangePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "No se pudo actualizar la contraseña: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
            }
    }

    /** Guarda el teléfono en RTDB y, si el correo cambió, actualiza Firebase Auth */
    private fun saveChanges() {
        val newPhone = editPhone.text.toString().trim()
        val newEmail = editEmail.text.toString().trim()
        val currentUser = auth.currentUser

        dbRef.child("phone").setValue(newPhone)

        if (currentUser != null && newEmail.isNotEmpty() && newEmail != currentUser.email) {
            // Cambiar el correo también requiere reautenticación reciente.
            askForPasswordThenUpdateEmail(newEmail)
        } else {
            Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show()
        }
    }

    private fun askForPasswordThenUpdateEmail(newEmail: String) {
        val passInput = EditText(this)
        passInput.hint = "Confirma tu contraseña actual"
        passInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Confirmar cambio de correo")
            .setView(passInput)
            .setPositiveButton("Confirmar") { _, _ ->
                val user = auth.currentUser ?: return@setPositiveButton
                val currentEmail = user.email ?: return@setPositiveButton
                val credential = EmailAuthProvider.getCredential(currentEmail, passInput.text.toString())

                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        user.verifyBeforeUpdateEmail(newEmail)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Se envió un correo de verificación a $newEmail",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "No se pudo cambiar el correo: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
