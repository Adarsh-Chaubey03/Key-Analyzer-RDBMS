package com.keyanalyzer.service;

import com.keyanalyzer.model.FunctionalDependency;
import com.keyanalyzer.model.KeyRequest;
import com.keyanalyzer.model.KeyResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeyServiceTest {

    private final KeyService keyService = new KeyService();

    @Test
    void computeKeysNormalizesWhitespaceAndDuplicateValues() {
        KeyRequest request = new KeyRequest();
        request.setAttributes(List.of(" ID ", "Dept", "Dept", "Name "));
        request.setFds(List.of(
                new FunctionalDependency(List.of(" ID "), List.of(" Name ")),
                new FunctionalDependency(List.of("ID"), List.of("Dept"))
        ));

        KeyResponse response = keyService.computeKeys(request);

        assertEquals(List.of(List.of("ID")), response.getCandidateKeys());
    }

    @Test
    void computeKeysRejectsUnknownAttributes() {
        KeyRequest request = new KeyRequest();
        request.setAttributes(List.of("A", "B"));
        request.setFds(List.of(new FunctionalDependency(List.of("A"), List.of("C"))));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> keyService.computeKeys(request));

        assertEquals("Attribute 'C' in FD right side is not in the relation.", exception.getMessage());
    }

    @Test
    void computeKeysSkipsSuperkeysWhenAttributeCountExceedsLimit() {
        KeyRequest request = new KeyRequest();
        request.setAttributes(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I"));
        request.setFds(List.of(new FunctionalDependency(List.of("A"), List.of("A"))));

        KeyResponse response = keyService.computeKeys(request);

        assertInstanceOf(String.class, response.getSuperkeys());
    }
}
