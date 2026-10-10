package com.example.aisignnova;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class LearnSignsActivity extends AppCompatActivity {

    private LinearLayout learnSignsContainer;

    private static class SignItem {
        String title;
        String desc;
        boolean isFavorite;

        SignItem(String title, String desc) {
            this.title = title;
            this.desc = desc;
            this.isFavorite = false;
        }
    }

    private final List<SignItem> signItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_learn_signs);

        View topBar = findViewById(R.id.topBarLearn);
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 12, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        ImageButton btnBack = findViewById(R.id.btnBackLearn);
        learnSignsContainer = findViewById(R.id.learnSignsContainer);

        signItems.add(new SignItem("Namaste", "Both hands together in front of chest"));
        signItems.add(new SignItem("Happy", "Victory sign (V) with index and middle finger"));
        signItems.add(new SignItem("Bye", "Open palm and gentle wave side-to-side"));

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        populateSigns();
    }

    private void populateSigns() {
        if (learnSignsContainer == null) return;
        learnSignsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (SignItem item : signItems) {
            View itemView = inflater.inflate(R.layout.item_learn_sign, learnSignsContainer, false);

            TextView tvTitle = itemView.findViewById(R.id.tvLearnSignTitle);
            TextView tvDesc = itemView.findViewById(R.id.tvLearnSignDesc);
            ImageButton btnFav = itemView.findViewById(R.id.btnFavoriteToggle);

            if (tvTitle != null) tvTitle.setText(item.title);
            if (tvDesc != null) tvDesc.setText(item.desc);

            if (btnFav != null) {
                btnFav.setImageResource(item.isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                btnFav.setOnClickListener(v -> {
                    item.isFavorite = !item.isFavorite;
                    btnFav.setImageResource(item.isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                    Toast.makeText(LearnSignsActivity.this,
                            item.isFavorite ? "Added to favorites" : "Removed from favorites",
                            Toast.LENGTH_SHORT).show();
                });
            }

            learnSignsContainer.addView(itemView);
        }
    }
}