package com.example.edkura.Narciso

data class FileMessage(
    var uploader: String = "", // Who sent the file
    var fileName: String = "", // The name of the file
    var fileData: String = "", // The Base64-encoded file content
    var timestamp: Long = 0, // When the file was sent
    var userId: String = "" // The ID of the user who sent the file
)