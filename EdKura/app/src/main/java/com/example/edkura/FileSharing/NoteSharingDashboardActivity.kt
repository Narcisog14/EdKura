package com.example.edkura.FileSharing

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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
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
import java.util.Calendar
import com.google.firebase.auth.FirebaseAuth

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
        currentUserId = intent.getStringExtra("USER_ID") ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(currentUserId)
            .child("name")
            .get()
            .addOnSuccessListener { snapshot ->
                senderName = snapshot.getValue(String::class.java) ?: "Unknown"
            }
        currentUserProfileName = intent.getStringExtra("USER_NAME") ?: ""
        currentUserCourse = intent.getStringExtra("USER_COURSE") ?: ""

        // Initialize Firebase
        database = FirebaseDatabase.getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com/").reference

        // Initialize custom adapter for ListView
        adapter = FileMessageAdapter(
            context = this,
            fileMessages = fileMessages,
            onDownload = { fileMsg -> downloadFile(fileMsg) }
        )
        binding.listViewFiles.adapter = adapter

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
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSortBy.adapter = spinnerAdapter

        // Listen for file messages in real time
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
        database.child("noteSharing").child(currentUserCourse).push().setValue(message)
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
        // Step 1: 读取当前用户的 partnerList
        database.child("study_partner_requests").child(course).child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val partnerId = partnerList.joinToString("\n") { student ->
                        "id: ${student.id}"
                    }
                    // Step 2: 读取文件数据
                    database.child("noteSharing").child(course)
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(fileSnapshot: DataSnapshot) {
                                fileMessages.clear()
                                val displayStrings = mutableListOf<String>()
                                Log.d("PartnerInfo", partnerId)

                                for (snapshot in fileSnapshot.children) {
                                    val fileMessage = snapshot.getValue(FileMessage::class.java) ?: continue
                                    val uploaderId = fileMessage.userId

                                    // 展示条件：1. 自己上传的；2. partner上传的 3.firebase课程是当前课程
                                    //Display conditions:1. Uploaded by yourself; 2. 3. The firebase course is the current course
                                    if ((uploaderId == currentUserId || partnerId.contains(uploaderId)) && fileMessage.courseName == currentCourse) {
                                        Log.d("FileMessage courseName", fileMessage.courseName)
                                        fileMessages.add(fileMessage)

                                        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                            .format(Date(fileMessage.timestamp))
                                        val displayText = "${fileMessage.title}\n" +
                                                "From: ${fileMessage.uploader}\n" +
                                                "File: ${fileMessage.fileName}\n" +
                                                "Date: ${fileMessage.classDate}\n" +
                                                "Time: $time\n(Tap to preview)"
                                        displayStrings.add(displayText)
                                    }
                                }

                                adapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@NoteSharingDashboardActivity, "Failed to load files", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@NoteSharingDashboardActivity, "Failed to load partner data", Toast.LENGTH_SHORT).show()
                }
            })

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