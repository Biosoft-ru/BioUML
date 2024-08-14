package biouml.plugins.sbml._test;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import biouml.plugins.sbml._test.TestStatistics.ResultComparison;
import biouml.plugins.sbml._test.TestStatistics.ResultRelativeErrors;
import biouml.plugins.sbml._test.TestStatistics.ResultComparison.Statistics;
import biouml.plugins.sbml._test.TestStatistics.ResultRelativeErrors.RelativeErrors;
import biouml.plugins.sbml._test.TestStatistics.ResultRelativeErrors.Values;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.standard.simulation.SimulationResult;

public class HtmlBiomodelsTestStatisticsLogger extends DefaultTestLogger
{
    protected static final String endl = System.getProperty("line.separator");

    private static int NO_DIFFERENCE = 0;
    private static int SMALL_DIFFERENCE = 1;
    private static int SIGNIFICANT_DIFFERENCE = 2;

    private final String summaryFileName;
    private final String summaryCompareFileName;
    private final String summaryCompareMedianesFileName;

    private TestStatistics statistics;

    private String scriptName;
    private String sbmlLevel;
    private OdeSimulationEngine simulationEngine;
    private double absTolerance;
    private double relTolerance;
    private double zero;
    private double timeIncrement;

    private SimulationResult simulationResult;

    private final Hashtable<String, TestProperty> testProperties = new Hashtable<>();
    private static class TestProperty
    {
        public String status;
        public String solver;
        public String atol;
        public String rtol;
        public String zero;
        public String tinc;
        public TestProperty(String status, String solver, String atol, String rtol, String zero, String tinc)
        {
            this.status = status;
            this.solver = solver;
            this.atol = atol;
            this.rtol = rtol;
            this.zero = zero;
            this.tinc = tinc;
        }
        public TestProperty()
        {
            this.status = "unknown";
            this.solver = "unknown";
            this.atol = "unknown";
            this.rtol = "unknown";
            this.zero = "unknown";
            this.tinc = "unknown";
        }
    }

    private final Map<TestDescription, ResultComparison> comparisons = new HashMap<>();
    private final Map<TestDescription, ResultRelativeErrors> relativeComparisons = new HashMap<>();

    public HtmlBiomodelsTestStatisticsLogger(String title, String summaryFileName, String summaryCompareFileName,
            String summaryCompareMedianesFileName)
    {
        super(title);
        this.summaryFileName = summaryFileName;
        this.summaryCompareFileName = summaryCompareFileName;
        this.summaryCompareMedianesFileName = summaryCompareMedianesFileName;
    }

