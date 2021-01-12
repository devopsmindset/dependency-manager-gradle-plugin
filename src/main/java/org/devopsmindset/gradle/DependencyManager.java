package org.devopsmindset.gradle;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class DependencyManager extends DefaultTask {

    public static final String DEFAULT_DEPENDENCY_FILE = "processed.dependencies";
    public static final String DEPENDENCIES = "dependencies";
    public static final String DECOMPRESS = "decompress";
    public static final String TARGET = "target";
    static final String DEFAULT_LOCATION = "dependency-manager";
    static final String DEFAULT_DEPENDENCY_BASE_FILE = "base.dependencies";
    static final String BASE_CONFIGURATION = "base";

    @TaskAction
    public void run() throws Exception {
        try {
            Boolean stripVersion = true;
            Boolean separateByGroupId = true;

            DependencyManagerExtension dpExtension = (DependencyManagerExtension) getProject().getConvention().getByName("dependenciesManagement");
            Preconditions.checkNotNull(dpExtension.getConfigurations(), "No configuration found for plugin dependency-manager");
            Preconditions.checkArgument(checkInitialArguments(dpExtension), "Invalid number of arguments");
            List<DownloadedDependency> downloadedDependencies = new ArrayList<>();

            // Delete output directory
            FileUtils.deleteDirectory(Paths.get(getProject().getBuildDir().toString(), DEFAULT_LOCATION).toFile());
            FileUtils.deleteQuietly(Paths.get(getProject().getBuildDir().toString(), DEFAULT_DEPENDENCY_FILE).toFile());
            FileUtils.deleteQuietly(Paths.get(getProject().getBuildDir().toString(), DEFAULT_DEPENDENCY_BASE_FILE).toFile());
            // Check delta dependency
            List<DownloadedDependency> baseDependencies = getBaseDependencies();

            int iteration = 0;
            for (String[] configurationArray : dpExtension.getConfigurations()) {
                if (dpExtension.getStripVersion() != null)
                    stripVersion = ((stripVersion = dpExtension.getStripVersion()[iteration]) != null) ? stripVersion : true;

                if (dpExtension.getSeparateByGroupId() != null)
                    separateByGroupId = ((separateByGroupId = dpExtension.getSeparateByGroupId()[iteration]) != null) ? separateByGroupId : true;

                getProject().getLogger().debug("stripVersion: {}", stripVersion);
                getProject().getLogger().debug("separateByGroupId: {}", separateByGroupId);
                for (String configuration : configurationArray) {
                    Configuration gradleConfiguration = getProject().getConfigurations().getByName(configuration);
                    ResolvedConfiguration resolvedConfiguration = gradleConfiguration.getResolvedConfiguration();

                    for (ResolvedArtifact artifact : resolvedConfiguration.getResolvedArtifacts()) {
                        DownloadedDependency downloadedDependency = copyDependency(artifact,
                                Paths.get(getProject().getBuildDir().toString(), DEFAULT_LOCATION, configuration),
                                stripVersion,
                                separateByGroupId,
                                configuration,
                                baseDependencies);
                        downloadedDependencies.add(downloadedDependency);
                    }
                }

                iteration++;
            }
            generateResolvedDependenciesFile(downloadedDependencies);
        } catch (Exception e) {
            getProject().getLogger().error("Error downloading dependencies: " + e.toString());
            throw e;
        }
    }

    private boolean checkInitialArguments(DependencyManagerExtension dpExtension) {
        return dpExtension.getConfigurations().length == dpExtension.getStripVersion().length &&
                dpExtension.getConfigurations().length == dpExtension.getSeparateByGroupId().length;
    }

    private List<DownloadedDependency> getBaseDependencies() throws IOException {
        List<DownloadedDependency> baseDependencies = null;
        try {
            Configuration baseConfiguration;
            baseConfiguration = getProject().getConfigurations().getByName(BASE_CONFIGURATION);
            ResolvedConfiguration resolvedBaseConfiguration = baseConfiguration.getResolvedConfiguration();
            for (ResolvedArtifact artifact : resolvedBaseConfiguration.getResolvedArtifacts()) {
                if (artifact.getExtension().equals(DEPENDENCIES)) {
                    Path baseLocation = Paths.get(getProject().getBuildDir().toString(), DEFAULT_DEPENDENCY_BASE_FILE);
                    Files.createDirectories(baseLocation.getParent());
                    Files.copy(artifact.getFile().toPath(), baseLocation, REPLACE_EXISTING);
                    ObjectMapper mapper = new ObjectMapper();
                    String readBaseLocation = new String(Files.readAllBytes(baseLocation));
                    baseDependencies = mapper.readValue(readBaseLocation, new TypeReference<List<DownloadedDependency>>() {
                    });
                    break;
                }
            }
        } catch (UnknownConfigurationException e) {
            getProject().getLogger().debug("Base configuration not found");
        }
        return baseDependencies;
    }

    private DownloadedDependency copyDependency(ResolvedArtifact artifact,
                                                Path downloadPath,
                                                boolean stripVersion,
                                                boolean separateByGroupId,
                                                String configuration,
                                                List<DownloadedDependency> baseDependencies) throws Exception {
        getProject().getLogger().info("=> Dependency Manager -> Downloading dependency:  {}", artifact.getModuleVersion().toString());

        DownloadedDependency downloadedDependency = new DownloadedDependency(artifact, stripVersion, separateByGroupId, configuration, downloadPath);
        // Search for the reason in dependency list as it is not informed in resolved artifact
        final Dependency dependencyFromArtifact = getDependencyFromArtifact(artifact, getProject().getConfigurations().getByName(configuration).getIncoming().getDependencies());

        if (dependencyFromArtifact != null) {
            downloadedDependency.setReason(dependencyFromArtifact.getReason());
            if (downloadedDependency.getReasons().containsKey(TARGET)) {
                final String location = Paths.get(getProject().getBuildDir().toString(), DEFAULT_LOCATION, TARGET, downloadedDependency.getReasons().get(TARGET), downloadedDependency.getProcessedArtifactName()).toString();
                getProject().getLogger().debug("moving to custom location: {}", location);
                downloadedDependency.setLocation(location);
                if (downloadedDependency.getReasons().containsKey(DECOMPRESS)) {
                    downloadedDependency.setLocation(Paths.get(downloadedDependency.getLocation()).getParent().toString());
                }
            }
        }

        // If already in base dependency it will not be added / processed
        if (baseDependencies == null || !downloadedDependency.findIn(baseDependencies)) {
            downloadedDependency.setDifferentFromBase(true);
            copyArtifact(artifact, Paths.get(downloadedDependency.getLocation()), downloadedDependency.getReasons());
        }

        return downloadedDependency;
    }

    private void copyArtifact(ResolvedArtifact artifact, Path dest, Map<String, String> reasons) throws Exception {
        if (isExtensionToDecompress(artifact.getExtension()) && reasons.containsKey(DECOMPRESS)) {
            Files.createDirectories(dest);
            if (!reasons.get(DECOMPRESS).equals(".")) {
                Path tempDirWithPrefix = Files.createTempDirectory("depmanager");
                try {
                    CompressionUtils.extract(artifact.getFile(), tempDirWithPrefix.toFile());

                    File origin = new File(Paths.get(tempDirWithPrefix.toString(), reasons.get(DECOMPRESS)).toString());
                    Path destination = dest;
                    if (origin.isFile()) {
                        destination = Paths.get(dest.toString(), origin.getName());
                        Files.copy(origin.toPath(), destination, REPLACE_EXISTING);
                    } else {
                        FileUtils.copyDirectory(origin, destination.toFile());
                    }
                } finally {
                    FileUtils.deleteQuietly(tempDirWithPrefix.toFile());
                }
            } else {
                CompressionUtils.extract(artifact.getFile(), dest.toFile());
            }
        } else {
            Files.createDirectories(dest.getParent());
            Files.copy(artifact.getFile().toPath(), dest, REPLACE_EXISTING);
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

    private void generateResolvedDependenciesFile(List<DownloadedDependency> downloadedDependencies) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(NON_NULL);
        mapper.writerWithDefaultPrettyPrinter();
        mapper.findAndRegisterModules();
        File downloadedDependenciesFile = Paths.get(getProject().getBuildDir().toString(), DEFAULT_DEPENDENCY_FILE).toFile();
        try {
            if (Files.exists(downloadedDependenciesFile.toPath())) {
                Files.delete(downloadedDependenciesFile.toPath());
            }
            //TODO refine downloaded dependencies to contain only original ones
            mapper.writerWithDefaultPrettyPrinter().writeValue(downloadedDependenciesFile, downloadedDependencies);
        } catch (IOException e) {
            getProject().getLogger().error(e.getMessage(), e);
        }
    }

    private boolean isExtensionToDecompress(String extension) {
        return (extension.equalsIgnoreCase("zip")
                || extension.equalsIgnoreCase("tgz")
                || extension.equalsIgnoreCase("tar.gz"));
    }

}
