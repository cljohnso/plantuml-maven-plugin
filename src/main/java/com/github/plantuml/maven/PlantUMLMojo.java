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

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.GeneratedImage;
import net.sourceforge.plantuml.Option;
import net.sourceforge.plantuml.OptionFlags;
import net.sourceforge.plantuml.SourceFileReader;
import net.sourceforge.plantuml.preproc.Defines;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

@Mojo(defaultPhase = LifecyclePhase.GENERATE_RESOURCES, name = "generate")
public final class PlantUMLMojo extends AbstractMojo {

    protected final Option option = new Option();

    /**
     * Whether to skip images generation altogether.
     * This is useful for configuring PlantUML in a parent POM while letting sub-modules to 
     * disable generation if they do not feature any diagrams.
     * <p>
     * If this is set to true then generation is skipped.
     */
    @Parameter(property = "skip", defaultValue = "false")
    protected boolean skipGeneration;
    
    /**
     * Truncate the ouput folder.
     *
     * @since 1.2
     */
    @Parameter(property = "truncatePattern")
    protected String truncatePattern;

    /**
     * Fileset to search plantuml diagrams in.
     *
     * @since 7232
     */
    @Parameter(property = "plantuml.sourceFiles", required = true)
    protected FileSet sourceFiles;

    /**
     * Directory where generated images are generated.
     */
    @Parameter(property = "plantuml.outputDirectory", defaultValue = "${basedir}/target/plantuml", required = true)
    protected File outputDirectory;

    /**
     * Whether or not to generate images in same directory as the source file.
     * This is useful for using PlantUML diagrams in Javadoc, as described here:
     * <a href="http://plantuml.sourceforge.net/javadoc.html">http://plantuml.
     * sourceforge.net/javadoc.html</a>.
     * <p>
     * If this is set to true then outputDirectory is ignored.
     */
    @Parameter(property = "plantuml.outputInSourceDirectory", defaultValue = "false")
    protected boolean outputInSourceDirectory;

    /**
     * Charset used during generation.
     */
    @Parameter(property = "plantuml.charset")
    protected String charset;

    /**
     * External configuration file location.
     */
    @Parameter(property = "plantuml.config")
    protected String config;

    /**
     * Specify output format. Supported values: xmi, xmi:argo, xmi:start, eps,
     * pdf, eps:txt, svg, png, dot, txt and utxt.
     */
    @Parameter(property = "plantuml.format")
    protected String format;

    /**
     * Fully qualified path to Graphviz home directory.
     */
    @Parameter(property = "plantuml.graphvizDot")
    protected String graphvizDot;

    /**
     * Wether or not to output details during generation.
     */
    @Parameter(property = "plantuml.verbose", defaultValue = "false")
    protected boolean verbose;

    /**
     * Specify to include metadata in the output files.
     *
     * @since 1.3
     */
    @Parameter(property = "plantuml.withMetadata")
    protected boolean withMetadata = false;

    /**
     * Specify to overwrite any output file, also if the target file is newer as the input file.
     *
     * @since 1.3
     */
    @Parameter(property = "plantuml.overwrite")
    protected boolean overwrite = false;

