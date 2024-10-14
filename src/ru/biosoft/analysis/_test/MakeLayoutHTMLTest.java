package ru.biosoft.analysis._test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Vector;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import ru.biosoft.graph.LayoutQualityMetrics;

import javax.imageio.ImageIO;

import one.util.streamex.StreamEx;

import com.developmentontheedge.application.ApplicationUtils;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;


public class MakeLayoutHTMLTest extends TestCase
{
    static String path = "ru/biosoft/analysis/_test/LayoutTestControl";
    static String imagePath = "ru/biosoft/analysis/_test/LayoutTestControl";
    
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
    static String FAILED_COLUMN = "";

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

    public MakeLayoutHTMLTest(String name)
    {
        super(name);
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite(MakeLayoutHTMLTest.class.getName());

        
        initVectors();
        makeTaskStrings();

        suite.addTest(new MakeLayoutHTMLTest("htmlTest"));
        return suite;
    }
    
    public static void initVectors() throws Exception
    {
        DIAGRAM_COLUMN = makeVector("Name" + "\t" + "Database" + "\t" + "Node&nbsp;count" + "\t" + "Edge&nbsp;count", "\t");
        LAYOUTER_COLUMN = makeVector ("time" + "\t" + "estimatedOperations" + "\t" + "Width" + "\t" + "Height" + "\t" + "Ratio"
                + "\t" + "Bends" + "\t" + "Crossings" + "\t" + "Confluences" + "\t" + "WithoutPath" + "\t" + "EdgeLength" + "\t" + "Sigma","\t");
        FAILED_COLUMN = "failed" + "\t" + "failed" + "\t" + "failed" + "\t" + "failed" + "\t" + "failed"
                + "\t" + "failed" + "\t" + "failed" + "\t" + "failed" + "\t" + "failed" + "\t" + "failed" + "\t" + "failed";
    }
    
    public static Vector<String> makeVector (String str, String delimiter)throws Exception
    {
        Vector<String> returnVect = new Vector<>();
        String[] iterArr =  str.split(delimiter);
        for (String s:iterArr) returnVect.add(s);
        return returnVect;
    }
    
     public static void makeTaskStrings () throws Exception
    {
        for (String lName : LAYOUTER_NAMES)
        {
        File f = new File(path + "/" + lName + ".out");
        if (f.exists())
            {
            layouterNames.add(lName);
            headerString += STRING_DELIMITER + "<b>"+lName+"</b>";
            columnNumber++;
            }
        }
        makeDiagramString(path + "/"+ layouterNames.get(0)+ ".out");
        for (String lName : layouterNames)
        {
            if (lName.equals("HierarchicLayouter"))
                makeLayouterString(lName, hierarchicString);
                else if (lName.equals("CompartmentCrossCostGridLayouter"))
                    makeLayouterString(lName, compCrossCostString);
                else if (lName.equals("ForceDirectedLayouter"))
                    makeLayouterString(lName, forceDirectedString);
                else if (lName.equals("FastGridLayouter"))
                    makeLayouterString(lName, fastGridString);
        }
//        for (String lName : LAYOUTER_NAMES)
//        {
//        makeLayouterString(lName)
//        }
        Vector<String> sss1 = hierarchicString;
        Vector<String> sss2 = fastGridString;
        Vector<String> sss3 = forceDirectedString;
        Vector<String> sss4 = compCrossCostString;
        
        for (int i = 0; i<diagramString.size(); i++)
        {
            if (hierarchicString.get(i) == null)
                {
                hierarchicString.remove(i);
                hierarchicString.add(i, FAILED_COLUMN);
                }
            if (fastGridString.get(i) == null)
            {
                fastGridString.remove(i);
                fastGridString.add(i, FAILED_COLUMN);
            }
            if (forceDirectedString.get(i) == null)
            {
                forceDirectedString.remove(i);
                forceDirectedString.add(i, FAILED_COLUMN);
            }
            
            if (compCrossCostString.get(i) == null)
                {
                compCrossCostString.remove(i);
                compCrossCostString.add(i, FAILED_COLUMN);
                }
        }
    }
    
