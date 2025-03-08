package com.example.autoclickerfusionbuild

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File


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

//        val lowerBlue = Scalar(90.0, 100.0, 50.0)  // Plus large vers les cyan-bleu
        //best
//        val lowerBlue = Scalar(78.0, 40.0, 80.0)
//        val upperBlue = Scalar(150.0, 255.0, 255.0) // Plus large vers les bleus violets

//        // 🔹 Masque pour ne garder que le bleu
//        val maskBlue = Mat()
//        Core.inRange(hsv, lowerBlue, upperBlue, maskBlue)
//
//        // Appliquer le masque sur l'image d'origine
//        val onlyBlue = Mat()
//        Core.bitwise_and(src, src, onlyBlue, maskBlue)
//        saveMatDirectly(onlyBlue, "2_only_blueter", context)
//
//        // 🔹 Inverser le masque pour enlever le bleu
//        val maskNoBlue = Mat()
//        Core.bitwise_not(maskBlue, maskNoBlue)
//
//        // Appliquer l'inverse du masque
//        val noBlue = Mat()
//        Core.bitwise_and(src, src, noBlue, maskNoBlue)
//        saveMatDirectly(noBlue, "3_no_blueter", context)



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
        detectAndDrawRectanglesOnMat(onlyBlue1, context, "2_only_blue_mask1")

        val onlyBlue2 = Mat()
        Core.bitwise_and(src, src, onlyBlue2, maskBlue2)
        saveMatDirectly(onlyBlue2, "3_only_blue_mask2", context)
        detectAndDrawRectanglesOnMat(onlyBlue2, context, "3_only_blue_mask2")

        val onlyBlue3 = Mat()
        Core.bitwise_and(src, src, onlyBlue3, maskBlue3)
        saveMatDirectly(onlyBlue3, "4_only_blue_mask3", context)
        detectAndDrawRectanglesOnMat(onlyBlue3, context, "4_only_blue_mask3")


        val onlyBlue4 = Mat()
        Core.bitwise_and(src, src, onlyBlue4, maskBlue4)
        saveMatDirectly(onlyBlue4, "5_only_blue_mask4", context)
        detectAndDrawRectanglesOnMat(onlyBlue4, context, "5_only_blue_mask4")


        src.release()
        hsv.release()
    }

    private fun detectAndDrawRectanglesOnMat(image: Mat, context: Context, imageName: String) {
        // Convertir l'image en niveaux de gris
        val gray = Mat()
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY)

        // Appliquer un flou pour réduire le bruit (si nécessaire)
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

        // Créer un masque où les zones non noires sont en blanc et les zones noires en noir
        val maskNonBlack = Mat()
        Core.inRange(blurred, Scalar(1.0), Scalar(255.0), maskNonBlack)  // On considère tous les pixels qui ne sont pas noirs (intensité > 1)
        saveMatDirectly(maskNonBlack, "maskNonBlack"+imageName, context)

        // Appliquer une dilatation pour combler les petits trous dans les zones
        val dilated = Mat()
        val kernel = Mat.ones(5, 5, CvType.CV_8U)  // Utilisation d'un noyau de 5x5 pour dilater
        Imgproc.dilate(maskNonBlack, dilated, kernel)
        saveMatDirectly(dilated, "dilated"+imageName, context)


        // Trouver les contours dans les zones dilatées
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(dilated, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        // Filtrer les contours en fonction de la taille et de l'aspect ratio
        val filteredContours = contours.filter { contour ->
            val boundingRect = Imgproc.boundingRect(contour)
            val area = Imgproc.contourArea(contour)
            val aspectRatio = boundingRect.width.toDouble() / boundingRect.height.toDouble()
//            area > 500.0 && area < 20000.0 && aspectRatio in 0.5..2.0  // Ajuster ces valeurs en fonction des cartes
            area > 80000.0 && area < 200000.0 && aspectRatio in 0.2..3.5  // Ajuster ces valeurs en fonction des cartes
        }

        // Tracer des rectangles orange autour des zones détectées
        for (contour in filteredContours) {
            val boundingRect = Imgproc.boundingRect(contour)
            Imgproc.rectangle(image, boundingRect.tl(), boundingRect.br(), Scalar(0.0, 165.0, 255.0), 3)
        }

        // Sauvegarder l'image avec les rectangles tracés
        saveMatDirectly(image, "detected_rectangles_$imageName", context)
    }

    //threshold caca
