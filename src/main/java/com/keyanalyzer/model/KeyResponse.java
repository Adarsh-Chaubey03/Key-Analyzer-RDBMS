package com.keyanalyzer.model;

import java.util.List;
import java.util.Map;

public class KeyResponse {

    private List<List<String>> candidateKeys;
    private Object superkeys; // List<List<String>> or String message
    private Map<String, Object> info;

    public KeyResponse() {}

    public List<List<String>> getCandidateKeys() { return candidateKeys; }
    public void setCandidateKeys(List<List<String>> candidateKeys) { this.candidateKeys = candidateKeys; }

    public Object getSuperkeys() { return superkeys; }
    public void setSuperkeys(Object superkeys) { this.superkeys = superkeys; }

    public Map<String, Object> getInfo() { return info; }
    public void setInfo(Map<String, Object> info) { this.info = info; }
}
