package org.devopsmindset.gradle;

import java.io.File;
import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;


public class DependencyManagerTest {

    private static String testProjectPath = "test-project";

    @org.junit.jupiter.api.Test
    void run() throws IOException {
        File projectDir = new File(testProjectPath);
        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("dependenciesDownload", "--refresh-dependencies", "--stacktrace", "--debug")
                .withDebug(true)
                .build();
    }
}
