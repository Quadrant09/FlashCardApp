package com.example;

import java.util.Comparator;
import java.util.List;


class WorstFirstSorter implements CardOrganizer {
    @Override
    public List<FlashCard> organize(List<FlashCard> cards) {
        cards.sort(Comparator.comparingInt(FlashCard::getMistakes).reversed());
        return cards;
    }
}

