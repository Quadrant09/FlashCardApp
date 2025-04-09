package com.example;

import java.util.Collections;
import java.util.List;
import java.util.*;

class RandomSorter implements CardOrganizer {
    private final Random random = new Random();
    @Override
    public List<FlashCard> organize(List<FlashCard> cards) {
        // For random, we simply shuffle the list.
        Collections.shuffle(cards, random);
        return cards;
    }
}

