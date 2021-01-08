package org.devopsmindset.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;


class DependencyManagerTest {

    private static final String testProjectPath = "test-project";

    @Test
    void run() {
        File projectDir = new File(testProjectPath);
        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("dependenciesDownload", "--refresh-dependencies", "--stacktrace", "--debug")
                .withDebug(true)
                .build();

        Assertions.assertNotNull(result);
    }
}
