package biouml.plugins.simulation_test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import one.util.streamex.StreamEx;

import ru.biosoft.util.NumberFormatter;
import ru.biosoft.util.NumberFormatter.FormatInterval;

public class StatisticsLogger
{
    protected static final String endl = System.getProperty("line.separator");

    protected String outDirectory;
    protected String csvDirectory;
    protected String xmlDirectory;
    protected String javaDirectory;

    private static int NO_DIFFERENCE = 0;
    private static int SMALL_DIFFERENCE = 1;
    private static int SIGNIFICANT_DIFFERENCE = 2;

    public static final String GRAPH_LINK_TEMPLATE = "http://www.ebi.ac.uk/compneur-srv/biomodels/models-main/publ/";

    PrintWriter summary1 = null;
    PrintWriter summary2 = null;
    PrintWriter summary3 = null;

    StringBuffer summary1_table = null;
    StringBuffer summary2_table = null;
    StringBuffer summary3_table = null;

    protected NumberFormatter errorsNumberFormatter;
    protected NumberFormatter valuesNumberFormatter;

    public StatisticsLogger(String outDirectory, String csvDirectory, String xmlDirectory, String javaDirectory)
    {
        this.outDirectory = outDirectory;
        this.csvDirectory = csvDirectory;
        this.xmlDirectory = xmlDirectory;
        this.javaDirectory = javaDirectory;
    }

    protected String allSolvers[];
    protected int passed_2[];
    protected int noDifference_2[];
    protected int smallDifference_2[];
    protected int significantDifference_2[];

    protected int passed_3[];
    protected int noDifference_3[];
    protected int smallDifference_3[];
    protected int significantDifference_3[];

    protected int testCount = 0;
    protected int successfulTestCount = 0;
    protected int failedTestCount = 0;
    protected int errorTestCount = 0;
    protected int needstuningTestCount = 0;

    protected long simulationTime = 0;

