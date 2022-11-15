package edu.neu.ccs.prl.meringue;

import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.csv.CSVFormatter;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.xml.XMLFormatter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public enum JacocoReportFormat {
    HTML() {
        @Override
        public IReportVisitor createVisitor(File outputDirectory) throws IOException {
            return new HTMLFormatter().createVisitor(new FileMultiReportOutput(new File(outputDirectory, "html")));
        }
    }, CSV() {
        @Override
        public IReportVisitor createVisitor(File outputDirectory) throws IOException {
            return new CSVFormatter().createVisitor(
                    Files.newOutputStream(new File(outputDirectory, "jacoco.csv").toPath()));
        }
    }, XML() {
        @Override
        public IReportVisitor createVisitor(File outputDirectory) throws IOException {
            return new XMLFormatter().createVisitor(
                    Files.newOutputStream(new File(outputDirectory, "jacoco.xml").toPath()));
        }
    };

    public abstract IReportVisitor createVisitor(File outputDirectory) throws IOException;
}