    @Override
    public void testCompleted()
    {
        try
        {
            super.testCompleted();

            TestDescription testDescription = new TestDescription(null, currentTest, status, messages, exception, this.getSimulationTime());

            // write separate file with test details
            initStatusMap(null, null);

            writeDetailsFile(testDescription, statusMap.get(Integer.valueOf(status)));
            writeRelativeErrorsDetails(testDescription, statusMap.get(Integer.valueOf(status)));
            writeRelativeMedianesErrorsDetails(testDescription, statusMap.get(Integer.valueOf(status)));
            writeCSVFile(testDescription);

            testDescriptions.add(testDescription);
            testProperties.put(testDescription.name, new TestProperty(statusMap.get(Integer.valueOf(status)).statusTitle, simulationEngine
                    .getEngineDescription(), String.valueOf(absTolerance), String.valueOf(relTolerance), String.valueOf(zero), String
                    .valueOf(timeIncrement)));
        }
        catch( Throwable t )
        {
            t.printStackTrace();
        }
    }
    private void writeDetailsFile(TestDescription test, StatusInfo info) throws Exception
    {
        File outDir = new File(outDirectory + "/comparisons/");
        if( !outDir.exists() )
            outDir.mkdirs();

        try(PrintWriter out = new PrintWriter(new File(outDir, test.name + "-" + simulationEngine.getEngineDescription() + ".html"), "UTF-8"))
        {
            out.write("<html>" + endl);
            out.write("<head>" + endl);
    
            out.write("<title>Test " + test.name + "-" + simulationEngine.getEngineDescription() + " details</title>" + endl);
    
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
            out.write("td {" + endl);
            out.write("        text-align:center;" + endl);
            out.write("        border-top:thin solid black;" + endl);
            out.write("        border-bottom:thin solid black;" + endl);
            out.write("        border-left:thin solid black;" + endl);
            out.write("        border-right:thin solid black;" + endl);
            out.write("}" + endl);
            out.write("</style>" + endl);
    
            out.write("<script language=\"Javascript1.1\">" + endl);
            out.write("function hideColumn(column, value)" + endl);
            out.write("{" + endl);
            out.write("    var cells = document.getElementById(\"result\").getElementsByTagName(\"td\");" + endl);
            out.write("    var c  = column;" + endl);
            out.write("    var display = value ? \"\" : \"none\";" + endl);
            out.write("    for(var i=0; i<cells.length; i++) " + endl);
            out.write("    {" + endl);
            out.write("        if( cells[i].className == c )" + endl);
            out.write("        {" + endl);
            out.write("            cells[i].style.display = display;" + endl);
            out.write("        }" + endl);
            out.write("    }" + endl);
            out.write("}" + endl);
            out.write("function hideRow(row, value)" + endl);
            out.write("{" + endl);
            out.write("    var cells = document.getElementById(\"result\").getElementsByTagName(\"tr\");" + endl);
            out.write("    var display = value ? \"\" : \"none\";" + endl);
            out.write("    for(var i=0; i<cells.length; i++) " + endl);
            out.write("    {" + endl);
            out.write("        if( cells[i].className == row )" + endl);
            out.write("        {" + endl);
            out.write("            cells[i].style.display = display;" + endl);
            out.write("        }" + endl);
            out.write("    }" + endl);
            out.write("}" + endl);
            out.write("</script>" + endl);
    
    
            out.write("</head>" + endl);
    
            out.write("<body>" + endl);
            out.write("<h3>Test: " + test.name + ".test (" + simulationEngine.getEngineDescription() + ") - " + info.statusTitle + "</font></h3>"
                    + endl);
    
            // legend
            out.write("<hr>" + endl);
    
            out.write("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);
            out.write("each cell contains two values:<br>" + endl);
            out.write("&nbsp; -&nbsp; value that should be obtained accrding to CSV file;<br>" + endl);
            out.write("&nbsp; -&nbsp; value that were approximated for corresponding point by BioUML workbench simulation engine." + endl);
    
            out.write("<p>Color indicates difference between values:<br>" + endl);
            out.write("&nbsp; -&nbsp; no significant difference ( &lt; 0.1%)<br>" + endl);
            out.write("&nbsp; -&nbsp; <font color=\"#800000\">small relative error ( &gt; 0.1% and &lt;50%)</font><br>" + endl);
    
            out.write("&nbsp; -&nbsp; <font color=\"#FF0000\">simulation results significantly differ ( &gt;50%)</font></p>" + endl);
    
            // main table
            if( statistics != null && simulationResult != null )
            {
                ResultComparison comparison = statistics.getComparison(simulationResult, this);
                comparisons.put(test, comparison);
    
                //write header
                int dimension = statistics.getValuesCount();
                if( dimension > 0 )
                {
                    Map<String, Integer> variablesMap = simulationResult.getVariableMap();
                    String[] vars = new String[variablesMap.size()];
                    int index = 0;
                    for( String v : variablesMap.keySet() )
                    {
                        vars[index] = v;
                        index++;
                    }
                    out.write("<b>Variables:</b> " + endl);
    
                    for( String v : vars )
                    {
                        out.write("<input class=\"quickColumn\" type=\"checkbox\"  onClick=\"hideColumn('" + v + "', this.checked);\" checked>"
                                + v + "&nbsp;" + endl);
                    }
                    out.write("<br>" + endl);
                    out.write("<b>Simulators:</b> " + endl);
                    out
                            .write("<input class=\"quickColumn\" type=\"checkbox\"  onClick=\"hideRow('BioUML', this.checked);\" checked>BioUML&nbsp;"
                                    + endl);
                    Set<String> solvers = new HashSet<>();
                    for( Map.Entry<String, Statistics> e : comparison.getRow(0).entrySet() )
                    {
                        Statistics s = e.getValue();
                        solvers.addAll(s.values.keySet());
                    }
                    for( String v : solvers )
                    {
                        out.write("<input class=\"quickColumn\" type=\"checkbox\"  onClick=\"hideRow('" + v + "', this.checked);\" checked>"
                                + v + "&nbsp;" + endl);
                    }
                    out.write("<br>" + endl);
    
                    out.write("<table class=\"sbml-table\" id=\"result\">");
                    out.write("<tr>");
                    out.write("<td class=\"sbml-column-head\">time</td>");
                    out.write("<td class=\"sbml-column-head\">result</td>");
    
                    for( String v : vars )
                    {
                        out.write("<td class=\"" + v + "\" bgcolor=\"c0c0c0\">\n");
                        out.write(v);
                        out.write("</td>\n");
                    }
                    out.write("</tr>\n");
    
    
                    for( int i = 0; i < dimension; i++ )
                    {
                        Map<String, Statistics> m = comparison.getRow(i);
                        Set<String> results = new HashSet<>();
                        for( Map.Entry<String, Statistics> e : m.entrySet() )
                        {
                            Statistics s = e.getValue();
                            results.addAll(s.values.keySet());
                        }
    
                        out.write("<tr class=\"BioUML\">\n");
                        String time = null;
                        if( m.values().iterator().hasNext() )
                        {
                            time = String.valueOf(m.values().iterator().next().time);
                            time = time.substring(0, time.indexOf('.') + 2);
                        }
                        else
                        {
                            time = "unknown";
                        }
                        out.write("<td class=\"sbml-cell\" >" + time + "</td>\n");
    
                        out.write("<td class=\"sbml-cell\" bgcolor=\"#aaaaaa\">biouml</td>\n");
                        int ind = 0;
                        for( Map.Entry<String, Statistics> e : m.entrySet() )
                        {
                            Statistics s = e.getValue();
                            out.write("<td class=\"" + vars[ind] + "\" bgcolor=\"#aaaaaa\">" + s.bioumlValue + "</td>\n");
                            ind++;
                        }
                        out.write("</tr>\n");
    
                        for( String r : results )
                        {
                            out.write("<tr class=\"" + r + "\">\n");
    
                            out.write("<td class=\"sbml-cell\">" + time + "</td>");
    
                            // output other solver values
                            String color = getSolverColor(r);
                            out.write("<td class=\"sbml-cell\" bgcolor = \"" + color + "\">" + r + "</td>");
    
                            ind = 0;
                            for( Map.Entry<String, Statistics> e : m.entrySet() )
                            {
                                Statistics s = e.getValue();
                                out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\">");
                                Double v = s.values.get(r);
                                if( v != null )
                                    out.write(String.valueOf(v));
                                else
                                    out.write("-");
                                out.write("</td>");
                                ind++;
                            }
    
                            out.write("</tr>\n");
                        }
    
                        //  output min value
                        out.write("<tr>\n");
                        out.write("<td class=\"sbml-cell\">" + time + "</td>");
    
                        out.write("<td class=\"sbml-cell\">min</td>\n");
                        ind = 0;
                        for( Map.Entry<String, Statistics> e : m.entrySet() )
                        {
                            Statistics s = e.getValue();
                            out.write("<td class=\"" + vars[ind] + "\">" + s.min + "</td>\n");
                            ind++;
                        }
                        out.write("</tr>\n");
    
                        //  output max value
                        out.write("<tr>\n");
                        out.write("<td class=\"sbml-cell\">" + time + "</td>");
                        out.write("<td class=\"sbml-cell\">max</td>\n");
                        ind = 0;
                        for( Map.Entry<String, Statistics> e : m.entrySet() )
                        {
                            Statistics s = e.getValue();
                            out.write("<td class=\"" + vars[ind] + "\">" + s.max + "</td>\n");
                            ind++;
                        }
                        out.write("</tr>\n");
    
                        //  output mean value
                        out.write("<tr>\n");
                        out.write("<td class=\"sbml-cell\">" + time + "</td>");
                        out.write("<td class=\"sbml-cell\">mean</td>\n");
                        ind = 0;
                        for( Map.Entry<String, Statistics> e : m.entrySet() )
                        {
                            Statistics s = e.getValue();
                            out.write("<td class=\"" + vars[ind] + "\">" + s.mean + "</td>\n");
                            ind++;
                        }
                        out.write("</tr>\n");
    
                        //  output relative error
                        out.write("<tr>\n");
                        out.write("<td class=\"sbml-cell\">" + time + "</td>");
                        out.write("<td class=\"sbml-cell\">relative error(%)</td>\n");
                        ind = 0;
                        for( Map.Entry<String, Statistics> e : m.entrySet() )
                        {
                            Statistics s = e.getValue();
                            String color = getTextColor(s.relativeError);
    
                            out.write("<td class=\"" + vars[ind] + "\"><font color=\"" + color + "\"><b>" + s.relativeError
                                    + "</b></font></td>");
                            ind++;
                        }
                        out.write("</tr>\n");
                    }
    
                    out.write("</table>\n");
                }
            }
            else
            {
                comparisons.put(test, new ResultComparison());
            }
    
            out.write("</body>" + endl);
            out.write("</html>" + endl);
        }
    }

