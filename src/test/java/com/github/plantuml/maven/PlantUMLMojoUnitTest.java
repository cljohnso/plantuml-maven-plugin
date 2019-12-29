package com.github.plantuml.maven;

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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assumptions.assumingThat;


// https://maven.apache.org/plugin-developers/plugin-testing.html
// https://cwiki.apache.org/confluence/display/MAVEN/Creating+a+Maven+Integration+Test
public class PlantUMLMojoUnitTest extends AbstractMojoTestCase {


    /**
     * @throws Exception if any
     */
    @Test
    @DisplayName("Check default values of mojo parameters")
    public void testSomething()
            throws Exception {
        final File pom = Paths.get("src/test/resources/unit/project-to-test/pom.xml").toFile();
        assumingThat(pom==null || !pom.exists(),()->{
            fail("test project pom not found");
        });
        final PlantUMLMojo myMojo = (PlantUMLMojo) lookupMojo("generate", pom);
        assertNotNull(myMojo);
        assertNotNull(myMojo.outputDirectory);

        // check outputDirectory
        final Path plantumlTargetDir = Paths.get("target/plantuml");
        assertEquals(plantumlTargetDir.toAbsolutePath().toFile(), myMojo.outputDirectory);

        // execute Mojo
        myMojo.execute();
    }

    /**
     * Do not need the MojoRule.
     */
    @WithoutMojo
    @Test
    public void testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn() {

    }

}
