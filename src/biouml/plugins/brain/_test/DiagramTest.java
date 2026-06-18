package biouml.plugins.brain._test;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.DiagramViewBuilder;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model._test.ViewTestCase;
import biouml.model.dynamics.TableElement;
import biouml.plugins.brain.diagram.BrainDiagramType;
import biouml.plugins.brain.diagram.BrainGenerateCompositeDiagramAction;
import biouml.plugins.brain.diagram.BrainReceptorModel;
import biouml.plugins.brain.diagram.BrainType;
import biouml.plugins.brain.diagram.BrainUtils;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.table.TableCSVImporter;
import ru.biosoft.table.TableCSVImporter.NullImportProperties;
import ru.biosoft.table.TableDataCollection;

import biouml.plugins.brain.diagram.BrainCellularModel;
import biouml.plugins.brain.diagram.BrainRegionalModel;
import biouml.plugins.brain.model.BrainCellularModelDeployer;
import biouml.plugins.brain.model.BrainReceptorModelDeployer;
import biouml.plugins.brain.model.BrainRegionalForMultilevelModelDeployer;
import biouml.plugins.brain.model.BrainRegionalModelDeployer;


public class DiagramTest extends ViewTestCase
{
	/**
	 * Here we store test data in 3 DataCollections
	 * 
	 * <pre>
	 * - data
	 *   - csv      - tables in CSV format (to import)  
	 *   - tables   - BioUML tables, we can specify corresponding path for model/dynamics/TableElement
	 *   - diagrams - brain diagrams
	 * </pre>  
	 */
	public static final String repositoryPath = "../data/test/biouml/plugins/brain";

	public static Test suite()
    {
        TestSuite suite = new TestSuite(DiagramTest.class.getName());

        suite.addTest(new DiagramTest("initRepository"));

        suite.addTest(new DiagramTest("importCSVTable"));
        
        suite.addTest(new DiagramTest("createRegionalDiagram"));
        suite.addTest(new DiagramTest("generateRegionalEquations"));
        suite.addTest(new DiagramTest("viewRegionalDiagram"));
        
        suite.addTest(new DiagramTest("createCellularDiagram"));
        suite.addTest(new DiagramTest("generateCellularEquations"));
        suite.addTest(new DiagramTest("viewCellularDiagram"));
        
        suite.addTest(new DiagramTest("createReceptorDiagram"));
        suite.addTest(new DiagramTest("generateReceptorEquations"));
        suite.addTest(new DiagramTest("viewReceptorDiagram"));
        
        suite.addTest(new DiagramTest("createCompDiagram"));
        suite.addTest(new DiagramTest("viewCompDiagram"));
        
        suite.addTest(new DiagramTest("createMultilevelModelDiagram"));
        suite.addTest(new DiagramTest("viewMultilevelModelDiagram"));
        
        return suite;
    }

    public DiagramTest(String name)
    {
        super(name);
    }

    @Override
    protected void tearDown() throws Exception	{}

    protected static DataCollection repository;
    public void initRepository() throws Exception
    {
    	System.out.println("roots:  " + CollectionFactory.getRootNames());
    	repository = CollectionFactory.createRepository(repositoryPath);
    	assertNotNull(repository);    	

    	System.out.println("data: " + repository.getNameList());    	
    }
        	