    protected final void setFormat(final String format) {
        if ("xmi".equalsIgnoreCase(format)) {
            this.option.setFileFormatOption(new FileFormatOption(FileFormat.XMI_STANDARD));
        } else if ("xmi:argo".equalsIgnoreCase(format)) {
            this.option.setFileFormatOption(new FileFormatOption(FileFormat.XMI_ARGO));
        } else if ("xmi:start".equalsIgnoreCase(format)) {
            this.option.setFileFormatOption(new FileFormatOption(FileFormat.XMI_STAR));
        } else if ("eps".equalsIgnoreCase(format)) {
            this.option.setFileFormatOption(new FileFormatOption(FileFormat.EPS));
        } else if ("eps:txt".equalsIgnoreCase(format)) {
            this.option.setFileFormatOption(new FileFormatOption(FileFormat.EPS_TEXT));
        } else if ("svg".equalsIgnoreCase(format)) {
            this.option.setFileFormatOption(new FileFormatOption(FileFormat.SVG));
        } else if ("txt".equalsIgnoreCase(format)) {
            this.option.setFileFormatOption(new FileFormatOption(FileFormat.ATXT));
        } else if ("utxt".equalsIgnoreCase(format)) {
            this.option.setFileFormatOption(new FileFormatOption(FileFormat.UTXT));
        } else if ("png".equalsIgnoreCase(format)) {
            this.option.setFileFormatOption(new FileFormatOption(FileFormat.PNG));
        } else if ("pdf".equalsIgnoreCase(format)) {
            this.option.setFileFormatOption(new FileFormatOption(FileFormat.PDF));
        } else {
            throw new IllegalArgumentException("Unrecognized format <" + format + ">");
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        if(this.skipGeneration){
            getLog().warn("Skipping plantuml generation due to configuration");
            return;
        }
        
        // early exit if sourceFiles directory is not available
        final String invalidSourceFilesDirectoryWarnMsg = this.sourceFiles.getDirectory() + " is not a valid path";
        if (null == this.sourceFiles.getDirectory() || this.sourceFiles.getDirectory().isEmpty()) {
            getLog().warn(invalidSourceFilesDirectoryWarnMsg);
            return;
        }
        File baseDir = null;
        try {
            baseDir = new File(this.sourceFiles.getDirectory());
        } catch (Exception e) {
            getLog().debug(invalidSourceFilesDirectoryWarnMsg, e);
        }
        if (null == baseDir || !baseDir.exists() || !baseDir.isDirectory()) {
            getLog().warn(invalidSourceFilesDirectoryWarnMsg);
            return;
        }
        if (!this.outputInSourceDirectory) {
            if (!this.outputDirectory.exists()) {
                // If output directoy does not exist yet create it.
                this.outputDirectory.mkdirs();
            }
            if (!this.outputDirectory.isDirectory()) {
                throw new IllegalArgumentException("<" + this.outputDirectory + "> is not a valid directory.");
            }
        }

        try {
            if (!this.outputInSourceDirectory) {
                this.option.setOutputDir(this.outputDirectory);
            }
            if (this.charset != null) {
                this.option.setCharset(this.charset);
            }
            if (this.config != null) {
                this.option.initConfig(this.config);
            }
            if (this.graphvizDot != null) {
                OptionFlags.getInstance().setDotExecutable(this.graphvizDot);
            }
            if (this.format != null) {
                setFormat(this.format);
            }
            if (this.verbose) {
                OptionFlags.getInstance().setVerbose(true);
            }

            final List<File> files = FileUtils.getFiles(
                    baseDir,
                    getCommaSeparatedList(this.sourceFiles.getIncludes()),
                    getCommaSeparatedList(this.sourceFiles.getExcludes()));
            for (final File file : files) {
                getLog().info("Processing file <" + file + ">");

                if (this.outputInSourceDirectory) {
                    this.option.setOutputDir(file.getParentFile());
                } else {

                    Path fileBasePath = file.getParentFile().toPath();

                    if (truncatePattern != null && truncatePattern.length() > 0) {
                        String[] truncateTokens = truncatePattern.split("/");
                        int truncateIndex = 0;
                        int endIndex = 0;
                        for (final Path path : fileBasePath) {
                            endIndex++;
                            String currentTruncateToken = truncateTokens[truncateIndex];

                            if ("*".equals(currentTruncateToken) || path.getFileName().toString().equals(currentTruncateToken)) {
                                truncateIndex++;
                                if (truncateIndex == truncateTokens.length) {
                                    // All tokens are found
                                    Path root = fileBasePath.getRoot();
                                    Path subpath = fileBasePath.subpath(0, endIndex);
                                    if (root != null) {
                                        fileBasePath = root.resolve(subpath);
                                    } else {
                                        // this is very unlikely because the
                                        // File has an absolute path
                                        fileBasePath = subpath;
                                    }
                                    break;
                                }
                            } else {
                                // doesn't match, reset the pattern
                                truncateIndex = 0;
                            }
                        }
                    }

                    Path relativize = fileBasePath.relativize(file.toPath().getParent());
                    File outputDir = outputDirectory.toPath().resolve(relativize).toFile();
                    this.option.setOutputDir(outputDir);
                }

                final SourceFileReader sourceFileReader = new SourceFileReader( Defines.createEmpty(), file, this.option.getOutputDir(), this.option.getConfig(), this.option.getCharset(), this.option.getFileFormatOption());
                for (final GeneratedImage image : sourceFileReader.getGeneratedImages()) {
                    getLog().debug(image + " " + image.getDescription());
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Exception during plantuml process", e);
        }
    }

    protected String getCommaSeparatedList(final List<String> list) {
        final StringBuilder builder = new StringBuilder();
        final Iterator it = list.iterator();
        while (it.hasNext()) {
            final Object object = it.next();
            builder.append(object.toString());
            if (it.hasNext()) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    protected FileFormatOption getFileFormatOption() {
        final FileFormatOption formatOptions = new FileFormatOption(this.option.getFileFormatOption().getFileFormat(), this.withMetadata);
        if (formatOptions.isWithMetadata() != withMetadata) {
            // Workarround to error in plantUML where the withMetadata flag is not correctly applied.
            return new FileFormatOption(this.option.getFileFormatOption().getFileFormat());
        }
        return formatOptions;
    }

//    protected void handleTruncatePattern(final List<File> files) throws IOException {
//        for (final File file : files) {
//            getLog().info("Processing file <" + file + ">");
//
//            if (this.outputInSourceDirectory) {
//                this.option.setOutputDir(file.getParentFile());
//            } else {
//
//                final Path fileBasePath = file.getParentFile().toPath();
//
//                if (truncatePattern != null && truncatePattern.length() > 0) {
//                    final String[] truncateTokens = truncatePattern.split("/");
//                    int truncateIndex = 0;
//                    int endIndex = 0;
//                    for (final Path path : fileBasePath) {
//                        endIndex++;
//                        String currentTruncateToken = truncateTokens[truncateIndex];
//
//                        if ("*".equals(currentTruncateToken) || path.getFileName().toString().equals(currentTruncateToken)) {
//                            truncateIndex++;
//                            if (truncateIndex == truncateTokens.length) {
//                                // All tokens are found
//                                Path root = fileBasePath.getRoot();
//                                Path subpath = fileBasePath.subpath(0, endIndex);
//                                if (root != null) {
//                                    fileBasePath = root.resolve(subpath);
//                                } else {
//                                    // this is very unlikely because the
//                                    // File has an absolute path
//                                    fileBasePath = subpath;
//                                }
//                                break;
//                            }
//                        } else {
//                            // doesn't match, reset the pattern
//                            truncateIndex = 0;
//                        }
//                    }
//                }
//
//                Path relativize = fileBasePath.relativize(file.toPath().getParent());
//                File outputDir = outputDirectory.toPath().resolve(relativize).toFile();
//                this.option.setOutputDir(outputDir);
//            }
//
//            final SourceFileReader sourceFileReader = new SourceFileReader(new Defines(), file, this.option.getOutputDir(), this.option.getConfig(), this.option.getCharset(), this.option.getFileFormatOption());
//            for (final GeneratedImage image : sourceFileReader.getGeneratedImages()) {
//                getLog().debug(image + " " + image.getDescription());
//            }
//        }
//    }

}
