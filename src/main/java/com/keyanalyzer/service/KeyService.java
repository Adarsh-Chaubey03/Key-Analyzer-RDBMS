package com.keyanalyzer.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.keyanalyzer.core.KeyAnalyzer;
import com.keyanalyzer.model.FunctionalDependency;
import com.keyanalyzer.model.KeyRequest;
import com.keyanalyzer.model.KeyResponse;

@Service
public class KeyService {

    public KeyResponse computeKeys(KeyRequest request) {
        NormalizedRequest normalizedRequest = normalize(request);
        KeyAnalyzer analyzer = new KeyAnalyzer(normalizedRequest.attributes(), normalizedRequest.fds());

        List<Set<String>> candidateKeys = analyzer.findCandidateKeys();
        List<Set<String>> superkeys = analyzer.findSuperkeys();

        KeyResponse response = new KeyResponse();
        response.setCandidateKeys(toLists(candidateKeys));
        response.setSuperkeys(
                superkeys != null
                        ? toLists(superkeys)
                        : buildSuperkeySkipMessage(normalizedRequest.attributes().size())
        );

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("attributeCount", normalizedRequest.attributes().size());
        info.put("candidateKeyCount", candidateKeys.size());
        info.put("superkeysEnumerated", superkeys != null);
        response.setInfo(info);

        return response;
    }

    private NormalizedRequest normalize(KeyRequest request) {
        List<String> attributes = normalizeAttributeList(request.getAttributes(), "Attributes list");
        Set<String> relationAttributes = new LinkedHashSet<>(attributes);

        List<FunctionalDependency> rawFds = request.getFds();
        if (rawFds == null || rawFds.isEmpty()) {
            throw new IllegalArgumentException("Functional dependencies list cannot be empty.");
        }

        List<FunctionalDependency> normalizedFds = rawFds.stream()
                .map(this::normalizeFunctionalDependency)
                .collect(Collectors.toList());

        for (FunctionalDependency fd : normalizedFds) {
            validateAttributesExist(relationAttributes, fd.getLeft(), "left");
            validateAttributesExist(relationAttributes, fd.getRight(), "right");
        }

        return new NormalizedRequest(attributes, normalizedFds);
    }

    private FunctionalDependency normalizeFunctionalDependency(FunctionalDependency fd) {
        return new FunctionalDependency(
                normalizeAttributeList(fd.getLeft(), "Left side of FD"),
                normalizeAttributeList(fd.getRight(), "Right side of FD")
        );
    }

    private List<String> normalizeAttributeList(List<String> rawValues, String fieldName) {
        if (rawValues == null) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }

        LinkedHashSet<String> normalizedValues = rawValues.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalizedValues.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }

        return new ArrayList<>(normalizedValues);
    }

    private void validateAttributesExist(Set<String> relationAttributes, List<String> fdAttributes, String side) {
        for (String attribute : fdAttributes) {
            if (!relationAttributes.contains(attribute)) {
                throw new IllegalArgumentException(
                        "Attribute '" + attribute + "' in FD " + side + " side is not in the relation."
                );
            }
        }
    }

    private List<List<String>> toLists(List<Set<String>> keys) {
        return keys.stream()
                .map(ArrayList::new)
                .collect(Collectors.toList());
    }

    private String buildSuperkeySkipMessage(int attributeCount) {
        return "Superkey enumeration was skipped for "
            + attributeCount
            + " attributes due to the exponential search space (2 to the power n subsets). Candidate keys were computed using optimized pruning algorithms to avoid exhaustive enumeration.";
    }

    private record NormalizedRequest(List<String> attributes, List<FunctionalDependency> fds) {
    }
}
