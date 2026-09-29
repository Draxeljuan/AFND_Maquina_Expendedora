package com.vending.machine.domain.service;

import com.vending.machine.domain.model.AutomatonResult;
import com.vending.machine.domain.model.NfaState;
import com.vending.machine.domain.model.Symbol;
import com.vending.machine.domain.ports.in.ProcessSequenceUseCase;

import java.util.*;

public class NfaTransitionEngine implements ProcessSequenceUseCase {

    @Override
    public AutomatonResult process(String sequence) {
        if (sequence == null) sequence = "";

        List<Symbol> symbols = new ArrayList<>();
        for (char c : sequence.toCharArray()) {
            Optional<Symbol> symbolOpt = Symbol.fromChar(c);
            if (symbolOpt.isEmpty()) {
                return AutomatonResult.invalidAlphabet(sequence);
            }
            symbols.add(symbolOpt.get());
        }

        // Clausura lambda inicial desde el estado de reposo
        Set<NfaState> currentStates = epsilonClosure(Set.of(NfaState.Q0));

        // Transiciones consumiendo la cadena
        for (Symbol symbol : symbols) {
            Set<NfaState> nextStates = new HashSet<>();

            for (NfaState state : currentStates) {
                nextStates.addAll(delta(state, symbol));
            }

            // Aplicar saltos lambda automáticos descubiertos tras consumir el símbolo
            currentStates = epsilonClosure(nextStates);

            if (currentStates.isEmpty()) {
                break;
            }
        }

        return AutomatonResult.processed(currentStates);
    }

    private Set<NfaState> epsilonClosure(Set<NfaState> states) {
        Set<NfaState> closure = new HashSet<>(states);
        Queue<NfaState> queue = new LinkedList<>(states);

        while (!queue.isEmpty()) {
            NfaState state = queue.poll();
            Set<NfaState> epsilonTransitions = delta(state, Symbol.LAMBDA);

            for (NfaState nextState : epsilonTransitions) {
                if (closure.add(nextState)) {
                    queue.add(nextState);
                }
            }
        }
        return closure;
    }

    /**
     * Función de Transición No Determinista δ(q, σ) mapeada para 15 productos
     */
    private Set<NfaState> delta(NfaState state, Symbol symbol) {
        return switch (state) {
            case Q0 -> switch (symbol) {
                case ZERO -> Set.of(NfaState.Q0);       // Bucle Reset[cite: 5]
                case LAMBDA -> Set.of(NfaState.Q_START); // Salto a evaluación[cite: 5]
                default -> Set.of();
            };

            // Nivel 1: No Determinismo Explícito (Exploración simultánea)[cite: 5]
            case Q_START -> switch (symbol) {
                case ONE -> Set.of(NfaState.Q1, NfaState.Q2);
                case TWO -> Set.of(NfaState.Q3, NfaState.Q4);
                case FIVE -> Set.of(NfaState.Q5, NfaState.Q6);
                default -> Set.of();
            };

            // Ramas '1'[cite: 5]
            case Q1 -> switch (symbol) {
                case ONE -> Set.of(NfaState.P1);
                case TWO -> Set.of(NfaState.P2);
                default -> Set.of();
            };
            case Q2 -> switch (symbol) {
                case FIVE -> Set.of(NfaState.Q2A);
                default -> Set.of();
            };
            case Q2A -> switch (symbol) {
                case LAMBDA -> Set.of(NfaState.P13); // Aceptación temprana de '15'[cite: 5]
                case FIVE -> Set.of(NfaState.P3);
                case ONE -> Set.of(NfaState.P4);
                default -> Set.of();
            };

            // Ramas '2'[cite: 5]
            case Q3 -> switch (symbol) {
                case ONE -> Set.of(NfaState.Q3A);
                default -> Set.of();
            };
            case Q3A -> switch (symbol) {
                case LAMBDA -> Set.of(NfaState.P5);  // Aceptación temprana de '21'[cite: 5]
                case FIVE -> Set.of(NfaState.P6);
                default -> Set.of();
            };
            case Q4 -> switch (symbol) {
                case TWO -> Set.of(NfaState.P7);
                case FIVE -> Set.of(NfaState.Q4A);
                default -> Set.of();
            };
            case Q4A -> switch (symbol) {
                case LAMBDA -> Set.of(NfaState.P14); // Aceptación temprana de '25'[cite: 5]
                case FIVE -> Set.of(NfaState.P8);
                default -> Set.of();
            };

            // Ramas '5'[cite: 5]
            case Q5 -> switch (symbol) {
                case FIVE -> Set.of(NfaState.Q5A);
                default -> Set.of();
            };
            case Q5A -> switch (symbol) {
                case LAMBDA -> Set.of(NfaState.P15); // Aceptación temprana de '55'[cite: 5]
                case FIVE -> Set.of(NfaState.P9);
                case ONE -> Set.of(NfaState.P10);
                default -> Set.of();
            };
            case Q6 -> switch (symbol) {
                case ONE -> Set.of(NfaState.Q6A);
                case TWO -> Set.of(NfaState.P12);
                default -> Set.of();
            };
            case Q6A -> switch (symbol) {
                case FIVE -> Set.of(NfaState.P11);
                default -> Set.of();
            };

            // Estados finales P1-P15 mueren si reciben más símbolos
            default -> Set.of();
        };
    }
}