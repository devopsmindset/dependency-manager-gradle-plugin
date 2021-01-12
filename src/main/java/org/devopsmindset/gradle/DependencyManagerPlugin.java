package org.devopsmindset.gradle;

import org.devopsmindset.gradle.model.DependencyManagerExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

import javax.inject.Inject;
import java.io.File;

class DependencyManagerPlugin implements Plugin<Project> {

    public static final String DEPENDENCIES_DOWNLOAD = "dependenciesDownload";
    private final ArtifactHandler artifactHandler;

    @Inject
    public DependencyManagerPlugin(ArtifactHandler artifactHandler) {
        this.artifactHandler = artifactHandler;
    }

    public void apply(Project project) {
        project.getPluginManager().apply(MavenPublishPlugin.class);
        project.getConvention().add("dependenciesManagement", DependencyManagerExtension.class);
        project.getTasks().register(DEPENDENCIES_DOWNLOAD, DependencyManager.class, w -> w.setGroup("dependency manager"));
        project.getTasks().getByName("publishToMavenLocal").dependsOn(DEPENDENCIES_DOWNLOAD);
        project.getTasks().getByName("publish").dependsOn(DEPENDENCIES_DOWNLOAD);

        project.getConfigurations().create("dependencyManager");

        PublishingExtension publishingExtension = (PublishingExtension) project.getConvention().getExtensionsAsDynamicObject().getProperty("publishing");

        publishingExtension.getPublications().create("mavenJava", MavenPublication.class, publication -> addArchive(new File(project.getBuildDir(), DependencyManager.DEFAULT_DEPENDENCY_FILE), publication, ""));

    }

    void addArchive(File newFile, MavenPublication publication, String classifier) {
        PublishArtifact artifact = artifactHandler.add("dependencyManager", newFile, w -> w.setClassifier(classifier));
        publication.artifact(artifact);

    }
}