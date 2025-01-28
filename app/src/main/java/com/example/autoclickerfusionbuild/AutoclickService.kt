package com.example.autoclickerfusionbuild

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class AutoclickService : AccessibilityService() {

    companion object {
        var instance: AutoclickService? = null
    }

//    override fun onServiceConnected() {
//        super.onServiceConnected()
//        val info = AccessibilityServiceInfo()
//        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
//        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED
//        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
//        info.notificationTimeout = 100
//        serviceInfo = info
//
//        instance = this
//        Log.d("ACCESSIBILITY", "Service d'accessibilité connecté.")
//    }

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

//    fun performClick(x: Float, y: Float) {
//        val path = Path().apply {
//            moveTo(x, y)
//        }
//        val gestureBuilder = GestureDescription.Builder()
//        gestureBuilder.addStroke(
//            GestureDescription.StrokeDescription(
//                path,
//                1,    // Délai avant le clic (en ms)
//                100   // Durée du clic (en ms)
//            )
//        )
//        dispatchGesture(
//            gestureBuilder.build(),
//            object : AccessibilityService.GestureResultCallback() {
//                override fun onCompleted(gestureDescription: GestureDescription?) {
//                    Log.d("CLICK", "Geste simulé avec succès.")
//                    showClickIndicator(x, y)
//                }
//
//                override fun onCancelled(gestureDescription: GestureDescription?) {
//                    Log.e("CLICK", "Le geste a été annulé.")
//                }
//            },
//            null
//        )
//    }

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

        dispatchGesture(
            gestureBuilder.build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d("CLICK", "Geste simulé avec succès.")
                    showClickIndicator(x, y)
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
