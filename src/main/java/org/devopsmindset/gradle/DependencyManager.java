package org.devopsmindset.gradle;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.devopsmindset.gradle.model.DependencyManagerExtension;
import org.devopsmindset.gradle.compress.CompressionUtils;
import org.devopsmindset.gradle.model.DownloadedDependency;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class DependencyManager extends DefaultTask {

    @TaskAction
    public void run() throws Exception {
        try {
            DependencyManagerExtension dpExtension = (DependencyManagerExtension) getProject().getConvention().getByName("dependenciesManagement");
            List<DownloadedDependency> downloadedDependencies = new ArrayList<DownloadedDependency>();
            if ((dpExtension.getSeparateByGroupId().length != dpExtension.getConfigurations().length) || (dpExtension.getStripVersion().length != dpExtension.getConfigurations().length)) {
                throw new Exception("[DependencyManager] downloadPath, configurations and separateByGroupId does not have the same length");
            }
            int iteration = 0;
            //FileUtils.deleteDirectory(Paths.get(getProject().getBuildDir().toString(),"dependency-manager");
            for (String[] configurationArray : dpExtension.getConfigurations()) {
                for (String configuration : configurationArray) {
                    Configuration gradleConfiguration = getProject().getConfigurations().getByName(configuration);
                    ResolvedConfiguration resolvedConfiguration = gradleConfiguration.getResolvedConfiguration();

                    for (ResolvedArtifact artifact : resolvedConfiguration.getResolvedArtifacts()) {

                        DownloadedDependency downloadedDependency = copyDependency(artifact, Paths.get(getProject().getBuildDir().toString(),"dependency-manager", configuration), dpExtension.getSeparateByGroupId()[iteration], dpExtension.getStripVersion()[iteration], dpExtension.getDecompress()[iteration]);
                        downloadedDependency.setConfiguration(configuration);
                        downloadedDependencies.add(downloadedDependency);
                    }
                }
                iteration++;
            }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.writerWithDefaultPrettyPrinter();
            mapper.findAndRegisterModules();
            //mapper = new ObjectMapper(new YAMLFactory().disable(Feature.WRITE_DOC_START_MARKER));
            File downloadedDependenciesFile = Paths.get(getProject().getBuildDir().toString(), "downloaded-dependencies.json").toFile();
            downloadedDependenciesFile.delete();
            mapper.writerWithDefaultPrettyPrinter().writeValue(downloadedDependenciesFile, downloadedDependencies);
        }catch(Exception e){
            getProject().getLogger().error("Error downloading dependencies " + e.toString());
            throw e;
        }
    }

    private DownloadedDependency copyDependency(ResolvedArtifact artifact, Path downloadPath, boolean separateByGroupId, boolean stripVersion, boolean decompress) throws Exception {
        getProject().getLogger().info("Dependency Manager - Downloading dependency " + artifact.getModuleVersion().toString());
        File fileArtifact = artifact.getFile();
        DownloadedDependency downloadedDependency = new DownloadedDependency();
        downloadedDependency.setArtifact(artifact.getModuleVersion().getId().getName());
        downloadedDependency.setClassifer(artifact.getClassifier());
        downloadedDependency.setExtension(artifact.getExtension().toLowerCase());
        downloadedDependency.setGroup(artifact.getModuleVersion().getId().getGroup());
        downloadedDependency.setVersion(artifact.getModuleVersion().getId().getVersion());

        String artifactDestination = fileArtifact.getName();
        if (stripVersion) {
            artifactDestination = artifactDestination.replaceAll("-" + artifact.getModuleVersion().getId().getVersion(), "");
        }

        Path dest = downloadPath;
        if (separateByGroupId){
            dest = Paths.get(dest.toString(), artifact.getModuleVersion().getId().getModule().getGroup());
        }

        if (decompress && isExtensionToDecompress(artifact.getExtension())){
            downloadedDependency.setDecompressed(true);
            dest = Paths.get(dest.toString(), artifactDestination);
            if (artifact.getExtension().toLowerCase().equals("zip")){
                CompressionUtils.unZipFile(fileArtifact, dest.toFile());
            }else{
                if (artifact.getExtension().toLowerCase().equals("tar.gz") || artifact.getExtension().toLowerCase().equals("tgz")){
                    CompressionUtils.unTarFile(fileArtifact, dest.toFile());
                }
            }
        }else {
            dest = Paths.get(dest.toString(), artifactDestination);
            Files.createDirectories(dest.getParent());
            Files.copy(fileArtifact.toPath() , dest, StandardCopyOption.REPLACE_EXISTING);
        }
        downloadedDependency.setLocation(dest.toString());
        return downloadedDependency;
    }

    private boolean isExtensionToDecompress(String extension){
        return (extension.toLowerCase().equals("zip") || extension.toLowerCase().equals("tgz") || extension.toLowerCase().equals("tar.gz"));
    }






}
