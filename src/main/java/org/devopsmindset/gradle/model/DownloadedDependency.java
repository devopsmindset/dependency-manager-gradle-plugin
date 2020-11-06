package org.devopsmindset.gradle.model;

import lombok.Data;

import javax.annotation.Nullable;

@Data
public class DownloadedDependency {
    private String group;
    private String artifact;
    private String version;
    private String extension;
    @Nullable
    private String classifer;
    private String location;
    private String configuration;
    private boolean decompressed = false;
}
