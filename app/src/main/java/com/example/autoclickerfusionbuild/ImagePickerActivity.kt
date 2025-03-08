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
import android.widget.Toast
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
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.features2d.ORB
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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

    //fait du clustering de textes par proximité horizontale pour determiner des cartes :/
//    private fun analyzeImageWithMLKit(imageUri: Uri, context: Context) {
//        val inputImage = InputImage.fromFilePath(context, imageUri)
//        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//
//        recognizer.process(inputImage)
//            .addOnSuccessListener { visionText ->
//                val extractedText = visionText.text
//                Log.d("OCR_RESULT", "Texte brut extrait :\n$extractedText")
//
//                val textBlocks = visionText.textBlocks
//
//                // Étape 1 : Regrouper les blocs en fonction de leur proximité horizontale
//                val cardClusters = mutableListOf<MutableList<Text.TextBlock>>()
//
//                for (block in textBlocks) {
//                    val rect = block.boundingBox
//                    if (rect != null) {
//                        var added = false
//
//                        // Vérifier si ce bloc appartient à un cluster existant
//                        for (cluster in cardClusters) {
//                            val firstBlock = cluster.first().boundingBox
//                            if (firstBlock != null && Math.abs(rect.left - firstBlock.left) < 100) {
//                                cluster.add(block)
//                                added = true
//                                break
//                            }
//                        }
//
//                        // Si aucun cluster correspondant, créer un nouveau cluster
//                        if (!added) {
//                            cardClusters.add(mutableListOf(block))
//                        }
//                    }
//                }
//
//                // Étape 2 : Extraire le texte du bloc le plus haut de chaque cluster (nom de carte)
//                val cardNames = mutableListOf<String>()
//                for (cluster in cardClusters) {
//                    Log.e("CLUSTER", "Cluster formé : ${cluster.joinToString { "[${it.text}] (x:${it.boundingBox?.left}, y:${it.boundingBox?.top})" }}")
//                    val topBlock = cluster.minByOrNull { it.boundingBox?.top ?: Int.MAX_VALUE }
//                    topBlock?.let {
//                        Log.d("CARD_DETECTION", "Carte détectée : ${it.text}")
//                        cardNames.add(it.text)
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
                    Log.e("CLUSTER", "Cluster formé : ${cluster.joinToString { "[${it.text}] (x:${it.boundingBox?.left}, y:${it.boundingBox?.top})" }}")
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


    private fun exergueCards(imageUri: Uri, context: Context) {
//        val inputBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
//        val src = Mat()
//        Utils.bitmapToMat(inputBitmap, src)
//
//        // 🔹 Correction de l’inversion BGR → RGB
//        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB)
//
//        // 0️⃣ Enregistrement de l’image originale corrigée
//        saveMatDirectly(src, "0_original_RGB", context)
//
//        // Convertir en espace de couleur HSV
//        val hsv = Mat()
//        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_RGB2HSV)
//        saveMatDirectly(hsv, "1_HSV", context)
//
////        // Définir la plage de bleu en HSV
////        val lowerBlue = Scalar(100.0, 150.0, 50.0)  // Min HSV
////        val upperBlue = Scalar(140.0, 255.0, 255.0) // Max HSV
//
//        val lowerOrange = Scalar(10.0, 100.0, 100.0)  // Teinte orange foncée
//        val upperOrange = Scalar(25.0, 255.0, 255.0) // Teinte orange claire
//
//        // Créer un masque pour détecter la couleur bleue
//        val mask = Mat()
//        Core.inRange(hsv, lowerOrange, upperOrange, mask)
//
//        // Appliquer le masque pour ne garder que la partie bleue
//        val result = Mat()
//        Core.bitwise_and(src, src, result, mask)
//
//
//
//
////        // 🎨 Définir la plage de l'orange en RGB
////        val lowerOrange = Scalar(150.0, 70.0, 0.0)   // Min RGB
////        val upperOrange = Scalar(255.0, 190.0, 100.0) // Max RGB
////
////        // 🔹 Créer un masque pour l’orange en RGB
////        val mask = Mat()
////        Core.inRange(src, lowerOrange, upperOrange, mask)
////
////        // 🔹 Appliquer le masque pour ne garder que l’orange
////        val result = Mat()
////        Core.bitwise_and(src, src, result, mask)
//
//
//
//        saveMatDirectly(result, "2_filtré-bleu", context)
//
//        // Libérer la mémoire des Mat
//        src.release()
////        hsv.release()
//        mask.release()
//        result.release()

        val inputBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        val src = Mat()
        Utils.bitmapToMat(inputBitmap, src)

        // ✅ Convertir en RGB (au cas où l'image est en BGR)
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB)
        saveMatDirectly(src, "0_original_RGB", context)

        // 🔹 Convertir en HSV
        val hsv = Mat()
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_RGB2HSV)
        saveMatDirectly(hsv, "1_HSV", context)

        // 📌 Listes des filtres
        val hsvFilters = listOf(
            "HSV_Strict" to Pair(Scalar(15.0, 150.0, 150.0), Scalar(25.0, 255.0, 255.0)),
            "HSV_Large" to Pair(Scalar(5.0, 100.0, 100.0), Scalar(35.0, 255.0, 255.0)),
            "HSV_Extra_Large" to Pair(Scalar(0.0, 70.0, 70.0), Scalar(40.0, 255.0, 255.0))
        )

        val rgbFilters = listOf(
            "RGB_Strict" to Pair(Scalar(150.0, 70.0, 0.0), Scalar(255.0, 190.0, 100.0)),
            "RGB_Large" to Pair(Scalar(130.0, 50.0, 0.0), Scalar(255.0, 210.0, 130.0)),
            "RGB_Extra_Large" to Pair(Scalar(100.0, 30.0, 0.0), Scalar(255.0, 230.0, 150.0))
        )

        // 🔹 Appliquer les masques en HSV
        for ((name, range) in hsvFilters) {
            val mask = Mat()
            Core.inRange(hsv, range.first, range.second, mask)

            val result = Mat()
            Core.bitwise_and(src, src, result, mask)

            saveMatDirectly(result, "2_$name", context)

            mask.release()
            result.release()
        }

        // 🔹 Appliquer les masques en RGB
        for ((name, range) in rgbFilters) {
            val mask = Mat()
            Core.inRange(src, range.first, range.second, mask)

            val result = Mat()
            Core.bitwise_and(src, src, result, mask)

            saveMatDirectly(result, "3_$name", context)

            mask.release()
            result.release()
        }

        // 🔹 Appliquer les combinaisons HSV + RGB
        for ((hsvName, hsvRange) in hsvFilters) {
            for ((rgbName, rgbRange) in rgbFilters) {
                val maskHSV = Mat()
                val maskRGB = Mat()
                Core.inRange(hsv, hsvRange.first, hsvRange.second, maskHSV)
                Core.inRange(src, rgbRange.first, rgbRange.second, maskRGB)

                val combinedMask = Mat()
                Core.bitwise_and(maskHSV, maskRGB, combinedMask)

                val result = Mat()
                Core.bitwise_and(src, src, result, combinedMask)

                saveMatDirectly(result, "4_${hsvName}_${rgbName}", context)

                maskHSV.release()
                maskRGB.release()
                combinedMask.release()
                result.release()
            }
        }





        // 📌 Définition des filtres
        val colorFilters = mapOf(
            "BLEU" to listOf(
                "Strict" to Pair(Scalar(100.0, 150.0, 100.0), Scalar(130.0, 255.0, 255.0)),
                "Large" to Pair(Scalar(90.0, 100.0, 50.0), Scalar(140.0, 255.0, 255.0)),
                "Extra_Large" to Pair(Scalar(80.0, 70.0, 30.0), Scalar(150.0, 255.0, 255.0))
            ),
            "VERT" to listOf(
                "Strict" to Pair(Scalar(35.0, 100.0, 100.0), Scalar(85.0, 255.0, 255.0)),
                "Large" to Pair(Scalar(30.0, 80.0, 50.0), Scalar(90.0, 255.0, 255.0)),
                "Extra_Large" to Pair(Scalar(25.0, 50.0, 30.0), Scalar(95.0, 255.0, 255.0))
            ),
            "BEIGE" to listOf(
                "Strict" to Pair(Scalar(15.0, 30.0, 180.0), Scalar(30.0, 90.0, 255.0)),
                "Large" to Pair(Scalar(10.0, 20.0, 150.0), Scalar(35.0, 100.0, 255.0)),
                "Extra_Large" to Pair(Scalar(5.0, 10.0, 120.0), Scalar(40.0, 120.0, 255.0))
            ),
            "ROUGE" to listOf(
                "Strict" to Pair(Scalar(0.0, 150.0, 100.0), Scalar(10.0, 255.0, 255.0)),
                "Large" to Pair(Scalar(0.0, 100.0, 50.0), Scalar(15.0, 255.0, 255.0)),
                "Extra_Large" to Pair(Scalar(0.0, 70.0, 30.0), Scalar(20.0, 255.0, 255.0))
            )
        )

        // 🔹 Appliquer les masques
        for ((color, filters) in colorFilters) {
            for ((size, range) in filters) {
                val mask = Mat()
                Core.inRange(hsv, range.first, range.second, mask)

                val result = Mat()
                Core.bitwise_and(src, src, result, mask)

                saveMatDirectly(result, "7_${color}_$size", context)

                mask.release()
                result.release()
            }
        }






        // 📌 Définition des masques
        val rougeExtraLargeMin = Scalar(0.0, 70.0, 30.0)
        val rougeExtraLargeMax = Scalar(20.0, 255.0, 255.0)
        val bleuExtraLargeMin = Scalar(80.0, 70.0, 30.0)
        val bleuExtraLargeMax = Scalar(150.0, 255.0, 255.0)

        // 🔹 Appliquer les masques
        val maskRougeExtraLarge = Mat()
        Core.inRange(hsv, rougeExtraLargeMin, rougeExtraLargeMax, maskRougeExtraLarge)

        val maskBleuExtraLarge = Mat()
        Core.inRange(hsv, bleuExtraLargeMin, bleuExtraLargeMax, maskBleuExtraLarge)

        // 🔹 Opération OR (union des pixels détectés)
        val maskOR = Mat()
        Core.bitwise_or(maskRougeExtraLarge, maskBleuExtraLarge, maskOR)
        val resultOR = Mat()
        Core.bitwise_and(src, src, resultOR, maskOR)
        saveMatDirectly(resultOR, "8_Rouge_Bleu_OR", context)

        // 🔹 Opération AND (intersection des pixels détectés)
        val maskAND = Mat()
        Core.bitwise_and(maskRougeExtraLarge, maskBleuExtraLarge, maskAND)
        val resultAND = Mat()
        Core.bitwise_and(src, src, resultAND, maskAND)
        saveMatDirectly(resultAND, "8_Rouge_Bleu_AND", context)

        // 🔄 Libération mémoire
        maskRougeExtraLarge.release()
        maskBleuExtraLarge.release()
        maskOR.release()
        maskAND.release()
        resultOR.release()
        resultAND.release()
        src.release()
        hsv.release()





        // 🔄 Libération mémoire
        src.release()
        hsv.release()

    }