    protected static String tableName; // name of the table created by csv file
    public void importCSVTable() throws Exception
    {
    	String[] fileNameArr = {
    			"1 region", "2 regions", "3 regions", // test connectivity matrices
    			"31x31_clusters_400_440", "31x31_clusters_760_800", "31x31_allconnected", "31x31_alldisconnected", // Rossler connectivity matrices
    			"28x28_2017" // Epileptor connectivity matrices
    			};
    	
    	int csvFileIndex = 2;
    	
    	tableName = csvFileIndex <= fileNameArr.length ? fileNameArr[csvFileIndex] : "";
    	String csvFileName = tableName + ".csv";
    	
    	FileDataElement file = (FileDataElement)CollectionFactory.getDataElement("data/csv/" + csvFileName);
    	assertNotNull("Can not get file 'data/csv/" + csvFileName, file);    	

        DataCollection tables = CollectionFactory.getDataCollection("data/tables");
    	assertNotNull("Can not get tables data collection 'data/tables'", tables);    
    
    	TableCSVImporter importer = new TableCSVImporter();
    	NullImportProperties importProperties = (NullImportProperties)importer.getProperties(tables, null, null);
    	importProperties.setGenerateUniqueID(true);
    	importProperties.setTableType(null);
    	importer.doImport(tables, file.getFile(), tableName, null, null);
    }
    
    protected static Diagram regionalDiagram; // diagram with regional model type and connectivity matrix
    protected static Diagram regionalDiagramEq; // diagram with autogenerated regional equations
    public void createRegionalDiagram() throws Exception
    {
    	DiagramInfo info = new DiagramInfo(null, "Regional diagram");
    	regionalDiagram = (new BrainDiagramType()).createDiagram(null, info.getName(), info);
    	assertNotNull(regionalDiagram);    	
    	
    	DiagramInfo infoEq = new DiagramInfo(null, "Regional diagram with autogenerated equations");
    	regionalDiagramEq = (new BrainDiagramType()).createDiagram(null, infoEq.getName(), infoEq);
    	assertNotNull(regionalDiagramEq);    

    	String tablePath = "data/tables/" + tableName;
    	TableDataCollection table = (TableDataCollection)CollectionFactory.getDataCollection(tablePath);
    	assertNotNull("Can not get table: " + tablePath, table); 
    	
    	String connectivityMatrixName = "Connectivity matrix";
    	Node connectivityMatrixNode = new Node(regionalDiagram, new Stub(regionalDiagram, connectivityMatrixName, BrainType.TYPE_CONNECTIVITY_MATRIX));
        TableElement connectivityMatrixRole = new TableElement(connectivityMatrixNode);
        connectivityMatrixRole.setTable(table);
        connectivityMatrixNode.setRole(connectivityMatrixRole);
        connectivityMatrixNode.setLocation(new Point(0, 100));
        regionalDiagram.put(connectivityMatrixNode);
        
        String regionalModelName = "Regional model";
        Node regionalModelNode = new Node(regionalDiagram, new Stub(null, regionalModelName, BrainType.TYPE_REGIONAL_MODEL));
        BrainRegionalModel regionalModelRole = new BrainRegionalModel(regionalModelName);
        //regionalModelRole.setRegionalModelType(BrainType.TYPE_REGIONAL_ROSSLER);
        regionalModelRole.setRegionalModelType(BrainType.TYPE_REGIONAL_EPILEPTOR);
        
        regionalModelRole.getRegionalModelProperties().setPortsFlag(true);
        
        regionalModelRole.setDiagramElement(regionalModelNode);
        regionalModelNode.setRole(regionalModelRole);
        regionalDiagram.put(regionalModelNode);
        
        DataCollection diagrams = CollectionFactory.getDataCollection("data/diagrams");
    	assertNotNull("Can not get diagrams data collection 'data/diagrams'", diagrams);    	
        
        diagrams.put(regionalDiagram);
    }
    
