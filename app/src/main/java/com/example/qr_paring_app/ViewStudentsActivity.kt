package com.example.qr_paring_app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ViewStudentsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var listViewStudents: ListView
    private val studentList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_view_students)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        listViewStudents = findViewById(R.id.listViewStudents)

        loadEnrolledStudents()
    }

    private fun loadEnrolledStudents() {
        val currentTeacherId = auth.currentUser?.uid ?: return

        // 1. මුලින්ම ටීචර්ගේ පන්තියේ ID එක හොයාගන්නවා
        db.collection("Classrooms")
            .whereEqualTo("teacherId", currentTeacherId)
            .limit(1)
            .get()
            .addOnSuccessListener { classDocs ->
                if (!classDocs.isEmpty) {
                    val classroomId = classDocs.documents[0].id

                    // 2. ඒ classroomId එක යටතේ රෙජිස්ටර් වුණු ළමයි ටික කියවනවා
                    db.collection("ClassroomStudents")
                        .whereEqualTo("classroomId", classroomId)
                        .get()
                        .addOnSuccessListener { students ->
                            studentList.clear()
                            for (doc in students) {
                                val name = doc.getString("studentName") ?: "Unknown Student"
                                val id = doc.getString("studentId") ?: ""
                                studentList.add("$name ($id)")
                            }

                            if (studentList.isEmpty()) {
                                studentList.add("No students enrolled yet.")
                            }

                            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, studentList)
                            listViewStudents.adapter = adapter
                        }
                } else {
                    Toast.makeText(this, "Create a class first!", Toast.LENGTH_SHORT).show()
                }
            }
    }
}