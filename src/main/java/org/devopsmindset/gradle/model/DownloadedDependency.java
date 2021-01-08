package org.devopsmindset.gradle.model;

import com.google.common.base.Splitter;
import lombok.Data;
import org.gradle.api.artifacts.ResolvedArtifact;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Data
public class DownloadedDependency {
    private String group;
    private String artifact;
    private String version;
    private String extension;
    private String classifier;
    private String location;
    private String configuration;
    private String reason;
    private Map<String, String> reasons;
    private Boolean differentFromBase = false;
    private String processedArtifactName;

    public DownloadedDependency(ResolvedArtifact artifact) {
        setArtifact(artifact.getModuleVersion().getId().getName());
        setClassifier(artifact.getClassifier());
        setExtension(artifact.getExtension().toLowerCase());
        setGroup(artifact.getModuleVersion().getId().getGroup());
        setVersion(artifact.getModuleVersion().getId().getVersion());
        reason = null;
        reasons = Collections.emptyMap();
    }

    public DownloadedDependency(ResolvedArtifact artifact, boolean stripVersion, boolean separateByGroupId, String configuration, Path downloadPath) {
        this(artifact);
        processedArtifactName =  artifact.getFile().getName();
        // Strip version from destination
        if (stripVersion) {
            processedArtifactName = processedArtifactName.replaceAll("-" + artifact.getModuleVersion().getId().getVersion(), "");
        }

        setConfiguration(configuration);
        if (separateByGroupId) {
            setLocation(Paths.get(downloadPath.toString(), artifact.getModuleVersion().getId().getModule().getGroup(), processedArtifactName).toString());
        }else{
            setLocation(Paths.get(downloadPath.toString(), processedArtifactName).toString());
        }
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
                .thenComparing(DownloadedDependency::getClassifier, Comparator.nullsLast(Comparator.reverseOrder()))
                .compare(this, downloadedDependency);
    }

    public void setReason(String reason){
        this.reason = reason;
        try {
            this.reasons = Splitter.on(";").withKeyValueSeparator("=").split(reason);
        }catch(Exception e){
            // Reason cannot be parsed, resolve as empty.
            reasons = Collections.emptyMap();
        }
    }

}