    public void generateRegionalEquations() throws Exception
    {
    	int regionalModelCount = BrainUtils.getRegionalModelNodes(regionalDiagram).size();
        int connectivityMatrixCount = BrainUtils.getConnectivityMatrixNodes(regionalDiagram).size();
        
        assertTrue("regional model count != 1", regionalModelCount == 1);
        assertTrue("connectivity matrix count != 1", connectivityMatrixCount == 1);
        
        TableElement te = (TableElement)BrainUtils.getConnectivityMatrixNodes(regionalDiagram).get(0).getRole();
	    int sizeRows = te.getTable().getSize();
	    int sizeColumns = te.getVariables().length;
	   
	    assertTrue("connectivity matrix is not square", sizeRows == sizeColumns);

	    regionalDiagramEq = BrainRegionalModelDeployer.deployBrainRegionalModel(regionalDiagram, regionalDiagramEq.getName());
	    
        DataCollection diagrams = CollectionFactory.getDataCollection("data/diagrams");
      	assertNotNull("Can not get diagrams data collection 'data/diagrams'", diagrams); 
      	diagrams.put(regionalDiagramEq);
    }

    public void viewRegionalDiagram() throws Exception
    {
        regionalDiagram = (Diagram)CollectionFactory.getDataCollection("data/diagrams/" + regionalDiagram.getName());
    	assertNotNull("Can not get regional diagram", regionalDiagram); 
    	
    	DiagramViewBuilder builder = regionalDiagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(regionalDiagram, getGraphics());

        ViewPane pane = new ViewPane();
        pane.setView((CompositeView)regionalDiagram.getView());
        assertView(pane, regionalDiagram.getName());
        
        regionalDiagramEq = (Diagram)CollectionFactory.getDataCollection("data/diagrams/" + regionalDiagramEq.getName());
    	assertNotNull("Can not get regional diagram with equations", regionalDiagramEq); 
    	
    	builder = regionalDiagramEq.getType().getDiagramViewBuilder();
        builder.createDiagramView(regionalDiagramEq, getGraphics());

        pane = new ViewPane();
        pane.setView((CompositeView)regionalDiagramEq.getView());
        assertView(pane, regionalDiagramEq.getName());
    }
    
    protected static Diagram cellularDiagram; // diagram with cellular model type
    protected static Diagram cellularDiagramEq; // diagram with autogenerated cellular equations
    public void createCellularDiagram() throws Exception
    {
    	DiagramInfo info = new DiagramInfo(null, "Cellular diagram");
    	cellularDiagram = (new BrainDiagramType()).createDiagram(null, info.getName(), info);
    	assertNotNull(cellularDiagram);    	
    	
    	DiagramInfo infoEq = new DiagramInfo(null, "Cellular diagram with autogenerated equations");
    	cellularDiagramEq = (new BrainDiagramType()).createDiagram(null, infoEq.getName(), infoEq);
    	assertNotNull(cellularDiagramEq);    
        
        String cellularModelName = "Cellular model";
        Node cellularModelNode = new Node( cellularDiagram, new Stub( null, cellularModelName, BrainType.TYPE_CELLULAR_MODEL ) );
        BrainCellularModel cellularModelRole = new BrainCellularModel(cellularModelName);
        cellularModelRole.setCellularModelType(BrainType.TYPE_CELLULAR_EPILEPTOR2);
        //cellularModelRole.setCellularModelType(BrainType.TYPE_CELLULAR_OXYGEN);
        //cellularModelRole.setCellularModelType(BrainType.TYPE_CELLULAR_MINIMAL);
        //cellularModelRole.setCellularModelType(BrainType.TYPE_CELLULAR_EPILEPTOR2_WITH_OXYGEN);
        cellularModelRole.setDiagramElement(cellularModelNode);
        cellularModelNode.setRole(cellularModelRole);
        cellularDiagram.put(cellularModelNode);
        
        DataCollection diagrams = CollectionFactory.getDataCollection("data/diagrams");
    	assertNotNull("Can not get diagrams data collection 'data/diagrams'", diagrams);    	
        
        diagrams.put(cellularDiagram);
    }

