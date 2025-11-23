package com.example.resqbeacon

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class HelplineActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_helpline)

        // 1. Setup Standard National Numbers
        setupDialer(R.id.card112, "112")
        setupDialer(R.id.cardPolice, "100")
        setupDialer(R.id.cardAmbulance, "108")
        setupDialer(R.id.cardWomen, "1091")

        // 2. Setup "Speed Dial" Favorites
        setupFavoriteCard(R.id.cardFav1, R.id.tvFav1Name, R.id.tvFav1Number, "FAV_1")
        setupFavoriteCard(R.id.cardFav2, R.id.tvFav2Name, R.id.tvFav2Number, "FAV_2")
    }

    // --- LOGIC FOR STANDARD CARDS ---
    private fun setupDialer(cardId: Int, number: String) {
        findViewById<MaterialCardView>(cardId).setOnClickListener {
            makeCall(number)
        }
    }

    // --- LOGIC FOR CUSTOM FAVORITE CARDS ---
    private fun setupFavoriteCard(cardId: Int, nameId: Int, numId: Int, keyPrefix: String) {
        val card = findViewById<MaterialCardView>(cardId)
        val tvName = findViewById<TextView>(nameId)
        val tvNum = findViewById<TextView>(numId)
        val prefs = getSharedPreferences("ResQ_Favorites", Context.MODE_PRIVATE)

        // A. Load Saved Data
        fun refreshView() {
            val name = prefs.getString("${keyPrefix}_NAME", null)
            val number = prefs.getString("${keyPrefix}_NUM", null)

            if (name != null && number != null) {
                tvName.text = name
                tvNum.text = number
            } else {
                tvName.text = "Tap to Add"
                tvNum.text = "Empty"
            }
        }
        refreshView()

        // B. Handle Click (Call or Add)
        card.setOnClickListener {
            val number = prefs.getString("${keyPrefix}_NUM", null)
            if (number != null) {
                makeCall(number)
            } else {
                showEditDialog(keyPrefix, ::refreshView)
            }
        }

        // C. Handle Long Click (Edit)
        card.setOnLongClickListener {
            showEditDialog(keyPrefix, ::refreshView)
            true
        }
    }

    // --- EDIT DIALOG ---
    private fun showEditDialog(keyPrefix: String, onSaved: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_favorite, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etNumber = dialogView.findViewById<EditText>(R.id.etNumber)

        // Pre-fill existing if any
        val prefs = getSharedPreferences("ResQ_Favorites", Context.MODE_PRIVATE)
        etName.setText(prefs.getString("${keyPrefix}_NAME", ""))
        etNumber.setText(prefs.getString("${keyPrefix}_NUM", ""))

        AlertDialog.Builder(this)
            .setTitle("Set Speed Dial")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = etName.text.toString().trim()
                val newNum = etNumber.text.toString().trim()

                if (newName.isNotEmpty() && newNum.isNotEmpty()) {
                    prefs.edit()
                        .putString("${keyPrefix}_NAME", newName)
                        .putString("${keyPrefix}_NUM", newNum)
                        .apply()
                    onSaved()
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear") { _, _ ->
                prefs.edit()
                    .remove("${keyPrefix}_NAME")
                    .remove("${keyPrefix}_NUM")
                    .apply()
                onSaved()
            }
            .show()
    }

    private fun makeCall(number: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$number")
        startActivity(intent)
    }
}