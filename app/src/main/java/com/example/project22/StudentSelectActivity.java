package com.example.project22;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

public class StudentSelectActivity extends AppCompatActivity {

    private Spinner spSubject, spDifficulty;
    private Button btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_select);

        spSubject = findViewById(R.id.spSubject);
        spDifficulty = findViewById(R.id.spDifficulty);
        btnStart = findViewById(R.id.btnStartQuiz);

        String[] subjects = new String[]{"DSA", "DBMS", "OS", "Networks"};
        String[] difficulties = new String[]{"Easy", "Medium", "Hard"};

        spSubject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjects));
        spDifficulty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, difficulties));

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String subject = (String) spSubject.getSelectedItem();
                String difficulty = (String) spDifficulty.getSelectedItem();
                Intent i = new Intent(StudentSelectActivity.this, QuizActivity.class);
                i.putExtra("subject", subject);
                i.putExtra("difficulty", difficulty);
                startActivity(i);
            }
        });
    }
}
