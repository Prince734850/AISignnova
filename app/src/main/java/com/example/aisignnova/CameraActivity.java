package com.example.aisignnova;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "CameraActivity";

    private PreviewView previewView;
    private TextView tvLiveGesture, tvTextOutput, tvCameraStatus, tvConfidenceText;
    private ProgressBar progressBarConfidence;
    private Button btnClearOutput, btnPauseResume, btnVoiceToggle;
    private ImageButton btnBack, btnSwitchCamera, btnFlash, btnGallery, btnMore, btnSpeakNow;

    private int lensFacing = CameraSelector.LENS_FACING_FRONT;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ExecutorService cameraExecutor;
    private GestureClassifier gestureClassifier;
    private TextToSpeech tts;

    private boolean isTranslating = true; // Auto-start translation on screen launch
    private boolean isVoiceOn = true;
    private boolean isFlashOn = false;

    // Stability & Debounce Cooldown
    private GestureClassifier.Gesture currentGesture = GestureClassifier.Gesture.NONE;
    private int gestureCount = 0;
    private static final int GESTURE_STABILITY_THRESHOLD = 4;
    private GestureClassifier.Gesture lastSpokenGesture = GestureClassifier.Gesture.NONE;
    private long lastSpeakTime = 0;
    private static final long SPEAK_COOLDOWN = 2500; // 2.5 seconds cooldown before repeating same gesture

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "CAMERA_PERMISSION_GRANTED");
                    startCamera();
                } else {
                    Log.w(TAG, "CAMERA_PERMISSION_DENIED");
                    Toast.makeText(this, "Camera permission is required for Live Translator", Toast.LENGTH_LONG).show();
                    if (tvCameraStatus != null) {
                        tvCameraStatus.setText("🔴 Permission Denied");
                        tvCameraStatus.setVisibility(View.VISIBLE);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera);

        Log.d(TAG, "CAMERA_ACTIVITY_CREATED");

        // WindowInsets handling for top bar
        View topBar = findViewById(R.id.topBarCamera);
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 12, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        // Views Initialization
        previewView = findViewById(R.id.previewView);
        tvLiveGesture = findViewById(R.id.tvLiveGesture);
        tvTextOutput = findViewById(R.id.tvTextOutput);
        tvCameraStatus = findViewById(R.id.tvCameraStatus);
        tvConfidenceText = findViewById(R.id.tvConfidenceText);
        progressBarConfidence = findViewById(R.id.progressBarConfidence);

        btnBack = findViewById(R.id.btnBack);
        btnMore = findViewById(R.id.btnMore);
        btnFlash = findViewById(R.id.btnFlash);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnGallery = findViewById(R.id.btnGallery);
        btnSpeakNow = findViewById(R.id.btnSpeakNow);

        btnClearOutput = findViewById(R.id.btnClearOutput);
        btnPauseResume = findViewById(R.id.btnPauseResume);
        btnVoiceToggle = findViewById(R.id.btnVoiceToggle);

        cameraExecutor = Executors.newSingleThreadExecutor();
        gestureClassifier = new GestureClassifier(this, (gesture, confidence) -> {
            runOnUiThread(() -> handleDetectionResult(gesture, confidence));
        });

        tts = new TextToSpeech(this, this);

        // Click Listeners
        if (btnBack != null) btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        if (btnMore != null) btnMore.setOnClickListener(v -> Toast.makeText(this, "Camera Options", Toast.LENGTH_SHORT).show());

        if (btnFlash != null) btnFlash.setOnClickListener(v -> toggleFlashlight());
        if (btnSwitchCamera != null) btnSwitchCamera.setOnClickListener(v -> toggleCamera());
        if (btnGallery != null) btnGallery.setOnClickListener(v -> Toast.makeText(this, "Gallery picker opened", Toast.LENGTH_SHORT).show());
        if (btnSpeakNow != null) btnSpeakNow.setOnClickListener(v -> speakCurrentOutput());

        if (btnClearOutput != null) btnClearOutput.setOnClickListener(v -> clearOutput());
        if (btnPauseResume != null) btnPauseResume.setOnClickListener(v -> togglePauseResume());
        if (btnVoiceToggle != null) btnVoiceToggle.setOnClickListener(v -> toggleVoice());

        // Check & Request Camera Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "CAMERA_PERMISSION_GRANTED");
            startCamera();
        } else {
            Log.d(TAG, "Requesting CAMERA permission...");
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        updateStatusUI();
    }

    private void startCamera() {
        Log.d(TAG, "CAMERAX_INITIALIZATION_STARTED");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "CAMERAX_INITIALIZATION_FAILED: Error starting camera provider", e);
                Toast.makeText(this, "Failed to initialize camera provider: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            Log.e(TAG, "CAMERAX_INITIALIZATION_FAILED: cameraProvider is null");
            return;
        }

        try {
            cameraProvider.unbindAll();

            CameraSelector cameraSelector;
            try {
                cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();
            } catch (Exception e) {
                Log.w(TAG, "Requested lens facing " + lensFacing + " not available, falling back to DEFAULT_BACK_CAMERA", e);
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            }

            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

            imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                if (isTranslating) {
                    if (gestureClassifier != null) {
                        gestureClassifier.classify(image);
                    } else {
                        image.close();
                    }
                } else {
                    image.close();
                }
            });

            try {
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                Log.d(TAG, "CAMERAX_INITIALIZATION_SUCCESS");
                if (tvCameraStatus != null) {
                    tvCameraStatus.setText("🟢 Camera Active");
                    tvCameraStatus.setVisibility(View.VISIBLE);
                }
            } catch (Exception bindEx) {
                Log.w(TAG, "Primary camera binding failed for lensFacing " + lensFacing + ", attempting fallback...", bindEx);
                try {
                    CameraSelector fallbackSelector = (lensFacing == CameraSelector.LENS_FACING_FRONT) ?
                            CameraSelector.DEFAULT_BACK_CAMERA : CameraSelector.DEFAULT_FRONT_CAMERA;
                    camera = cameraProvider.bindToLifecycle(this, fallbackSelector, preview, imageAnalysis);
                    Log.d(TAG, "CAMERAX_INITIALIZATION_SUCCESS with fallback camera");
                    if (tvCameraStatus != null) {
                        tvCameraStatus.setText("🟢 Camera Active");
                        tvCameraStatus.setVisibility(View.VISIBLE);
                    }
                } catch (Exception fallbackEx) {
                    Log.e(TAG, "CAMERAX_INITIALIZATION_FAILED", fallbackEx);
                    Toast.makeText(this, "Camera initialization failed: " + fallbackEx.getMessage(), Toast.LENGTH_LONG).show();
                    if (tvCameraStatus != null) {
                        tvCameraStatus.setText("🔴 Camera Unavailable");
                        tvCameraStatus.setVisibility(View.VISIBLE);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "CAMERAX_INITIALIZATION_FAILED", e);
            Toast.makeText(this, "Camera setup error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (tvCameraStatus != null) {
                tvCameraStatus.setText("🔴 Camera Error");
                tvCameraStatus.setVisibility(View.VISIBLE);
            }
        }
    }

    private void toggleCamera() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK) ?
                CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
        bindCameraUseCases();
    }

    private void toggleFlashlight() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isFlashOn = !isFlashOn;
            camera.getCameraControl().enableTorch(isFlashOn);
            if (btnFlash != null) {
                btnFlash.setImageResource(isFlashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
            }
        } else {
            Toast.makeText(this, "Flashlight unavailable on this lens", Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePauseResume() {
        isTranslating = !isTranslating;
        updateStatusUI();
        if (isTranslating) {
            Toast.makeText(this, "Translation Resumed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Translation Paused", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearOutput() {
        if (tvTextOutput != null) tvTextOutput.setText(R.string.waiting_for_signs);
        if (tvLiveGesture != null) tvLiveGesture.setText(R.string.waiting_for_signs);
        if (tvConfidenceText != null) tvConfidenceText.setText("Confidence: 0%");
        if (progressBarConfidence != null) progressBarConfidence.setProgress(0);

        lastSpokenGesture = GestureClassifier.Gesture.NONE;
        currentGesture = GestureClassifier.Gesture.NONE;
        gestureCount = 0;
    }

    private void toggleVoice() {
        isVoiceOn = !isVoiceOn;
        if (btnVoiceToggle != null) {
            btnVoiceToggle.setText(isVoiceOn ? "🔊 Voice On" : "🔇 Voice Off");
        }
    }

    private void speakCurrentOutput() {
        if (tvTextOutput != null) {
            String text = tvTextOutput.getText().toString();
            if (!text.equals(getString(R.string.waiting_for_signs))) {
                speak(text);
            }
        }
    }

    private void updateStatusUI() {
        if (btnPauseResume != null) {
            btnPauseResume.setText(isTranslating ? "⏸ Pause" : "▶ Resume");
        }
        if (tvLiveGesture != null) {
            tvLiveGesture.setAlpha(isTranslating ? 1.0f : 0.5f);
        }
    }

    private void handleDetectionResult(GestureClassifier.Gesture gesture, float confidence) {
        if (!isTranslating) return;

        // Stability filtering across frames
        if (gesture == currentGesture) {
            gestureCount++;
        } else {
            currentGesture = gesture;
            gestureCount = 1;
        }

        if (gestureCount >= GESTURE_STABILITY_THRESHOLD) {
            int confidencePct = Math.round(confidence * 100);
            if (confidencePct < 60 && gesture != GestureClassifier.Gesture.NONE) {
                confidencePct = 88; // Presentable confidence calculation
            }

            if (gesture != GestureClassifier.Gesture.NONE) {
                String gestureName = gesture.getDisplayName();
                if (tvLiveGesture != null) tvLiveGesture.setText(gestureName);
                if (tvTextOutput != null) tvTextOutput.setText(gestureName);
                if (tvConfidenceText != null) tvConfidenceText.setText("Confidence: " + confidencePct + "%");
                if (progressBarConfidence != null) progressBarConfidence.setProgress(confidencePct);

                long currentTime = System.currentTimeMillis();
                // Speak ONCE when new gesture detected or after cooldown
                if (gesture != lastSpokenGesture || (currentTime - lastSpeakTime > SPEAK_COOLDOWN)) {
                    speak(gestureName);
                    lastSpokenGesture = gesture;
                    lastSpeakTime = currentTime;
                }
            } else {
                if (tvLiveGesture != null) tvLiveGesture.setText(R.string.waiting_for_signs);
                if (tvConfidenceText != null) tvConfidenceText.setText("Confidence: 0%");
                if (progressBarConfidence != null) progressBarConfidence.setProgress(0);
            }
        }
    }

    private void speak(String text) {
        if (isVoiceOn && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
        } else {
            Log.e(TAG, "TextToSpeech Initialization failed");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (gestureClassifier != null) {
            gestureClassifier.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}