    private void writeRelativeErrorsDetails(TestDescription test, StatusInfo info) throws Exception
    {
        File outDir = new File(outDirectory + "/relativeErrors/");
        if( !outDir.exists() )
            outDir.mkdirs();

        try(PrintWriter out = new PrintWriter(new File(outDir, test.name + "-" + simulationEngine.getEngineDescription() + ".html"), "UTF-8"))
        {
            out.write("<html>" + endl);
            out.write("<head>" + endl);
    
            out.write("<title>Test " + test.name + "-" + simulationEngine.getEngineDescription() + " details</title>" + endl);
    
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
            out.write("        border-left:thin solid black;" + endl);
            out.write("        border-right:thin solid black;" + endl);
            out.write("        padding-left:5;" + endl);
            out.write("        padding-right:5;" + endl);
            out.write("}" + endl);
            out.write("td {" + endl);
            out.write("        text-align:center;" + endl);
            out.write("        border-top:thin solid black;" + endl);
            out.write("        border-bottom:thin solid black;" + endl);
            out.write("        border-left:thin solid black;" + endl);
            out.write("        border-right:thin solid black;" + endl);
            out.write("}" + endl);
            out.write("</style>" + endl);
    
            out.write("<script language=\"Javascript1.1\">" + endl);
            out.write("function hideColumn(column, value)" + endl);
            out.write("{" + endl);
            out.write("    var cells = document.getElementById(\"result\").getElementsByTagName(\"td\");" + endl);
            out.write("    var c  = column;" + endl);
            out.write("    var display = value ? \"\" : \"none\";" + endl);
            out.write("    for(var i=0; i<cells.length; i++) " + endl);
            out.write("    {" + endl);
            out.write("        if( cells[i].className == c )" + endl);
            out.write("        {" + endl);
            out.write("            cells[i].style.display = display;" + endl);
            out.write("        }" + endl);
            out.write("    }" + endl);
            out.write("}" + endl);
            out.write("function hideRow(row, value)" + endl);
            out.write("{" + endl);
            out.write("    var cells = document.getElementById(\"result\").getElementsByTagName(\"tr\");" + endl);
            out.write("    var display = value ? \"\" : \"none\";" + endl);
            out.write("    for(var i=0; i<cells.length; i++) " + endl);
            out.write("    {" + endl);
            out.write("        if( cells[i].className == row )" + endl);
            out.write("        {" + endl);
            out.write("            cells[i].style.display = display;" + endl);
            out.write("        }" + endl);
            out.write("    }" + endl);
            out.write("}" + endl);
            out.write("</script>" + endl);
    
            out.write("</head>" + endl);
    
            out.write("<body>" + endl);
            out.write("<h3>Test: " + test.name + ".test (" + simulationEngine.getEngineDescription() + ") - " + info.statusTitle + "</font></h3>"
                    + endl);
    
            // main table
            if( statistics != null )
            {
                ResultRelativeErrors relativeErrors = statistics.getRelativeErrors(simulationResult, this);
                if( relativeErrors != null )
                {
                    relativeComparisons.put(test, relativeErrors);
    
                    //write header
                    int dimension = statistics.getValuesCount();
                    if( dimension > 0 )
                    {
                        Map<String, Integer> variablesMap = statistics.getVariableNames();
                        String[] vars = new String[variablesMap.size()];
                        int index = 0;
                        for( String v : variablesMap.keySet() )
                        {
                            vars[index] = v;
                            index++;
                        }
                        out.write("<b>Variables:</b> " + endl);
    
                        for( String v : vars )
                        {
                            out.write("<input class=\"quickColumn\" type=\"checkbox\"  onClick=\"hideColumn('" + v
                                    + "', this.checked);\" checked>" + v + "&nbsp;" + endl);
                        }
                        out.write("<br>" + endl);
                        out.write("<b>Simulators:</b> " + endl);
                        Set<String> solvers = new HashSet<>();
                        for( Map.Entry<String, RelativeErrors> e : relativeErrors.getRow(0).entrySet() )
                        {
                            RelativeErrors s = e.getValue();
                            solvers.addAll(s.relativeErrors.keySet());
                        }
                        for( String v : solvers )
                        {
                            out.write("<input class=\"quickColumn\" type=\"checkbox\"  onClick=\"hideRow('" + v
                                    + "', this.checked);\" checked>" + v + "&nbsp;" + endl);
                        }
                        out.write("<br>" + endl);
    
                        out.write("<table class=\"sbml-table\" id=\"result\">");
                        out.write("<tr>");
                        out.write("<td class=\"sbml-column-head\" rowspan=2>time</td>");
                        out.write("<td class=\"sbml-column-head\" rowspan=2>simulation</td>");
    
                        for( String v : vars )
                        {
                            out.write("<td class=\"" + v + "\" bgcolor=\"c0c0c0\" colspan=2>\n");
                            out.write(v);
                            out.write("</td>\n");
                        }
                        out.write("</tr>\n");
    
                        for( String v : vars )
                        {
                            out.write("<td class=\"" + v + "\" bgcolor=\"c0c0c0\">var</td>\n");
                            out.write("<td class=\"" + v + "\" bgcolor=\"c0c0c0\">error</td>\n");
                        }
                        out.write("</tr>\n");
    
                        for( int i = 0; i < dimension; i++ )
                        {
                            Map<String, RelativeErrors> m = relativeErrors.getRow(i);
    
                            String time = null;
                            if( m.values().iterator().hasNext() )
                            {
                                time = String.valueOf(m.values().iterator().next().time);
                                time = time.substring(0, time.indexOf('.') + 2);
                            }
                            else
                            {
                                time = "unknown";
                            }
    
                            for( String r : solvers )
                            {
                                out.write("<tr class=\"" + r + "\">\n");
    
                                out.write("<td class=\"sbml-cell\" >" + time + "</td>\n");
    
                                String color = getSolverColor(r);
                                out.write("<td class=\"sbml-cell\" bgcolor = \"" + color + "\">" + r + "</td>");
    
                                int ind = 0;
                                for( Map.Entry<String, RelativeErrors> e : m.entrySet() )
                                {
                                    RelativeErrors s = e.getValue();
                                    Values values = null;
                                    if( s != null && s.relativeErrors != null && ( values = s.relativeErrors.get( r ) ) != null )
                                    {
                                        out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\">");
                                        double v = values.value;
                                        out.write(String.valueOf(v));
                                        double v2 = values.relativeError;
                                        out.write("</b></font></td>");
                                        out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\"><font color=\""
                                                + getTextColor(v2) + "\"><b>");
                                        out.write(String.valueOf(v2));
                                        out.write("</b></font></td>");
                                    }
                                    else
                                    {
                                        out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\"> - </td>");
                                        out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\"> - </td>");
                                    }
                                    ind++;
                                }
    
                                out.write("</tr>\n");
                            }
    
                        }
                        out.flush();
    
                        //summary
                        out.write("<tr>");
                        out.write("<td class=\"sbml-column-head\" colspan=2><b>Max relative error<b></td>");
    
                        int ind = 0;
                        for( String v : relativeErrors.getMaxByVariables().keySet() )
                        {
                            Double maxValue = relativeErrors.getMaxByVariables().get(v);
                            if( maxValue != null )
                            {
                                out.write("<td class=\"" + vars[ind] + "\" bgcolor=\"c0c0c0\" colspan=2><font color=\""
                                        + getTextColor(maxValue) + "\"><b>\n");
                                out.write(maxValue.toString());
                                out.write("</b></font></td>\n");
                            }
                            else
                            {
                                out.write("<td class=\"" + vars[ind] + "\" bgcolor=\"c0c0c0\" colspan=2> - </td>\n");
                            }
                            ind++;
                        }
                        out.write("</tr>\n");
                        out.write("</table>\n");
                    }
                }
            }
            else
            {
                relativeComparisons.put(test, new ResultRelativeErrors());
            }
    
            out.write("</body>" + endl);
            out.write("</html>" + endl);
        }
    }

    private void writeRelativeMedianesErrorsDetails(TestDescription test, StatusInfo info) throws Exception
    {
        File outDir = new File(outDirectory + "/relativeMedianesErrors/");
        if( !outDir.exists() )
            outDir.mkdirs();

        try(PrintWriter out = new PrintWriter(new File(outDir, test.name + "-" + simulationEngine.getEngineDescription() + ".html"), "UTF-8"))
        {
            out.write("<html>" + endl);
            out.write("<head>" + endl);

            out.write("<title>Test " + test.name + "-" + simulationEngine.getEngineDescription() + " details</title>" + endl);

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
            out.write("        border-left:thin solid black;" + endl);
            out.write("        border-right:thin solid black;" + endl);
            out.write("        padding-left:5;" + endl);
            out.write("        padding-right:5;" + endl);
            out.write("}" + endl);
            out.write("td {" + endl);
            out.write("        text-align:center;" + endl);
            out.write("        border-top:thin solid black;" + endl);
            out.write("        border-bottom:thin solid black;" + endl);
            out.write("        border-left:thin solid black;" + endl);
            out.write("        border-right:thin solid black;" + endl);
            out.write("}" + endl);
            out.write("</style>" + endl);

            out.write("<script language=\"Javascript1.1\">" + endl);
            out.write("function hideColumn(column, value)" + endl);
            out.write("{" + endl);
            out.write("    var cells = document.getElementById(\"result\").getElementsByTagName(\"td\");" + endl);
            out.write("    var c  = column;" + endl);
            out.write("    var display = value ? \"\" : \"none\";" + endl);
            out.write("    for(var i=0; i<cells.length; i++) " + endl);
            out.write("    {" + endl);
            out.write("        if( cells[i].className == c )" + endl);
            out.write("        {" + endl);
            out.write("            cells[i].style.display = display;" + endl);
            out.write("        }" + endl);
            out.write("    }" + endl);
            out.write("}" + endl);
            out.write("function hideRow(row, value)" + endl);
            out.write("{" + endl);
            out.write("    var cells = document.getElementById(\"result\").getElementsByTagName(\"tr\");" + endl);
            out.write("    var display = value ? \"\" : \"none\";" + endl);
            out.write("    for(var i=0; i<cells.length; i++) " + endl);
            out.write("    {" + endl);
            out.write("        if( cells[i].className == row )" + endl);
            out.write("        {" + endl);
            out.write("            cells[i].style.display = display;" + endl);
            out.write("        }" + endl);
            out.write("    }" + endl);
            out.write("}" + endl);
            out.write("</script>" + endl);

            out.write("</head>" + endl);

            out.write("<body>" + endl);
            out.write("<h3>Test: " + test.name + ".test (" + simulationEngine.getEngineDescription() + ") - " + info.statusTitle + "</font></h3>"
                    + endl);

            // main table
            if( statistics != null )
            {
                ResultRelativeErrors relativeErrors = statistics.getRelativeErrors(simulationResult, this);
                if( relativeErrors != null )
                {
                    relativeComparisons.put(test, relativeErrors);

                    //write header
                    int dimension = statistics.getValuesCount();
                    if( dimension > 0 )
                    {
                        Map<String, Integer> variablesMap = statistics.getVariableNames();
                        String[] vars = new String[variablesMap.size()];
                        int index = 0;
                        for( String v : variablesMap.keySet() )
                        {
                            vars[index] = v;
                            index++;
                        }
                        out.write("<b>Variables:</b> " + endl);

                        for( String v : vars )
                        {
                            out.write("<input class=\"quickColumn\" type=\"checkbox\"  onClick=\"hideColumn('" + v
                                    + "', this.checked);\" checked>" + v + "&nbsp;" + endl);
                        }
                        out.write("<br>" + endl);
                        out.write("<b>Simulators:</b> " + endl);
                        Set<String> solvers = new HashSet<>();
                        for( Map.Entry<String, RelativeErrors> e : relativeErrors.getRow(0).entrySet() )
                        {
                            RelativeErrors s = e.getValue();
                            solvers.addAll(s.relativeErrors.keySet());
                        }
                        for( String v : solvers )
                        {
                            out.write("<input class=\"quickColumn\" type=\"checkbox\"  onClick=\"hideRow('" + v
                                    + "', this.checked);\" checked>" + v + "&nbsp;" + endl);
                        }
                        out.write("<br>" + endl);

                        out.write("<table class=\"sbml-table\" id=\"result\">");
                        out.write("<tr>");
                        out.write("<td class=\"sbml-column-head\" rowspan=2>time</td>");
                        out.write("<td class=\"sbml-column-head\" rowspan=2>simulation</td>");

                        for( String v : vars )
                        {
                            out.write("<td class=\"" + v + "\" bgcolor=\"c0c0c0\" colspan=2>\n");
                            out.write(v);
                            out.write("</td>\n");
                        }
                        out.write("</tr>\n");

                        for( String v : vars )
                        {
                            out.write("<td class=\"" + v + "\" bgcolor=\"c0c0c0\">var</td>\n");
                            out.write("<td class=\"" + v + "\" bgcolor=\"c0c0c0\">error</td>\n");
                        }
                        out.write("</tr>\n");

                        for( int i = 0; i < dimension; i++ )
                        {
                            Map<String, RelativeErrors> m = relativeErrors.getRow(i);

                            String time = null;
                            if( m.values().iterator().hasNext() )
                            {
                                time = String.valueOf(m.values().iterator().next().time);
                                time = time.substring(0, time.indexOf('.') + 2);
                            }
                            else
                            {
                                time = "unknown";
                            }

                            for( String r : solvers )
                            {
                                out.write("<tr class=\"" + r + "\">\n");

                                out.write("<td class=\"sbml-cell\" >" + time + "</td>\n");

                                String color = getSolverColor(r);
                                out.write("<td class=\"sbml-cell\" bgcolor = \"" + color + "\">" + r + "</td>");

                                int ind = 0;
                                for( Map.Entry<String, RelativeErrors> e : m.entrySet() )
                                {
                                    RelativeErrors s = e.getValue();
                                    Values values = null;
                                    if( s != null && s.relativeErrors != null && ( values = s.relativeErrors.get( r ) ) != null )
                                    {
                                        out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\">");
                                        double v = values.value;
                                        out.write(String.valueOf(v));
                                        double v2 = values.relativeMedianesError;
                                        out.write("</b></font></td>");
                                        out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\"><font color=\""
                                                + getTextColor(v2) + "\"><b>");
                                        out.write(String.valueOf(v2));
                                        out.write("</b></font></td>");
                                    }
                                    else
                                    {
                                        out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\"> - </td>");
                                        out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\"> - </td>");
                                    }
                                    ind++;
                                }

                                out.write("</tr>\n");
                            }

                        }
                        out.flush();

                        out.write("</tr>\n");
                        out.write("</table>\n");
                    }
                }
            }

            out.write("</body>" + endl);
            out.write("</html>" + endl);
        }
    }

    private void writeCSVFile(TestDescription test)
    {
        File outDir = new File(outDirectory + "/csvResults/");
        if( !outDir.exists() )
            outDir.mkdirs();

        if( statistics != null && simulationResult != null )
        {
            File outputFile = new File(outDir, test.name + ".BioUML.csv");
            SbmlCSVHander handler = new SbmlCSVHander();

            Map<String, Integer> variablesMap = simulationResult.getVariableMap();
            String[] vars = new String[variablesMap.size()];
            int index = 0;
            for( String v : variablesMap.keySet() )
            {
                vars[index] = v;
                index++;
            }
            List<String> varList = new ArrayList<>();
            List<String> fullVarList = new ArrayList<>();
            fullVarList.add("time");
            for( String v : vars )
            {
                varList.add(v);
                fullVarList.add(v);
            }
            handler.setVariableNames(fullVarList);

            List<double[]> values = new ArrayList<>();
            double[] times = new double[1001];
            double t = 0.0;
            for( int i = 0; i < times.length - 1; i++ )
            {
                times[i] = t;
                t += 0.01;
            }
            times[1000] = 9.999;
            try
            {
                List<double[]> tmpValues = statistics.getInterpolatedValues(simulationResult, times, varList);
                int ind = 0;
                for( double[] line : tmpValues )
                {
                    double[] newLine = new double[line.length + 1];
                    newLine[0] = times[ind];
                    System.arraycopy(line, 0, newLine, 1, line.length);
                    values.add(newLine);
                    ind++;
                }
            }
            catch( Exception e )
            {

            }
            handler.setVariableValues(values);

            handler.writeCSVFile(outputFile);
        }
    }

    @Override
    public void complete()
    {
        try
        {
            try(PrintWriter out = new PrintWriter(summaryFileName, "UTF-8"))
            {
                out.write("<html>" + endl);
                out.write("<head>" + endl);
                out.write("<title>Summary statistics</title>" + endl);
                out.write("</head>" + endl);
                out.write("<body>" + endl);

                writeTestStatistics(out);

                out.write("</body>" + endl);
                out.write("</html>" + endl);
            }

            try(PrintWriter out2 = new PrintWriter(summaryCompareFileName, "UTF-8"))
            {
                out2.write("<html>" + endl);
                out2.write("<head>" + endl);
                out2.write("<title>Summary statistics</title>" + endl);
                out2.write("</head>" + endl);
                out2.write("<body>" + endl);

                writeTestCompareStatistics(out2);

                out2.write("</body>" + endl);
                out2.write("</html>" + endl);
            }

            try(PrintWriter out3 = new PrintWriter(summaryCompareMedianesFileName, "UTF-8"))
            {
                out3.write("<html>" + endl);
                out3.write("<head>" + endl);
                out3.write("<title>Summary statistics</title>" + endl);
                out3.write("</head>" + endl);
                out3.write("<body>" + endl);

                writeTestCompareMedianesStatistics(out3);

                out3.write("</body>" + endl);
                out3.write("</html>" + endl);
            }
        }
        catch( Throwable t )
        {
            t.printStackTrace();
        }
    }

    private void writeTestStatistics(PrintWriter out)
    {
        out.write("<table cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">");
        out.write("<tr>");
        out.write("<td valign=\"top\" >Test</td>");
        out.write("<td valign=\"top\" >Status</td>");
        out.write("<td valign=\"top\" >Maximum relative error</td>");
        out.write("<td valign=\"top\" >Time</td>");
        out.write("<td valign=\"top\" >Solver</td>");
        out.write("<td valign=\"top\" >ATOL, RTOL, ZERO, TINC</td>");
        out.write("</tr>");
        Set<Entry<TestDescription, ResultComparison>> entrySet = comparisons.entrySet();
        Entry<TestDescription, ResultComparison>[] eArray = entrySet.toArray(new Entry[entrySet.size()]);
        Arrays.sort(eArray, (e1, e2) -> {
            int n1, n2;
            try
            {
                n1 = Integer.parseInt(e1.getKey().name.replaceAll("\\.xml", "").replaceAll("BIOMD0+", ""));
                n2 = Integer.parseInt(e2.getKey().name.replaceAll("\\.xml", "").replaceAll("BIOMD0+", ""));
            }
            catch( Exception e )
            {
                return 0;
            }
            return n1 < n2 ? -1 : ( n1 == n2 ? 0 : 1 );
        });


        for( Map.Entry<TestDescription, ResultComparison> e : eArray )
        {
            TestProperty testProperty = testProperties.get(e.getKey().name);
            if( testProperty == null )
                testProperty = new TestProperty();
            out.write("<tr>");
            out.write("<td><a href=\"comparisons/" + e.getKey().name + "-" + testProperty.solver + ".html\">" + e.getKey().name
                    + "</a></td>");

            out.write("<td>" + testProperty.status + "</td>");

            double maxRelativeError = e.getValue().getMaxRelativeError();
            String color = getTextColor(maxRelativeError);
            out.write("<td><font color=\"" + color + "\">" + maxRelativeError + "</font></td>");

            out.write("<td>" + ( (float)e.getKey().simulationTime / 1000 ) + "s. </font></td>");

            out.write("<td>" + testProperty.solver + "</td>");

            out
                    .write("<td>" + testProperty.atol + ", " + testProperty.rtol + ", " + testProperty.zero + ", " + testProperty.tinc
                            + "</td>");

            out.write("</tr>");
        }
        out.write("</table>");
    }

    private void writeTestCompareStatistics(PrintWriter out)
    {
        out.write("<table cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">");
        out.write("<tr>");
        out.write("<td valign=\"top\" >Test</td>");
        out.write("<td valign=\"top\" >BioUML</td>");
        for( String solver : SbmlTestSimulationEngineAdapter.SOLVER_EXTENSIONS )
        {
            out.write("<td valign=\"top\" >" + solver + "</td>");
        }
        out.write("</tr>");
        Set<Entry<TestDescription, ResultRelativeErrors>> entrySet = relativeComparisons.entrySet();
        Entry<TestDescription, ResultRelativeErrors>[] eArray = entrySet.toArray(new Entry[entrySet.size()]);
        Arrays.sort(eArray, (e1, e2) -> {
            int n1, n2;
            try
            {
                n1 = Integer.parseInt(e1.getKey().name.replaceAll("\\.xml", "").replaceAll("BIOMD0+", ""));
                n2 = Integer.parseInt(e2.getKey().name.replaceAll("\\.xml", "").replaceAll("BIOMD0+", ""));
            }
            catch( Exception e )
            {
                return 0;
            }
            return n1 < n2 ? -1 : ( n1 == n2 ? 0 : 1 );
        });


        String allSolvers[] = new String[SbmlTestSimulationEngineAdapter.SOLVER_EXTENSIONS.length + 1];
        allSolvers[0] = "BioUML";
        System.arraycopy(SbmlTestSimulationEngineAdapter.SOLVER_EXTENSIONS, 0, allSolvers, 1,
                SbmlTestSimulationEngineAdapter.SOLVER_EXTENSIONS.length);

        int passed[] = new int[allSolvers.length];
        int noDifference[] = new int[allSolvers.length];
        int smallDifference[] = new int[allSolvers.length];
        int significantDifference[] = new int[allSolvers.length];

        for( Map.Entry<TestDescription, ResultRelativeErrors> e : eArray )
        {
            TestProperty testProperty = testProperties.get(e.getKey().name);
            if( testProperty == null )
                testProperty = new TestProperty();
            out.write("<tr>");
            out.write("<td><a href=\"relativeErrors/" + e.getKey().name + "-" + testProperty.solver + ".html\">" + e.getKey().name
                    + "</a></td>");

            ResultRelativeErrors resultComparison = e.getValue();
            Set<String> solvers = new HashSet<>();

            Map<String, RelativeErrors> m = resultComparison.getRow(0);
            if( m != null )
            {
                for( Map.Entry<String, RelativeErrors> map : m.entrySet() )
                {
                    RelativeErrors s = map.getValue();
                    solvers.addAll(s.relativeErrors.keySet());
                }
            }

            for( int i = 0; i < allSolvers.length; i++ )
            {
                boolean hasResult = false;
                if( solvers.size() != 0 )
                {
                    if( solvers.contains(allSolvers[i]) )
                        hasResult = true;
                    if( i == 0 && testProperty.status.equals(statusMap.get(Integer.valueOf(Status.FAILED)).statusTitle) )
                        hasResult = false;
                    if( hasResult )
                    {
                        passed[i]++;
                        Double max = resultComparison.getMaxBySolvers().get(allSolvers[i]);
                        int type = getResultType(max);
                        if( type == NO_DIFFERENCE )
                        {
                            noDifference[i]++;
                        }
                        else if( type == SMALL_DIFFERENCE )
                        {
                            smallDifference[i]++;
                        }
                        else
                        {
                            significantDifference[i]++;
                        }
                        out.write("<td><font color=\"" + getTextColor(max.doubleValue()) + "\">" + max + "</font></td>");
                    }
                    else
                    {
                        out.write("<td><font color=\"black\">-</font></td>");
                    }
                }
                else
                {
                    if( i == 0 )
                    {
                        out.write("<td><font color=\"black\">-</font></td>");
                    }
                    else
                    {
                        passed[i]++;
                        out.write("<td><font color=\"black\">???</font></td>");
                    }
                }
            }
            out.write("</tr>");
        }

        out.write("<tr>");
        out.write("<td colspan=" + ( allSolvers.length + 1 ) + " ><b>TOTAL:<b></td>");
        out.write("</tr>");

        //passed
        out.write("<tr>");
        out.write("<td><b>passed:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            out.write("<td>" + passed[i] + "</td>");
        }
        out.write("</tr>");

        // no difference
        out.write("<tr>");
        out.write("<td><b>no difference:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            out.write("<td>" + noDifference[i] + "</td>");
        }
        out.write("</tr>");

        // small difference
        out.write("<tr>");
        out.write("<td><b>small difference:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            out.write("<td>" + smallDifference[i] + "</td>");
        }
        out.write("</tr>");

        // significant difference
        out.write("<tr>");
        out.write("<td><b>significant difference:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            out.write("<td>" + significantDifference[i] + "</td>");
        }
        out.write("</tr>");

        out.write("</table>");
    }

    private void writeTestCompareMedianesStatistics(PrintWriter out)
    {
        out.write("<table cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">");
        out.write("<tr>");
        out.write("<td valign=\"top\" >Test</td>");
        out.write("<td valign=\"top\" >BioUML</td>");
        for( String solver : SbmlTestSimulationEngineAdapter.SOLVER_EXTENSIONS )
        {
            out.write("<td valign=\"top\" >" + solver + "</td>");
        }
        out.write("</tr>");
        Set<Entry<TestDescription, ResultRelativeErrors>> entrySet = relativeComparisons.entrySet();
        Entry<TestDescription, ResultRelativeErrors>[] eArray = entrySet.toArray(new Entry[entrySet.size()]);
        Arrays.sort(eArray, (e1, e2) -> {
            int n1, n2;
            try
            {
                n1 = Integer.parseInt(e1.getKey().name.replaceAll("\\.xml", "").replaceAll("BIOMD0+", ""));
                n2 = Integer.parseInt(e2.getKey().name.replaceAll("\\.xml", "").replaceAll("BIOMD0+", ""));
            }
            catch( Exception e )
            {
                return 0;
            }
            return n1 < n2 ? -1 : ( n1 == n2 ? 0 : 1 );
        });


        String allSolvers[] = new String[SbmlTestSimulationEngineAdapter.SOLVER_EXTENSIONS.length + 1];
        allSolvers[0] = "BioUML";
        System.arraycopy(SbmlTestSimulationEngineAdapter.SOLVER_EXTENSIONS, 0, allSolvers, 1,
                SbmlTestSimulationEngineAdapter.SOLVER_EXTENSIONS.length);

        int passed[] = new int[allSolvers.length];
        int noDifference[] = new int[allSolvers.length];
        int smallDifference[] = new int[allSolvers.length];
        int significantDifference[] = new int[allSolvers.length];

        for( Map.Entry<TestDescription, ResultRelativeErrors> e : eArray )
        {
            TestProperty testProperty = testProperties.get(e.getKey().name);
            if( testProperty == null )
                testProperty = new TestProperty();
            out.write("<tr>");
            out.write("<td><a href=\"relativeMedianesErrors/" + e.getKey().name + "-" + testProperty.solver + ".html\">" + e.getKey().name
                    + "</a></td>");

            ResultRelativeErrors resultComparison = e.getValue();
            Set<String> solvers = new HashSet<>();

            Map<String, RelativeErrors> m = resultComparison.getRow(0);
            if( m != null )
            {
                for( Map.Entry<String, RelativeErrors> map : m.entrySet() )
                {
                    RelativeErrors s = map.getValue();
                    solvers.addAll(s.relativeErrors.keySet());
                }
            }

            for( int i = 0; i < allSolvers.length; i++ )
            {
                boolean hasResult = false;
                if( solvers.size() != 0 )
                {
                    if( solvers.contains(allSolvers[i]) )
                        hasResult = true;
                    if( i == 0 && testProperty.status.equals(statusMap.get(Integer.valueOf(Status.FAILED)).statusTitle) )
                        hasResult = false;
                    if( hasResult )
                    {
                        passed[i]++;
                        Double max = resultComparison.getMaxByMedianesBySolvers().get(allSolvers[i]);
                        int type = getResultType(max);
                        if( type == NO_DIFFERENCE )
                        {
                            noDifference[i]++;
                        }
                        else if( type == SMALL_DIFFERENCE )
                        {
                            smallDifference[i]++;
                        }
                        else
                        {
                            significantDifference[i]++;
                        }
                        out.write("<td><font color=\"" + getTextColor(max.doubleValue()) + "\">" + max + "</font></td>");
                    }
                    else
                    {
                        out.write("<td><font color=\"black\">-</font></td>");
                    }
                }
                else
                {
                    if( i == 0 )
                    {
                        out.write("<td><font color=\"black\">-</font></td>");
                    }
                    else
                    {
                        passed[i]++;
                        out.write("<td><font color=\"black\">???</font></td>");
                    }
                }
            }
            out.write("</tr>");
        }

        out.write("<tr>");
        out.write("<td colspan=" + ( allSolvers.length + 1 ) + " ><b>TOTAL:<b></td>");
        out.write("</tr>");

        //passed
        out.write("<tr>");
        out.write("<td><b>passed:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            out.write("<td>" + passed[i] + "</td>");
        }
        out.write("</tr>");

        // no difference
        out.write("<tr>");
        out.write("<td><b>no difference:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            out.write("<td>" + noDifference[i] + "</td>");
        }
        out.write("</tr>");

        // small difference
        out.write("<tr>");
        out.write("<td><b>small difference:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            out.write("<td>" + smallDifference[i] + "</td>");
        }
        out.write("</tr>");

        // significant difference
        out.write("<tr>");
        out.write("<td><b>significant difference:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            out.write("<td>" + significantDifference[i] + "</td>");
        }
        out.write("</tr>");

        out.write("</table>");
    }


    private String getSolverColor(String solver)
    {
        int i = 0;
        for( String s : SbmlTestSimulationEngineAdapter.SOLVER_EXTENSIONS )
        {
            if( s.equals(solver) )
            {
                break;
            }
            i++;
        }
        if( i < SbmlTestSimulationEngineAdapter.SOLVER_COLORS.length )
        {
            return SbmlTestSimulationEngineAdapter.SOLVER_COLORS[i];
        }
        return "white";
    }

    private String getTextColor(double value)
    {
        int type = getResultType(value);
        if( type == NO_DIFFERENCE )
        {
            return "black";
        }
        else if( type == SMALL_DIFFERENCE )
        {
            return "#800000";
        }
        else
        {
            return "#ff0000";
        }
    }

    private int getResultType(double value)
    {
        if( value < 1.0 )
        {
            return NO_DIFFERENCE;
        }
        else if( value <= 50.0 )
        {
            return SMALL_DIFFERENCE;
        }
        else
        {
            return SIGNIFICANT_DIFFERENCE;
        }
    }

    public void setStatistics(TestStatistics statistics)
    {
        this.statistics = statistics;
    }

    public void setScriptName(String scriptName)
    {
        this.scriptName = scriptName;
    }

    private String outDirectory;
    public String getOutDirectory()
    {
        return outDirectory;
    }

    public void setOutDirectory(String outDirectory)
    {
        this.outDirectory = outDirectory;
    }

    public void setSbmlLevel(String sbmlLevel)
    {
        this.sbmlLevel = sbmlLevel;
    }

    public void setSimulationResult(SimulationResult simulationResult)
    {
        this.simulationResult = simulationResult;
    }

    public void setSimulationEngine(OdeSimulationEngine simulationEngine)
    {
        this.simulationEngine = simulationEngine;
        this.absTolerance = simulationEngine.getAbsTolerance();
        this.relTolerance = simulationEngine.getRelTolerance();
        this.timeIncrement = simulationEngine.getTimeIncrement();
    }

    public void setZero(double zero)
    {
        this.zero = zero;
    }
}
