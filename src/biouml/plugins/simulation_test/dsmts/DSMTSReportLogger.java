

package biouml.plugins.simulation_test.dsmts;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import biouml.plugins.simulation_test.SbmlCSVHandler;
import biouml.plugins.simulation_test.SemanticTestLogger;
import biouml.plugins.simulation_test.Status;
import biouml.plugins.simulation_test.SemanticTestListParser.Category;

public class DSMTSReportLogger extends SemanticTestLogger
{

    public DSMTSReportLogger(File categoryFile, Writer writer, String testDir, String detailsDir, String bioumlResultsDir, boolean newFlag)
    {
        super(categoryFile, writer, testDir, detailsDir, bioumlResultsDir, newFlag);
        setSbmlLevel("");
    }

    protected int simulationNumber = 1;
    public void setSimulationNumber(int number)
    {
        simulationNumber = number;
    }

    public void setDuration(double duration)
    {
        this.duration = duration;
    }

    public void setInitialTime(double initialTime)
    {
        this.initialTime = initialTime;
    }

    public void setStep(double step)
    {
        this.step = step;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    private String modelFilePath;
    private String javaFilePath;

    protected double initialTime;
    protected double duration;
    protected double step;
    protected boolean isExactTesting = true;
    protected String[] variables;
    public void setVariables(String[] variables)
    {
        this.variables = variables.clone();
    }

    public void setModelFilePath(String modelFilePath)
    {
        this.modelFilePath = modelFilePath;
    }

    protected void setJavaFilePath(String javaFilePath)
    {
        this.javaFilePath = javaFilePath;
    }

    @Override
    protected String getModelFilePath(String testName)
    {
        return modelFilePath;
    }

    @Override
    protected String getJavaCodePath(String testName, String level)
    {
        return javaFilePath;
    }

    public void setExactTesting(boolean isExactTesting)
    {
        this.isExactTesting = isExactTesting;
    }
    
    @Override
    public void testCompleted()
    {
        super.testCompleted();
        test2Description.get(currentTest).simulationNumber = simulationNumber;
    }

    @Override
    protected void writeDetailedStatistics(PrintWriter out) throws Exception
    {
        Iterator<Category> categoryIter = name2Category.values().iterator();
        while( categoryIter.hasNext() )
        {
            Category category = categoryIter.next();
            if( withCategory )
                out.write("<a name=\"" + category.name + "\">" + "<h3>Category " + category.name + "</h3></a></p>" + endl);
            out.write("<table class=\"details\" cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">" + endl);
            out.write("<tbody>" + endl);

            out.write("  <tr valign=\"top\">" + endl);
            out.write("    <th valign=\"middle\">Name</th>" + endl);
            out.write("    <th>Status</th>" + endl);
            out.write("    <th width=\"80%\">Description</th>" + endl);
            out.write("    <th nowrap=\"nowrap\">Simulation times</th>" + endl);
            out.write("    <th nowrap=\"nowrap\">Average time (ms)</th>" + endl);
            out.write("    <th nowrap=\"nowrap\">Time (s)</th>" + endl);
            out.write("  </tr>" + endl);

            for( String testName : category.tests )
            {
                if( !test2Description.containsKey(testName) )
                    continue;

                TestDescription test = test2Description.get(testName);
                StatusInfo info = statusMap.get(test.status);

                out.write("  <tr valign=\"center\">" + endl);
                out.write("    <td nowrap=\"nowrap\"><a href=\"" + "details/" + stripTestName(test.name) + "-" + simulationEngineName+"-details.html\">" + test.name + "</td>" + endl);
                out.write("    <td>" + info.statusTitle + "</td>" + endl);

                String description = "&nbsp";
                if( test.messages != null )
                    description = test.messages.replaceAll("\n", "\n<br>");

                if( test.exception != null )
                {
                    String ref = "<a href=\"#" + category.name + "_" + test.name + "_stack\"> " + test.exception.getMessage() + "</a>";
                    if( "&nbsp".equals(description) )
                        description = ref;
                    else
                        description += "<br>" + ref;
                }
                out.write("    <td width=\"60%\">" + description + "</td>" + endl);
                out.write("    <td valign=\"middle\">" + ( test.simulationNumber ) + "</td>" + endl);
                out.write("    <td valign=\"middle\">" + (  test.simulationTime / (double)test.simulationNumber ) + "</td>" + endl);
                out.write("    <td valign=\"middle\">" + ( test.simulationTime / 1000.0 ) + "</td>" + endl);
                out.write("  </tr>" + endl);
            }

            out.write("</tbody>" + endl);
            out.write("</table>" + endl);

            for( String testName : category.tests )
            {
                if( !test2Description.containsKey(testName) )
                    continue;

                TestDescription test = test2Description.get(testName);
                if( test.exception != null )
                {
                    out.write("<a name=\"" + category.name + "_" + test.name + "_stack\"><h4>" + test.name + " stack trace: </h4></a>"
                            + endl);

                    out.write("<pre>" + endl);
                    test.exception.printStackTrace(out);
                    out.write("</pre>" + endl);
                }
            }

            out.write("<p><a href=\"#top\">Back to top</a>" + endl);
            out.write("<hr align=\"left\" width=\"95%\" size=\"1\">" + endl);
        }
    }
    
    @Override
    protected void writeSimulationResultTable(PrintWriter out, String testURL) throws Exception
    {
        out.print("<a name=\"results\"></a><h3>Simulation results (<a href=\"#results_legend\">legend</a>)</h3></a>" + endl);

        File simulatedMeanFile = new File(bioumlResultsDir + testURL + "-BioUML.mean.csv");
        File simualtedSDFile = new File(bioumlResultsDir + testURL + "-BioUML.sd.csv");

        File testFile = new File(testDir + testURL + "-results.csv");

        if( !simualtedSDFile.exists() )
        {
            out.write("<font color=red>CSV file absents, file=" + simualtedSDFile.getName() + ".</font><br>");
            return;
        }

        if( !simulatedMeanFile.exists() )
        {
            out.write("<font color=red>CSV file absents, file=" + simulatedMeanFile.getName() + ".</font><br>");
            return;
        }

        if( !testFile.exists() )
        {
            out.write("<font color=red>CSV file absents, file=" + testFile.getName() + ".</font><br>");
            return;
        }

        SbmlCSVHandler simulatedMeanHandler = new SbmlCSVHandler(simulatedMeanFile);
        SbmlCSVHandler simulatedSDHandler = new SbmlCSVHandler(simualtedSDFile);
        SbmlCSVHandler expectedHandler = new SbmlCSVHandler(testFile);

        List<String> variableNames = simulatedMeanHandler.getVariableNames();
        double[] simulatedMeanTimes = simulatedMeanHandler.getTimes();
        List<double[]> simulatedMean = simulatedMeanHandler.getVariableValues();
        List<double[]> simulatedSD = simulatedSDHandler.getVariableValues();
        List<double[]> expected = expectedHandler.getVariableValues();

        if( simulatedMean == null )
        {
            out.print("<b>simulated mean values == null</b>" + endl);
            return;
        }

        if( simulatedSD == null )
        {
            out.print("<b>simulated standard deviation values == null</b>" + endl);
            return;
        }

        if( expected == null )
        {
            out.print("<b>test values == null</b>" + endl);
            return;
        }

        out.print("<table border=\"1\">" + endl);
        out.print("<tr>");
        out.print("<td>time</td>");

        Iterator<String> iter = variableNames.iterator();
        while( iter.hasNext() )
        {
            String name = iter.next();
            if( !name.equals("time") )
            {
                out.print("<td>" + name + " Mean Value</td>");
                out.print("<td>" + name + " Standard Deviation</td>");
            }
        }
        out.print("</tr>");

       
        for( int i = 0; i < simulatedMeanTimes.length; i++ )
        {
            double[] simulatedMeanValues = simulatedMean.get(i);
            double[] simulatedSDValues = simulatedSD.get(i);
            double[] expectedValues = expected.get(i);
            
            out.print("<tr>" + endl);
            out.print("<td>" + endl);
            out.print(simulatedMeanTimes[i]);
            out.print("</td>" + endl);

            for( String varName : variableNames )
            {
                if( varName.equals("time") || varName.equals("Time") )
                    continue;
                double simulatedMeanValue = simulatedMeanValues[simulatedMeanHandler.getVariableNames().indexOf(varName)];
                double expectedMeanValue =  expectedValues[expectedHandler.getVariableNames().indexOf(varName+"-mean")];
                double simulatedSDValue = simulatedSDValues[simulatedSDHandler.getVariableNames().indexOf(varName)];
                double expectedSDValue = expectedValues[expectedHandler.getVariableNames().indexOf(varName+"-sd")];

                out.print("<td>" + endl);
                out.print(expectedMeanValue);
                if( !CalculateDSMTSStatistics.checkMeanValues(simulatedMeanValue, expectedMeanValue, expectedSDValue, simulationNumber,
                        isExactTesting) )
                    out.print("<br><font color=\"#FF0000\">" + simulatedMeanValue + "</font>");
                else
                    out.print("<br>" + simulatedMeanValue);

                out.print("<td>" + endl);
                out.print(expectedSDValue);
                if( !CalculateDSMTSStatistics.checkSDValues(simulatedSDValue, expectedSDValue, simulationNumber, isExactTesting) )
                    out.print("<br><font color=\"#FF0000\">" + simulatedSDValue + "</font>");
                else
                    out.print("<br>" + simulatedSDValue);

                out.print("</td>" + endl);

                out.print("</td>" + endl);
            }

            out.print("</tr>" + endl);
        }

        out.print("</table>" + endl);
    }

    @Override
    protected long initStatusMap(Iterator i, Map testMap)
    {
        statusMap = new TreeMap<>();

        put(Status.SUCCESSFULL, "Successfull", true, "ok", null);
        put(Status.FAILED, "Failed", true, "<font color=\"red\">failed</font>", "an exception has occured");

        put(Status.NUMERICALLY_WRONG, "Errors", true, "<font color=\"pink\">Error</font>",
                "simulation results significantly differ from the known ones");

        put(DSMTSStatus.MEAN_VALUES_ERROR, "Only Mean value errors", true, "<font color=\"pink\">Mean value errors</font>",
                "simulated mean values significantly differ from the known ones");

        put(DSMTSStatus.STANDARD_DEVIATION_ERROR, "Only Standard deviation errors", true,
                "<font color=\"pink\">Standard deviation errors</font>",
                "simulated standard deviation significantly differ from the known ones");

        put(Status.RESULT_DIFFER, "Result differs", true, "<font color=\"#800000\">result differs</font>",
                "some variable or time point is missing in simulation engine output.");

        put(Status.CSV_ERROR, "CSV error", true, "<font color=\"magenta\">CSV error</font>",
                "original CSV data is missing or can not be parsed");

        put(Status.COMPILATION_FAILED, "Compilation error", true, "<font color=\"#000080\">Compilation error</font>",
                "Could not compile generated model file");

        if( i == null )
            return 0;

        int totalTime = 0;
        while( i.hasNext() )
        {
            Object obj = i.next();
            TestDescription test = null;
            if( obj instanceof TestDescription )
                test = (TestDescription)obj;
            else
                test = (TestDescription)testMap.get(obj);

            if( test != null )
            {
                StatusInfo status = statusMap.get(test.status);
                if( status != null )
                    status.testNumber++;

                totalTime += test.simulationTime;
            }
        }

        return totalTime;
    }

    @Override
    protected String writeSimulationParameters(String testName)
    {
        String parameters = "<b>Parameters:</b><pre>";
        parameters += "Start: " + initialTime;
        parameters += "<br>Duration: " + duration;
        parameters += "<br>Step: " + step;

        if( variables != null && variables.length > 1 )
        {
            String variableNames = String.join( ",", variables );
            parameters += "<br>Variables: " + variableNames;
        }
        parameters += "<br>Number of iterations: " + simulationNumber;
        String exactSimulator = ( isExactTesting ) ? "Yes" : "No";
        parameters += "<br>Exact simulator: " + exactSimulator;
        parameters += "</pre>";
        return parameters;
    }

    @Override
    protected void writeModelDescriptionBlock(PrintWriter out, String testURL, String diagramGif)
    {
        return;
    }

    @Override
    protected void writePlotsBlock(PrintWriter out, String testURL, String simulatorName)
    {
        out.write("<hr>" + endl);
        out.write("<a name=\"plots\"><h3>Plots</h3></a>" + endl);
        out.write("<img src=\"../csvResults/" + simulatorName + "/" + testURL + "-mean.png\" height=\"450\" width=\"600\">");
        out.write("<img src=\"../csvResults/" + simulatorName + "/" + testURL + "-sd.png\" height=\"450\" width=\"600\">" + endl);
    }
    
    @Override
    protected void writeLegendBlock(PrintWriter out)
    {
        out.write("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);
        out.write("each cell contains two values:<br>" + endl);
        out.write("&nbsp; -&nbsp; value that should be obtained accrding to CSV file;<br>" + endl);
        out.write("&nbsp; -&nbsp; value that were approximated for corresponding point by BioUML workbench stochastic simulation engine." + endl);

        out.write("<p>Color indicates difference between values:<br>" + endl);
        out.write("&nbsp; -&nbsp; simulation results satisfy DSMTS passing criterion<br>" + endl);
        out
                .write("&nbsp; -&nbsp; <font color=\"#FF0000\">simulation results don't satisfy DSMTS passing criterion and significantly differs</font></p>"
                        + endl);
    }

    @Override
    protected boolean shouldWritePlots(String testURL)
    {
        return true;
    }
}
