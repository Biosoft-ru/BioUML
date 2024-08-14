package biouml.plugins.sbml._test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.sbml._test.TestListParser.Category;
import biouml.standard.simulation.SimulationResult;

public class HtmlSemanticTestStatisticsLogger extends DefaultTestLogger
{
    protected static final String endl = System.getProperty("line.separator");

    protected Iterable<TestListParser.Category> categories;

    protected Map<String, Map<String, TestDescription>> category2tests = new HashMap<>();

    protected Writer writer;

    public HtmlSemanticTestStatisticsLogger(String title, List<TestListParser.Category> categories, Writer writer)
    {
        super(title);
        this.categories = categories;
        this.writer = writer;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    private String simulationEngineName;
    public String getSimulationEngineName()
    {
        return simulationEngineName;
    }
    public void setSimulationEngineName(String simulationEngineName)
    {
        this.simulationEngineName = simulationEngineName;
    }

    private String htmlLinksPrefix = "../semantic-test-suite/";
    public String getHtmlLinksPrefix()
    {
        return htmlLinksPrefix;
    }
    public void setHtmlLinksPrefix(String htmlLinksPrefix)
    {
        this.htmlLinksPrefix = htmlLinksPrefix;
    }

    private String testDir = "biouml/plugins/sbml/_test/semantic-test-suite/";
    public String getTestDir()
    {
        return testDir;
    }
    public void setTestDir(String testDir)
    {
        this.testDir = testDir;
    }

    private String detailsDir = "biouml/plugins/sbml/_test/details";
    public String getDetailsDir()
    {
        return detailsDir;
    }
    public void setDetailsDir(String detailsDir)
    {
        this.detailsDir = detailsDir;
    }

    private SimulationResult simulationResult;
    public SimulationResult getSimulationResult()
    {
        return simulationResult;
    }
    public void setSimulationResult(SimulationResult simulationResult)
    {
        this.simulationResult = simulationResult;
    }

    private String scriptName;
    public String getScriptName()
    {
        return scriptName;
    }
    public void setScriptName(String scriptName)
    {
        this.scriptName = scriptName;
    }

    private String sbmlLevel;
    public String getSbmlLevel()
    {
        return sbmlLevel;
    }
    public void setSbmlLevel(String sbmlLevel)
    {
        this.sbmlLevel = sbmlLevel;
    }

    ///////////////////////////////////////////////////////////////////////////
    // TestLogger interface implementation
    //

    @Override
    public void testCompleted()
    {
        try
        {
            super.testCompleted();

            TestDescription testDescription = new TestDescription(currentCategory, currentTest, status, messages, exception, this
                    .getSimulationTime());

            if( category2tests.containsKey(currentCategory) )
            {
                Map<String, TestDescription> testToDescriptions = category2tests.get(currentCategory);
                testToDescriptions.put(currentTest, testDescription);
            }
            else
            {
                Map<String, TestDescription> map = new HashMap<>();
                map.put(currentTest, testDescription);
                category2tests.put(currentCategory, map);
            }

            // write separate file with test details
            initStatusMap(null, null);
            writeDetailsFile(testDescription, statusMap.get(Integer.valueOf(status)));

            testDescriptions.add(testDescription);
        }
        catch( Throwable t )
        {
            t.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Utility classes
    //

    @Override
    public void complete()
    {
        PrintWriter out = writer instanceof PrintWriter ? (PrintWriter)writer : new PrintWriter(writer);

        try
        {
            out.write("<html>" + endl);
            out.write("<head>" + endl);
            out.write("<title>Summary statistics</title>" + endl);
            out.write("</head>" + endl);
            out.write("<body>" + endl);

            writeSummary(out);
            writeCategoryStatistics(out);
            writeDetailedStatistics(out);

            out.write("</body>" + endl);
            out.write("</html>" + endl);

            out.close();
        }
        catch( Throwable t )
        {
            t.printStackTrace(out);
        }
    }

    protected void writeStatusHeader(PrintWriter out)
    {
        for(StatusInfo status : statusMap.values())
        {
            if( status.showColumn )
                out.write("      <th>" + status.columnTitle + "</th>" + endl);
        }
    }

    protected void writeStatusValues(PrintWriter out)
    {
        for( Map.Entry<Integer, StatusInfo> e : statusMap.entrySet() )
        {
            StatusInfo status = e.getValue();
            if( status.showColumn )
                out.write("      <td valign=\"middle\" align=\"center\">" + status.testNumber + "</td>" + endl);
        }
    }

    private void writeSummary(PrintWriter out)
    {
        long totalTime = initStatusMap(testDescriptions.iterator(), null);

        out.write("<h2>Summary</h2>" + endl);
        out.write("<table class=\"details\" cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">" + endl);
        out.write("<tbody>" + endl);
        out.write("  <tr valign=\"top\" style=\"border: medium solid ;\">" + endl);
        out.write("    <th align=\"center\" valign=\"middle\">Tests</th>" + endl);
        writeStatusHeader(out);
        out.write("      <th>Success rate</th>" + endl);
        out.write("      <th>Time (s)</th>" + endl);
        out.write("  </tr>" + endl);

        float successRate = 100.0f * statusMap.get(Status.SUCCESSFULL).testNumber
                / ( testDescriptions.size() - statusMap.get(Status.CSV_ERROR).testNumber );

        out.write("  <tr valign=\"top\">" + endl);
        out.write("    <td valign=\"middle\" align=\"center\">" + testDescriptions.size() + "</td>" + endl);
        writeStatusValues(out);
        out.write("    <td valign=\"middle\" align=\"center\">" + successRate + " %</td>" + endl);
        out.write("    <td valign=\"middle\" align=\"center\">" + totalTime / 1000 + "</td>" + endl);
        out.write("  </tr>" + endl);

        out.write("</tbody>" + endl);
        out.write("</table>" + endl);

        out.write("<p><i><u>Legend:</u></i><br>" + endl);
        for(StatusInfo status : statusMap.values())
        {
            if( status.showColumn && status.description != null )
                out.write("  " + status.columnTitle + " - " + status.description + ".<br>");
        }

        out.write("<hr align=\"left\" width=\"95%\" size=\"1\"><br>" + endl);
    }

    private void writeCategoryStatistics(PrintWriter out)
    {
        out.write("<table class=\"details\" cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">" + endl);
        out.write("<tbody>" + endl);
        out.write("  <tr valign=\"top\">" + endl);
        out.write("    <th width=\"80%\">Name</th>" + endl);
        out.write("    <th>Tests (s)</th>" + endl);
        writeStatusHeader(out);
        out.write("    <th>Time (s)</th>" + endl);
        out.write("  </tr>" + endl);

        Iterator<Category> categoryIter = categories.iterator();
        while( categoryIter.hasNext() )
        {
            TestListParser.Category category = categoryIter.next();
            Map<String, TestDescription> testMap = category2tests.get(category.name);
            long totalTime = initStatusMap(category.tests.iterator(), testMap);

            out.write("  <tr>" + endl);
            out.write("    <td valign=\"middle\"><a href=\"#" + category.name + "\">" + category.name + "</a></td>" + endl);
            out.write("    <td valign=\"middle\">" + category.tests.size() + "</td>" + endl);
            writeStatusValues(out);
            out.write("    <td valign=\"middle\" align=\"center\">" + totalTime / 1000 + "</td>" + endl);
            out.write("  </tr>" + endl);
        }

        out.write("  </tbody>" + endl);
        out.write("</table>" + endl);
        out.write("<hr align=\"left\" width=\"95%\" size=\"1\">" + endl);
    }

    private void writeDetailedStatistics(PrintWriter out) throws Exception
    {
        Iterator<Category> categoryIter = categories.iterator();
        while( categoryIter.hasNext() )
        {
            TestListParser.Category category = categoryIter.next();
            Map<String, TestDescription> testMap = category2tests.get(category.name);

            out.write("<a name=\"" + category.name + "\">" + "<h3>Category " + category.name + "</h3></a></p>" + endl);
            out.write("<table class=\"details\" cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">" + endl);
            out.write("<tbody>" + endl);

            out.write("  <tr valign=\"top\">" + endl);
            out.write("    <th valign=\"middle\">Name</th>" + endl);
            out.write("    <th>Status</th>" + endl);
            out.write("    <th width=\"80%\">Description</th>" + endl);
            out.write("    <th nowrap=\"nowrap\">Time (s)</th>" + endl);
            out.write("  </tr>" + endl);

            for( String testName : category.tests )
            {
                TestDescription test = testMap.get(testName);
                StatusInfo info = statusMap.get(test.status);

                out.write("  <tr valign=\"center\">" + endl);
                out.write("    <td><a href=\"" + "details/" + stripTestName(test.name) + "-" + simulationEngineName + sbmlLevel
                        + "-details.html\">" + test.name + "</td>" + endl);
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

                out.write("    <td>" + ( test.simulationTime / 1000.0 ) + "</td>" + endl);
                out.write("  </tr>" + endl);
            }

            out.write("</tbody>" + endl);
            out.write("</table>" + endl);

            for( String testName : category.tests )
            {
                TestDescription test = testMap.get(testName);
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

    ///////////////////////////////////////////////////////////////////////////
    // Write file with details for the test
    //

    private void writeDetailsFile(TestDescription test, StatusInfo info) throws Exception
    {
        File outDir = new File(detailsDir);
        if( !outDir.exists() )
            outDir.mkdirs();

        try(PrintWriter out = new PrintWriter( new File( outDir, stripTestName( test.name ) + "-" + simulationEngineName + sbmlLevel
                + "-details.html" ), "UTF-8" ))
        {
            String params = readFile(testDir + test.name + ".test");
            String testURL = test.name;
            
            // while URL in test file is wrong, then this is special trick fo JDesigner
            if( testURL.endsWith("jdesigner") )
                testURL = testURL.substring(0, testURL.length() - 10);
            
            out.write("<html>" + endl);
            out.write("<head>" + endl);
            
            out.write("<title>SBML semantic test</title>" + endl);
            
            out.write("<style type=\"text/css\">" + endl);
            out.write("table.sbml-table {" + endl);
            out.write("        margin:0 auto;" + endl);
            out.write("        background-color:white;" + endl);
            out.write("        border:thin solid black;" + endl);
            out.write("        border-collapse:collapse;" + endl);
            out.write("}" + endl);
            
            out.write("td.sbml-column-head {" + endl);
            out.write("        font-size:small;" + endl);
            out.write("        font-weight:bold;" + endl);
            out.write("        font-variant:small-caps;" + endl);
            out.write("        font-style:normal;" + endl);
            out.write("        color:purple;" + endl);
            out.write("        background-color:#c0c0c0;" + endl);
            out.write("        text-align:center;" + endl);
            out.write("        border-top:thin solid black;" + endl);
            out.write("        border-bottom:thin solid black;" + endl);
            out.write("        padding-left:5;" + endl);
            out.write("        padding-right:5;" + endl);
            out.write("}" + endl);
            out.write("</style>" + endl);
            out.write("</head>" + endl);
            
            out.write("<body>" + endl);
            out.write("<h3>Test: " + test.name + ".test - " + info.statusTitle + "</font></h3>" + endl);
            
            // check whether diagram gif and plots are available
            String diagramGif = null;
            if( ( new File(testDir + testURL + "-diagram.gif") ).exists() )
                diagramGif = htmlLinksPrefix + testURL + "-diagram.gif";
            
            String plotGif = null;
            if( ( new File(testDir + testURL + ".GIF") ).exists() )
                plotGif = htmlLinksPrefix + testURL + "GIF";
            
            out.write("<pre> test              : " + test.name + ".test" + endl);
            out.write(" model             : ");
            out.write("<a href=\"" + htmlLinksPrefix + test.name + sbmlLevel + ".xml" + "\" target=\"_blank\">xml</a>, ");
            out.write("<a href=\"" + test.name + "\"( </a><a href=\"#model\">");
            if( diagramGif != null )
                out.write("diagram, ");
            out.write("description</a>" + endl);
            out.write(" simulation engine : " + simulationEngineName + endl);
            
            out.write(" generated code    : ");
            if( scriptName == null )
                out.write("code was not generated - model is static." + endl);
            else
            {
                if( scriptName.endsWith("m") )
                    out.write("<a href=\"../matlab/" + scriptName + "\" target=\"_blank\">" + scriptName + "</a>" + endl);
                else if( scriptName.endsWith("java") )
                    out.write("<a href=\"../java/" + scriptName + "\" target=\"_blank\">" + scriptName + "</a>" + endl);
            }
            
            out.write(" results           : <a href=\"#results\">table with results</a>" + endl);
            if( plotGif != null )
                out.write(" plots             : </a><a href=\"#plots\">normal</a>, <a href=\"#plot_log\">log</a>" + endl);
            out.write(" status            : " + info.statusTitle + endl);
            out.write(" simulation time   : " + test.simulationTime / 1000.0 + " s." + endl);
            out.write("</pre>" + endl);
            
            out.write("<b>Parameters:</b><pre>");
            out.write(params);
            out.write("</pre>");
            
            if( test.exception != null )
            {
                out.write("Error, stack trace:" + endl);
                out.write("<pre>" + endl);
                test.exception.printStackTrace(out);
                out.write("</pre>" + endl);
            }
            
            out.write("<hr>" + endl);
            out.write("<a name=\"model\"><h3>SBML model&nbsp;</h3></a>" + endl);
            String generatedBy = "";
            if( diagramGif == null )
                generatedBy = writeModelDescription(out, testURL);
            else
            {
                out.write("<table>" + endl);
                out.write("  <tr valign=top align=center>" + endl);
                out.write("      <td>Diagram</td>" + endl);
                out.write("    <td width=\"20\">&nbsp;</td>" + endl);
                out.write("      <td>Model description" + endl);
                out.write("      </td>" + endl);
                out.write("  <tr valign=top>" + endl);
                out.write("    <td><img src=\"" + diagramGif + "\"></td>" + endl);
                out.write("    <td width=\"5\">&nbsp;</td>" + endl);
                out.write("    <td>" + endl);
                generatedBy = writeModelDescription(out, testURL);
                out.write("    </td>" + endl);
                out.write("    </tr>" + endl);
                out.write("    </table>" + endl);
            }
            out.write(generatedBy);
            
            if( plotGif != null )
            {
                out.write("<hr>" + endl);
                out.write("<a name=\"plots\"><h3>Plots</h3></a>" + endl);
                
                out.write("<p><i>Normal plot</i><br>" + endl);
                out.write("<img src=\"" + htmlLinksPrefix + testURL + ".GIF\">" + endl);
                
                if( ( new File(testDir + testURL + "-log.GIF") ).exists() )
                {
                    out.write("<p><a name=plot_log><i>Logarithmic plot</i><br>" + endl);
                    out.write("<img src=\"" + htmlLinksPrefix + testURL + "-log.GIF\">" + endl);
                }
            }
            
            // write table comparing the result of simulation
            // and the prescribed one
            out.write("<hr>" + endl);
            writeSimulationResultTable(out, testURL);
            
            out.write("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);
            out.write("each cell contains two values:<br>" + endl);
            out.write("&nbsp; -&nbsp; value that should be obtained accrding to CSV file;<br>" + endl);
            out.write("&nbsp; -&nbsp; value that were approximated for corresponding point by BioUML workbench simulation engine." + endl);
            
            out.write("<p>Color indicates difference between values:<br>" + endl);
            out.write("&nbsp; -&nbsp; no significant difference ( &lt; 0.1%)<br>" + endl);
            out.write("&nbsp; -&nbsp; <font color=\"#800000\">small relative error ( &gt; 0.1% and &lt;50%)</font><br>" + endl);
            
            out.write("&nbsp; -&nbsp; <font color=\"#FF0000\">simulation results significantly differ ( &gt;50%)</font></p>" + endl);
        }
    }

    protected String writeModelDescription(PrintWriter out, String testURL) throws IOException
    {
        String str = readFile(testDir + testURL + ".html");
        String descr = substring(str, "<table class=\"sbml-table\"", "<hr>");
        out.print(descr + endl);

        String generatedBy = substring(str, "<hr>", "</div></body>");
        if( generatedBy.length() > 10 )
            generatedBy = generatedBy.substring(5);

        return generatedBy;
    }

    protected void writeSimulationResultTable(PrintWriter out, String testURL)
    {
        out.print("<hr>" + endl);
        out.print("<a name=\"results\"></a><h3>Simulation results (<a href=\"#results_legend\">legend</a>)</h3></a>" + endl);

        if( simulationResult == null )
        {
            out.write("<font color=red>Result was not generated.</font><br>");
            return;
        }

        File csvFile = new File(testDir + testURL + ".CSV");
        if( !csvFile.exists() )
        {
            out.write("<font color=red>CSV file absents, file=" + csvFile.getName() + ".</font><br>");
            return;
        }

        SbmlCSVHander csvHandler = new SbmlCSVHander(csvFile, this);
        List<String> variableNames = csvHandler.getVariableNames();
        Map<String, Integer> mangledMap = SbmlTestSimulationEngineAdapter.getMangledNamesMap(simulationResult.getVariableMap());

        int varCount = variableNames.size();
        if( varCount < mangledMap.size() )
            varCount = mangledMap.size();

        int[] indeces = new int[varCount];
        int counter = 0;

        for(String name : variableNames)
        {
            if( !name.equals("time") )
            {
                if( mangledMap.containsKey(name) )
                    indeces[counter++] = mangledMap.get(name).intValue();
                else
                    indeces[counter++] = -1;
            }
        }

        double[] times = simulationResult.getTimes();
        if( times == null )
        {
            out.print("<b>times == null</b>" + endl);
            return;
        }

        double[][] simulatedValues = simulationResult.getValues();
        if( simulatedValues == null )
        {
            out.print("<b>simulatedValues == null</b>" + endl);
            return;
        }

        out.print("<table border=\"1\">" + endl);
        out.print("<tr>");
        out.print("<td>time</td>");

        for(String name : variableNames)
        {
            if( !name.equals("time") )
                out.print("<td>" + name + "</td>");
        }
        out.print("</tr>");


        for( double[] csvValues : csvHandler.getVariableValues() )
        {
            // find corresponding time in simulated values
            int _time = 0;
            for( ; _time < times.length; )
            {
                if( Math.abs(csvValues[0] - times[_time]) <= 1e-7 * ( Math.abs(csvValues[0]) + Math.abs(times[_time]) ) )
                    break;

                _time++;
            }

            if( _time == times.length )
            {
                out.print("No corresponding time found for value " + csvValues[0]);
                continue;
            }

            out.print("<tr>" + endl);
            out.print("<td>" + endl);
            out.print(csvValues[0]);
            out.print("</td>" + endl);

            for( int i = 0; i < varCount - 1; i++ )
            {
                if( indeces[i] >= 0 && i < csvValues.length - 1 )
                {
                    out.print("<td>" + endl);
                    out.print(csvValues[i + 1]);
                    if( SbmlTestSimulationEngineAdapter.significantlyDiffer(csvValues[i + 1], simulatedValues[_time][indeces[i]]) )
                    {
                        out.print("<br><font color=\"#FF0000\">" + simulatedValues[_time][indeces[i]] + "</font>");
                    }
                    else if( !SbmlTestSimulationEngineAdapter.almostEqual(csvValues[i + 1], simulatedValues[_time][indeces[i]]) )
                    {
                        out.print("<br><font color=\"#800000\">" + simulatedValues[_time][indeces[i]] + "</font>");
                    }
                    else
                    {
                        out.print("<br>" + simulatedValues[_time][indeces[i]]);
                    }
                    out.print("</td>" + endl);
                }
            }

            out.print("</tr>" + endl);
        }

        out.print("</table>" + endl);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utilities
    //

    public String stripTestName(String fullTestName)
    {
        int index = fullTestName.lastIndexOf('/');
        return index == -1 ? fullTestName : fullTestName.substring(index + 1, fullTestName.length());
    }

    public String readFile(String fileName) throws IOException
    {
        File file = new File(fileName);
        return ApplicationUtils.readAsString(file);
    }

    public String substring(String source, String start, String end)
    {
        int iStart = source.indexOf(start);
        if( iStart != -1 )
        {
            int iEnd = source.indexOf(end, iStart);
            if( iEnd != -1 )
                return source.substring(iStart, iEnd);
        }

        return "";
    }
}
