package com.vending.machine.domain.model;

import java.util.Set;

public record AutomatonResult(
        Set<NfaState> finalActiveStates,
        boolean belongsToAlphabet,
        boolean isAccepted,
        NfaState acceptedSlot,
        String message
) {
    public static AutomatonResult invalidAlphabet(String input) {
        return new AutomatonResult(
                Set.of(), false, false, null,
                "La cadena '" + input + "' contiene símbolos inválidos. No pertenece al alfabeto Σ."
        );
    }

    public static AutomatonResult processed(Set<NfaState> finalStates) {
        // En un AFND, la cadena es aceptada si al menos un estado activo final es de aceptación.
        NfaState slot = finalStates.stream()
                .filter(NfaState::isAcceptance)
                .findFirst()
                .orElse(null);

        boolean accepted = (slot != null);
        String msg = accepted
                ? "Cadena ACEPTADA. Se alcanzó el " + slot.name()
                : "Cadena RECHAZADA. Los caminos murieron o no alcanzaron un estado final.";

        return new AutomatonResult(finalStates, true, accepted, slot, msg);
    }
}
