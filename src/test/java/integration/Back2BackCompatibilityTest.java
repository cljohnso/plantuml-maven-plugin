package integration;

/*-
 * #%L
 * Maven PlantUML plugin
 * %%
 * Copyright (C) 2011 - 2019 Julien Eluard
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecution;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static io.takari.maven.testing.TestResources.assertFilesPresent;

// http://takari.io/book/70-testing.html
@RunWith(MavenJUnitTestRunner.class)
// Maven 3.1.1 works, but is so old it wants to download from Maven Central via HTTP instead of HTTPS. But HTTP download
// has been deactivated by Sonatype a while ago. If someone works with a proxy or behind a Nexus + firewall in an
// enterprise setting on an old Maven version, this can still work. Maven 3.1.1 can use this plugin, it is simply a
// download problem.
@MavenVersions({ /*"3.1.1",*/ "3.2.5", "3.3.9", "3.5.4", "3.6.3", "3.8.1" })
public class Back2BackCompatibilityTest {

    @Rule
    public final TestResources resources = new TestResources("src/test/resources/integration", "target/test-integration");

    public final MavenRuntime maven;

    public Back2BackCompatibilityTest(MavenRuntime.MavenRuntimeBuilder builder) throws Exception {
        maven = builder.withCliOptions("-B", "-U").build();
    }

    @Test
    public void checkHuluvu424242Mojo() throws Exception {
        // This plugin works with both the Smetana engine and with a local GraphViz installation.
        checkMojo("funthomas424242", true);
    }

    @Test
    public void checkJmdesprezMojo() throws Exception {
        // ATTENTION: This legacy plugin needs an old API and a local GraphViz installation.
        // If it was not for this test, no local GraphViz installation would be necessary for the whole test suite.
        checkMojo("jmdesprez", true);
    }

    @Test
    public void checkJeluardMojo() throws Exception {
        // This plugin works with both the Smetana engine and with a local GraphViz installation.
        checkMojo("jeluard", false);
    }

    private void checkMojo(String githubID, boolean canTruncatePattern) throws Exception {
        File baseDir = resources.getBasedir("truncate-project");
        MavenExecution mavenExecution = maven.forProject(baseDir).withCliOption("-P" + githubID);
        String pluginMavenCoordinates = "com.github." + githubID + ":plantuml-maven-plugin:generate";
        MavenExecutionResult result = mavenExecution.execute("clean", pluginMavenCoordinates);
        result.assertErrorFreeLog();

        // 'jdot' binary not found
        result.assertNoLogText("java.io.IOException");

        // Problems interacting with Smetana API
        result.assertNoLogText("java.lang.InvocationTargetException");
        result.assertNoLogText("java.lang.UnsupportedOperationException");
        result.assertNoLogText("java.lang.ClassFormatError");

        String subDir = "target/plantuml/" + (canTruncatePattern ? "" : "src/main/plantuml/");
        assertFilesPresent(baseDir, subDir + "AblaufManuelleGenerierung.png");
        assertFilesPresent(baseDir, subDir + "QueueStatechart.png");
    }
}
