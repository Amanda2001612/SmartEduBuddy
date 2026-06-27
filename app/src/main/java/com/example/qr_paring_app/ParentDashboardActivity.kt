package com.example.qr_paring_app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

class ParentDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var realtimeDb: FirebaseDatabase

    private lateinit var tvWelcomeName: TextView
    private lateinit var tvRobotStatus: TextView
    private lateinit var tvBatteryLevel: TextView
    private lateinit var tvPendingHomework: TextView
    private lateinit var tvScreenTime: TextView
    private lateinit var tvProgressPercentage: TextView
    private lateinit var tvLessonsStatus: TextView
    private lateinit var tvClassroomName: TextView
    private lateinit var pbDailyProgress: ProgressBar
    private lateinit var imgProfile: ImageView

    private lateinit var cardHomework: MaterialCardView
    private lateinit var cardRobotStatus: MaterialCardView
    private lateinit var cardClassroomStatus: MaterialCardView

    // Navigation Buttons
    private lateinit var btnNavHome: LinearLayout
    private lateinit var btnNavJoinClass: LinearLayout
    private lateinit var btnNavRobotSettings: LinearLayout // 🤖 රොබෝ කන්ට්‍රෝල් එක
    private lateinit var btnNavProfileLayout: LinearLayout

    private var studentId = "student_001"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_parent_dashboard)

        auth = FirebaseAuth.getInstance()
        firestoreDb = Firebase.firestore
        realtimeDb = FirebaseDatabase.getInstance("https://smartedubuddy-default-rtdb.firebaseio.com/")

        tvWelcomeName = findViewById(R.id.tvWelcomeName)
        tvRobotStatus = findViewById(R.id.tvRobotStatus)
        tvBatteryLevel = findViewById(R.id.tvBatteryLevel)
        tvPendingHomework = findViewById(R.id.tvPendingHomework)
        tvScreenTime = findViewById(R.id.tvScreenTime)
        tvProgressPercentage = findViewById(R.id.tvProgressPercentage)
        tvLessonsStatus = findViewById(R.id.tvLessonsStatus)
        tvClassroomName = findViewById(R.id.tvClassroomName)
        pbDailyProgress = findViewById(R.id.pbDailyProgress)
        imgProfile = findViewById(R.id.imgProfile)

        cardHomework = findViewById(R.id.cardHomework)
        cardRobotStatus = findViewById(R.id.cardRobotStatus)
        cardClassroomStatus = findViewById(R.id.cardClassroomStatus)

        btnNavHome = findViewById(R.id.btnNavHome)
        btnNavJoinClass = findViewById(R.id.btnNavJoinClass)
        btnNavRobotSettings = findViewById(R.id.btnNavRobotSettings) // 🔑 XML එකත් එක්ක නිවැරදිව සම්බන්ධ කළා
        btnNavProfileLayout = findViewById(R.id.btnNavProfileLayout)

        loadParentData()

        imgProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        cardHomework.setOnClickListener {
            val intent = Intent(this, ViewHomeworkActivity::class.java)
            startActivity(intent)
        }

        cardClassroomStatus.setOnClickListener {
            val intent = Intent(this, JoinClassActivity::class.java)
            startActivity(intent)
        }

        btnNavJoinClass.setOnClickListener {
            val intent = Intent(this, JoinClassActivity::class.java)
            startActivity(intent)
        }

        // 🤖 Robo UI (Robot Settings) පිටුවට යාම සක්‍රීය කිරීම 🎉
        btnNavRobotSettings.setOnClickListener {
            val intent = Intent(this, RobotSettingsActivity::class.java)
            startActivity(intent)
        }

        btnNavProfileLayout.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        btnNavHome.setOnClickListener {
            Toast.makeText(this, "You are already on Home", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadParentData()
    }

    private fun loadParentData() {
        val userId = auth.currentUser?.uid ?: return

        firestoreDb.collection("Users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: "Parent"
                    tvWelcomeName.text = "Mrs. $name"

                    val robotId = document.getString("robotId") ?: "12345"
                    studentId = document.getString("studentId") ?: "student_001"

                    listenToRobotRealtimeStatus(robotId)
                    listenToStudentStats(studentId)
                }
            }
    }

    private fun listenToRobotRealtimeStatus(robotId: String) {
        val robotRef = realtimeDb.getReference("Robots").child(robotId)
        robotRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val battery = snapshot.child("Battery").getValue(Long::class.java) ?: 0
                    tvRobotStatus.text = "Robot Online"
                    tvBatteryLevel.text = "$battery%"
                } else {
                    tvRobotStatus.text = "Robot Offline"
                    tvBatteryLevel.text = "0%"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenToStudentStats(studentId: String) {
        firestoreDb.collection("StudentStats").document(studentId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) {
                    tvClassroomName.text = "Not Enrolled (Tap to Join)"
                    return@addSnapshotListener
                }

                val screenTime = snapshot.getString("screenTimeUsed") ?: "0h 0m"
                val progress = snapshot.getLong("dailyProgress")?.toInt() ?: 0
                val lessons = snapshot.getLong("completedLessons") ?: 0
                val classroomId = snapshot.getString("classroomId") ?: ""

                tvScreenTime.text = "$screenTime Used"
                tvProgressPercentage.text = "$progress%"
                pbDailyProgress.progress = progress
                tvLessonsStatus.text = "Your child completed $lessons lessons"

                if (classroomId.isNotEmpty()) {
                    firestoreDb.collection("Classrooms").document(classroomId).get()
                        .addOnSuccessListener { classDoc ->
                            if (classDoc.exists()) {
                                val className = classDoc.getString("className") ?: "Classroom"
                                val grade = classDoc.getString("grade") ?: ""
                                tvClassroomName.text = "$className ($grade)"
                            }
                        }

                    firestoreDb.collection("Homework")
                        .whereEqualTo("classroomId", classroomId)
                        .addSnapshotListener { homeworkSnapshot, _ ->
                            val homeworkCount = homeworkSnapshot?.size() ?: 0
                            tvPendingHomework.text = "$homeworkCount Pending"
                        }
                } else {
                    tvClassroomName.text = "Not Enrolled (Tap to Join)"
                    tvPendingHomework.text = "0 Pending"
                }
            }
    }
}