    public static void makeDiagramString (String filePath) throws Exception
    {
        File f = new File(filePath);
        StringBuilder addString = new StringBuilder();
        try(BufferedReader reader = ApplicationUtils.utfReader( f ))
        {
            String currString;
            while( ( currString = reader.readLine() ) != null )
            {
               boolean flag = false;
               for (String s : DATABASE_NAMES)
               {
               if (currString.contains(s))
                   {
                   flag = true;
                   continue;
                   }
               }
               if (flag)
               {
                   if (addString.length() > 0) diagramString.add(addString.toString());
                   addString = new StringBuilder();
                   addString.append(currString.split("\t")[1]).append('\t').append(currString.split("\t")[0]);//currString.substring(0, currString.lastIndexOf("\t"));
               }
               else
               {
                   if (currString.startsWith("// Nodes:"))
                   {
                       addString.append('\t').append( currString.substring(currString.lastIndexOf(" ")+1));
                   }
                   if (currString.startsWith("// Edges:"))
                   {
                       addString.append('\t').append( currString.substring(currString.lastIndexOf(" ")+1));
                   }
               }
            }
        }
        if (addString.length() > 0) diagramString.add(addString.toString()); // add last element
        hierarchicString.setSize(diagramString.size());
        fastGridString.setSize(diagramString.size());
        forceDirectedString.setSize(diagramString.size());
        compCrossCostString.setSize(diagramString.size());
        
    }
    public static void makeLayouterString (String layouterName, Vector<String> layouterString) throws Exception
    {
        File f = new File(path + "/" + layouterName + ".out");
        String currString;
        StringBuilder findStrBuilder = new StringBuilder();
        StringBuilder addStrBuilder = new StringBuilder();
        try(BufferedReader reader = ApplicationUtils.utfReader( f ))
        {
            while( ( currString = reader.readLine() ) != null )
            {
                if( StreamEx.of( DATABASE_NAMES ).anyMatch( currString::contains ) )
                {
                    if( findStrBuilder.length() != 0 )
                    {
                        String temp = replaceAllStringData( addStrBuilder.toString() ).trim();
                        Vector<String> sss = diagramString;
                        int index = diagramString.indexOf( findStrBuilder.toString() );
                        //                    layouterString.add(diagramString.indexOf(findString), temp);
                        layouterString.remove( index );
                        layouterString.add( index, temp );
                    }
                    String[] currStrParts = currString.split( "\t" );
                    findStrBuilder = new StringBuilder();
                    findStrBuilder.append( currStrParts[1] ).append( "\t" ).append( currStrParts[0] );

                    addStrBuilder = new StringBuilder();
                    addStrBuilder.append( currString.substring( currString.lastIndexOf( "\t" ) + 1 ) ).append( "\t" ); // add time
                    //                addString += "0,00" + "\t";// add time
                }
                else
                {
                    if( currString.startsWith( "// Nodes:" ) )
                    {
                        findStrBuilder.append( "\t" ).append( currString.substring( currString.lastIndexOf( " " ) + 1 ) );
                    }
                    if( currString.startsWith( "// Edges:" ) )
                    {
                        findStrBuilder.append( "\t" ).append( currString.substring( currString.lastIndexOf( " " ) + 1 ) );
                    }
                    if( currString.startsWith( "estimatedOperations:" ) )
                    {
                        addStrBuilder.append( currString.replaceAll( ",   ", "\t" ) );
                    }
                }
            }
        }
        if( findStrBuilder.length() != 0 ) // for the last diagram
        {
            String temp = replaceAllStringData( addStrBuilder.toString() );
            int index = diagramString.indexOf( findStrBuilder.toString() );
            layouterString.remove( index );
            layouterString.add( index, temp );
        }
        Vector<String> sss = layouterString;
    }
   
    public static String replaceAllStringData(String task) throws Exception
    {
        String temp = task;
        for (String s : LAYOUTER_COLUMN)
        {
            temp = temp.replaceAll(s+": ", "");
        }
        return temp;
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
                detail.append("<td  align = left valign=top>").append(makeHTMLTable(tempArr, 0, 1, false, "\t")).append("</td>");
                currentDiagName = tableDetailString.split("\t")[1] + "_" + tableDetailString.split("\t")[0];
                getScaleCoeffResizeAndSave(currentDiagName, PICTURE_WIDTH);
                currentLayouterIndex = 0;
            }
            else
            {
                Vector<String>[] tempArr = new Vector[2];
                tempArr[0]=LAYOUTER_COLUMN;
                tempArr[1]=makeVector(tableDetailString,"\t");
                String fullImgName = currentDiagName + "_" + layouterNames.get(currentLayouterIndex) + ".png";
                String miniImgName = currentDiagName + "_" + layouterNames.get(currentLayouterIndex) + "_mini.png";
            
                int w=500;
                int h=500;
                try
                {
                    File f = new File(imagePath + "/"+fullImgName);
                    BufferedImage image = ImageIO.read(f);
                    w = image.getWidth();
                    h = image.getHeight();
                }
                catch( IOException e )
                {
                    System.out.println("image missing");
                }
                detail.append("<td  align = left valign=top>" + makeHTMLTable(tempArr, 0, 1, false, "\t")
                        + "<a href=\"javascript:window.open('" + fullImgName + "', '" + fullImgName + "',' width=" + w + ", height=" + h
                        + "')\">" + "<img src =\"" + miniImgName + "\">"+ "</a>" + "</td>");
                //              detail.append("<td  align = left valign=top>"+makeHTMLTable(tempArr,0,1, false, "\t")+"<img src =\"" +currentDiagName +"_"+ layouterNames.get(currentLayouterIndex)+".png"+"\" width=100 height=100>"+"</td>");
                currentLayouterIndex++;
            }
        }
        else
        {
            detail.append("<td  align = left valign=top>").append(tableDetailString).append("</td>");
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
            String miniImageName = imagePath + "/" + currentDiagName + "_" + layouterNames.get(i) + "_mini.png";
            try
            {
                File f = new File(fullImageName);
                BufferedImage image = ImageIO.read(f);
                image = resize(image, PICTURE_WIDTH, scaleCoeff);
                File fmini = new File(miniImageName);
                fmini.createNewFile();
                saveImage(image, miniImageName);
            }
            catch( IOException e )
            {
                System.out.println("image "+fullImageName+" missing");
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

     public static void main(String[] args) throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }

}
