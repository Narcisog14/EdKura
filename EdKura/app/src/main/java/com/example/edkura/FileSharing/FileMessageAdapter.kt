package com.example.edkura.FileSharing

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.edkura.R
import java.io.File
import com.example.edkura.FileSharing.FileMessage
import com.example.edkura.FileSharing.NoteSharingDashboardActivity

class FileMessageAdapter(
    private val context: Context,
    private val fileMessages: List<FileMessage>,
    private val onDownload: (FileMessage) -> Unit
) : BaseAdapter() {

    override fun getCount() = fileMessages.size
    override fun getItem(position: Int) = fileMessages[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ViewHolder
        val rowView: View

        if (convertView == null) {
            rowView = LayoutInflater.from(context)
                .inflate(R.layout.item_file_message, parent, false)
            holder = ViewHolder(rowView)
            rowView.tag = holder
        } else {
            rowView = convertView
            holder = rowView.tag as ViewHolder
        }

        val fileMsg = getItem(position)
        // Title (the user-supplied note title)
        holder.textTitle.text = "Title: ${fileMsg.title}"
        // Infer file format from fileName extension
        val fileFormat = fileMsg.fileName.substringAfterLast('.', "Unknown")
        holder.textFileFormat.text = "File Format: $fileFormat"
        // Class date (from fileMsg.classDate)
        holder.textDate.text = "Date: ${fileMsg.classDate}"
        // Uploader name
        holder.textUploader.text = "From: ${fileMsg.uploader}"

        // Button
        holder.buttonDownload.setOnClickListener {
            onDownload(fileMsg)
        }

        return rowView
    }

    private class ViewHolder(view: View) {
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textFileFormat: TextView = view.findViewById(R.id.textFileFormat)
        val textDate: TextView = view.findViewById(R.id.textDate)
        val textUploader: TextView = view.findViewById(R.id.textUploader)
        val buttonDownload: Button = view.findViewById(R.id.buttonDownload)
    }
}
