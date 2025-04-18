package com.example;


class FlashCard {
    private final String question;
    private final String answer;
    private int mistakes;
    private int correctAnswers;
    private int attempts;
    // Session-only field: updated whenever a mistake happens.
    private int sessionCorrect = 0;
    private int sessionMistakes = 0;
    private int lastSessionWithMistake = 0;
    private int mistakeOrderInLastSession = 0;

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
    public int getSessionCorrect() { return sessionCorrect; }
    public int getSessionMistakes() {return sessionMistakes;}
    public int getLastSessionWithMistake() {return lastSessionWithMistake;}
    public int getMistakeOrderInLastSession() { return mistakeOrderInLastSession; }


    public void recordMistake(int sessionNumber, int mistakeOrder) {
        this.mistakes++;
        this.lastSessionWithMistake = sessionNumber;
        this.mistakeOrderInLastSession = mistakeOrder;
    }

    public void updateLastMistakeSession(int currentSession) {
        if (currentSession > this.lastSessionWithMistake) {
            this.lastSessionWithMistake = currentSession;
        }
    }

    public void incrementMistakes(int currentSession) {
        this.mistakes++;
        updateLastMistakeSession(currentSession);
    }

    public void incrementCorrectAnswers(int currentSession) {
        this.correctAnswers++;
        this.sessionCorrect++;
        
        // Always reset if answered correctly in a new session
        if (currentSession != this.lastSessionWithMistake) {
            this.lastSessionWithMistake = 0;
            this.mistakeOrderInLastSession = 0;
        }
    }
    public void incrementAttempts() { this.attempts++; }

    // Reset session-specific correct count.
    public void resetSessionProgress() {
        this.sessionCorrect = 0;
        this.sessionMistakes = 0;
    }

    // Merge performance data from log into this card.
    public void updateFromLog(int attempts, int mistakes, int correctAnswers, int lastSessionWithMistake) {
        this.attempts = attempts;
        this.mistakes = mistakes;
        this.correctAnswers = correctAnswers;
        this.lastSessionWithMistake = lastSessionWithMistake; 
    }
    
}