//    private fun detectAndDrawRectanglesOnMat(image: Mat, context: Context, imageName: String) {
//        // Convertir l'image en niveaux de gris
//        val gray = Mat()
//        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY)
//        saveMatDirectly(gray, "gray"+imageName, context)
//
//        // Appliquer un seuil pour isoler les cartes
//        val threshold = Mat()
//        Imgproc.threshold(gray, threshold, 127.0, 255.0, Imgproc.THRESH_BINARY)
//        saveMatDirectly(threshold, "threshold"+imageName, context)
//
//
//        // Détecter les contours dans l'image
//        val contours = ArrayList<MatOfPoint>()
//        val hierarchy = Mat()
//        Imgproc.findContours(threshold, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
//        saveMatDirectly(hierarchy, "hierarchy"+imageName, context)
//
//
//        // Tracer des rectangles orange autour des cartes détectées
//        for (contour in contours) {
//            val boundingRect = Imgproc.boundingRect(contour)
//            Imgproc.rectangle(image, boundingRect.tl(), boundingRect.br(), Scalar(0.0, 165.0, 255.0), 3)
//        }
//
//        // Sauvegarder l'image avec les rectangles tracés
//        saveMatDirectly(image, "detected_rectangles_$imageName", context)
//    }



//    //canny fout la merde et segment l'image. à parametrer?
//    private fun detectAndDrawRectanglesOnMat(image: Mat, context: Context, imageName: String) {
//        // Convertir l'image en niveaux de gris
//        val gray = Mat()
//        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY)
//        saveMatDirectly(gray, "gray"+imageName, context)
//
//        // Appliquer une réduction de bruit pour améliorer les contours
//        val blurred = Mat()
//        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
//        saveMatDirectly(blurred, "blurred"+imageName, context)
//
//
//        // Détecter les contours en utilisant Canny (cela permet de capturer les bordures)
//        val edges = Mat()
//        Imgproc.Canny(blurred, edges, 50.0, 150.0)
//        saveMatDirectly(edges, "edges"+imageName, context)
//
//
//        // Trouver les contours dans l'image des bords
//        val contours = ArrayList<MatOfPoint>()
//        val hierarchy = Mat()
//        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
//
//        // Filtrer les contours pour exclure les petites zones (en fonction de la taille de la carte)
//        val filteredContours = contours.filter { contour ->
//            val boundingRect = Imgproc.boundingRect(contour)
//            val area = Imgproc.contourArea(contour)
//            val aspectRatio = boundingRect.width.toDouble() / boundingRect.height.toDouble()
//            area > 500.0 && area < 20000.0 && aspectRatio in 0.5..2.0
//        }
//
//        // Tracer des rectangles orange autour des cartes détectées
//        for (contour in filteredContours) {
//            val boundingRect = Imgproc.boundingRect(contour)
//            Imgproc.rectangle(image, boundingRect.tl(), boundingRect.br(), Scalar(0.0, 165.0, 255.0), 3)
//        }
//
//        // Sauvegarder l'image avec les rectangles tracés
//        saveMatDirectly(image, "detected_rectangles_$imageName", context)
//    }



    /**
     * Enregistre un Mat directement en tant qu'image PNG sans conversion
     */
    private fun saveMatDirectly(mat: Mat, filename: String, context: Context) {
        val file = File(context.cacheDir, "$filename.png")
        Imgcodecs.imwrite(file.absolutePath, mat)
        Log.d("ImageProcessing", "Image enregistrée : ${file.absolutePath}")
    }


    //avec 1 seule image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
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
