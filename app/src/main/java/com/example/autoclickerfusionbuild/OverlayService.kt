package com.example.autoclickerfusionbuild

import android.accessibilityservice.AccessibilityService
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class OverlayService : Service() {

    private lateinit var overlayView: View
    private lateinit var autoclickMenuView: View
    private lateinit var windowManager: WindowManager
    private lateinit var autoclickMenuParams: WindowManager.LayoutParams
    var semaphore = 0
    // Utilisation d'un Map pour stocker les points associés à chaque ID de bouton
    private val buttonPointsMap = mutableMapOf<Int, MutableList<Pair<Float, Float>>>()

    override fun onCreate() {
        super.onCreate()
        setupOverlayView()
        setupAutoclickMenu()
    }

    private fun setupOverlayView() {
        val inflater = getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.floating_window, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        windowManager = getSystemService(Service.WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, params)

    }

    private fun setupAutoclickMenu() {
        val inflater = getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        autoclickMenuView = inflater.inflate(R.layout.autoclick_menu, null)
        autoclickMenuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            ,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            x = -500
            y = -1000
        }
        windowManager.addView(autoclickMenuView, autoclickMenuParams)
        setupDraggableMenu(autoclickMenuView, autoclickMenuParams)
        setupButtons(autoclickMenuView)
    }

    private fun setupDraggableMenu(view: View, params: WindowManager.LayoutParams) {
        var lastX = 0
        var lastY = 0
        var initialTouchX = 0
        var initialTouchY = 0

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = params.x
                    lastY = params.y
                    initialTouchX = event.rawX.toInt()
                    initialTouchY = event.rawY.toInt()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = lastX + (event.rawX.toInt() - initialTouchX)
                    params.y = lastY + (event.rawY.toInt() - initialTouchY)
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    var isCreatingRoutine = false

    private fun setupButtons(view: View) {
        val xClick = view.findViewById<EditText>(R.id.xClick)
        val yClick = view.findViewById<EditText>(R.id.yClick)
        val startClickButtonM = view.findViewById<Button>(R.id.startClickButton)
        val closeButton = view.findViewById<ImageButton>(R.id.closeButton)
        val collapseButton = view.findViewById<ImageButton>(R.id.collapseButton)
        val contentLayout = view.findViewById<LinearLayout>(R.id.contentLayout)
        val createRoutineButton = view.findViewById<Button>(R.id.createRoutine)
        val clickPositions = mutableListOf<Pair<Float, Float>>()

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val fullScreenTouchableView = inflater.inflate(R.layout.transparent_overlay, null)

        setUpCreateRoutineButton(createRoutineButton, fullScreenTouchableView, xClick, yClick, clickPositions)
        setUpCollapseButton(collapseButton, contentLayout)
        setUpStartClickButton(startClickButtonM, xClick, yClick)
        setUpCloseButton(closeButton, view)
    }

    private fun setUpCreateRoutineButton(
        createRoutineButton: Button,
        fullScreenTouchableView: View,
        xClick: EditText,
        yClick: EditText,
        clickPositions: MutableList<Pair<Float, Float>>
    ) {
        createRoutineButton.setOnClickListener {
            if (isCreatingRoutine) {
                stopRoutineCreation(createRoutineButton, fullScreenTouchableView, clickPositions)
            } else {
                startRoutineCreation(createRoutineButton, fullScreenTouchableView, xClick, yClick, clickPositions)
            }
        }
    }

    private fun startRoutineCreation(
        createRoutineButton: Button,
        fullScreenTouchableView: View,
        xClick: EditText,
        yClick: EditText,
        clickPositions: MutableList<Pair<Float, Float>>
    ) {
        isCreatingRoutine = true
        createRoutineButton.text = "Enregistrer"
        windowManager.removeView(overlayView)

        clickPositions.clear()

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager.removeView(autoclickMenuView)
        windowManager.addView(fullScreenTouchableView, layoutParams)
        windowManager.addView(autoclickMenuView, autoclickMenuParams)

        setUpFullScreenTouchListener(fullScreenTouchableView, xClick, yClick, clickPositions)
    }

    private fun stopRoutineCreation(createRoutineButton: Button, fullScreenTouchableView: View, clickPositions: MutableList<Pair<Float, Float>>) {
        isCreatingRoutine = false
        createRoutineButton.text = "Créer routine"
        windowManager.removeView(fullScreenTouchableView)

        val buttonNumber = buttonPointsMap.size + 1
        buttonPointsMap[buttonNumber] = ArrayList(clickPositions)

        // Ajouter un bouton à autoclickMenuView
        val newButton = Button(autoclickMenuView.context).apply {
             // Liste des IDs des boutons
            text = "Routine $buttonNumber"


            setOnClickListener {
                val storedPoints = buttonPointsMap[buttonNumber]

                val screenLocation = IntArray(2)
                // Vérifier laquelle des vues existe et récupérer sa position à l'écran
                if (fullScreenTouchableView.isShown) {
                    fullScreenTouchableView.getLocationOnScreen(screenLocation)
                    Log.d("QUELLE VUE", "fullScreenTouchableView")
                } else if (overlayView.isShown) {
                    overlayView.getLocationOnScreen(screenLocation)
                    Log.d("QUELLE VUE", "overlayView")
                }else{
                    Log.e("ERREUR", "ni fullScreenTouchableView, ni overlayView n'existent")
                }

                storedPoints?.let { points ->
                    // Lancer handleAutoclick avec un délai de 500 ms entre chaque appel
                    val handler = Handler()
                    for (i in points.indices) {
                        val point = points[i]
                        // Utilisation de postDelayed pour appeler handleAutoclick avec un délai
                        handler.postDelayed({
                            // Appeler handleAutoclick pour chaque point
                            val xClick = point.first
                            val yClick = point.second
                            handleAutoclickFLOAT(xClick, yClick, screenLocation[1].toFloat()) // Assurez-vous que screenLocation[1] est valide
                        }, i * 3000L) // Délai de 3s entre chaque appel
                    }
                }

//                storedPoints?.let { points ->
//                    Log.d("stopRoutineCreation", "Liste des points: $points")
//                    Toast.makeText(context, "Points: $points", Toast.LENGTH_SHORT).show()
//                }
            }

        }

        // Effectuer un cast explicite pour éviter l'erreur de smart cast
        (autoclickMenuView as? ViewGroup)?.let { menuView ->
            menuView.addView(newButton)
            windowManager.updateViewLayout(menuView, autoclickMenuParams)
        } ?: Log.e("stopRoutineCreation", "autoclickMenuView n'est pas un ViewGroup, impossible d'ajouter le bouton.")

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(overlayView, params)
    }

    private fun setUpFullScreenTouchListener(
        fullScreenTouchableView: View,
        xClick: EditText,
        yClick: EditText,
        clickPositions: MutableList<Pair<Float, Float>>
    ) {
        fullScreenTouchableView.setOnTouchListener { _, event ->
            Log.d("0SEMAPHORE setUpFullScreenTouchListener", semaphore.toString())
            if(semaphore == 0){
                Log.d("SEMAPHORE setUpFullScreenTouchListener", semaphore.toString())
                semaphore += 1
                val screenLocation = IntArray(2)
                fullScreenTouchableView.getLocationOnScreen(screenLocation)

                // Les coordonnées du touché
                val x = event.x + screenLocation[0]
                val y = event.y + screenLocation[1]
                clickPositions.add(Pair(x, y))

                xClick.setText(x.toString())
                yClick.setText(y.toString())

                val layoutParams = fullScreenTouchableView.layoutParams as WindowManager.LayoutParams
                layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                windowManager.updateViewLayout(fullScreenTouchableView, layoutParams)

                // ✅ Attendre que la vue soit bien mise à jour
                CoroutineScope(Dispatchers.Main).launch {
                    suspendUntilApplied(fullScreenTouchableView, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    handleAutoclick(xClick, yClick, screenLocation[1].toFloat())
                    Log.d("THREAD", "handleAutoclick executed on thread: ${Thread.currentThread().name}")
                    Log.d("LISTE DES POSITIONS CLIQUEES", clickPositions.toString())

                    layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                    windowManager.updateViewLayout(fullScreenTouchableView, layoutParams)

                    CoroutineScope(Dispatchers.Main).launch {
                        suspendUntilAppliedInverse(fullScreenTouchableView, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()) // Attends que la vue devienne interactive
                    }

                    Log.d("THREAD", "View set back to TOUCHABLE on thread: ${Thread.currentThread().name}")
                    semaphore -= 1
                    Log.d("2SEMAPHORE setUpFullScreenTouchListener", semaphore.toString())
                }, 200)

            }
            false
        }
    }


    // 🛠 Fonction suspendue qui attend que la vue ait bien appliqué les flags
    private suspend fun suspendUntilApplied(view: View, expectedFlags: Int) {
        suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())

            val checkFlags = object : Runnable {
                override fun run() {
                    val layoutParams = view.layoutParams as WindowManager.LayoutParams
                    if ((layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) == expectedFlags) {
                        continuation.resume(Unit)
                    } else {
                        handler.postDelayed(this, 10) // Vérifie toutes les 10ms
                    }
                }
            }

            handler.post(checkFlags)
        }
    }

    private suspend fun suspendUntilAppliedInverse(view: View, expectedFlags: Int) {
        suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())

            val checkFlags = object : Runnable {
                override fun run() {
                    val layoutParams = view.layoutParams as WindowManager.LayoutParams
                    if ((layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()) == expectedFlags) {
                        continuation.resume(Unit)
                    } else {
                        handler.postDelayed(this, 10) // Vérifie toutes les 10ms
                    }
                }
            }

            handler.post(checkFlags)
        }
    }




    private fun setUpCollapseButton(collapseButton: ImageButton, contentLayout: LinearLayout) {
        var isCollapsed = false
        collapseButton.setOnClickListener {
            if (isCollapsed) {
                contentLayout.visibility = View.VISIBLE
                collapseButton.setImageResource(android.R.drawable.ic_media_play)
            } else {
                contentLayout.visibility = View.GONE
                collapseButton.setImageResource(android.R.drawable.ic_media_pause)
            }
            isCollapsed = !isCollapsed
        }
    }

    private fun setUpStartClickButton(startClickButtonM: Button, xClick: EditText, yClick: EditText) {
        startClickButtonM.setOnClickListener {
            handleAutoclick(xClick, yClick, 0f)
        }
    }

    private fun setUpCloseButton(closeButton: ImageButton, view: View) {
        closeButton.setOnClickListener {
            stopSelf()
            windowManager.removeView(overlayView)
            windowManager.removeView(view)
        }
    }


    private fun handleAutoclick(xClick: EditText, yClick: EditText, ydecalage: Float) {
        Log.d("SEMAPHORE handleAutoclick", semaphore.toString())

        if(semaphore == 0 || semaphore == 1) {
            semaphore += 1
            Log.d("2SEMAPHORE handleAutoclick", semaphore.toString())

            val xClickInt = xClick.text.toString().toFloatOrNull() ?: 0f
            val yClickInt = yClick.text.toString().toFloatOrNull() ?: 0f

            if (isAccessibilityServiceEnabled(this, AutoclickService::class.java)) {
                val autoclickService = AutoclickService.instance
                autoclickService?.performClick(xClickInt, yClickInt, ydecalage)
                Log.d(null, "CLIQUE!!!!!!")
                // Reset flag après avoir effectué le clic
            } else {
                Log.e("AccessibilityService", "Le service d'accessibilité n'est pas activé.")
                val intent = Intent(this, AccessibilityPermissionActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            Log.d("3SEMAPHORE handleAutoclick", semaphore.toString())
            semaphore -= 1
            Log.d("4SEMAPHORE handleAutoclick", semaphore.toString())

        }
    }


    private fun handleAutoclickFLOAT(xClick: Float, yClick: Float, ydecalage: Float) {
        Log.d("SEMAPHORE handleAutoclick", semaphore.toString())

        if(semaphore == 0 || semaphore == 1) {
            semaphore += 1
            Log.d("2SEMAPHORE handleAutoclick", semaphore.toString())

            val xClickInt = xClick
            val yClickInt = yClick

            if (isAccessibilityServiceEnabled(this, AutoclickService::class.java)) {
                val autoclickService = AutoclickService.instance
                autoclickService?.performClick(xClickInt, yClickInt, ydecalage)
                Log.d(null, "CLIQUE!!!!!!")
                // Reset flag après avoir effectué le clic
            } else {
                Log.e("AccessibilityService", "Le service d'accessibilité n'est pas activé.")
                val intent = Intent(this, AccessibilityPermissionActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            Log.d("3SEMAPHORE handleAutoclick", semaphore.toString())
            semaphore -= 1
            Log.d("4SEMAPHORE handleAutoclick", semaphore.toString())

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


//    private fun setupButtons(view: View) {
//        val xClick = view.findViewById<EditText>(R.id.xClick)
//        val yClick = view.findViewById<EditText>(R.id.yClick)
//        val startClickButtonM = view.findViewById<Button>(R.id.startClickButton)
//        val closeButton = view.findViewById<ImageButton>(R.id.closeButton)
//        val collapseButton = view.findViewById<ImageButton>(R.id.collapseButton)
//        val contentLayout = view.findViewById<LinearLayout>(R.id.contentLayout)
//        var isCollapsed = false
//
//        val createRoutineButton = view.findViewById<Button>(R.id.createRoutine)
//        var isCreatingRoutine = false
//        val clickPositions = mutableListOf<Pair<Float, Float>>()
//
//        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//        val fullScreenTouchableView = inflater.inflate(R.layout.transparent_overlay, null)
//
//        createRoutineButton.setOnClickListener {
//            isCreatingRoutine = !isCreatingRoutine
//            if (isCreatingRoutine) {
//                createRoutineButton.text = "Enregistrer"
//
//                windowManager.removeView(overlayView)
//
//
//                val layoutParams = WindowManager.LayoutParams(
//                    WindowManager.LayoutParams.MATCH_PARENT,
//                    WindowManager.LayoutParams.MATCH_PARENT,
//                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
//                    PixelFormat.TRANSLUCENT
//                )
//
//                //enlever menu
//                windowManager.removeView(autoclickMenuView)
//                // Ajouter la vue flottante
//                windowManager.addView(fullScreenTouchableView, layoutParams)
//                //réajouter le menu au premier plan
//                windowManager.addView(autoclickMenuView, autoclickMenuParams)
//
//                fullScreenTouchableView.setOnTouchListener { _, event ->
//                    Log.d("AAAAAAAAAAAAAAAAAAA", "BBBBBBBBBBB")
//
//                    val x = event.x
//                    val y = event.y
//                    clickPositions.add(Pair(x, y))
//
//                    // Mettre à jour les EditText si tu veux afficher les coordonnées
//                    xClick.setText(x.toString())
//                    yClick.setText(y.toString())
//
//
//                    val layoutParams = fullScreenTouchableView.layoutParams as WindowManager.LayoutParams
//                    layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
//                    windowManager.updateViewLayout(fullScreenTouchableView, layoutParams)
//
//
//                    // Attendre 50ms avant de rendre la vue non touchable
//                    Handler(Looper.getMainLooper()).postDelayed({
//                        handleAutoclick(xClick, yClick)
//                        Handler(Looper.getMainLooper()).postDelayed({
//                            layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
//                            windowManager.updateViewLayout(fullScreenTouchableView, layoutParams)
//                        }, 250)
//                    }, 50)
//
//                    false // Laisse l'événement continuer son chemin
//                }
//
//            } else {
//                createRoutineButton.text = "Créer routine"
//                windowManager.removeView(fullScreenTouchableView)
//                val params = WindowManager.LayoutParams(
//                    WindowManager.LayoutParams.MATCH_PARENT,
//                    WindowManager.LayoutParams.MATCH_PARENT,
//                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
//                            or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
//                    PixelFormat.TRANSLUCENT
//                )
//                windowManager.addView(overlayView, params)
//                // Ajoutez la logique pour "créer" ici, si nécessaire
//            }
//        }
//
//        collapseButton.setOnClickListener {
//            if (isCollapsed) {
//                // Ré-affiche le contenu
//                contentLayout.visibility = View.VISIBLE
//                collapseButton.setImageResource(android.R.drawable.ic_media_play) // Icône "Play"
//            } else {
//                // Cache le contenu
//                contentLayout.visibility = View.GONE
//                collapseButton.setImageResource(android.R.drawable.ic_media_pause) // Icône "Pause"
//            }
//            isCollapsed = !isCollapsed
//        }
//
//        startClickButtonM.setOnClickListener {
//            handleAutoclick(xClick, yClick)
//        }
//        closeButton.setOnClickListener {
//            stopSelf()
//            windowManager.removeView(overlayView)
//            windowManager.removeView(view)
//        }
//    }