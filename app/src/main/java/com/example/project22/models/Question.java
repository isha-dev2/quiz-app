package com.example.project22.models;

import java.util.List;

public class Question {
    public String id;  // Document ID
    public String subject;
    public String difficulty;
    public String text;
    public List<String> options;
    public Integer answerIndex;
    public Boolean active;

    public Question() {}
}
