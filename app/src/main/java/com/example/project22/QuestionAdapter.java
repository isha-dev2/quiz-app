package com.example.project22;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project22.models.Question;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder> {

    private List<Question> questions;
    private Context context;

    public QuestionAdapter(List<Question> questions, Context context) {
        this.questions = questions;
        this.context = context;
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        Question question = questions.get(position);
        holder.tvQuestionText.setText(question.text);
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddQuestionActivity.class);
            intent.putExtra("docId", question.id);
            context.startActivity(intent);
        });
        holder.btnDelete.setOnClickListener(v -> confirmDelete(question));
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    private void confirmDelete(Question question) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Question")
                .setMessage("Are you sure you want to delete this question? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteQuestion(question))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteQuestion(Question question) {
        FirebaseFirestore.getInstance().collection("questions").document(question.id).delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(context, "Question deleted", Toast.LENGTH_SHORT).show();
                    questions.remove(question);
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestionText;
        Button btnEdit, btnDelete;

        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
