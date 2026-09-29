package com.vending.machine.domain.model;

import java.util.Optional;

public enum Symbol {
    ZERO('0'),
    ONE('1'),
    TWO('2'),
    FIVE('5'),
    LAMBDA('L'); // Representa la transición vacía (λ)

    private final char character;

    Symbol(char character) {
        this.character = character;
    }

    public static Optional<Symbol> fromChar(char c) {
        return switch (c) {
            case '0' -> Optional.of(ZERO);
            case '1' -> Optional.of(ONE);
            case '2' -> Optional.of(TWO);
            case '5' -> Optional.of(FIVE);
            default -> Optional.empty();
        };
    }
}
