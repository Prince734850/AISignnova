package com.example.aisignnova;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private LinearLayout historyContainer;
    private TextView tabAll, tabNamaste, tabHappy, tabBye;
    private TextToSpeech tts;

    private static class HistoryItem {
        String name;
        String dateTime;

        HistoryItem(String name, String dateTime) {
            this.name = name;
            this.dateTime = dateTime;
        }
    }

    private final List<HistoryItem> allItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        View topBar = findViewById(R.id.topBarHistory);
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 12, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        ImageButton btnBack = findViewById(R.id.btnBackHistory);
        historyContainer = findViewById(R.id.historyContainer);

        tabAll = findViewById(R.id.tabHistoryAll);
        tabNamaste = findViewById(R.id.tabHistoryNamaste);
        tabHappy = findViewById(R.id.tabHistoryHappy);
        tabBye = findViewById(R.id.tabHistoryBye);

        tts = new TextToSpeech(this, this);

        allItems.add(new HistoryItem("Namaste", "04 Oct 2026 • 09:32 AM"));
        allItems.add(new HistoryItem("Happy", "04 Oct 2026 • 09:30 AM"));
        allItems.add(new HistoryItem("Bye", "04 Oct 2026 • 09:28 AM"));
        allItems.add(new HistoryItem("Namaste", "03 Oct 2026 • 04:15 PM"));
        allItems.add(new HistoryItem("Happy", "03 Oct 2026 • 02:10 PM"));

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        tabAll.setOnClickListener(v -> filterHistory("All"));
        tabNamaste.setOnClickListener(v -> filterHistory("Namaste"));
        tabHappy.setOnClickListener(v -> filterHistory("Happy"));
        tabBye.setOnClickListener(v -> filterHistory("Bye"));

        filterHistory("All");
    }

    private void filterHistory(String category) {
        updateTabStyles(category);
        if (historyContainer == null) return;
        historyContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (HistoryItem item : allItems) {
            if (category.equals("All") || item.name.equalsIgnoreCase(category)) {
                View itemView = inflater.inflate(R.layout.item_history_card, historyContainer, false);

                TextView tvName = itemView.findViewById(R.id.tvHistoryName);
                TextView tvDateTime = itemView.findViewById(R.id.tvHistoryDateTime);
                ImageButton btnPlay = itemView.findViewById(R.id.btnHistoryPlayAudio);

                if (tvName != null) tvName.setText(item.name);
                if (tvDateTime != null) tvDateTime.setText(item.dateTime);

                if (btnPlay != null) {
                    btnPlay.setOnClickListener(v -> speakText(item.name));
                }

                historyContainer.addView(itemView);
            }
        }
    }

    private void updateTabStyles(String selectedCategory) {
        resetTabStyle(tabAll);
        resetTabStyle(tabNamaste);
        resetTabStyle(tabHappy);
        resetTabStyle(tabBye);

        TextView selectedTab = tabAll;
        if (selectedCategory.equalsIgnoreCase("Namaste")) selectedTab = tabNamaste;
        else if (selectedCategory.equalsIgnoreCase("Happy")) selectedTab = tabHappy;
        else if (selectedCategory.equalsIgnoreCase("Bye")) selectedTab = tabBye;

        if (selectedTab != null) {
            selectedTab.setBackgroundResource(R.drawable.bg_tab_selected);
            selectedTab.setTextColor(getResources().getColor(R.color.white, getTheme()));
        }
    }

    private void resetTabStyle(TextView tab) {
        if (tab != null) {
            tab.setBackgroundResource(R.drawable.bg_tab_unselected);
            tab.setTextColor(getResources().getColor(R.color.tab_unselected_text, getTheme()));
        }
    }

    private void speakText(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            Toast.makeText(this, "Speaking: " + text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}