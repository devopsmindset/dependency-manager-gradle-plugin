package org.devopsmindset.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


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

        assertNotNull(result);
        assertEquals(result.getTasks().get(0).getOutcome(), TaskOutcome.SUCCESS);
    }
}
