package biouml.plugins.brain.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import biouml.model.Diagram;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.util.DiagramXmlConstants;
import biouml.plugins.brain.diagram.BrainDiagramSemanticController;
import biouml.plugins.brain.diagram.BrainDiagramType;
import biouml.plugins.brain.diagram.BrainRegionalModel;
import biouml.plugins.brain.diagram.BrainType;
import biouml.plugins.brain.diagram.BrainUtils;
import biouml.plugins.brain.model.regional.RosslerRegionalModelProperties;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Type;
import ru.biosoft.util.DPSUtils;

/*
 * Equations deployer for regional brain models which will be used in multilevel brain model.
 */
public class BrainRegionalForMultilevelModelDeployer 
{	
	public static @Nonnull Diagram deployBrainRegionalForMultilevelModel(Diagram src, String name) throws Exception
	{
	    return deployBrainRegionalForMultilevelModel(src, name, true);
	}

    public static @Nonnull Diagram deployBrainRegionalForMultilevelModel(Diagram src, String name, boolean needLayout) throws Exception
    {
    	BrainRegionalModel regionalModel = BrainUtils.getRegionalModelNodes(src).get(0).getRole(BrainRegionalModel.class);
    	String regionalModelType = regionalModel.getRegionalModelType();
    	switch (regionalModelType)
    	{
    	    case BrainType.TYPE_REGIONAL_ROSSLER:
    	    	return fillDiagramWithEquationsRosslerForMultilevelModel(src, name);
    	    case BrainType.TYPE_REGIONAL_EPILEPTOR:
    	    	return null;
    	    default:
    	    	return (new BrainDiagramType()).createDiagram(null, null, null);
    	}
    }
	
