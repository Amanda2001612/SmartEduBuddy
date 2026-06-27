package com.example.qr_paring_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeacherDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvTeacherName: TextView
    private lateinit var rvClassroomsProgress: RecyclerView
    private lateinit var btnRefreshProgress: TextView

    private lateinit var btnCreateClassroom: MaterialCardView
    private lateinit var btnAddHomework: MaterialCardView
    private lateinit var btnViewStudents: MaterialCardView
    private lateinit var btnHelpSupport: MaterialCardView // 🔄 Settings වෙනුවට වෙනස් කළා

    private lateinit var profileCard: MaterialCardView
    private lateinit var btnNavProfile: ImageView

    private lateinit var classroomAdapter: ClassroomProgressAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_teacher_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvTeacherName = findViewById(R.id.tvTeacherName)
        rvClassroomsProgress = findViewById(R.id.rvClassroomsProgress)
        btnRefreshProgress = findViewById(R.id.btnRefreshProgress)

        btnCreateClassroom = findViewById(R.id.btnCreateClassroom)
        btnAddHomework = findViewById(R.id.btnAddHomework)
        btnViewStudents = findViewById(R.id.btnViewStudents)
        btnHelpSupport = findViewById(R.id.btnHelpSupport) // 🔄 ID එක සම්බන්ධ කළා

        profileCard = findViewById(R.id.profileCard)
        btnNavProfile = findViewById(R.id.btnNavProfile)

        rvClassroomsProgress.layoutManager = LinearLayoutManager(this)
        rvClassroomsProgress.isNestedScrollingEnabled = false

        classroomAdapter = ClassroomProgressAdapter(this, ArrayList())
        rvClassroomsProgress.adapter = classroomAdapter

        btnCreateClassroom.setOnClickListener {
            startActivity(Intent(this, CreateClassActivity::class.java))
        }

        btnAddHomework.setOnClickListener {
            startActivity(Intent(this, AddHomeworkActivity::class.java))
        }

        btnViewStudents.setOnClickListener {
            startActivity(Intent(this, ViewStudentsActivity::class.java))
        }

        // 🔄 Click කළාම HelpActivity එකට යන ලෙස වෙනස් කළා
        btnHelpSupport.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        profileCard.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnNavProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnRefreshProgress.setOnClickListener {
            loadTeacherAllClassroomsProgress()
            Toast.makeText(this, "All Classrooms Updated 🔄", Toast.LENGTH_SHORT).show()
        }

        loadTeacherAllClassroomsProgress()
    }

    override fun onResume() {
        super.onResume()
        loadTeacherAllClassroomsProgress()
    }

    private fun loadTeacherAllClassroomsProgress() {
        val teacherId = auth.currentUser?.uid ?: return

        db.collection("Users").document(teacherId).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    tvTeacherName.text = userDoc.getString("name") ?: "Teacher"
                }
            }

        db.collection("Classrooms")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .addOnSuccessListener { classSnapshots ->
                val classroomsList = ArrayList<ClassroomProgressModel>()

                if (classSnapshots.isEmpty) {
                    classroomAdapter.updateData(classroomsList)
                    return@addOnSuccessListener
                }

                var fetchedCount = 0
                for (classDoc in classSnapshots) {
                    val classroomId = classDoc.id
                    val className = classDoc.getString("className") ?: "Unnamed Class"
                    val classCode = classDoc.getString("classCode") ?: "------"

                    val studentCountStr = classDoc.getString("studentCount") ?: "0"
                    val totalStudents = studentCountStr.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0

                    db.collection("Homework")
                        .whereEqualTo("classroomId", classroomId)
                        .get()
                        .addOnSuccessListener { homeworkSnapshots ->
                            var latestHWTitle = "No Homework Assigned"
                            var completedCount = 0

                            if (!homeworkSnapshots.isEmpty) {
                                val latestHW = homeworkSnapshots.documents.last()
                                latestHWTitle = latestHW.getString("title") ?: "Recent Homework"
                                val completedList = latestHW.get("completedBy") as? ArrayList<*>
                                completedCount = completedList?.size ?: 0
                            }

                            classroomsList.add(ClassroomProgressModel(className, classCode, latestHWTitle, completedCount, totalStudents))

                            fetchedCount++
                            if (fetchedCount == classSnapshots.size()) {
                                classroomAdapter.updateData(classroomsList)
                            }
                        }
                }
            }
    }

    data class ClassroomProgressModel(
        val className: String,
        val classCode: String,
        val homeworkTitle: String,
        val completedCount: Int,
        val totalStudents: Int
    )

    class ClassroomProgressAdapter(
        private val context: Context,
        private var list: ArrayList<ClassroomProgressModel>
    ) : RecyclerView.Adapter<ClassroomProgressAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvClassName: TextView = view.findViewById(R.id.itemTvClassName)
            val tvClassCode: TextView = view.findViewById(R.id.itemTvClassCode)
            val tvHomeworkTitle: TextView = view.findViewById(R.id.itemTvHomeworkTitle)
            val tvRatio: TextView = view.findViewById(R.id.itemTvRatio)
            val progressBar: ProgressBar = view.findViewById(R.id.itemProgressBar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_classroom_progress, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvClassName.text = item.className
            holder.tvClassCode.text = item.classCode
            holder.tvHomeworkTitle.text = item.homeworkTitle
            holder.tvRatio.text = "${item.completedCount}/${item.totalStudents}"

            holder.progressBar.max = item.totalStudents
            holder.progressBar.progress = item.completedCount
        }

        override fun getItemCount(): Int = list.size

        fun updateData(newList: ArrayList<ClassroomProgressModel>) {
            this.list = newList
            notifyDataSetChanged()
        }
    }
}