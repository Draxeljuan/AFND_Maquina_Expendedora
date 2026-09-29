package com.vending.machine.domain.model;

public enum NfaState {
    Q0, Q_START,
    Q1, Q2, Q3, Q4, Q5, Q6,
    Q2A, Q3A, Q4A, Q5A, Q6A,
    P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15;

    /**
     * Identifica si el estado actual pertenece al conjunto de estados de aceptación F (P1 a P15).
     */
    public boolean isAcceptance() {
        return this.name().startsWith("P");
    }
}