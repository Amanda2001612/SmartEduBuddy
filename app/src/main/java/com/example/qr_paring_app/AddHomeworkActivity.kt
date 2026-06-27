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

class AddHomeworkActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var spinnerSelectClass: Spinner
    private lateinit var etHomeworkTitle: EditText
    private lateinit var etHomeworkVoiceText: EditText
    private lateinit var btnSubmitHomework: MaterialButton
    private lateinit var btnHomeworkBack: ImageView

    // පන්ති වල නම් සහ ඒවායේ සැබෑ Document IDs තියාගන්න ලිස්ට් 2ක්
    private val classNamesList = ArrayList<String>()
    private val classIdsList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_add_homework)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        spinnerSelectClass = findViewById(R.id.spinnerSelectClass)
        etHomeworkTitle = findViewById(R.id.etHomeworkTitle)
        etHomeworkVoiceText = findViewById(R.id.etHomeworkVoiceText)
        btnSubmitHomework = findViewById(R.id.btnSubmitHomework)
        btnHomeworkBack = findViewById(R.id.btnHomeworkBack)

        btnHomeworkBack.setOnClickListener {
            finish()
        }

        // 🏫 ටීචර්ගේ පන්ති ලැයිස්තුව ලෝඩ් කරලා Dropdown එකට සෙට් කිරීම
        loadTeacherClassroomsToSpinner()

        btnSubmitHomework.setOnClickListener {
            submitHomeworkLogic()
        }
    }

    private fun loadTeacherClassroomsToSpinner() {
        val teacherId = auth.currentUser?.uid ?: return

        db.collection("Classrooms")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                classNamesList.clear()
                classIdsList.clear()

                classNamesList.add("Select Classroom")
                classIdsList.add("")

                for (doc in querySnapshot) {
                    val className = doc.getString("className") ?: "Unnamed Class"
                    val grade = doc.getString("grade") ?: ""

                    classNamesList.add("$className ($grade)")
                    classIdsList.add(doc.id) // සැබෑ Firestore ID එක සේව් කරගන්නවා
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, classNamesList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSelectClass.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading classes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun submitHomeworkLogic() {
        val title = etHomeworkTitle.text.toString().trim()
        val voiceText = etHomeworkVoiceText.text.toString().trim()
        val selectedPosition = spinnerSelectClass.selectedItemPosition

        // Validation
        if (selectedPosition == 0 || title.isEmpty() || voiceText.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and select a valid classroom", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedClassroomId = classIdsList[selectedPosition] // තෝරාගත් පන්තියේ සැබෑ ID එක ගැනීම

        btnSubmitHomework.isEnabled = false
        btnSubmitHomework.text = "Assigning..."

        // 📊 𝓒𝓡𝓘𝓣𝓘𝓒𝓐𝓛 𝓤𝓟𝓓𝓐𝓣𝓔: හෝම්වර්ක් සිතියම සාදන කොටස
        val homeworkMap = hashMapOf(
            "title" to title,
            "voiceText" to voiceText,
            "classroomId" to selectedClassroomId,
            "completedBy" to arrayListOf<String>(), // 📈 ප්‍රොග්‍රෙස් බාර් එක වැඩ කරන්න මුලින්ම හිස් Array එකක් යවනවා 🎉
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("Homework").add(homeworkMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Homework Assigned Successfully! 🎉", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSubmitHomework.isEnabled = true
                btnSubmitHomework.text = "Assign Homework to Class"
            }
    }
}