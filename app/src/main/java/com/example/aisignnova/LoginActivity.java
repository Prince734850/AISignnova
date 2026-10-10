package com.example.aisignnova;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private EditText regFullNameInput, regEmailInput, regPasswordInput, regConfirmPasswordInput;
    private TextView tabLogin, tabRegister;
    private LinearLayout loginFormContainer, registerFormContainer;
    private ImageView btnEyePassword, btnRegEyePassword;

    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;
    private boolean isRegPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        View topLogo = findViewById(R.id.loginLogo);
        if (topLogo != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topLogo, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 16, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        tabLogin = findViewById(R.id.tabLogin);
        tabRegister = findViewById(R.id.tabRegister);
        loginFormContainer = findViewById(R.id.loginFormContainer);
        registerFormContainer = findViewById(R.id.registerFormContainer);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        btnEyePassword = findViewById(R.id.btnEyePassword);
        Button loginButton = findViewById(R.id.loginActionButton);
        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);
        LinearLayout btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        TextView registerText = findViewById(R.id.registerText);

        regFullNameInput = findViewById(R.id.regFullNameInput);
        regEmailInput = findViewById(R.id.regEmailInput);
        regPasswordInput = findViewById(R.id.regPasswordInput);
        regConfirmPasswordInput = findViewById(R.id.regConfirmPasswordInput);
        btnRegEyePassword = findViewById(R.id.btnRegEyePassword);
        Button regActionButton = findViewById(R.id.regActionButton);
        TextView loginLinkText = findViewById(R.id.loginLinkText);

        tabLogin.setOnClickListener(v -> showLoginForm());
        tabRegister.setOnClickListener(v -> showRegisterForm());

        btnEyePassword.setOnClickListener(v -> togglePasswordVisibility());
        if (btnRegEyePassword != null) {
            btnRegEyePassword.setOnClickListener(v -> toggleRegPasswordVisibility());
        }

        loginButton.setOnClickListener(v -> loginUser());

        if (forgotPasswordText != null) {
            forgotPasswordText.setOnClickListener(v -> handleForgotPassword());
        }

        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v ->
                    Toast.makeText(LoginActivity.this, "Google Sign-In selected", Toast.LENGTH_SHORT).show()
            );
        }

        registerText.setOnClickListener(v -> showRegisterForm());

        if (regActionButton != null) {
            regActionButton.setOnClickListener(v -> registerUserFromTab());
        }

        if (loginLinkText != null) {
            loginLinkText.setOnClickListener(v -> showLoginForm());
        }
    }

    private void showLoginForm() {
        tabLogin.setBackgroundResource(R.drawable.bg_tab_selected);
        tabLogin.setTextColor(getResources().getColor(R.color.white, getTheme()));

        tabRegister.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabRegister.setTextColor(getResources().getColor(R.color.tab_unselected_text, getTheme()));

        loginFormContainer.setVisibility(View.VISIBLE);
        registerFormContainer.setVisibility(View.GONE);
    }

    private void showRegisterForm() {
        tabRegister.setBackgroundResource(R.drawable.bg_tab_selected);
        tabRegister.setTextColor(getResources().getColor(R.color.white, getTheme()));

        tabLogin.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabLogin.setTextColor(getResources().getColor(R.color.tab_unselected_text, getTheme()));

        loginFormContainer.setVisibility(View.GONE);
        registerFormContainer.setVisibility(View.VISIBLE);
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnEyePassword.setImageResource(R.drawable.ic_eye);
        } else {
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnEyePassword.setImageResource(R.drawable.ic_eye_off);
        }
        isPasswordVisible = !isPasswordVisible;
        passwordInput.setSelection(passwordInput.getText().length());
    }

    private void toggleRegPasswordVisibility() {
        if (regPasswordInput == null) return;
        if (isRegPasswordVisible) {
            regPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnRegEyePassword.setImageResource(R.drawable.ic_eye);
        } else {
            regPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnRegEyePassword.setImageResource(R.drawable.ic_eye_off);
        }
        isRegPasswordVisible = !isRegPasswordVisible;
        regPasswordInput.setSelection(regPasswordInput.getText().length());
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(LoginActivity.this, "Login failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerUserFromTab() {
        String name = regFullNameInput.getText().toString().trim();
        String email = regEmailInput.getText().toString().trim();
        String password = regPasswordInput.getText().toString().trim();
        String confirmPass = regConfirmPasswordInput.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && !name.isEmpty()) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates);
                        }
                        Toast.makeText(LoginActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(LoginActivity.this, "Registration Failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleForgotPassword() {
        String email = emailInput.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email above to reset password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                    } else {
                        String error = (task.getException() != null) ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(LoginActivity.this, "Failed to send reset email: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}