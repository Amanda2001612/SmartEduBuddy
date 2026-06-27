package com.example.qr_paring_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)

        // 🚀 Get Started බටන් එක ක්ලික් කළ විට පමණක් සෙෂන් එක චෙක් කිරීම සිදුවේ
        btnGetStarted.setOnClickListener {
            checkUserSessionAndNavigate()
        }
    }

    private fun checkUserSessionAndNavigate() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // 🔐 යූසර් දැනටමත් ලොග් වී සිටී නම්, එයාගේ Role එක Firestore එකෙන් කියවීම
            db.collection("Users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userRole = document.getString("role")

                        when (userRole) {
                            "Parent" -> {
                                startActivity(Intent(this, ParentDashboardActivity::class.java))
                                finish()
                            }
                            "Teacher" -> {
                                startActivity(Intent(this, TeacherDashboardActivity::class.java))
                                finish()
                            }
                            "Student" -> {
                                // 🔑 FIXED: දැනට StudentLoginActivity එක නැති නිසා සාමාන්‍ය LoginActivity එකටම යවයි
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            else -> {
                                navigateToLogin()
                            }
                        }
                    } else {
                        navigateToLogin()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Connection error. Please try again!", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }
        } else {
            // 🔓 ලොග් වී නොමැති නම් කෙළින්ම Login පේජ් එකට යැවීම
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}