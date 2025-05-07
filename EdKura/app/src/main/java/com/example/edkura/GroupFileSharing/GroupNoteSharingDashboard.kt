package com.example.edkura.GroupFileSharing

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import com.example.edkura.FileSharing.FileMessage
import com.example.edkura.FileSharing.FileMessageAdapter
import com.example.edkura.Narciso.Student
import com.example.edkura.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.edkura.databinding.ActivityNoteSharingDashboardBinding
import com.example.edkura.models.ProjectGroup
import java.util.Calendar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GroupNoteSharingDashboard : AppCompatActivity() {

    private lateinit var binding: ActivityNoteSharingDashboardBinding
    private lateinit var database: DatabaseReference
    private lateinit var dashboardTitle: TextView

    // User info
    private var currentUserId: String = ""
    private var groupId: String = ""
    private var currentUserCourse: String = ""
    private val currentCourse: String by lazy {
        intent.getStringExtra("courseName") ?: ""
    }
    private var senderName: String = "Unknown"

    // Data
    private val fileMessages = mutableListOf<FileMessage>()
    private val fileKeys     = mutableListOf<String>()    // <-- new
    private lateinit var adapter: FileMessageAdapter

    // For upload
    private var selectedFileUri: Uri? = null
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
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

        dashboardTitle = findViewById(R.id.dashboardTitle)
        dashboardTitle.text = "Group Notes"

        partnerList = intent.getParcelableArrayListExtra("partnerList") ?: listOf()
        groupId      = intent.getStringExtra("GROUP_ID")    ?: ""
        currentUserId = intent.getStringExtra("USER_ID")
            ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        currentUserCourse = intent.getStringExtra("USER_COURSE") ?: ""

        // load my name
        FirebaseDatabase.getInstance().reference
            .child("users").child(currentUserId).child("name")
            .get().addOnSuccessListener { snap ->
                senderName = snap.getValue(String::class.java) ?: "Unknown"
            }

        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com/")
            .reference

        adapter = FileMessageAdapter(
            context      = this,
            fileMessages = fileMessages,
            onDownload   = { fm -> downloadFile(fm) }
        )
        binding.listViewFiles.adapter = adapter

        // long-press for report
        binding.listViewFiles.setOnItemLongClickListener { _, _, pos, _ ->
            showReportDialog(pos)
            true
        }

        binding.fabUpload.setOnClickListener { showUploadDialog() }
        binding.backButton.setOnClickListener { finish() }
        binding.editTextSearch.addTextChangedListener { filterNotes(it.toString()) }

        // sort spinner (unmodified)
        val sortOptions = listOf("Sort by Timestamp","Sort by Title")
        val spinnerAdpt = ArrayAdapter(this,
            android.R.layout.simple_spinner_item,
            sortOptions
        )
        spinnerAdpt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSortBy.adapter = spinnerAdpt

        listenForFiles()
    }

    private fun showUploadDialog() {
        val dialogView = layoutInflater.inflate(R.layout.upload_note_dialog, null)
        val noteTitleEdit = dialogView.findViewById<EditText>(R.id.editTextNoteTitle)
        val classDateText = dialogView.findViewById<TextView>(R.id.textViewClassDate)
        val pickFileButton = dialogView.findViewById<Button>(R.id.buttonPickFile)
        val uploadButton = dialogView.findViewById<Button>(R.id.buttonUpload)

        // Default date text
        classDateText.text = "Select Class Date"
        classDateText.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this,
                { _, year, month, dayOfMonth ->
                    val selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    classDateText.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
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
            val title = noteTitleEdit.text.toString().trim()
            val classDate = classDateText.text.toString().trim()
            if (title.isEmpty() || classDate == "Select Class Date") {
                Toast.makeText(this, "Enter a title and select a date", Toast.LENGTH_SHORT).show()
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
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                result = cursor.getString(nameIndex)
            }
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

    private fun sendFile(title: String, classDate: String, fileName: String, fileData: String ) {
        val message = FileMessage(
            title = title,
            classDate = classDate,
            fileName = fileName,
            fileData = fileData,
            timestamp = System.currentTimeMillis(),
            userId = currentUserId ,
            uploader = senderName,
            courseName = currentCourse
        )
        database.child("GroupNoteSharing").child(currentUserCourse).push().setValue(message)
            .addOnSuccessListener {
                Toast.makeText(this, "File sent", Toast.LENGTH_SHORT).show()
                selectedFileUri = null
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send file", Toast.LENGTH_SHORT).show()
            }
    }


    private fun listenForFiles() {
        // first load group members
        database.child("projectGroups").child(groupId).child("members")
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(ms: DataSnapshot) {
                    val members = ms.children.mapNotNull { it.key }.toSet()
                    // then load files
                    database.child("GroupNoteSharing").child(currentUserCourse)
                        .addValueEventListener(object: ValueEventListener {
                            override fun onDataChange(fs: DataSnapshot) {
                                fileMessages.clear()
                                fileKeys.clear()
                                for (snap in fs.children) {
                                    val fm = snap.getValue(FileMessage::class.java) ?: continue
                                    if ((fm.userId == currentUserId || members.contains(fm.userId))
                                        && fm.courseName == currentCourse) {
                                        fileMessages.add(fm)
                                        fileKeys.add(snap.key ?: "")
                                    }
                                }
                                adapter.notifyDataSetChanged()
                            }
                            override fun onCancelled(e: DatabaseError) {
                                Toast.makeText(this@GroupNoteSharingDashboard,
                                    "Failed loading files",Toast.LENGTH_SHORT).show()
                            }
                        })
                }
                override fun onCancelled(e: DatabaseError) {
                    Toast.makeText(this@GroupNoteSharingDashboard,
                        "Failed loading group members",Toast.LENGTH_SHORT).show()
                }
            })
    }

    // --- REPORT FEATURE ---
    private fun showReportDialog(position: Int) {
        val reasons = arrayOf(
            "Spam","Hate Speech","Harassment","Inappropriate Content","Other"
        )
        var choice = 0
        AlertDialog.Builder(this)
            .setTitle("Report File")
            .setSingleChoiceItems(reasons,0) { _, which -> choice = which }
            .setPositiveButton("Report") { _,_ ->
                performReport(position, reasons[choice])
            }
            .setNegativeButton("Cancel",null)
            .show()
    }

    private fun performReport(position: Int, reason: String) {
        val key = fileKeys.getOrNull(position) ?: return
        val reportsRef = database
            .child("fileReports")
            .child(currentUserCourse)
            .child(key)

        // log my report
        reportsRef.child(currentUserId).setValue(reason)
            .addOnSuccessListener {
                Toast.makeText(this,"Report submitted",Toast.LENGTH_SHORT).show()
                // now check threshold
                reportsRef.addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(rSnap: DataSnapshot) {
                        val count = rSnap.childrenCount.toInt()
                        // count enrolled students
                        database.child("users")
                            .addListenerForSingleValueEvent(object: ValueEventListener {
                                override fun onDataChange(uSnap: DataSnapshot) {
                                    var total = 0
                                    for (u in uSnap.children) {
                                        val courses = u.child("courses").children
                                            .mapNotNull { it.getValue(String::class.java) }
                                        if (currentUserCourse in courses) total++
                                    }
                                    // remove if ≥50% reported
                                    if (count * 2 >= total) {
                                        database.child("GroupNoteSharing")
                                            .child(currentUserCourse)
                                            .child(key)
                                            .removeValue()
                                            .addOnSuccessListener {
                                                Toast.makeText(this@GroupNoteSharingDashboard,
                                                    "Removed due to multiple reports",
                                                    Toast.LENGTH_LONG).show()
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

    private fun filterNotes(query: String) {
        val filtered = fileMessages.filter { fileMsg ->
            fileMsg.title.contains(query, ignoreCase = true) ||
                    fileMsg.fileName.contains(query, ignoreCase = true) ||
                    fileMsg.uploader.contains(query, ignoreCase = true) ||
                    fileMsg.classDate.contains(query, ignoreCase = true)
        }
        val displayStrings = filtered.map { fileMsg ->
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(Date(fileMsg.timestamp))
            "${fileMsg.title}\n" +
                    "From: ${fileMsg.uploader}\n" +
                    "File: ${fileMsg.fileName}\n" +
                    "Date: ${fileMsg.classDate}\n" +
                    "Time: $time\n(Tap to preview)"
        }
        val newAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayStrings)
        binding.listViewFiles.adapter = newAdapter
    }


    private fun downloadFile(fileMessage: FileMessage) {
        try {
            // Decode the Base64-encoded file data
            val decodedBytes = Base64.decode(fileMessage.fileData, Base64.DEFAULT)

            // Get the public Downloads folder
            val downloadsFolder = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )

            // Create output file in the Downloads folder
            val outFile = File(downloadsFolder, fileMessage.fileName)
            outFile.outputStream().use { it.write(decodedBytes) }

            Toast.makeText(this, "File downloaded to: ${outFile.absolutePath}", Toast.LENGTH_LONG).show()

            // Get a content URI using FileProvider
            val contentUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                outFile
            )

            // Determine MIME type from file extension
            val extension = fileMessage.fileName.substringAfterLast('.', "")
            val mimeType = android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"

            // Prompt user to open file
            AlertDialog.Builder(this)
                .setTitle("Download Complete")
                .setMessage("Do you want to open this file?")
                .setPositiveButton("Open") { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, mimeType)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "No application found to open this file", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to download file", Toast.LENGTH_SHORT).show()
        }
    }
}