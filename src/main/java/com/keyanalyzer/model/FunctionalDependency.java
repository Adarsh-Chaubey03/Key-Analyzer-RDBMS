package com.keyanalyzer.model;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class FunctionalDependency {

    @NotEmpty(message = "Left side of FD cannot be empty")
    private List<String> left;

    @NotEmpty(message = "Right side of FD cannot be empty")
    private List<String> right;

    public FunctionalDependency() {}

    public FunctionalDependency(List<String> left, List<String> right) {
        this.left = left;
        this.right = right;
    }

    public List<String> getLeft() { return left; }
    public void setLeft(List<String> left) { this.left = left; }

    public List<String> getRight() { return right; }
    public void setRight(List<String> right) { this.right = right; }
}