	public static Diagram fillDiagramWithEquationsRosslerForMultilevelModel(Diagram diagram, String name) throws Exception
	{
		String dName;
    	if (name.isEmpty())
    	{
    		String prefix = diagram.getName();
            String suffix = "regional_Rossler_equations_for_multilevel_model";
            dName = suffix.isEmpty() ? prefix : prefix + "_" + suffix;
    	}
    	else
    	{
    		dName = name;
    	}
        Diagram diagramEq = (diagram.getType()).createDiagram(diagram.getOrigin(), dName, new DiagramInfo(dName));
        
        if (diagram.getComment() != null)
        {
            diagramEq.setComment(diagram.getComment());
        }

    	double[][] connectivityMatrix = BrainUtils.getConnectivityMatrix(diagram);
    	int sizeRows = connectivityMatrix.length;
    	int sizeColumns = connectivityMatrix[0].length;
    	
    	BrainRegionalModel regionalModel = BrainUtils.getRegionalModelNodes(diagram).get(0).getRole(BrainRegionalModel.class);
    	RosslerRegionalModelProperties regionalModelProperties = (RosslerRegionalModelProperties)regionalModel.getRegionalModelProperties();
    	BrainDiagramSemanticController semanticController = (BrainDiagramSemanticController)diagramEq.getType().getSemanticController();
        
        int yGroupOffset = 80;
        int yLocalOffset = 60;
        Point point = new Point(0, 0);
        
        double[][] couplingMatrix = BrainUtils.getCouplingMatrix(connectivityMatrix);
        
        /*
         * Region dynamic 
         * (x_i, y_i, z_i)
         */
        for (int i = 0; i < sizeColumns; i++)
        {
        	String xi = "x_" + String.valueOf(i + 1);
        	String yi = "y_" + String.valueOf(i + 1);
        	String zi = "z_" + String.valueOf(i + 1);
        	
            String xFormula = "-omega*" + yi + "-" + zi;
            for (int j = 0; j < sizeRows; j ++) 
            {
            	if (j == i || !(couplingMatrix[j][i] > 0.0) ) 
            	{
            		continue;
            	}
            	else 
            	{
            		String xj = "x_" + String.valueOf(j + 1);
                	xFormula += "+epsilon_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1) + "*(" + xj + "-" + xi + ")";
            	}
            }
            xFormula += "+A_d*sin(2.0*pi*(time-tau_" + String.valueOf(i + 1) + ")/T_d)";
            
            String yFormula = "omega*" + xi + "+alpha*" + yi;
            String zFormula = "b+" + zi + "*(" + xi + "-gamma)";
            
            BrainUtils.createEquation("equation_" + xi, xi, xFormula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset);
            
            BrainUtils.createEquation( "equation_" + yi, yi, yFormula,
                    Equation.TYPE_RATE, diagramEq, point );
            point.translate(0, yLocalOffset);
            
            BrainUtils.createEquation("equation_" + zi, zi, zFormula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yGroupOffset);
        }
        
        for (int i = 0; i < sizeColumns; i++)
        {
        	/*
        	 * Synaptic strength (eta, eta_eq) different for each region
        	 * because of different excitation signal (u_exc)
        	 * which is coming through corresponding cellular model
        	 */
        	String etaI = "eta_" + String.valueOf(i + 1);
        	String etaEqI = "eta_eq_" + String.valueOf(i + 1);
        	String uExcI = "u_exc_" + String.valueOf(i + 1);
        	/*
        	 * k_insensitive is used in formal mechanism of stopping seizures
        	 */
        	String kInsensitive = "k_insensitive_" + String.valueOf(i + 1);
        	
            String etaFormula = "(1.0 - " + etaI + ")*(a_1*(" + etaI + " - " + etaEqI + ")*(" + etaI + " - " + etaEqI + ") + b_1*" + kInsensitive + "*" + uExcI + ")";
            BrainUtils.createEquation("equation_" + etaI, etaI, etaFormula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset);
            
            
            String etaEqFormula = "-a_2*" + etaEqI + " + b_2*(" + etaI + " - " + etaEqI + ")";
            BrainUtils.createEquation("equation_" + etaEqI, etaEqI, etaEqFormula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yGroupOffset);
        }
        
        /*
         * EEG recordings (V_EEG)
         */
        String eegFormula = "";
        for (int i = 0; i < sizeColumns; i++) 
        {
        	eegFormula += "+lambda_" + String.valueOf(i + 1) + "*x_" + String.valueOf(i + 1);
        }
        BrainUtils.createEquation("equation_EEG", "V_EEG", eegFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yGroupOffset);
        
        boolean distinctRegions = true; // show synaptic strength and excitation signal for distinct regions on one plot
        if (distinctRegions) 
        {
            for (int i = 0; i < sizeColumns; i++) 
            {
            	String etaI = "eta_" + String.valueOf(i + 1);
          	    String etaMonitorFormula = etaI + "-1.5*" + String.valueOf(i); // offset for graph with all channels 
                BrainUtils.createEquation("equation_eta_monitor", "SynapticMonitor_" + String.valueOf(i + 1), etaMonitorFormula,
                		Equation.TYPE_SCALAR, diagramEq, point);
                
                String uExcI = "u_exc_" + String.valueOf(i + 1);
                String uExcMonitorFormula = uExcI + "-35*" + String.valueOf(i); // offset for graph with all channels 
                BrainUtils.createEquation("equation_u_exc_monitor", "ExcitationMonitor_" + String.valueOf(i + 1), uExcMonitorFormula,
                		Equation.TYPE_SCALAR, diagramEq, new Point(point.x + 220, point.y));
                
                point.translate(0, yLocalOffset - 20);
            }
        }
        point.translate(0, yGroupOffset);
        
        /*
         * epsilon_ji matrix
         */
        for (int i = 0; i < sizeColumns; i++)
        {        	
            for (int j = 0; j < sizeRows; j++)
            {
            	if (j == i || !(couplingMatrix[j][i] > 0.0) )
            	{
            		continue;
            	}
            	else 
            	{
            		/*
            		 * coupling term:
            		 * epsilon_ji = epsilonmin_ji + epsilon0_ji*eta
            		 */
            		String epsilonJI = "epsilon_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1);
            		String epsilonMinJI = "epsilonmin_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1);
            		String epsilon0JI = "+epsilon0_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1);
            		String etaJ = "eta_" + String.valueOf(j + 1);
            		
            		String epsilonFormula = epsilonMinJI + "+" + epsilon0JI + "*" + etaJ;
            		BrainUtils.createEquation("equation_" + epsilonJI, epsilonJI, epsilonFormula,
                            Equation.TYPE_SCALAR, diagramEq, point);
                	point.translate(0, yLocalOffset - 20);
            	}
            }
        }
        point.translate(0, yGroupOffset);
        
        for (int i = 0; i < sizeColumns; i++)
        {
        	String uExcI = "u_exc_" + String.valueOf(i + 1);
  
        	/*
        	 * Formal mechanism to stop seizures in this model:
        	 * 
        	 * Once the transition to short-term plasticity is fired (eta > eta_th), 
        	 * a counter with duration of 2500 time units is engaged. 
        	 * When the counter reached this threshold, 
        	 * the synaptic strength returns exponentially to 0 
        	 * and remains insensitive to excitations for another 2500 time units.
        	 */
        	String etaI = "eta_" + String.valueOf(i + 1);
        	String etaThI = "eta_th_" + String.valueOf(i + 1);
        	String etaEqI = "eta_eq_" + String.valueOf(i + 1);
        	String timeCounterI = "time_counter_" + String.valueOf(i + 1);
        	String timeInsensitiveI = "time_insensitive_" + String.valueOf(i + 1);
        	String kInsensitiveI = "k_insensitive_" + String.valueOf(i + 1);
            
            BrainUtils.createEquation("equation_" + timeCounterI, timeCounterI, "-1.0",
        			Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset + 20);
            
            BrainUtils.createEquation("equation_" + timeInsensitiveI, timeInsensitiveI, "-1.0",
        			Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset + 20);
            
            BrainUtils.createEvent("reached_" + etaThI, etaI + ">" + etaThI, new Assignment[] {new Assignment(timeCounterI, "counter_duration")},
        			diagramEq, point);
            point.translate(0, yLocalOffset + 40);
            
            BrainUtils.createEvent("ended_" + timeCounterI, timeCounterI + "<=0.0", 
            		new Assignment[] 
            		{ 
            			new Assignment(etaI, "0.0"), 
            			new Assignment(etaEqI, "0.0"),  
            			new Assignment(timeInsensitiveI, "insensitive_duration"), 
            			new Assignment(kInsensitiveI, "0.0") 
            		},
        			diagramEq, point);
            point.translate(0, yLocalOffset + 130);
            
            BrainUtils.createEvent("ended_" + timeInsensitiveI, timeInsensitiveI + "<=0.0", 
            		new Assignment[] 
            		{ 
            				new Assignment(kInsensitiveI, "1.0"), 
            				new Assignment(etaEqI, "0.0")
            		},
        			diagramEq, point);
            point.translate(0, yLocalOffset + 80);
            
            BrainUtils.createPort(uExcI, uExcI, Type.TYPE_INPUT_CONNECTION_PORT,
            		diagramEq, point);
            point.translate(0, yLocalOffset + 10);
        }
        
        setInitialValuesRosslerForMultilevelModel(diagramEq, connectivityMatrix, regionalModelProperties);
        
        //Simulator settings
        JavaSimulationEngine se = new JavaSimulationEngine();
        se.setDiagram(diagramEq);
        se.setCompletionTime(2000.0);
        se.setTimeIncrement(0.1);
        se.setSolver(new JVodeSolver());
        diagramEq.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, se));
        
        setPlotsRosslerForMultilevelModel(diagramEq);
        
      	return diagramEq;
	}
	
	private static void setInitialValuesRosslerForMultilevelModel(Diagram diagram, double[][] connectivityMatrix, RosslerRegionalModelProperties regionalModelProperties)
	{
		int sizeRows = connectivityMatrix.length;
    	int sizeColumns = connectivityMatrix[0].length;
    	
        double omega = regionalModelProperties.getOmega();
        double alpha = regionalModelProperties.getAlpha();
        double b = regionalModelProperties.getB();
        double gamma = regionalModelProperties.getGamma();
        
        double a_1 = regionalModelProperties.getA1();
        double b_1 = regionalModelProperties.getB1();
        double a_2 = regionalModelProperties.getA2();
        double b_2 = regionalModelProperties.getB2();
        double eta_th = regionalModelProperties.getEtaTh();
   
        boolean portsFlag = true;
        
        double eta_eq = portsFlag ? 0.0 : 0.1;
        double counter_duration = 250.0;
        double insensitive_duration = 250.0;
        double k_insensitive = 1.0;
        
        BrainUtils.setInitialValue(diagram, "omega", omega);
        BrainUtils.setInitialValue(diagram, "alpha", alpha);
        BrainUtils.setInitialValue(diagram, "b", b);
        BrainUtils.setInitialValue(diagram, "gamma", gamma);
        
        BrainUtils.setInitialValue(diagram, "a_1", a_1);
        BrainUtils.setInitialValue(diagram, "b_1", b_1);
        BrainUtils.setInitialValue(diagram, "a_2", a_2);
        BrainUtils.setInitialValue(diagram, "b_2", b_2);
        
        BrainUtils.setInitialValue(diagram, "counter_duration", counter_duration);
        BrainUtils.setInitialValue(diagram, "insensitive_duration", insensitive_duration);
        
        for (int i = 0; i < sizeColumns; i++)
        {
        	String etaThI = "eta_th_" + String.valueOf(i + 1);
        	String etaEqI = "eta_eq_" + String.valueOf(i + 1);
        	String kInsensitiveI = "k_insensitive_" + String.valueOf(i + 1);
        	
        	BrainUtils.setInitialValue(diagram, etaThI, eta_th);
        	BrainUtils.setInitialValue(diagram, etaEqI, eta_eq);
        	BrainUtils.setInitialValue(diagram, kInsensitiveI, k_insensitive);
        }
        
        for (int i = 0; i < sizeColumns; i++) 
        {
            BrainUtils.setInitialValue(diagram, "lambda_" + String.valueOf(i + 1), 1.0 / sizeRows);
        }
        
        ArrayList<ArrayList<Integer>> clusters = BrainUtils.getClusters(connectivityMatrix);
        for (int j = 0; j < sizeRows; j++)
        {
        	double tau = 0.0;
        	int clusterNumber = 1;
        	for (ArrayList<Integer> cluster: clusters) 
        	{
        		if (cluster.contains(j)) 
        		{
        			tau = 2.0 * Math.PI * clusterNumber / clusters.size();
        			break;
        		}
        		clusterNumber++;
        	}
            BrainUtils.setInitialValue(diagram, "tau_" + String.valueOf(j + 1), tau);
        }
        
        double A_d = 1.0;
        double T_d = 6.0;
        BrainUtils.setInitialValue(diagram, "A_d", A_d);
        BrainUtils.setInitialValue(diagram, "T_d", T_d);

        double[][] couplingMatrix = BrainUtils.getCouplingMatrix(connectivityMatrix);
        for (int i = 0; i < sizeColumns; i++) 
        {
        	for (int j = 0; j < sizeRows; j++) 
        	{
        		String epsilonMinJI = "epsilonmin_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1);
        		String epsilon0JI = "epsilon0_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1);
        		
                BrainUtils.setInitialValue(diagram, epsilonMinJI, 0.0);
                
            	// set low coupling strength for use in composite diagram
            	double couplingStrength = portsFlag ? couplingMatrix[j][i] / 10.0 : couplingMatrix[j][i];
            	BrainUtils.setInitialValue(diagram, epsilon0JI, couplingStrength);
        	}
        }
        
        
        double time_counter = 50000.0;
        double time_insensitive = 50000.0;
        for (int i = 0; i < sizeColumns; i++)
        {
        	String timeCounterI = "time_counter_" + String.valueOf(i + 1);
        	String timeInsensitiveI = "time_insensitive_" + String.valueOf(i + 1);
        	
            BrainUtils.setInitialValue(diagram, timeCounterI, time_counter);
            BrainUtils.setInitialValue(diagram, timeInsensitiveI, time_insensitive);
        }
	}
	
	private static void setPlotsRosslerForMultilevelModel(Diagram diagram)
	{
    	EModel emodel = diagram.getRole(EModel.class);
    	PlotsInfo plotsInfo = new PlotsInfo(emodel);
    	
        PlotInfo varPlotsExcitationSignal = new PlotInfo();
        varPlotsExcitationSignal.setTitle("Excitation signal 1");
        
        PlotInfo varPlotsSynapticStrength = new PlotInfo();
        varPlotsSynapticStrength.setTitle("Synaptic strength 1");
        
        PlotInfo varPlotsEegRecording = new PlotInfo();
        varPlotsEegRecording.setTitle("EEG Recording");
        
        plotsInfo.setPlots( new PlotInfo[] {varPlotsExcitationSignal, varPlotsSynapticStrength, varPlotsEegRecording});

        List<Curve> curvesExcitationSignal = new ArrayList<Curve>();
        curvesExcitationSignal.add(new Curve("", "u_exc_1", "u_exc_1", emodel));
        
        List<Curve> curvesSynapticStrength = new ArrayList<Curve>();
        curvesSynapticStrength.add(new Curve("", "eta_1", "eta_1", emodel));
        
        List<Curve> curvesEegRecording = new ArrayList<Curve>();
        curvesEegRecording.add(new Curve("", "V_EEG", "V_EEG", emodel));
        
        varPlotsExcitationSignal.setYVariables(curvesExcitationSignal.stream().toArray(Curve[]::new));
        varPlotsSynapticStrength.setYVariables(curvesSynapticStrength.stream().toArray(Curve[]::new));
        varPlotsEegRecording.setYVariables(curvesEegRecording.stream().toArray(Curve[]::new));
        DiagramUtility.setPlotsInfo(diagram, plotsInfo);
	}
}
