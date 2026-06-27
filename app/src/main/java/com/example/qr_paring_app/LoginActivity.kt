package com.example.qr_paring_app

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // Login බට්න් එක එබුවාම ක්‍රියාත්මක වන කොටස
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                btnLogin.isEnabled = false
                btnLogin.text = "Logging in..."

                // 1. Firebase Auth එකෙන් Login වෙනවා
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid

                            // 2. Database එකෙන් එයාගේ නියම Role එක හොයනවා
                            userId?.let { uid ->
                                db.collection("Users").document(uid).get()
                                    .addOnSuccessListener { document ->
                                        if (document != null && document.exists()) {
                                            val dbRole = document.getString("role") ?: "Parent"
                                            Toast.makeText(this, "Welcome Back $dbRole!", Toast.LENGTH_SHORT).show()

                                            // 3. Role එක අනුව කෙළින්ම අදාළ Dashboard එකටම රීඩිරෙක්ට් කිරීම 🚀
                                            if (dbRole == "Teacher") {
                                                startActivity(Intent(this, TeacherDashboardActivity::class.java))
                                            } else {
                                                startActivity(Intent(this, ParentDashboardActivity::class.java))
                                            }

                                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                            finish() // Login පිටුව වසා දමයි
                                        } else {
                                            Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show()
                                            btnLogin.isEnabled = true
                                            btnLogin.text = "Login"
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                        btnLogin.isEnabled = true
                                        btnLogin.text = "Login"
                                    }
                            }
                        } else {
                            Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            btnLogin.isEnabled = true
                            btnLogin.text = "Login"
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        // Forgot Password ලින්ක් එක එබුවාම
        tvForgotPassword.setOnClickListener {
            val emailInput = EditText(this)
            emailInput.hint = "Enter your registered email"
            emailInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            emailInput.setPadding(50, 50, 50, 50)

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Reset Password")
            builder.setMessage("Enter your email to receive a password reset link.")
            builder.setView(emailInput)

            builder.setPositiveButton("Send") { _, _ ->
                val resetEmail = emailInput.text.toString().trim()
                if (resetEmail.isNotEmpty()) {
                    auth.sendPasswordResetEmail(resetEmail)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Reset link sent! Please check your email.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            builder.create().show()
        }

        // Sign Up ලින්ක් එක එබුවාම
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // 🌟 FIXED: මෙතන තිබුණු පරණ onStart() රීඩිරෙක්ට් ලොජික් එක සහමුලින්ම ඉවත් කර ඇත.
    // ඒ නිසා දැන් Splash එකෙන් එන Flow එක පිරිසිදුවටම වැඩ කරනවා!
}