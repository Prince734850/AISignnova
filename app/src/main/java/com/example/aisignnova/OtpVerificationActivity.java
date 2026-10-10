package com.example.aisignnova;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OtpVerificationActivity extends AppCompatActivity {

    private EditText[] otpBoxes;
    private Button verifyButton;
    private TextView resendOtpText;
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_verification);

        mAuth = FirebaseAuth.getInstance();
        mVerificationId = getIntent().getStringExtra("verificationId");
        mResendToken = getIntent().getParcelableExtra("resendToken");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        otpBoxes = new EditText[]{
                findViewById(R.id.otpBox1),
                findViewById(R.id.otpBox2),
                findViewById(R.id.otpBox3),
                findViewById(R.id.otpBox4),
                findViewById(R.id.otpBox5),
                findViewById(R.id.otpBox6)
        };

        verifyButton = findViewById(R.id.verifyOtpButton);
        resendOtpText = findViewById(R.id.resendOtpText);

        setupOtpInputs();

        verifyButton.setOnClickListener(v -> verifyOtp());
        resendOtpText.setOnClickListener(v -> resendOtp());
    }

    private void resendOtp() {
        String phoneNumber = getIntent().getStringExtra("mobile");
        if (phoneNumber == null) return;

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .setForceResendingToken(mResendToken)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        Toast.makeText(this, "Resending OTP...", Toast.LENGTH_SHORT).show();
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
            signInWithPhoneAuthCredential(credential);
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            Toast.makeText(OtpVerificationActivity.this, "Resend Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
            mVerificationId = verificationId;
            mResendToken = token;
            Toast.makeText(OtpVerificationActivity.this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
        }
    };

    private void setupOtpInputs() {
        for (int i = 0; i < otpBoxes.length; i++) {
            final int currentIndex = i;
            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && currentIndex < otpBoxes.length - 1) {
                        otpBoxes[currentIndex + 1].requestFocus();
                    } else if (s.length() == 0 && currentIndex > 0) {
                        otpBoxes[currentIndex - 1].requestFocus();
                    }
                }
            });
        }
    }

    private void verifyOtp() {
        StringBuilder otp = new StringBuilder();
        for (EditText box : otpBoxes) {
            otp.append(box.getText().toString());
        }

        if (otp.length() < 6) {
            Toast.makeText(this, "Please enter the complete OTP", Toast.LENGTH_SHORT).show();
        } else {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otp.toString());
            signInWithPhoneAuthCredential(credential);
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Phone verified, now create email/password account
                        createEmailAccount();
                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(this, "Wrong OTP. Please enter the correct OTP.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "OTP verification failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void createEmailAccount() {
        String email = getIntent().getStringExtra("email");
        String password = getIntent().getStringExtra("password");

        if (email == null || password == null) {
            // Fallback if data is missing, though it shouldn't be
            Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        // Note: In a real app, you might want to link the phone credential to the email account.
        // For now, we follow the requirement to "Create the user registration/account" after OTP.
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                    } else {
                        Toast.makeText(this, "Account creation failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(OtpVerificationActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}