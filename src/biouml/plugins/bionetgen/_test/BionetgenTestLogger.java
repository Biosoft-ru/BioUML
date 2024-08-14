package biouml.plugins.bionetgen._test;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class BionetgenTestLogger
{
    protected static final String endl = "\n";

    protected HashMap<String, TestDescription> test2Description = new HashMap<>();

    protected Writer writer;

    protected String testDir;
    private final String detailsDir;
    protected String bioumlResultsDir;

    public BionetgenTestLogger(Writer writer, String testDir, String detailsDir, String bioumlResultsDir)
    {
        this.writer = writer;
        this.testDir = testDir;
        this.detailsDir = detailsDir;
        this.bioumlResultsDir = bioumlResultsDir;
    }

    public static class TestDescription
    {
        public String name;
        public int status;
        public String messages;
        public long simulationTime;
        public String solverName;

        public TestDescription(String name, int status, String messages, long simulationTime)
        {
            this.name = name;
            this.status = status;
            this.messages = messages;
            this.simulationTime = simulationTime;
        }
    }

    public static class StatusInfo
    {
        public String columnTitle;
        public boolean showColumn;
        public String statusTitle;
        public int testNumber;
        public String description;

        public StatusInfo(String columnTitle, boolean showColumn, String statusTitle, String description)
        {
            this.columnTitle = columnTitle;
            this.showColumn = showColumn;
            this.statusTitle = statusTitle;
            this.description = description;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //
    private double aTol;
    public void setATol(double tol)
    {
        aTol = tol;
    }
    public double getATol()
    {
        return aTol;
    }
    private double rTol;
    public void setRTol(double tol)
    {
        rTol = tol;
    }
    public double getRTol()
    {
        return rTol;
    }

    protected String simulationEngineName;
    public String getSimulationEngineName()
    {
        return simulationEngineName;
    }
    public void setSimulationEngineName(String simulationEngineName)
    {
        this.simulationEngineName = simulationEngineName;
    }

    ///////////////////////////////////////////////////////////////////////////
    // TestLogger interface implementation
    //

    protected int status;
    protected String messages;

    public int getStatus()
    {
        return status;
    }

    public void testStarted(String testName)
    {
        currentTest = testName;
        status = Status.SUCCESSFULL;
        messages = "";
    }

    public void testCompleted()
    {
        try
        {
            TestDescription testDescription = new TestDescription(currentTest, status, messages, simulationTime);
            testDescription.solverName = simulationEngineName;
            test2Description.put(currentTest, testDescription);

            // write separate file with test details
            initStatusMap(null, null);
            writeDetailsFile(testDescription, statusMap.get(Integer.valueOf(status)));
        }
        catch( Throwable t )
        {
            t.printStackTrace();
        }
    }

    protected long simulationTime;
    public void setSimulationTime(long simulationTime)
    {
        this.simulationTime = simulationTime;
    }

    ///////////////////////////////////////////////////////////////////
    // Utility classes
    //

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
            writeDetailedStatistics(out);

            out.write("</body>" + endl);
            out.write("</html>" + endl);

            out.close();
        }
        catch( Throwable t )
        {
            t.printStackTrace();
            t.printStackTrace(out);
        }
    }

    protected void writeStatusHeader(PrintWriter out)
    {
        for( StatusInfo status : statusMap.values() )
        {
            if( status.showColumn )
                out.write("      <th>" + status.columnTitle + "</th>" + endl);
        }
    }

    protected void writeStatusValues(PrintWriter out)
    {
        for( StatusInfo status : statusMap.values() )
        {
            if( status.showColumn )
                out.write("      <td valign=\"middle\" align=\"center\">" + status.testNumber + "</td>" + endl);
        }
    }

    private void writeSummary(PrintWriter out)
    {
        long totalTime = initStatusMap( test2Description.values(), null );

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
                / ( test2Description.size() - statusMap.get(Status.CSV_ERROR).testNumber );

        out.write("  <tr valign=\"top\">" + endl);
        out.write("    <td valign=\"middle\" align=\"center\">" + test2Description.size() + "</td>" + endl);
        writeStatusValues(out);
        out.write("    <td valign=\"middle\" align=\"center\">" + successRate + " %</td>" + endl);
        out.write("    <td valign=\"middle\" align=\"center\">" + totalTime / 1000 + "</td>" + endl);
        out.write("  </tr>" + endl);

        out.write("</tbody>" + endl);
        out.write("</table>" + endl);

        out.write("<p><i><u>Legend:</u></i><br>" + endl);
        for( StatusInfo status : statusMap.values() )
        {
            if( status.showColumn && status.description != null )
                out.write("  " + status.columnTitle + " - " + status.description + ".<br>");
        }

        out.write("<hr align=\"left\" width=\"95%\" size=\"1\"><br>" + endl);
    }


    protected void writeDetailedStatistics(PrintWriter out) throws Exception
    {
        out.write("<table class=\"details\" cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">" + endl);
        out.write("<tbody>" + endl);

        out.write("  <tr valign=\"top\">" + endl);
        out.write("    <th valign=\"middle\">Name</th>" + endl);
        out.write("    <th>Status</th>" + endl);
        out.write("    <th width=\"80%\">Description</th>" + endl);
        out.write("    <th nowrap=\"nowrap\">Time (s)</th>" + endl);
        out.write("  </tr>" + endl);
        for( TestDescription test : test2Description.values() )
        {
            StatusInfo info = statusMap.get(test.status);

            out.write("  <tr valign=\"center\">" + endl);
            out.write("    <td><a href=\"" + "../details/" + test.name + "-" + test.solverName //+ sbmlLevel
                    + "-details.html\">" + test.name + "</td>" + endl);
            out.write("    <td>" + info.statusTitle + "</td>" + endl);

            String description = "&nbsp";
            if( test.messages != null )
                description = test.messages.replaceAll("\n", "\n<br>");

            out.write("    <td width=\"60%\">" + description + "</td>" + endl);

            out.write("    <td>" + ( test.simulationTime / 1000.0 ) + "</td>" + endl);
            out.write("  </tr>" + endl);
        }
        out.write("</tbody>" + endl);
        out.write("</table>" + endl);

        out.write("<p><a href=\"#top\">Back to top</a>" + endl);
        out.write("<hr align=\"left\" width=\"95%\" size=\"1\">" + endl);

    }

    ///////////////////////////////////////////////////////////////////////////
    // Write file with details for the test
    //

    protected String getModelFilePath(String testName)
    {
        return "../../../" + testDir + testName + ".bngl";
    }

    protected String getJavaFilePath(String testName)
    {
        return "../java_out/" + testName + ".java";
    }

    protected String getImageFilePath(String imageName)
    {
        return "../images/" + imageName + ".png";
    }

    private void writeDetailsFile(TestDescription test, StatusInfo info) throws Exception
    {
        File outDir = new File(detailsDir);
        if( !outDir.exists() && !outDir.mkdirs() )
            throw new Exception("Failed to create details directory");

        try (PrintWriter out = new PrintWriter( new File( outDir, test.name + "-" + test.solverName + "-details.html" ),
                StandardCharsets.UTF_8.toString() ))
        {
            String testURL = test.name;

            // while URL in test file is wrong, then this is special trick of JDesigner
            if( testURL.endsWith( "jdesigner" ) )
                testURL = testURL.substring( 0, testURL.length() - 10 );

            out.write( "<html>" + endl );
            out.write( "<head>" + endl );

            out.write( "<title>BNGL semantic test</title>" + endl );

            out.write( "<style type=\"text/css\">" + endl );
            out.write( "table.bngl-table {" + endl );
            out.write( "        margin:0 auto;" + endl );
            out.write( "        background-color:white;" + endl );
            out.write( "        border:thin solid black;" + endl );
            out.write( "        border-collapse:collapse;" + endl );
            out.write( "}" + endl );

            out.write( "td.bngl-column-head {" + endl );
            out.write( "        font-size:small;" + endl );
            out.write( "        font-weight:bold;" + endl );
            out.write( "        font-variant:small-caps;" + endl );
            out.write( "        font-style:normal;" + endl );
            out.write( "        color:purple;" + endl );
            out.write( "        background-color:#c0c0c0;" + endl );
            out.write( "        text-align:center;" + endl );
            out.write( "        border-top:thin solid black;" + endl );
            out.write( "        border-bottom:thin solid black;" + endl );
            out.write( "        padding-left:5;" + endl );
            out.write( "        padding-right:5;" + endl );
            out.write( "}" + endl );
            out.write( "</style>" + endl );
            out.write( "</head>" + endl );

            out.write( "<body>" + endl );

            out.write( "<h3>Test: " + test.name + "   " + info.statusTitle + "</font></h3>" + endl );

            // check whether diagram gif and plots are available
            String diagramGif = null;
            if( ( new File( bioumlResultsDir + "images/" + testURL + ".png" ) ).exists() )
                diagramGif = getImageFilePath( testURL );

            out.write( "<pre> test              : " + test.name + endl );
            out.write( " model             : " );
            out.write( "<a href=\"" + getModelFilePath( test.name ) + "\" target=\"_blank\">bngl</a>, " );
            out.write( "<a href=\"" + test.name + "\"( </a><a href=\"#model\">" );
            if( diagramGif != null )
                out.write( "diagram, " );
            out.write( "description</a>" + endl );
            out.write( " simulation engine : " + test.solverName + endl );

            out.write( " generated code    : " );
            out.write( "<a href=\"" + getJavaFilePath( test.name ) + "\" target=\"_blank\">" + ( test.name + ".java" ) + "</a>" + endl );

            out.write( " results           : <a href=\"#results\">table with results</a>" + endl );
            out.write( " status            : " + info.statusTitle + endl );
            out.write( " simulation time   : " + test.simulationTime / 1000.0 + " s." + endl );
            out.write( "</pre>" + endl );

            out.write( writeSimulationParameters( test.name ) );

            out.write( "<hr>" + endl );
            writeModelDescriptionBlock( out, testURL, diagramGif );
            out.write( "<hr>" + endl );

            // write table comparing the result of simulation
            // and the prescribed one

            out.print( "<hr>" + endl );

            writeSimulationResultTable( out, testURL );
            writeLegendBlock( out );

            out.write( "<p><a href=\"#top\">Back to top</a>" + endl );
            out.write( "<hr align=\"left\" width=\"95%\" size=\"1\">" + endl );
        }
    }

    protected String writeSimulationParameters(String testName)
    {
        String parameters = "<b>Parameters:</b><pre>";
        parameters += BionetgenTestUtility.readFile(bioumlResultsDir + testName + "-settings.txt");
        parameters += "</pre>";
        return parameters;
    }

    protected String writeModelDescription(String testURL)
    {
        String descriptionFile = bioumlResultsDir + testURL + "-model" + ".html";
        if( ! ( new File(descriptionFile) ).exists() )
            return "";

        return BionetgenTestUtility.readFile( descriptionFile ).replaceAll( "\n", "<br>" );
    }

    protected void writeModelDescriptionBlock(PrintWriter out, String testURL, String diagramGif)
    {
        out.append( "<a name=\"model\"><h3>BNGL model&nbsp;</h3></a>" + endl );
        String generatedBy = writeModelDescription( testURL );
        if( diagramGif == null )
        {
            out.write( "<pre>" + generatedBy + "</pre>" );
        }
        else
        {
            out.write( "<pre>Diagram before convertation : <a href =\"" + getImageFilePath( testURL + "-template" )
                    + "\" target=\"_blank\">Template diagram image</a>" + endl );
            out.write( "Diagram after convertation  : <a href =\"" + diagramGif + "\" target=\"_blank\">Result diagram image</a></pre>"
                    + endl );
            if( generatedBy.isEmpty() )
                return;
            out.write( "<table>" + endl );
            out.write( "  <tr valign=top align=center>" + endl );
            out.write( "    <td>Model description" + endl );
            out.write( "    </td>" + endl );
            out.write( "  </tr>" + endl );
            out.write( "  <tr valign=top>" + endl );
            out.write( "    <td>" + generatedBy );
            out.write( "    </td>" + endl );
            out.write( "  </tr>" + endl );
            out.write( "</table>" + endl );
        }
    }

    protected void writeSimulationResultTable(PrintWriter out, String testURL) throws Exception
    {
        out.print("<a name=\"results\"></a><h3>Simulation results (<a href=\"#results_legend\">legend</a>)</h3></a>" + endl);

        String postfix = "-results";

        String csvFileName = testDir + testURL + ".gdat";
        File csvFile = new File(csvFileName);
        if( !csvFile.exists() )
        {
            out.write( "<font color=red>.dat file absents, file=" + csvFileName + ".</font><br>" );
            return;
        }

        String bioumlCsvFileName = bioumlResultsDir + testURL + postfix + ".csv";
        File bioumlCsvFile = new File(bioumlCsvFileName);
        if( !bioumlCsvFile.exists() )
        {
            out.write( "<font color=red>CSV file absents, file=" + bioumlCsvFileName + ".</font><br>" );
            return;
        }

        Map<String, double[]> bioumlResults = BionetgenTestUtility.readResults(bioumlCsvFileName);
        Map<String, double[]> testResults = BionetgenTestUtility.readResults(csvFileName);

        if( bioumlResults == null || bioumlResults.isEmpty() || bioumlResults.get( "time" ).length == 0 )
        {
            out.print("<b>simulatedValues == null</b>" + endl);
            return;
        }

        out.print("<table border=\"1\">" + endl);
        out.print("<tr>");
        out.print("<td>time</td>");

        for( String name : testResults.keySet() )
        {
            if( !name.equals("time") )
                out.print("<td>" + name + "</td>");
        }
        out.print("</tr>");

        double[] times = testResults.get("time");
        int resultSize = times.length;
        for( int i = 0; i < resultSize; i++ )
        {
            out.print("<tr>" + endl);
            out.print("<td>" + endl);
            out.print(times[i]);
            out.print("</td>" + endl);

            for( Map.Entry<String, double[]> entry : testResults.entrySet())
            {
                String varName = entry.getKey();
                if( varName.equals("time") )
                    continue;
                double csvValue = entry.getValue()[i];
                double bioumlValue = bioumlResults.get(varName)[i];
                out.print("<td>" + endl);
                out.print(csvValue);
                if( BionetgenStatisticsCalculator.significantlyDiffer(csvValue, bioumlValue, aTol, rTol) )
                    out.print("<br><font color=\"#FF0000\">" + bioumlValue + "</font>");
                else if( !BionetgenStatisticsCalculator.almostEqual(csvValue, bioumlValue, aTol, rTol) )
                    out.print("<br><font color=\"#800000\">" + bioumlValue + "</font>");
                else
                    out.print("<br>" + bioumlValue);
                out.print("</td>" + endl);
            }

            out.print("</tr>" + endl);
        }

        out.print("</table>" + endl);
    }

    protected void writeLegendBlock(PrintWriter out)
    {
        out.write("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);
        out.write("each cell contains two values:<br>" + endl);
        out.write("&nbsp; -&nbsp; value that should be obtained according to .dat file;<br>" + endl);
        out.write("&nbsp; -&nbsp; value that were approximated for corresponding point by BioUML workbench simulation engine." + endl);

        out.write("<p>Color indicates difference between values:<br>" + endl);
        out.write("&nbsp; -&nbsp; simulation results satisfy SBML passing criterion<br>" + endl);
        out.write("&nbsp; -&nbsp; <font color=\"#800000\">relative error (&gt; SBML passing criterion allows and &lt;50%)</font><br>"
                + endl);

        out.write("&nbsp; -&nbsp; <font color=\"#FF0000\">simulation results don't satisfy SBML passing criterion and significantly differs (&gt;50%)</font></p>"
                + endl);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utilities
    //

    protected Map<Integer, StatusInfo> statusMap;
    protected long initStatusMap(Collection<?> collection, Map<?, ?> testMap)
    {
        statusMap = new TreeMap<>();

        put(Status.SUCCESSFULL, "Successfull", true, "ok", null);

        put(Status.NUMERICALLY_WRONG, "Errors", true, "<font color=\"pink\">Error</font>",
                "simulation results significantly differ from the known ones");

        put(Status.NEEDS_TUNING, "Needs tuning", true, "<font color=\"orange\">Needs tuning</font>", "relative error is not small enough");

        put(Status.CSV_ERROR, "CSV error", true, "<font color=\"magenta\">CSV error</font>",
                "original CSV data is missing or can not be parsed");

        if( collection == null || collection.isEmpty() )
            return 0;

        int totalTime = 0;
        for( Object obj : collection )
        {
            TestDescription test;
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

    protected void put(int status, String columnTitle, boolean showColumn, String statusTitle, String description)
    {
        statusMap.put(status, new StatusInfo(columnTitle, showColumn, statusTitle, description));
    }

    protected String currentTest;
    public String getCurrentTest()
    {
        return currentTest;
    }

    public void error(int status, String message)
    {
        this.status = status;
        this.messages += message + "  ";
    }
}
