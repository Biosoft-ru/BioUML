package biouml.plugins.sbml.celldesigner._test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.sbml.celldesigner.CellDesignerImporter;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

/**
 * @author tolstyh
 * Generates images for imported diagrams from PANTHER database to compare with CellDesigner
 */
public class CompareWithPantherTest extends AbstractBioUMLTest
{
    protected static final String TEMP_DATA_COLLECTION = "../../models/CellDesigner/CellDesigner diagrams";

    protected static final String SOURCE_PATH = "../../models/CellDesigner";//Path to PANTHER SBML(CellDesigner) files
    protected static final String RESULT_PATH = "../../models/CellDesigner/Comparison";//Result path
    protected static final String CD_IMAGES_FOLDER = SOURCE_PATH + "/Images";
    protected static final String CD_SBML_FOLDER = SOURCE_PATH + "/SBML";
    protected static final String IMAGES_CD_SHORT = "images_celldesigner";
    protected static final String IMAGES_BIOUML_SHORT = "images_biouml";
    protected static final String IMAGES_CD = RESULT_PATH + "/" + IMAGES_CD_SHORT;
    protected static final String IMAGES_BIOUML = RESULT_PATH + "/" + IMAGES_BIOUML_SHORT;
    protected static final String RESULT_PAGES_SHORT = "pages";
    protected static final String RESULT_PAGES = RESULT_PATH + "/" + RESULT_PAGES_SHORT;
    protected static final String IMAGES_BIOUML_SMALL_SHORT = "biouml";
    protected static final String IMAGES_CD_SMALL_SHORT = "cd";
    protected static final String IMAGES_BIOUML_SMALL = RESULT_PAGES + "/" + IMAGES_BIOUML_SMALL_SHORT;
    protected static final String IMAGES_CD_SMALL = RESULT_PAGES + "/" + IMAGES_CD_SMALL_SHORT;
    protected static final String MODEL_LIST = SOURCE_PATH + "/Model list.txt";
    protected static final String STOP_KEYWORD = "END";

    protected List<String> diagramNames = null;

    public CompareWithPantherTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(CompareWithPantherTest.class.getName());
        suite.addTest(new CompareWithPantherTest("testGenerateImages"));
        return suite;
    }

    public void testGenerateImages() throws Exception
    {
        init();
        try (PrintWriter out = new PrintWriter(new File(RESULT_PATH, "index.html")))
        {
            out.println("<html><head><title>Imported diagrams</title></head><body><table border=0>");
            for( String name : ApplicationUtils.readAsList(new File(SOURCE_PATH, MODEL_LIST)) )
            {
                if( name.equals(STOP_KEYWORD) )
                    break;
                System.out.println("Processing " + name);
                Diagram diagram = importDiagram(new File(CD_SBML_FOLDER, name + ".xml"));
                if( diagram != null )
                {
                    TestUtil.writeImage(diagram, new File(IMAGES_BIOUML, name + ".png"));
                    out.println("<tr><td><a href='" + RESULT_PAGES_SHORT + File.separator + name + ".html'>" + name + "</a></td></tr>");
                    diagramNames.add(name);
                }
                else
                {
                    System.err.println("Diagram is null: " + name);
                }
            }
            for( String diagramName : diagramNames )
                generateDiagramPage(diagramName);

            out.println("</table></body></html>");
        }
    }

    private void init() throws IOException
    {
        File resultlDir = new File(RESULT_PATH);
        ApplicationUtils.removeDir(resultlDir);
        resultlDir.mkdirs();

        new File(IMAGES_BIOUML).mkdirs();
        new File(IMAGES_CD).mkdirs();
        new File(RESULT_PAGES).mkdirs();
        new File(IMAGES_BIOUML_SMALL).mkdirs();
        new File(IMAGES_CD_SMALL).mkdirs();

        File cdImagesBase = new File(CD_IMAGES_FOLDER);
        if( cdImagesBase.exists() && cdImagesBase.isDirectory() )
        {
            for( File f : cdImagesBase.listFiles() )
                ApplicationUtils.copyFile(new File(IMAGES_CD, f.getName()), f);
        }
        diagramNames = new ArrayList<>();
    }

    protected Diagram importDiagram(File file) throws Exception
    {
        DataCollection<?> repository = CollectionFactory.createRepository(TEMP_DATA_COLLECTION);
        return (Diagram)new CellDesignerImporter().doImport(repository, file, file.getName(), null, null);
    }

    protected void generateDiagramPage(String diagramName) throws IOException, FileNotFoundException
    {
        File pagesDir = new File(RESULT_PAGES);
        //create small images
        TestUtil.resizeImage(new File(IMAGES_BIOUML, diagramName + ".png"), new File(IMAGES_BIOUML_SMALL, diagramName + ".png"));
        TestUtil.resizeImage(new File(IMAGES_CD, diagramName + ".png"), new File(IMAGES_CD_SMALL, diagramName + ".png"));

        StringBuffer page = new StringBuffer();
        page.append("<html>\n");
        page.append("<title>" + diagramName + "</title>\n");
        page.append("<head>\n</head>\n");
        page.append("<body>\n");
        page.append("  <table border=0 width=\"100%\" height=\"100%\">\n");
        page.append("    <tr height=\"10%\"><td align=\"center\" colspan=2><h3>");
        page.append(diagramName);
        page.append("</h3></td></tr>\n");
        page.append("    <tr height=\"5%\"><td align=\"center\">BioUML</td><td align=\"center\">CellDesigner</td></tr>\n");

        String imageName = diagramName + ".png";
        page.append("    <tr height=\"80%\"><td align=\"center\"><a href=\"../" + IMAGES_BIOUML_SHORT + "/" + imageName
                + "\"><img border=0 src='" + IMAGES_BIOUML_SMALL_SHORT + "/" + imageName + "'/></a></td><td align=\"center\"><a href=\"../"
                + IMAGES_CD_SHORT + "/" + imageName + "\"><img border=0 src='" + IMAGES_CD_SMALL_SHORT + "/" + imageName
                + "'/></a></td></tr>\n");

        int currentPos = diagramNames.indexOf(diagramName);
        int size = diagramNames.size();
        String first = diagramNames.get(0);
        String prev = ( currentPos == 0 ) ? null : diagramNames.get(currentPos - 1);
        String next = ( currentPos == ( size - 1 ) ) ? null : diagramNames.get(currentPos + 1);
        String last = diagramNames.get(size - 1);
        page.append("    <tr height=\"5%\"><td align=\"center\" colspan=2>\n");
        page.append("      <a href=\"" + first + ".html\">First</a>&nbsp;\n");
        page.append(prev != null ? "      <a href=\"" + prev + ".html\">Prev</a>&nbsp;\n" : "      Prev&nbsp;\n");
        page.append(next != null ? "      <a href=\"" + next + ".html\">Next</a>&nbsp;\n" : "      Next&nbsp;\n");
        page.append("      <a href=\"" + last + ".html\">Last</a>&nbsp;\n");
        page.append("      <a href=\"../index.html\">List</a>&nbsp;\n");
        page.append("    </td></tr>\n");
        page.append("   </table>\n");
        page.append("</body>\n");
        page.append("</html>\n");

        try (PrintWriter out = new PrintWriter(new File(pagesDir, diagramName + ".html")))
        {
            out.print(page.toString());
        }
    }
}
