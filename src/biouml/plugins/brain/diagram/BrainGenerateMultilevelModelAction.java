package biouml.plugins.brain.diagram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.DiagramViewBuilder;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.util.DiagramXmlConstants;
import biouml.plugins.agentmodeling.AgentModelDiagramType;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.AgentSimulationEngineWrapper;
import biouml.plugins.brain.model.BrainCellularModelDeployer;
import biouml.plugins.brain.model.BrainReceptorModelDeployer;
import biouml.plugins.brain.model.BrainRegionalModelDeployer;
import biouml.plugins.brain.sde.EulerStochastic;
import biouml.plugins.brain.sde.JavaSdeSimulationEngine;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.EulerSimple.ESOptions;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.diagram.CompositeSemanticController;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PathwaySimulationDiagramViewOptions;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.access.subaction.DynamicAction;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.util.DPSUtils;

@SuppressWarnings ("serial")
public class BrainGenerateMultilevelModelAction extends BackgroundDynamicAction
{	
    public BrainGenerateMultilevelModelAction()
    {
        setNumSelected(DynamicAction.SELECTED_ZERO_OR_ANY);
    }
    @Override
    public void validateParameters(Object model, List<DataElement> selectedItems)
    {
        if (!isApplicable(model))
            throw new ParameterNotAcceptableException("Document", String.valueOf(model));
    }

    @Override
    public boolean isApplicable(Object object)
    {
        if (!(object instanceof Diagram))
        {
        	return false;
        }
        
        Diagram diagram = (Diagram)object;
        DiagramType type = diagram.getType();
        if (type == null)
            return false;
        return type instanceof BrainDiagramType;
    }

