package org.devopsmindset.gradle;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.FileUtils;
import org.devopsmindset.gradle.model.DependencyManagerExtension;
import org.devopsmindset.gradle.compress.CompressionUtils;
import org.devopsmindset.gradle.model.DownloadedDependency;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.*;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class DependencyManager extends DefaultTask {

    final String DEFAULT_LOCATION = "dependency-manager";
    final String DEFAULT_DEPENDENCY_FILE = "downloaded-dependencies.json";

    @TaskAction
    public void run() throws Exception {
        try {
            Boolean stripVersion;
            Boolean decompress;

            DependencyManagerExtension dpExtension = (DependencyManagerExtension) getProject().getConvention().getByName("dependenciesManagement");
            List<DownloadedDependency> downloadedDependencies = new ArrayList<DownloadedDependency>();

            int iteration = 0;
            // Delete output directory
            FileUtils.deleteDirectory(Paths.get(getProject().getBuildDir().toString(),DEFAULT_LOCATION).toFile());

            for (String[] configurationArray : dpExtension.getConfigurations()) {
                stripVersion = ((stripVersion = dpExtension.getStripVersion()[iteration]) != null) ? stripVersion : true;
                decompress = ((decompress = dpExtension.getDecompress()[iteration]) != null) ? decompress : true;

                for (String configuration : configurationArray) {
                    Configuration gradleConfiguration = getProject().getConfigurations().getByName(configuration);
                    ResolvedConfiguration resolvedConfiguration = gradleConfiguration.getResolvedConfiguration();

                    for (ResolvedArtifact artifact : resolvedConfiguration.getResolvedArtifacts()) {
                        DownloadedDependency downloadedDependency = copyDependency(artifact,
                                Paths.get(getProject().getBuildDir().toString(),DEFAULT_LOCATION, configuration),
                                stripVersion,
                                decompress,
                                configuration);

                        downloadedDependencies.add(downloadedDependency);
                    }
                }

                iteration++;
            }

            generateResolvedDependenciesFile(downloadedDependencies);

        }catch(Exception e){
            getProject().getLogger().error("Error downloading dependencies " + e.toString());
            throw e;
        }
    }

    private DownloadedDependency copyDependency(ResolvedArtifact artifact, Path downloadPath, boolean stripVersion, boolean decompress, String configuration) throws Exception {
        getProject().getLogger().info("Dependency Manager - Downloading dependency " + artifact.getModuleVersion().toString());

        DownloadedDependency downloadedDependency = new DownloadedDependency(artifact, stripVersion, decompress, isExtensionToDecompress(artifact.getExtension()), configuration, downloadPath);
        // Search for the reason in dependency list as it is not informed in resolved artifact
        downloadedDependency.setReason(getDependencyFromArtifact(artifact, getProject().getConfigurations().getByName(configuration).getIncoming().getDependencies()).getReason());

        copyArtifact(artifact, decompress, Paths.get(downloadedDependency.getLocation()));

        return downloadedDependency;
    }

    private void copyArtifact(ResolvedArtifact artifact, boolean decompress, Path dest) throws IOException, Exception{
        if (decompress && isExtensionToDecompress(artifact.getExtension())){
            if (artifact.getExtension().toLowerCase().equals("zip")){
                CompressionUtils.unZipFile(artifact.getFile(), dest.toFile());
            }else{
                if (artifact.getExtension().toLowerCase().equals("tar.gz") || artifact.getExtension().toLowerCase().equals("tgz")){
                    CompressionUtils.unTarFile(artifact.getFile(), dest.toFile());
                }
            }
        }else {
            Files.createDirectories(dest.getParent());
            Files.copy(artifact.getFile().toPath() , dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Dependency getDependencyFromArtifact(ResolvedArtifact artifact, DependencySet dependencies) {
        Dependency foundDependency = null;
        for (Dependency dependency : dependencies) {
            if (dependency.getName().equals(artifact.getName())){
                foundDependency = dependency;
                break;
            }
        }
        return foundDependency;
    }

    private void generateResolvedDependenciesFile(List<DownloadedDependency> downloadedDependencies) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.writerWithDefaultPrettyPrinter();
        mapper.findAndRegisterModules();
        File downloadedDependenciesFile = Paths.get(getProject().getBuildDir().toString(), DEFAULT_DEPENDENCY_FILE).toFile();
        downloadedDependenciesFile.delete();
        mapper.writerWithDefaultPrettyPrinter().writeValue(downloadedDependenciesFile, downloadedDependencies);
    }

    private boolean isExtensionToDecompress(String extension){
        return (extension.toLowerCase().equals("zip") || extension.toLowerCase().equals("tgz") || extension.toLowerCase().equals("tar.gz"));
    }






}
