package com.example.project22;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private RadioGroup rgRole;
    private Button btnRegister;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        rgRole = findViewById(R.id.rgRole);
        btnRegister = findViewById(R.id.btnRegister);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(RegisterActivity.this, "Enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }
                String role = "student";
                btnRegister.setEnabled(false);
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            btnRegister.setEnabled(true);
                            if (task.isSuccessful()) {
                                FirebaseUser user = auth.getCurrentUser();
                                if (user == null) {
                                    Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Map<String, Object> data = new HashMap<>();
                                data.put("role", role);
                                data.put("score", 0);  // Initialize score to 0 for new users
                                data.put("email", email);  // Store email for display
                                db.collection("roles").document(user.getUid()).set(data)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(RegisterActivity.this, "Registered as " + role, Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Failed to save role", Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(RegisterActivity.this, String.valueOf(task.getException() != null ? task.getException().getMessage() : "Registration failed"), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }
}
