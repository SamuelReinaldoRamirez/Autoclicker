package com.example.autoclickerfusionbuild

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Rect
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import android.graphics.Bitmap
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import java.io.FileOutputStream
import java.io.IOException


class DifferenceImagePickerActivity : AppCompatActivity(){

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


    private fun exergueCards(imageUri: Uri, context: Context) {

        val inputBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        val src = Mat()
        Utils.bitmapToMat(inputBitmap, src)

        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2BGR)
        saveMatDirectly(src, "0_original", context)

        // 🔹 Convertir en HSV
        val hsv = Mat()
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV)
        saveMatDirectly(hsv, "1_BGRHSV", context)

        // Mask 2 : Plus large vers des teintes bleu-vert
//        val lowerBlue1 = Scalar(78.0, 40.0, 80.0)
        val lowerBlue1 = Scalar(90.0, 100.0, 50.0)
        val upperBlue1 = Scalar(150.0, 255.0, 255.0) // Plus large vers les bleus violets

        // Mask 2 : Bleu standard
        val lowerBlue2 = Scalar(78.0, 40.0, 80.0)
        val upperBlue2 = Scalar(150.0, 255.0, 255.0)

        // Mask 3 : Plus centré sur les teintes de bleu clair
        val lowerBlue3 = Scalar(80.0, 90.0, 60.0)
        val upperBlue3 = Scalar(130.0, 255.0, 255.0)

//        // Mask 2 : Bleu standard
//        val lowerBlue2 = Scalar(78.0, 40.0, 80.0)
//        val upperBlue2 = Scalar(150.0, 255.0, 255.0)
//
//        // Mask 3 : Plus centré sur les teintes de bleu clair
//        val lowerBlue3 = Scalar(90.0, 40.0, 80.0)
//        val upperBlue3 = Scalar(130.0, 255.0, 255.0)

        // Mask 4 : Inclut des tons de bleu-vert plus foncés et saturés
        val lowerBlue4 = Scalar(78.0, 50.0, 70.0)
        val upperBlue4 = Scalar(145.0, 255.0, 255.0)

        // 🔹 Création des masques pour chaque plage définie
        val maskBlue1 = Mat()
        Core.inRange(hsv, lowerBlue1, upperBlue1, maskBlue1)

        val maskBlue2 = Mat()
        Core.inRange(hsv, lowerBlue2, upperBlue2, maskBlue2)

        val maskBlue3 = Mat()
        Core.inRange(hsv, lowerBlue3, upperBlue3, maskBlue3)

        val maskBlue4 = Mat()
        Core.inRange(hsv, lowerBlue4, upperBlue4, maskBlue4)

        // 🔹 Application des masques "only_blue"
        val onlyBlue1 = Mat()
        Core.bitwise_and(src, src, onlyBlue1, maskBlue1)
        saveMatDirectly(onlyBlue1, "2_only_blue_mask1", context)
        detectAndDrawRectanglesOnMat(onlyBlue1, context, "2_only_blue_mask1", src)
//        detectAndDrawRectanglesOnMat(onlyBlue1, context, "2_only_blue_mask1")

        val onlyBlue2 = Mat()
        Core.bitwise_and(src, src, onlyBlue2, maskBlue2)
        saveMatDirectly(onlyBlue2, "3_only_blue_mask2", context)
        detectAndDrawRectanglesOnMat(onlyBlue2, context, "3_only_blue_mask2", src)
//        detectAndDrawRectanglesOnMat(onlyBlue2, context, "3_only_blue_mask2")

        val onlyBlue3 = Mat()
        Core.bitwise_and(src, src, onlyBlue3, maskBlue3)
        saveMatDirectly(onlyBlue3, "4_only_blue_mask3", context)
        detectAndDrawRectanglesOnMat(onlyBlue3, context, "4_only_blue_mask3", src)
//        detectAndDrawRectanglesOnMat(onlyBlue3, context, "4_only_blue_mask3")


        val onlyBlue4 = Mat()
        Core.bitwise_and(src, src, onlyBlue4, maskBlue4)
        saveMatDirectly(onlyBlue4, "5_only_blue_mask4", context)
        detectAndDrawRectanglesOnMat(onlyBlue4, context, "5_only_blue_mask4", src)
