package com.example.project22;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvToRegister;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvToRegister = findViewById(R.id.tvToRegister);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "Enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isOnline()) {
                    Toast.makeText(LoginActivity.this, "No internet connection. Check Wi‑Fi or mobile data.", Toast.LENGTH_LONG).show();
                    return;
                }
                btnLogin.setEnabled(false);
                auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            btnLogin.setEnabled(true);
                            if (task.isSuccessful()) {
                                FirebaseUser user = auth.getCurrentUser();
                                if (user == null) {
                                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                routeByRole(user.getUid());
                            } else {
                                Exception ex = task.getException();
                                Log.e("LoginActivity", "Login failed", ex);
                                Toast.makeText(LoginActivity.this, String.valueOf(ex != null ? ex.getMessage() : "Login failed"), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        tvToRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void routeByRole(String uid) {
        String email = etEmail.getText().toString().trim();
        Map<String, Object> updates = new HashMap<>();
        updates.put("email", email);
        
        // Update the user's email in the roles collection
        db.collection("roles").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // After updating email, get the role
                    db.collection("roles").document(uid).get()
                            .addOnSuccessListener(this::handleRoleDoc)
                            .addOnFailureListener(e -> {
                                String msg = e != null && e.getMessage() != null ? e.getMessage() : "Failed to get role";
                                Toast.makeText(LoginActivity.this, "Failed to get role: " + msg, Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    // If update fails (maybe document doesn't exist), try to set it
                    updates.put("role", "student"); // Default role
                    updates.put("score", 0); // Initialize score
                    db.collection("roles").document(uid)
                            .set(updates, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> {
                                // After creating/updating, get the role
                                db.collection("roles").document(uid).get()
                                        .addOnSuccessListener(this::handleRoleDoc)
                                        .addOnFailureListener(e2 -> {
                                            String msg = e2 != null && e2.getMessage() != null ? e2.getMessage() : "Failed to get role";
                                            Toast.makeText(LoginActivity.this, "Failed to get role: " + msg, Toast.LENGTH_LONG).show();
                                        });
                            })
                            .addOnFailureListener(e2 -> {
                                String msg = e2 != null && e2.getMessage() != null ? e2.getMessage() : "Failed to update user data";
                                Toast.makeText(LoginActivity.this, "Failed to update user data: " + msg, Toast.LENGTH_LONG).show();
                            });
                });
    }

    private void handleRoleDoc(DocumentSnapshot doc) {
        String role = doc != null ? doc.getString("role") : null;
        if ("admin".equalsIgnoreCase(role)) {
            startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
        } else {
            startActivity(new Intent(LoginActivity.this, StudentSelectActivity.class));
        }
        finish();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (nc == null) return false;
        return nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
}