    public void start()
    {
        try
        {
            errorsNumberFormatter = new NumberFormatter(new FormatInterval[] {new FormatInterval(0.0, 1.0, "0.00"),
                    new FormatInterval(1.0, 100.0, "##.0"), new FormatInterval(100.0, 1000.0, "###"),
                    new FormatInterval(1000.0, Double.MAX_VALUE, "0.00E0")});
            valuesNumberFormatter = new NumberFormatter(new FormatInterval[] {new FormatInterval( -Double.MAX_VALUE, Double.MAX_VALUE,
                    "0.00000E0")});

            allSolvers = new String[CalculateBiomodelsStatistics.SOLVER_EXTENSIONS.length + 1];
            allSolvers[0] = "BioUML";
            System.arraycopy(CalculateBiomodelsStatistics.SOLVER_EXTENSIONS, 0, allSolvers, 1,
                    CalculateBiomodelsStatistics.SOLVER_EXTENSIONS.length);

            summary1 = new PrintWriter(outDirectory + "BioUML_summary_results.html", "UTF-8");
            summary1.write("<html>" + endl);
            summary1.write("<head>" + endl);
            summary1.write("<title>Summary statistics</title>" + endl);
            summary1.write("</head>" + endl);
            summary1.write("<body>" + endl);
            summary1_table = new StringBuffer();
            summary1_table.append("<h3> Details </h3>");
            //          legend
            summary1_table.append("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);
            summary1_table.append("<p>&nbsp; -&nbsp;<i>Maximum relative error</i>: maximum of relative errors by all variables<br>" + endl);
            summary1_table
                    .append("&nbsp; -&nbsp;<i>Relative error</i> =  Min(Abs( ( min - x ) / min), Abs( ( x - max ) / max)) * 100.0<br>"
                            + endl);
            summary1_table.append("</p>" + endl);
            summary1_table.append("<table cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">");
            summary1_table.append("<tr>");
            summary1_table.append("<td valign=\"top\" >Test</td>");
            summary1_table.append("<td valign=\"top\" >Status</td>");
            summary1_table.append("<td valign=\"top\" >Maximum relative error</td>");
            summary1_table.append("<td valign=\"top\" >Time</td>");
            summary1_table.append("<td valign=\"top\" >Solver</td>");
            summary1_table.append("<td valign=\"top\" >ATOL, RTOL, ZERO, STEP</td>");
            summary1_table.append("</tr>");

            summary2 = new PrintWriter(outDirectory + "AllSolvers_summary_results.html", "UTF-8");
            summary2.write("<html>" + endl);
            summary2.write("<head>" + endl);
            summary2.write("<title>Summary statistics</title>" + endl);
            summary2.write("</head>" + endl);
            summary2.write("<body>" + endl);
            summary2_table = new StringBuffer();
            summary2_table.append("<table cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">");
            summary2_table.append("<tr>");
            summary2_table.append("<td valign=\"top\" >Test</td>");
            summary2_table.append("<td valign=\"top\" >BioUML</td>");
            for( String solver : CalculateBiomodelsStatistics.SOLVER_EXTENSIONS )
            {
                summary2_table.append("<td valign=\"top\" >" + solver + "</td>");
            }
            summary2_table.append("</tr>");

            passed_2 = new int[allSolvers.length];
            noDifference_2 = new int[allSolvers.length];
            smallDifference_2 = new int[allSolvers.length];
            significantDifference_2 = new int[allSolvers.length];

            summary3 = new PrintWriter(outDirectory + "AllSolvers_summary_results(medianes).html", "UTF-8");
            summary3.write("<html>" + endl);
            summary3.write("<head>" + endl);
            summary3.write("<title>Summary statistics</title>" + endl);
            summary3.write("</head>" + endl);
            summary3.write("<body>" + endl);
            summary3_table = new StringBuffer();
            summary3_table.append("<table cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">");
            summary3_table.append("<tr>");
            summary3_table.append("<td valign=\"top\" >Test</td>");
            summary3_table.append("<td valign=\"top\" >BioUML</td>");
            for( String solver : CalculateBiomodelsStatistics.SOLVER_EXTENSIONS )
            {
                summary3_table.append("<td valign=\"top\" >" + solver + "</td>");
            }
            summary3_table.append("</tr>");

            passed_3 = new int[allSolvers.length];
            noDifference_3 = new int[allSolvers.length];
            smallDifference_3 = new int[allSolvers.length];
            significantDifference_3 = new int[allSolvers.length];
        }
        catch( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    public void end()
    {
        summary1_table.append("</table>");
        writeSummaryToStatistics1();
        summary1.write(summary1_table.toString());
        summary1.write("</body>" + endl);
        summary1.write("</html>" + endl);
        summary1.close();

        writeSummaryToRelativeSummary();
        summary2.write("</table>" + endl);
        summary2.write("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);
        summary2.write("passed &nbsp; -&nbsp; total number of results;<br>" + endl);
        summary2
                .write("no difference &nbsp; -&nbsp; (( x >=  min * 0.999  ) and ( x <= max * 1.001 ))  or x < ZERO and (min < ZERO || max < ZERO);<br>"
                        + endl);
        summary2.write("small difference &nbsp; -&nbsp; (x >= min * 0.5) and (x <= max * 1.5);<br>" + endl);
        summary2.write("significant difference &nbsp; -&nbsp; otherwise;<br>" + endl);
        summary2.write("<h3> Details </h3>" + endl);
        //          legend
        summary2.write("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);
        summary2.write("<p>Text color indicates difference between values:<br>" + endl);
        summary2.write("&nbsp; -&nbsp; no significant difference ( &lt; 0.1%)<br>" + endl);
        summary2.write("&nbsp; -&nbsp; <font color=\"#800000\">small relative error ( &gt; 0.1% and &lt;50%)</font><br>" + endl);
        summary2.write("&nbsp; -&nbsp; <font color=\"#FF0000\">simulation results significantly differ ( &gt;50%)</font></p>" + endl);
        summary2.write(summary2_table.toString());
        summary2.write("</body>" + endl);
        summary2.write("</html>" + endl);
        summary2.close();

        writeSummaryToMedianesSummary();
        summary3.write("</table>" + endl);
        summary3.write("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);
        summary3.write("passed &nbsp; -&nbsp; total number of results;<br>" + endl);
        summary3.write("no difference &nbsp; -&nbsp; Abs( (x-median)/median ) < 0.01  or x < ZERO and median < ZERO;<br>" + endl);
        summary3.write("small difference &nbsp; -&nbsp; Abs( (x- median)/median ) < 0.5;<br>" + endl);
        summary3.write("significant difference &nbsp; -&nbsp; otherwise;<br>" + endl);
        summary3.write("<h3> Details </h3>" + endl);
        //          legend
        summary3.write("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);
        summary3.write("<p>Text color indicates difference between values:<br>" + endl);
        summary3.write("&nbsp; -&nbsp; no significant difference ( &lt; 0.1%)<br>" + endl);
        summary3.write("&nbsp; -&nbsp; <font color=\"#800000\">small relative error ( &gt; 0.1% and &lt;50%)</font><br>" + endl);
        summary3.write("&nbsp; -&nbsp; <font color=\"#FF0000\">simulation results significantly differ ( &gt;50%)</font></p>" + endl);
        summary3.write(summary3_table.toString());
        summary3.write("</body>" + endl);
        summary3.write("</html>" + endl);
        summary3.close();
    }

    public void addTestStatistics(TestStatistics testStatistics, String name, Properties properties)
    {
        try
        {
            simulationTime += testStatistics.getSimulationTime();
            writeDetailsFile(testStatistics, name, properties);
            writeRelativeErrorsDetails(testStatistics, name, properties);
            writeMedianesErrorsDetails(testStatistics, name, properties);
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    private void writeDetailsFile(TestStatistics testStatistics, String name, Properties properties) throws Exception
    {
        File outDir = new File(outDirectory + "/comparisons/");
        if( !outDir.exists() )
            outDir.mkdirs();

        try(PrintWriter out = new PrintWriter(new File(outDir, name + ".html"), "UTF-8"))
        {
            out.write("<html>" + endl);
            out.write("<head>" + endl);

            out.write("<title>Test " + name + " details</title>" + endl);

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
            out.write("        white-space: nowrap;" + endl);
            out.write("}" + endl);
            out.write("td.head {" + endl);
            out.write("        text-align:left;" + endl);
            out.write("        border-top:thin solid white;" + endl);
            out.write("        border-bottom:thin solid white;" + endl);
            out.write("        border-left:thin solid white;" + endl);
            out.write("        border-right:thin solid white;" + endl);
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

            out.write("<h3>Test: " + name + " - " + getStatusName(properties.getProperty("status")) + "</font></h3>" + endl);

            String shortName = name.substring(0, name.indexOf('.'));
            String plotPicture = BiomodelsPlotLinksLoader.getInstance(outDirectory).getPlotLinkForTest(shortName);
            String plotLink = "-";
            if( plotPicture != null )
            {
                plotLink = "<a href=\"#plot\">plot</a>";
            }

            out.write("<hr>" + endl);
            out.write("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">" + endl);
            out.write("<tr><td class=\"head\">test</td><td class=\"head\">: " + name + "</td></tr>" + endl);
            out.write("<tr><td class=\"head\">model</td><td class=\"head\">: <a href=\"" + xmlDirectory + shortName
                    + ".xml\">xml</a>, <a href=\"#graph\">reaction graph</a></td></tr>" + endl);
            out.write("<tr><td class=\"head\">simulator engine</td><td class=\"head\">: " + properties.getProperty("simulator") + "</td></tr>"
                    + endl);
            out.write("<tr><td class=\"head\">generated code</td><td class=\"head\">: <a href=\"../" + javaDirectory + shortName + ".java\">"
                    + shortName + ".java</a></td></tr>" + endl);
            out.write("<tr><td class=\"head\">results</td><td class=\"head\">: <a href=\"../" + csvDirectory + shortName
                    + ".xml.BioUML.csv\">CSV file</a></td></tr>" + endl);
            out.write("<tr><td class=\"head\">plots</td><td class=\"head\">: " + plotLink + "</td></tr>" + endl);
            out
                    .write("<tr><td class=\"head\">comparison</td><td class=\"head\">: <a href=\"#comparison\">comparison with other simulators</a></td></tr>"
                            + endl);
            out.write("<tr><td class=\"head\">status</td><td class=\"head\">: " + getStatusName(properties.getProperty("status"))
                    + "</td></tr>" + endl);
            out.write("<tr><td class=\"head\">simulation time</td><td class=\"head\">: " + testStatistics.getSimulationTime() / 1000
                    + " s. </a></td></tr>" + endl);
            out.write("</table>" + endl);

            out.write("<br><b>Parameters:</b><br>" + endl);
            out.write("ZERO " + properties.getProperty("zero") + "<br>" + endl);
            out.write("ATOL " + properties.getProperty("atol") + "<br>" + endl);
            out.write("RTOL " + properties.getProperty("rtol") + "<br>" + endl);
            out.write("STEP " + properties.getProperty("step") + "<br>" + endl);

            out.write("<a name=\"graph\"/><h3> Reaction graph </h3>");
            out.write("<img src=\"" + GRAPH_LINK_TEMPLATE + shortName + ".gif\">" + endl);
            out.write("<p><i>Note: downloaded from</i> <a href=\"" + GRAPH_LINK_TEMPLATE + shortName + ".gif\">" + GRAPH_LINK_TEMPLATE
                    + shortName + ".gif</a></p>" + endl);
            out.write("<hr>" + endl);

            if( plotPicture != null )
            {
                out.write("<a name=\"plot\"/><h3> Plots </h3>" + endl);
                out.write("<img src=\"" + plotPicture + "\">" + endl);
                out.write("<p><i>Note: downloaded from</i> <a href=\"" + plotPicture + "\">" + plotPicture + "</a></p>" + endl);
                out.write("<hr>" + endl);
            }

            out.write("<a name=\"comparison\"/><h3> Comparison with other simulators </h3>");

            //legend
            out.write("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);
            out.write("<p>Text color indicates difference between values:<br>" + endl);
            out.write("&nbsp; -&nbsp; no significant difference ( &lt; 0.1%)<br>" + endl);
            out.write("&nbsp; -&nbsp; <font color=\"#800000\">small relative error ( &gt; 0.1% and &lt;50%)</font><br>" + endl);
            out.write("&nbsp; -&nbsp; <font color=\"#FF0000\">simulation results significantly differ ( &gt;50%)</font></p>" + endl);

            // main table
            if( properties.getProperty("status").equals("0") )
            {
                TestStatistics.ResultComparison comparison = testStatistics.getComparison();

                //write header
                int dimension = testStatistics.getValuesCount();
                if( dimension > 0 )
                {

                    Map<String, Integer> variablesMap = testStatistics.getVariableNames();
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
                    Set<String> solvers = new HashSet<>();
                    for( Map.Entry<String, TestStatistics.ResultComparison.Statistics> e : comparison.getRow(0).entrySet() )
                    {
                        TestStatistics.ResultComparison.Statistics s = e.getValue();
                        solvers.addAll(s.values.keySet());
                    }
                    for( String v : solvers )
                    {
                        out.write("<input class=\"quickColumn\" type=\"checkbox\"  onClick=\"hideRow('" + v + "', this.checked);\" checked>"
                                + v + "&nbsp;" + endl);
                    }
                    out.write("<br>" + endl);

                    out.write("<table cellpadding=\"3\" class=\"sbml-table\" id=\"result\">");
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
                        Map<String, TestStatistics.ResultComparison.Statistics> m = comparison.getRow(i);
                        Set<String> results = new HashSet<>();
                        for( Map.Entry<String, TestStatistics.ResultComparison.Statistics> e : m.entrySet() )
                        {
                            TestStatistics.ResultComparison.Statistics s = e.getValue();
                            results.addAll(s.values.keySet());
                        }

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
                        int ind = 0;

                        for( String r : StreamEx.of(results).sorted() )
                        {
                            out.write("<tr class=\"" + r + "\">\n");

                            out.write("<td class=\"sbml-cell\">" + time + "</td>");

                            // output other solver values
                            String color = getSolverColor(r);
                            out.write("<td class=\"sbml-cell\" bgcolor = \"" + color + "\">" + r + "</td>");

                            ind = 0;
                            for( Map.Entry<String, TestStatistics.ResultComparison.Statistics> e : m.entrySet() )
                            {
                                TestStatistics.ResultComparison.Statistics s = e.getValue();
                                out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\">");
                                Double v = s.values.get(r);
                                if( v != null )
                                    out.write(valuesNumberFormatter.format(v.doubleValue()));
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
                        for( Map.Entry<String, TestStatistics.ResultComparison.Statistics> e : m.entrySet() )
                        {
                            TestStatistics.ResultComparison.Statistics s = e.getValue();
                            out.write("<td class=\"" + vars[ind] + "\">" + valuesNumberFormatter.format(s.min) + "</td>\n");
                            ind++;
                        }
                        out.write("</tr>\n");

                        //  output max value
                        out.write("<tr>\n");
                        out.write("<td class=\"sbml-cell\">" + time + "</td>");
                        out.write("<td class=\"sbml-cell\">max</td>\n");
                        ind = 0;
                        for( Map.Entry<String, TestStatistics.ResultComparison.Statistics> e : m.entrySet() )
                        {
                            TestStatistics.ResultComparison.Statistics s = e.getValue();
                            out.write("<td class=\"" + vars[ind] + "\">" + valuesNumberFormatter.format(s.max) + "</td>\n");
                            ind++;
                        }
                        out.write("</tr>\n");

                        //  output mean value
                        out.write("<tr>\n");
                        out.write("<td class=\"sbml-cell\">" + time + "</td>");
                        out.write("<td class=\"sbml-cell\">mean</td>\n");
                        ind = 0;
                        for( Map.Entry<String, TestStatistics.ResultComparison.Statistics> e : m.entrySet() )
                        {
                            TestStatistics.ResultComparison.Statistics s = e.getValue();
                            out.write("<td class=\"" + vars[ind] + "\">" + valuesNumberFormatter.format(s.mean) + "</td>\n");
                            ind++;
                        }
                        out.write("</tr>\n");

                        //  output relative error
                        out.write("<tr>\n");
                        out.write("<td class=\"sbml-cell\">" + time + "</td>");
                        out.write("<td class=\"sbml-cell\">relative error(%)</td>\n");
                        ind = 0;
                        for( Map.Entry<String, TestStatistics.ResultComparison.Statistics> e : m.entrySet() )
                        {
                            TestStatistics.ResultComparison.Statistics s = e.getValue();

                            out.write("<td class=\"" + vars[ind] + "\"><b>"
                                    + getColoredValue(s.relativeError, errorsNumberFormatter.format(s.relativeError)) + "</b></td>");
                            ind++;
                        }
                        out.write("</tr>\n");
                    }
                    out.write("</table>\n");

                    writeLineToSummary(properties, name, comparison.getMaxRelativeError());
                }
            }
            else
            {
                writeLineToSummary(properties, name, Double.NaN);
            }

            out.write("</body>" + endl);
            out.write("</html>" + endl);
        }
    }
    
    private void writeLineToSummary(Properties properties, String name, double maxRelativeError)
    {
        summary1_table.append("<tr>");
        summary1_table.append("<td><a href=\"comparisons/" + name + ".html\">" + name + "</a></td>");

        summary1_table.append("<td>" + getStatusName(properties.getProperty("status")) + "</td>");

        summary1_table.append("<td>" + getColoredValue(maxRelativeError, errorsNumberFormatter.format(maxRelativeError)) + "</td>");

        summary1_table.append("<td>" + ( (float) Integer.parseInt(properties.getProperty("time")) / 1000 ) + "s. </font></td>");

        summary1_table.append("<td>" + properties.getProperty("simulator") + "</td>");

        summary1_table.append("<td>" + properties.getProperty("atol") + ", " + properties.getProperty("rtol") + ", "
                + properties.getProperty("zero") + ", " + properties.getProperty("step") + "</td>");

        summary1_table.append("</tr>");

        testCount++;
        if( Double.isNaN(maxRelativeError) )
        {
            failedTestCount++;
        }
        else
        {
            int type = getResultType(maxRelativeError);
            if( type == NO_DIFFERENCE )
            {
                successfulTestCount++;
            }
            else if( type == SMALL_DIFFERENCE )
            {
                needstuningTestCount++;
            }
            else
            {
                errorTestCount++;
            }
        }
    }

    private void writeRelativeErrorsDetails(TestStatistics testStatistics, String name, Properties properties) throws Exception
    {
        File outDir = new File(outDirectory + "/relativeErrors/");
        if( !outDir.exists() )
            outDir.mkdirs();

        try(PrintWriter out = new PrintWriter(new File(outDir, name + ".html"), "UTF-8"))
        {
            out.write("<html>" + endl);
            out.write("<head>" + endl);

            out.write("<title>Test " + name + " details</title>" + endl);

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
            out.write("        white-space: nowrap;" + endl);
            out.write("}" + endl);
            out.write("td.head {" + endl);
            out.write("        text-align:left;" + endl);
            out.write("        border-top:thin solid white;" + endl);
            out.write("        border-bottom:thin solid white;" + endl);
            out.write("        border-left:thin solid white;" + endl);
            out.write("        border-right:thin solid white;" + endl);
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
            out.write("<h3>Test: " + name + " - " + getStatusName(properties.getProperty("status")) + "</font></h3>" + endl);

            String shortName = name.substring(0, name.indexOf('.'));
            String plotPicture = BiomodelsPlotLinksLoader.getInstance(outDirectory).getPlotLinkForTest(shortName);
            String plotLink = "-";
            if( plotPicture != null )
            {
                plotLink = "<a href=\"#plot\">plot</a>";
            }

            out.write("<hr>" + endl);
            out.write("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">" + endl);
            out.write("<tr><td class=\"head\">test</td><td class=\"head\">: " + name + "</td></tr>" + endl);
            out.write("<tr><td class=\"head\">model</td><td class=\"head\">: <a href=\"" + xmlDirectory + shortName
                    + ".xml\">xml</a>, <a href=\"#graph\">reaction graph</a></td></tr>" + endl);
            out.write("<tr><td class=\"head\">plots</td><td class=\"head\">: " + plotLink + "</td></tr>" + endl);
            out
                    .write("<tr><td class=\"head\">comparison</td><td class=\"head\">: <a href=\"#comparison\">comparison of simulators</a></td></tr>"
                            + endl);
            out.write("</table>" + endl);

            out.write("<a name=\"graph\"/><h3> Reaction graph </h3>");
            out.write("<img src=\"" + GRAPH_LINK_TEMPLATE + shortName + ".gif\">" + endl);
            out.write("<p><i>Note: downloaded from</i> <a href=\"" + GRAPH_LINK_TEMPLATE + shortName + ".gif\">" + GRAPH_LINK_TEMPLATE
                    + shortName + ".gif</a></p>" + endl);
            out.write("<hr>" + endl);

            if( plotPicture != null )
            {
                out.write("<a name=\"plot\"/><h3> Plots </h3>" + endl);
                out.write("<img src=\"" + plotPicture + "\">" + endl);
                out.write("<p><i>Note: downloaded from</i> <a href=\"" + plotPicture + "\">" + plotPicture + "</a></p>" + endl);
                out.write("<hr>" + endl);
            }

            out.write("<a name=\"comparison\"/><h3> Comparison with other simulators </h3>");

            //          legend
            out.write("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);

            out.write("<p>Text color indicates difference between values:<br>" + endl);
            out.write("&nbsp; -&nbsp; no significant difference ( &lt; 0.1%)<br>" + endl);
            out.write("&nbsp; -&nbsp; <font color=\"#800000\">small relative error ( &gt; 0.1% and &lt;50%)</font><br>" + endl);

            out.write("&nbsp; -&nbsp; <font color=\"#FF0000\">simulation results significantly differ ( &gt;50%)</font></p>" + endl);
            // main table
            if( testStatistics != null )
            {
                TestStatistics.ResultRelativeErrors relativeErrors = testStatistics.getRelativeErrors();
                if( relativeErrors != null )
                {
                    //write header
                    int dimension = testStatistics.getValuesCount();
                    if( dimension > 0 )
                    {
                        Map<String, Integer> variablesMap = testStatistics.getVariableNames();
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
                        for( Map.Entry<String, TestStatistics.ResultRelativeErrors.RelativeErrors> e : relativeErrors.getRow(0).entrySet() )
                        {
                            TestStatistics.ResultRelativeErrors.RelativeErrors s = e.getValue();
                            solvers.addAll(s.relativeErrors.keySet());
                        }
                        List<String> solvers2 = new ArrayList<>();
                        for( String r : solvers )
                        {
                            solvers2.add(r);
                        }
                        Collections.sort(solvers2);
                        for( String v : solvers2 )
                        {
                            out.write("<input class=\"quickColumn\" type=\"checkbox\"  onClick=\"hideRow('" + v
                                    + "', this.checked);\" checked>" + v + "&nbsp;" + endl);
                        }
                        out.write("<br>" + endl);

                        out.write("<table cellpadding=\"3\" class=\"sbml-table\" id=\"result\">");
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
                            Map<String, TestStatistics.ResultRelativeErrors.RelativeErrors> m = relativeErrors.getRow(i);

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

                            for( String r : solvers2 )
                            {
                                out.write("<tr class=\"" + r + "\">\n");

                                out.write("<td class=\"sbml-cell\" >" + time + "</td>\n");

                                String color = getSolverColor(r);
                                out.write("<td class=\"sbml-cell\" bgcolor = \"" + color + "\">" + r + "</td>");

                                int ind = 0;
                                for( Map.Entry<String, TestStatistics.ResultRelativeErrors.RelativeErrors> e : m.entrySet() )
                                {
                                    TestStatistics.ResultRelativeErrors.RelativeErrors s = e.getValue();
                                    TestStatistics.ResultRelativeErrors.Values values = null;
                                    if( s != null && s.relativeErrors != null && ( values = s.relativeErrors.get( r ) ) != null )
                                    {
                                        double v = values.value;
                                        out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\">"
                                                + valuesNumberFormatter.format(v) + "</td>");
                                        double v2 = values.relativeError;
                                        out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\"><b>"
                                                + getColoredValue(v2, errorsNumberFormatter.format(v2)) + "</b></td>");
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
                                out.write("<td class=\"" + vars[ind] + "\" bgcolor=\"c0c0c0\" colspan=2><b>"
                                        + getColoredValue(maxValue.doubleValue(), errorsNumberFormatter.format(maxValue.doubleValue()))
                                        + "</b></td>\n");
                            }
                            else
                            {
                                out.write("<td class=\"" + vars[ind] + "\" bgcolor=\"c0c0c0\" colspan=2> - </td>\n");
                            }
                            ind++;
                        }
                        out.write("</tr>\n");
                        out.write("</table>\n");

                        writeLineToRelativeSummary(properties, name, relativeErrors);
                    }
                }
            }

            out.write("</body>" + endl);
            out.write("</html>" + endl);
        }
    }

    private void writeLineToRelativeSummary(Properties properties, String name, TestStatistics.ResultRelativeErrors resultComparison)
    {
        summary2_table.append("<tr>");
        summary2_table.append("<td><a href=\"relativeErrors/" + name + ".html\">" + name + "</a></td>");


        Set<String> solvers = new HashSet<>();

        Map<String, TestStatistics.ResultRelativeErrors.RelativeErrors> m = resultComparison.getRow(0);
        if( m != null )
        {
            for( Map.Entry<String, TestStatistics.ResultRelativeErrors.RelativeErrors> map : m.entrySet() )
            {
                TestStatistics.ResultRelativeErrors.RelativeErrors s = map.getValue();
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
                if( i == 0 && !properties.getProperty("status").equals("0") )
                    hasResult = false;
                if( hasResult )
                {
                    passed_2[i]++;
                    Double max = resultComparison.getMaxBySolvers().get(allSolvers[i]);
                    int type = getResultType(max);
                    if( type == NO_DIFFERENCE )
                    {
                        noDifference_2[i]++;
                    }
                    else if( type == SMALL_DIFFERENCE )
                    {
                        smallDifference_2[i]++;
                    }
                    else
                    {
                        significantDifference_2[i]++;
                    }
                    summary2_table.append("<td>" + getColoredValue(max.doubleValue(), errorsNumberFormatter.format(max.doubleValue()))
                            + "</td>");
                }
                else
                {
                    summary2_table.append("<td><font color=\"black\">-</font></td>");
                }
            }
            else
            {
                if( i == 0 )
                {
                    summary2_table.append("<td><font color=\"black\">-</font></td>");
                }
                else
                {
                    passed_2[i]++;
                    summary2_table.append("<td><font color=\"black\">???</font></td>");
                }
            }
        }
        summary2_table.append("</tr>");
    }

    private void writeSummaryToRelativeSummary()
    {
        summary2.write("<h1> Simulator comparisons: BioModels release 9 </h1>");
        summary2.write("<h3> Summary </h3>" + endl);
        summary2.write("<table cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">");
        summary2.write("<tr>");
        summary2.write("<td valign=\"top\" >Test</td>");
        summary2.write("<td valign=\"top\" >BioUML</td>");
        for( String solver : CalculateBiomodelsStatistics.SOLVER_EXTENSIONS )
        {
            summary2.write("<td valign=\"top\" >" + solver + "</td>");
        }
        summary2.write("</tr>");

        //passed
        summary2.write("<tr>");
        summary2.write("<td><b>passed:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            summary2.write("<td>" + passed_2[i] + "</td>");
        }
        summary2.write("</tr>");

        // no difference
        summary2.write("<tr>");
        summary2.write("<td><b>no difference:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            summary2.write("<td>" + noDifference_2[i] + "</td>");
        }
        summary2.write("</tr>");

        // small difference
        summary2.write("<tr>");
        summary2.write("<td><b>small difference:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            summary2.write("<td>" + smallDifference_2[i] + "</td>");
        }
        summary2.write("</tr>");

        // significant difference
        summary2.write("<tr>");
        summary2.write("<td><b>significant difference:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            summary2.write("<td>" + significantDifference_2[i] + "</td>");
        }
        summary2.write("</tr>");

        summary2.write("</table>");
    }

    private void writeMedianesErrorsDetails(TestStatistics testStatistics, String name, Properties properties) throws Exception
    {
        File outDir = new File(outDirectory + "/relativeMedianesErrors/");
        if( !outDir.exists() )
            outDir.mkdirs();

        try(PrintWriter out = new PrintWriter(new File(outDir, name + ".html"), "UTF-8"))
        {
            out.write("<html>" + endl);
            out.write("<head>" + endl);

            out.write("<title>Test " + name + " details</title>" + endl);

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
            out.write("        white-space: nowrap;" + endl);
            out.write("}" + endl);
            out.write("td.head {" + endl);
            out.write("        text-align:left;" + endl);
            out.write("        border-top:thin solid white;" + endl);
            out.write("        border-bottom:thin solid white;" + endl);
            out.write("        border-left:thin solid white;" + endl);
            out.write("        border-right:thin solid white;" + endl);
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
            out.write("<h3>Test: " + name + " - " + getStatusName(properties.getProperty("status")) + "</font></h3>" + endl);

            String shortName = name.substring(0, name.indexOf('.'));
            String plotPicture = BiomodelsPlotLinksLoader.getInstance(outDirectory).getPlotLinkForTest(shortName);
            String plotLink = "-";
            if( plotPicture != null )
            {
                plotLink = "<a href=\"#plot\">plot</a>";
            }

            out.write("<hr>" + endl);

            out.write("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">" + endl);
            out.write("<tr><td class=\"head\">test</td><td class=\"head\">: " + name + "</td></tr>" + endl);
            out.write("<tr><td class=\"head\">model</td><td class=\"head\">: <a href=\"" + xmlDirectory + shortName
                    + ".xml\">xml</a>, <a href=\"#graph\">reaction graph</a></td></tr>" + endl);
            out.write("<tr><td class=\"head\">plots</td><td class=\"head\">: " + plotLink + "</td></tr>" + endl);
            out
                    .write("<tr><td class=\"head\">comparison</td><td class=\"head\">: <a href=\"#comparison\">comparison of simulators</a></td></tr>"
                            + endl);
            out.write("</table>" + endl);

            out.write("<a name=\"graph\"/><h3> Reaction graph </h3>");
            out.write("<img src=\"" + GRAPH_LINK_TEMPLATE + shortName + ".gif\">" + endl);
            out.write("<p><i>Note: downloaded from</i> <a href=\"" + GRAPH_LINK_TEMPLATE + shortName + ".gif\">" + GRAPH_LINK_TEMPLATE
                    + shortName + ".gif</a></p>" + endl);
            out.write("<hr>" + endl);

            if( plotPicture != null )
            {
                out.write("<a name=\"plot\"/><h3> Plots </h3>" + endl);
                out.write("<img src=\"" + plotPicture + "\">" + endl);
                out.write("<p><i>Note: downloaded from</i> <a href=\"" + plotPicture + "\">" + plotPicture + "</a></p>" + endl);
                out.write("<hr>" + endl);
            }

            out.write("<a name=\"comparison\"/><h3> Comparison with other simulators </h3>");
            //      legend
            out.write("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);

            out.write("<p>Text color indicates difference between values:<br>" + endl);
            out.write("&nbsp; -&nbsp; no significant difference ( &lt; 0.1%)<br>" + endl);
            out.write("&nbsp; -&nbsp; <font color=\"#800000\">small relative error ( &gt; 0.1% and &lt;50%)</font><br>" + endl);
            out.write("&nbsp; -&nbsp; <font color=\"#FF0000\">simulation results significantly differ ( &gt;50%)</font></p>" + endl);
            // main table
            if( testStatistics != null )
            {
                TestStatistics.ResultRelativeErrors relativeErrors = testStatistics.getRelativeErrors();
                if( relativeErrors != null )
                {
                    //write header
                    int dimension = testStatistics.getValuesCount();
                    if( dimension > 0 )
                    {
                        Map<String, Integer> variablesMap = testStatistics.getVariableNames();
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
                        for( Map.Entry<String, TestStatistics.ResultRelativeErrors.RelativeErrors> e : relativeErrors.getRow(0).entrySet() )
                        {
                            TestStatistics.ResultRelativeErrors.RelativeErrors s = e.getValue();
                            solvers.addAll(s.relativeErrors.keySet());
                        }
                        List<String> solvers2 = new ArrayList<>();
                        for( String r : solvers )
                        {
                            solvers2.add(r);
                        }
                        Collections.sort(solvers2);
                        for( String v : solvers2 )
                        {
                            out.write("<input class=\"quickColumn\" type=\"checkbox\"  onClick=\"hideRow('" + v
                                    + "', this.checked);\" checked>" + v + "&nbsp;" + endl);
                        }
                        out.write("<br>" + endl);

                        out.write("<table cellpadding=\"3\" class=\"sbml-table\" id=\"result\">");
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
                            Map<String, TestStatistics.ResultRelativeErrors.RelativeErrors> m = relativeErrors.getRow(i);

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

                            for( String r : solvers2 )
                            {
                                out.write("<tr class=\"" + r + "\">\n");

                                out.write("<td class=\"sbml-cell\" >" + time + "</td>\n");

                                String color = getSolverColor(r);
                                out.write("<td class=\"sbml-cell\" bgcolor = \"" + color + "\">" + r + "</td>");

                                int ind = 0;
                                for( Map.Entry<String, TestStatistics.ResultRelativeErrors.RelativeErrors> e : m.entrySet() )
                                {
                                    TestStatistics.ResultRelativeErrors.RelativeErrors s = e.getValue();
                                    TestStatistics.ResultRelativeErrors.Values values = null;
                                    if( s != null && s.relativeErrors != null && ( values = s.relativeErrors.get( r ) ) != null )
                                    {
                                        double v = values.value;
                                        out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\">"
                                                + valuesNumberFormatter.format(v) + "</td>");
                                        double v2 = values.relativeMedianesError;
                                        out.write("<td class=\"" + vars[ind] + "\" bgcolor = \"" + color + "\"><b>"
                                                + getColoredValue(v2, errorsNumberFormatter.format(v2)) + "</b></td>");
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

                        writeLineToMedianesSummary(properties, name, relativeErrors);
                    }
                }
            }

            out.write("</body>" + endl);
            out.write("</html>" + endl);
        }
    }

    private void writeLineToMedianesSummary(Properties properties, String name, TestStatistics.ResultRelativeErrors resultComparison)
    {
        summary3_table.append("<tr>");
        summary3_table.append("<td><a href=\"relativeMedianesErrors/" + name + ".html\">" + name + "</a></td>");

        Set<String> solvers = new HashSet<>();

        Map<String, TestStatistics.ResultRelativeErrors.RelativeErrors> m = resultComparison.getRow(0);
        for( Map.Entry<String, TestStatistics.ResultRelativeErrors.RelativeErrors> map : m.entrySet() )
        {
            TestStatistics.ResultRelativeErrors.RelativeErrors s = map.getValue();
            solvers.addAll(s.relativeErrors.keySet());
        }

        for( int i = 0; i < allSolvers.length; i++ )
        {
            boolean hasResult = false;
            if( solvers.size() != 0 )
            {
                if( solvers.contains(allSolvers[i]) )
                    hasResult = true;
                if( i == 0 && !properties.getProperty("status").equals("0") )
                    hasResult = false;
                if( hasResult )
                {
                    passed_3[i]++;
                    Double max = resultComparison.getMaxByMedianesBySolvers().get(allSolvers[i]);
                    int type = getResultType(max);
                    if( type == NO_DIFFERENCE )
                    {
                        noDifference_3[i]++;
                    }
                    else if( type == SMALL_DIFFERENCE )
                    {
                        smallDifference_3[i]++;
                    }
                    else
                    {
                        significantDifference_3[i]++;
                    }
                    summary3_table.append("<td>" + getColoredValue(max.doubleValue(), errorsNumberFormatter.format(max.doubleValue()))
                            + "</td>");
                }
                else
                {
                    summary3_table.append("<td><font color=\"black\">-</font></td>");
                }
            }
            else
            {
                if( i == 0 )
                {
                    summary3_table.append("<td><font color=\"black\">-</font></td>");
                }
                else
                {
                    passed_3[i]++;
                    summary3_table.append("<td><font color=\"black\">???</font></td>");
                }
            }
        }
        summary3_table.append("</tr>");
    }

    private void writeSummaryToMedianesSummary()
    {
        summary3.write("<h1> Simulator comparisons: BioModels release 9 </h1>");
        summary3.write("<h3> Summary </h3>" + endl);
        summary3.write("<table cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">");
        summary3.write("<tr>");
        summary3.write("<td valign=\"top\" >Test</td>");
        summary3.write("<td valign=\"top\" >BioUML</td>");
        for( String solver : CalculateBiomodelsStatistics.SOLVER_EXTENSIONS )
        {
            summary3.write("<td valign=\"top\" >" + solver + "</td>");
        }
        summary3.write("</tr>");

        //passed
        summary3.write("<tr>");
        summary3.write("<td><b>passed:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            summary3.write("<td>" + passed_3[i] + "</td>");
        }
        summary3.write("</tr>");

        // no difference
        summary3.write("<tr>");
        summary3.write("<td><b>no difference:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            summary3.write("<td>" + noDifference_3[i] + "</td>");
        }
        summary3.write("</tr>");

        // small difference
        summary3.write("<tr>");
        summary3.write("<td><b>small difference:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            summary3.write("<td>" + smallDifference_3[i] + "</td>");
        }
        summary3.write("</tr>");

        // significant difference
        summary3.write("<tr>");
        summary3.write("<td><b>significant difference:<b></td>");
        for( int i = 0; i < allSolvers.length; i++ )
        {
            summary3.write("<td>" + significantDifference_3[i] + "</td>");
        }
        summary3.write("</tr>");

        summary3.write("</table>");
    }

    private void writeSummaryToStatistics1()
    {
        summary1.write("<h1> BioUML simulation test: BioModels release 9 </h1>");
        summary1.write("<h3> Summary </h3>");
        summary1.write("<table cellspacing=\"2\" cellpadding=\"5\" width=\"95%\" border=\"1\">");
        summary1.write("<tr>");
        summary1.write("<td valign = \"top\"><b>Tests</b></td>");
        summary1.write("<td valign = \"top\"><b>Successful</b></td>");
        summary1.write("<td valign = \"top\"><b>Failed</b></td>");
        summary1.write("<td valign = \"top\"><b>Errors</b></td>");
        summary1.write("<td valign = \"top\"><b>Needs tuning</b></td>");
        summary1.write("<td valign = \"top\"><b>Success rate</b></td>");
        summary1.write("<td valign = \"top\"><b>Time(sec)</b></td>");
        summary1.write("</tr>");

        float successRate = 100.0f * successfulTestCount / testCount;

        summary1.write("<tr>");
        summary1.write("<td>" + testCount + "</td>");
        summary1.write("<td>" + successfulTestCount + "</td>");
        summary1.write("<td>" + failedTestCount + "</td>");
        summary1.write("<td>" + errorTestCount + "</td>");
        summary1.write("<td>" + needstuningTestCount + "</td>");
        summary1.write("<td>" + successRate + "</td>");
        summary1.write("<td>" + simulationTime / 1000 + "</td>");
        summary1.write("</tr>");

        summary1.write("</table>");

        summary1.write("<p><a name=\"results_legend\"><i>Legend:</i></a><br>" + endl);
        summary1.write("Failed &nbsp; -&nbsp; an exception has occurred;<br>" + endl);
        summary1
                .write("Errors &nbsp; -&nbsp; simulation results significantly differ from the known ones: (x >= min * 0.5) and (x <= max * 1.5);<br>"
                        + endl);
        summary1
                .write("Needs tuning &nbsp; -&nbsp; relative error is not small enough: not (( x >=  min * 0.999  ) and ( x <= max * 1.001 ));<br>"
                        + endl);
    }

    private String getSolverColor(String solver)
    {
        int i = 0;
        for( String s : CalculateBiomodelsStatistics.SOLVER_EXTENSIONS )
        {
            if( s.equals(solver) )
            {
                break;
            }
            i++;
        }
        if( i < CalculateBiomodelsStatistics.SOLVER_COLORS.length )
        {
            return CalculateBiomodelsStatistics.SOLVER_COLORS[i];
        }
        return "white";
    }

    private String getColoredValue(double value, String strValue)
    {
        int type = getResultType(value);
        if( type == NO_DIFFERENCE )
        {
            return strValue;
        }
        else if( type == SMALL_DIFFERENCE )
        {
            return "<font color=\"#800000\">" + strValue + "</font>";
        }
        else
        {
            return "<font color=\"#ff0000\">" + strValue + "</font>";
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

    private String getStatusName(String input)
    {
        if( input.equals("0") )
        {
            return "Ok";
        }
        return "Failed";
    }
}
