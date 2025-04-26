package com.example;

import java.util.List;

class RecentMistakesFirstSorter implements CardOrganizer {
    @Override
    public List<FlashCard> organize(List<FlashCard> cards) {
        // DEBUG: Show sorting inputs
        System.out.println("Sorting by recent mistakes:");
        for (FlashCard card : cards) {
            System.out.println("  " + card.getQuestion() +
                " | Session: " + card.getLastSessionWithMistake() +
                ", Order: " + card.getMistakeOrderInLastSession());
        }

        // SORT logic (most recent mistake session, then latest order)
        cards.sort((c1, c2) -> {
            int sessionCompare = Integer.compare(
                c2.getLastSessionWithMistake(),
                c1.getLastSessionWithMistake()
            );
            if (sessionCompare != 0) return sessionCompare;

            // Smaller order means happened earlier → so to get most recent last, we want descending
            return Integer.compare(
                c2.getMistakeOrderInLastSession(),  // DESCENDING mistake order
                c1.getMistakeOrderInLastSession()
            );
        });

        // DEBUG: Show sorted result
        System.out.println("Sorted order:");
        for (FlashCard card : cards) {
            System.out.println("  " + card.getQuestion() +
                " | Session: " + card.getLastSessionWithMistake() +
                ", Order: " + card.getMistakeOrderInLastSession());
        }

        return cards;
    }
}


