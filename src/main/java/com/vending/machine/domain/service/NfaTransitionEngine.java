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

        // Validar el alfabeto primero
        List<Symbol> symbols = new ArrayList<>();
        for (char c : sequence.toCharArray()) {
            Optional<Symbol> symbolOpt = Symbol.fromChar(c);
            if (symbolOpt.isEmpty()) {
                return AutomatonResult.invalidAlphabet(sequence);
            }
            symbols.add(symbolOpt.get());
        }

        // Estado Inicial: Partimos de q0 y aplicamos la clausura lambda inicial
        Set<NfaState> currentStates = epsilonClosure(Set.of(NfaState.Q0));

        // Procesar cada símbolo de la cadena
        for (Symbol symbol : symbols) {
            Set<NfaState> nextStates = new HashSet<>();

            // Evaluamos la transición para cada uno de los estados activos simultáneamente
            for (NfaState state : currentStates) {
                nextStates.addAll(delta(state, symbol));
            }

            // Tras consumir el símbolo, aplicamos transiciones vacías automáticas
            currentStates = epsilonClosure(nextStates);

            // Si en algún punto el conjunto queda vacío, los caminos murieron
            if (currentStates.isEmpty()) {
                break;
            }
        }

        return AutomatonResult.processed(currentStates);
    }

    /**
     * Calcula la Clausura Lambda (ε-closure).
     * Encuentra todos los estados alcanzables sin consumir ningún símbolo de entrada.
     */
    private Set<NfaState> epsilonClosure(Set<NfaState> states) {
        Set<NfaState> closure = new HashSet<>(states);
        Queue<NfaState> queue = new LinkedList<>(states);

        while (!queue.isEmpty()) {
            NfaState state = queue.poll();
            Set<NfaState> epsilonTransitions = delta(state, Symbol.LAMBDA);

            for (NfaState nextState : epsilonTransitions) {
                if (closure.add(nextState)) { // Si es un estado nuevo descubierto
                    queue.add(nextState);
                }
            }
        }
        return closure;
    }

    /**
     * Función de Transición No Determinista δ(q, σ)
     * Retorna un CONJUNTO de estados resultantes.
     */
    private Set<NfaState> delta(NfaState state, Symbol symbol) {
        return switch (state) {
            case Q0 -> switch (symbol) {
                case ZERO -> Set.of(NfaState.Q0); // Bucle de reinicio
                case LAMBDA -> Set.of(NfaState.Q_START); // Salto para evaluación
                default -> Set.of();
            };
            case Q_START -> switch (symbol) {
                case ONE -> Set.of(NfaState.Q1);
                case TWO -> Set.of(NfaState.Q2);
                case FIVE -> Set.of(NfaState.Q5);
                default -> Set.of();
            };
            // Desglose Rama '1'
            case Q1 -> switch (symbol) {
                case ONE -> Set.of(NfaState.Q11);
                case TWO -> Set.of(NfaState.P2);
                case FIVE -> Set.of(NfaState.P3);
                default -> Set.of();
            };
            case Q11 -> switch (symbol) {
                case LAMBDA -> Set.of(NfaState.P1); // Secuencia corta '11'
                case FIVE -> Set.of(NfaState.P4);   // Secuencia larga '115'
                default -> Set.of();
            };
            // Desglose Rama '2'
            case Q2 -> switch (symbol) {
                case ONE -> Set.of(NfaState.Q21);
                case TWO -> Set.of(NfaState.P6);
                case FIVE -> Set.of(NfaState.P7);
                default -> Set.of();
            };
            case Q21 -> switch (symbol) {
                case LAMBDA -> Set.of(NfaState.P5);
                case FIVE -> Set.of(NfaState.P8);
                default -> Set.of();
            };
            // Desglose Rama '5'
            case Q5 -> switch (symbol) {
                case ONE -> Set.of(NfaState.P9);
                case TWO -> Set.of(NfaState.P10);
                case FIVE -> Set.of(NfaState.Q55);
                default -> Set.of();
            };
            case Q55 -> switch (symbol) {
                case LAMBDA -> Set.of(NfaState.P11);
                case FIVE -> Set.of(NfaState.P12);
                default -> Set.of();
            };
            // Los estados finales P1-P12 no tienen transiciones salientes en este modelo
            default -> Set.of();
        };
    }
}