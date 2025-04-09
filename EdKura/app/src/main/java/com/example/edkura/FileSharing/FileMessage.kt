package com.example.edkura.FileSharing

data class FileMessage(
    var title: String = "",         // New: Title of the note
    var classDate: String = "",     // New: Date of the class the note is for
    var uploader: String = "",      // Uploader's name
    var fileName: String = "",      // File name
    var fileData: String = "",      // Base64-encoded file content
    var timestamp: Long = 0,        // When the file was sent
    var userId: String = ""         // ID of the uploader
)