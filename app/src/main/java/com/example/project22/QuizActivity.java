package com.example.project22;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.project22.models.Question;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class QuizActivity extends AppCompatActivity {

    private TextView tvSubject, tvDifficulty, tvTimer, tvQuestion, tvProgress;
    private RadioGroup rgOptions;
    private RadioButton rb1, rb2, rb3, rb4;
    private Button btnPrev, btnNext, btnSubmit;
    private ProgressBar progressBar;

    private String subject;
    private String difficulty;
    private List<Question> questions = new ArrayList<>();
    private Integer[] selectedAnswers; // -1 for not answered
    private int index = 0;

    private CountDownTimer timer;
    private long totalDurationMs = 30L * 60L * 1000L;
    private long remainingMs = totalDurationMs;

    private boolean autoSubmitted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        tvSubject = findViewById(R.id.tvSubject);
        tvDifficulty = findViewById(R.id.tvDifficulty);
        tvTimer = findViewById(R.id.tvTimer);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvProgress = findViewById(R.id.tvProgress);
        rgOptions = findViewById(R.id.rgOptions);
        rb1 = findViewById(R.id.rb1);
        rb2 = findViewById(R.id.rb2);
        rb3 = findViewById(R.id.rb3);
        rb4 = findViewById(R.id.rb4);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        subject = getIntent().getStringExtra("subject");
        difficulty = getIntent().getStringExtra("difficulty");
        tvSubject.setText(subject);
        tvDifficulty.setText(difficulty);

        fetchQuestions();

        btnPrev.setOnClickListener(v -> {
            saveSelection();
            if (index > 0) {
                index--;
                renderQuestion();
            }
        });

        btnNext.setOnClickListener(v -> {
            saveSelection();
            if (index < questions.size() - 1) {
                index++;
                renderQuestion();
            }
        });

        btnSubmit.setOnClickListener(v -> {
            saveSelection();
            submitQuiz(false);
        });

        ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_STOP) {
                    if (!autoSubmitted) {
                        saveSelection();
                        submitQuiz(true);
                    }
                }
            }
        });
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!autoSubmitted) {
            saveSelection();
            submitQuiz(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null) timer.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!autoSubmitted && questions.size() == 20) startTimer(remainingMs);
    }

    private void fetchQuestions() {
        setLoading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query q = db.collection("questions")
                .whereEqualTo("subject", subject)
                .whereEqualTo("difficulty", difficulty)
                .whereEqualTo("active", true)
                .limit(20);
        q.get().addOnCompleteListener(task -> {
            setLoading(false);
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot d : task.getResult()) {
                    Question qu = d.toObject(Question.class);
                    qu.id = d.getId();
                    questions.add(qu);
                }
                if (questions.size() > 0) {
                    // Ensure we have exactly 20 questions
                    if (questions.size() > 20) {
                        // If more than 20, take first 20 after shuffling
                        Collections.shuffle(questions);
                        questions = questions.subList(0, 20);
                    } else if (questions.size() < 20) {
                        // If less than 20, show what we have
                        // You might want to handle this case differently
                        android.widget.Toast.makeText(this, "Only " + questions.size() + " questions available for " + subject + " / " + difficulty, android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        // Exactly 20, just shuffle them
                        Collections.shuffle(questions);
                    }
                    
                    selectedAnswers = new Integer[questions.size()];
                    for (int i = 0; i < selectedAnswers.length; i++) selectedAnswers[i] = -1;
                    renderQuestion();
                    startTimer(totalDurationMs);
                } else {
                    android.widget.Toast.makeText(this, "No questions found for " + subject + " / " + difficulty + ". Please add questions in Firestore.", android.widget.Toast.LENGTH_LONG).show();
                    finish();
                }
            } else {
                String msg = (task.getException() != null && task.getException().getMessage() != null) ? task.getException().getMessage() : "Unknown error";
                android.widget.Toast.makeText(this, "Failed to load questions: " + msg, android.widget.Toast.LENGTH_LONG).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            setLoading(false);
            String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "Unknown error";
            android.widget.Toast.makeText(this, "Error loading questions: " + msg, android.widget.Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void renderQuestion() {
        tvProgress.setText(String.format(Locale.getDefault(), "%d/%d", index + 1, questions.size()));
        Question q = questions.get(index);
        tvQuestion.setText(q.text);
        List<String> opts = q.options;
        if (opts == null || opts.size() < 4) {
            opts = new java.util.ArrayList<>();
            opts.add("Option 1");
            opts.add("Option 2");
            opts.add("Option 3");
            opts.add("Option 4");
        }
        rb1.setText(opts.get(0));
        rb2.setText(opts.get(1));
        rb3.setText(opts.get(2));
        rb4.setText(opts.get(3));
        rgOptions.clearCheck();
        Integer sel = selectedAnswers[index];
        if (sel != null && sel >= 0 && sel <= 3) {
            if (sel == 0) rb1.setChecked(true);
            if (sel == 1) rb2.setChecked(true);
            if (sel == 2) rb3.setChecked(true);
            if (sel == 3) rb4.setChecked(true);
        }
        btnPrev.setEnabled(index > 0);
        btnNext.setEnabled(index < questions.size() - 1);
    }

    private void saveSelection() {
        int checked = rgOptions.getCheckedRadioButtonId();
        int sel = -1;
        if (checked == R.id.rb1) sel = 0;
        else if (checked == R.id.rb2) sel = 1;
        else if (checked == R.id.rb3) sel = 2;
        else if (checked == R.id.rb4) sel = 3;
        selectedAnswers[index] = sel;
    }

    private void startTimer(long durationMs) {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMs = millisUntilFinished;
                long min = millisUntilFinished / 60000;
                long sec = (millisUntilFinished % 60000) / 1000;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                remainingMs = 0;
                saveSelection();
                submitQuiz(true);
            }
        }.start();
    }

    private void submitQuiz(boolean auto) {
        autoSubmitted = true;
        if (timer != null) timer.cancel();
        int score = 0;
        int attempted = 0;
        for (int i = 0; i < questions.size(); i++) {
            Integer sel = selectedAnswers[i];
            if (sel != null && sel >= 0) {
                attempted++;
                if (sel.equals(questions.get(i).answerIndex)) score++;
            }
        }
        Intent i = new Intent(this, ResultsActivity.class);
        i.putExtra("score", score);
        i.putExtra("total", questions.size());
        i.putExtra("attempted", attempted);
        i.putExtra("auto", auto);
        startActivity(i);
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPrev.setEnabled(!loading);
        btnNext.setEnabled(!loading);
        btnSubmit.setEnabled(!loading);
    }
}
