package com.example.filesharing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

data class FileMessage(
    val sender: String = "",
    val receiver: String = "",
    val fileName: String = "",
    val fileData: String = "",
    val timestamp: Long = 0
)

class MainActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private val RC_PICK_FILE = 101

    // Default users
    private var currentUser = "User1"
    private var otherUser = "User2"

    private lateinit var tvCurrentUser: TextView
    private lateinit var spinnerUser: Spinner
    private lateinit var btnPickFile: Button
    private lateinit var listViewMessages: ListView

    // NEW: Store the actual FileMessage objects
    private val fileMessages = ArrayList<FileMessage>()

    // We still need a parallel list of strings to display in the ListView
    private val displayList = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvCurrentUser = findViewById(R.id.tvCurrentUser)
        spinnerUser = findViewById(R.id.spinnerUser)
        btnPickFile = findViewById(R.id.btnPickFile)
        listViewMessages = findViewById(R.id.listViewMessages)

        // Setup spinner with two predefined users
        val users = listOf("User1", "User2")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, users)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUser.adapter = spinnerAdapter

        spinnerUser.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                currentUser = users[position]
                otherUser = if (currentUser == "User1") "User2" else "User1"
                tvCurrentUser.text = "Current User: $currentUser"

                // Reload the incoming files for the newly selected user
                listenForIncomingFiles()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnPickFile.setOnClickListener {
            pickFile()
        }

        // Use the displayList for the ListView
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listViewMessages.adapter = adapter

        // NEW: Set up an item click listener to "download" the file
        listViewMessages.setOnItemClickListener { _, _, position, _ ->
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
            .getInstance("https://file-sharing-e1c97-default-rtdb.firebaseio.com/")
            .reference
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(Intent.createChooser(intent, "Select a file"), RC_PICK_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_PICK_FILE && resultCode == Activity.RESULT_OK && data != null) {
            val fileUri: Uri? = data.data
            if (fileUri != null) {
                val fileName = getFileName(fileUri)
                val fileData = readFileAsBase64(fileUri)
                if (fileData != null) {
                    sendFile(fileName, fileData)
                }
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
        return result
    }

    // Read file bytes and convert to a Base64-encoded string
    private fun readFileAsBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                Base64.encodeToString(bytes, Base64.DEFAULT)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Create a FileMessage object and push it to Firebase
    private fun sendFile(fileName: String, fileData: String) {
        val message = FileMessage(
            sender = currentUser,
            receiver = otherUser,
            fileName = fileName,
            fileData = fileData,
            timestamp = System.currentTimeMillis()
        )
        database.child("messages").push().setValue(message)
            .addOnSuccessListener {
                Toast.makeText(this, "File sent", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send file", Toast.LENGTH_SHORT).show()
            }
    }

    // Listen for incoming file messages where the receiver matches the current user
    private fun listenForIncomingFiles() {
        database.child("messages")
            .orderByChild("receiver")
            .equalTo(currentUser)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Clear both lists
                    fileMessages.clear()
                    displayList.clear()

                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(FileMessage::class.java)
                        if (message != null) {
                            fileMessages.add(message)

                            // Prepare a display string for the ListView
                            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                .format(Date(message.timestamp))
                            val displayText = """
                                From: ${message.sender}
                                File: ${message.fileName}
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
                        this@MainActivity,
                        "Failed to load messages",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // NEW: Decode and save the file locally
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

            // OPTIONAL: You could also launch an ACTION_VIEW intent here to open the file
            // openFile(outFile)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to download file", Toast.LENGTH_SHORT).show()
        }
    }

    // OPTIONAL: Method to open the file in another app (requires a FileProvider & Manifest config)
    /*
    private fun openFile(file: File) {
        val fileUri = FileProvider.getUriForFile(
            this,
            "${BuildConfig.APPLICATION_ID}.provider", // or your custom authority
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = fileUri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = getMimeType(file) // a helper to guess the MIME type from extension
        }
        startActivity(Intent.createChooser(intent, "Open file with"))
    }
    */
}
