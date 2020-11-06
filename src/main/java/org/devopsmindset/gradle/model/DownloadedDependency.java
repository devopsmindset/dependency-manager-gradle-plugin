package org.devopsmindset.gradle.model;

import lombok.Data;
import org.gradle.api.artifacts.ResolvedArtifact;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;

@Data
public class DownloadedDependency {
    private String group;
    private String artifact;
    private String version;
    private String extension;
    private String classifer;
    private String location;
    private String configuration;
    private String reason;
    private boolean decompressed = false;

    public DownloadedDependency(ResolvedArtifact artifact) {
        setArtifact(artifact.getModuleVersion().getId().getName());
        setClassifer(artifact.getClassifier());
        setExtension(artifact.getExtension().toLowerCase());
        setGroup(artifact.getModuleVersion().getId().getGroup());
        setVersion(artifact.getModuleVersion().getId().getVersion());
    }

    public DownloadedDependency(ResolvedArtifact artifact, boolean stripVersion, boolean decompress, boolean extensionToDecompress, String configuration, Path downloadPath) {
        this(artifact);
        String artifactDestination =  artifact.getFile().getName();
        // Strip version from destination
        if (stripVersion) {
            artifactDestination = artifactDestination.replaceAll("-" + artifact.getModuleVersion().getId().getVersion(), "");
        }
        if (decompress && extensionToDecompress){
            setDecompressed(true);
        }
        setConfiguration(configuration);
        setLocation(Paths.get(downloadPath.toString(), artifact.getModuleVersion().getId().getModule().getGroup(), artifactDestination).toString());
    }
}
