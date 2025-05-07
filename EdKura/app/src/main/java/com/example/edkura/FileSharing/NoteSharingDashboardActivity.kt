package com.example.edkura.FileSharing

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import com.example.edkura.Narciso.Student
import com.example.edkura.R
import com.example.edkura.databinding.ActivityNoteSharingDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class NoteSharingDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteSharingDashboardBinding
    private lateinit var database: DatabaseReference

    // User info
    private var currentUserId: String = ""
    private var currentUserProfileName: String = ""
    private var currentUserCourse: String = ""
    private val currentCourse: String by lazy {
        intent.getStringExtra("courseName") ?: ""
    }
    private var senderName: String = "Unknown"

    // Data
    private val fileMessages = mutableListOf<FileMessage>()
    private val fileKeys     = mutableListOf<String>()        // <-- NEW
    private lateinit var adapter: FileMessageAdapter

    // For storing the selected file URI from the upload dialog
    private var selectedFileUri: Uri? = null

    // Using the new Activity Result API to pick a file
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            Toast.makeText(this, "File selected", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
        }
    }

    private var partnerList: List<Student> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteSharingDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("currentCourse", currentCourse)

        partnerList = intent.getParcelableArrayListExtra<Student>("partnerList") ?: listOf()

        // Retrieve extras from Intent
        currentUserId = intent.getStringExtra("USER_ID")
            ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(currentUserId)
            .child("name")
            .get()
            .addOnSuccessListener { snapshot ->
                senderName = snapshot.getValue(String::class.java) ?: "Unknown"
            }
        currentUserProfileName = intent.getStringExtra("USER_NAME") ?: ""
        currentUserCourse      = intent.getStringExtra("USER_COURSE") ?: ""

        // Initialize Firebase
        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com/")
            .reference

        // Initialize custom adapter for ListView
        adapter = FileMessageAdapter(
            context      = this,
            fileMessages = fileMessages,
            onDownload   = { fileMsg -> downloadFile(fileMsg) }
        )
        binding.listViewFiles.adapter = adapter

        // Long-press to report
        binding.listViewFiles.setOnItemLongClickListener { _, _, position, _ ->
            showReportDialog(position)
            true
        }

        // Setup FloatingActionButton (plus button) for upload
        binding.fabUpload.setOnClickListener { showUploadDialog() }

        // Back button to exit dashboard
        binding.backButton.setOnClickListener { finish() }

        // Setup search bar filtering
        binding.editTextSearch.addTextChangedListener { text ->
            filterNotes(text.toString())
        }

        // Setup sort spinner (for further extension; not fully implemented here)
        val sortOptions = listOf("Sort by Timestamp", "Sort by Title")
        val spinnerAdapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item,
            sortOptions
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSortBy.adapter = spinnerAdapter

        // Listen for file messages in real time
        listenForFiles()
    }

    // --- REPORT FEATURE BELOW ---

    private fun showReportDialog(position: Int) {
        val reasons = arrayOf(
            "Spam",
            "Hate Speech",
            "Harassment",
            "Inappropriate Content",
            "Other"
        )
        var choiceIndex = 0

        AlertDialog.Builder(this)
            .setTitle("Report File")
            .setSingleChoiceItems(reasons, 0) { _, which ->
                choiceIndex = which
            }
            .setPositiveButton("Report") { _, _ ->
                performReport(position, reasons[choiceIndex])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performReport(position: Int, reason: String) {
        val fileKey = fileKeys.getOrNull(position) ?: return
        val reportsRef = database
            .child("fileReports")
            .child(currentUserCourse)
            .child(fileKey)

        // record this user's reason
        reportsRef.child(currentUserId).setValue(reason)
            .addOnSuccessListener {
                Toast.makeText(this, "Report submitted", Toast.LENGTH_SHORT).show()

                // now check if ≥50% of class has reported
                reportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(rSnap: DataSnapshot) {
                        val reportCount = rSnap.childrenCount.toInt()

                        // count all enrolled in this course:
                        database.child("users")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(uSnap: DataSnapshot) {
                                    var total = 0
                                    for (u in uSnap.children) {
                                        val courses = u.child("courses").children
                                            .mapNotNull { it.getValue(String::class.java) }
                                        if (currentUserCourse in courses) total++
                                    }
                                    // if threshold reached, delete file
                                    if (reportCount * 2 >= total) {
                                        database.child("noteSharing")
                                            .child(currentUserCourse)
                                            .child(fileKey)
                                            .removeValue()
                                            .addOnSuccessListener {
                                                Toast.makeText(
                                                    this@NoteSharingDashboardActivity,
                                                    "File removed due to multiple reports",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                    }
                                }
                                override fun onCancelled(e: DatabaseError) {}
                            })
                    }
                    override fun onCancelled(e: DatabaseError) {}
                })
            }
    }

    // --- END REPORT FEATURE ---

    private fun showUploadDialog() {
        val dialogView = layoutInflater.inflate(R.layout.upload_note_dialog, null)
        val noteTitleEdit   = dialogView.findViewById<EditText>(R.id.editTextNoteTitle)
        val classDateText   = dialogView.findViewById<TextView>(R.id.textViewClassDate)
        val pickFileButton  = dialogView.findViewById<Button>(R.id.buttonPickFile)
        val uploadButton    = dialogView.findViewById<Button>(R.id.buttonUpload)

        // Default date text
        classDateText.text = "Select Class Date"
        classDateText.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this,
                { _, y, m, d ->
                    classDateText.text = String.format(
                        Locale.getDefault(),
                        "%04d-%02d-%02d",
                        y, m + 1, d
                    )
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        pickFileButton.setOnClickListener {
            pickFileLauncher.launch("*/*")
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Upload Note")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        uploadButton.setOnClickListener {
            val title     = noteTitleEdit.text.toString().trim()
            val classDate = classDateText.text.toString().trim()
            if (title.isEmpty() || classDate == "Select Class Date") {
                Toast.makeText(this, "Enter a title and date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedFileUri == null) {
                Toast.makeText(this, "Please pick a file", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val fileName = getFileName(selectedFileUri!!)
            val fileData = readFileAsBase64(selectedFileUri!!)
            if (fileData != null) {
                sendFile(title, classDate, fileName, fileData)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Error processing file", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun getFileName(uri: Uri): String {
        var result = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) result = cursor.getString(idx)
        }
        return result
    }

    private fun readFileAsBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            bytes?.let { Base64.encodeToString(it, Base64.DEFAULT) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun sendFile(
        title: String,
        classDate: String,
        fileName: String,
        fileData: String
    ) {
        val message = FileMessage(
            title      = title,
            classDate  = classDate,
            fileName   = fileName,
            fileData   = fileData,
            timestamp  = System.currentTimeMillis(),
            userId     = currentUserId,
            uploader   = senderName,
            courseName = currentCourse
        )
        database.child("noteSharing")
            .child(currentUserCourse)
            .push().setValue(message)
            .addOnSuccessListener {
                Toast.makeText(this, "File sent", Toast.LENGTH_SHORT).show()
                selectedFileUri = null
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send file", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenForFiles() {
        val course = currentUserCourse
        // load partnerList first (unchanged)
        database.child("study_partner_requests")
            .child(course)
            .child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // now load files
                    database.child("noteSharing").child(course)
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(fileSnapshot: DataSnapshot) {
                                fileMessages.clear()
                                fileKeys.clear()  // <-- reset keys
                                for (snap in fileSnapshot.children) {
                                    val fm = snap.getValue(FileMessage::class.java)
                                        ?: continue
                                    if ((fm.userId == currentUserId ||
                                                partnerList.any { it.id == fm.userId })
                                        && fm.courseName == currentCourse
                                    ) {
                                        fileMessages.add(fm)
                                        fileKeys.add(snap.key ?: "")
                                    }
                                }
                                adapter.notifyDataSetChanged()
                            }
                            override fun onCancelled(e: DatabaseError) {
                                Toast.makeText(
                                    this@NoteSharingDashboardActivity,
                                    "Failed to load files",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }
                override fun onCancelled(e: DatabaseError) {
                    Toast.makeText(
                        this@NoteSharingDashboardActivity,
                        "Failed to load partner data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun filterNotes(query: String) {
        val filtered = fileMessages.filter { fm ->
            fm.title.contains(query, true) ||
                    fm.fileName.contains(query, true) ||
                    fm.uploader.contains(query, true) ||
                    fm.classDate.contains(query, true)
        }
        val display = filtered.map { fm ->
            val t = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(Date(fm.timestamp))
            "${fm.title}\n" +
                    "From: ${fm.uploader}\n" +
                    "File: ${fm.fileName}\n" +
                    "Date: ${fm.classDate}\n" +
                    "Time: $t\n(Tap to preview)"
        }
        binding.listViewFiles.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            display
        )
    }

    private fun downloadFile(fileMessage: FileMessage) {
        try {
            val decoded = Base64.decode(fileMessage.fileData, Base64.DEFAULT)
            val downloads = android.os.Environment
                .getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
            val outFile = File(downloads, fileMessage.fileName)
            outFile.outputStream().use { it.write(decoded) }

            Toast.makeText(this,
                "File downloaded to: ${outFile.absolutePath}",
                Toast.LENGTH_LONG
            ).show()

            val contentUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                outFile
            )
            val ext = fileMessage.fileName.substringAfterLast('.', "")
            val mime = android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(ext.lowercase()) ?: "*/*"

            AlertDialog.Builder(this)
                .setTitle("Download Complete")
                .setMessage("Open this file?")
                .setPositiveButton("Open") { _, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, mime)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                    })
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this,
                "Failed to download file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
