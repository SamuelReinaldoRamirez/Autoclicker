package com.example.autoclickerfusionbuild

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class AutoclickService : AccessibilityService() {

    companion object {
        var instance: AutoclickService? = null
    }


    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        serviceInfo = info

        Log.d("ACCESSIBILITY", "Service d'accessibilité configuré et connecté.")
        instance = this
    }


    @SuppressLint("ResourceType")
    fun performClick(x: Float, y: Float) {
        val path = Path().apply {
            moveTo(x, y)
        }
        Log.d("CLICK", "moved to : ($x, $y)")

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(
            GestureDescription.StrokeDescription(
                path,
                0,    // Délai avant le clic (en ms)
                200   // Durée du clic (en ms)
            )
        )
        Log.d("CLICK", "Tentative de dispatchGesture à la position : ($x, $y)")

        var windowManager = getSystemService(Service.WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val horizontalRedLineView = inflater.inflate(R.drawable.horizontal_red_line, null)
        val verticalRedLineView = inflater.inflate(R.drawable.vertical_red_line, null)

        val horizontalRedLineParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            this.y = y.toInt() - 100 // Ajustement pour bien centrer la ligne horizontale
        }

        val verticalRedLineParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, // Largeur ajustée
            WindowManager.LayoutParams.MATCH_PARENT, // Hauteur pleine
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START // Positionnement horizontal
            this.x = x.toInt() - 2 // Ajustement pour bien centrer la ligne verticale
        }

        dispatchGesture(
            gestureBuilder.build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d("CLICK", "Geste simulé avec succès.")

//                    //definir les variabbles en dehors de la fonction pour utiliser moins de memoire
//                    val inflater = getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//                    val horizontalRedLineView = inflater.inflate(R.drawable.horizontal_red_line, null)
//                    val verticalRedLineView = inflater.inflate(R.drawable.vertical_red_line, null)
//                    var windowManager = getSystemService(Service.WINDOW_SERVICE) as WindowManager
//                    val horizontalRedLineParams = WindowManager.LayoutParams(
//                        WindowManager.LayoutParams.MATCH_PARENT,
//                        WindowManager.LayoutParams.WRAP_CONTENT,
//                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//                        PixelFormat.TRANSLUCENT
//                    ).apply {
//                        gravity = Gravity.TOP
//                        this.y = y.toInt() - 2 // Ajustement pour bien centrer la ligne
//                    }
//
//                    val verticalRedLineParams = WindowManager.LayoutParams(
//                        WindowManager.LayoutParams.MATCH_PARENT,
//                        WindowManager.LayoutParams.WRAP_CONTENT,
//                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//                        PixelFormat.TRANSLUCENT
//                    ).apply {
//                        gravity = Gravity.START
//                        this.x = x.toInt() - 2 // Ajustement pour bien centrer la ligne
//                    }

                    windowManager.addView(horizontalRedLineView, horizontalRedLineParams)
                    windowManager.addView(verticalRedLineView, verticalRedLineParams)

                    // Supprime la ligne après 1 seconde
                    Handler(Looper.getMainLooper()).postDelayed({
                        windowManager.removeView(horizontalRedLineView)
                        windowManager.removeView(verticalRedLineView)
                    }, 1000)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.e("CLICK", "Le geste a été annulé.")
                }
            },
            null
        )
    }


    private fun showClickIndicator(x: Float, y: Float) {
        // Créer un WindowManager
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Configuration des paramètres de la vue flottante
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // Assurez-vous d'avoir l'autorisation
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.x = x.toInt()
        layoutParams.y = y.toInt()

        // Créer une vue (par exemple un cercle rouge)
        val indicatorView = View(this).apply {
            setBackgroundResource(R.drawable.circle) // Utilisez un drawable pour un cercle
            layoutParams.width = 100
            layoutParams.height = 100
        }

        // Ajouter la vue au WindowManager
        windowManager.addView(indicatorView, layoutParams)

        // Supprimer la vue après un délai (1 seconde ici)
        Handler(Looper.getMainLooper()).postDelayed({
            windowManager.removeView(indicatorView)
        }, 1000)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        Log.d("ACCESSIBILITY", "Service d'accessibilité déconnecté.")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Exemple de traitement d'événement
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Logique pour un clic de vue
                Log.d("AutoclickService", "Une vue a été cliquée : ${event.text}")
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                // Logique pour un défilement de vue
                Log.d("AutoclickService", "Une vue a défilé")
            }

            else -> {
                Log.d("AutoclickService", "Autre événement : ${event.eventType}")
            }
        }
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
