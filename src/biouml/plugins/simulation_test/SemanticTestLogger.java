package biouml.plugins.simulation_test;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.simulation_test.SemanticTestListParser.Category;
import ru.biosoft.access.exception.BiosoftParseException;
import ru.biosoft.util.TextUtil2;

public class SemanticTestLogger
{
    protected static final String endl = System.getProperty("line.separator");

    protected HashMap<String, Category> name2Category = new HashMap<>();

    protected HashMap<String, TestDescription> test2Description = new HashMap<>();
    protected HashMap<String, ArrayList<String>> test2Categories = new HashMap<>();

    protected Writer writer;

    protected String webEditionURL = "https://ict.biouml.org/bioumlweb/index.html#de=databases/Tests SBML 3.3.0/";

    protected String testDir;
    private final String detailsDir;
    protected String bioumlResultsDir;
    private final boolean newFlag;
    
    protected boolean timeCourse = true;
    
    private final Category defaultCategory = new Category("No Category", new ArrayList<String>());

    protected boolean withCategory = false;

    public SemanticTestLogger(File categoryFile, Writer writer, String testDir, String detailsDir, String bioumlResultsDir, boolean newFlag)
    {
        if( categoryFile != null && categoryFile.exists())
        {
            test2Categories = new SemanticTestListParser().parseTagFile(categoryFile);
            withCategory = true;
        }
        name2Category = new HashMap<>();

        this.writer = writer;
        this.testDir = testDir;
        this.detailsDir = detailsDir;
        this.bioumlResultsDir = bioumlResultsDir;
        this.newFlag = newFlag;
        if( newFlag )
            this.htmlLinksPrefix = "../../semantic/";
    }

    public static class TestDescription
    {
        public List<Category> categories;
        public String name;
        public int status;
        public String messages;
        public long simulationTime;
        public Exception exception;
        public int simulationNumber; //added for stochastic testing
        public String solverName;

        public TestDescription(List<Category> categories, String name, int status, String messages, Exception exception, long simulationTime)
        {
            this.categories = categories;
            this.name = name;
            this.status = status;
            this.messages = messages;
            this.exception = exception;
            this.simulationTime = simulationTime;
        }
    }

    public static class StatusInfo
    {
        public int status;
        public String columnTitle;
        public boolean showColumn;
        public String statusTitle;
        public int testNumber;
        public String description;

