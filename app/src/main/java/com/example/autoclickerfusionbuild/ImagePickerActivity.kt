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
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

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
                analyzeImageWithMLKit(it, this)
//                val fileName = getFileName(it) // Récupère le nom du fichier
//                copyToClipboard(fileName) // Copie dans le presse-papiers

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

    //pas testée
//    private fun analyzeImageWithMLKit(imageUri: Uri, context: Context) {
//        val inputImage = InputImage.fromFilePath(context, imageUri)
//        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//
//        recognizer.process(inputImage)
//            .addOnSuccessListener { visionText ->
//                val extractedText = visionText.text
//                Log.d("OCR_RESULT", "Texte brut extrait :\n$extractedText")
//
//                val cardNames = mutableListOf<String>()
//
//                // On parcourt chaque bloc de texte détecté
//                for (block in visionText.textBlocks) {
//                    val rect = block.boundingBox // 📍 Récupérer la position du texte
//                    if (rect != null) {
//                        Log.d("OCR_BLOCK", "Texte détecté : ${block.text} | Position : $rect")
//
//                        // 📌 Filtrer uniquement les textes situés dans le haut de l'image (ex: premier tiers)
//                        val imageHeight = inputImage.height
//                        if (rect.top < imageHeight * 0.3) { // Seulement le premier tiers haut
//                            cardNames.add(block.text)
//                        }
//                    }
//                }
//
//                Log.d("FILTERED_CARDS", "Noms de cartes détectés :\n$cardNames")
//
//                if (cardNames.isNotEmpty()) {
//                    copyToClipboard(context, cardNames.joinToString("\n")) // Copier avec des retours à la ligne
//                } else {
//                    Log.w("FILTERED_CARDS", "Aucun nom de carte détecté !")
//                }
//            }
//            .addOnFailureListener { e ->
//                Log.e("OCR_ERROR", "Erreur lors de la reconnaissance du texte", e)
//            }
//    }


    //récupère tout le texte présent sur la photo
//    private fun analyzeImageWithMLKit(imageUri: Uri, context: Context) {
//        val inputImage = InputImage.fromFilePath(context, imageUri)
//        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//
//        recognizer.process(inputImage)
//            .addOnSuccessListener { visionText ->
//                val extractedText = visionText.text
//                val cardNames = filterYuGiOhCardNames(extractedText) // 🔥 Filtrage des noms de cartes
//                copyToClipboard(context, cardNames)
//            }
//            .addOnFailureListener { e ->
//                e.printStackTrace()
//            }
//    }

    private fun analyzeImageWithMLKit(imageUri: Uri, context: Context) {
        val inputImage = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                Log.d("OCR_RESULT", "Texte brut extrait :\n$extractedText")

                val textBlocks = visionText.textBlocks

                // Étape 1 : Regrouper les blocs en fonction de leur proximité horizontale
                val cardClusters = mutableListOf<MutableList<Text.TextBlock>>()

                for (block in textBlocks) {
                    val rect = block.boundingBox
                    if (rect != null) {
                        var added = false

                        // Vérifier si ce bloc appartient à un cluster existant
                        for (cluster in cardClusters) {
                            val firstBlock = cluster.first().boundingBox
                            if (firstBlock != null && Math.abs(rect.left - firstBlock.left) < 100) {
                                cluster.add(block)
                                added = true
                                break
                            }
                        }

                        // Si aucun cluster correspondant, créer un nouveau cluster
                        if (!added) {
                            cardClusters.add(mutableListOf(block))
                        }
                    }
                }

                // Étape 2 : Extraire le texte du bloc le plus haut de chaque cluster (nom de carte)
                val cardNames = mutableListOf<String>()
                for (cluster in cardClusters) {
                    val topBlock = cluster.minByOrNull { it.boundingBox?.top ?: Int.MAX_VALUE }
                    topBlock?.let {
                        Log.d("CARD_DETECTION", "Carte détectée : ${it.text}")
                        cardNames.add(it.text)
                    }
                }

                Log.d("FILTERED_CARDS", "Noms de cartes détectés :\n$cardNames")

                if (cardNames.isNotEmpty()) {
                    copyToClipboard(context, cardNames.joinToString("\n")) // Copier avec des retours à la ligne
                } else {
                    Log.w("FILTERED_CARDS", "Aucun nom de carte détecté !")
                }
            }
            .addOnFailureListener { e ->
                Log.e("OCR_ERROR", "Erreur lors de la reconnaissance du texte", e)
            }
    }


//    private fun filterYuGiOhCardNames(text: String): String {
//        val knownCards = listOf("Dark Magician", "Blue-Eyes White Dragon", "Red-Eyes Black Dragon") // 📌 Ajoute d'autres noms
////        return text.lines()
////            .map { it.trim() }
////            .filter { line -> knownCards.any { card -> line.contains(card, ignoreCase = true) } }
////            .joinToString("\n")
//        return text
//    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("YuGiOh Cards", text)
        clipboard.setPrimaryClip(clip)
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
