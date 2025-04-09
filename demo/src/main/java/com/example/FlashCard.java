package com.example;


// FlashCard model class
class FlashCard {
    private final String question;
    private final String answer;
    private int mistakes;
    private int correctAnswers;
    private int attempts;

    public FlashCard(String question, String answer) {
        this.question = question;
        this.answer = answer;
        this.mistakes = 0;
        this.correctAnswers = 0;
        this.attempts = 0;
    }

    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public int getMistakes() { return mistakes; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getAttempts() { return attempts; }

    public void incrementMistakes() { this.mistakes++; }
    public void incrementCorrectAnswers() { this.correctAnswers++; }
    public void incrementAttempts() { this.attempts++; }

    public void updateFromLog(int attempts, int mistakes, int correctAnswers) {
        this.attempts = attempts;
        this.mistakes = mistakes;
        this.correctAnswers = correctAnswers;
    }

    public void resetSessionProgress() {
        this.sessionCorrect = 0;
    }

    // Track correct answers only for current session repetition display
    private int sessionCorrect = 0;
    public void incrementSessionCorrect() { this.sessionCorrect++; }
    public int getSessionCorrect() { return sessionCorrect; }
}
