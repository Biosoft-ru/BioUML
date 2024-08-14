package ru.biosoft.analysis._test;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Vector;

import javax.imageio.ImageIO;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.DiagramViewBuilder;
import biouml.model.util.ImageGenerator;
import biouml.workbench.graph.DiagramToGraphTransformer;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.graph.CompartmentCrossCostGridLayouter;
import ru.biosoft.graph.FastGridLayouter;
import ru.biosoft.graph.ForceDirectedLayouter;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.LayoutJobControl;
import ru.biosoft.graph.LayoutJobControlImpl;
import ru.biosoft.graph.LayoutQualityMetrics;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.Node;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.workbench.Framework;

//import junit.framework.TestSuite;
//import junit.framework.Test;



public class MultithreadingLayoutTest extends TestCase
{
    static String path = "C:/projects/java/BioUML/src/ru/biosoft/analysis/_test/LayoutTestControl";
    static String imagePath = "C:/projects/java/BioUML/src/ru/biosoft/analysis/_test/LayoutTestControl";

    public static final String repositoryPath1 = "../data";
    public static final String repositoryPath2 = "../data_resources";
    static Vector<String> layouterNames = new Vector<>();
    static Vector<String> dataBaseNames = new Vector<>();
    static Vector<String> diagramNames = new Vector<>();
    static final String[] DATABASE_NAMES = {"Biomodels", "Transpath", "Kegg"};
    static final String[] LAYOUTER_NAMES = {"CompartmentCrossCostGridLayouter", "HierarchicLayouter", "ForceDirectedLayouter",
            "FastGridLayouter"};

    static Vector<String> DIAGRAM_COLUMN = new Vector<>();
    static Vector<String> LAYOUTER_COLUMN = new Vector<>();

    static Vector<String> fastGridString = new Vector<>();
    static Vector<String> hierarchicString = new Vector<>();
    static Vector<String> forceDirectedString = new Vector<>();
    static Vector<String> compCrossCostString = new Vector<>();
    static Vector<String> diagramString = new Vector<>();
    static String headerString = "<b>Diagram parameters</b>";

    static String currentDiagName = "";
    static int currentLayouterIndex = 0;

    static String STRING_DELIMITER = "\n";

    static int columnNumber = 1;

    static final int PICTURE_WIDTH = 220;
    static final double MAX_PICTURE_SIZE = 420;

    static final int THREAD_NUMBER = 2;

    static final String ITEM_DELIMITER = ";";

    static Vector<String> queue = new Vector<>();

    public MultithreadingLayoutTest(String name)
    {
        super(name);
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite(MultithreadingLayoutTest.class.getName());
                        
        Framework.initRepository(repositoryPath1);
        Framework.initRepository(repositoryPath2);
        
        initVectors();
        processParameters();
        formQueueString();
 
        if( layouterNames.contains("CompartmentCrossCostGridLayouter") )
        {
            headerString += STRING_DELIMITER + "<b>CrossCostGridLayouter</b>";
            columnNumber++;
            String outputName = "CompartmentCrossCostGridLayouter.out";
            File f = new File(path + "/" + outputName);
            if (!f.exists())
            {
            f.createNewFile();
            }
        }
        if( layouterNames.contains("HierarchicLayouter") )
        {
            headerString += STRING_DELIMITER + "<b>HierarchicLayouter</b>";
            columnNumber++;
            String outputName = "HierarchicLayouter.out";
            File f = new File(path + "/" + outputName);
            if (!f.exists())
            {
            f.createNewFile();
            }
        }
        if( layouterNames.contains("ForceDirectedLayouter") )
        {
            headerString += STRING_DELIMITER + "<b>ForceDirectedLayouter</b>";
            columnNumber++;
            String outputName = "ForceDirectedLayouter.out";
            File f = new File(path + "/" + outputName);
            if (!f.exists())
            {
            f.createNewFile();
            }
        }

        if( layouterNames.contains("FastGridLayouter") )
        {
            headerString += STRING_DELIMITER + "<b>FastGridLayouter</b>";
            columnNumber++;
            String outputName = "FastGridLayouter.out";
            File f = new File(path + "/" + outputName);
            if (!f.exists())
            {
            f.createNewFile();
            }
        }
        
        Thread[] tr = new Thread[THREAD_NUMBER];

        for (int i = 0; i<THREAD_NUMBER; i++)
        {
            tr[i] = new LayouterThread();
            try
            {
                tr[i].start();
            }
            catch (Exception e){e.printStackTrace();}
        }
        
        try
        {
            for (int i = 0; i<THREAD_NUMBER; i++)
            {
                tr[i].join();
            }
        }
        catch (Exception e){e.printStackTrace();}

       // htmlTest();

        suite.addTest(new MultithreadingLayoutTest("readyTest"));
        return suite;
    }
    
