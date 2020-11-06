package org.devopsmindset.gradle;

import org.devopsmindset.gradle.model.DependencyManagerExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

class DependencyManagerPlugin implements Plugin<Project> {
    public void apply(Project project) {
        project.getConvention().add("dependenciesManagement", DependencyManagerExtension.class);
        project.getTasks().register("dependenciesDownload", DependencyManager.class,  w -> {
                    w.setGroup("dependency manager");
                }
        );
    }
}