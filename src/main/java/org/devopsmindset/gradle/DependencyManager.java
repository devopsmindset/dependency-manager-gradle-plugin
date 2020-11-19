package org.devopsmindset.gradle;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.devopsmindset.gradle.compress.CompressionUtils;
import org.devopsmindset.gradle.model.DependencyManagerExtension;
import org.devopsmindset.gradle.model.DownloadedDependency;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.*;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class DependencyManager extends DefaultTask {

    public static final String DEFAULT_DEPENDENCY_FILE = "processed.dependencies";
    final String DEFAULT_LOCATION = "dependency-manager";
    final String DEFAULT_DEPENDENCY_BASE_FILE = "base.dependencies";
    final String BASE_CONFIGURATION = "base";

    @TaskAction
    public void run() throws Exception {
        try {
            Boolean stripVersion = true;
            Boolean decompress = true;

            DependencyManagerExtension dpExtension = (DependencyManagerExtension) getProject().getConvention().getByName("dependenciesManagement");
            List<DownloadedDependency> downloadedDependencies = new ArrayList<DownloadedDependency>();

            // Delete output directory
            FileUtils.deleteDirectory(Paths.get(getProject().getBuildDir().toString(), DEFAULT_LOCATION).toFile());
            FileUtils.deleteQuietly(Paths.get(getProject().getBuildDir().toString(), DEFAULT_DEPENDENCY_FILE).toFile());
            FileUtils.deleteQuietly(Paths.get(getProject().getBuildDir().toString(), DEFAULT_DEPENDENCY_BASE_FILE).toFile());
            // Check delta dependency
            List<DownloadedDependency> baseDependencies = getBaseDependencies();

            int iteration = 0;
            Preconditions.checkNotNull(dpExtension.getConfigurations(), "No configuration found for plugin dependency-manager");
            for (String[] configurationArray : dpExtension.getConfigurations()) {
                if (dpExtension.getStripVersion() != null)
                    stripVersion = ((stripVersion = dpExtension.getStripVersion()[iteration]) != null) ? stripVersion : true;
                if (dpExtension.getDecompress() != null)
                    decompress = ((decompress = dpExtension.getDecompress()[iteration]) != null) ? decompress : true;

                for (String configuration : configurationArray) {
                    Configuration gradleConfiguration = getProject().getConfigurations().getByName(configuration);
                    ResolvedConfiguration resolvedConfiguration = gradleConfiguration.getResolvedConfiguration();

                    for (ResolvedArtifact artifact : resolvedConfiguration.getResolvedArtifacts()) {
                        DownloadedDependency downloadedDependency = copyDependency(artifact,
                                Paths.get(getProject().getBuildDir().toString(), DEFAULT_LOCATION, configuration),
                                stripVersion,
                                decompress,
                                configuration,
                                baseDependencies);
                        downloadedDependencies.add(downloadedDependency);
                    }
                }

                iteration++;
            }
            generateResolvedDependenciesFile(downloadedDependencies);
        } catch (Exception e) {
            getProject().getLogger().error("Error downloading dependencies " + e.toString());
            throw e;
        }
    }

    private List<DownloadedDependency> getBaseDependencies() throws IOException {
        List<DownloadedDependency> baseDependencies = null;
        Configuration baseConfiguration = getProject().getConfigurations().getByName(BASE_CONFIGURATION);
        if (baseConfiguration != null) {
            ResolvedConfiguration resolvedBaseConfiguration = baseConfiguration.getResolvedConfiguration();
            for (ResolvedArtifact artifact : resolvedBaseConfiguration.getResolvedArtifacts()) {
                if (artifact.getExtension().equals("dependencies")) {
                    Path baseLocation = Paths.get(getProject().getBuildDir().toString(), DEFAULT_DEPENDENCY_BASE_FILE);
                    Files.createDirectories(baseLocation.getParent());
                    Files.copy(artifact.getFile().toPath(), baseLocation, StandardCopyOption.REPLACE_EXISTING);
                    ObjectMapper mapper = new ObjectMapper();
                    baseDependencies = mapper.readValue(Files.readString(baseLocation), new TypeReference<List<DownloadedDependency>>() {
                    });
                    break;
                }
            }
        }
        return baseDependencies;
    }

    private DownloadedDependency copyDependency(ResolvedArtifact artifact, Path downloadPath, boolean stripVersion, boolean decompress, String configuration, List<DownloadedDependency> baseDependencies) throws Exception {
        getProject().getLogger().info("Dependency Manager - Downloading dependency:  {}", artifact.getModuleVersion().toString());

        DownloadedDependency downloadedDependency = new DownloadedDependency(artifact, stripVersion, decompress, isExtensionToDecompress(artifact.getExtension()), configuration, downloadPath);
        // Search for the reason in dependency list as it is not informed in resolved artifact
        final Dependency reason = getDependencyFromArtifact(artifact, getProject().getConfigurations().getByName(configuration).getIncoming().getDependencies());

        if (reason != null) {
            downloadedDependency.setReason(reason.getReason());
        }

        // If already in base dependency it will not be added / processed
        if (!downloadedDependency.findIn(baseDependencies)) {
            downloadedDependency.setDifferentFromBase(true);
            copyArtifact(artifact, decompress, Paths.get(downloadedDependency.getLocation()));
        }

        return downloadedDependency;
    }

    private void copyArtifact(ResolvedArtifact artifact, boolean decompress, Path dest) throws Exception {
        if (decompress && isExtensionToDecompress(artifact.getExtension())) {
            if (artifact.getExtension().equalsIgnoreCase("zip")) {
                CompressionUtils.unZipFile(artifact.getFile(), dest.toFile());
            } else {
                if (artifact.getExtension().equalsIgnoreCase("tar.gz") || artifact.getExtension().equalsIgnoreCase("tgz")) {
                    CompressionUtils.unTarFile(artifact.getFile(), dest.toFile());
                }
            }
        } else {
            Files.createDirectories(dest.getParent());
            Files.copy(artifact.getFile().toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Dependency getDependencyFromArtifact(ResolvedArtifact artifact, DependencySet dependencies) {
        Dependency foundDependency = null;
        for (Dependency dependency : dependencies) {
            if (dependency.getName().equals(artifact.getName())) {
                foundDependency = dependency;
                break;
            }
        }
        return foundDependency;
    }

    private void generateResolvedDependenciesFile(List<DownloadedDependency> downloadedDependencies) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.writerWithDefaultPrettyPrinter();
        mapper.findAndRegisterModules();
        File downloadedDependenciesFile = Paths.get(getProject().getBuildDir().toString(), DEFAULT_DEPENDENCY_FILE).toFile();
        Files.delete(downloadedDependenciesFile.toPath());
        mapper.writerWithDefaultPrettyPrinter().writeValue(downloadedDependenciesFile, downloadedDependencies);
    }

    private boolean isExtensionToDecompress(String extension) {
        return (extension.equalsIgnoreCase("zip")
                || extension.equalsIgnoreCase("tgz")
                || extension.equalsIgnoreCase("tar.gz"));
    }


}