    public void generateCellularEquations() throws Exception
    {
    	int cellularModelCount = BrainUtils.getCellularModelNodes(cellularDiagram).size();
        assertTrue("cellular model count != 1", cellularModelCount == 1);
        
        cellularDiagramEq = BrainCellularModelDeployer.deployBrainCellularModel(cellularDiagram, cellularDiagramEq.getName());
	    
        DataCollection diagrams = CollectionFactory.getDataCollection("data/diagrams");
      	assertNotNull("Can not get diagrams data collection 'data/diagrams'", diagrams); 
      	diagrams.put(cellularDiagramEq);
    }
    
    public void viewCellularDiagram() throws Exception
    {
        cellularDiagram = (Diagram)CollectionFactory.getDataCollection("data/diagrams/" + cellularDiagram.getName());
    	assertNotNull("Can not get cellular diagram", cellularDiagram); 
    	
    	DiagramViewBuilder builder = cellularDiagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(cellularDiagram, getGraphics());

        ViewPane pane = new ViewPane();
        pane.setView((CompositeView)cellularDiagram.getView());
        assertView(pane, cellularDiagram.getName());
        
        cellularDiagramEq = (Diagram)CollectionFactory.getDataCollection("data/diagrams/" + cellularDiagramEq.getName());
    	assertNotNull("Can not get cellular diagram with equations", cellularDiagramEq); 
    	
    	builder = cellularDiagramEq.getType().getDiagramViewBuilder();
        builder.createDiagramView(cellularDiagramEq, getGraphics());

        pane = new ViewPane();
        pane.setView((CompositeView)cellularDiagramEq.getView());
        assertView(pane, cellularDiagramEq.getName());
    }
    
    protected static Diagram receptorDiagram; // diagram with receptor model type
    protected static Diagram receptorDiagramEq; // diagram with autogenerated receptor equations
    public void createReceptorDiagram() throws Exception
    {
    	DiagramInfo info = new DiagramInfo(null, "Receptor diagram");
    	receptorDiagram = (new BrainDiagramType()).createDiagram(null, info.getName(), info);
    	assertNotNull(receptorDiagram);    	
    	
    	DiagramInfo infoEq = new DiagramInfo(null, "Receptor diagram with autogenerated equations");
    	receptorDiagramEq = (new BrainDiagramType()).createDiagram(null, infoEq.getName(), infoEq);
    	assertNotNull(receptorDiagramEq);    
        
        String receptorModelName = "Receptor model";
        Node receptorModelNode = new Node( receptorDiagram, new Stub( null, receptorModelName, BrainType.TYPE_RECEPTOR_MODEL ) );
        BrainReceptorModel receptorModelRole = new BrainReceptorModel(receptorModelName);
        receptorModelRole.setReceptorModelType(BrainType.TYPE_RECEPTOR_AMPA);
        receptorModelRole.setDiagramElement(receptorModelNode);
        receptorModelNode.setRole(receptorModelRole);
        receptorDiagram.put(receptorModelNode);
        
        DataCollection diagrams = CollectionFactory.getDataCollection("data/diagrams");
    	assertNotNull("Can not get diagrams data collection 'data/diagrams'", diagrams);    	
        
        diagrams.put(receptorDiagram);
    }

    public void generateReceptorEquations() throws Exception
    {
    	int receptorModelCount = BrainUtils.getReceptorModelNodes(receptorDiagram).size();
        assertTrue("receptor model count != 1", receptorModelCount == 1);
        
        receptorDiagramEq = BrainReceptorModelDeployer.deployBrainReceptorModel(receptorDiagram, receptorDiagramEq.getName());
	    
        DataCollection diagrams = CollectionFactory.getDataCollection("data/diagrams");
      	assertNotNull("Can not get diagrams data collection 'data/diagrams'", diagrams); 
      	diagrams.put(receptorDiagramEq);
    }
    
