package biouml.plugins.brain.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
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
import biouml.plugins.brain.model.BrainCellularModelDeployer;
import biouml.plugins.brain.model.BrainRegionalModelDeployer;
import biouml.plugins.brain.sde.EulerStochastic;
import biouml.plugins.brain.sde.JavaSdeSimulationEngine;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.DiagramInfo;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.access.subaction.DynamicAction;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.util.DPSUtils;

@SuppressWarnings ("serial")
public class BrainGenerateCompositeDiagramAction extends BackgroundDynamicAction
{	
    public BrainGenerateCompositeDiagramAction()
    {
        setNumSelected( DynamicAction.SELECTED_ZERO_OR_ANY );
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
        return new AbstractJobControl( log )
        {
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    Diagram diagram = (Diagram)model;
                 
                    int regionalModelCount = BrainUtils.getRegionalModelNodes(diagram).size();
                    int cellularModelCount = BrainUtils.getCellularModelNodes(diagram).size();
                    int connectivityMatrixCount = BrainUtils.getConnectivityMatrixNodes(diagram).size();
                    
                    if (cellularModelCount == 1 && regionalModelCount == 1 && connectivityMatrixCount == 1) 
                    {
                    	log.log(Level.INFO, "BrainGenerateCompositeDiagram: starting composite diagram generation.");
                    	
                    	BrainUtils.getCellularModelNodes(diagram).get(0).getRole(BrainCellularModel.class).getCellularModelProperties().setPortsFlag(true);
                    	Diagram cellularDiagram = BrainCellularModelDeployer.deployBrainCellularModel(diagram, "Cellular model"); 
                    	CollectionFactoryUtils.save(cellularDiagram);
                    	
                    	BrainUtils.getRegionalModelNodes(diagram).get(0).getRole(BrainRegionalModel.class).getRegionalModelProperties().setPortsFlag(true);
                    	Diagram regionalDiagram = BrainRegionalModelDeployer.deployBrainRegionalModel(diagram, "Regional model"); 
                    	CollectionFactoryUtils.save(regionalDiagram);
                    	
                    	Diagram compDiagram = initCompositeDiagram(cellularDiagram, regionalDiagram, diagram.getOrigin());
                    	CollectionFactoryUtils.save(compDiagram);
                    	
                    	log.log(Level.INFO, "BrainGenerateCompositeDiagram: composite diagram generation is completed.");
                    	return;
                    }
                    else 
                    {
                    	log.log(Level.WARNING, "BrainGenerateCompositeDiagram: please, provide 1 cellular model, 1 regional model and 1 connectivity matrix for composite diagram generation.");
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
     * method creates simple composite model which consists of 2 subdiagrams: 1 cellular and 1 regional.
     * it creates connections between specific ports.
     * input suddiagrams should already contain ports.
     */
    public Diagram initCompositeDiagram(Diagram cellularDiagram, Diagram regionalDiagram, DataCollection<?> origin) throws Exception
    {
    	String compDiagramName = "Composite brain model";
    	DiagramType compDiagramType = new CompositeDiagramType();
    	Diagram compDiagram = compDiagramType.createDiagram(origin, compDiagramName, new DiagramInfo(compDiagramName));
    	
    	SubDiagram cellularSubDiagram = new SubDiagram(compDiagram, cellularDiagram, cellularDiagram.getName());
    	SubDiagram regionalSubDiagram = new SubDiagram(compDiagram, regionalDiagram, regionalDiagram.getName());
    	cellularSubDiagram.setShapeSize(new Dimension(200, 200));
    	regionalSubDiagram.setShapeSize(new Dimension(200, 400));
    	compDiagram.put(cellularSubDiagram);
    	compDiagram.put(regionalSubDiagram);
    	
    	int xDiagramOffset = 200;
    	
    	Point point = new Point(0, 0);
    	cellularSubDiagram.setLocation(point);
    	BrainUtils.alignPorts(cellularSubDiagram);
    	
    	point = new Point(cellularSubDiagram.getLocation().x + cellularSubDiagram.getShapeSize().width + xDiagramOffset, 
    			cellularSubDiagram.getLocation().y);
    	regionalSubDiagram.setLocation(point);
    	BrainUtils.alignPorts(regionalSubDiagram);
    
    	Node portFrom = BrainUtils.findPort(cellularSubDiagram, "nu_norm");
    	Node portTo = BrainUtils.findPort(regionalSubDiagram, "u_exc");
     	DiagramUtility.createConnection(compDiagram, portFrom, portTo, true);

        //Simulator settings
        JavaSdeSimulationEngine se = new JavaSdeSimulationEngine();
        se.setDiagram(compDiagram);
        se.setCompletionTime(2000.0);
        se.setTimeIncrement(0.1);
        se.setSolver(new EulerStochastic());
        compDiagram.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, se));
     	
        setPlotsCompositeDiagram(compDiagram, cellularSubDiagram, regionalSubDiagram);
        
    	return compDiagram;
    }

    /*
     * method sets plots on simple brain composite diagram
     * which contains 1 regional and 1 cellular model
     * using variables from cellularSubDiagram and regionalSubDiagram.
     */
    private void setPlotsCompositeDiagram(Diagram compDiagram, SubDiagram cellularSubDiagram, SubDiagram regionalSubDiagram)
    {
    	EModel emodel = compDiagram.getRole(EModel.class);
    	PlotsInfo plotsInfo = new PlotsInfo(emodel);
    	
        PlotInfo varPlotsExcitationSignal = new PlotInfo();
        varPlotsExcitationSignal.setTitle("Regional: Excitation signal");
      
        PlotInfo varPlotsSynapticStrength = new PlotInfo();
        varPlotsSynapticStrength.setTitle("Regional: Synaptic strength");
        
        PlotInfo varPlotsEegRecording = new PlotInfo();
        varPlotsEegRecording.setTitle("Regional: EEG Recording");
        
        PlotInfo varPlotsFiringRate = new PlotInfo();
        varPlotsFiringRate.setTitle("Cellular: Firing rate");
        varPlotsFiringRate.setXTitle("time (s)");
        varPlotsFiringRate.setYTitle("nu (Hz)");
        
        PlotInfo varPlotsDepolarization = new PlotInfo();
        varPlotsDepolarization.setTitle("Cellular: Depolarization");
        varPlotsDepolarization.setXTitle("time (s)");
        varPlotsDepolarization.setYTitle("V (mV)");
        
        PlotInfo varPlotsSynapticResource = new PlotInfo();
        varPlotsSynapticResource.setTitle("Cellular: Synaptic resource");
        varPlotsSynapticResource.setXTitle("time (s)");
        varPlotsSynapticResource.setYTitle("x_D");
        
        PlotInfo varPlotsIonicConcentrations = new PlotInfo();
        varPlotsIonicConcentrations.setTitle("Cellular: Ionic concentrations");
        varPlotsIonicConcentrations.setXTitle("time (s)");
        varPlotsIonicConcentrations.setYTitle("Na_i, K_o (mM)");
        
        PlotInfo varPlotsPumpActivity = new PlotInfo();
        varPlotsPumpActivity.setTitle("Cellular: Na-K pump activity");
        varPlotsPumpActivity.setXTitle("time (s)");
        varPlotsPumpActivity.setYTitle("I_pump (mWs)");
        
        PlotInfo varPlotsOxygenDynamics = new PlotInfo();
        varPlotsOxygenDynamics.setTitle("Cellular: Oxygen dynamics");
        varPlotsOxygenDynamics.setXTitle("time (s)");
        varPlotsOxygenDynamics.setYTitle("O2_o (mg/L)");
        
        plotsInfo.setPlots( new PlotInfo[] {varPlotsExcitationSignal, varPlotsSynapticStrength, varPlotsEegRecording,
        		varPlotsFiringRate, varPlotsDepolarization, varPlotsSynapticResource, varPlotsIonicConcentrations, varPlotsPumpActivity, varPlotsOxygenDynamics});

        List<Curve> curvesExcitationSignal = new ArrayList<Curve>();
        curvesExcitationSignal.add(new Curve(regionalSubDiagram.getName(), "u_exc", "u_exc", emodel));
        
        List<Curve> curvesSynapticStrength = new ArrayList<Curve>();
        curvesSynapticStrength.add(new Curve(regionalSubDiagram.getName(), "eta", "eta", emodel));
        
        List<Curve> curvesEegRecording = new ArrayList<Curve>();
        curvesEegRecording.add(new Curve(regionalSubDiagram.getName(), "V_EEG", "V_EEG", emodel));
        
        List<Curve> curvesFiringRate = new ArrayList<Curve>();
        curvesFiringRate.add(new Curve(cellularSubDiagram.getName(), "nu", "nu", emodel));
        
        List<Curve> curvesDepolarization = new ArrayList<Curve>();
        curvesDepolarization.add(new Curve(cellularSubDiagram.getName(), "V", "V", emodel));
        
        List<Curve> curvesSynapticResource = new ArrayList<Curve>();
        curvesSynapticResource.add(new Curve(cellularSubDiagram.getName(), "x_D", "x_D", emodel));
        
        List<Curve> curvesIonicConcentrations = new ArrayList<Curve>();
        curvesIonicConcentrations.add(new Curve(cellularSubDiagram.getName(), "K_o", "K_o", emodel));
        curvesIonicConcentrations.add(new Curve(cellularSubDiagram.getName(), "Na_i", "Na_i", emodel));
  
        List<Curve> curvesPumpActivity = new ArrayList<Curve>();
        curvesPumpActivity.add(new Curve(cellularSubDiagram.getName(), "I_pump", "I_pump", emodel));
        
        List<Curve> curvesOxygenDynamics = new ArrayList<Curve>();
        curvesOxygenDynamics.add(new Curve(cellularSubDiagram.getName(), "O2_o", "O2_o", emodel));
        
        varPlotsExcitationSignal.setYVariables(curvesExcitationSignal.stream().toArray( Curve[]::new));
        varPlotsSynapticStrength.setYVariables(curvesSynapticStrength.stream().toArray( Curve[]::new));
        varPlotsEegRecording.setYVariables(curvesEegRecording.stream().toArray( Curve[]::new));
        varPlotsFiringRate.setYVariables( curvesFiringRate.stream().toArray( Curve[]::new ) );
        varPlotsDepolarization.setYVariables( curvesDepolarization.stream().toArray( Curve[]::new ) );
        varPlotsSynapticResource.setYVariables( curvesSynapticResource.stream().toArray( Curve[]::new ) );
        varPlotsIonicConcentrations.setYVariables( curvesIonicConcentrations.stream().toArray( Curve[]::new ) );
        varPlotsPumpActivity.setYVariables( curvesPumpActivity.stream().toArray( Curve[]::new ) );
        varPlotsOxygenDynamics.setYVariables( curvesOxygenDynamics.stream().toArray( Curve[]::new ) );
        DiagramUtility.setPlotsInfo(compDiagram, plotsInfo);
    }
}
