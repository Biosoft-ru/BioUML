package biouml.plugins.sbgn.sbgnml._test;

import java.awt.Dimension;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbgn.SbgnDiagramViewOptions;
import biouml.plugins.sbgn.sbgnml.SbgnMlReader;
import biouml.plugins.sbgn.sbgnml.SbgnMlWriter;
import biouml.standard.diagram.Util;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.workbench.diagram.ImageExporter;
import junit.framework.Test;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class TestSbgnMlExportImport extends AbstractBioUMLTest
{
    private static final int ERROR_FOR_DIMENSION = 3; //due to different pen sizes of original and reimported, TODO: should be removed when we will read render extension
    private Map<String, String> bioumlToSbgnId;
    private Diagram diagram;
    private static final String TEST_PATH = "../data/test/biouml/plugins/sbgn/";
    private static final String SBGN_ML_FOLDER = TEST_PATH + "SBGN-ML/";

    private static final String SBML_LAYOUT_HTML_REF = "SBML_Layout/";
    private static final String SPECIFICATION_FOLDER = "Specification/";
    private static final String BIOUML_FOLDER = "BioUML/";
    private static final String VANTED_FOLDER = "VANTED/";

    public TestSbgnMlExportImport(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestSbgnMlExportImport.class.getName());
        suite.addTest(new TestSbgnMlExportImport("test"));
        return suite;
    }

    //TODO: we yet do not support render extension
    private static Set<String> EXCEPTIONS = new HashSet<String>()
    {
        {
            add(SBGNPropertyConstants.STYLE_ATTR);
            add(ConnectionPort.VARIABLE_NAME_ATTR); //TODO: composite models reading should be improved
        }
    };

    Map<String, Diagram> bioumlDiagrams;
    Map<String, Diagram> reimportedDiagrams;
    Map<String, Diagram> importedDiagrams;

    private static List<String> names = new ArrayList<String>()
    {
        {
            add("Glycolysis");
            add("MAPK_cascade");
            add("Neuro-muscular junction");
            add("Repressilator");
            add("IGF signaling");
            add("IRF1 gene induction");
        }
    };

    Map<String, String> reports;

    public void test() throws Exception
    {
        File bioumlDir = new File(TEST_PATH + BIOUML_FOLDER);
        File testDir = new File(TEST_PATH);


        bioumlDir.mkdirs();
        for( File f : bioumlDir.listFiles() )
            f.delete();

        for( File f : testDir.listFiles() )
            if( !f.isDirectory() )
                f.delete();

        bioumlDiagrams = new HashMap<>();
        reimportedDiagrams = new HashMap<>();
        importedDiagrams = new HashMap<>();
        reports = new HashMap<>();

        test("Glycolysis");
        test("MAPK_cascade");
        test("Neuro-muscular junction");
        test("Repressilator");
        test("IGF signaling");
        test("IRF1 gene induction");

        generateReport(TEST_PATH, "SBGNML_report.html");

        int failedTests = 0;
        for( Map.Entry<String, String> e : reports.entrySet() )
        {
            String name = e.getKey();
            String report = e.getValue();
            if( report.isEmpty() )
                continue;

            failedTests++;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(TEST_PATH, name + "_Report.txt"))))
            {
                bw.write(report);
            }
            System.out.println(name + " failed");
        }

        assertEquals( failedTests + " tests failed. ", 0, failedTests );
    }

    public void test(String name) throws Exception
    {
        DataCollection collection = getExampleCollection();
        assertNotNull(collection);
        Object object = collection.get(name);
        assert ( object instanceof Diagram );
        doTest((Diagram)object);
        testImport(name);
    }

    public void testImport(String name)
    {
        try
        {
            SbgnMlReader reader = new SbgnMlReader();
            Diagram importedDiagram = reader.read(null, new File(SBGN_ML_FOLDER, name + ".sbgn"), name + "_Imported");
            importedDiagram.getType().getDiagramViewBuilder().createDiagramView(importedDiagram, ApplicationUtils.getGraphics());
            importedDiagrams.put(name, importedDiagram);
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }

    }

    public void doTest(Diagram sourceDiagram)
    {
        try
        {
            assertNotNull(sourceDiagram);
            assert ( sourceDiagram.getType() instanceof SbgnDiagramType );

            sourceDiagram.getType().getDiagramViewBuilder().createDiagramView(sourceDiagram, ApplicationUtils.getGraphics());
            Util.moveToPositive(sourceDiagram);
            
            diagram = sourceDiagram.clone(null, sourceDiagram.getName());
            diagram.getType().getDiagramViewBuilder().createDiagramView(diagram, ApplicationUtils.getGraphics());

            File file = new File(TEST_PATH + BIOUML_FOLDER + diagram.getName() + "_Exported.xml");

            SbgnMlWriter writer = new SbgnMlWriter();
            writer.write(diagram, file);
            bioumlToSbgnId = writer.getBioumlToSbgnId();

            SbgnMlReader reader = new SbgnMlReader();
            Diagram newDiagram = reader.read(null, file, diagram.getName());
            newDiagram.getType().getDiagramViewBuilder().createDiagramView(newDiagram, ApplicationUtils.getGraphics());

            bioumlDiagrams.put(sourceDiagram.getName(), sourceDiagram);
            reimportedDiagrams.put(sourceDiagram.getName(), newDiagram);
            reports.put(diagram.getName(), compare(sourceDiagram, newDiagram));

        }
        catch( Exception ex )
        {
            reports.put(diagram.getName(), ex.getMessage() + ":\n" + StreamEx.of(ex.getStackTrace()).joining("\n"));
        }
    }

    protected String getSbgnId(DiagramElement de)
    {
        return bioumlToSbgnId.get(de.getCompleteNameInDiagram());
    }

    public static Diagram getExampleDiagram(String name) throws Exception
    {
        return getDiagram(DATA_RESOURCES_REPOSITORY, EXAMPLE_DIAGRAMS_COLLECTION, name);
    }

    public static DataCollection getExampleCollection() throws Exception
    {
        return getCollection(DATA_RESOURCES_REPOSITORY, EXAMPLE_DIAGRAMS_COLLECTION);
    }

    public static DataCollection getCollection(String repositoryPath, String collectionName) throws Exception
    {
        CollectionFactory.unregisterAllRoot();
        CollectionFactory.createRepository(repositoryPath);
        return CollectionFactory.getDataCollection(collectionName);
    }

    public static Diagram getDiagram(String repositoryPath, String collectionName, String name) throws Exception
    {
        CollectionFactory.unregisterAllRoot();
        CollectionFactory.createRepository(repositoryPath);
        DataCollection collection1 = CollectionFactory.getDataCollection(collectionName);
        DataElement de = collection1.get(name);
        return (Diagram)de;
    }

    protected void generateImage(Diagram diagram, String dir, String fileName) throws Exception
    {
        ImageExporter imageWriter = new ImageExporter();
        File file = new File(dir, fileName + ".png");
        Properties properties = new Properties();
        properties.setProperty(DataElementExporterRegistry.FORMAT, "PNG");
        properties.setProperty(DataElementExporterRegistry.SUFFIX, ".png");
        imageWriter.init(properties);
        imageWriter.doExport(diagram, file);
    }

    protected static final String endl = "\n";

    protected void generateReport(String dir, String fileName) throws Exception
    {
        File f = new File(dir, fileName);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f)))
        {
            bw.write("<html>" + endl);
            bw.write("<head>" + endl);
            bw.write("<title>SBGN models</title>" + endl);
            bw.write("</head>" + endl);
            bw.write("<body>" + endl);

            bw.write( "<table border=\"1\">" );

            String header = String.join( "</center></td><td><center>", "Original", "BioUML", "SBML Layout", "VANTED",
                    "Manually created in BioUML", "Exported from BioUML and imported back" );
            bw.write("<tr><td><center>");
            bw.write(header);
            bw.write( "</center></td></tr>" + endl );
            for( String name: names )
            {
                Diagram originalDiagram = bioumlDiagrams.get(name);

                if( originalDiagram != null )
                    generateImage(originalDiagram, TEST_PATH + BIOUML_FOLDER, name);

                String reimportName = name + "_Reimport";
                Diagram reimportDiagram = reimportedDiagrams.get(name);
                if( reimportDiagram != null )
                    generateImage(reimportDiagram, TEST_PATH + BIOUML_FOLDER, reimportName);

                String importName = name + "_Imported";
                Diagram importDiagram = importedDiagrams.get(name);
                if( importDiagram != null )
                    generateImage(importDiagram, TEST_PATH + BIOUML_FOLDER, importName);


                bw.write( "<tr><td colspan = \"6\"><center>" + name + "</center></td></tr>" + endl );
                bw.write("<tr>");
                bw.write( "<td><img src=\"" + SPECIFICATION_FOLDER + name + ".png\" //width=\"600\"/></td>" + endl );

                bw.write( "<td><img src=\"" + BIOUML_FOLDER + importName + ".png\" //width=\"600\"/></td>" + endl );
                bw.write( "<td><img src=\"" + SBML_LAYOUT_HTML_REF + name + ".png\" //width=\"600\"/></td>" + endl );
                bw.write( "<td><img src=\"" + VANTED_FOLDER + name + ".png\" //width=\"600\"/></td>" + endl );
                bw.write( "<td><img src=\"" + BIOUML_FOLDER + name + ".png\" //width=\"600\"/></td>" + endl );
                bw.write( "<td><img src=\"" + BIOUML_FOLDER + reimportName + ".png\" width=\"600\"/></td>" + endl );
                bw.write("</tr>" + endl);
            }

            bw.write("</table>" + endl);
            bw.write("</body>" + endl);
            bw.write("</html>" + endl);
        }
    }

    protected String compare(Diagram diagram, Diagram newDiagram) throws Exception
    {
        StringBuilder report = new StringBuilder();
        if( diagram.recursiveStream().count() != newDiagram.recursiveStream().count() )
            report.append("different number of elements\n");

        for( DiagramElement de : diagram.recursiveStream() )
        {
            if( de instanceof Diagram )
                continue;

            DiagramElement otherDe = newDiagram.getDiagramElement(de.getCompleteNameInDiagram());
            if( otherDe == null )
                otherDe = newDiagram.findDiagramElement(getSbgnId(de));
            if( otherDe == null )
            {
                for( DiagramElement newDe : newDiagram.recursiveStream() )
                {
                    if( newDe.getTitle().equals(getSbgnId(de)) )
                        otherDe = newDe;
                }
            }

            if( otherDe == null )
            {
                report.append("can not find diagram element " + de.getCompleteNameInDiagram() + "\n");
                continue;
            }


            if( de instanceof Compartment )
                compare(de, ( (Compartment)otherDe ).getSize(), ( (Compartment)de ).getSize(), "wrong number of elements", report);

            if( de.getRole() != null )
            {
                compare(de, otherDe.getRole().getClass(), de.getRole().getClass(), "wrong role", report);

                if( de.getRole() instanceof VariableRole )
                {
                    compare( de, otherDe.getRole( VariableRole.class ).getAssociatedElements().length,
                            otherDe.getRole( VariableRole.class ).getAssociatedElements().length, "wrong clones number",
                            report );
                }
            }

            if( de instanceof Edge )
                compare(de, ( (Edge)otherDe ).getPath(), ( (Edge)de ).getPath(), "wrong path", report);

            if( de instanceof Node )
            {
                compare(de, ( (Node)otherDe ).getLocation(), ( (Node)de ).getLocation(), "wrong location", report);

                if( ( (Node)de ).getShapeSize() != null )
                    compare(de, ( (Node)otherDe ).getShapeSize(), ( (Node)de ).getShapeSize(), "wrong shapesize", report);
            }

            for( DynamicProperty dp : de.getAttributes() )
            {
                if( EXCEPTIONS.contains(dp.getName()) )
                    continue;
                
                //if original diagram doesn't care about orientations neither should we
                if (! ((SbgnDiagramViewOptions)diagram.getViewOptions()).isOrientedReactions() && de instanceof Node && Util.isReaction(de) && SBGNPropertyConstants.ORIENTATION.equals(dp.getName()))
                    continue;
                
                DynamicProperty otherDP = otherDe.getAttributes().getProperty(dp.getName());

                if( otherDP == null )
                {
                    report.append("Can not find property " + dp.getName() + " in node " + otherDe.getCompleteNameInDiagram() + "\n");
                    continue;
                }

                compare(de, otherDP.getType(), dp.getType(), "wrong property type " + dp.getName(), report);
                compare(de, otherDP.getValue(), dp.getValue(), "wrong property value " + dp.getName(), report);
            }

            compare(de, otherDe.getKernel().getType(), de.getKernel().getType(), "wrong kernel type ", report);
            compare(de, otherDe.getKernel().getClass(), otherDe.getKernel().getClass(), "wrong kernel class", report);

            if( de.getKernel() instanceof SpecieReference )
                compare(de, ( (SpecieReference)otherDe.getKernel() ).getRole(), ( (SpecieReference)de.getKernel() ).getRole(),
                        "wrong specie reference role", report);
        }
        return report.toString();
    }

    private void compare(DiagramElement de, Object obj1, Object obj2, String message, StringBuilder report)
    {
        boolean passed = false;
        if (obj1 instanceof Dimension)
        {
            Dimension d1 = (Dimension)obj1;
            Dimension d2 = (Dimension)obj2;
            passed = Math.max(Math.abs(d1.width - d2.width), Math.abs(d1.height - d2.height)) <= ERROR_FOR_DIMENSION;
        }
        else
        {
            passed = obj1.equals(obj2);
        }
        if( !passed)
            report.append(
                    de.getCompleteNameInDiagram() + " : " + message + " : " + obj1.toString() + ", expected " + obj2.toString() + ".\n");
    }

    public static final String DATA_RESOURCES_REPOSITORY = "../data_resources";
    public static final String EXAMPLE_DIAGRAMS_COLLECTION = "data/Examples/SBGN/Diagrams";
}
