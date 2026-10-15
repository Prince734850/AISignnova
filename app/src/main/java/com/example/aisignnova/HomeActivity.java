package com.example.aisignnova;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        View topHeader = findViewById(R.id.homeHeader);
        if (topHeader != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topHeader, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 16, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        View bottomNavRow = findViewById(R.id.bottomNavIconsRow);
        if (bottomNavRow != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavRow, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + 16);
                return insets;
            });
        }

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        ImageButton btnMenu = findViewById(R.id.btnMenu);
        ImageButton btnNotification = findViewById(R.id.btnNotification);
        ImageButton btnProfileTop = findViewById(R.id.btnProfile);

        Button btnStartTranslating = findViewById(R.id.btnStartTranslating);

        LinearLayout cardProfile = findViewById(R.id.cardProfile);
        LinearLayout cardHistory = findViewById(R.id.cardHistory);
        LinearLayout cardLearn = findViewById(R.id.cardLearn);
        LinearLayout cardSettings = findViewById(R.id.cardSettings);

        setupNavigationDrawerMenu();
        updateDrawerHeaderEmail();

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        if (btnNotification != null) {
            btnNotification.setOnClickListener(v ->
                    Toast.makeText(HomeActivity.this, "No new notifications", Toast.LENGTH_SHORT).show()
            );
        }

        View.OnClickListener openProfile = v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class));

        if (btnProfileTop != null) btnProfileTop.setOnClickListener(openProfile);
        if (cardProfile != null) cardProfile.setOnClickListener(openProfile);

        if (cardHistory != null) {
            cardHistory.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, HistoryActivity.class)));
        }

        if (cardLearn != null) {
            cardLearn.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, LearnSignsActivity.class)));
        }

        if (cardSettings != null) {
            cardSettings.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, SettingsActivity.class)));
        }

        if (btnStartTranslating != null) {
            btnStartTranslating.setOnClickListener(v -> {
                Log.d("HomeActivity", "START_TRANSLATING_CLICKED");
                startActivity(new Intent(HomeActivity.this, CameraActivity.class));
            });
        }
    }

    private void setupNavigationDrawerMenu() {
        if (navigationView == null) return;

        Menu menu = navigationView.getMenu();
        menu.clear();
        menu.add(0, 1, 0, "Home").setIcon(R.drawable.signnova_logo);
        menu.add(0, 2, 1, "Profile").setIcon(R.drawable.ic_person);
        menu.add(0, 3, 2, "History").setIcon(R.drawable.ic_history);
        menu.add(0, 4, 3, "Learn Signs").setIcon(R.drawable.ic_learn_signs);
        menu.add(0, 5, 4, "Settings").setIcon(R.drawable.ic_settings);
        menu.add(0, 6, 5, "Help & Support").setIcon(R.drawable.ic_email);
        menu.add(0, 7, 6, "Logout").setIcon(R.drawable.ic_logout);

        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            int id = item.getItemId();
            if (id == 2) {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
            } else if (id == 3) {
                startActivity(new Intent(HomeActivity.this, HistoryActivity.class));
            } else if (id == 4) {
                startActivity(new Intent(HomeActivity.this, LearnSignsActivity.class));
            } else if (id == 5) {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
            } else if (id == 6) {
                Toast.makeText(HomeActivity.this, "Support: support@aisignnova.com", Toast.LENGTH_LONG).show();
            } else if (id == 7) {
                handleLogout();
            }
            return true;
        });
    }

    private void updateDrawerHeaderEmail() {
        if (navigationView == null) return;
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView tvEmail = headerView.findViewById(R.id.navHeaderEmail);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (tvEmail != null && user != null && user.getEmail() != null) {
                tvEmail.setText(user.getEmail());
            }
        }
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(HomeActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}