package com.example.project22;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Button;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ResultsActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        int score = getIntent().getIntExtra("score", 0);
        int total = getIntent().getIntExtra("total", 0);
        int attempted = getIntent().getIntExtra("attempted", 0);
        boolean auto = getIntent().getBooleanExtra("auto", false);

        TextView tvSummary = findViewById(R.id.tvSummary);
        TextView tvScoreBig = findViewById(R.id.tvScoreBig);
        TextView tvPercent = findViewById(R.id.tvPercent);
        ProgressBar circle = findViewById(R.id.circle);
        Button btnRetake = findViewById(R.id.btnRetake);
        Button btnHome = findViewById(R.id.btnHome);

        int percent = (total > 0) ? Math.round((score * 100f) / total) : 0;
        circle.setProgress(percent);
        tvScoreBig.setText(score + "/" + total);
        tvPercent.setText(percent + "%");

        // Update score in Firestore
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            
            // Create a map with the score update
            Map<String, Object> updates = new HashMap<>();
            updates.put("score", FieldValue.increment(score));
            
            // Update the user's score in Firestore
            db.collection("roles").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ResultsActivity", "Score updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("ResultsActivity", "Error updating score", e);
                });
        }

        String msg = "Attempted: " + attempted + "/" + total + "\n" +
                (auto ? "Auto-submitted due to app switch or timeout" : "Submitted by you");
        tvSummary.setText(msg);

        btnRetake.setOnClickListener(v -> {
            // Retake: go back to selection screen
            Intent i = new Intent(ResultsActivity.this, StudentSelectActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });

        btnHome.setOnClickListener(v -> {
            Intent i = new Intent(ResultsActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });

        // Add leaderboard button click listener
        Button btnLeaderboard = findViewById(R.id.btnLeaderboard);
        btnLeaderboard.setOnClickListener(v -> {
            Intent intent = new Intent(ResultsActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });
    }
}
