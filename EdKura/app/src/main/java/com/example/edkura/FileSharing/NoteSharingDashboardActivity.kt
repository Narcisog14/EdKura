package com.example.edkura.FileSharing

import android.R
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.edkura.FileSharing.FileMessage
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

class NoteSharingDashboardActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private val RC_PICK_FILE = 101

    private lateinit var binding: ActivityNoteSharingDashboardBinding

    // Store the actual FileMessage objects
    private val fileMessages = ArrayList<FileMessage>()

    // Parallel list of strings for the ListView
    private val displayList = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    // Get the name and id of the user
    private var currentUserId: String = ""
    private var currentUserProfileName: String = ""
    private var currentUserCourse: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteSharingDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get the extras from the intent
        currentUserId = intent.getStringExtra("USER_ID") ?: ""
        currentUserProfileName = intent.getStringExtra("USER_NAME") ?: ""
        currentUserCourse = intent.getStringExtra("USER_COURSE") ?: ""

        // Initialize the button using the binding
        binding.btnPickFile.setOnClickListener {
            pickFile()
        }

        // Use the displayList for the ListView
        adapter = ArrayAdapter(this, R.layout.simple_list_item_1, displayList)
        binding.listViewFiles.adapter = adapter

        // Set up an item click listener to "download" the file
        binding.listViewFiles.setOnItemClickListener { _, _, position, _ ->
            val selectedMessage = fileMessages[position]
            if (selectedMessage.fileData.isNotEmpty()) {
                // Decode and save to local storage
                downloadFile(selectedMessage)
            } else {
                Toast.makeText(this, "No file data found", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize Firebase Database using your URL
        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com/")
            .reference

        listenForFiles()

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" //Corrected type
        startActivityForResult(Intent.createChooser(intent, "Select a file"), RC_PICK_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_PICK_FILE && resultCode == RESULT_OK && data != null) {
            val fileUri: Uri? = data.data
            if (fileUri != null) {
                Log.d("FileSelection", "File URI: $fileUri")
                val fileName = getFileName(fileUri)
                val fileData = readFileAsBase64(fileUri)
                if (fileData != null) {
                    sendFile(fileName, fileData)
                }
            } else {
                Log.e("FileSelection", "fileUri is null")
            }
        }
    }

    // Get file name from Uri using content resolver
    private fun getFileName(uri: Uri): String {
        var result = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                result = cursor.getString(nameIndex)
            }
        }
        Log.d("getFileName", "File Name: $result")
        return result
    }

    // Read file bytes and convert to a Base64-encoded string
    private fun readFileAsBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                val encoded = Base64.encodeToString(bytes, Base64.DEFAULT)
                Log.d("readFileAsBase64", "Encoded data: $encoded")
                return encoded
            } else {
                Log.e("readFileAsBase64", "Bytes are null")
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("readFileAsBase64", "Error reading file", e)
            return null
        }
    }

    // Create a FileMessage object and push it to Firebase
    private fun sendFile(fileName: String, fileData: String) {
        val message = FileMessage(
            uploader = currentUserProfileName, // Send user name
            fileName = fileName,
            fileData = fileData,
            timestamp = System.currentTimeMillis(),
            userId = currentUserId // Send user ID
        )
        database.child("courses").child(currentUserCourse).push().setValue(message)
            .addOnSuccessListener {
                Log.d("SendFile", "File sent successfully")
                Toast.makeText(this, "File sent", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Log.e("SendFile", "Failed to send file", it)
                Toast.makeText(this, "Failed to send file", Toast.LENGTH_SHORT).show()
            }
    }

    // Listen for incoming file messages where the receiver matches the current user
    private fun listenForFiles() {
        database.child("courses").child(currentUserCourse)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Clear both lists
                    fileMessages.clear()
                    displayList.clear()

                    for (fileSnapshot in snapshot.children) {
                        val fileMessage = fileSnapshot.getValue(FileMessage::class.java)
                        if (fileMessage != null) {
                            fileMessages.add(fileMessage)

                            // Prepare a display string for the ListView
                            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                .format(Date(fileMessage.timestamp))
                            val displayText = """
                                From: ${fileMessage.uploader}
                                File: ${fileMessage.fileName}
                                Time: $time
                                (Tap to download)
                            """.trimIndent()

                            displayList.add(displayText)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@NoteSharingDashboardActivity,
                        "Failed to load files",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // Decode and save the file locally
    private fun downloadFile(fileMessage: FileMessage) {
        try {
            // Decode Base64
            val decodedBytes = Base64.decode(fileMessage.fileData, Base64.DEFAULT)

            // Save the file to app-specific external storage (visible to the user but only removable by uninstall)
            val outFile = File(getExternalFilesDir(null), fileMessage.fileName)
            outFile.outputStream().use { it.write(decodedBytes) }

            Toast.makeText(
                this,
                "File saved to: ${outFile.absolutePath}",
                Toast.LENGTH_LONG
            ).show()

            // OPTIONAL: You could also launch an intent to open the saved file
        } catch (e: Exception) {
            // Handle exceptions (e.g., file system errors)
            e.printStackTrace()
            Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show()
        }
    }
}