//travail en cours march moyen
//    private fun exergueCards(imageUri: Uri, context: Context) {
//        val inputBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
//        val src = Mat()
//        Utils.bitmapToMat(inputBitmap, src)
//
//        // 🔹 Correction de l’inversion BGR → RGB
//        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB)
//
//        // 0️⃣ Enregistrement de l’image originale corrigée
//        saveMatDirectly(src, "0_original_RGB", context)
//
//        // 1️⃣ Conversion en niveaux de gris
//        val gray = Mat()
//        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY)
//        saveMatDirectly(gray, "1_grayscale", context)
//
//        // 2️⃣ Appliquer un flou gaussien (noyau plus grand)
//        val blurred = Mat()
//        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
//        saveMatDirectly(blurred, "2_blurred", context)
//
//        var treshArray = mutableListOf<Mat>()
//
//        // 3️⃣ Appliquer un seuillage plus robuste
//        var thresh = Mat()
//        Imgproc.adaptiveThreshold(blurred, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2.0)
//        saveMatDirectly(thresh, "3AA_threshold", context)
//        treshArray.add(thresh)
//
//        thresh = Mat()
//        Imgproc.adaptiveThreshold(blurred, thresh, 120.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2.0)
//        saveMatDirectly(thresh, "3AB_threshold", context)
//        treshArray.add(thresh)
//
//
//        thresh = Mat()
//        Imgproc.threshold(blurred, thresh, 120.0, 255.0, Imgproc.THRESH_BINARY)
//        saveMatDirectly(thresh, "3BA_threshold", context)
//        treshArray.add(thresh)
//
//        thresh = Mat()
//        Imgproc.threshold(blurred, thresh, 120.0, 255.0, Imgproc.THRESH_BINARY_INV)
//        saveMatDirectly(thresh, "3CA_threshold", context)
//        treshArray.add(thresh)
//
//
//
//        var edgesArray = mutableListOf<Mat>()
//        var contoursOnThreshArray = mutableListOf<ArrayList<MatOfPoint>>()
//        var contoursOnThreshArray2 = mutableListOf<ArrayList<MatOfPoint>>()
//        for (threshIndex in treshArray.indices) {
//
//            //faire un petit for pour tester differents thresholds sur le canny
//            // 4️⃣ Détection des contours (Canny + findContours)
//            var edges = Mat()
//            Imgproc.Canny(treshArray.get(threshIndex), edges, 50.0, 150.0) // Seuils ajustables
//            saveMatDirectly(edges, "4" + threshIndex + " _canny_edges", context)
//            edgesArray.add(edges)
//
//            // 4️⃣ Détection des contours (Canny + findContours)
//            edges = Mat()
//            Imgproc.Canny(treshArray.get(threshIndex), edges, 90.0, 150.0) // Seuils ajustables
//            saveMatDirectly(edges, "41" + threshIndex + " _canny_edges", context)
//            edgesArray.add(edges)
//
//            // 4️⃣ Détection des contours (Canny + findContours)
//            edges = Mat()
//            Imgproc.Canny(treshArray.get(threshIndex), edges, 50.0, 230.0) // Seuils ajustables
//            saveMatDirectly(edges, "42" + threshIndex + " _canny_edges", context)
//            edgesArray.add(edges)
//
//            // 4️⃣ Détection des contours (Canny + findContours)
//            edges = Mat()
//            Imgproc.Canny(treshArray.get(threshIndex), edges, 90.0, 230.0) // Seuils ajustables
//            saveMatDirectly(edges, "43" + threshIndex + " _canny_edges", context)
//            edgesArray.add(edges)
//
//            // 4️⃣ Détection des contours (Canny + findContours)
//            edges = Mat()
//            Imgproc.Canny(treshArray.get(threshIndex), edges, 15.0, 70.0) // Seuils ajustables
//            saveMatDirectly(edges, "44" + threshIndex + " _canny_edges", context)
//            edgesArray.add(edges)
//            //fin du for à faire
//
//
//            val contours = ArrayList<MatOfPoint>()
//            val contours2 = ArrayList<MatOfPoint>()
//            val hierarchy = Mat()
//            Imgproc.findContours(
//                treshArray.get(threshIndex),
//                contours,
//                hierarchy,
//                Imgproc.RETR_EXTERNAL,
//                Imgproc.CHAIN_APPROX_SIMPLE
//            )
//
//            Imgproc.findContours(
//                treshArray.get(threshIndex),
//                contours2,
//                hierarchy,
//                Imgproc.RETR_LIST,
//                Imgproc.CHAIN_APPROX_SIMPLE
//            )
//
//            contoursOnThreshArray.add(contours)
//            contoursOnThreshArray2.add(contours2)
//
//        }
//
//        for (contours in contoursOnThreshArray) {
//            val cardRects = mutableListOf<Rect>()
//            for (contour in contours) {
//                val area = Imgproc.contourArea(contour)
//                if (area > 10000 && area < 50000) { // Ajuster le seuil si nécessaire
//                    val rect = Imgproc.boundingRect(contour)
//                    cardRects.add(rect)
//                }
//            }
//
//            // 5️⃣ Dessiner les rectangles jaunes sur les cartes détectées
//            for (rect in cardRects) {
//                Imgproc.rectangle(src, rect.tl(), rect.br(), Scalar(0.0, 255.0, 255.0), 3)
//            }
//            saveMatDirectly(src, "6A" + contoursOnThreshArray.indexOf(contours) + " _final_annotated", context)
//
//
//        }
//
//        for (contours in contoursOnThreshArray2) {
//            val cardRects = mutableListOf<Rect>()
//            for (contour in contours) {
//                val area = Imgproc.contourArea(contour)
//                if (area > 10000 && area < 50000) { // Ajuster le seuil si nécessaire
//                    val rect = Imgproc.boundingRect(contour)
//                    cardRects.add(rect)
//                }
//            }
//
//            // 5️⃣ Dessiner les rectangles jaunes sur les cartes détectées
//            for (rect in cardRects) {
//                Imgproc.rectangle(src, rect.tl(), rect.br(), Scalar(0.0, 255.0, 255.0), 3)
//            }
//            saveMatDirectly(src, "6B" + contoursOnThreshArray2.indexOf(contours) + " _final_annotated", context)
//        }
//
//        var contoursOnEdgesArray = mutableListOf<ArrayList<MatOfPoint>>()
//        var contoursOnEdgesArray2 = mutableListOf<ArrayList<MatOfPoint>>()
//        for (edges in edgesArray) {
//            val contours = ArrayList<MatOfPoint>()
//            val contours2 = ArrayList<MatOfPoint>()
//            val hierarchy = Mat()
//            Imgproc.findContours(
//                edges,
//                contours,
//                hierarchy,
//                Imgproc.RETR_EXTERNAL,
//                Imgproc.CHAIN_APPROX_SIMPLE
//            )
//
//            Imgproc.findContours(
//                edges,
//                contours2,
//                hierarchy,
//                Imgproc.RETR_LIST,
//                Imgproc.CHAIN_APPROX_SIMPLE
//            )
//
//            contoursOnEdgesArray.add(contours)
//            contoursOnEdgesArray2.add(contours2)
//        }
//        for (contours in contoursOnEdgesArray) {
//            val cardRects = mutableListOf<Rect>()
//            for (contour in contours) {
//                val area = Imgproc.contourArea(contour)
//                if (area > 10000 && area < 50000) { // Ajuster le seuil si nécessaire
//                    val rect = Imgproc.boundingRect(contour)
//                    cardRects.add(rect)
//                }
//            }
//            // 5️⃣ Dessiner les rectangles jaunes sur les cartes détectées
//            for (rect in cardRects) {
//                Imgproc.rectangle(src, rect.tl(), rect.br(), Scalar(0.0, 255.0, 255.0), 3)
//            }
//            saveMatDirectly(src, "6EA" + contoursOnEdgesArray.indexOf(contours) + " _final_annotated", context)
//        }
//        for (contours in contoursOnEdgesArray2) {
//            val cardRects = mutableListOf<Rect>()
//            for (contour in contours) {
//                val area = Imgproc.contourArea(contour)
//                if (area > 10000 && area < 50000) { // Ajuster le seuil si nécessaire
//                    val rect = Imgproc.boundingRect(contour)
//                    cardRects.add(rect)
//                }
//            }
//            // 5️⃣ Dessiner les rectangles jaunes sur les cartes détectées
//            for (rect in cardRects) {
//                Imgproc.rectangle(src, rect.tl(), rect.br(), Scalar(0.0, 255.0, 255.0), 3)
//            }
//            saveMatDirectly(src, "6EB" + contoursOnEdgesArray2.indexOf(contours) + " _final_annotated", context)
//        }
//
//
//        Log.d("ImageProcessing", "Processus terminé, images enregistrées dans le cache.")
//
//    }

    /**
     * Enregistre un Mat directement en tant qu'image PNG sans conversion
     */
    private fun saveMatDirectly(mat: Mat, filename: String, context: Context) {
        val file = File(context.cacheDir, "$filename.png")
        Imgcodecs.imwrite(file.absolutePath, mat)
        Log.d("ImageProcessing", "Image enregistrée : ${file.absolutePath}")
    }


    fun detectCards(imageUri: Uri, context: Context): Pair<Mat, List<Rect>> {
        // Charger l'image en Bitmap
        val inputBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        val src = Mat()
        Utils.bitmapToMat(inputBitmap, src)

        // Vérifier si l'image est en niveaux de gris
        val isGray = src.channels() == 1

        // Convertir en niveaux de gris si ce n'est pas déjà le cas
        val gray = Mat()
        if (isGray) {
            src.copyTo(gray) // On garde l'image d'origine si elle était déjà en gris
        } else {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        }

        // Appliquer un flou gaussien pour réduire le bruit
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(3.0, 3.0), 0.0)

        // Appliquer le seuillage d'Otsu
        val thresh = Mat()
        Imgproc.threshold(blurred, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)

        // Trouver les contours
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val cardRects = mutableListOf<Rect>()

        for (contour in contours) {
            val area = Imgproc.contourArea(contour)

            // Filtrer les contours trop petits pour éviter le bruit
            if (area > 400) {  // Ajuste ce seuil si nécessaire
                val rect = Imgproc.boundingRect(contour)
                cardRects.add(rect)

                // Dessiner les rectangles détectés
                val color = if (isGray) Scalar(255.0) else Scalar(0.0, 255.0, 0.0)
                Imgproc.rectangle(thresh, rect.tl(), rect.br(), color, 3)
            }
        }

        return Pair(thresh, cardRects) // Retourne l'image binaire et les rectangles détectés
    }





