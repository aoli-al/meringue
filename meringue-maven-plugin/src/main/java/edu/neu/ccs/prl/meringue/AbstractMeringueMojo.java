package edu.neu.ccs.prl.meringue;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.surefire.SurefireHelper;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.booter.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

abstract class AbstractMeringueMojo extends AbstractMojo {
    /**
     * Current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    /**
     * Directory to which output files should be written.
     */
    @Parameter(property = "meringue.outputDir", defaultValue = "${project.build.directory}/meringue")
    private File outputDir;
    /**
     * Fully-qualified name of the test class.
     *
     * @see Class#forName(String className)
     */
    @Parameter(property = "meringue.testClass", required = true)
    private String testClass;
    /**
     * Name of the test method.
     */
    @Parameter(property = "meringue.testMethod", required = true)
    private String testMethod;
    /**
     * Java executable that should be used. If not specified, the executable used to run Maven will be used.
     */
    @Parameter(property = "meringue.javaExec")
    private File javaExec = FileUtil.javaHomeToJavaExec(new File(System.getProperty("java.home")));
    /**
     * Fully-qualified name of the fuzzing framework that should be used.
     */
    @Parameter(property = "meringue.framework", readonly = true, required = true)
    private String framework;
    /**
     * Arguments used to configure the fuzzing framework.
     */
    @Parameter(readonly = true)
    private Properties frameworkArguments = new Properties();
    /**
     * Java command line options that should be used for test JVMs.
     */
    @Parameter(property = "meringue.javaOptions")
    private List<String> javaOptions = new ArrayList<>();
    /**
     * Textual representation of the maximum amount of time to execute the fuzzing campaign in the ISO-8601 duration
     * format. The default value is one day.
     * <p>
     * See {@link java.time.Duration#parse(CharSequence)}.
     */
    @Parameter(property = "meringue.duration", defaultValue = "P1D")
    private String duration;

    Duration getDuration() {
        return Duration.parse(duration);
    }

    CampaignConfiguration createConfiguration() throws MojoExecutionException {
        return new CampaignConfiguration(testClass, testMethod, getDuration(), getCampaignDirectory(), javaOptions,
                                         createTestJar(), javaExec);
    }

    Set<File> getTestClassPathElements() throws MojoExecutionException {
        try {
            return project.getTestClasspathElements().stream().map(File::new).collect(Collectors.toSet());
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Required test dependency was not resolved", e);
        }
    }

    File getLibraryDirectory() {
        return new File(outputDir, "lib");
    }

    void initialize() throws MojoExecutionException {
        validateJavaExec();
        try {
            FileUtil.ensureDirectory(outputDir);
            FileUtil.ensureDirectory(getLibraryDirectory());
            FileUtil.ensureDirectory(getCampaignDirectory());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to initialize output directory", e);
        }
    }

    private void validateJavaExec() throws MojoExecutionException {
        if (!SystemUtils.endsWithJavaPath(javaExec.getAbsolutePath()) || !javaExec.isFile()) {
            throw new MojoExecutionException("Invalid Java executable: " + javaExec);
        }
    }

    private File getCampaignDirectory() {
        return new File(outputDir, "campaign");
    }

    private File createTestJar() throws MojoExecutionException {
        try {
            File jar = new File(getLibraryDirectory(), "test.jar");
            FileUtil.buildManifestJar(getTestClassPathElements(), jar);
            return jar;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create test manifest JAR", e);
        }
    }

    String getFramework() {
        return framework;
    }

    File getOutputDir() {
        return outputDir;
    }

    MavenProject getProject() {
        return project;
    }

    Properties getFrameworkArguments() {
        return frameworkArguments;
    }

    public static FuzzFramework createFramework(CampaignConfiguration config, String frameworkName,
                                                Properties frameworkArguments) throws MojoExecutionException {
        try {
            FuzzFramework instance =
                    (FuzzFramework) Class.forName(frameworkName).getDeclaredConstructor().newInstance();
            instance.initialize(config, frameworkArguments);
            return instance;
        } catch (ClassCastException | ReflectiveOperationException | IOException e) {
            throw new MojoExecutionException("Failed to create fuzzing framework instance", e);
        }
    }

    public static String buildClassPath(File... classPathElements) {
        return Arrays.stream(classPathElements).map(File::getAbsolutePath).map(SurefireHelper::escapeToPlatformPath)
                     .collect(Collectors.joining(File.pathSeparator));
    }
}