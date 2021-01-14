package org.devopsmindset.gradle.model;

import lombok.Data;

/**
 * Dependency Manager Extension
 */
@Data
public class DependencyManagerExtension {
    private String[][] configurations;
    private Boolean[] stripVersion;
    private Boolean[] separateByGroupId;
}