//        detectAndDrawRectanglesOnMat(onlyBlue4, context, "5_only_blue_mask4")
        src.release()
        hsv.release()
    }


    private fun detectAndDrawRectanglesOnMat(image: Mat, context: Context, imageName: String, originalMat: Mat) {
        // Convertir l'image en niveaux de gris
        val gray = Mat()
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY)

        // Appliquer un flou pour réduire le bruit (si nécessaire)
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

        // Créer un masque où les zones non noires sont en blanc et les zones noires en noir
        val maskNonBlack = Mat()
        Core.inRange(blurred, Scalar(1.0), Scalar(255.0), maskNonBlack)
        saveMatDirectly(maskNonBlack, "maskNonBlack_$imageName", context)

        // Appliquer une dilatation pour combler les petits trous dans les zones
        val dilated = Mat()
        val kernel = Mat.ones(5, 5, CvType.CV_8U)
        Imgproc.dilate(maskNonBlack, dilated, kernel)
        saveMatDirectly(dilated, "dilated_$imageName", context)

        // Trouver les contours dans les zones dilatées
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(dilated, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        // Liste des rectangles détectés
        val detectedCards = mutableListOf<Rect>()

        // Filtrer les contours en fonction de la taille et de l'aspect ratio
        val filteredContours = contours.filter { contour ->
            val boundingRect = Imgproc.boundingRect(contour)
            val area = Imgproc.contourArea(contour)
            val aspectRatio = boundingRect.width.toDouble() / boundingRect.height.toDouble()

            area > 40000.0 && area < 200000.0 && aspectRatio in 0.3..0.8
        }

        // Tracer des rectangles orange autour des zones détectées et les stocker
        for (contour in filteredContours) {
            val boundingRect = Imgproc.boundingRect(contour)
            Imgproc.rectangle(image, boundingRect.tl(), boundingRect.br(), Scalar(0.0, 165.0, 255.0), 3)
            detectedCards.add(boundingRect)  // ✅ Ajout des cartes détectées
        }

        // Sauvegarder l'image avec les rectangles tracés
        saveMatDirectly(image, "detected_rectangles_$imageName", context)

        // 🔥 Extraire les titres des cartes détectées avec ML Kit (⚠️ en passant originalMat)
        extractTitlesFromCards(context, detectedCards, originalMat, imageName)
    }

    fun extractTitlesFromCards(context: Context, detectedCards: List<Rect>, originalMat: Mat, imageName: String) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val detectedTitles = mutableListOf<String>() // ✅ Liste pour stocker les titres

        if (detectedCards.isEmpty()) {
            Log.e("OCR", "❌ Aucun rectangle détecté !")
            return
        }

        for ((index, cardRect) in detectedCards.withIndex()) {
            Log.d("OCR", "🔍 Traitement de la carte #$index avec Rect: $cardRect")

            // Extraire la zone de la carte
            val cardMat = Mat(originalMat, cardRect)

            // ✅ Convertir BGR -> RGB avant la conversion en Bitmap
            val cardMatRGB = Mat()
            Imgproc.cvtColor(cardMat, cardMatRGB, Imgproc.COLOR_BGR2RGB)

            // Convertir en Bitmap
            val cardBitmap = Bitmap.createBitmap(cardMatRGB.width(), cardMatRGB.height(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(cardMatRGB, cardBitmap)

            // Définir la zone du titre (12% supérieur, 85% largeur)
            val titleHeight = (cardBitmap.height * 0.12).toInt()
            val titleWidth = (cardBitmap.width * 0.85).toInt()
            val titleBitmap = Bitmap.createBitmap(cardBitmap, 0, 0, titleWidth, titleHeight)

            Log.d("OCR", "📸 Zone du titre extraite pour la carte #$index")
            saveBitmapDirectly(titleBitmap, "titre $index $imageName", context)

            // Créer une image ML Kit
            val image = InputImage.fromBitmap(titleBitmap, 0)

            // Lancer l'OCR
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detectedTitle = visionText.text.trim()
                    if (detectedTitle.isNotEmpty()) {
                        detectedTitles.add(detectedTitle) // ✅ Ajouter le titre trouvé
                    }
                    Log.d("OCR", "✅ Titre détecté pour la carte #$index : $detectedTitle")

                    // ✅ Si c'était la dernière carte, copier dans le presse-papiers
                    if (detectedTitles.size == detectedCards.size) {
                        copyToClipboard(context, detectedTitles)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("OCR", "❌ Erreur OCR carte #$index : ${e.message}")
                }
        }
    }

//    fun extractTitlesFromCards(context: Context, detectedCards: List<Rect>, originalMat: Mat, imageName: String) {
//        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//
//        if (detectedCards.isEmpty()) {
//            Log.e("OCR", "❌ Aucun rectangle détecté !")
//            return
//        }
//
//        for ((index, cardRect) in detectedCards.withIndex()) {
//            Log.d("OCR", "🔍 Traitement de la carte #$index avec Rect: $cardRect")
//
//            // Extraire la zone de la carte
//            val cardMat = Mat(originalMat, cardRect)
//
//            // Convertir BGR -> RGB avant la conversion en Bitmap
//            val cardMatRGB = Mat()
//            Imgproc.cvtColor(cardMat, cardMatRGB, Imgproc.COLOR_BGR2RGB)
//
//            // Convertir en Bitmap
//            val cardBitmap = Bitmap.createBitmap(cardMatRGB.width(), cardMatRGB.height(), Bitmap.Config.ARGB_8888)
//            Utils.matToBitmap(cardMatRGB, cardBitmap)
//
////            // Convertir en Bitmap
////            val cardBitmap = Bitmap.createBitmap(cardMat.width(), cardMat.height(), Bitmap.Config.ARGB_8888)
////            Utils.matToBitmap(cardMat, cardBitmap)
//
//            // Définir la zone du titre (20% supérieur)
//            val titleHeight = (cardBitmap.height * 0.12).toInt()
//            val titleWidth = (cardBitmap.width * 0.85).toInt()
//            val titleBitmap = Bitmap.createBitmap(cardBitmap, 0, 0, titleWidth, titleHeight)
//
//            Log.d("OCR", "📸 Zone du titre extraite pour la carte #$index")
//            saveBitmapDirectly(titleBitmap, "titre $index $imageName", context)
//            // Créer une image ML Kit
//            val image = InputImage.fromBitmap(titleBitmap, 0)
//
//            // Lancer l'OCR
//            recognizer.process(image)
//                .addOnSuccessListener { visionText ->
//                    val detectedTitle = visionText.text.trim()
//                    Log.d("OCR", "✅ Titre détecté pour la carte #$index : $detectedTitle")
//                }
//                .addOnFailureListener { e ->
//                    Log.e("OCR", "❌ Erreur OCR carte #$index : ${e.message}")
//                }
//        }
//    }

    fun copyToClipboard(context: Context, titles: List<String>) {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val textToCopy = titles.joinToString("\n") // ✅ Joindre les titres avec un saut de ligne
        val clip = ClipData.newPlainText("Detected Titles", textToCopy)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "📋 Titres copiés dans le presse-papiers !", Toast.LENGTH_SHORT).show()
    }



    /**
     * Enregistre un Mat directement en tant qu'image PNG sans conversion
     */
    private fun saveMatDirectly(mat: Mat, filename: String, context: Context) {
        val file = File(context.cacheDir, "$filename.png")
        Imgcodecs.imwrite(file.absolutePath, mat)
        Log.d("ImageProcessing", "Image enregistrée : ${file.absolutePath}")
    }

    private fun saveBitmapDirectly(bitmap: Bitmap, filename: String, context: Context) {
        val file = File(context.cacheDir, "$filename.png")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d("ImageProcessing", "Image enregistrée : ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("ImageProcessing", "Erreur lors de l'enregistrement de l'image : ${e.message}")
        }
    }


    //avec 1 seule image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let {

                exergueCards(it, this)

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


    companion object {
        const val REQUEST_CODE_PICK_IMAGE = 1001
        const val ACTION_IMAGE_SELECTED = "com.example.IMAGE_SELECTED"
        const val EXTRA_IMAGE_URI = "image_uri"
    }
}
