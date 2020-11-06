package org.devopsmindset.gradle.model;

import lombok.Data;

@Data
public class DependencyManagerExtension {
    private String[][] configurations;
    private boolean[] separateByGroupId;
    private boolean[] stripVersion;
    private boolean[] decompress;
}