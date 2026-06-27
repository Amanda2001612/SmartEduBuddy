package com.example.qr_paring_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etRegEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etRegPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val etRobotId = findViewById<TextInputEditText>(R.id.etRobotId)
        val layoutRobotId = findViewById<LinearLayout>(R.id.layoutRobotId)

        val rgRole = findViewById<RadioGroup>(R.id.rgRole)
        val cbTerms = findViewById<CheckBox>(R.id.cbTerms)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val tvLoginHere = findViewById<TextView>(R.id.tvLoginHere)

        // 🔄 Radio Button එක මාරු කරද්දී dynamic ලෙස Robot ID Field එක පෙන්වීම/සැඟවීම
        rgRole.setOnCheckedChangeListener { _, checkedId ->
            val selectedRb = findViewById<RadioButton>(checkedId)?.text.toString()
            if (selectedRb.contains("Student", ignoreCase = true)) {
                layoutRobotId.visibility = View.VISIBLE
            } else {
                layoutRobotId.visibility = View.GONE
            }
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val robotId = etRobotId.text.toString().trim()

            val selectedRoleId = rgRole.checkedRadioButtonId
            val selectedRole = findViewById<RadioButton>(selectedRoleId)?.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!cbTerms.isChecked) {
                Toast.makeText(this, "Please agree to the Terms of Service", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Role එක වෙන් කර හඳුනා ගැනීම
            val finalRole = when {
                selectedRole.contains("Teacher", ignoreCase = true) -> "Teacher"
                selectedRole.contains("Parent", ignoreCase = true) -> "Parent"
                else -> "Student"
            }

            // ශිෂ්‍යයෙක් නම් Robot ID එක අනිවාර්යයෙන්ම ඇතුළත් කළ යුතුය
            if (finalRole == "Student" && robotId.isEmpty()) {
                Toast.makeText(this, "Please enter your Robot Hardware ID!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            btnRegister.text = "Registering..."

            // 1. Firebase Auth එකෙන් User සෑදීම
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid

                        if (userId != null) {
                            if (finalRole == "Student") {
                                generateNextStudentCode { studentCode ->
                                    val userMap = hashMapOf<String, Any>(
                                        "name" to name,
                                        "email" to email,
                                        "role" to finalRole,
                                        "studentId" to studentCode,
                                        "robotId" to robotId // 🤖 රොබෝ ID එක කෙළින්ම ඇතුළත් කරා
                                    )
                                    saveUserToFirestore(userId, userMap, btnRegister)
                                }
                            } else {
                                val userMap = hashMapOf<String, Any>(
                                    "name" to name,
                                    "email" to email,
                                    "role" to finalRole
                                )
                                saveUserToFirestore(userId, userMap, btnRegister)
                            }
                        }
                    } else {
                        Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        btnRegister.isEnabled = true
                        btnRegister.text = "Sign Up"
                    }
                }
        }

        tvLoginHere.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun generateNextStudentCode(onCounterGenerated: (String) -> Unit) {
        val counterRef = db.collection("Counters").document("studentCounter")

        db.runTransaction { transaction ->
            val snapshot = transaction.get(counterRef)
            val lastId = snapshot.getLong("lastStudentId") ?: 0
            val nextId = lastId + 1

            transaction.update(counterRef, "lastStudentId", nextId)
            nextId
        }.addOnSuccessListener { nextId ->
            val formattedCode = String.format("STD-%03d", nextId)
            onCounterGenerated(formattedCode)
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Counter Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserToFirestore(userId: String, userMap: Map<String, Any>, btnRegister: MaterialButton) {
        db.collection("Users").document(userId).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_LONG).show()
                btnRegister.isEnabled = true
                btnRegister.text = "Sign Up"
            }
    }
}