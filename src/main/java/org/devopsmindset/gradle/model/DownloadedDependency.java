package org.devopsmindset.gradle.model;

import lombok.Data;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

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
    private boolean differentFromBase = false;

    public DownloadedDependency() {
    }

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

    public boolean findIn(List<DownloadedDependency> baseDependencies) {
        if (baseDependencies != null) {
            for (DownloadedDependency baseDependency : baseDependencies) {
                if (this.compareTo(baseDependency) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public int compareTo(DownloadedDependency downloadedDependency){
        return Comparator.comparing(DownloadedDependency::getGroup)
                .thenComparing(DownloadedDependency::getArtifact)
                .thenComparing(DownloadedDependency::getVersion)
                .thenComparing(DownloadedDependency::getExtension)
                .thenComparing(DownloadedDependency::getClassifer, Comparator.nullsLast(Comparator.reverseOrder()))
                .compare(this, downloadedDependency);
    }

}
