package com.keyanalyzer.core;

import com.keyanalyzer.model.FunctionalDependency;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure algorithm class for computing attribute closure, candidate keys, and superkeys.
 * No Spring dependency — fully testable in isolation.
 */
public class KeyAnalyzer {

    private final Set<String> allAttributes;
    private final List<FunctionalDependency> fds;
    private int stepsCount = 0;

    public KeyAnalyzer(Collection<String> attributes, List<FunctionalDependency> fds) {
        this.allAttributes = new LinkedHashSet<>(attributes);
        this.fds = fds;
    }

    public int getStepsCount() { return stepsCount; }

    // --- Attribute Closure ---

    /**
     * Computes the closure of a set of attributes under the given FDs.
     * Iterative fixpoint: keep applying FDs until nothing changes.
     */
    public Set<String> attributeClosure(Set<String> attrs) {
        Set<String> closure = new LinkedHashSet<>(attrs);
        boolean changed = true;
        while (changed) {
            changed = false;
            stepsCount++;
            for (FunctionalDependency fd : fds) {
                if (closure.containsAll(fd.getLeft())) {
                    if (closure.addAll(fd.getRight())) {
                        changed = true;
                    }
                }
            }
        }
        return closure;
    }

    /**
     * Checks if a set of attributes is a superkey (its closure covers all attributes).
     */
    public boolean isSuperkey(Set<String> attrs) {
        return attributeClosure(attrs).containsAll(allAttributes);
    }

    // --- Candidate Key Discovery ---

    /**
     * Finds all candidate keys using BFS with RHS-reduction pruning.
     *
     * Strategy:
     * 1. Partition attributes into ESSENTIAL (never on any RHS) and NON-ESSENTIAL.
     * 2. Start from the set of essential attributes as the seed.
     * 3. If the seed is already a superkey, it's the only candidate key.
     * 4. Otherwise, do level-wise BFS: at each level, expand current sets by
     *    adding one non-essential attribute. Prune any set that is a superset
     *    of an already-found candidate key.
     */
    public List<Set<String>> findCandidateKeys() {
        Set<String> rhsAttributes = new LinkedHashSet<>();
        for (FunctionalDependency fd : fds) {
            rhsAttributes.addAll(fd.getRight());
        }

        // Essential attributes: appear ONLY on the left side (never on any RHS)
        Set<String> essential = new LinkedHashSet<>();
        Set<String> nonEssential = new LinkedHashSet<>();
        for (String attr : allAttributes) {
            if (!rhsAttributes.contains(attr)) {
                essential.add(attr);
            } else {
                nonEssential.add(attr);
            }
        }

        List<Set<String>> candidateKeys = new ArrayList<>();

        // Check if essential set alone is a superkey
        if (isSuperkey(essential)) {
            candidateKeys.add(essential);
            return candidateKeys;
        }

        // BFS: start with essential set, expand with non-essential attributes level by level
        List<String> nonEssentialList = new ArrayList<>(nonEssential);
        Queue<Set<String>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        // Initialize: essential + each single non-essential attribute OR just essential if empty
        if (essential.isEmpty()) {
            for (String attr : nonEssentialList) {
                Set<String> seed = new LinkedHashSet<>();
                seed.add(attr);
                String key = canonicalKey(seed);
                if (visited.add(key)) {
                    queue.add(seed);
                }
            }
        } else {
            // Try essential set first, then expand
            for (String attr : nonEssentialList) {
                Set<String> candidate = new LinkedHashSet<>(essential);
                candidate.add(attr);
                String key = canonicalKey(candidate);
                if (visited.add(key)) {
                    queue.add(candidate);
                }
            }
        }

        while (!queue.isEmpty()) {
            Set<String> current = queue.poll();

            // Prune: skip if current is a superset of a known candidate key
            if (isSupersetOfAny(current, candidateKeys)) {
                continue;
            }

            if (isSuperkey(current)) {
                candidateKeys.add(current);
                continue; // Don't expand further — supersets would not be minimal
            }

            // Expand: add one more non-essential attribute (maintain sorted order to avoid duplicates)
            String maxAttr = Collections.max(current);
            for (String attr : nonEssentialList) {
                if (attr.compareTo(maxAttr) > 0 && !current.contains(attr)) {
                    Set<String> expanded = new LinkedHashSet<>(current);
                    expanded.add(attr);
                    String key = canonicalKey(expanded);
                    if (visited.add(key) && !isSupersetOfAny(expanded, candidateKeys)) {
                        queue.add(expanded);
                    }
                }
            }
        }

        // Sort candidate keys by size, then lexicographically
        candidateKeys.sort(Comparator.<Set<String>>comparingInt(Set::size)
                .thenComparing(s -> String.join(",", s)));

        return candidateKeys;
    }

    // --- Superkey Generation ---

    /**
     * Generates all superkeys ONLY if attribute count ≤ 8.
     * Returns null if skipped due to exponential growth.
     */
    public List<Set<String>> findSuperkeys() {
        if (allAttributes.size() > 8) {
            return null; // Signal to caller to use skip message
        }

        List<String> attrList = new ArrayList<>(allAttributes);
        int n = attrList.size();
        List<Set<String>> superkeys = new ArrayList<>();

        // Enumerate all non-empty subsets via bitmask
        for (int mask = 1; mask < (1 << n); mask++) {
            Set<String> subset = new LinkedHashSet<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.add(attrList.get(i));
                }
            }
            if (isSuperkey(subset)) {
                superkeys.add(subset);
            }
        }

        // Sort by size, then lexicographically
        superkeys.sort(Comparator.<Set<String>>comparingInt(Set::size)
                .thenComparing(s -> String.join(",", s)));

        return superkeys;
    }

    // --- Helpers ---

    private boolean isSupersetOfAny(Set<String> candidate, List<Set<String>> keys) {
        for (Set<String> key : keys) {
            if (candidate.containsAll(key)) {
                return true;
            }
        }
        return false;
    }

    private String canonicalKey(Set<String> attrs) {
        List<String> sorted = new ArrayList<>(attrs);
        Collections.sort(sorted);
        return String.join(",", sorted);
    }
}
