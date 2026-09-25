package com.vending.machine.domain.ports.in;

import com.vending.machine.domain.model.AutomatonResult;

public interface ProcessSequenceUseCase {
    AutomatonResult process(String sequence);
}