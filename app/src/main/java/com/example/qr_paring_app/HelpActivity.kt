package com.example.qr_paring_app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class HelpActivity : AppCompatActivity() {

    private lateinit var btnHelpBack: ImageView
    private lateinit var btnContactWhatsApp: MaterialButton
    private lateinit var btnContactEmail: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_help)

        btnHelpBack = findViewById(R.id.btnHelpBack)
        btnContactWhatsApp = findViewById(R.id.btnContactWhatsApp)
        btnContactEmail = findViewById(R.id.btnContactEmail)

        // 🔙 Back button logic
        btnHelpBack.setOnClickListener {
            finish()
        }

        // 💬 Contact via WhatsApp
        btnContactWhatsApp.setOnClickListener {
            val contactNumber = "+94761836052" // ඔයාගේ WhatsApp නම්බර් එක
            val message = "Hello Support, I am a Teacher using SmartEduBuddy and I need some assistance."

            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$contactNumber&text=${Uri.encode(message)}")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "WhatsApp is not installed!", Toast.LENGTH_SHORT).show()
            }
        }

        // 📧 Contact via Email
        btnContactEmail.setOnClickListener {
            val emailAddress = "rashmiamanda11220@gmail.com" // ඔයාගේ Email එක
            val subject = "SmartEduBuddy - Teacher Support Request"

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }

            try {
                startActivity(Intent.createChooser(intent, "Send Email using..."))
            } catch (e: Exception) {
                Toast.makeText(this, "No Email app found!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}