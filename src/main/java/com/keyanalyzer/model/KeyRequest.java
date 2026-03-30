package com.keyanalyzer.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class KeyRequest {

    @NotEmpty(message = "Attributes list cannot be empty")
    private List<String> attributes;

    @NotEmpty(message = "Functional dependencies list cannot be empty")
    @Valid
    private List<FunctionalDependency> fds;

    public KeyRequest() {}

    public List<String> getAttributes() { return attributes; }
    public void setAttributes(List<String> attributes) { this.attributes = attributes; }

    public List<FunctionalDependency> getFds() { return fds; }
    public void setFds(List<FunctionalDependency> fds) { this.fds = fds; }
}
