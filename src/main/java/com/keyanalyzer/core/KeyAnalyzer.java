package com.keyanalyzer.core;

import com.keyanalyzer.model.FunctionalDependency;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure algorithm class for computing attribute closure, candidate keys, and superkeys.
 */
public class KeyAnalyzer {

    public static final int SUPERKEY_ATTRIBUTE_LIMIT = 8;

    private final Set<String> allAttributes;
    private final List<String> orderedAttributes;
    private final Map<String, Integer> attributeOrder;
    private final List<FunctionalDependency> fds;
    private final Map<String, Set<String>> closureCache = new HashMap<>();
    private final Map<String, Boolean> superkeyCache = new HashMap<>();
    private int stepsCount = 0;

    public KeyAnalyzer(Collection<String> attributes, List<FunctionalDependency> fds) {
        this.allAttributes = new LinkedHashSet<>(attributes);
        this.orderedAttributes = new ArrayList<>(this.allAttributes);
        this.attributeOrder = new HashMap<>();
        for (int i = 0; i < orderedAttributes.size(); i++) {
            attributeOrder.put(orderedAttributes.get(i), i);
        }
        this.fds = List.copyOf(fds);
    }

    public int getStepsCount() {
        return stepsCount;
    }

    public Set<String> attributeClosure(Set<String> attrs) {
        String cacheKey = canonicalKey(attrs);
        Set<String> cached = closureCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Set<String> closure = new LinkedHashSet<>(sortAttributes(attrs));
        boolean changed = true;
        while (changed) {
            changed = false;
            stepsCount++;
            for (FunctionalDependency fd : fds) {
                if (closure.containsAll(fd.getLeft()) && closure.addAll(fd.getRight())) {
                    changed = true;
                }
            }
        }

        Set<String> computedClosure = Collections.unmodifiableSet(new LinkedHashSet<>(closure));
        closureCache.put(cacheKey, computedClosure);
        return computedClosure;
    }

    public boolean isSuperkey(Set<String> attrs) {
        String cacheKey = canonicalKey(attrs);
        return superkeyCache.computeIfAbsent(cacheKey, ignored -> attributeClosure(attrs).containsAll(allAttributes));
    }

    public List<Set<String>> findCandidateKeys() {
        Set<String> rhsAttributes = new LinkedHashSet<>();
        for (FunctionalDependency fd : fds) {
            rhsAttributes.addAll(fd.getRight());
        }

        Set<String> essential = new LinkedHashSet<>();
        Set<String> nonEssential = new LinkedHashSet<>();
        for (String attribute : allAttributes) {
            if (!rhsAttributes.contains(attribute)) {
                essential.add(attribute);
            } else {
                nonEssential.add(attribute);
            }
        }

        List<Set<String>> candidateKeys = new ArrayList<>();
        if (isSuperkey(essential)) {
            candidateKeys.add(new LinkedHashSet<>(essential));
            return candidateKeys;
        }

        List<String> nonEssentialList = new ArrayList<>(nonEssential);
        Queue<SearchState> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        for (int i = 0; i < nonEssentialList.size(); i++) {
            Set<String> seed = new LinkedHashSet<>(essential);
            seed.add(nonEssentialList.get(i));
            if (visited.add(canonicalKey(seed))) {
                queue.add(new SearchState(seed, i + 1));
            }
        }

        while (!queue.isEmpty()) {
            SearchState state = queue.poll();
            Set<String> current = state.attributes();

            if (isSupersetOfAny(current, candidateKeys)) {
                continue;
            }

            if (isSuperkey(current)) {
                candidateKeys.add(new LinkedHashSet<>(current));
                continue;
            }

            for (int i = state.nextIndex(); i < nonEssentialList.size(); i++) {
                String attribute = nonEssentialList.get(i);
                if (current.contains(attribute)) {
                    continue;
                }

                Set<String> expanded = new LinkedHashSet<>(current);
                expanded.add(attribute);
                if (visited.add(canonicalKey(expanded)) && !isSupersetOfAny(expanded, candidateKeys)) {
                    queue.add(new SearchState(expanded, i + 1));
                }
            }
        }

        candidateKeys.sort(Comparator.<Set<String>>comparingInt(Set::size)
                .thenComparing(this::canonicalKey));
        return candidateKeys;
    }

    public List<Set<String>> findSuperkeys() {
        if (allAttributes.size() > SUPERKEY_ATTRIBUTE_LIMIT) {
            return null;
        }

        int attributeCount = orderedAttributes.size();
        List<Set<String>> superkeys = new ArrayList<>();

        for (int mask = 1; mask < (1 << attributeCount); mask++) {
            Set<String> subset = new LinkedHashSet<>();
            for (int i = 0; i < attributeCount; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.add(orderedAttributes.get(i));
                }
            }

            if (isSuperkey(subset)) {
                superkeys.add(subset);
            }
        }

        superkeys.sort(Comparator.<Set<String>>comparingInt(Set::size)
                .thenComparing(this::canonicalKey));
        return superkeys;
    }

    private boolean isSupersetOfAny(Set<String> candidate, List<Set<String>> keys) {
        for (Set<String> key : keys) {
            if (candidate.containsAll(key)) {
                return true;
            }
        }
        return false;
    }

    private String canonicalKey(Set<String> attrs) {
        return sortAttributes(attrs).stream().collect(Collectors.joining(","));
    }

    private List<String> sortAttributes(Collection<String> attrs) {
        return attrs.stream()
                .sorted(Comparator.comparingInt(attributeOrder::get))
                .collect(Collectors.toList());
    }

    private record SearchState(Set<String> attributes, int nextIndex) {
    }
}
