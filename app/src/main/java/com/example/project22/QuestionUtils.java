package com.example.project22;

import android.content.Context;
import android.content.res.AssetManager;

import com.example.project22.models.Question;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuestionUtils {
    
    public static class QuestionData {
        public String subject;
        public String difficulty;
        public String text;
        
        public QuestionData(String subject, String difficulty, String text) {
            this.subject = subject;
            this.difficulty = difficulty;
            this.text = text.trim().toLowerCase();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            QuestionData that = (QuestionData) obj;
            return subject.equalsIgnoreCase(that.subject) &&
                   difficulty.equalsIgnoreCase(that.difficulty) &&
                   text.equals(that.text);
        }
        
        @Override
        public int hashCode() {
            int result = subject.toLowerCase().hashCode();
            result = 31 * result + difficulty.toLowerCase().hashCode();
            result = 31 * result + text.hashCode();
            return result;
        }
    }
    
    public static Set<QuestionData> loadQuestionsFromAssets(Context context) {
        Set<QuestionData> questionSet = new HashSet<>();
        try {
            String json = readAssetText(context, "questions_clean.json");
            if (json == null || json.isEmpty()) {
                return questionSet;
            }
            
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.has("subject") && o.has("difficulty") && o.has("text")) {
                    String subject = o.getString("subject");
                    String difficulty = o.getString("difficulty");
                    String text = o.getString("text");
                    questionSet.add(new QuestionData(subject, difficulty, text));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return questionSet;
    }
    
    private static String readAssetText(Context context, String filename) throws IOException {
        AssetManager am = context.getAssets();
        try (InputStream is = am.open(filename);
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
    
    public static List<Question> filterQuestions(List<Question> questions, Set<QuestionData> validQuestions) {
        List<Question> filtered = new ArrayList<>();
        for (Question q : questions) {
            QuestionData qd = new QuestionData(q.subject, q.difficulty, q.text);
            if (validQuestions.contains(qd)) {
                filtered.add(q);
            }
        }
        return filtered;
    }
}
