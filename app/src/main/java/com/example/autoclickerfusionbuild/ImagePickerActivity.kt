package com.example.autoclickerfusionbuild

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class ImagePickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        )


        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let {
                val fileName = getFileName(it) // Récupère le nom du fichier
                copyToClipboard(fileName) // Copie dans le presse-papiers

                sendBroadcast(Intent(ACTION_IMAGE_SELECTED).apply {
                    putExtra(EXTRA_IMAGE_URI, it.toString())
                })
            }
        }

        // Réaffiche l'overlay avant de fermer l'activité
        val serviceIntent = Intent(this, OverlayService::class.java)
        serviceIntent.action = "SHOW_OVERLAY"
        startService(serviceIntent)

        finish() // Ferme l'activité après sélection
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "image_selected.jpg" // Nom par défaut si rien n'est trouvé
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    fileName = it.getString(index)
                }
            }
        }
        return fileName
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("File Name", text)
        clipboard.setPrimaryClip(clip)
    }


    companion object {
        const val REQUEST_CODE_PICK_IMAGE = 1001
        const val ACTION_IMAGE_SELECTED = "com.example.IMAGE_SELECTED"
        const val EXTRA_IMAGE_URI = "image_uri"
    }
}
