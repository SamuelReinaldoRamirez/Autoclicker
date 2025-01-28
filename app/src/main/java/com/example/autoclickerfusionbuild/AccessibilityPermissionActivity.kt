package com.example.autoclickerfusionbuild

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

class AccessibilityPermissionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ouvrir les paramètres d'accessibilité
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)

        // Ferme l'activité après avoir ouvert les paramètres
        finish()
    }
}
