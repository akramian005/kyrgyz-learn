package com.example.kyrgyzlearn.entity.lesson;

public enum QuestionType {
    SINGLE_CHOICE,   // один правильный вариант
    MULTIPLE_CHOICE, // несколько правильных вариантов, порядок не важен
    ORDER,           // ответ — последовательность вариантов в правильном порядке
    MATCHING;        // сопоставление: каждый вариант — пара text ↔ matchText, ученик соединяет их

    public static final int MAX_MATCHING_PAIRS = 10;
}
