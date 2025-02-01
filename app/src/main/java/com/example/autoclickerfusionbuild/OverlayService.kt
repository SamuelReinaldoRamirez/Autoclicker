package com.example.autoclickerfusionbuild

import android.accessibilityservice.AccessibilityService
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView

class OverlayService : Service() {

    private lateinit var overlayView: View
    private lateinit var windowManager: WindowManager

    override fun onCreate() {
        super.onCreate()
        // Affichage de l'overlay principal
        val inflater = getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.floating_window, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        windowManager = getSystemService(Service.WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, params)

        val indicatorContainer = overlayView.findViewById<FrameLayout>(R.id.indicatorContainer)

        var autoclickMenuView = inflater.inflate(R.layout.autoclick_menu, null)
        val autoclickMenuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        autoclickMenuParams.x = -500  // Position du bouton sur l'écran
        autoclickMenuParams.y = -1000  // Position du bouton sur l'écran
        windowManager.addView(autoclickMenuView, autoclickMenuParams)

        var lastX = 0
        var lastY = 0
        var initialTouchX = 0
        var initialTouchY = 0

        autoclickMenuView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = autoclickMenuParams.x
                    lastY = autoclickMenuParams.y
                    initialTouchX = event.rawX.toInt()
                    initialTouchY = event.rawY.toInt()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    autoclickMenuParams.x = lastX + (event.rawX.toInt() - initialTouchX)
                    autoclickMenuParams.y = lastY + (event.rawY.toInt() - initialTouchY)
                    windowManager.updateViewLayout(autoclickMenuView, autoclickMenuParams)
                    true
                }

                else -> false
            }
        }

        val xClick = autoclickMenuView.findViewById<EditText>(R.id.xClick)
        val yClick = autoclickMenuView.findViewById<EditText>(R.id.yClick)

        val startClickButtonM = autoclickMenuView.findViewById<Button>(R.id.startClickButton)

        startClickButtonM.setOnClickListener {

            // Convertir en Int (en gérant le cas où ce n'est pas un nombre)
            val xClickInt = xClick.text.toString().toFloatOrNull() ?: 0f
            val yClickInt = yClick.text.toString().toFloatOrNull() ?: 0f

            if (isAccessibilityServiceEnabled(this, AutoclickService::class.java)) {
                val autoclickService = AutoclickService.instance


//                showClickIndicator(indicatorContainer, 1200f, 500f)
//                showClickIndicator(indicatorContainer, 1200f, 0f)
//                showClickIndicator(indicatorContainer, 0f, 500f)
//                if (autoclickService != null) {
//                    autoclickService.performClick(1200f, 500f)
//                }

                showClickIndicator(indicatorContainer, xClickInt, yClickInt)
                showClickIndicator(indicatorContainer, xClickInt, 0f)
                showClickIndicator(indicatorContainer, 0f, yClickInt)
                if (autoclickService != null) {
                    autoclickService.performClick(xClickInt, yClickInt)
                }


                Log.d(null,"CLIQUE!!!!!!")
            } else {
                Log.e("AccessibilityService", "Le service d'accessibilité n'est pas activé.")
                val intent = Intent(this, AccessibilityPermissionActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Important pour démarrer depuis un service
                startActivity(intent)
            }

        }

        // Gérer le clic sur le bouton "Fermer"
        val closeButton = autoclickMenuView.findViewById<ImageButton>(R.id.closeButton)
        closeButton.setOnClickListener {
            stopSelf() // Arrête le service
            windowManager.removeView(overlayView) // Supprime l'overlay principal
            windowManager.removeView(autoclickMenuView)
        }


    }


    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        if (enabledServices == null) {
            Log.e("AccessibilityCheck", "Aucun service d'accessibilité activé.")
            return false
        }

        val serviceString = "${context.packageName}/${service.name}"
        val enabledServicesList = enabledServices.split(':')

        // Ajout de logs pour vérifier les services activés
        Log.d("AccessibilityCheck", "Services d'accessibilité activés : $enabledServices")
        Log.d("AccessibilityCheck", "Service recherché : $serviceString")

        val isServiceEnabled = enabledServicesList.contains(serviceString)

        if (isServiceEnabled) {
            Log.d("AccessibilityCheck", "Le service $serviceString est activé.")
        } else {
            Log.w("AccessibilityCheck", "Le service $serviceString n'est pas activé.")
        }

        return isServiceEnabled
    }


    private fun showClickIndicator(container: FrameLayout, x: Float, y: Float) {
        // Créer une vue circulaire pour représenter le clic
        val indicator = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(100, 100).apply {
                leftMargin = (x).toInt() // Centrer le cercle
                topMargin = (y).toInt()
            }
            setBackgroundColor(Color.RED)
            alpha = 0.7f
        }
        // Ajouter l'indicateur au conteneur
        container.addView(indicator)
        // Retirer l'indicateur après 1 seconde
        indicator.postDelayed({ container.removeView(indicator) }, 1500)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Supprime les overlays lorsque le service est détruit
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}