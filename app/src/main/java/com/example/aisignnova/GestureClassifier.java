package com.example.aisignnova;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.SystemClock;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import java.util.List;

/**
 * Real-time Gesture Recognition using MediaPipe Hand Landmarker in LIVE_STREAM mode.
 */
public class GestureClassifier {
    private static final String TAG = "GestureClassifier";
    private static final String MODEL_FILE = "hand_landmarker.task";

    public enum Gesture {
        NAMASTE("Namaste"),
        HAPPY("Happy"),
        BYE("Bye"),
        NONE("Waiting for signs...");

        private final String displayName;

        Gesture(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public interface RecognitionCallback {
        void onGestureDetected(Gesture gesture, float confidence);
    }

    private final RecognitionCallback callback;
    private HandLandmarker handLandmarker;
    private boolean isInitialized = false;

    public GestureClassifier(Context context, RecognitionCallback callback) {
        this.callback = callback;
        initializeHandLandmarker(context);
    }

    private void initializeHandLandmarker(Context context) {
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath(MODEL_FILE)
                    .build();

            HandLandmarker.HandLandmarkerOptions options = HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinHandDetectionConfidence(0.35f)
                    .setMinTrackingConfidence(0.35f)
                    .setMinHandPresenceConfidence(0.35f)
                    .setNumHands(2) // 2 hands for Namaste
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener(this::onResult)
                    .setErrorListener(this::onError)
                    .build();

            handLandmarker = HandLandmarker.createFromOptions(context, options);
            isInitialized = true;
            Log.d(TAG, "MediaPipe HandLandmarker initialized successfully in LIVE_STREAM mode");
        } catch (Exception e) {
            Log.e(TAG, "MEDIAPIPE_INITIALIZATION_FAILED: " + e.getMessage(), e);
            Log.e(TAG, "CRITICAL ERROR: Ensure 'hand_landmarker.task' asset is present in app/src/main/assets/");
        }
    }

