package udb.edu.sv.dsm.udbet

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment(R.layout.activity_home_fragment) {

    private var imgProfile: ImageView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgProfile = view.findViewById(R.id.imgProfile)
        val imgProfileCard = view.findViewById<View>(R.id.imgProfileCard)

        // Tocar la foto de perfil abre la pantalla de usuario
        imgProfileCard.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }

        // Fútbol y baloncesto llevan a la pestaña de Deportes
        view.findViewById<View>(R.id.cardFutbol).setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_sports)
        }
        view.findViewById<View>(R.id.cardBasquet).setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_sports)
        }

        // Casino lleva a la pestaña de Casino
        view.findViewById<View>(R.id.cardCasino).setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_casino)
        }
    }

    override fun onResume() {
        super.onResume()
        // Se recarga cada vez que este fragment vuelve a primer plano
        // (p. ej. al volver de ProfileActivity, o al cambiar de pestaña y regresar)
        loadProfilePicture()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imgProfile = null
    }

    private fun loadProfilePicture() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("profileImage")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val base64Image = snapshot.getValue(String::class.java) ?: return
                try {
                    val bytes = Base64.decode(base64Image, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    imgProfile?.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    // Imagen no decodificable: se deja el avatar por defecto
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Sin conexión o sin permisos: se deja el avatar por defecto
            }
        })
    }
}
