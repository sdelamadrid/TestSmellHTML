package com.slama.testsmellhtml;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "test-smell-html", defaultPhase = LifecyclePhase.TEST)
public class TestSmellMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {

        TestSmellDetector testSmellDetector = new TestSmellDetector();

        /*
         * Read the input file and build the TestFile objects
         */
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader("pathToInputFile.csv"));
            String str;

            String[] lineItem;
            TestFile testFile;
            List<TestFile> testFiles = new ArrayList<>();
            while ((str = in.readLine()) != null) {
                // use comma as separator
                lineItem = str.split(",");

                // check if the test file has an associated production file
                if (lineItem.length == 2) {
                    testFile = new TestFile(lineItem[0], lineItem[1], "");
                } else {
                    testFile = new TestFile(lineItem[0], lineItem[1], lineItem[2]);
                }

                testFiles.add(testFile);
            }
            in.close();

            /*
             * Initialize the output file - Create the output file and add the column names
             */
            ResultsWriter resultsWriter = ResultsWriter.createResultsWriter();
            List<String> columnNames;
            List<String> columnValues;

            columnNames = testSmellDetector.getTestSmellNames();
            columnNames.add(0, "App");
            columnNames.add(1, "TestClass");
            columnNames.add(2, "TestFilePath");
            columnNames.add(3, "ProductionFilePath");
            columnNames.add(4, "RelativeTestFilePath");
            columnNames.add(5, "RelativeProductionFilePath");

            resultsWriter.writeColumnName(columnNames);

            /*
             * Iterate through all test files to detect smells and then write the output
             */
            TestFile tempFile;
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date;
            for (TestFile file : testFiles) {
                date = new Date();
                System.out.println(dateFormat.format(date) + " Processing: " + file.getTestFilePath());
                System.out.println("Processing: " + file.getTestFilePath());

                // detect smells
                tempFile = testSmellDetector.detectSmells(file);

                // write output
                columnValues = new ArrayList<>();
                columnValues.add(file.getApp());
                columnValues.add(file.getTestFileName());
                columnValues.add(file.getTestFilePath());
                columnValues.add(file.getProductionFilePath());
                columnValues.add(file.getRelativeTestFilePath());
                columnValues.add(file.getRelativeProductionFilePath());
                for (AbstractSmell smell : tempFile.getTestSmells()) {
                    try {
                        columnValues.add(String.valueOf(smell.getHasSmell()));
                    } catch (NullPointerException e) {
                        columnValues.add("");
                    }
                }
                resultsWriter.writeLine(columnValues);
            }

            // read lines of csv to a string array list
            List<String> lines = new ArrayList<String>();
            try (BufferedReader reader = new BufferedReader(new FileReader(resultsWriter.getOutputFile()))) {
                String currentLine;
                while ((currentLine = reader.readLine()) != null) {
                    lines.add(currentLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // embrace <td> and <tr> for lines and columns
            for (int i = 0; i < lines.size(); i++) {
                lines.set(i, "<tr><td>" + lines.get(i) + "</td></tr>");
                lines.set(i, lines.get(i).replaceAll(",", "</td><td>"));
            }

            // embrace <table> and </table>
            lines.set(0, "<table border>" + lines.get(0));
            lines.set(lines.size() - 1, lines.get(lines.size() - 1) + "</table>");

            // output result
            String time = String.valueOf(Calendar.getInstance().getTimeInMillis());
            String htmlFile = MessageFormat.format("{0}_{1}_{2}.{3}", "Output", "TestSmellDetection", time, "html");
            try (FileWriter writer = new FileWriter(htmlFile)) {
                for (String line : lines) {
                    writer.write(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("end");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
