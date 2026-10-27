package udb.edu.sv.dsm.udbet.fragment

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import udb.edu.sv.dsm.udbet.MainActivity
import udb.edu.sv.dsm.udbet.ProfileActivity
import udb.edu.sv.dsm.udbet.R
import udb.edu.sv.dsm.udbet.SaldoActivity

// DEFINIMOS LA CLASE AQUÍ UNA SOLA VEZ
data class HistorialEntry(
    val gameName: String = "",
    val won: Double = 0.0,
    val lost: Double = 0.0,
    val timestamp: Long = 0L
)

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var imgProfile: ImageView? = null
    private var txtBalance: TextView? = null
    private var recientesContainer: LinearLayout? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgProfile = view.findViewById(R.id.imgProfile)
        val imgProfileCard = view.findViewById<View>(R.id.imgProfileCard)
        txtBalance = view.findViewById(R.id.txtBalance)
        recientesContainer = view.findViewById(R.id.recientesContainer)

        // --- NUEVO: Al tocar el buscador del Home, nos vamos a la pestaña de Búsqueda ---
        val editSearchHome = view.findViewById<EditText>(R.id.editSearch)
        editSearchHome.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_search)
        }

        imgProfileCard.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }

        txtBalance?.setOnClickListener {
            startActivity(Intent(requireContext(), SaldoActivity::class.java))
        }

        view.findViewById<View>(R.id.cardFutbol).setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_sports)
        }
        view.findViewById<View>(R.id.cardBasquet).setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_sports)
        }

        view.findViewById<View>(R.id.cardCasino).setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_casino)
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfilePicture()
        loadBalance()
        loadRecentHistory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imgProfile = null
        txtBalance = null
        recientesContainer = null
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
                } catch (e: Exception) { }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadBalance() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("balance")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val balance = snapshot.getValue(Double::class.java) ?: 0.0
                txtBalance?.text = String.format("$%.2f", balance)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadRecentHistory() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val cont = recientesContainer ?: return

        val historyRef = FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("history")

        historyRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entradas = snapshot.children
                    .mapNotNull { it.getValue(HistorialEntry::class.java) }
                    .sortedByDescending { it.timestamp }
                    .take(3)

                mostrarRecientes(cont, entradas)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun mostrarRecientes(container: LinearLayout, entradas: List<HistorialEntry>) {
        container.removeAllViews()

        if (entradas.isEmpty()) {
            val txt = TextView(requireContext())
            txt.text = "Aún no has realizado apuestas.\n¡Explora los deportes y juega!"
            txt.setTextColor(resources.getColor(R.color.TextHint, null))
            txt.gravity = android.view.Gravity.CENTER
            txt.setPadding(0, 32, 0, 32)
            container.addView(txt)
            return
        }

        entradas.forEach { entrada ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_history_entry, container, false)

            itemView.findViewById<TextView>(R.id.txtHistGameName).text = entrada.gameName
            itemView.findViewById<TextView>(R.id.txtHistGanado).text =
                "Ganado: $%.2f".format(entrada.won)
            itemView.findViewById<TextView>(R.id.txtHistPerdido).text =
                "Perdido: $%.2f".format(entrada.lost)

            container.addView(itemView)
        }
    }
}