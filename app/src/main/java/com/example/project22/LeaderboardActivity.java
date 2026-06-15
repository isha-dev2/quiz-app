package com.example.project22;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project22.adapters.LeaderboardAdapter;
import com.example.project22.models.LeaderboardEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<LeaderboardEntry> leaderboardList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.rvLeaderboard);
        Button btnBack = findViewById(R.id.btnBack);

        // Setup RecyclerView
        adapter = new LeaderboardAdapter(leaderboardList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load leaderboard data
        loadLeaderboardData();

        // Back button click listener
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadLeaderboardData() {
        // Show loading state
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        findViewById(R.id.tvEmptyState).setVisibility(View.GONE);
        
        // First try with server-side sorting
        try {
            db.collection("roles")
                    .whereEqualTo("role", "student")
                    .orderBy("score", Query.Direction.DESCENDING)
                    .orderBy(FieldPath.documentId())
                    .limit(50)
                    .get()
                    .addOnCompleteListener(serverTask -> {
                        if (serverTask.isSuccessful() && serverTask.getResult() != null) {
                            // If server-side sort works, process results
                            processLeaderboardData(serverTask.getResult());
                        } else {
                            // If server-side sort fails, fall back to client-side sorting
                            Log.d("LeaderboardActivity", "Falling back to client-side sorting");
                            fetchAndSortClientSide();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LeaderboardActivity", "Server-side sort failed, falling back to client-side", e);
                        fetchAndSortClientSide();
                    });
        } catch (Exception e) {
            Log.e("LeaderboardActivity", "Error in server-side query, falling back to client-side", e);
            fetchAndSortClientSide();
        }
    }
    
    private void fetchAndSortClientSide() {
        Log.d("LeaderboardActivity", "Starting client-side sort");
        
        // Fetch all student documents without server-side sorting
        db.collection("roles")
                .whereEqualTo("role", "student")
                .get()
                .addOnCompleteListener(task -> {
                    // Hide loading state
                    runOnUiThread(() -> findViewById(R.id.progressBar).setVisibility(View.GONE));
                    
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<QueryDocumentSnapshot> documents = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            documents.add(doc);
                            // Log the raw document data for debugging
                            Log.d("LeaderboardActivity", "Document data: " + doc.getData());
                        }
                        
                        Log.d("LeaderboardActivity", "Fetched " + documents.size() + " documents for client-side sorting");
                        
                        // Filter out documents with null or invalid data
                        List<QueryDocumentSnapshot> validDocuments = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : documents) {
                            try {
                                String email = doc.getString("email");
                                Long score = doc.getLong("score");
                                if (email != null && !email.isEmpty() && score != null) {
                                    validDocuments.add(doc);
                                }
                            } catch (Exception e) {
                                Log.e("LeaderboardActivity", "Error processing document: " + doc.getId(), e);
                            }
                        }
                        
                        // Sort by score in descending order
                        Collections.sort(validDocuments, (a, b) -> {
                            try {
                                Long scoreA = a.getLong("score");
                                Long scoreB = b.getLong("score");
                                if (scoreA == null) scoreA = 0L;
                                if (scoreB == null) scoreB = 0L;
                                return Long.compare(scoreB, scoreA);
                            } catch (Exception e) {
                                Log.e("LeaderboardActivity", "Error comparing scores", e);
                                return 0;
                            }
                        });
                        
                        // Limit to top 50
                        int count = Math.min(validDocuments.size(), 50);
                        Log.d("LeaderboardActivity", "Displaying top " + count + " valid entries");
                        
                        // Process the sorted results
                        leaderboardList.clear();
                        for (int i = 0; i < count; i++) {
                            QueryDocumentSnapshot doc = validDocuments.get(i);
                            try {
                                String email = doc.getString("email");
                                Long score = doc.getLong("score");
                                
                                if (email != null && score != null) {
                                    String displayName = email.split("@")[0];
                                    leaderboardList.add(new LeaderboardEntry(
                                            i + 1, // rank
                                            displayName,
                                            score.intValue()
                                    ));
                                    
                                    Log.d("LeaderboardActivity", "Added entry - " + 
                                          "Rank: " + (i + 1) + 
                                          ", Name: " + displayName + 
                                          ", Score: " + score);
                                }
                            } catch (Exception e) {
                                Log.e("LeaderboardActivity", "Error processing document: " + doc.getId(), e);
                            }
                        }
                        
                        // Update UI on the main thread
                        runOnUiThread(this::updateUI);
                    } else {
                        String error = task.getException() != null ? 
                                     task.getException().getMessage() : "Unknown error";
                        Log.e("LeaderboardActivity", "Error getting leaderboard: " + error);
                        showError("Failed to load leaderboard. Please check your connection and try again.");
                    }
                });
    }
    
    private void processLeaderboardData(com.google.firebase.firestore.QuerySnapshot result) {
        // Hide loading state
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        
        leaderboardList.clear();
        int rank = 1;
        
        Log.d("LeaderboardActivity", "Processing " + result.size() + " leaderboard entries");
        
        for (QueryDocumentSnapshot document : result) {
            try {
                String email = document.getString("email");
                Long score = document.getLong("score");
                String documentId = document.getId();
                
                Log.d("LeaderboardActivity", "Processing document " + documentId + 
                      ", email: " + email + ", score: " + score);
                
                if (email != null && score != null) {
                    String displayName = email.split("@")[0];
                    int scoreValue = score.intValue();
                    
                    Log.d("LeaderboardActivity", "Adding entry - Rank: " + rank + 
                          ", Name: " + displayName + ", Score: " + scoreValue);
                    
                    leaderboardList.add(new LeaderboardEntry(
                            rank++,
                            displayName,
                            scoreValue
                    ));
                } else {
                    Log.e("LeaderboardActivity", "Missing email or score in document: " + documentId);
                }
            } catch (Exception e) {
                Log.e("LeaderboardActivity", "Error processing document: " + document.getId(), e);
            }
        }
        
        Log.d("LeaderboardActivity", "Total entries processed: " + leaderboardList.size());
        updateUI();
    }
    
    // Helper method to update the UI
    private void updateUI() {
        runOnUiThread(() -> {
            if (leaderboardList.isEmpty()) {
                findViewById(R.id.tvEmptyState).setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                findViewById(R.id.tvEmptyState).setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        });
    }
    
    // Helper method to show error messages
    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            TextView tvEmptyState = findViewById(R.id.tvEmptyState);
            tvEmptyState.setText("Error loading leaderboard. Please try again.");
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        });
    }
}