    public void viewReceptorDiagram() throws Exception
    {
    	receptorDiagram = (Diagram)CollectionFactory.getDataCollection("data/diagrams/" + receptorDiagram.getName());
    	assertNotNull("Can not get receptor diagram", receptorDiagram); 
    	
    	DiagramViewBuilder builder = receptorDiagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(receptorDiagram, getGraphics());

        ViewPane pane = new ViewPane();
        pane.setView((CompositeView)receptorDiagram.getView());
        assertView(pane, receptorDiagram.getName());
        
        receptorDiagramEq = (Diagram)CollectionFactory.getDataCollection("data/diagrams/" + receptorDiagramEq.getName());
    	assertNotNull("Can not get receptor diagram with equations", receptorDiagramEq); 
    	
    	builder = receptorDiagramEq.getType().getDiagramViewBuilder();
        builder.createDiagramView(receptorDiagramEq, getGraphics());

        pane = new ViewPane();
        pane.setView((CompositeView)receptorDiagramEq.getView());
        assertView(pane, receptorDiagramEq.getName());
    }
    
    protected static Diagram compDiagram; // composite diagram which includes cellular and regional models
    public void createCompDiagram() throws Exception
    { 	    
    	createRegionalDiagram();
    	BrainRegionalModel regionalModel = BrainUtils.getRegionalModelNodes(regionalDiagram).get(0).getRole(BrainRegionalModel.class);
    	regionalModel.setRegionalModelType(BrainType.TYPE_REGIONAL_ROSSLER);
    	regionalModel.getRegionalModelProperties().setPortsFlag(true);
    	generateRegionalEquations();
    	assertNotNull(regionalDiagramEq); 
    	
	    createCellularDiagram();
	    BrainCellularModel cellularModel = BrainUtils.getCellularModelNodes(cellularDiagram).get(0).getRole(BrainCellularModel.class);
	    cellularModel.setCellularModelType(BrainType.TYPE_CELLULAR_EPILEPTOR2_WITH_OXYGEN);
	    cellularModel.getCellularModelProperties().setPortsFlag(true);
	    generateCellularEquations();
    	assertNotNull(cellularDiagramEq);
	    
    	BrainGenerateCompositeDiagramAction generateCompDiagramAction = new BrainGenerateCompositeDiagramAction();
    	compDiagram = generateCompDiagramAction.initCompositeDiagram(cellularDiagramEq, regionalDiagramEq, cellularDiagramEq.getOrigin());
    	
        DataCollection diagrams = CollectionFactory.getDataCollection("data/diagrams");
    	assertNotNull("Can not get diagrams data collection 'data/diagrams'", diagrams);    	
        
        diagrams.put(compDiagram);
    }
    
    public void viewCompDiagram() throws Exception
    {
        compDiagram = (Diagram)CollectionFactory.getDataCollection("data/diagrams/" + compDiagram.getName());
    	assertNotNull("Can not get composite diagram", compDiagram); 
    	
    	DiagramViewBuilder builder = compDiagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(compDiagram, getGraphics());

        ViewPane pane = new ViewPane();
        pane.setView((CompositeView)compDiagram.getView());
        assertView(pane, compDiagram.getName());
    }
    
    protected static Diagram multilevelModelDiagram; // composite diagram, which includes a separate cellular model for each region in the regional model.
    public void createMultilevelModelDiagram() throws Exception
    {
        DataCollection diagrams = CollectionFactory.getDataCollection("data/diagrams");
      	assertNotNull("Can not get diagrams data collection 'data/diagrams'", diagrams); 
    	
    	createRegionalDiagram();
    	BrainRegionalModel regionalModel = BrainUtils.getRegionalModelNodes(regionalDiagram).get(0).getRole(BrainRegionalModel.class);
    	regionalModel.setRegionalModelType(BrainType.TYPE_REGIONAL_ROSSLER);
    	regionalModel.getRegionalModelProperties().setPortsFlag(true);
    	regionalDiagramEq = BrainRegionalForMultilevelModelDeployer.fillDiagramWithEquationsRosslerForMultilevelModel(regionalDiagram, regionalDiagramEq.getName());
      	diagrams.put(regionalDiagramEq);
	    assertNotNull(regionalDiagramEq); 
    	
	    createCellularDiagram();
	    BrainCellularModel cellularModel = BrainUtils.getCellularModelNodes(cellularDiagram).get(0).getRole(BrainCellularModel.class);
	    cellularModel.setCellularModelType(BrainType.TYPE_CELLULAR_EPILEPTOR2_WITH_OXYGEN);
	    cellularModel.getCellularModelProperties().setPortsFlag(true);
	    generateCellularEquations();
    	assertNotNull(cellularDiagramEq);
	    
    	int regionCount = BrainUtils.getConnectivityMatrix(regionalDiagram).length;
    	multilevelModelDiagram = initMultilevelModel(cellularDiagramEq, regionalDiagramEq, regionCount);	
          
        diagrams.put(multilevelModelDiagram);
    }
    
