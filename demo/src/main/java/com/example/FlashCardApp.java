package com.example;
import java.util.*;
import java.io.*;
//java -cp out com.example.FlashCardApp flashcards.txt --order random



// CLI Flashcard Application
public class FlashCardApp {
    private static final String LOG_FILE = "flashcard_log.txt";

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("--help")) {
            printHelp();
            return;
        }

        String fileName = args[0];
        String order = "random";
        int repetitions = 1;
        boolean invertCards = false;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--order":
                    if (i + 1 < args.length) {
                        order = args[++i];
                    }
                    break;
                case "--repetitions":
                    if (i + 1 < args.length) {
                        try {
                            repetitions = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid repetitions number. Using default of 1.");
                            repetitions = 1;
                        }
                    }
                    break;
                case "--invertCards":
                    invertCards = true;
                    break;
            }
        }

        List<FlashCard> cards = loadCards(fileName);
        if (cards.isEmpty()) {
            System.out.println("No cards loaded. Check your file.");
            return;
        }

        Map<String, PerformanceData> logData = loadLog();
        mergeLogIntoCards(cards, logData);

        for (FlashCard card : cards) {
            card.resetSessionProgress();
        }

        List<FlashCard> pool = new ArrayList<>();
        for (FlashCard card : cards) {
            for (int i = 0; i < repetitions; i++) {
                pool.add(card);
            }
        }

        CardOrganizer sorter = getSorter(order);
        pool = sorter.organize(pool);

        Scanner scanner = new Scanner(System.in);
        long sessionStart = System.currentTimeMillis();

        for (FlashCard current : pool) {
            System.out.println("Question: " + (invertCards ? current.getAnswer() : current.getQuestion()));
            String userAnswer = scanner.nextLine();
            if (userAnswer.equalsIgnoreCase("exit")) {
                System.out.println("Session exited by user.");
                break;
            }
            current.incrementAttempts();
            String correctAnswer = invertCards ? current.getQuestion() : current.getAnswer();
            if (!userAnswer.equalsIgnoreCase(correctAnswer)) {
                current.incrementMistakes();
                System.out.println("Incorrect!");
            } else {
                current.incrementCorrectAnswers();
                current.incrementSessionCorrect();
                System.out.println("Correct! (" + current.getSessionCorrect() + "/" + repetitions + ")");
            }
        }

        long sessionEnd = System.currentTimeMillis();
        double avgTime = (sessionEnd - sessionStart) / 1000.0 / pool.size();
        scanner.close();

        evaluateAchievements(cards, avgTime);
        printFlashCardLog(cards);
        writeLog(cards);
    }

    private static void printHelp() {
        System.out.println("Usage: flashcard <cards-file> [options]");
        System.out.println("Options:");
        System.out.println("  --help Тусламжийн мэдээлэл харуулах");
        System.out.println("  --order <order> Зохион байгуулалтын төрөл, default нь \"random\"");
        System.out.println("      [сонголт: \"random\", \"worst-first\", \"recent-mistakes-first\"]");
        System.out.println("  --repetitions <num> Нэг картыг хэдэн удаа асууна (тахирлах)");
        System.out.println("  --invertCards Тохиргоо идэвхэжсэн бол картын асуулт, хариултыг сольж харуулна.");
    }

    private static List<FlashCard> loadCards(String filename) {
        List<FlashCard> cards = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length == 2) {
                    cards.add(new FlashCard(parts[0].trim(), parts[1].trim()));
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading flashcards file: " + e.getMessage());
        }
        return cards;
    }

    private static CardOrganizer getSorter(String order) {
        switch (order) {
            case "worst-first": return new WorstFirstSorter();
            case "recent-mistakes-first": return new RecentMistakesFirstSorter();
            default: return new RandomSorter();
        }
    }

    private static void evaluateAchievements(List<FlashCard> cards, double avgTime) {
        boolean allCorrect = true;
        boolean repeatAchieved = false;
        boolean confidentAchieved = false;

        for (FlashCard card : cards) {
            if (card.getMistakes() > 0) allCorrect = false;
            if (card.getAttempts() > 5) repeatAchieved = true;
            if (card.getCorrectAnswers() >= 3) confidentAchieved = true;
        }
        System.out.println("\nAchievements:");
        if (allCorrect) System.out.println("✔ CORRECT: All cards answered correctly in the last round!");
        if (repeatAchieved) System.out.println("✔ REPEAT: A card was attempted more than 5 times!");
        if (confidentAchieved) System.out.println("✔ CONFIDENT: A card was answered correctly at least 3 times!");
        if (avgTime < 5.0) System.out.println("✔ SPEEDSTER: Average response time under 5 seconds!");
    }

    private static void printFlashCardLog(List<FlashCard> cards) {
        System.out.println("\nFlashcard Log:");
        for (FlashCard card : cards) {
            System.out.println("-------------------------------------------------");
            System.out.println("Question: " + card.getQuestion());
            System.out.println("Answer: " + card.getAnswer());
            System.out.println("Attempts: " + card.getAttempts());
            System.out.println("Correct Answers: " + card.getCorrectAnswers());
            System.out.println("Mistakes: " + card.getMistakes());
        }
        System.out.println("-------------------------------------------------");
    }

    static class PerformanceData {
        int attempts;
        int mistakes;
        int correctAnswers;
        public PerformanceData(int attempts, int mistakes, int correctAnswers) {
            this.attempts = attempts;
            this.mistakes = mistakes;
            this.correctAnswers = correctAnswers;
        }
    }

    private static Map<String, PerformanceData> loadLog() {
        Map<String, PerformanceData> map = new HashMap<>();
        File logFile = new File(LOG_FILE);
        if (!logFile.exists()) return map;
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length == 4) {
                    String question = parts[0].trim();
                    int attempts = Integer.parseInt(parts[1].trim());
                    int mistakes = Integer.parseInt(parts[2].trim());
                    int correctAnswers = Integer.parseInt(parts[3].trim());
                    map.put(question, new PerformanceData(attempts, mistakes, correctAnswers));
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading log file: " + e.getMessage());
        }
        return map;
    }

    private static void mergeLogIntoCards(List<FlashCard> cards, Map<String, PerformanceData> logData) {
        for (FlashCard card : cards) {
            if (logData.containsKey(card.getQuestion())) {
                PerformanceData pd = logData.get(card.getQuestion());
                card.updateFromLog(pd.attempts, pd.mistakes, pd.correctAnswers);
            }
        }
    }

    private static void writeLog(List<FlashCard> cards) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE))) {
            for (FlashCard card : cards) {
                writer.println(card.getQuestion() + ";" + card.getAttempts() + ";" +
                        card.getMistakes() + ";" + card.getCorrectAnswers());
            }
        } catch (IOException e) {
            System.out.println("Error writing log file: " + e.getMessage());
        }
    }
}
