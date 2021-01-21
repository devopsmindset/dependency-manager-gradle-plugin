package org.devopsmindset.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.*;

import java.io.File;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DependencyManagerTest {

    private static final String testProjectPath = "test-project";

    @Test
    @Tag("internal")
    void run() {
        File projectDir = new File(testProjectPath);
        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("dependenciesDownload", "--refresh-dependencies", "--stacktrace", "--debug")
                .withDebug(true)
                .build();

        assertNotNull(result);
        assertEquals(SUCCESS, result.getTasks().get(0).getOutcome());
    }

    @Test
    @DisplayName("third party vendors integration")
    void third_party_vendors() {
        File projectDir = new File("test-third-party-vendors");
        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("dependenciesDownload", "--refresh-dependencies", "--stacktrace", "--debug")
                .withDebug(true)
                .build();

        assertNotNull(result);
        assertEquals(SUCCESS, result.getTasks().get(0).getOutcome());
    }
}