//    fun detectCards(imageUri: Uri, context: Context): List<Rect> {
//        // Charger l'image en Bitmap
//        val inputBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
//        val src = Mat()
//        Utils.bitmapToMat(inputBitmap, src)
//
//        // Convertir en niveaux de gris
//        val gray = Mat()
//        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
//
//        // Appliquer un flou pour réduire le bruit
//        val blurred = Mat()
//        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
//
//        // Détection des contours avec Canny
//        val edges = Mat()
//        Imgproc.Canny(blurred, edges, 75.0, 200.0)
//
//        // Trouver les contours
//        val contours = ArrayList<MatOfPoint>()
//        val hierarchy = Mat()
//        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
//
//        val cardRects = mutableListOf<Rect>()
//
//        for (contour in contours) {
//            val approx = MatOfPoint2f()
//            val contour2f = MatOfPoint2f(*contour.toArray())
//
//            // Approximation polygonale
//            Imgproc.approxPolyDP(contour2f, approx, 0.02 * Imgproc.arcLength(contour2f, true), true)
//
//            // Vérifier si c'est un rectangle (4 côtés)
//            if (approx.total() == 4L) {
//                val rect = Imgproc.boundingRect(contour)
//
//                // Filtrer les petits objets
//                if (rect.width > 100 && rect.height > 100) {
//                    cardRects.add(rect)
//                }
//            }
//        }
//
//        return cardRects
//    }


    fun drawCardContours(imageUri: Uri, context: Context, cardRects: List<Rect>): Bitmap {
        Log.e("ImageProcessing", "Nombre de cartes détectées : ${cardRects.size}")
        // Charger l'image
        val inputBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        val src = Mat()
        Utils.bitmapToMat(inputBitmap, src)

        val numChannels = src.channels()
        Log.d("ImageProcessing", "🔍 L'image a $numChannels canal(aux).")
        if (src.channels() == 4) {
            Log.d("ImageProcessing", "🎨 Conversion de BGRA (4 canaux) vers BGR (3 canaux)...")
            Imgproc.cvtColor(src, src, Imgproc.COLOR_BGRA2BGR)
        }

        val colorYellow = Scalar(0.0, 255.0, 255.0) // Jaune

//        for (rect in cardRects) {
//            Log.d("ImageProcessing", "Ajout du rectangle $index : x=${rect.x}, y=${rect.y}, largeur=${rect.width}, hauteur=${rect.height}")
//
//            Imgproc.rectangle(
//                src,
//                Point(rect.x.toDouble(), rect.y.toDouble()),
//                Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()),
//                colorYellow, 3
//            )
//        }

        cardRects.forEachIndexed { index, rect ->
            Log.d("ImageProcessing", "Ajout du rectangle $index : x=${rect.x}, y=${rect.y}, largeur=${rect.width}, hauteur=${rect.height}")

            Imgproc.rectangle(
                src,
                Point(rect.x.toDouble(), rect.y.toDouble()),
                Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()),
                colorYellow, 30
            )
        }


        // Vérification après dessin
        if (cardRects.isEmpty()) {
            Log.w("ImageProcessing", "⚠️ Aucun rectangle jaune ajouté à l'image.")
        } else {
            Log.d("ImageProcessing", "✅ Tous les rectangles jaunes ont été ajoutés.")
        }


        // Convertir en Bitmap
        val outputBitmap = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(src, outputBitmap)

        return outputBitmap
    }


//avec 1 seule image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let {
//                val enhancedBitmap = enhanceImage(it, this)
//                val enhancedImageUri = saveBitmapToCache(enhancedBitmap, this) // On enregistre l’image améliorée
//                if (enhancedImageUri != null) {
//                    analyzeImageWithMLKit(enhancedImageUri, this)
//                } else {
//                    Log.e("ImageProcessing", "Échec de la sauvegarde de l'image améliorée")
//                }

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
        const val REQUEST_CODE_PICK_IMAGE_CARDS = 1002
        const val ACTION_IMAGE_SELECTED = "com.example.IMAGE_SELECTED"
        const val EXTRA_IMAGE_URI = "image_uri"
    }
}
