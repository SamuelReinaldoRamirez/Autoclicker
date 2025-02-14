package com.example.autoclickerfusionbuild

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream

class ImagePickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Échec de l'initialisation d'OpenCV")
        } else {
            Log.d("OpenCV", "OpenCV initialisé avec succès")
        }

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
                val enhancedBitmap = enhanceImage(it, this)
                val enhancedImageUri = saveBitmapToCache(enhancedBitmap, this) // On enregistre l’image améliorée
                if (enhancedImageUri != null) {
                    analyzeImageWithMLKit(enhancedImageUri, this)
                } else {
                    Log.e("ImageProcessing", "Échec de la sauvegarde de l'image améliorée")
                }

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

//    private fun saveBitmapToCache(bitmap: Bitmap, context: Context): Uri {
//        val cachePath = File(context.cacheDir, "images")
//        cachePath.mkdirs() // Crée le dossier si besoin
//
//        val file = File(cachePath, "enhanced_image.png")
//        FileOutputStream(file).use { out ->
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // Compression sans perte
//        }
//
//        return FileProvider.getUriForFile(
//            context,
//            "${context.packageName}.provider", // Assure-toi d’avoir un FileProvider configuré dans ton Manifest !
//            file
//        )
//    }


    private fun saveBitmapToCache(bitmap: Bitmap, context: Context): Uri? {
        val cachePath = File(context.cacheDir, "images")
        if (!cachePath.exists()) {
            val created = cachePath.mkdirs()
            Log.d("ImageProcessing", "Dossier cache créé : $created")
        }

        val file = File(cachePath, "enhanced_image.png")

        return try {
            FileOutputStream(file).use { out ->
                val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                Log.d("ImageProcessing", "Compression réussie : $success")
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            Log.d("ImageProcessing", "Uri de l'image enregistrée : $uri")
            uri
        } catch (e: Exception) {
            Log.e("ImageProcessing", "Erreur lors de l'enregistrement de l'image : ${e.message}")
            null
        }
    }


    fun enhanceImage(imageUri: Uri, context: Context): Bitmap {
        Log.d("ImageProcessing", "Début du traitement de l'image")

        // Charger l'image en Bitmap
        val inputBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        Log.d("ImageProcessing", "Image chargée depuis Uri")

        // Convertir en Mat (format OpenCV)
        val src = Mat()
        Utils.bitmapToMat(inputBitmap, src)
        Log.d("ImageProcessing", "Conversion en Mat réussie")

        // Convertir en niveaux de gris
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        Log.d("ImageProcessing", "Conversion en niveaux de gris réussie")

        // Appliquer un filtre de netteté (Sharpening)
        val sharpened = Mat()
        val kernel = Mat(3, 3, CvType.CV_32F)
        kernel.put(
            0, 0,  -1.0, -1.0, -1.0,
            -1.0,  9.0, -1.0,
            -1.0, -1.0, -1.0
        )
        Imgproc.filter2D(gray, sharpened, -1, kernel)
        Log.d("ImageProcessing", "Filtre de netteté appliqué")

        // Appliquer CLAHE (Contrast Limited Adaptive Histogram Equalization)
        val clahe = Imgproc.createCLAHE()
        clahe.clipLimit = 4.0
        val enhanced = Mat()
        clahe.apply(sharpened, enhanced)
        Log.d("ImageProcessing", "CLAHE appliqué")

        // Convertir le Mat OpenCV en Bitmap
        val outputBitmap = Bitmap.createBitmap(enhanced.cols(), enhanced.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(enhanced, outputBitmap)
        Log.d("ImageProcessing", "Image traitée et convertie en Bitmap")

        return outputBitmap
    }



//    fun enhanceImage(imageUri: Uri, context: Context): Bitmap {
//        // Charger l'image en Bitmap
//        val inputBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
//
//        // Convertir en Mat (format OpenCV)
//        val src = Mat()
//        Utils.bitmapToMat(inputBitmap, src)
//
//        // Convertir en niveaux de gris
//        val gray = Mat()
//        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
//
//        // Appliquer un filtre de netteté (Sharpening)
//        val sharpened = Mat()
//        val kernel = Mat(3, 3, CvType.CV_32F)
//        kernel.put(
//            0, 0,  -1.0, -1.0, -1.0,
//            -1.0,  9.0, -1.0,
//            -1.0, -1.0, -1.0
//        )
//        Imgproc.filter2D(gray, sharpened, -1, kernel)
//
//        // Appliquer CLAHE (Contrast Limited Adaptive Histogram Equalization)
//        val clahe = Imgproc.createCLAHE()
//        clahe.clipLimit = 4.0
//        val enhanced = Mat()
//        clahe.apply(sharpened, enhanced)
//
//        // Convertir le Mat OpenCV en Bitmap
//        val outputBitmap = Bitmap.createBitmap(enhanced.cols(), enhanced.rows(), Bitmap.Config.ARGB_8888)
//        Utils.matToBitmap(enhanced, outputBitmap)
//
//        return outputBitmap
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


    //marche bien
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
//            val selectedImageUri: Uri? = data?.data
//            selectedImageUri?.let {
//                analyzeImageWithMLKit(it, this)
////                val fileName = getFileName(it) // Récupère le nom du fichier
////                copyToClipboard(fileName) // Copie dans le presse-papiers
//
//                sendBroadcast(Intent(ACTION_IMAGE_SELECTED).apply {
//                    putExtra(EXTRA_IMAGE_URI, it.toString())
//                })
//            }
//        }
//
//        // Réaffiche l'overlay avant de fermer l'activité
//        val serviceIntent = Intent(this, OverlayService::class.java)
//        serviceIntent.action = "SHOW_OVERLAY"
//        startService(serviceIntent)
//
//        finish() // Ferme l'activité après sélection
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
