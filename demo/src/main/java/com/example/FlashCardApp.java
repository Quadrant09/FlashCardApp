package com.example;
import java.util.*;
import java.io.*;
//java -cp out com.example.FlashCardApp flashcards.txt --order random
//javac -d out src/main/java/com/example/*.java


// CLI Flashcard Application
public class FlashCardApp {
    private static final String LOG_FILE = "flashcard_log.txt";
    private static int sessionCounter = 0;
    private static int mistakeCounter = 0;

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

        Map<String, PerformanceData> logData = loadLog();
        sessionCounter++;
        mistakeCounter = 0;
        System.out.println("[DEBUG] Starting session #" + sessionCounter);

        List<FlashCard> cards = loadCards(fileName);
        if (cards.isEmpty()) {
            System.out.println("No cards loaded. Check your file.");
            return;
        }

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
                mistakeCounter++;
                current.recordMistake(sessionCounter, mistakeCounter);
                System.out.println("Incorrect!");
            } else {
                current.incrementCorrectAnswers(sessionCounter);
                System.out.println("Correct! (" + current.getSessionCorrect() + "/" + repetitions + ")");
            }
        }
        scanner.close();

        evaluateAchievements(cards);
        printFlashCardLog(cards);
        writeLog(cards);
    }

    private static void printHelp() {
        System.out.println("Usage: flashcard <cards-file> [options]");
        System.out.println("Options:");
        System.out.println("  --help                 Тусламжийн мэдээлэл харуулах");
        System.out.println("  --order <order>        Зохион байгуулалтын төрөл, default нь \"random\"");
        System.out.println("                         [сонголт: \"random\", \"worst-first\", \"recent-mistakes-first\"]");
        System.out.println("  --repetitions <num>    Нэг картыг хэдэн удаа асууна (тахирлах).");
        System.out.println("  --invertCards          Тохиргоо идэвхэжсэн бол картын асуулт, хариултыг сольж харуулна.");
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
            case "worst-first": 
                return new WorstFirstSorter();
            case "recent-mistakes-first":
                return new RecentMistakesFirstSorter();
            default:
                return new RandomSorter();
        }
    }

    private static void evaluateAchievements(List<FlashCard> cards) {
        boolean allCorrect = true;
        boolean repeatAchieved = false;
        boolean confidentAchieved = false;

        for (FlashCard card : cards) {
            if (card.getMistakes() > 0) allCorrect = false;
            if (card.getAttempts() > 5) repeatAchieved = true;
            if (card.getCorrectAnswers() >= 3) confidentAchieved = true;
        }
        System.out.println("\nAchievements:");
        if (allCorrect) System.out.println(" CORRECT: All cards answered correctly in the last round!");
        if (repeatAchieved) System.out.println("REPEAT: A card was attempted more than 5 times!");
        if (confidentAchieved) System.out.println("CONFIDENT: A card was answered correctly at least 3 times!");
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
        int lastSessionWithMistake;
        int lastMistakeOrderInSession;

        public PerformanceData(int attempts, int mistakes, int correctAnswers,
                               int lastSessionWithMistake, int lastMistakeOrderInSession) {
            this.attempts = attempts;
            this.mistakes = mistakes;
            this.correctAnswers = correctAnswers;
            this.lastSessionWithMistake = lastSessionWithMistake;
            this.lastMistakeOrderInSession = lastMistakeOrderInSession;
        }
    }

    private static Map<String, PerformanceData> loadLog() {
        Map<String, PerformanceData> map = new HashMap<>();
        File logFile = new File(LOG_FILE);
        if (!logFile.exists()) return map;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String firstLine = reader.readLine();
            if (firstLine != null && firstLine.startsWith("SESSION_COUNTER;")) {
                String[] sessionParts = firstLine.split(";");
                if (sessionParts.length >= 2) {
                    sessionCounter = Integer.parseInt(sessionParts[1].trim());
                }
            } else if (firstLine != null) {
                processFlashcardLine(firstLine, map);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                processFlashcardLine(line, map);
            }
        } catch (IOException e) {
            System.out.println("Error reading log file: " + e.getMessage());
        }
        return map;
    }

    private static void processFlashcardLine(String line, Map<String, PerformanceData> map) {
        String[] parts = line.split(";");
        if (parts.length == 6) {
            String question = parts[0].trim();
            int attempts = Integer.parseInt(parts[1].trim());
            int mistakes = Integer.parseInt(parts[2].trim());
            int correctAnswers = Integer.parseInt(parts[3].trim());
            int lastSession = Integer.parseInt(parts[4].trim());
            int lastOrder = Integer.parseInt(parts[5].trim());
            map.put(question, new PerformanceData(attempts, mistakes, correctAnswers, lastSession, lastOrder));
        }
    }

    private static void mergeLogIntoCards(List<FlashCard> cards, Map<String, PerformanceData> logData) {
        for (FlashCard card : cards) {
            if (logData.containsKey(card.getQuestion())) {
                PerformanceData pd = logData.get(card.getQuestion());
                card.updateFromLog(pd.attempts, pd.mistakes, pd.correctAnswers, pd.lastSessionWithMistake);
                card.recordMistake(pd.lastSessionWithMistake, pd.lastMistakeOrderInSession);
            }
        }
    }

    private static void writeLog(List<FlashCard> cards) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE))) {
            writer.println("SESSION_COUNTER;" + sessionCounter);
            for (FlashCard card : cards) {
                writer.println(card.getQuestion() + ";" + card.getAttempts() + ";" +
                        card.getMistakes() + ";" + card.getCorrectAnswers() + ";" +
                        card.getLastSessionWithMistake() + ";" + card.getMistakeOrderInLastSession());
            }
        } catch (IOException e) {
            System.out.println("Error writing log file: " + e.getMessage());
        }
    }
}
