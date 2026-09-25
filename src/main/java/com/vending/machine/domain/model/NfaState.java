package com.vending.machine.domain.model;

public enum NfaState {
    Q0, Q_START,
    Q1, Q2, Q5,
    Q11, Q21, Q55,
    P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12;

    /**
     * Identifica si el estado actual pertenece al conjunto de estados de aceptación F.
     */
    public boolean isAcceptance() {
        return this.name().startsWith("P");
    }
}