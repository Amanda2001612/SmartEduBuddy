package com.example.qr_paring_app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.AlertDialog

class JoinClassActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etChildName: EditText
    private lateinit var spinnerChildGrade: Spinner
    private lateinit var etClassCode: EditText
    private lateinit var btnJoinClass: MaterialButton

    // Grade ලැයිස්තුව ලෑස්ති කරගන්නවා
    private val gradesList = arrayOf("Select Grade", "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_join_class)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etChildName = findViewById(R.id.etChildName)
        spinnerChildGrade = findViewById(R.id.spinnerChildGrade)
        etClassCode = findViewById(R.id.etClassCode)
        btnJoinClass = findViewById(R.id.btnJoinClass)

        // Spinner එකට Grade ටික සෙට් කරනවා
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, gradesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerChildGrade.adapter = adapter

        btnJoinClass.setOnClickListener {
            val childName = etChildName.text.toString().trim()
            val classCode = etClassCode.text.toString().trim()
            val selectedGrade = spinnerChildGrade.selectedItem.toString()

            if (childName.isEmpty() || classCode.isEmpty() || selectedGrade == "Select Grade") {
                Toast.makeText(this, "Please fill all fields and select a valid grade", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnJoinClass.isEnabled = false
            btnRegisterTextUpdate("Joining...")

            enrollStudentWithGradeValidation(childName, classCode, selectedGrade)
        }
    }

    private fun enrollStudentWithGradeValidation(childName: String, classCode: String, selectedGrade: String) {
        val parentId = auth.currentUser?.uid ?: return

        // 🏫 දිග Document ID එක වෙනුවට අකුරු 6 කෙටි කෝඩ් එකෙන් (classCode) Query කරනවා
        db.collection("Classrooms")
            .whereEqualTo("classCode", classCode.uppercase()) // Simple ගැහුවත් Capital කරලා සර්ච් කරයි
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->

                // 1. ඒ කෝඩ් එකට අදාළ පන්තියක් ඩේටාබේස් එකේ තියෙනවා නම් විතරක් ඇතුළට යනවා
                if (!querySnapshot.isEmpty) {
                    val classDoc = querySnapshot.documents[0]
                    val realClassroomId = classDoc.id // පන්තියේ සැබෑ Firestore Document ID එක මෙතනින් ගත්තා
                    val dbClassGrade = classDoc.getString("grade") ?: "Grade 1"

                    // 🛠️ Smart Grade Normalization:
                    // ඩේටාබේස් එකෙන් එන එකේ (e.g., "grade 03") ඉලක්කම් ටික විතරක් අරන් Integer එකක් කරයි (ලැබෙන්නේ 3)
                    val dbGradeNumber = dbClassGrade.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 1

                    // Spinner එකෙන් සිලෙක්ට් කරපු එකේ (e.g., "Grade 3") ඉලක්කම් ටික විතරක් අරන් Integer එකක් කරයි (ලැබෙන්නේ 3)
                    val selectedGradeNumber = selectedGrade.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 1

                    // 2. 🧑‍🎓 දැන් අකුරු ප්‍රශ්න හෝ බින්දු ප්‍රශ්න නැත, පිරිසිදු ඉලක්කම් දෙක සමානදැයි බලයි (3 == 3)
                    if (dbGradeNumber == selectedGradeNumber) {

                        // RegisterActivity එකෙන් එන අලුත් studentId එක මෙතනට ගන්නවා
                        db.collection("Users").document(parentId).get()
                            .addOnSuccessListener { userDoc ->
                                val studentId = userDoc.getString("studentId") ?: ("student_" + parentId.takeLast(4))

                                val enrollmentData = hashMapOf(
                                    "classroomId" to realClassroomId, // සැබෑ පන්ති ID එක දැම්මා
                                    "studentId" to studentId,
                                    "studentName" to childName,
                                    "parentId" to parentId,
                                    "grade" to selectedGrade
                                )

                                // ClassroomStudents එකට දානවා ටීචර්ට පෙනෙන්න
                                db.collection("ClassroomStudents").document(studentId).set(enrollmentData)
                                    .addOnSuccessListener {

                                        // StudentStats එකට ළමයාගේ grade එකත් එක්කම අප්ඩේට් කරනවා
                                        val statsData = hashMapOf(
                                            "classroomId" to realClassroomId, // සැබෑ පන්ති ID එක දැම්මා
                                            "studentName" to childName,
                                            "grade" to selectedGrade,
                                            "pendingHomeworks" to 0,
                                            "screenTimeUsed" to "0h 0m",
                                            "dailyProgress" to 0
                                        )

                                        db.collection("StudentStats").document(studentId).set(statsData)
                                            .addOnSuccessListener {
                                                showSuccessDialog()
                                            }
                                    }
                            }
                    } else {
                        // Grade එක ඇත්තටම ගැලපෙන්නේ නැත්නම් (උදා: 4 වසරේ පන්තියකට 2 වසරේ ළමයෙක් දාන්න හැදුවොත්) 🚫
                        Toast.makeText(this, "Grade Mismatch! This class is for $dbClassGrade, but your child is in $selectedGrade.", Toast.LENGTH_LONG).show()
                        resetButton()
                    }
                } else {
                    Toast.makeText(this, "Invalid Class Code! Classroom not found.", Toast.LENGTH_LONG).show()
                    resetButton()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                resetButton()
            }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Successfully Enrolled! 🎉")
            .setMessage("Your child has been successfully verified and added to the class according to the grade.")
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }.show()
    }

    private fun btnRegisterTextUpdate(text: String) {
        btnJoinClass.text = text
    }

    private fun resetButton() {
        btnJoinClass.isEnabled = true
        btnJoinClass.text = "Join & Link Child"
    }
}