    public void viewMultilevelModelDiagram() throws Exception
    {
        compDiagram = (Diagram)CollectionFactory.getDataCollection("data/diagrams/" + multilevelModelDiagram.getName());
    	assertNotNull("Can not get multilevel model diagram", multilevelModelDiagram); 
    	
    	DiagramViewBuilder builder = multilevelModelDiagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(multilevelModelDiagram, getGraphics());

        ViewPane pane = new ViewPane();
        pane.setView((CompositeView)multilevelModelDiagram.getView());
        assertView(pane, multilevelModelDiagram.getName());
    }
    
    public Diagram initMultilevelModel(Diagram cellularDiagram, Diagram regionalDiagram, int regionCount) throws Exception
    {
    	Dimension cellularDiagramShape = new Dimension(200, 50);
    	Dimension regionalDiagramShape = new Dimension(300, 25 * regionCount);
    	
    	String compDiagramName = "Multilevel brain model";
    	DiagramType compDiagramType = new CompositeDiagramType();
    	Diagram compDiagram = compDiagramType.createDiagram(null, compDiagramName, new DiagramInfo(compDiagramName));
    	
    	SubDiagram regionalSubDiagram = new SubDiagram(compDiagram, regionalDiagram, regionalDiagram.getName());
    	regionalSubDiagram.setShapeSize(regionalDiagramShape);
    	compDiagram.put(regionalSubDiagram);
    	
    	int xDiagramOffset = 200;
    	int yDiagramOffset = 50;
    	
    	Point pointStart = new Point(0, 0);
    	
    	Point point = new Point(pointStart);
    	for (int i = 0; i < regionCount; i++)
    	{
    		Diagram nextCellularDiagram = cellularDiagram.clone(null, "Cellular diagram " + String.valueOf(i + 1));
    		SubDiagram nextCellularSubDiagram = new SubDiagram(compDiagram, nextCellularDiagram, nextCellularDiagram.getName());
    		nextCellularSubDiagram.setShapeSize(cellularDiagramShape);
        	compDiagram.put(nextCellularSubDiagram);
    		nextCellularSubDiagram.setLocation(point);
    		point = new Point(nextCellularSubDiagram.getLocation().x,
    				nextCellularSubDiagram.getLocation().y + nextCellularSubDiagram.getShapeSize().height + yDiagramOffset);
        	BrainUtils.alignPorts(nextCellularSubDiagram);
        	
        	Node portFrom = BrainUtils.findPort(nextCellularSubDiagram, "nu_norm");
        	Node portTo = BrainUtils.findPort(regionalSubDiagram, "u_exc_" + String.valueOf(i + 1));
         	DiagramUtility.createConnection(compDiagram, portFrom, portTo, true);
    	}
    	
    	point = new Point(pointStart.x + cellularDiagramShape.width + xDiagramOffset, 
    			pointStart.y);
    	regionalSubDiagram.setLocation(point);
    	BrainUtils.alignPorts(regionalSubDiagram);
    	
    	return compDiagram;
    }
}	

