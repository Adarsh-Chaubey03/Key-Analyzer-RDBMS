package com.keyanalyzer.core;

import com.keyanalyzer.model.FunctionalDependency;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KeyAnalyzerTest {

    @Test
    void findCandidateKeysHandlesMultiStepExpansionFromEssentialSeed() {
        KeyAnalyzer analyzer = new KeyAnalyzer(
                List.of("Z", "A", "B", "C", "D"),
                List.of(
                        fd("A", "B"),
                        fd("B", "A"),
                        fd("C", "D"),
                        fd("D", "C")
                )
        );

        List<String> candidateKeys = analyzer.findCandidateKeys().stream()
                .map(key -> String.join("", key))
                .collect(Collectors.toList());

        assertEquals(List.of("ZAC", "ZAD", "ZBC", "ZBD"), candidateKeys);
    }

    @Test
    void findSuperkeysReturnsNullWhenEnumerationWouldExplode() {
        KeyAnalyzer analyzer = new KeyAnalyzer(
                List.of("A", "B", "C", "D", "E", "F", "G", "H", "I"),
                List.of(fd("A", "A"))
        );

        assertNull(analyzer.findSuperkeys());
    }

    private FunctionalDependency fd(String left, String right) {
        return new FunctionalDependency(split(left), split(right));
    }

    private List<String> split(String value) {
        return value.chars()
                .mapToObj(character -> String.valueOf((char) character))
                .collect(Collectors.toList());
    }
}
