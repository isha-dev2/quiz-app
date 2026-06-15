package com.example.project22;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuestionManagementActivity extends AppCompatActivity {
    private static final String TAG = "QuestionImport";
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_management);

        // Initialize UI components
        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnUpdate = findViewById(R.id.btnUpdate);
        Button btnDelete = findViewById(R.id.btnDelete);
        Button btnImport = findViewById(R.id.btnImport);

        // Set click listeners
        btnAdd.setOnClickListener(v -> startActivity(new android.content.Intent(this, AddQuestionActivity.class)));
        btnUpdate.setOnClickListener(v -> startActivity(new android.content.Intent(this, UpdateQuestionActivity.class)));
        btnDelete.setOnClickListener(v -> startActivity(new android.content.Intent(this, DeleteQuestionActivity.class)));
        btnImport.setOnClickListener(v -> showImportConfirmation());
    }

    // Normalize question text for comparison (removes extra spaces and special characters)
    private String normalizeQuestionText(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")  // Replace multiple spaces with single space
                .replaceAll("[^a-zA-Z0-9\\s]", ""); // Remove special characters
    }

    // Show confirmation dialog before starting import
    private void showImportConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Import Questions")
                .setMessage("This will import all questions from the JSON file. Existing questions will be skipped. Continue?")
                .setPositiveButton("Import", (dialog, which) -> startImportProcess())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Start the import process
    private void startImportProcess() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Preparing to import questions...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Read JSON file in background
        new Thread(this::readAndProcessJson).start();
    }

    // Read and process the JSON file
    private void readAndProcessJson() {
        try {
            Log.d(TAG, "Starting to read questions_clean.json from assets");
            String json = readAssetText("questions_clean.json");
            if (json == null || json.isEmpty()) {
                String error = "Error: questions_clean.json is empty or not found in assets";
                Log.e(TAG, error);
                showError(error);
                return;
            }
            Log.d(TAG, "Successfully read questions_clean.json, length: " + json.length());

            try {
                JSONArray questionsArray = new JSONArray(json);
                Log.d(TAG, "Successfully parsed JSON, found " + questionsArray.length() + " questions");
                checkExistingQuestions(questionsArray);
            } catch (JSONException e) {
                String error = "Error parsing JSON: " + e.getMessage();
                Log.e(TAG, error, e);
                showError(error);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error reading JSON", e);
            showError("Error reading questions: " + e.getMessage());
        }
    }

    // Check for existing questions to avoid duplicates
    private void checkExistingQuestions(JSONArray questionsArray) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Set<String> existingQuestionTexts = new HashSet<>();
        int totalQuestions = questionsArray.length();

        runOnUiThread(() -> {
            progressDialog.setMessage("Checking existing questions...");
            progressDialog.setMax(totalQuestions);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        });

        db.collection("questions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Store all existing question texts for duplicate checking
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String existingText = document.getString("text");
                        if (existingText != null) {
                            existingQuestionTexts.add(normalizeQuestionText(existingText));
                        }
                    }
                    processQuestionsBatch(questionsArray, existingQuestionTexts);
                })
                .addOnFailureListener(e -> {
                    showError("Error checking existing questions: " + e.getMessage());
                });
    }

    // Process questions in batches and import them
    private void processQuestionsBatch(JSONArray questionsArray, Set<String> existingQuestionTexts) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        int importedCount = 0;
        int duplicateCount = 0;
        int errorCount = 0;
        final int BATCH_LIMIT = 100; // Reduced batch size to 100 to avoid timeouts

        try {
            for (int i = 0; i < questionsArray.length(); i++) {
                try {
                    JSONObject q = questionsArray.getJSONObject(i);

                    // Validate required fields
                    if (!isValidQuestion(q)) {
                        errorCount++;
                        continue;
                    }

                    String questionText = q.getString("text").trim();
                    String normalizedText = normalizeQuestionText(questionText);

                    // Skip if question already exists
                    if (existingQuestionTexts.contains(normalizedText)) {
                        duplicateCount++;
                        continue;
                    }

                    // Add question to batch
                    batch.set(db.collection("questions").document(), createQuestionMap(q, questionText));
                    importedCount++;

                    // Commit batch if we've reached the limit
                    if (importedCount > 0 && importedCount % BATCH_LIMIT == 0) {
                        commitBatch(batch, db);
                        batch = db.batch(); // Start a new batch
                    }

                    // Update progress
                    updateProgress(i + 1, questionsArray.length());

                } catch (JSONException e) {
                    errorCount++;
                    Log.e(TAG, "Error parsing question " + i, e);
                }
            }

            // Commit any remaining questions in the last batch
            if (importedCount > 0) {
                commitBatch(batch, db);
                Log.d(TAG, "Final batch committed with " + (importedCount % BATCH_LIMIT) + " questions");
            }

            // Create final copies of the variables for the lambda
            final int finalImportedCount = importedCount;
            final int finalDuplicateCount = duplicateCount;
            final int finalErrorCount = errorCount;
            
            // Show import results on UI thread
            runOnUiThread(() -> showImportResults(finalImportedCount, finalDuplicateCount, finalErrorCount));

        } catch (Exception e) {
            Log.e(TAG, "Error in batch processing", e);
            showError("Error processing questions: " + e.getMessage());
        }
    }

    // Validate question JSON object
    private boolean isValidQuestion(JSONObject q) throws JSONException {
        return q.has("subject") && q.has("difficulty") && q.has("text") &&
                q.has("options") && q.has("answerIndex") &&
                q.getJSONArray("options").length() == 4;
    }

    // Create a question map from JSON object
    private Map<String, Object> createQuestionMap(JSONObject q, String questionText) throws JSONException {
        Map<String, Object> question = new HashMap<>();
        question.put("subject", q.getString("subject"));
        question.put("difficulty", q.getString("difficulty"));
        question.put("text", questionText);

        // Process options
        JSONArray optionsArray = q.getJSONArray("options");
        List<String> options = new ArrayList<>();
        for (int j = 0; j < optionsArray.length(); j++) {
            options.add(optionsArray.getString(j));
        }
        question.put("options", options);

        // Add answer index and active status
        question.put("answerIndex", q.getInt("answerIndex"));
        question.put("active", q.optBoolean("active", true));

        return question;
    }

    // Commit a batch of writes to Firestore
    private void commitBatch(WriteBatch batch, FirebaseFirestore db) {
        batch.commit()
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Batch commit successful"))
            .addOnFailureListener(e -> {
                Log.e(TAG, "Batch commit failed: " + e.getMessage(), e);
                showError("Failed to save questions: " + e.getMessage());
            });
    }

    // Update progress dialog
    private void updateProgress(int progress, int max) {
        runOnUiThread(() -> {
            progressDialog.setProgress(progress);
            progressDialog.setMax(max);
        });
    }

    // Show error message
    private void showError(String message) {
        runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    // Show import results
    private void showImportResults(int imported, int duplicates, int errors) {
        runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            String message = String.format(
                    "Import complete!\n" +
                            "• %d questions imported successfully\n" +
                            "• %d duplicates skipped\n" +
                            "• %d errors encountered",
                    imported, duplicates, errors
            );

            new AlertDialog.Builder(this)
                    .setTitle("Import Results")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    // Read text from assets file
    private String readAssetText(String filename) {
        Log.d(TAG, "Attempting to read asset file: " + filename);
        StringBuilder sb = new StringBuilder();
        
        // First, check if the file exists
        try {
            String[] files = getAssets().list("");
            if (files != null) {
                boolean fileExists = false;
                for (String file : files) {
                    if (file.equals(filename)) {
                        fileExists = true;
                        break;
                    }
                }
                if (!fileExists) {
                    Log.e(TAG, "File not found in assets: " + filename);
                    return null;
                }
            }
            
            // If we get here, the file exists, so try to read it
            try (InputStream is = getAssets().open(filename);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String str;
                int lineCount = 0;
                while ((str = br.readLine()) != null) {
                    sb.append(str).append("\n");
                    lineCount++;
                }
                Log.d(TAG, "Successfully read " + lineCount + " lines from " + filename);
                return sb.toString().trim();
            }
        } catch (IOException e) {
            String error = "Error reading asset file " + filename + ": " + e.getMessage();
            Log.e(TAG, error, e);
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        // Dismiss progress dialog to prevent memory leaks
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDestroy();
    }
}