    public static void initVectors() throws Exception
    {
        DIAGRAM_COLUMN = makeVector("Name" + "\t" + "Database" + "\t" + "Node&nbsp;count" + "\t" + "Edge&nbsp;count", "\t");
        LAYOUTER_COLUMN = makeVector ("time" + "\t" + "estamatedOperations" + "\t" + "width" + "\t" + "height" + "\t" + "ratio"
                + "\t" + "bends" + "\t" + "crossings" + "\t" + "confluences" + "\t" + "withoutPath" + "\t" + "edgeLength" + "\t" + "sigma","\t");
    }
    
    public static Vector<String> makeVector (String str, String delimiter)throws Exception
    {
        Vector<String> returnVect = new Vector<>();
        String[] iterArr =  str.split(delimiter);
        for (String s:iterArr) returnVect.add(s);
        return returnVect;
    }
    
    public Graph readGraph(String name) throws Exception
    {
        StringBuilder fileData = new StringBuilder();
        try(BufferedReader reader = ApplicationUtils.asciiReader( path + "/" + name))
        {
            char[] buf = new char[1024];
            int numRead = 0;
            while( ( numRead = reader.read(buf) ) != -1 )
            {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
        }

        Graph graph = new Graph();
        graph.fillFromText(fileData.toString());

        return graph;
    }

    
    static class LayouterThread extends Thread
    {
       public String taskString = "";
       @Override
    public void run()
       {
         while (queue.size() != 0)
           {
             try
             {
             this.taskString = updateQueue(queue);

             if( taskString.split(ITEM_DELIMITER)[0].equals("CompartmentCrossCostGridLayouter") )
             {
                 compCrossCostGridLayoutTest(taskString);
             }
             else if( taskString.split(ITEM_DELIMITER)[0].equals("HierarchicLayouter") )
             {
                 hierarchicLayouterTest(taskString);
             }
             else if( taskString.split(ITEM_DELIMITER)[0].equals("ForceDirectedLayouter") )
             {
                 forceDirectedLayoutTest(taskString);
             }
             else if( taskString.split(ITEM_DELIMITER)[0].equals("FastGridLayouter") )
             {
                 fastGridLayoutTest(taskString);
             }
             }
             catch( Exception e )
             {
                 e.printStackTrace();
             }
             
           }
             stop();
            }
       }
    
    public static void hierarchicLayouterTest(String task) throws Exception
    {

        String databaseName = task.split(ITEM_DELIMITER)[2];
        String diagramName = task.split(ITEM_DELIMITER)[1];

        Layouter layouter = new HierarchicLayouter();
        DataElement targetDiagramObj;

        String out = "HierarchicLayouter Layout: " + "\n";
        String outputName = "HierarchicLayouter.out";
        boolean append = false;
        File f = new File(path + "/" + outputName);
        if (f.exists())
        {
        append = true;
        out = "\n";
        }
        BufferedWriter buf = new BufferedWriter(new FileWriter(path + "/" + outputName, append));
        long totalTime = 0;

        String fullDiagName = "databases/" + databaseName + "/Diagrams/" + diagramName;

        targetDiagramObj = CollectionFactory.getDataElement(fullDiagName);

        Diagram diagram = (Diagram)targetDiagramObj;

        Graphics g = ApplicationUtils.getGraphics();
        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(diagram, g);

        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        setNodesFixed(graph, false);
        synchronized(diagramString){
        if( !diagramString.contains(diagramName + "\t" + databaseName + "\t" + graph.nodeCount() + "\t" + graph.edgeCount()) )
            diagramString.add(diagramName + "\t" + databaseName + "\t" + graph.nodeCount() + "\t" + graph.edgeCount());
        }
        int numberOfEstimatedOperations = 0;
        try
        {
        numberOfEstimatedOperations = layouter.estimate(graph, 0);
        }
        catch (Exception e ) {e.printStackTrace();}
        LayoutJobControl lJC = new LayoutJobControlImpl(numberOfEstimatedOperations);

        long time1 = System.currentTimeMillis();

        PathwayLayouter pl = new PathwayLayouter(layouter);
        try
        {
        pl.doLayout(graph, lJC);
        }
        catch (Exception e ) {e.printStackTrace();}

        long time2 = System.currentTimeMillis();
        totalTime = time2 - time1;

        diagram.setView(null);
        DiagramToGraphTransformer.applyLayout(graph, diagram);
        BufferedImage image = ImageGenerator.generateDiagramImage(diagram);

        String imageName = databaseName + "_" + diagramName + "_HierarchicLayouter.png";
        saveImage(image, imagePath + "/" + imageName);


        LayoutQualityMetrics m = LayoutQualityMetrics.getMetrics(graph);
        String titleString = "\n" + databaseName + "\t" + diagramName + "\t" + "time: "+ totalTime + "\n"+"estimatedOperations: "+ numberOfEstimatedOperations+ ",   "+ m.getDebugInfo() + "\n"
                + "+++++++++++++++++++++++++" + "\n";
        out += titleString;
        out += graph.generateText(true);
        out += "+++++++++++++++++++++++++" + "\n";
        buf.append(out);
        out = "";
        synchronized(hierarchicString){
        hierarchicString.add( makeResultString(totalTime, numberOfEstimatedOperations, m));

        }



        buf.flush();

    }



    public static void compCrossCostGridLayoutTest(String task) throws Exception
    {
        String databaseName = task.split(ITEM_DELIMITER)[2];
        String diagramName = task.split(ITEM_DELIMITER)[1];

        Layouter layouter = new CompartmentCrossCostGridLayouter();
        DataElement targetDiagramObj;
        String out = "CompartmentCrossCostGridLayouter Layout: " + "\n";
        String outputName = "CompartmentCrossCostGridLayouter.out";
        boolean append = false;
        File f = new File(path + "/" + outputName);
        if (f.exists())
        {
        append = true;
        out = "\n";
        }
        BufferedWriter buf = new BufferedWriter(new FileWriter(path + "/" + outputName, append));
        long totalTime = 0;

        String fullDiagName = "databases/" + databaseName + "/Diagrams/" + diagramName;

        targetDiagramObj = CollectionFactory.getDataElement(fullDiagName);

        Diagram diagram = (Diagram)targetDiagramObj;

        Graphics g = ApplicationUtils.getGraphics();
        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(diagram, g);

        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        setNodesFixed(graph, false);
        synchronized(diagramString){
            if( !diagramString.contains(diagramName + "\t" + databaseName + "\t" + graph.nodeCount() + "\t" + graph.edgeCount()) )
                diagramString.add(diagramName + "\t" + databaseName + "\t" + graph.nodeCount() + "\t" + graph.edgeCount());
            }
        int numberOfEstimatedOperations = 0;
        try
        {
        numberOfEstimatedOperations = layouter.estimate(graph, 0);
        }
        catch (Exception e ) {e.printStackTrace();}
        LayoutJobControl lJC = null;//new LayoutJobControlImpl(numberOfEstimatedOperations);

        long time1 = System.currentTimeMillis();

        PathwayLayouter pl = new PathwayLayouter(layouter);
        try
        {
        pl.doLayout(graph, lJC);
        }
        catch (Exception e ) {e.printStackTrace();}

        long time2 = System.currentTimeMillis();
        totalTime = time2 - time1;

        diagram.setView(null);
        DiagramToGraphTransformer.applyLayout(graph, diagram);

        BufferedImage image = ImageGenerator.generateDiagramImage(diagram);

        String imageName = databaseName + "_" + diagramName + "_CompartmentCrossCostGridLayouter.png";
        saveImage(image, imagePath + "/" + imageName);


        LayoutQualityMetrics m = LayoutQualityMetrics.getMetrics(graph);
        String titleString = "\n" + databaseName + "\t" + diagramName + "\t" + "time: "+ totalTime + "\n"+ "estimatedOperations: "+ numberOfEstimatedOperations+ ",   " + m.getDebugInfo() + "\n"
                + "+++++++++++++++++++++++++" + "\n";
        out += titleString;
        out += graph.generateText(true);
        out += "+++++++++++++++++++++++++" + "\n";
        buf.write(out);
        out = "";
        synchronized(compCrossCostString){
        compCrossCostString.add(makeResultString(totalTime, numberOfEstimatedOperations, m));
        }
        buf.flush();
    }


    public static void forceDirectedLayoutTest(String task) throws Exception
    {
        String databaseName = task.split(ITEM_DELIMITER)[2];
        String diagramName = task.split(ITEM_DELIMITER)[1];

        Layouter layouter = new ForceDirectedLayouter();
        DataElement targetDiagramObj;
        String out = "ForceDirectedLayouter Layout: " + "\n";
        String outputName = "ForceDirectedLayouter.out";
        boolean append = false;
        File f = new File(path + "/" + outputName);
        if (f.exists())
        {
        append = true;
        out = "\n";
        }
        BufferedWriter buf = new BufferedWriter(new FileWriter(path + "/" + outputName, append));
        long totalTime = 0;

        String fullDiagName = "databases/" + databaseName + "/Diagrams/" + diagramName;

        targetDiagramObj = CollectionFactory.getDataElement(fullDiagName);

        Diagram diagram = (Diagram)targetDiagramObj;

        Graphics g = ApplicationUtils.getGraphics();
        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(diagram, g);

        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        setNodesFixed(graph, false);
        synchronized(diagramString){
            if( !diagramString.contains(diagramName + "\t" + databaseName + "\t" + graph.nodeCount() + "\t" + graph.edgeCount()) )
                diagramString.add(diagramName + "\t" + databaseName + "\t" + graph.nodeCount() + "\t" + graph.edgeCount());
            }
        int numberOfEstimatedOperations = 0;
        try
        {
        numberOfEstimatedOperations = layouter.estimate(graph, 0);
        }
        catch (Exception e ) {e.printStackTrace();}
        LayoutJobControl lJC = null;//new LayoutJobControlImpl(numberOfEstimatedOperations);

        long time1 = System.currentTimeMillis();

        PathwayLayouter pl = new PathwayLayouter(layouter);
        try
        {
        pl.doLayout(graph, lJC);
        }
        catch (Exception e ) {e.printStackTrace();}

        long time2 = System.currentTimeMillis();
        totalTime = time2 - time1;

        diagram.setView(null);
        DiagramToGraphTransformer.applyLayout(graph, diagram);

        BufferedImage image = ImageGenerator.generateDiagramImage(diagram);

        String imageName = databaseName + "_" + diagramName + "_ForceDirectedLayouter.png";
        saveImage(image, imagePath + "/" + imageName);


        LayoutQualityMetrics m = LayoutQualityMetrics.getMetrics(graph);
        String titleString = "\n" + databaseName + "\t" + diagramName + "\t" + "time: "+ totalTime + "\n"+ "estimatedOperations: "+ numberOfEstimatedOperations+ ",   " + m.getDebugInfo() + "\n"
                + "+++++++++++++++++++++++++" + "\n";
        out += titleString;
        out += graph.generateText(true);
        out += "+++++++++++++++++++++++++" + "\n";
        buf.write(out);
        out = "";
        synchronized(forceDirectedString){
        forceDirectedString.add(makeResultString(totalTime, numberOfEstimatedOperations, m));
        }
        buf.flush();
    }


    public static void fastGridLayoutTest(String task) throws Exception
    {
        String databaseName = task.split(ITEM_DELIMITER)[2];
        String diagramName = task.split(ITEM_DELIMITER)[1];

        Layouter layouter = new FastGridLayouter();
        DataElement targetDiagramObj;
        String out = "FastGridLayouter Layout: " + "\n";
        String outputName = "FastGridLayouter.out";
        boolean append = false;
        File f = new File(path + "/" + outputName);
        if (f.exists())
            {
            append = true;
            out = "\n";
            }
        BufferedWriter buf = new BufferedWriter(new FileWriter(path + "/" + outputName, append));
//        BufferedWriter buf = new BufferedWriter(new FileWriter(path + "/" + outputName));
        long totalTime = 0;

        String fullDiagName = "databases/" + databaseName + "/Diagrams/" + diagramName;

        targetDiagramObj = CollectionFactory.getDataElement(fullDiagName);

        Diagram diagram = (Diagram)targetDiagramObj;

        Graphics g = ApplicationUtils.getGraphics();
        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(diagram, g);

        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        setNodesFixed(graph, false);
        synchronized(diagramString){
            if( !diagramString.contains(diagramName + "\t" + databaseName + "\t" + graph.nodeCount() + "\t" + graph.edgeCount()) )
                diagramString.add(diagramName + "\t" + databaseName + "\t" + graph.nodeCount() + "\t" + graph.edgeCount());
            }
        int numberOfEstimatedOperations = 0;
        try
        {
        numberOfEstimatedOperations = layouter.estimate(graph, 0);
        }
        catch (Exception e ) {e.printStackTrace();}
        LayoutJobControl lJC = new LayoutJobControlImpl(numberOfEstimatedOperations);

        long time1 = System.currentTimeMillis();

        PathwayLayouter pl = new PathwayLayouter(layouter);
        try
        {
        pl.doLayout(graph, lJC);
        }
        catch (Exception e ) {e.printStackTrace();}

        long time2 = System.currentTimeMillis();
        totalTime = time2 - time1;

        diagram.setView(null);
        DiagramToGraphTransformer.applyLayout(graph, diagram);
        BufferedImage image = ImageGenerator.generateDiagramImage(diagram);

        String imageName = databaseName + "_" + diagramName + "_FastGridLayouter.png";
        saveImage(image, imagePath + "/" + imageName);


        LayoutQualityMetrics m = LayoutQualityMetrics.getMetrics(graph);
        String titleString = "\n" + databaseName + "\t" + diagramName + "\t" + "time: "+ totalTime + "\n"+"estimatedOperations: "+ numberOfEstimatedOperations+ ",   " + m.getDebugInfo() + "\n"
                + "+++++++++++++++++++++++++" + "\n";
        out += titleString;
        out += graph.generateText(true);
        out += "+++++++++++++++++++++++++" + "\n";
        buf.write(out);
        out = "";
        synchronized(fastGridString){
        fastGridString.add(makeResultString(totalTime, numberOfEstimatedOperations, m));
        }

        buf.flush();
    }

    public static void processParameters() throws Exception
    {
        try (BufferedReader reader = ApplicationUtils.asciiReader( path + "/" + "parameters.txt" ))
        {
            String currString;
            while( ( currString = reader.readLine() ) != null )
            {
                
                if( currString.startsWith("databases:") )
                {
                    processDatabaseString(currString);
                }
                if( currString.startsWith("layouters:") )
                {
                    processLayouterString(currString);
                }
                if( currString.startsWith("Diagram Names:") )
                {
                    processDiagramNamesTag(currString, reader);
                }
            }
        }
    }

    public static void processDatabaseString(String databaseString) throws Exception
    {
        databaseString = databaseString.replaceFirst("databases:", "").trim();
        for( String name : DATABASE_NAMES )
            if( databaseString.contains(name) )
                dataBaseNames.add(name);
    }

    public static void processLayouterString(String layouterString) throws Exception
    {
        layouterString = layouterString.replaceFirst("layouters:", "").trim();
        for( String name : LAYOUTER_NAMES )
            if( layouterString.contains(name) )
                layouterNames.add(name);
    }

    public static void processDiagramNamesTag(String layouterString, BufferedReader reader) throws Exception
    {
        String currString;
        while( ( currString = reader.readLine() ) != null )
        {
            diagramNames.add(currString.trim());
        }
    }

    // returns tab delimited String of layoutQualityMetrics parameters in order:  time<\t>numberOfEstimatedOperations<\t>width<\t>height<\t>ratio<\t>bends<\t>crossings<\t>confluences<\t>withoutPath<\t>edgeLength<\t>sigma
    public static String makeResultString(long time, long numberOfEstimatedOperations, LayoutQualityMetrics m) throws Exception
    {
        DecimalFormat df = new DecimalFormat("#.###");
        return time + "\t" + numberOfEstimatedOperations + "\t" + m.width + "\t" + m.height + "\t"
                + df.format((float)m.width / (float)m.height) + "\t" + m.bends + "\t" + m.crossings + "\t" + m.confluences + "\t"
                + m.withoutPath + "\t" + df.format(m.edgeLength) + "\t" + df.format(m.sigma);
    }

    public static void makeHTMLFile(Vector<String>[] tableColumns) throws Exception
    {
        String outputName = "testresult.htm";
        StringBuilder out = new StringBuilder();
        out.append("<html><head><title>Layout test result</title></head><body>");

        out.append(makeHTMLTable(tableColumns, 2, 10, true, STRING_DELIMITER));

        out.append("</body></html>");
        ApplicationUtils.writeString( new File(path, outputName), out.toString() );
    }

    public static String makeHTMLTable(Vector<String>[] tableColumns, int tableBorder, int cellpadding, boolean haveHead, String rowDelimiter)
            throws Exception
    {
        StringBuilder table = new StringBuilder();
        StringBuilder tableRow;
        table.append("<table border =").append(tableBorder).append(" cellpadding=").append(cellpadding).append(">");
        if( haveHead )
            table.append(makeHTMLTableRow(headerString, rowDelimiter));
        for( int i = 0; i < tableColumns[0].size(); i++ )
        {
            tableRow = new StringBuilder();
            for( Vector<String> tableColumn : tableColumns )
            {
                 tableRow.append(rowDelimiter).append(tableColumn.get(i));
            }
            table.append(makeHTMLTableRow(tableRow.toString().replaceFirst(rowDelimiter, ""), rowDelimiter));
        }
        table.append("</table>");
        return table.toString();
    }

    public static String makeHTMLTableRow(String tableRowString, String delimiter) throws Exception
    {
        StringBuilder row = new StringBuilder();
        String[] rowArr = tableRowString.split(delimiter);
        row.append("<tr>");
        for( String element : rowArr )
        {
            row.append(makeHTMLTableDetail(element));
        }
        row.append("</tr>");
        return row.toString();
    }

    public static String makeHTMLTableDetail(String tableDetailString) throws Exception
    {
        StringBuilder detail = new StringBuilder();
        if( tableDetailString.contains("\t") )
        {
            if( tableDetailString.split("\t").length == DIAGRAM_COLUMN.size() )
            {
                Vector<String>[] tempArr = new Vector[2];
                tempArr[0]=DIAGRAM_COLUMN;
                tempArr[1]=makeVector(tableDetailString,"\t");
                detail.append("<td  align = left valign=top>" + makeHTMLTable(tempArr, 0, 1, false, "\t") + "</td>");
                currentDiagName = tableDetailString.split("\t")[1] + "_" + tableDetailString.split("\t")[0];
                getScaleCoeffResizeAndSave(currentDiagName, PICTURE_WIDTH);
                currentLayouterIndex = 0;
            }
            else
            {
                Vector<String>[] tempArr = new Vector[2];
                tempArr[0]=LAYOUTER_COLUMN;
                tempArr[1]=makeVector(tableDetailString,"\t");
                detail.append("<td  align = left valign=top>" + makeHTMLTable(tempArr, 0, 1, false, "\t") + "<img src =\""
                        + currentDiagName + "_" + layouterNames.get(currentLayouterIndex) + ".png" + "\">" + "</td>");
                //              detail.append("<td  align = left valign=top>"+makeHTMLTable(tempArr,0,1, false, "\t")+"<img src =\"" +currentDiagName +"_"+ layouterNames.get(currentLayouterIndex)+".png"+"\" width=100 height=100>"+"</td>");
                currentLayouterIndex++;
            }
        }
        else
        {
            detail.append("<td  align = left valign=top>" + tableDetailString + "</td>");
        }
        return detail.toString();
    }
    
    public static void readyTest() throws Exception
    {
      System.out.println("Layout test complete.");
    }

    public static void htmlTest() throws Exception
    {
        Vector<String>[] tableColumns = new Vector [columnNumber];
        
        int counter = 0;
        tableColumns[counter++] = diagramString;
        if( layouterNames.contains("CompartmentCrossCostGridLayouter") )
        {
            tableColumns[counter++] = compCrossCostString;
        }
        if( layouterNames.contains("HierarchicLayouter") )
        {
            tableColumns[counter++] = hierarchicString;
        }
        if( layouterNames.contains("ForceDirectedLayouter") )
        {
            tableColumns[counter++] = forceDirectedString;
        }
        if( layouterNames.contains("FastGridLayouter") )
        {
            tableColumns[counter++] = fastGridString;
        }

        makeHTMLFile(tableColumns);
    }

    public static void getScaleCoeffResizeAndSave(String currentDiagName, int fitWidth)
    {
        double scaleCoeff = 100;
        for( int i = 0; i < layouterNames.size(); i++ )
        {
            String fullImageName = imagePath + "/" + currentDiagName + "_" + layouterNames.get(i) + ".png";
            try
            {
                File f = new File(fullImageName);
                BufferedImage image = ImageIO.read(f);
                int w = image.getWidth();
                int h = image.getHeight();
                double maxWH = Math.max(w, h);
                double newScale = 1 / Math.log(Math.E + maxWH / fitWidth - 1);
                if( newScale < scaleCoeff )
                    scaleCoeff = newScale;
                if (maxWH*scaleCoeff > MAX_PICTURE_SIZE)
                    scaleCoeff = MAX_PICTURE_SIZE/maxWH;
 
            }
            catch( IOException e )
            {
                System.out.println("image missing");
            }
        }

        for( int i = 0; i < layouterNames.size(); i++ )
        {
            String fullImageName = imagePath + "/" + currentDiagName + "_" + layouterNames.get(i) + ".png";
            try
            {
                File f = new File(fullImageName);
                BufferedImage image = ImageIO.read(f);
                image = resize(image, PICTURE_WIDTH, scaleCoeff);
                saveImage(image, fullImageName);
            }
            catch( IOException e )
            {
                System.out.println("image missing");
            }
        }
    }

    public static BufferedImage resize(BufferedImage image, int fitWidth, double scaleCoeff)
    {
        int w = image.getWidth();
        int h = image.getHeight();

        if( scaleCoeff == -1 )
            scaleCoeff = Math.log(Math.E + (double)w / (double)fitWidth - 1);
        int newW = (int) ( w * scaleCoeff );
        int newH = (int) ( h * scaleCoeff );

        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_3BYTE_BGR); //image.getType()
        Graphics2D graphics = dimg.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, newW, newH, 0, 0, w, h, null);
        graphics.dispose();
        return dimg;
    }

    public static void saveImage(BufferedImage image, String path)
    {
        try (FileOutputStream fos = new FileOutputStream( path ))
        {
            ImageIO.write(image, "png", fos);
            image.flush();
        }
        catch( IOException e )
        {
            System.out.println("can't save image");
        }
    }

    protected static void setNodesFixed(Graph graph, boolean fix)
    {
        Iterator<Node> nodeIter = graph.nodeIterator();
        while( nodeIter.hasNext() )
        {
            nodeIter.next().fixed = fix;
        }
    }

    protected static void formQueueString()
    {
        queue = new Vector<>();
        for( String databaseName : dataBaseNames )
        {
            for( String diagramName : diagramNames )
            {
                String fullDiagName = "databases/" + databaseName + "/Diagrams/" + diagramName;
                if( CollectionFactory.getDataElement(fullDiagName) != null )
                {
                    for( String layouterName : layouterNames )
                    {
                        queue.add(layouterName + ITEM_DELIMITER + diagramName + ITEM_DELIMITER + databaseName);
                    }
                }
            }
        }
    }

    protected static synchronized String updateQueue(Vector<String> queueVect)
    {
        String taskString = "";
        if (queueVect.size() != 0)
        {
         taskString = queueVect.get(0);
         queueVect.removeElementAt(0);
         queue = queueVect;
        }
        else
        {
            queue = new Vector<>();
        }

        return taskString.trim();
    }
    
    public static void main(String[] args) throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }

}
