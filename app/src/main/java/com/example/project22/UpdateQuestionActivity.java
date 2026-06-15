package com.example.project22;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project22.models.Question;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UpdateQuestionActivity extends AppCompatActivity {

    private Spinner spSubject, spDifficulty;
    private Button btnLoadQuestions;
    private RecyclerView rvQuestions;
    private QuestionUpdateAdapter adapter;
    private List<Question> questions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_question);

        spSubject = findViewById(R.id.spSubject);
        spDifficulty = findViewById(R.id.spDifficulty);
        btnLoadQuestions = findViewById(R.id.btnLoadQuestions);
        rvQuestions = findViewById(R.id.rvQuestions);

        // Setup spinners
        String[] subjects = new String[]{"DSA", "DBMS", "OS", "Networks"};
        String[] difficulties = new String[]{"Easy", "Medium", "Hard"};
        spSubject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjects));
        spDifficulty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, difficulties));

        // Setup RecyclerView
        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuestionUpdateAdapter(questions, this);
        rvQuestions.setAdapter(adapter);

        // Load questions on button click
        btnLoadQuestions.setOnClickListener(v -> loadQuestions());
    }

    private void loadQuestions() {
        String subject = (String) spSubject.getSelectedItem();
        String difficulty = (String) spDifficulty.getSelectedItem();

        if (subject == null || difficulty == null) {
            Toast.makeText(this, "Select subject and difficulty", Toast.LENGTH_SHORT).show();
            return;
        }

        questions.clear();
        adapter.notifyDataSetChanged();

        // Load questions from JSON in assets
        new Thread(() -> {
            Set<QuestionUtils.QuestionData> validQuestions = QuestionUtils.loadQuestionsFromAssets(this);
            
            runOnUiThread(() -> {
                // Now load from Firestore and filter
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("questions")
                        .whereEqualTo("subject", subject)
                        .whereEqualTo("difficulty", difficulty)
                        .limit(20)  // Limit to 20 questions
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            List<Question> allQuestions = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : querySnapshot) {
                                Question question = doc.toObject(Question.class);
                                question.id = doc.getId();
                                allQuestions.add(question);
                            }
                            
                            // Filter questions to only include those in the JSON file
                            questions.clear();
                            questions.addAll(QuestionUtils.filterQuestions(allQuestions, validQuestions));
                            adapter.notifyDataSetChanged();
                            
                            if (questions.isEmpty()) {
                                Toast.makeText(this, "No matching questions found in the quiz", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> 
                            Toast.makeText(this, "Failed to load questions: " + e.getMessage(), Toast.LENGTH_LONG).show()
                        );
            });
        }).start();
    }
}
