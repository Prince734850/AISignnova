package com.example.aisignnova;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private boolean isBindingUi = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        View topBar = findViewById(R.id.topBarSettings);
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 12, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        ImageButton btnBack = findViewById(R.id.btnBackSettings);
        SwitchMaterial switchSound = findViewById(R.id.switchSound);
        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);

        RelativeLayout rowLanguage = findViewById(R.id.rowLanguage);
        RelativeLayout rowClearHistory = findViewById(R.id.rowClearHistory);
        RelativeLayout rowAboutApp = findViewById(R.id.rowAboutApp);
        RelativeLayout rowHelpSupport = findViewById(R.id.rowHelpSupport);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        if (switchDarkMode != null) {
            switchDarkMode.setChecked(ThemeHelper.isDarkMode(this));
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isBindingUi) return;
                String targetTheme = isChecked ? ThemeHelper.THEME_DARK : ThemeHelper.THEME_LIGHT;
                ThemeHelper.setTheme(SettingsActivity.this, targetTheme);
            });
        }

        if (switchSound != null) {
            switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isBindingUi) return;
                Toast.makeText(SettingsActivity.this, isChecked ? "Sound Effects Enabled" : "Sound Effects Muted", Toast.LENGTH_SHORT).show();
            });
        }

        if (rowLanguage != null) {
            rowLanguage.setOnClickListener(v ->
                    Toast.makeText(SettingsActivity.this, "Default language: English (US)", Toast.LENGTH_SHORT).show()
            );
        }

        if (rowClearHistory != null) {
            rowClearHistory.setOnClickListener(v ->
                    Toast.makeText(SettingsActivity.this, "Detection history cleared", Toast.LENGTH_SHORT).show()
            );
        }

        if (rowAboutApp != null) {
            rowAboutApp.setOnClickListener(v -> showAboutDialog());
        }

        if (rowHelpSupport != null) {
            rowHelpSupport.setOnClickListener(v ->
                    Toast.makeText(SettingsActivity.this, "Contact us at support@aisignnova.com", Toast.LENGTH_LONG).show()
            );
        }

        isBindingUi = false;
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Aisignnova v1.0")
                .setMessage("Aisignnova - Real-time Indian Sign Language Translation\nTagline: Bridging Hands, Building Understanding\n\nBuilt with Android CameraX, MediaPipe & Firebase.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}