        public StatusInfo(int status, String columnTitle, boolean showColumn, String statusTitle, String description)
        {
            this.status = status;
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
    
    public void setTimeCourse(boolean timeCourse)
    {
        this.timeCourse = timeCourse;
    }
    public boolean isTimeCourse()
    {
        return timeCourse;
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

    private String htmlLinksPrefix = "../semantic-test-suite/";
    public String getHtmlLinksPrefix()
    {
        return htmlLinksPrefix;
    }
    public void setHtmlLinksPrefix(String htmlLinksPrefix)
    {
        this.htmlLinksPrefix = htmlLinksPrefix;
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
        exception = null;
    }

    public void testCompleted()
    {
        try
        {
            List<Category> categories = new ArrayList<>();

            if( !test2Categories.containsKey(currentTest) || test2Categories.get(currentTest).isEmpty() )
            {
                categories.add(defaultCategory);
                defaultCategory.tests.add(currentTest);
                name2Category.put(defaultCategory.name, defaultCategory);
            }
            else
            {
                for( String categoryName : test2Categories.get(currentTest) )
                {
                    Category category;
                    if( name2Category.containsKey(categoryName) )
                        category = name2Category.get(categoryName);
                    else
                        category = new Category(categoryName, new ArrayList<String>());
                    category.tests.add(currentTest);
                    categories.add(category);
                    name2Category.put(category.name, category);
                }
            }

            TestDescription testDescription = new TestDescription(categories, currentTest, status, messages, exception, simulationTime);
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

    protected Exception exception;
    public Exception getException()
    {
        return exception;
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
            if( withCategory )
                writeCategoryStatistics(out);

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
        long totalTime = initStatusMap(test2Description.values().iterator(), null);

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
        out.write("    <th>Tests</th>" + endl);
        writeStatusHeader(out);
        out.write("    <th>Time (s)</th>" + endl);
        out.write("  </tr>" + endl);

        Iterator<Category> categoryIter = name2Category.values().iterator();
        while( categoryIter.hasNext() )
        {
            Category category = categoryIter.next();
            long totalTime = initStatusMap(category.tests.iterator(), test2Description);

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
            out.write("    <th nowrap=\"nowrap\">Time (s)</th>" + endl);
            out.write("  </tr>" + endl);

            for( String testName : category.tests )
            {
                if( !test2Description.containsKey(testName) )
                    continue;

                TestDescription test = test2Description.get(testName);
                StatusInfo info = statusMap.get(test.status);

                out.write("  <tr valign=\"center\">" + endl);
                out.write("    <td><a href=\"" + "../details/" + stripTestName(test.name) + "-" + test.solverName + sbmlLevel
                        + "-details.html\">" + stripTestName(test.name) + "</td>" + endl);
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

    ///////////////////////////////////////////////////////////////////////////
    // Write file with details for the test
    //

    protected String getModelFilePath(String testName)
    {
        return /*"../" +*/ htmlLinksPrefix + testName + sbmlLevel + ".xml";
    }

    protected String getJavaCodePath(String testName, String correctLevel)
    {
        if( scriptName != null )
        {
            if( scriptName.endsWith( "java" ) )
                return "../java_out/" + scriptName;
        }

        scriptName = "_" + stripTestName( testName ) + correctLevel;

        return "../java_out/" + scriptName + ".java";
    }

    private void writeDetailsFile(TestDescription test, StatusInfo info) throws Exception
    {
        File outDir = new File(detailsDir);
        if( !outDir.exists() )
            outDir.mkdirs();
        
        try(PrintWriter out = new PrintWriter(new File(outDir, stripTestName(test.name) + "-" + test.solverName + sbmlLevel
                + "-details.html"), "UTF-8"))
        {
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

            out.write("<h3>Test: " + stripTestName(test.name) + "   " + info.statusTitle + "</font></h3>" + endl);

            // check whether diagram gif and plots are available
            String diagramGif = null;
            if( ( new File(testDir + testURL + "-diagram.gif") ).exists() )
                diagramGif = htmlLinksPrefix + testURL + "-diagram.gif";

            boolean writePlot = shouldWritePlots(testURL);

            out.write("<pre> test              : " + stripTestName(test.name) + endl);
            out.write(" model             : ");
            out.write("<a href=\"" + getModelFilePath(test.name) + "\" target=\"_blank\">xml</a>, ");
            out.write("<a href=\"" + test.name + "\"( </a><a href=\"#model\">");
            if( diagramGif != null )
                out.write("diagram, ");
            out.write("description</a>" + endl);
            out.write(" simulation engine : " + test.solverName + endl);

            out.write(" generated code    : ");
            String correctLevel = ( sbmlLevel == null ) ? "???" : sbmlLevel.replaceAll("-", "_");
//            scriptName = "_" + stripTestName(test.name) + correctLevel + ".java";

//            if( scriptName == null )
//                out.write("code was not generated - model is static." + endl);
//            else
            String  javaCodePath = getJavaCodePath(test.name, correctLevel);
                out.write("<a href=\"" + getJavaCodePath(test.name, correctLevel) + "\" target=\"_blank\">" + new File(javaCodePath).getName() + "</a>" + endl);

            out.write(" results           : <a href=\"#results\">table with results</a>" + endl);
            if( writePlot )
                out.write(" plots             : </a><a href=\"#plots\">normal</a>" + endl); //, <a href=\"#plot_log\">log</a>
            out.write(" status            : " + info.statusTitle + endl);
            out.write(" simulation time   : " + test.simulationTime / 1000.0 + " s." + endl);
            out.write("</pre>" + endl);

            out.write(writeSimulationParameters(test.name));

            if( test.exception != null )
            {
                out.write("Error, stack trace:" + endl);
                out.write("<pre>" + endl);
                test.exception.printStackTrace(out);
                out.write("</pre>" + endl);
            }

            String levelFolderName = sbmlLevel.replace("-sbml-", "");

            out.write("You can run simulation using <a href=\"" + webEditionURL + levelFolderName + "/" + stripTestName(test.name) + sbmlLevel
                    + "\">BioUML web edition</a>");

            writeDiagramImages(out, test.name);
            out.write("<hr>" + endl);
            writeModelDescriptionBlock(out, testURL, diagramGif);
            out.write("<hr>" + endl);
            
            if( writePlot )
            {
                writePlotsBlock(out, testURL, test.solverName);
                out.write("<hr>" + endl);
            }
            // write table comparing the result of simulation
            // and the prescribed one

            out.print("<hr>" + endl);
            
            if( timeCourse )
            {
                writeSimulationResultTable(out, testURL);
            }
            else
            {
                writeSteadyStateResultTable(out, testURL);
            }
            writeLegendBlock(out);
        }
    }
    
    public String diagramFigsPath;
    protected void writeDiagramImages(PrintWriter out, String testName) throws Exception
    {
        if (diagramFigsPath == null)
            return;
        out.write("<title>BioUML diagram</title>" + endl);
        File figsDirectory = new File( diagramFigsPath );
        if( !figsDirectory.exists() || !figsDirectory.isDirectory() )
        {
            return;
        }
        testName = TextUtil2.split( testName, '/' )[0];
        File testFigs = new File( figsDirectory, testName );
        if( !testFigs.exists() || !testFigs.isDirectory() )
        {
            return;
        }

        Map<String, List<String>> figuresHierarchy = new HashMap<>();
        File figuresList = new File( testFigs, "figures.txt" );
        String start = null;
        try(BufferedReader br = ApplicationUtils.utfReader( figuresList ))
        {
            String line = br.readLine();
            if( line == null )
                throw new BiosoftParseException( new IllegalArgumentException( "Empty file" ), figuresList.getPath() );
            start = TextUtil2.split(line, '/')[0];
            while( line != null )
            {
                String[] fileNames = line.split("\t");
                String key = fileNames[0];

                if( key.length() < start.length() && !key.equals("processed") )
                {
                    start = key;
                }
                List<String> childFiles = new ArrayList<>();
                for( int i = 1; i < fileNames.length; i++ )
                {
                    childFiles.add(fileNames[i]);
                }
                figuresHierarchy.put(key, childFiles);
                line = br.readLine();
            }
        }
        out.write("<hr>" + endl);
        out.write("<b><font size = 4>BioUML diagrams</b></font><br>"+ endl );
        String str = createImageTable(start, figuresHierarchy, "../figs_out/" + testName + "/");
        out.write( str );
        
        if (figuresHierarchy.containsKey( "processed" ))
        {
            out.write("<hr>" + endl);
            List<String> processedNames = figuresHierarchy.get( "processed" );
            out.write("<b><font size = 4>Processed (plain) BioUML diagram</b></font><br>"+ endl );
            out.write(" <img border = \"1\" src=\"" + "../figs_out/" + testName + "/" + processedNames.get(0) + ".png\">" + endl );
        }
    }
    
    protected String createImageTable(String start, Map<String, List<String>> hierarchy, String path)
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "<table cellpadding =\"10\">" + endl );
        buf.append( "  <tr valign=\"top\" align=\"center\" >" + endl );
        buf.append( " <td width=\"20\"colspan = \""+hierarchy.get( start ).size()+"\">"+ endl);
        buf.append("<b><font size = 4>"+start.substring( start.lastIndexOf( "_" )+1 )+"</b></font><br>"+ endl );
        buf.append("    <img  border = \"1\" src=\"" + path + start + ".png\">" + endl );
       
        buf.append("    </td>" + endl );
        
        buf.append( "  <tr valign=\"top\" align=\"center\">" + endl );
        for (String child: hierarchy.get( start ))
        {
            buf.append( "  <td valign=\"top\" align=\"center\">" + endl );
            String childTable = createImageTable(child, hierarchy, path);
            buf.append( childTable );
           
        }
        buf.append( "</table>" + endl );
        return buf.toString();
    }

    protected String writeSimulationParameters(String testName)
    {
        String parameters = "<b>Parameters:</b><pre>";

        String postfix = ".test";
        if( newFlag )
            postfix = "-settings.txt";
        parameters += readFile(testDir + testName + postfix);
        parameters += "</pre>";
        return parameters;
    }

    protected String writeModelDescription(PrintWriter out, String testURL)
    {
        String generatedBy;
        String postfix = "";
        if( newFlag )
            postfix = "-model";
        String str = readFile(testDir + testURL + postfix + ".html");

        if( newFlag )
        {
            generatedBy = str.replaceAll("\n", "");
        }
        else
        {
            String descr = substring(str, "<table class=\"sbml-table\"", "<hr>");
            out.print(descr + endl);

            generatedBy = substring(str, "<hr>", "</div></body>");
            if( generatedBy.length() > 10 )
                generatedBy = generatedBy.substring(5);
        }

        return generatedBy;
    }

    protected void writeModelDescriptionBlock(PrintWriter out, String testURL, String diagramGif)
    {
        out.append("<a name=\"model\"><h3>SBML model&nbsp;</h3></a>" + endl);
        String generatedBy;
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
    }

    protected boolean shouldWritePlots(String testURL)
    {
        String postfix = ".GIF";
        if( newFlag )
            postfix = "-plot.jpg";

        return new File(testDir + testURL + postfix).exists();
    }

    protected void writePlotsBlock(PrintWriter out, String testURL, String simulatorName)
    {
        out.write("<a name=\"plots\"><h3>Plots</h3></a>" + endl);

        out.write("<p><i>Normal plot</i><br>" + endl);
        String postfix = ".GIF";
        if( newFlag )
            postfix = "-plot.jpg";
        out.write("<img src=\"" + htmlLinksPrefix + testURL + postfix + "\">" + endl);

        if( ( new File(testDir + testURL + "-log.GIF") ).exists() )
        {
            out.write("<p><a name=plot_log><i>Logarithmic plot</i><br>" + endl);
            out.write("<img src=\"" + htmlLinksPrefix + testURL + "-log.GIF\">" + endl);
        }
    }

    protected void writeSimulationResultTable(PrintWriter out, String testURL) throws Exception
    {
        out.print("<a name=\"results\"></a><h3>Simulation results (<a href=\"#results_legend\">legend</a>)</h3></a>" + endl);

        String postfix = "";
        if( newFlag )
        {
            postfix = "-results";
        }

        File csvFile = new File(testDir + testURL + postfix + ".csv");
        if( !csvFile.exists() )
        {
            out.write("<font color=red>CSV file absents, file=" + csvFile.getName() + ".</font><br>");
            return;
        }

        File bioumlCsvFile = new File(bioumlResultsDir + testURL + ".BioUML.csv");
        if( !bioumlCsvFile.exists() )
        {
            out.write("<font color=red>CSV file absents, file=" + bioumlCsvFile.getName() + ".</font><br>");
            return;
        }

        SbmlCSVHandler csvHandler = new SbmlCSVHandler(csvFile);
        SbmlCSVHandler bioumlCsvHandler = new SbmlCSVHandler(bioumlCsvFile);

        List<String> variableNames = bioumlCsvHandler.getVariableNames();

        int varCount = variableNames.size();

        double[] times = bioumlCsvHandler.getTimes();
        if( times == null )
        {
            out.print("<b>times == null</b>" + endl);
            return;
        }

        List<double[]> simulatedValues = bioumlCsvHandler.getVariableValues();
        if( simulatedValues == null )
        {
            out.print("<b>simulatedValues == null</b>" + endl);
            return;
        }

        out.print("<table border=\"1\">" + endl);
        out.print("<tr>");
        out.print("<td>time</td>");

        for( String name : variableNames )
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
                String varName = variableNames.get(i + 1);
                double csvValue = csvValues[csvHandler.getVariableNames().indexOf(varName)];
                double bioumlValue = simulatedValues.get(_time)[i + 1];
                out.print("<td>" + endl);
                out.print(csvValue);
                if( CalculateSemanticStatistics.significantlyDiffer(csvValue, bioumlValue, aTol, rTol) )
                {
                    out.print("<br><font color=\"#FF0000\">" + bioumlValue + "</font>");
                }
                else if( !CalculateSemanticStatistics.almostEqual(csvValue, bioumlValue, aTol, rTol) )
                {
                    out.print("<br><font color=\"#800000\">" + bioumlValue + "</font>");
                }
                else
                {
                    out.print("<br>" + bioumlValue);
                }
                out.print("</td>" + endl);
            }

            out.print("</tr>" + endl);
        }

        out.print("</table>" + endl);
    }
    
    protected void writeSteadyStateResultTable(PrintWriter out, String testURL) throws Exception
    {
        out.print("<a name=\"results\"></a><h3>Simulation results (<a href=\"#results_legend\">legend</a>)</h3></a>" + endl);

        String postfix = "";
        if( newFlag )
        {
            postfix = "-results";
        }

        File csvFile = new File(testDir + testURL + postfix + ".csv");
        if( !csvFile.exists() )
        {
            out.write("<font color=red>CSV file absents, file=" + csvFile.getName() + ".</font><br>");
            return;
        }

        File bioumlCsvFile = new File(bioumlResultsDir + testURL + ".BioUML.csv");
        if( !bioumlCsvFile.exists() )
        {
            out.write("<font color=red>CSV file absents, file=" + bioumlCsvFile.getName() + ".</font><br>");
            return;
        }

        SbmlCSVHandler csvHandler = new SbmlCSVHandler(csvFile, false);
        SbmlCSVHandler bioumlCsvHandler = new SbmlCSVHandler(bioumlCsvFile, false);

        List<String> variableNames = bioumlCsvHandler.getVariableNames();

        int varCount = variableNames.size();

        List<double[]> simulatedValues = bioumlCsvHandler.getVariableValues();
        if( simulatedValues == null )
        {
            out.print("<b>simulatedValues == null</b>" + endl);
            return;
        }
        out.print("<table border=\"1\">" + endl);
        out.print("<tr>");

        Iterator<String> iter = variableNames.iterator();
        while( iter.hasNext() )
        {
            String name = iter.next();
            if( !name.equals("time") )
                out.print("<td>" + name + "</td>");
        }
        out.print("</tr>");
       
        double[] csvValues = csvHandler.getVariableValues().get(0);
        double[] bioUMLValues = bioumlCsvHandler.getVariableValues().get(0);
        
            out.print("<tr>" + endl);
          
            for( int i = 0; i < varCount; i++ )
            {
                String varName = variableNames.get(i);
                double csvValue = csvValues[csvHandler.getVariableNames().indexOf(varName)];
                double bioumlValue = bioUMLValues[bioumlCsvHandler.getVariableNames().indexOf(varName)];
                out.print("<td>" + endl);
                out.print(csvValue);
                if( CalculateSemanticStatistics.significantlyDiffer(csvValue, bioumlValue, aTol, rTol) )
                {
                    out.print("<br><font color=\"#FF0000\">" + bioumlValue + "</font>");
                }
                else if( !CalculateSemanticStatistics.almostEqual(csvValue, bioumlValue, aTol, rTol) )
                {
                    out.print("<br><font color=\"#800000\">" + bioumlValue + "</font>");
                }
                else
                {
                    out.print("<br>" + bioumlValue);
                }
                out.print("</td>" + endl);
            }

            out.print("</tr>" + endl);

        out.print("</table>" + endl);
    }

    protected void writeLegendBlock(PrintWriter out)
    {
        out.write("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);
        out.write("each cell contains two values:<br>" + endl);
        out.write("&nbsp; -&nbsp; value that should be obtained accrding to CSV file;<br>" + endl);
        out.write("&nbsp; -&nbsp; value that were approximated for corresponding point by BioUML workbench simulation engine." + endl);

        out.write("<p>Color indicates difference between values:<br>" + endl);
        out.write("&nbsp; -&nbsp; simulation results satisfy SBML passing criterion<br>" + endl);
        out.write("&nbsp; -&nbsp; <font color=\"#800000\">relative error (&gt; SBML passing criterion allows and &lt;50%)</font><br>"
                + endl);

        out
                .write("&nbsp; -&nbsp; <font color=\"#FF0000\">simulation results don't satisfy SBML passing criterion and significantly differs (&gt;50%)</font></p>"
                        + endl);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utilities
    //

    public String stripTestName(String fullTestName)
    {
        int index = fullTestName.lastIndexOf('/');
        return index == -1 ? fullTestName : fullTestName.substring(index + 1, fullTestName.length());
    }

    public String readFile(String fileName)
    {
        try
        {
            File file = new File(fileName);
            return ApplicationUtils.readAsString(file);
        }
        catch( Exception e )
        {
            return ( "Can not read file '" + fileName + "', error: " + e );
        }
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

    protected Map<Integer, StatusInfo> statusMap;
    protected long initStatusMap(Iterator i, Map testMap)
    {
        statusMap = new TreeMap<>();

        put(Status.SUCCESSFULL, "Successfull", true, "ok", null);
        put(Status.FAILED, "Failed", true, "<font color=\"red\">failed</font>", "an exception has occured");

        put(Status.NUMERICALLY_WRONG, "Errors", true, "<font color=\"pink\">Error</font>",
                "simulation results significantly differ from the known ones");

        put(Status.NEEDS_TUNING, "Needs tuning", true, "<font color=\"orange\">Needs tuning</font>", "relative error is not small enough");

        put(Status.RESULT_DIFFER, "Result differs", true, "<font color=\"#800000\">result differs</font>",
                "some variable or time point is missing in simulation engine output.");

        put(Status.CSV_ERROR, "CSV error", true, "<font color=\"magenta\">CSV error</font>",
                "original CSV data is missing or can not be parsed");

        put(Status.PROBLEM_IS_STIFF, "Problem is stiff", true, "<font color=\"#006400\">Problem is stiff</font>",
                "Problem is too stiff for this solver");

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

    protected void put(int status, String columnTitle, boolean showColumn, String statusTitle, String description)
    {
        statusMap.put(status, new StatusInfo(status, columnTitle, showColumn, statusTitle, description));
    }

    protected List<Category> currentTestCategories;
    protected String currentTest;
    public String getCurrentTest()
    {
        return currentTest;
    }

    public void warn(String message)
    {
        this.messages += "; " + message;
    }

    public void error(int status, String message)
    {
        this.status = status;
        this.messages += message + "  ";
    }
}
