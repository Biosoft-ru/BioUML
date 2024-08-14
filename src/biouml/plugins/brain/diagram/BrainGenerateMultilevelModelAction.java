package biouml.plugins.brain.diagram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import biouml.model.Diagram;
import biouml.model.DiagramType;
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
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.DiagramInfo;
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
                    	Diagram cellularDiagram = BrainCellularModelDeployer.deployBrainCellularModel(diagram, "Cellular_diagram");
                    	cellularDiagram.getRole(EModel.class).declareVariable("epileptogenicity_flag", 0.0);
                    	BrainUtils.setInitialValue(cellularDiagram, "time_start", Double.MAX_VALUE);
                    	
                    	BrainReceptorModel receptorModel = BrainUtils.getReceptorModelNodes(diagram).get(0).getRole(BrainReceptorModel.class);
                    	receptorModel.getReceptorModelProperties().setPortsFlag(true);
                    	Diagram receptorDiagram = BrainReceptorModelDeployer.deployBrainReceptorModel(diagram, "Receptor_diagram");
                    	receptorDiagram.getRole(EModel.class).declareVariable("epileptogenicity_flag", 0.0);
                    	
                    	//initAndSaveRosslerEpileptor2CompositeModel(regionalDiagram, cellularDiagram, regionCount, diagram.getOrigin());
                    	initAndSaveEpileptorEpileptor2AmpaAgentModel(regionalDiagram, cellularDiagram, receptorDiagram, 
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
    public static Diagram initAndSaveEpileptorEpileptor2AmpaAgentModel(Diagram regionalDiagram, Diagram cellularDiagram, Diagram receptorDiagram, 
    		int regionCount, List<Integer> epileptogenicZoneNodesNumbers, DataCollection<?> origin) throws Exception
    {    	
    	DataElementPath regionalSubModelsPath = DataElementPath.create(origin.getCompletePath().toString() + "/regional_submodels");
    	DataCollectionUtils.createFoldersForPath(regionalSubModelsPath);
    	DataCollection<DataElement> regionalSubModelsDataCollection = DataCollectionUtils.createSubCollection(regionalSubModelsPath);
    	
    	DataElementPath cellularSubModelsPath = DataElementPath.create(origin.getCompletePath().toString() + "/cellular_submodels");
    	DataCollectionUtils.createFoldersForPath(cellularSubModelsPath);
    	DataCollection<DataElement> cellularSubModelsDataCollection = DataCollectionUtils.createSubCollection(cellularSubModelsPath);
    	
    	DataElementPath receptorsSubModelsPath = DataElementPath.create(origin.getCompletePath().toString() + "/receptor_submodels");
    	DataCollectionUtils.createFoldersForPath(receptorsSubModelsPath);
    	DataCollection<DataElement> receptorSubModelsDataCollection = DataCollectionUtils.createSubCollection(receptorsSubModelsPath);
    	
    	Dimension regionalDiagramShape = new Dimension(300, 40 * regionCount);
    	Dimension cellularDiagramShape = new Dimension(250, 70);
    	Dimension receptorDiagramShape = new Dimension(200, 40);
    	
    	int xDiagramOffset = 200;
    	int yDiagramOffset = 50;

    	regionalDiagram.setOrigin(regionalSubModelsDataCollection);
    	regionalSubModelsDataCollection.put(regionalDiagram);
    	
    	String agentDiagramName = "Brain_multilevel_agent_model";
    	DiagramType agentDiagramType = new AgentModelDiagramType();
    	Diagram agentDiagram = agentDiagramType.createDiagram(origin, agentDiagramName, new DiagramInfo(agentDiagramName));
    	
    	SubDiagram regionalSubDiagram = new SubDiagram(agentDiagram, regionalDiagram, regionalDiagram.getName());
    	regionalSubDiagram.setShapeSize(regionalDiagramShape);
    	agentDiagram.put(regionalSubDiagram);
    	
        regionalSubDiagram.setLocation(new Point(receptorDiagramShape.width + xDiagramOffset + cellularDiagramShape.width + xDiagramOffset, 0));
        BrainUtils.alignPorts(regionalSubDiagram);
        
        BrainUtils.createNote("Info: N = " + String.valueOf(regionCount), new Dimension(80, 25), agentDiagram, 
        		new Point(regionalSubDiagram.getLocation().x + regionalSubDiagram.getShapeSize().width + xDiagramOffset, regionalSubDiagram.getLocation().y));
    	
    	Point pointStart = new Point(0, 0);
    	
    	List<SubDiagram> receptorSubDiagrams = new ArrayList<SubDiagram>();
    	List<SubDiagram> cellularSubDiagrams = new ArrayList<SubDiagram>();
    	
    	Point point = new Point(pointStart);
    	for (int i = 0; i < regionCount; i++)
    	{
    		if (epileptogenicZoneNodesNumbers.contains(i+ 1))
    		{
    			BrainUtils.setInitialValue(receptorDiagram, "epileptogenicity_flag", 1.0);
    			BrainUtils.setInitialValue(cellularDiagram, "epileptogenicity_flag", 1.0);
    			BrainUtils.setInitialValue(cellularDiagram, "A_exc", 14.0);
    			BrainUtils.setInitialValue(cellularDiagram, "tau_K", 100.0);
    		}
    		else
    		{
    			BrainUtils.setInitialValue(receptorDiagram, "epileptogenicity_flag", 0.0);
    			BrainUtils.setInitialValue(cellularDiagram, "epileptogenicity_flag", 0.0);
    			BrainUtils.setInitialValue(cellularDiagram, "A_exc", 0.0);
    			BrainUtils.setInitialValue(cellularDiagram, "tau_K", 2.5);
    		}
    		Diagram nextReceptorDiagram = receptorDiagram.clone(receptorSubModelsDataCollection, receptorDiagram.getName() + "_" + String.valueOf(i + 1));
    		receptorSubModelsDataCollection.put(nextReceptorDiagram);
    		SubDiagram nextReceptorSubDiagram = new SubDiagram(agentDiagram, nextReceptorDiagram, nextReceptorDiagram.getName());
    		nextReceptorSubDiagram.setShapeSize(receptorDiagramShape);
    		agentDiagram.put(nextReceptorSubDiagram);
    		nextReceptorSubDiagram.setLocation(point);
        	BrainUtils.alignPorts(nextReceptorSubDiagram);
        	receptorSubDiagrams.add(nextReceptorSubDiagram);
    		
    		Diagram nextCellularDiagram = cellularDiagram.clone(cellularSubModelsDataCollection, cellularDiagram.getName() + "_" + String.valueOf(i + 1));
    		if (epileptogenicZoneNodesNumbers.contains(i+ 1))
    		{
                BrainUtils.createPort("time_start", "time_start", Type.TYPE_INPUT_CONNECTION_PORT,
                		nextCellularDiagram, new Point(650, 0));
    		}
    		cellularSubModelsDataCollection.put(nextCellularDiagram);
    		SubDiagram nextCellularSubDiagram = new SubDiagram(agentDiagram, nextCellularDiagram, nextCellularDiagram.getName());
    		nextCellularSubDiagram.setShapeSize(cellularDiagramShape);
    		agentDiagram.put(nextCellularSubDiagram);
    		nextCellularSubDiagram.setLocation(new Point(point.x + nextReceptorSubDiagram.getShapeSize().width + xDiagramOffset, 
    				nextReceptorSubDiagram.getLocation().y));
        	BrainUtils.alignPorts(nextCellularSubDiagram);
        	cellularSubDiagrams.add(nextCellularSubDiagram);
        	
        	if (cellularDiagram.getRole(EModel.class).getVariable("epileptogenicity_flag").getInitialValue() == 1.0)
        	{
        		String timeStart = "time_start_" + String.valueOf(i + 1);
        		Node portFrom = BrainUtils.createPrivatePort(timeStart, timeStart, Type.TYPE_OUTPUT_CONNECTION_PORT, agentDiagram, 
        				new Point(nextCellularSubDiagram.getLocation().x - 150, nextCellularSubDiagram.getLocation().y + 50));
        		Node portTo = BrainUtils.findInputPort(nextCellularSubDiagram, "time_start");
        		DiagramUtility.createConnection(agentDiagram, portFrom, portTo, true);
        		BrainUtils.setInitialValue(agentDiagram, timeStart, 50.0);
        	}

        	point.translate(0, nextCellularSubDiagram.getShapeSize().height + yDiagramOffset);
    		
    		BrainUtils.createConnection(agentDiagram, nextReceptorSubDiagram, "SF", nextCellularSubDiagram, "SF");
    		
    		BrainUtils.createConnection(agentDiagram, nextCellularSubDiagram, "SR", nextReceptorSubDiagram, "SR");
    		BrainUtils.createConnection(agentDiagram, nextCellularSubDiagram, "FR_norm", regionalSubDiagram, "u_exc_" + String.valueOf(i + 1));
    		//BrainUtils.createConnection(agentDiagram, nextCellularSubDiagram, "low_excitability_flag", regionalSubDiagram, "low_excitability_flag_" + String.valueOf(i + 1));
    		//BrainUtils.createConnection(agentDiagram, nextCellularSubDiagram, "SF", regionalSubDiagram, "SF_" + String.valueOf(i + 1));
    		//BrainUtils.createConnection(agentDiagram, nextCellularSubDiagram, "W", regionalSubDiagram, "W_" + String.valueOf(i + 1));
    		BrainUtils.createConnection(agentDiagram, nextCellularSubDiagram, "SF_norm", regionalSubDiagram, "W_" + String.valueOf(i + 1));
    		
    		BrainUtils.createConnection(agentDiagram, regionalSubDiagram, "x1_" + String.valueOf(i + 1), nextCellularSubDiagram, "x1");
    	}
    	
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
    		else if (ase.getDiagram().getName().contains(cellularDiagram.getName()))
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
    		else if (ase.getDiagram().getName().contains(receptorDiagram.getName()))
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
        		regionCount, epileptogenicZoneNodesNumbers);
        
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
}