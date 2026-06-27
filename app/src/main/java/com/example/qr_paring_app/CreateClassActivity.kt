package com.example.qr_paring_app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateClassActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etClassName: EditText
    private lateinit var etSubjectName: EditText
    private lateinit var etStudentCount: EditText
    private lateinit var spinnerGrade: Spinner
    private lateinit var btnSubmitClass: MaterialButton
    private lateinit var btnBack: ImageView

    // Grade ලැයිස්තුව
    private val gradesList = arrayOf("Select Grade", "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_create_class)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // XML Components හරියටම සම්බන්ධ කිරීම
        etClassName = findViewById(R.id.etClassName)
        etSubjectName = findViewById(R.id.etSubjectName)
        etStudentCount = findViewById(R.id.etStudentCount)
        spinnerGrade = findViewById(R.id.spinnerGrade)
        btnSubmitClass = findViewById(R.id.btnSubmitClass)
        btnBack = findViewById(R.id.btnBack)

        // Spinner එකට දත්ත ඇතුළත් කිරීම
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, gradesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGrade.adapter = adapter

        // ආපසු යාමේ බොත්තම (Back Button) ක්‍රියාත්මක කිරීම
        btnBack.setOnClickListener {
            finish()
        }

        // Classroom සාදන බොත්තම එබූ විට
        btnSubmitClass.setOnClickListener {
            createClassroomLogic()
        }
    }

    private fun createClassroomLogic() {
        val className = etClassName.text.toString().trim()
        val subject = etSubjectName.text.toString().trim()
        val studentCount = etStudentCount.text.toString().trim()
        val selectedGrade = spinnerGrade.selectedItem.toString()
        val teacherId = auth.currentUser?.uid ?: return

        // හිස්තැන් තිබේදැයි පරික්ෂා කිරීම
        if (className.isEmpty() || subject.isEmpty() || studentCount.isEmpty() || selectedGrade == "Select Grade") {
            Toast.makeText(this, "Please fill all fields and select a grade", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmitClass.isEnabled = false
        btnSubmitClass.text = "Creating Classroom..."

        // 🎲 𝓒𝓡𝓘𝓣𝓘𝓦𝓐𝓁 𝓤𝓟𝓓𝓐𝓣𝓔: අකුරු සහ ඉලක්කම් 6ක කෙටි කෝඩ් එකක් ඉබේම සාදයි 🎉
        val generatedClassCode = generateRandomClassCode()

        // Firestore එකට යවන දත්ත සිතියම (Map)
        val classroomMap = hashMapOf(
            "className" to className,
            "subject" to subject,
            "studentCount" to studentCount,
            "teacherId" to teacherId,
            "classCode" to generatedClassCode, // ඔටෝ ජෙනරේට් වූ කෝඩ් එක
            "grade" to selectedGrade           // Grade Validation සඳහා ශ්‍රේණිය
        )

        // Classrooms කලෙක්ෂන් එකට ඇතුළත් කිරීම
        db.collection("Classrooms").add(classroomMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Classroom Created Successfully! Code: $generatedClassCode 🎉", Toast.LENGTH_LONG).show()
                finish() // Dashboard එකට ආපසු යයි
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSubmitClass.isEnabled = true
                btnSubmitClass.text = "Create Classroom"
            }
    }

    // 🎲 අකුරු/ඉලක්කම් 6 ක කෙටි කේත සාදන රන්ඩම් ෆන්ක්ෂන් එක
    private fun generateRandomClassCode(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }
}