    @Override
    public JobControl getJobControl(Object model, List<DataElement> selectedItems, Object properties) throws Exception
    {
        return new AbstractJobControl(log)
        {
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    Diagram diagram = (Diagram)model;
                 
                    int regionalModelCount = BrainUtils.getRegionalModelNodes(diagram).size();
                    int connectivityMatrixCount = BrainUtils.getConnectivityMatrixNodes(diagram).size();
                    int cellularModelCount = BrainUtils.getCellularModelNodes(diagram).size();
                    int receptorModelCount = BrainUtils.getReceptorModelNodes(diagram).size();
                    
                    if (regionalModelCount == 1 && connectivityMatrixCount == 1 
                    		&& cellularModelCount == 1 && receptorModelCount == 1) 
                    {
                    	log.log(Level.INFO, "BrainGenerateMultilevelModel: starting multilevel model generation.");
                    	
                    	BrainRegionalModel regionalModel = BrainUtils.getRegionalModelNodes(diagram).get(0).getRole(BrainRegionalModel.class);
                    	regionalModel.getRegionalModelProperties().setPortsFlag(true);
                    	//Diagram regionalDiagram = BrainRegionalForMultilevelModelDeployer.deployBrainRegionalForMultilevelModel(diagram, "Regional diagram");
                    	Diagram regionalDiagram = BrainRegionalModelDeployer.deployBrainRegionalModel(diagram, "Regional_diagram");
                    	CollectionFactoryUtils.save(regionalDiagram);
                    	
                    	int regionCount = BrainUtils.getConnectivityMatrix(diagram).length;
                    	setSameEpileptogenicityIndex(regionalDiagram, regionCount, -2.3);
                    	
                        List<Integer> epileptogenicZoneNodesNumbers = new ArrayList<Integer>();
                        if (regionCount == 2)
                        {
                        	epileptogenicZoneNodesNumbers.add(1);
                        }
                        else if (regionCount == 28)
                        {
                         	epileptogenicZoneNodesNumbers.addAll(Arrays.asList(23, 24, 25));
                        }
                        else if (regionCount == 84)
                        {
                        	epileptogenicZoneNodesNumbers.add(64);
                        	
                        	//epileptogenicZoneNodesNumbers.add(61);
                        }
                    	
                    	BrainCellularModel cellularModel = BrainUtils.getCellularModelNodes(diagram).get(0).getRole(BrainCellularModel.class);
                       	String cellularModelType = cellularModel.getCellularModelType();
                    	if (!(cellularModelType.equals(BrainType.TYPE_CELLULAR_EPILEPTOR2)
                    			|| cellularModelType.equals(BrainType.TYPE_CELLULAR_EPILEPTOR2_WITH_OXYGEN))) 
                    	{
                    		log.log(Level.WARNING, "BrainGenerateMultilevelModel: cellular model should be Epileptor-2 or Epileptor-2 with oxygen dynamics.");
                    		return;
                    	}
                    	cellularModel.getCellularModelProperties().setPortsFlag(true);
                    	
                    	Diagram cellularEpileptogenicDiagram = BrainCellularModelDeployer.deployBrainCellularModel(diagram, "Cellular_diagram_EZ");
            			BrainUtils.setInitialValue(cellularEpileptogenicDiagram, "epileptogenicity_flag", 1.0);
            			BrainUtils.setInitialValue(cellularEpileptogenicDiagram, "A_exc", 14.0);
            			BrainUtils.setInitialValue(cellularEpileptogenicDiagram, "tau_K", 100.0);
                        BrainUtils.createPort("time_start", "time_start", Type.TYPE_INPUT_CONNECTION_PORT,
                        		cellularEpileptogenicDiagram, new Point(650, 0));
            			CollectionFactoryUtils.save(cellularEpileptogenicDiagram);
                    	
                    	Diagram cellularHealthyDiagram = BrainCellularModelDeployer.deployBrainCellularModel(diagram, "Cellular_diagram_HZ");
            			BrainUtils.setInitialValue(cellularHealthyDiagram, "epileptogenicity_flag", 0.0);
            			BrainUtils.setInitialValue(cellularHealthyDiagram, "A_exc", 0.0);
            			BrainUtils.setInitialValue(cellularHealthyDiagram, "tau_K", 2.5);
                    	BrainUtils.setInitialValue(cellularHealthyDiagram, "time_start", Double.MAX_VALUE);
                    	CollectionFactoryUtils.save(cellularHealthyDiagram);
            			
                    	BrainReceptorModel receptorModel = BrainUtils.getReceptorModelNodes(diagram).get(0).getRole(BrainReceptorModel.class);
                    	receptorModel.getReceptorModelProperties().setPortsFlag(true);
                    	
                    	Diagram receptorEpileptogenicDiagram = BrainReceptorModelDeployer.deployBrainReceptorModel(diagram, "Receptor_diagram_EZ");
                    	BrainUtils.setInitialValue(receptorEpileptogenicDiagram, "epileptogenicity_flag", 1.0);
                    	CollectionFactoryUtils.save(receptorEpileptogenicDiagram);
                    	
                    	Diagram receptorHealthyDiagram = BrainReceptorModelDeployer.deployBrainReceptorModel(diagram, "Receptor_diagram_HZ");
                    	BrainUtils.setInitialValue(receptorHealthyDiagram, "epileptogenicity_flag", 0.0);
                    	CollectionFactoryUtils.save(receptorHealthyDiagram);
                    	
                    	//initAndSaveRosslerEpileptor2CompositeModel(regionalDiagram, cellularDiagram, regionCount, diagram.getOrigin());
                    	initAndSaveEpileptorEpileptor2AmpaAgentModel(regionalDiagram, 
                    			cellularEpileptogenicDiagram, cellularHealthyDiagram,
                    			receptorEpileptogenicDiagram, receptorHealthyDiagram,
                    			regionCount, epileptogenicZoneNodesNumbers, diagram.getOrigin());
                    	
                    	log.log(Level.INFO, "BrainGenerateMultilevelModel: multilevel model generation is completed.");
                    	return;
                    }
                    else 
                    {
                    	log.log(Level.WARNING, "BrainGenerateMultilevelModel: please, provide 1 receprot model, 1 cellular model, 1 regional model"
                    			+ " and 1 connectivity matrix for multilevel model generation.");
                    	return;
                    }         
                }
                catch(Exception e)
                {
                    throw new JobControlException(e);
                }
            }
        };
    }
    
    /*
     * method sets the same epileptogenicity index values for all regions in the epileptor model.
     */
    public static void setSameEpileptogenicityIndex(Diagram regionalDiagram, int regionCout, double epileptogenicityIndex)
    {
    	 for (int i = 1; i <= regionCout; i++) 
         {
         	String x0i = "x0_" + String.valueOf(i);
         	BrainUtils.setInitialValue(regionalDiagram, x0i, epileptogenicityIndex);
         }
    }
    
    /*
     * method creates agent model for Epileptor + Epileptor-2 + AMPA multilevel model, 
     * which includes a separate cellular model for each region in the regional model
     * and separete AMPA-receptors model for each cellular model.
     * 
     * regional submodel is saved to subdirectory /regional_submodels,
     * cellular submodels are saved to subdirectory /cellular_submodels,
     * AMPA-receptors submodels are saved to subdirectory /receptor_submodels,
     * multilevel model is saved to origin.
     */
    public static Diagram initAndSaveEpileptorEpileptor2AmpaAgentModel(Diagram regionalDiagram, 
    		Diagram cellularEpileptogenicDiagram, Diagram cellularHealthyDiagram,
    		Diagram receptorEpileptogenicDiagram, Diagram receptorHealthyDiagram,
    		int nRegion, List<Integer> epileptogenicZoneNodesNumbers, DataCollection<?> origin) throws Exception
    {    	
//    	DataElementPath regionalSubModelsPath = DataElementPath.create(origin.getCompletePath().toString() + "/regional_submodels");
//    	DataCollectionUtils.createFoldersForPath(regionalSubModelsPath);
//    	DataCollection<DataElement> regionalSubModelsDataCollection = DataCollectionUtils.createSubCollection(regionalSubModelsPath);
//    	
//    	DataElementPath cellularSubModelsPath = DataElementPath.create(origin.getCompletePath().toString() + "/cellular_submodels");
//    	DataCollectionUtils.createFoldersForPath(cellularSubModelsPath);
//    	DataCollection<DataElement> cellularSubModelsDataCollection = DataCollectionUtils.createSubCollection(cellularSubModelsPath);
//    	
//    	DataElementPath receptorsSubModelsPath = DataElementPath.create(origin.getCompletePath().toString() + "/receptor_submodels");
//    	DataCollectionUtils.createFoldersForPath(receptorsSubModelsPath);
//    	DataCollection<DataElement> receptorSubModelsDataCollection = DataCollectionUtils.createSubCollection(receptorsSubModelsPath);
    	
    	String agentDiagramName = "Brain_multilevel_agent_model";
    	DiagramType agentDiagramType = new AgentModelDiagramType();
    	Diagram agentDiagram = agentDiagramType.createDiagram(origin, agentDiagramName, new DiagramInfo(agentDiagramName));
    	PathwaySimulationDiagramViewOptions compDiagramViewOptions = (PathwaySimulationDiagramViewOptions)agentDiagram.getViewOptions();
    	compDiagramViewOptions.setNodeTitleFont(new ColorFont("Arial", Font.PLAIN, 16, Color.black));
    	compDiagramViewOptions.setPortTitleFont(new ColorFont("Arial", Font.PLAIN, 16, Color.black));
    	compDiagramViewOptions.setCompartmentTitleFont(new ColorFont("Arial", Font.BOLD, 20, Color.black));
    	CollectionFactoryUtils.save(agentDiagram);
    	
    	DiagramViewBuilder builder = agentDiagram.getType().getDiagramViewBuilder();
    	
    	int xLocalOffset = 50;
    	int xGroupOffset = 80;
    	int yLocalOffset = 40;
    	int xDiagramOffset = 400;
    	int yDiagramOffset = 200;
    	
    	Point point = new Point(0,0);
    	 
        BrainUtils.createNote("Info: N = " + String.valueOf(nRegion), new Dimension(100, 25), agentDiagram, point);
        point.translate(0, yLocalOffset * 2);
        
        BrainUtils.setInitialValue(agentDiagram, "time_start", 50.0, "EZ time trigger");
        Node timePort = BrainUtils.createPort("time_start", "time_start",
        		Type.TYPE_OUTPUT_CONNECTION_PORT, Stub.ConnectionPort.PRIVATE, agentDiagram, point);
        Node timeBus = BrainUtils.createBus("time_start", Color.black, agentDiagram, new Point(point.x + builder.getNodeBounds(timePort).width + xLocalOffset, point.y));
        DiagramUtility.createConnection(agentDiagram, timePort, timeBus, true);
        BrainUtils.alignCenterY(agentDiagram, timePort, timeBus);
        
        point.translate(xDiagramOffset, 0);
        
    	List<SubDiagram> receptorSubDiagrams = new ArrayList<SubDiagram>();
    	List<SubDiagram> cellularSubDiagrams = new ArrayList<SubDiagram>();
    	
        int yPos = point.y;
    	SubDiagram nextReceptorSubDiagram = null;
    	for (int i = 0; i < nRegion; i++)
    	{
    		if (epileptogenicZoneNodesNumbers.contains(i+ 1))
    		{
    			nextReceptorSubDiagram = new SubDiagram(agentDiagram, receptorEpileptogenicDiagram, "Receptor_diagram_" + (i + 1));
    		}
    		else
    		{
    			nextReceptorSubDiagram = new SubDiagram(agentDiagram, receptorHealthyDiagram, "Receptor_diagram_" + (i + 1));
    		}
    		//nextReceptorSubDiagram.setShapeSize(receptorDiagramShape);
    		nextReceptorSubDiagram.setLocation(new Point(point.x, yPos));
    		//BrainUtils.alignPorts(nextReceptorSubDiagram);
    		layoutReceptor(nextReceptorSubDiagram, i + 1);
    		yPos += nextReceptorSubDiagram.getShapeSize().height + yDiagramOffset / 2;
    		agentDiagram.put(nextReceptorSubDiagram);
    		receptorSubDiagrams.add(nextReceptorSubDiagram);
    	}
    	point.translate(nextReceptorSubDiagram.getShapeSize().width + xDiagramOffset, 0);
    	
    	yPos = point.y;
     	SubDiagram nextCellularSubDiagram = null;
     	for (int i = 0; i < nRegion; i++)
     	{
     		boolean isEpileptogenic;
     		if (epileptogenicZoneNodesNumbers.contains(i + 1))
     		{
     			isEpileptogenic = true;
     			nextCellularSubDiagram = new SubDiagram(agentDiagram, cellularEpileptogenicDiagram, "Cellular_diagram_" + (i + 1));
     		}
     		else
     		{
     			isEpileptogenic = false;
     			nextCellularSubDiagram = new SubDiagram(agentDiagram, cellularHealthyDiagram, "Cellular_diagram_" + (i + 1));
     		}
     		//nextCellularSubDiagram.setShapeSize(cellularDiagramShape);
     		nextCellularSubDiagram.setLocation(new Point(point.x, yPos));
     		//BrainUtils.alignPorts(nextCellularSubDiagram);
     		layoutCellular(nextCellularSubDiagram, i + 1, isEpileptogenic);
     		yPos += nextCellularSubDiagram.getShapeSize().height + yDiagramOffset / 2;
     		agentDiagram.put(nextCellularSubDiagram);
     		cellularSubDiagrams.add(nextCellularSubDiagram);
     	}
     	point.translate(nextCellularSubDiagram.getShapeSize().width + xDiagramOffset, 0);
    	
    	SubDiagram regionalSubDiagram = new SubDiagram(agentDiagram, regionalDiagram, regionalDiagram.getName());
    	//regionalSubDiagram.setShapeSize(regionalDiagramShape);
    	regionalSubDiagram.setLocation(point);
    	//BrainUtils.alignPorts(regionalSubDiagram);
    	layoutRegional(regionalSubDiagram, nRegion);
    	agentDiagram.put(regionalSubDiagram);

    	
    	AgentModelSimulationEngine agentEngine = new AgentModelSimulationEngine();
    	agentEngine.setDiagram(agentDiagram);
    	agentEngine.setInitialTime(0.0);
    	double simTimeMax = 30000.0;
    	agentEngine.setCompletionTime(simTimeMax);
    	agentEngine.setTimeIncrement(1.0);
    	
    	//JavaSdeSimulationEngine mainEngine = new JavaSdeSimulationEngine();
    	JavaSimulationEngine mainEngine = new JavaSimulationEngine();
    	agentEngine.setMainEngine(mainEngine);
    	//EulerStochastic mainSolver = new EulerStochastic();
    	JVodeSolver mainSolver = new JVodeSolver();
    	mainEngine.setSolver(mainSolver);
    	//ESOptions mainOptions = mainSolver.getOptions();
    	//mainOptions.setInitialStep(0.005);
    	
    	for(AgentSimulationEngineWrapper ase : agentEngine.getEngines())
        {    		
    		if (ase.getDiagram().getName().contains(regionalDiagram.getName()))
    		{
    			JavaSdeSimulationEngine regionalEngine = new JavaSdeSimulationEngine();
    		    ase.setEngine(regionalEngine);
    			EulerStochastic regionalSolver = new EulerStochastic();
    			ESOptions regionalOptions = regionalSolver.getOptions();
    			regionalOptions.setInitialStep(0.005);
    			regionalOptions.setEventLocation(false); // there is no events in regional model
    			ase.setSolver(regionalSolver);
    	        ase.setCompletionTime(simTimeMax);
    	        ase.setTimeIncrement(1.0);
    	        ase.setTimeScale(1.0);
    	        
    	        // write the solver options in the submodel diagram.
    	        DiagramUtility.setPreferredEngine(ase.getDiagram(), regionalEngine);
    	        ase.getDiagram().save();
    		}
    		else if (ase.getDiagram().getName().contains(cellularHealthyDiagram.getName()))
    		{
    			JavaSdeSimulationEngine cellularEngine = new JavaSdeSimulationEngine();
    			ase.setEngine(cellularEngine);
    			EulerStochastic cellularSolver = new EulerStochastic();
    			ESOptions cellularOptions = cellularSolver.getOptions();
    			cellularOptions.setInitialStep(0.001);
    			ase.setSolver(cellularSolver);
    			ase.setCompletionTime(simTimeMax / 50.0);
    			ase.setTimeIncrement(0.01);
    			ase.setTimeScale(50.0);
    		}
    		else if (ase.getDiagram().getName().contains(receptorHealthyDiagram.getName()))
    		{
    			JavaSimulationEngine receptorEngine = new JavaSimulationEngine();
    			ase.setEngine(receptorEngine);
    			JVodeSolver receptorSolver = new JVodeSolver();
    			ase.setSolver(receptorSolver);
    			ase.setCompletionTime(simTimeMax / 50.0);
    			ase.setTimeIncrement(1.0);
    			ase.setTimeScale(50.0);
    		}
        }
        agentDiagram.getAttributes()
                .add( DPSUtils.createHiddenReadOnlyTransient(DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, agentEngine));
        
        setPlotsEpileptorEpileptor2AmpaAgentModel(agentDiagram, regionalSubDiagram, cellularSubDiagrams, receptorSubDiagrams, 
        		nRegion, epileptogenicZoneNodesNumbers);
        
    	CollectionFactoryUtils.save(agentDiagram);
    	return agentDiagram;
    }
    
    /*
     * method creates composite diagram for Rossler + Epileptor-2 multilevel model, 
     * which includes a separate cellular model for each region in the regional model.
     * cellular models are saved to subdirectory /cellular submodels,
     * regional models are saved to subdirectory /regional submodels,
     * multilevel model is saved to origin.
     */
    public static Diagram initAndSaveRosslerEpileptor2CompositeModel(Diagram regionalDiagram, Diagram cellularDiagram, int regionCount, DataCollection<?> origin) throws Exception
    {
    	DataElementPath regionalSubModelsPath = DataElementPath.create(origin.getCompletePath().toString() + "/regional submodels");
    	DataCollectionUtils.createFoldersForPath(regionalSubModelsPath);
    	DataCollection<DataElement> regionalSubModelsDataCollection = DataCollectionUtils.createSubCollection(regionalSubModelsPath);
    	
    	DataElementPath cellularSubModelsPath = DataElementPath.create(origin.getCompletePath().toString() + "/cellular submodels");
    	DataCollectionUtils.createFoldersForPath(cellularSubModelsPath);
    	DataCollection<DataElement> cellularSubModelsDataCollection = DataCollectionUtils.createSubCollection(cellularSubModelsPath);
    	
    	Dimension cellularDiagramShape = new Dimension(200, 50);
    	Dimension regionalDiagramShape = new Dimension(300, 25 * regionCount);
    	
    	regionalDiagram.setOrigin(regionalSubModelsDataCollection);
    	regionalSubModelsDataCollection.put(regionalDiagram);
    	
    	String compDiagramName = "Brain multilevel model";
    	DiagramType compDiagramType = new CompositeDiagramType();
    	Diagram compDiagram = compDiagramType.createDiagram(origin, compDiagramName, new DiagramInfo(compDiagramName));
    	
    	SubDiagram regionalSubDiagram = new SubDiagram(compDiagram, regionalDiagram, regionalDiagram.getName());
    	regionalSubDiagram.setShapeSize(regionalDiagramShape);
    	compDiagram.put(regionalSubDiagram);
    	
    	int xDiagramOffset = 200;
    	int yDiagramOffset = 50;
    	
    	Point pointStart = new Point(0, 0);
    	
    	List<SubDiagram> cellularSubDiagrams = new ArrayList<SubDiagram>();
    	
    	Point point = new Point(pointStart);
    	for (int i = 0; i < regionCount; i++)
    	{
    		Diagram nextCellularDiagram = cellularDiagram.clone(cellularSubModelsDataCollection, cellularDiagram.getName() + " " + String.valueOf(i + 1));
    		cellularSubModelsDataCollection.put(nextCellularDiagram);
    		SubDiagram nextCellularSubDiagram = new SubDiagram(compDiagram, nextCellularDiagram, nextCellularDiagram.getName());
    		nextCellularSubDiagram.setShapeSize(cellularDiagramShape);
        	compDiagram.put(nextCellularSubDiagram);
    		nextCellularSubDiagram.setLocation(point);
    		point = new Point(nextCellularSubDiagram.getLocation().x,
    				nextCellularSubDiagram.getLocation().y + nextCellularSubDiagram.getShapeSize().height + yDiagramOffset);
        	BrainUtils.alignPorts(nextCellularSubDiagram);
        	cellularSubDiagrams.add(nextCellularSubDiagram);
        	
        	Node portFrom = BrainUtils.findPort(nextCellularSubDiagram, "nu_norm");
        	Node portTo = BrainUtils.findPort(regionalSubDiagram, "u_exc_" + String.valueOf(i + 1));
         	DiagramUtility.createConnection(compDiagram, portFrom, portTo, true);
    	}
    	
    	point = new Point(pointStart.x + cellularDiagramShape.width + xDiagramOffset, 
    			pointStart.y);
    	regionalSubDiagram.setLocation(point);
    	BrainUtils.alignPorts(regionalSubDiagram);
    	
    	//Simulator settings
        JavaSdeSimulationEngine se = new JavaSdeSimulationEngine();
        se.setDiagram(compDiagram);
        se.setCompletionTime(2000.0);
        se.setTimeIncrement(0.1);
        se.setSolver(new EulerStochastic());
        compDiagram.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, se));
    	
        setPlotsRosslerEpileptor2CompositeModel(compDiagram, regionalSubDiagram, cellularSubDiagrams, regionCount);
        
    	CollectionFactoryUtils.save(compDiagram);
    	return compDiagram;
    }

    /*
     * method sets plots for multilevel agent model
     * which contains different cellular submodels for each region in regional model
     * and different receptor submodels for each cellular submodel
     * using variables from regional, cellular and receptor submodels.
     */
    private static void setPlotsEpileptorEpileptor2AmpaAgentModel(Diagram agentDiagram, SubDiagram regionalSubDiagram, List<SubDiagram> cellularSubDiagrams, List<SubDiagram> receptorSubDiagrams, 
    		int regionCount,  List<Integer> epileptogenicZoneNodesNumbers)
    {
    	int N = regionCount;
    	
    	EModel emodel = agentDiagram.getRole(EModel.class);
    	PlotsInfo plotsInfo = new PlotsInfo(emodel);
    	
    	Pen penBlack = new Pen(1, Color.black);
    	Pen penRed = new Pen(1, Color.red);
    	Pen penYellow = new Pen(1, Color.yellow);
    	Pen pen = new Pen();
    	
        PlotInfo varPlotsFirstHalf = new PlotInfo();
        varPlotsFirstHalf.setTitle("LFP recordings regions 1-" + (int)Math.floor(N / 2));
        varPlotsFirstHalf.setXTitle("time (s)");
        varPlotsFirstHalf.setYTitle("node index");
        
        PlotInfo varPlotsSecondHalf = new PlotInfo();
        varPlotsSecondHalf.setTitle("LFP recordings regions " + (int)Math.floor(N / 2 + 1) + "-" + N);
        varPlotsSecondHalf.setXTitle("time (s)");
        varPlotsSecondHalf.setYTitle("node index");
        
        if ((int)Math.floor(N / 2) > 0) 
        {
        	plotsInfo.setPlots(new PlotInfo[] {varPlotsFirstHalf, varPlotsSecondHalf});
        }
        else
        {
        	plotsInfo.setPlots(new PlotInfo[] {varPlotsSecondHalf});
        }
        
        List<Curve> curvesFirstHalf = new ArrayList<Curve>();
        List<Curve> curvesSecondHalf = new ArrayList<Curve>();
        
        List<String> regionalVarsToPlot = new ArrayList<String>();
        for (int i = 1; i <= N; i++) 
        {
        	regionalVarsToPlot.add("LFP_" + String.valueOf(i));
        }

        for (int i = 0; i < regionalVarsToPlot.size(); i++)
        {
        	String regionalVar = regionalVarsToPlot.get(i);
        	Curve curve = new Curve(regionalSubDiagram.getName(), regionalVar, regionalVar, emodel);
        	
        	if (epileptogenicZoneNodesNumbers.contains(i + 1))
        	{
        		pen = penRed;
        	}
        	else
        	{
        		pen = penBlack;
        	}
        	curve.setPen(pen);
        	
        	if (i + 1 <= Math.floor(N / 2)) 
        	{
        		curvesFirstHalf.add(curve);
        	}
        	else
        	{
        		curvesSecondHalf.add(curve);
        	}
        }
        
        varPlotsFirstHalf.setXVariable(new Curve(regionalSubDiagram.getName(), "time_sec", "time_sec", emodel));
        varPlotsFirstHalf.setYVariables(curvesFirstHalf.stream().toArray(Curve[]::new));
        
        varPlotsSecondHalf.setXVariable(new Curve(regionalSubDiagram.getName(), "time_sec", "time_sec", emodel));
        varPlotsSecondHalf.setYVariables(curvesSecondHalf.stream().toArray(Curve[]::new));
        
        DiagramUtility.setPlotsInfo(agentDiagram, plotsInfo);
    }
    
    /*
     * method sets plots on multilevel model composite diagram
     * which contains different cellular model for each region in regional model
     * using variables from cellular submodels and regional submodel.
     */
    private static void setPlotsRosslerEpileptor2CompositeModel(Diagram compDiagram, SubDiagram regionalSubDiagram, List<SubDiagram> cellularSubDiagrams, int regionCount)
    {
    	EModel emodel = compDiagram.getRole(EModel.class);
    	PlotsInfo plotsInfo = new PlotsInfo(emodel);
    	
        PlotInfo varPlotsEegRecording = new PlotInfo();
        varPlotsEegRecording.setTitle("EEG Recording");
    	
        PlotInfo varPlotsExcitationSignalFirstHalf = new PlotInfo();
        varPlotsExcitationSignalFirstHalf.setTitle("Excitation signal 1-" + (int)Math.floor(regionCount / 2));
        PlotInfo varPlotsExcitationSignalSecondHalf = new PlotInfo();
        varPlotsExcitationSignalSecondHalf.setTitle("Excitation signal " + (int)Math.floor(regionCount / 2 + 1) + "-" + regionCount);
        
        PlotInfo varPlotsSynapticStrengthFirstHalf = new PlotInfo();
        varPlotsSynapticStrengthFirstHalf.setTitle("Synaptic strength 1-" + (int)Math.floor(regionCount / 2));
        PlotInfo varPlotsSynapticStrengthSecondHalf = new PlotInfo();
        varPlotsSynapticStrengthSecondHalf.setTitle("Synaptic strength " + (int)Math.floor(regionCount / 2 + 1) + "-" + regionCount);
        
        plotsInfo.setPlots(new PlotInfo[] 
            {
        	    varPlotsEegRecording, 
        	    varPlotsExcitationSignalFirstHalf, varPlotsExcitationSignalSecondHalf, 
        		varPlotsSynapticStrengthFirstHalf, varPlotsSynapticStrengthSecondHalf
        	}
        );
        
        if ((int)Math.floor(regionCount / 2) > 0)
        {
        	plotsInfo.setPlots(new PlotInfo[] 
                {
                    varPlotsEegRecording, 
                    varPlotsExcitationSignalFirstHalf, varPlotsExcitationSignalSecondHalf, 
                    varPlotsSynapticStrengthFirstHalf, varPlotsSynapticStrengthSecondHalf
                }
            );
        }
        else
        {
        	plotsInfo.setPlots(new PlotInfo[] 
                {
                    varPlotsEegRecording, 
                    varPlotsExcitationSignalFirstHalf, 
                    varPlotsSynapticStrengthFirstHalf
                }
            );
        }
        
        List<Curve> curvesEegRecording = new ArrayList<Curve>();
        curvesEegRecording.add(new Curve(regionalSubDiagram.getName(), "V_EEG", "V_EEG", emodel));

        List<Curve> curvesExcitationSignalFirstHalf = new ArrayList<Curve>();
        List<Curve> curvesExcitationSignalSecondHalf = new ArrayList<Curve>();
        List<Curve> curvesSynapticStrengthFirstHalf = new ArrayList<Curve>();
        List<Curve> curvesSynapticStrengthSecondHalf = new ArrayList<Curve>();
        
        for (int i = 0; i < regionCount; i++)
        {
        	String uExcMonitorI = "ExcitationMonitor_" + String.valueOf(i + 1);
        	String etaMonitorI = "SynapticMonitor_" + String.valueOf(i + 1);
        	
        	if (i + 1 <= Math.floor(regionCount / 2)) 
        	{
        		curvesExcitationSignalFirstHalf.add(new Curve(regionalSubDiagram.getName(), uExcMonitorI, uExcMonitorI, emodel));
        		curvesSynapticStrengthFirstHalf.add(new Curve(regionalSubDiagram.getName(), etaMonitorI, etaMonitorI, emodel));
        	}
        	else
        	{
        		curvesExcitationSignalSecondHalf.add(new Curve(regionalSubDiagram.getName(), uExcMonitorI, uExcMonitorI, emodel));
        		curvesSynapticStrengthSecondHalf.add(new Curve(regionalSubDiagram.getName(), etaMonitorI, etaMonitorI, emodel));
        	}
        }
        
        varPlotsEegRecording.setYVariables(curvesEegRecording.stream().toArray(Curve[]::new));
        varPlotsExcitationSignalFirstHalf.setYVariables(curvesExcitationSignalFirstHalf.stream().toArray(Curve[]::new));
        varPlotsExcitationSignalSecondHalf.setYVariables(curvesExcitationSignalSecondHalf.stream().toArray(Curve[]::new));
        varPlotsSynapticStrengthFirstHalf.setYVariables(curvesSynapticStrengthFirstHalf.stream().toArray(Curve[]::new));
        varPlotsSynapticStrengthSecondHalf.setYVariables(curvesSynapticStrengthSecondHalf.stream().toArray(Curve[]::new));
        DiagramUtility.setPlotsInfo(compDiagram, plotsInfo);
    }
    
    public static void layoutReceptor(SubDiagram receptorSubDiagram, int compNumber) throws Exception 
    {
        int xBorderOffset = 5;
        int yBorderOffset = 5;
         
        int subDiagramWidth = 0;
        int subDiagramHeight = 0;
        
        int xBusOffset = 30;
        int yBusOffset = 30;

        Diagram compDiagram = (Diagram)receptorSubDiagram.getOrigin();
        DiagramViewBuilder builder = compDiagram.getType().getDiagramViewBuilder();
        
        Node srPort = BrainUtils.findInputPortByTitle(receptorSubDiagram, "SR");
    	movePortToSide(srPort, receptorSubDiagram, Side.LEFT, builder);
    	subDiagramHeight += 3 * builder.getNodeBounds(srPort).height;
    	subDiagramWidth += builder.getNodeBounds(srPort).width;
    	
        Node sfPort = BrainUtils.findOutputPortByTitle(receptorSubDiagram, "SF");
    	movePortToSide(srPort, receptorSubDiagram, Side.RIGHT, builder);
    	subDiagramWidth += builder.getNodeBounds(sfPort).width;
    	
    	receptorSubDiagram.setShapeSize(new Dimension(
        		Math.max(subDiagramWidth + 2 * xBorderOffset, 400), 
        		Math.max(subDiagramHeight + 2 * yBorderOffset, 60))
    		);
        
        int yPos = receptorSubDiagram.getLocation().y + receptorSubDiagram.getShapeSize().height / 2 - builder.getNodeBounds(srPort).height / 2;
        movePortToSide(srPort, receptorSubDiagram, Side.LEFT, builder);
        srPort.setLocation(srPort.getLocation().x, yPos);
        Node srBus = BrainUtils.createBus(srPort.getTitle() + "_" + compNumber, Color.green, compDiagram, new Point(srPort.getLocation().x - xBusOffset, srPort.getLocation().y));
        srBus.setLocation(srBus.getLocation().x - builder.getNodeBounds(srBus).width, srBus.getLocation().y);
        BrainUtils.alignCenterY(compDiagram, srPort, srBus);
        DiagramUtility.createConnection(compDiagram, srBus, srPort, true);
       
        yPos = receptorSubDiagram.getLocation().y + receptorSubDiagram.getShapeSize().height / 2 - builder.getNodeBounds(sfPort).height / 2;
        movePortToSide(sfPort, receptorSubDiagram, Side.RIGHT, builder);
        sfPort.setLocation(sfPort.getLocation().x, yPos);
        Node sfBus = BrainUtils.createBus(sfPort.getTitle() + "_" + compNumber, Color.orange, compDiagram, new Point(sfPort.getLocation().x + builder.getNodeBounds(sfPort).width + xBusOffset, sfPort.getLocation().y));
        sfBus.setLocation(sfBus.getLocation().x, sfBus.getLocation().y);
        BrainUtils.alignCenterY(compDiagram, sfPort, sfBus);
        DiagramUtility.createConnection(compDiagram, sfPort, sfBus, true);
    }
    
    public static void layoutCellular(SubDiagram cellularSubDiagram, int compNumber, boolean isEpileptogenic) throws Exception 
    {
        int xBorderOffset = 5;
        int yBorderOffset = 5;
         
        int subDiagramWidth = 0;
        int subDiagramHeight = 0;
        
        int xBusOffset = 30;
        int yBusOffset = 30;

        Diagram compDiagram = (Diagram)cellularSubDiagram.getOrigin();
        DiagramViewBuilder builder = compDiagram.getType().getDiagramViewBuilder();
         
        Node sfPort = BrainUtils.findInputPortByTitle(cellularSubDiagram, "SF");
    	movePortToSide(sfPort, cellularSubDiagram, Side.LEFT, builder);
    	
        Node x1Port = BrainUtils.findInputPortByTitle(cellularSubDiagram, "x1");
    	movePortToSide(x1Port, cellularSubDiagram, Side.RIGHT, builder);
    	subDiagramWidth += Math.max(builder.getNodeBounds(sfPort).width, builder.getNodeBounds(x1Port).width);
         
        List<Node> rightPorts = new ArrayList<>();
        double rightPortsOffsetMultiplier = 1.5;
        List<String> rightBusNames = new ArrayList<>();
        for (String entity : new String[]{"FR_norm", "SF_norm", "SR"})
        {
            Node port = BrainUtils.findOutputPortByTitle(cellularSubDiagram, entity);
            movePortToSide(port, cellularSubDiagram, Side.RIGHT, builder);
            rightPorts.add(port);
            subDiagramHeight += rightPortsOffsetMultiplier * builder.getNodeBounds(port).height;
            
            String mapped;
            switch (entity) 
            {
                case "FR_norm": 
                	mapped = "u_exc";
                	break;
                case "SF_norm": 
                	mapped = "W";     
                	break;
                case "SR":      
                	mapped = "SR";    
                	break;
                default:        
                	mapped = entity;
            }
            String busName = mapped + "_" + compNumber;
            rightBusNames.add(busName);
        }
        
        if (isEpileptogenic)
        {
            Node timePort = BrainUtils.findInputPortByTitle(cellularSubDiagram, "time_start");
            movePortToSide(timePort, cellularSubDiagram, Side.TOP, builder);
        }
        subDiagramWidth += builder.getNodeBounds(rightPorts.get(0)).width;
        
        cellularSubDiagram.setShapeSize(new Dimension(
	        		Math.max(2 * subDiagramWidth + 2 * xBorderOffset, 400), 
	        		subDiagramHeight + 2 * yBorderOffset)
        		);
        
        int yPos = cellularSubDiagram.getLocation().y + cellularSubDiagram.getShapeSize().height * 1 / 4 - builder.getNodeBounds(sfPort).height / 2;
        movePortToSide(sfPort, cellularSubDiagram, Side.LEFT, builder);
        sfPort.setLocation(sfPort.getLocation().x, yPos);
        Node sfBus = BrainUtils.createBus(sfPort.getTitle() + "_" + compNumber, Color.orange, compDiagram, new Point(sfPort.getLocation().x - xBusOffset, sfPort.getLocation().y));
        sfBus.setLocation(sfBus.getLocation().x - builder.getNodeBounds(sfBus).width, sfBus.getLocation().y);
        BrainUtils.alignCenterY(compDiagram, sfPort, sfBus);
        DiagramUtility.createConnection(compDiagram, sfBus, sfPort, true);
       
        yPos = cellularSubDiagram.getLocation().y + cellularSubDiagram.getShapeSize().height * 3 / 4 - builder.getNodeBounds(x1Port).height / 2;
        movePortToSide(x1Port, cellularSubDiagram, Side.LEFT, builder);
        x1Port.setLocation(x1Port.getLocation().x, yPos);
        Node x1Bus = BrainUtils.createBus(x1Port.getTitle() + "_" + compNumber, Color.blue, compDiagram, new Point(x1Port.getLocation().x - xBusOffset, x1Port.getLocation().y));
        x1Bus.setLocation(x1Bus.getLocation().x - builder.getNodeBounds(x1Bus).width, x1Bus.getLocation().y);
        BrainUtils.alignCenterY(compDiagram, x1Port, x1Bus);
        DiagramUtility.createConnection(compDiagram, x1Bus, x1Port, true);

        yPos = cellularSubDiagram.getLocation().y + yBorderOffset;
        layoutPortsWithBuses(cellularSubDiagram, compDiagram, rightPorts, rightBusNames, ConnectionDirection.PORT_TO_BUS, Side.RIGHT,
        		new Point(rightPorts.get(0).getLocation().x, yPos), Color.green, xBusOffset, yBusOffset, rightPortsOffsetMultiplier);

        if (isEpileptogenic)
        {
            Node timePort = BrainUtils.findInputPortByTitle(cellularSubDiagram, "time_start");
            int xPos = cellularSubDiagram.getLocation().x + cellularSubDiagram.getShapeSize().width / 2 - builder.getNodeBounds(timePort).width / 2;
            movePortToSide(timePort, cellularSubDiagram, Side.TOP, builder);
            timePort.setLocation(xPos, timePort.getLocation().y);
            
            Node timeBus = BrainUtils.createBus(timePort.getTitle(), Color.black, compDiagram, new Point(timePort.getLocation().x, timePort.getLocation().y - yBusOffset));
            timeBus.setLocation(timeBus.getLocation().x, timeBus.getLocation().y - builder.getNodeBounds(timeBus).height);
            BrainUtils.alignCenterX(compDiagram, timePort, timeBus);
            DiagramUtility.createConnection(compDiagram, timeBus, timePort, true);
        }
    }
    
    public static void layoutRegional(SubDiagram regionalSubDiagram, int nRegion) throws Exception 
    {
        int xBorderOffset = 5;
        int yBorderOffset = 5;
         
        int subDiagramWidth = 0;
        int subDiagramHeight = 0;
        
        int xBusOffset = 30;
        int yBusOffset = 30;

        Diagram compDiagram = (Diagram)regionalSubDiagram.getOrigin();
        DiagramViewBuilder builder = compDiagram.getType().getDiagramViewBuilder();
         
        List<Node> leftPorts = new ArrayList<>();
        double leftPortsOffsetMultiplier = 1.0;
        List<String> leftBusNames = new ArrayList<>();
        for (int i = 0; i < nRegion; i++)
        {
            for (String entity : new String[]{"u_exc", "W"})
            {
            	String portTitle = entity + "_" + (i + 1);
                Node port = BrainUtils.findInputPortByTitle(regionalSubDiagram, portTitle);
                movePortToSide(port, regionalSubDiagram, Side.LEFT, builder);
                leftPorts.add(port);
                subDiagramHeight += leftPortsOffsetMultiplier * builder.getNodeBounds(port).height;
                
                String busName = port.getTitle();
                leftBusNames.add(busName);
            }
            //subDiagramHeight += leftPortsOffsetMultiplier * builder.getNodeBounds(leftPorts.get(leftPorts.size() - 1)).height;
        }
         
        List<Node> rightPorts = new ArrayList<>();
        double rightPortsOffsetMultiplier = 1.0;
        List<String> rightBusNames = new ArrayList<>();
        for (int i = 0; i < nRegion; i++)
        {
            for (String entity : new String[]{"x1"})
            {
            	String portTitle = entity + "_" + (i + 1);
                Node port = BrainUtils.findOutputPortByTitle(regionalSubDiagram, portTitle);
                movePortToSide(port, regionalSubDiagram, Side.RIGHT, builder);
                rightPorts.add(port);
                
                String busName = port.getTitle();
                rightBusNames.add(busName);
            }
        }
        
        subDiagramWidth += builder.getNodeBounds(leftPorts.get(0)).width;
        subDiagramWidth += builder.getNodeBounds(rightPorts.get(0)).width;
        
        regionalSubDiagram.setShapeSize(new Dimension(
	        		Math.max(subDiagramWidth + 2 * xBorderOffset, 500), 
	        		subDiagramHeight + 2 * yBorderOffset)
        		);
        
        int yPos = regionalSubDiagram.getLocation().y + yBorderOffset;
        layoutPortsWithBuses(regionalSubDiagram, compDiagram, leftPorts, leftBusNames, ConnectionDirection.BUS_TO_PORT, Side.LEFT,
        		new Point(leftPorts.get(0).getLocation().x, yPos), Color.black, xBusOffset, yBusOffset, leftPortsOffsetMultiplier);

        yPos = regionalSubDiagram.getLocation().y + yBorderOffset;
        layoutPortsWithBuses(regionalSubDiagram, compDiagram, rightPorts, rightBusNames, ConnectionDirection.PORT_TO_BUS, Side.RIGHT,
        		new Point(rightPorts.get(0).getLocation().x, yPos), Color.blue, xBusOffset, yBusOffset, rightPortsOffsetMultiplier);
    }
    
    public enum Side 
    {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }
    
    public enum ConnectionDirection 
    {
        PORT_TO_BUS,
        BUS_TO_PORT
    }
    
    public static void movePortToSide(Node port, SubDiagram subDiagram, Side side, DiagramViewBuilder builder)
    {
    	Point zeroPoint = subDiagram.getLocation();
    	
    	switch (side)
    	{
    		case LEFT:
    			port.setLocation(new Point(zeroPoint.x, zeroPoint.y + subDiagram.getShapeSize().height / 2));
    			break;
    		case RIGHT:
    			port.setLocation(new Point(zeroPoint.x + subDiagram.getShapeSize().width, zeroPoint.y + subDiagram.getShapeSize().height / 2));
    			break;
    		case TOP:
    			port.setLocation(new Point(zeroPoint.x + subDiagram.getShapeSize().width / 2, zeroPoint.y));
    			break;
        	case BOTTOM:
        		port.setLocation(new Point(zeroPoint.x + subDiagram.getShapeSize().width / 2, zeroPoint.y + subDiagram.getShapeSize().height));
        		break;
    	}
    	
    	CompositeSemanticController.movePortToEdge(port, subDiagram, new Dimension(0, 0), false);
    	correctPortPosition(port, subDiagram, builder);
    }

    public static void correctPortPosition(Node port, SubDiagram subDiagram, DiagramViewBuilder builder) 
    {
    	int xPort = port.getLocation().x;
    	int yPort = port.getLocation().y;
    	
    	int leftBound = subDiagram.getLocation().x;
    	int rightBound = subDiagram.getLocation().x + subDiagram.getShapeSize().width;
    	int topBound = subDiagram.getLocation().y;
    	int bottomBound = subDiagram.getLocation().y + subDiagram.getShapeSize().height;
    	
    	
    	if (xPort < leftBound)
    	{
    		port.setLocation(new Point(port.getLocation().x + builder.getNodeBounds(port).width, port.getLocation().y));
    	}
    	
    	if (xPort >= rightBound)
    	{
    		port.setLocation(new Point(port.getLocation().x - builder.getNodeBounds(port).width, port.getLocation().y));
    	}
    	
    	if (yPort < topBound)
    	{
    		port.setLocation(new Point(port.getLocation().x, port.getLocation().y + builder.getNodeBounds(port).height));
    	}
    	
    	if (yPort >= bottomBound)
    	{
    		port.setLocation(new Point(port.getLocation().x, port.getLocation().y - builder.getNodeBounds(port).height));
    	}
    }
    
    public static void layoutPortsWithBuses(SubDiagram subDiagram,
            Diagram compDiagram,
            List<Node> ports,
            List<String> busNames,
            ConnectionDirection direction,
            Side side,
            Point startPoint,
            Color color,
            int xBusOffset,
            int yBusOffset,
            double portSpacingMultiplier) throws Exception
    {
        DiagramViewBuilder builder = ((Diagram) subDiagram.getOrigin()).getType().getDiagramViewBuilder();

        List<Node> evenBuses = new ArrayList<>();
        List<Node> oddBuses  = new ArrayList<>();

        Node widestEvenBus = null;
        Node highestEvenBus = null;
        Node widestOddBus  = null;
        Node highestOddBus  = null;
        int maxEvenWidth = 0;
        int maxEvenHeight = 0;
        int maxOddWidth  = 0;
        int maxOddHeight  = 0;

        int xPos = startPoint.x;
        int yPos = startPoint.y;


        for (int index = 0; index < ports.size(); index++)
        {
            Node port = ports.get(index);

            switch (side) 
            {
                case LEFT:
                	movePortToSide(port, subDiagram, Side.LEFT, builder);
                	break;
                case RIGHT:
                	movePortToSide(port, subDiagram, Side.RIGHT, builder);
                	break;
                case TOP:
                	movePortToSide(port, subDiagram, Side.TOP, builder);
                	break;
                case BOTTOM:
                	movePortToSide(port, subDiagram, Side.BOTTOM, builder);
                	break;
            }

            switch (side) 
            {
                case LEFT:
                case RIGHT:
                {
                    port.setLocation(port.getLocation().x, yPos);
                    yPos += portSpacingMultiplier * builder.getNodeBounds(port).height;
                    break;
                }
                case TOP:
                case BOTTOM:
                {
                    port.setLocation(xPos, port.getLocation().y);
                    xPos += portSpacingMultiplier * builder.getNodeBounds(port).width;
                    break;
                }
            }

            String busName = busNames.get(index);
            Point busPoint = null;
            switch (side) 
            {
                case LEFT:
                	busPoint = new Point((int)(builder.getNodeBounds(port).getMinX() - xBusOffset), port.getLocation().y);
                	break;
                case RIGHT:
                	busPoint = new Point((int)(builder.getNodeBounds(port).getMaxX() + xBusOffset), port.getLocation().y);
                	break;
                case TOP:
                	busPoint = new Point(port.getLocation().x, (int)(builder.getNodeBounds(port).getMinY() - yBusOffset));
                	break;
                case BOTTOM:
                	busPoint = new Point(port.getLocation().x, (int)(builder.getNodeBounds(port).getMaxY() + yBusOffset));
                	break;
            }
            Node bus = BrainUtils.createBus(busName, color, compDiagram, busPoint);
            int busWidth = builder.getNodeBounds(bus).width;
            int busHeight = builder.getNodeBounds(bus).height;

            switch (side) 
            {
                case LEFT:
                {
                    bus.setLocation(bus.getLocation().x - busWidth, bus.getLocation().y);
                    BrainUtils.alignCenterY(compDiagram, port, bus);
                    break;
                }
                case RIGHT:
                {
                	BrainUtils.alignCenterY(compDiagram, port, bus);
                    break;
                }
                case TOP:
                {
                    bus.setLocation(bus.getLocation().x, bus.getLocation().y - busHeight);
                    BrainUtils.alignCenterX(compDiagram, port, bus);
                    break;
                }
                case BOTTOM:
                {
                	BrainUtils.alignCenterX(compDiagram, port, bus);
                    break;
                }
            }

            if (index % 2 == 0) 
            {
                evenBuses.add(bus);
                if (busWidth > maxEvenWidth) 
                {
                    maxEvenWidth = busWidth;
                    widestEvenBus = bus;
                }
                if (busHeight > maxEvenHeight) 
                {
                    maxEvenHeight = busHeight;
                    highestEvenBus = bus;
                }
            } 
            else 
            {
                oddBuses.add(bus);
                if (busWidth > maxOddWidth) 
                {
                    maxOddWidth = busWidth;
                    widestOddBus = bus;
                }
                if (busHeight > maxOddHeight) 
                {
                    maxOddHeight = busHeight;
                    highestOddBus = bus;
                }
            }

            switch (direction) 
            {
                case BUS_TO_PORT:
                	DiagramUtility.createConnection(compDiagram, bus, port, true);
                	break;
                case PORT_TO_BUS:
                	DiagramUtility.createConnection(compDiagram, port, bus, true);
                	break;
            }
        }

        if ((side == Side.LEFT || side == Side.RIGHT) && maxEvenWidth > 0) 
        {
            for (Node bus : oddBuses) 
            {
                if (side == Side.LEFT) 
                {
                    bus.setLocation(bus.getLocation().x - maxEvenWidth, bus.getLocation().y);
                } 
                else 
                {
                    bus.setLocation(bus.getLocation().x + maxEvenWidth, bus.getLocation().y);
                }
            }
        }
        else if ((side == Side.TOP || side == Side.BOTTOM) && maxEvenHeight > 0) 
        {
            for (Node bus : oddBuses) 
            {
                if (side == Side.TOP) 
                {
                    bus.setLocation(bus.getLocation().x, bus.getLocation().y - maxEvenHeight);
                } 
                else 
                {
                    bus.setLocation(bus.getLocation().x, bus.getLocation().y + maxEvenHeight);
                }
            }
        }

        if ((side == Side.LEFT || side == Side.RIGHT)) 
        {
        	if (!evenBuses.isEmpty())
        	{
        		BrainUtils.alignCenterX(compDiagram, widestEvenBus, evenBuses);
        	}
        	if (!oddBuses.isEmpty()) 
            {
        		BrainUtils.alignCenterX(compDiagram, widestOddBus, oddBuses);
            }
        }
        
        if ((side == Side.TOP || side == Side.BOTTOM)) 
        {
        	if (!evenBuses.isEmpty())
        	{
        		BrainUtils.alignCenterY(compDiagram, highestEvenBus, evenBuses);
        	}
        	if (!oddBuses.isEmpty()) 
            {
        		BrainUtils.alignCenterY(compDiagram, highestOddBus, oddBuses);
            }
        }
    }
}