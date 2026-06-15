package com.example.project22;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AddQuestionActivity extends AppCompatActivity {

    private Spinner spSubject, spDifficulty;
    private EditText etQuestion, etOpt1, etOpt2, etOpt3, etOpt4;
    private RadioGroup rgAnswer;
    private Switch swActive;
    private Button btnSave;
    private String docId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_question);

        spSubject = findViewById(R.id.spSubject);
        spDifficulty = findViewById(R.id.spDifficulty);
        etQuestion = findViewById(R.id.etQuestion);
        etOpt1 = findViewById(R.id.etOpt1);
        etOpt2 = findViewById(R.id.etOpt2);
        etOpt3 = findViewById(R.id.etOpt3);
        etOpt4 = findViewById(R.id.etOpt4);
        rgAnswer = findViewById(R.id.rgAnswer);
        swActive = findViewById(R.id.swActive);
        btnSave = findViewById(R.id.btnSave);

        String[] subjects = new String[]{"DSA", "DBMS", "OS", "Networks"};
        String[] difficulties = new String[]{"Easy", "Medium", "Hard"};
        spSubject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjects));
        spDifficulty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, difficulties));

        // Check if editing existing doc
        if (getIntent() != null && getIntent().hasExtra("docId")) {
            docId = getIntent().getStringExtra("docId");
            btnSave.setText("Update Question");
            loadExisting(docId);
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveQuestion();
            }
        });
    }

    private void loadExisting(String id) {
        btnSave.setEnabled(false);
        FirebaseFirestore.getInstance().collection("questions").document(id).get()
                .addOnSuccessListener(this::fillFromDoc)
                .addOnFailureListener(e -> Toast.makeText(this, "Load failed: " + (e != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show())
                .addOnCompleteListener(t -> btnSave.setEnabled(true));
    }

    private void fillFromDoc(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            Toast.makeText(this, "Document not found", Toast.LENGTH_LONG).show();
            return;
        }
        String subject = doc.getString("subject");
        String difficulty = doc.getString("difficulty");
        String text = doc.getString("text");
        java.util.List<String> options = (java.util.List<String>) doc.get("options");
        Number ans = (Number) doc.get("answerIndex");
        Boolean active = doc.getBoolean("active");

        if (subject != null) spSubject.setSelection(((ArrayAdapter<String>) spSubject.getAdapter()).getPosition(subject));
        if (difficulty != null) spDifficulty.setSelection(((ArrayAdapter<String>) spDifficulty.getAdapter()).getPosition(difficulty));
        etQuestion.setText(text != null ? text : "");
        if (options != null && options.size() == 4) {
            etOpt1.setText(options.get(0));
            etOpt2.setText(options.get(1));
            etOpt3.setText(options.get(2));
            etOpt4.setText(options.get(3));
        }
        if (ans != null) {
            int idx = ans.intValue();
            if (idx == 0) rgAnswer.check(R.id.rbA1);
            else if (idx == 1) rgAnswer.check(R.id.rbA2);
            else if (idx == 2) rgAnswer.check(R.id.rbA3);
            else if (idx == 3) rgAnswer.check(R.id.rbA4);
        }
        swActive.setChecked(active == null || active);
    }

    private void saveQuestion() {
        String subject = (String) spSubject.getSelectedItem();
        String difficulty = (String) spDifficulty.getSelectedItem();
        String text = etQuestion.getText().toString().trim();
        String o1 = etOpt1.getText().toString().trim();
        String o2 = etOpt2.getText().toString().trim();
        String o3 = etOpt3.getText().toString().trim();
        String o4 = etOpt4.getText().toString().trim();

        int checkedId = rgAnswer.getCheckedRadioButtonId();
        int answerIdx = -1;
        if (checkedId == R.id.rbA1) answerIdx = 0;
        else if (checkedId == R.id.rbA2) answerIdx = 1;
        else if (checkedId == R.id.rbA3) answerIdx = 2;
        else if (checkedId == R.id.rbA4) answerIdx = 3;

        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(o1) || TextUtils.isEmpty(o2) || TextUtils.isEmpty(o3) || TextUtils.isEmpty(o4) || answerIdx == -1) {
            Toast.makeText(this, "Fill all fields and select correct answer", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("subject", subject);
        doc.put("difficulty", difficulty);
        doc.put("text", text);
        doc.put("options", Arrays.asList(o1, o2, o3, o4));
        doc.put("answerIndex", answerIdx);
        doc.put("active", swActive.isChecked());

        btnSave.setEnabled(false);
        if (docId == null) {
            FirebaseFirestore.getInstance().collection("questions").add(doc)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Question added", Toast.LENGTH_SHORT).show();
                        clearForm();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + (e != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show())
                    .addOnCompleteListener(task -> btnSave.setEnabled(true));
        } else {
            FirebaseFirestore.getInstance().collection("questions").document(docId).set(doc)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Question updated", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + (e != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show())
                    .addOnCompleteListener(task -> btnSave.setEnabled(true));
        }
    }

    private void clearForm() {
        etQuestion.setText("");
        etOpt1.setText("");
        etOpt2.setText("");
        etOpt3.setText("");
        etOpt4.setText("");
        rgAnswer.clearCheck();
        swActive.setChecked(true);
    }
}
