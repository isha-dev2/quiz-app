package com.example.project22;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project22.models.Question;

import java.util.List;

public class QuestionUpdateAdapter extends RecyclerView.Adapter<QuestionUpdateAdapter.QuestionViewHolder> {

    private List<Question> questions;
    private Context context;

    public QuestionUpdateAdapter(List<Question> questions, Context context) {
        this.questions = questions;
        this.context = context;
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question_update, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        Question question = questions.get(position);
        holder.tvQuestionText.setText(question.text);
        holder.btnUpdate.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddQuestionActivity.class);
            intent.putExtra("docId", question.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestionText;
        Button btnUpdate;

        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            btnUpdate = itemView.findViewById(R.id.btnUpdate);
        }
    }
}
