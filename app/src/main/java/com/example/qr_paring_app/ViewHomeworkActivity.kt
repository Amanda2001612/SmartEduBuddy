package com.example.qr_paring_app

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class ViewHomeworkActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var listViewHomework: ListView

    private lateinit var tts: TextToSpeech
    private val homeworkList = ArrayList<String>()
    private val homeworkVoiceTexts = ArrayList<String>()
    private val homeworkIds = ArrayList<String>() // 🔑 හෝම්වර්ක් වල IDs තියාගන්න අලුත් ලිස්ට් එකක්

    private var globalStudentId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_view_homework)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        listViewHomework = findViewById(R.id.listViewHomework)

        tts = TextToSpeech(this, this)

        loadClassHomework()

        // 🤖 හෝම්වර්ක් එක ක්ලික් කළ විට
        listViewHomework.setOnItemClickListener { _, _, position, _ ->
            if (position < homeworkVoiceTexts.size && position < homeworkIds.size) {
                val textToRead = homeworkVoiceTexts[position]
                val homeworkId = homeworkIds[position]

                if (textToRead.isNotEmpty() && textToRead != "No Text") {
                    Toast.makeText(this, "🤖 SmartEduBuddy is reading aloud...", Toast.LENGTH_SHORT).show()
                    tts.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, null)

                    // 📈 𝓒𝓡𝓘𝓣𝓘𝓒𝓐𝓛 𝓤𝓟𝓓𝓐𝓣𝓔: ළමයා බැලුවා කියලා ඩේටාබේස් එකේ Array එකට studentId එක එකතු කරනවා
                    if (globalStudentId.isNotEmpty() && homeworkId.isNotEmpty()) {
                        db.collection("Homework").document(homeworkId)
                            .update("completedBy", FieldValue.arrayUnion(globalStudentId))
                            .addOnSuccessListener {
                                // ටීචර්ගේ පැත්තට real-time අප්ඩේට් එක යනවා
                            }
                    }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US)
            tts.setPitch(1.1f)
            tts.setSpeechRate(0.9f)
        }
    }

    private fun loadClassHomework() {
        val parentId = auth.currentUser?.uid ?: return

        db.collection("Users").document(parentId).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    globalStudentId = userDoc.getString("studentId") ?: ""

                    if (globalStudentId.isEmpty()) {
                        finish()
                        return@addOnSuccessListener
                    }

                    db.collection("StudentStats").document(globalStudentId).get()
                        .addOnSuccessListener { statsDoc ->
                            val classroomId = statsDoc.getString("classroomId") ?: ""

                            if (classroomId.isEmpty()) {
                                finish()
                                return@addOnSuccessListener
                            }

                            db.collection("Homework")
                                .whereEqualTo("classroomId", classroomId)
                                .get()
                                .addOnSuccessListener { homeworkDocs ->
                                    homeworkList.clear()
                                    homeworkVoiceTexts.clear()
                                    homeworkIds.clear()

                                    for (doc in homeworkDocs) {
                                        val title = doc.getString("title") ?: "No Title"
                                        val desc = doc.getString("voiceText") ?: "No Description"

                                        homeworkList.add("$title\n👉 Click to Let Robot Read Aloud")
                                        homeworkVoiceTexts.add("Homework Title is $title. Description is $desc")
                                        homeworkIds.add(doc.id) // 🔑 Document ID එක සේව් කරගත්තා
                                    }

                                    if (homeworkList.isEmpty()) {
                                        homeworkList.add("No homework assigned for this class! 🎉")
                                        homeworkVoiceTexts.add("No Text")
                                    }

                                    val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, homeworkList)
                                    listViewHomework.adapter = adapter
                                }
                        }
                }
            }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}