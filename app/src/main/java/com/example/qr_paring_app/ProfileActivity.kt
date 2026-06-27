package com.example.qr_paring_app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var etProfileName: EditText
    private lateinit var etProfileEmail: EditText
    private lateinit var etProfilePassword: EditText

    // Teacher inputs
    private lateinit var cardTeacherFeatures: MaterialCardView
    private lateinit var etTeacherSchool: EditText

    // Parent inputs
    private lateinit var cardParentFeatures: MaterialCardView
    private lateinit var etParentRelation: EditText
    private lateinit var etParentEmergencyContact: EditText

    private lateinit var btnUpdateProfile: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnBack: ImageView
    private lateinit var imgProfilePic: ImageView
    private lateinit var btnEditProfilePic: MaterialCardView

    private var imageUri: Uri? = null
    private var userRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        etProfileName = findViewById(R.id.etProfileName)
        etProfileEmail = findViewById(R.id.etProfileEmail)
        etProfilePassword = findViewById(R.id.etProfilePassword)

        // Dynamic Card bindings
        cardTeacherFeatures = findViewById(R.id.cardTeacherFeatures)
        etTeacherSchool = findViewById(R.id.etTeacherSchool)

        cardParentFeatures = findViewById(R.id.cardParentFeatures)
        etParentRelation = findViewById(R.id.etParentRelation)
        etParentEmergencyContact = findViewById(R.id.etParentEmergencyContact)

        btnUpdateProfile = findViewById(R.id.btnUpdateProfile)
        btnLogout = findViewById(R.id.btnLogout)
        btnBack = findViewById(R.id.btnBack)
        imgProfilePic = findViewById(R.id.imgProfilePic)
        btnEditProfilePic = findViewById(R.id.btnEditProfilePic)

        loadUserData()

        val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                imageUri = result.data?.data
                imgProfilePic.setImageURI(imageUri)
            }
        }

        btnEditProfilePic.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        btnUpdateProfile.setOnClickListener {
            val newName = etProfileName.text.toString().trim()
            val newPassword = etProfilePassword.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Name is empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔐 Password එකක් ඇතුළත් කරලා තියෙනවා නම් ඒක වෙනස් කරනවා
            if (newPassword.isNotEmpty()) {
                if (newPassword.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                auth.currentUser?.updatePassword(newPassword)
                    ?.addOnFailureListener {
                        Toast.makeText(this, "Password update failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            if (imageUri != null) {
                uploadImageAndSave(newName)
            } else {
                saveDataToFirestore(newName, null)
            }
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        etProfileEmail.setText(auth.currentUser?.email)

        db.collection("Users").document(uid).get().addOnSuccessListener { document ->
            if (document.exists()) {
                etProfileName.setText(document.getString("name"))

                // 🔑 Role එක අනුව අදාළ සෙක්ෂන්ස් විතරක් පෙන්වීම
                userRole = document.getString("role") ?: "Teacher"

                if (userRole == "Teacher") {
                    cardTeacherFeatures.visibility = View.VISIBLE
                    etTeacherSchool.setText(document.getString("schoolName") ?: "")
                } else if (userRole == "Parent") {
                    cardParentFeatures.visibility = View.VISIBLE
                    etParentRelation.setText(document.getString("relationship") ?: "")
                    etParentEmergencyContact.setText(document.getString("emergencyContact") ?: "")
                }
            }
        }
    }

    private fun uploadImageAndSave(newName: String) {
        val uid = auth.currentUser?.uid ?: return
        val ref = storage.reference.child("ProfilePictures/$uid.jpg")

        // 🔄 TaskSnapshot එක හරහා නිවැරදිව putFile එක රන් කරවනවා
        ref.putFile(imageUri!!)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                // අප්ලෝඩ් එක සර්වසම්පූර්ණ වුණාට පස්සෙමයි downloadUrl එක ඉල්ලන්නේ
                return@continueWithTask ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                // දැන් කිසිම කරදරයක් නැතුව URL එක ලැබෙනවා
                saveDataToFirestore(newName, downloadUri.toString())
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveDataToFirestore(newName: String, imageUrl: String?) {
        val uid = auth.currentUser?.uid ?: return
        val data = mutableMapOf<String, Any>("name" to newName)
        if (imageUrl != null) data["profilePic"] = imageUrl

        // Role එක අනුව අමතර දත්ත Firestore එකට එකතු කිරීම
        if (userRole == "Teacher") {
            data["schoolName"] = etTeacherSchool.text.toString().trim()
        } else if (userRole == "Parent") {
            data["relationship"] = etParentRelation.text.toString().trim()
            data["emergencyContact"] = etParentEmergencyContact.text.toString().trim()
        }

        db.collection("Users").document(uid).update(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Saved Successfully! ✨", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Save failed!", Toast.LENGTH_SHORT).show()
            }
    }
}