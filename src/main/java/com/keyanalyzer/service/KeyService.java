package com.keyanalyzer.service;

import com.keyanalyzer.core.KeyAnalyzer;
import com.keyanalyzer.model.FunctionalDependency;
import com.keyanalyzer.model.KeyRequest;
import com.keyanalyzer.model.KeyResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KeyService {

    private static final int SUPERKEY_ATTRIBUTE_LIMIT = 8;

    public KeyResponse computeKeys(KeyRequest request) {

        List<String> attributes = request.getAttributes().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // Normalize FDs: trim whitespace
        List<FunctionalDependency> normalizedFds = new ArrayList<>();
        for (FunctionalDependency fd : request.getFds()) {
            List<String> left = fd.getLeft().stream().map(String::trim).collect(Collectors.toList());
            List<String> right = fd.getRight().stream().map(String::trim).collect(Collectors.toList());
            normalizedFds.add(new FunctionalDependency(left, right));
        }

        // Validate: all FD attributes must be in the attribute set
        Set<String> attrSet = new LinkedHashSet<>(attributes);
        for (FunctionalDependency fd : normalizedFds) {
            for (String a : fd.getLeft()) {
                if (!attrSet.contains(a))
                    throw new IllegalArgumentException("Attribute '" + a + "' in FD left side is not in the relation.");
            }
            for (String a : fd.getRight()) {
                if (!attrSet.contains(a))
                    throw new IllegalArgumentException(
                            "Attribute '" + a + "' in FD right side is not in the relation.");
            }
        }

        KeyAnalyzer analyzer = new KeyAnalyzer(attributes, normalizedFds);

        // Compute candidate keys
        List<Set<String>> candidateKeys = analyzer.findCandidateKeys();

        // Compute superkeys (conditional)
        List<Set<String>> superkeys = analyzer.findSuperkeys();

        // Build response
        KeyResponse response = new KeyResponse();
        response.setCandidateKeys(
                candidateKeys.stream()
                        .map(s -> new ArrayList<>(s))
                        .collect(Collectors.toList()));

        if (superkeys != null) {
            response.setSuperkeys(
                    superkeys.stream()
                            .map(s -> new ArrayList<>(s))
                            .collect(Collectors.toList()));
        } else {
            response.setSuperkeys(
                    "Superkey enumeration skipped — with " + attributes.size()
                            + " attributes, the number of possible subsets (2^" + attributes.size() + " = "
                            + (1L << attributes.size())
                            + ") grows exponentially, making full enumeration computationally infeasible.");
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("attributeCount", attributes.size());
        info.put("candidateKeyCount", candidateKeys.size());
        response.setInfo(info);

        return response;
    }
}