    public void classify(ImageProxy imageProxy) {
        if (!isInitialized || handLandmarker == null) {
            Log.e(TAG, "MediaPipe HandLandmarker not initialized. Skipping frame.");
            imageProxy.close();
            return;
        }

        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        Log.d(TAG, "Camera frame received. ImageProxy rotation: " + rotationDegrees);

        try {
            Bitmap bitmap = imageProxy.toBitmap();

            // Rotate bitmap to upright portrait orientation for MediaPipe if necessary
            if (rotationDegrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
            long timestamp = SystemClock.uptimeMillis();

            Log.d(TAG, "Sending frame to MediaPipe detectAsync at timestamp: " + timestamp);
            handLandmarker.detectAsync(mpImage, timestamp);

        } catch (Exception e) {
            Log.e(TAG, "Error in classify image conversion / MediaPipe submission: " + e.getMessage(), e);
        } finally {
            imageProxy.close();
        }
    }

    private void onResult(HandLandmarkerResult result, MPImage image) {
        Log.d(TAG, "MediaPipe result received");

        if (result == null || result.landmarks().isEmpty()) {
            Log.d(TAG, "No hand detected");
            callback.onGestureDetected(Gesture.NONE, 0.0f);
            return;
        }

        int numberOfHands = result.landmarks().size();
        Log.d(TAG, "Hands detected: " + numberOfHands);

        // 1. NAMASTE (2 folded hands close together)
        if (numberOfHands == 2) {
            float namasteConf = calculateNamasteConfidence(result);
            if (namasteConf > 0.55f) {
                Log.d(TAG, "Detected gesture = NAMASTE (2 hands), confidence: " + namasteConf);
                callback.onGestureDetected(Gesture.NAMASTE, namasteConf);
                return;
            }
        }

        // Single hand gestures
        if (numberOfHands >= 1) {
            List<NormalizedLandmark> landmarks = result.landmarks().get(0);
            Log.d(TAG, "Hand 1 landmarks count: " + landmarks.size());

            // 2. HAPPY (Victory V-Sign ✌️)
            float happyConf = calculateHappyConfidence(landmarks);
            if (happyConf > 0.65f) {
                Log.d(TAG, "Detected gesture = HAPPY, confidence: " + happyConf);
                callback.onGestureDetected(Gesture.HAPPY, happyConf);
                return;
            }

            // 3. BYE (Open Palm 🖐️)
            float byeConf = calculateByeConfidence(landmarks);
            if (byeConf > 0.65f) {
                Log.d(TAG, "Detected gesture = BYE, confidence: " + byeConf);
                callback.onGestureDetected(Gesture.BYE, byeConf);
                return;
            }

            // Single hand folded Namaste
            float singleNamasteConf = calculateSingleHandNamasteConfidence(landmarks);
            if (singleNamasteConf > 0.65f) {
                Log.d(TAG, "Detected gesture = NAMASTE (single hand folded), confidence: " + singleNamasteConf);
                callback.onGestureDetected(Gesture.NAMASTE, singleNamasteConf);
                return;
            }
        }

        Log.d(TAG, "Hand detected, but gesture = UNKNOWN/NONE");
        callback.onGestureDetected(Gesture.NONE, 0.0f);
    }

    private void onError(RuntimeException e) {
        Log.e(TAG, "MediaPipe HandLandmarker Exception: " + e.getMessage(), e);
    }

    private boolean isFingerExtended(List<NormalizedLandmark> lm, int tipIdx, int pipIdx) {
        NormalizedLandmark wrist = lm.get(0);
        NormalizedLandmark tip = lm.get(tipIdx);
        NormalizedLandmark pip = lm.get(pipIdx);

        float dTip = (float) Math.hypot(tip.x() - wrist.x(), tip.y() - wrist.y());
        float dPip = (float) Math.hypot(pip.x() - wrist.x(), pip.y() - wrist.y());

        return dTip > (dPip * 1.10f);
    }

    private boolean isFingerFolded(List<NormalizedLandmark> lm, int tipIdx, int mcpIdx) {
        NormalizedLandmark wrist = lm.get(0);
        NormalizedLandmark tip = lm.get(tipIdx);
        NormalizedLandmark mcp = lm.get(mcpIdx);

        float dTip = (float) Math.hypot(tip.x() - wrist.x(), tip.y() - wrist.y());
        float dMcp = (float) Math.hypot(mcp.x() - wrist.x(), mcp.y() - wrist.y());

        return dTip <= (dMcp * 1.30f);
    }

    private float calculateNamasteConfidence(HandLandmarkerResult result) {
        List<NormalizedLandmark> h1 = result.landmarks().get(0);
        List<NormalizedLandmark> h2 = result.landmarks().get(1);

        float wristDistance = (float) Math.hypot(h1.get(0).x() - h2.get(0).x(), h1.get(0).y() - h2.get(0).y());

        boolean h1Up = isFingerExtended(h1, 8, 6) && isFingerExtended(h1, 12, 10);
        boolean h2Up = isFingerExtended(h2, 8, 6) && isFingerExtended(h2, 12, 10);

        if (wristDistance < 0.45f && h1Up && h2Up) {
            return Math.min(0.96f, 0.75f + (0.45f - wristDistance) * 0.5f);
        }

        return 0.0f;
    }

    private float calculateSingleHandNamasteConfidence(List<NormalizedLandmark> lm) {
        boolean allExtended = isFingerExtended(lm, 8, 6) &&
                isFingerExtended(lm, 12, 10) &&
                isFingerExtended(lm, 16, 14) &&
                isFingerExtended(lm, 20, 18);

        // Check if fingers are pressed tight together (low spread between index and pinky tips)
        float fingerSpread = (float) Math.hypot(lm.get(8).x() - lm.get(20).x(), lm.get(8).y() - lm.get(20).y());

        if (allExtended && fingerSpread < 0.12f) {
            return 0.88f;
        }

        return 0.0f;
    }

    private float calculateByeConfidence(List<NormalizedLandmark> lm) {
        boolean indexExt = isFingerExtended(lm, 8, 6);
        boolean middleExt = isFingerExtended(lm, 12, 10);
        boolean ringExt = isFingerExtended(lm, 16, 14);
        boolean pinkyExt = isFingerExtended(lm, 20, 18);

        float fingerSpread = (float) Math.hypot(lm.get(8).x() - lm.get(20).x(), lm.get(8).y() - lm.get(20).y());

        if (indexExt && middleExt && ringExt && pinkyExt && fingerSpread >= 0.12f) {
            return Math.min(0.95f, 0.80f + fingerSpread * 0.4f);
        }

        return 0.0f;
    }

    private float calculateHappyConfidence(List<NormalizedLandmark> lm) {
        // Victory V-Sign: Index & Middle extended, Ring & Pinky folded
        boolean indexExt = isFingerExtended(lm, 8, 6);
        boolean middleExt = isFingerExtended(lm, 12, 10);
        boolean ringClosed = isFingerFolded(lm, 16, 13);
        boolean pinkyClosed = isFingerFolded(lm, 20, 17);

        if (indexExt && middleExt && ringClosed && pinkyClosed) {
            float vSpread = (float) Math.hypot(lm.get(8).x() - lm.get(12).x(), lm.get(8).y() - lm.get(12).y());
            return Math.min(0.95f, 0.82f + vSpread * 0.5f);
        }

        return 0.0f;
    }

    public void close() {
        if (handLandmarker != null) {
            try {
                handLandmarker.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing HandLandmarker: " + e.getMessage());
            }
            isInitialized = false;
            Log.d(TAG, "MediaPipe HandLandmarker closed successfully");
        }
    }
}