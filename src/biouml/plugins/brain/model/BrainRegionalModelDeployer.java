package biouml.plugins.brain.model;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
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
import biouml.plugins.brain.diagram.BrainDiagramType;
import biouml.plugins.brain.diagram.BrainRegionalModel;
import biouml.plugins.brain.diagram.BrainType;
import biouml.plugins.brain.diagram.BrainUtils;
import biouml.plugins.brain.model.regional.EpileptorRegionalModelProperties;
import biouml.plugins.brain.model.regional.RosslerRegionalModelProperties;
import biouml.plugins.brain.sde.EulerStochastic;
import biouml.plugins.brain.sde.JavaSdeSimulationEngine;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Type;
import ru.biosoft.graphics.Pen;
import ru.biosoft.util.DPSUtils;

/*
 * Equations deployer for regional brain models.
 */
public class BrainRegionalModelDeployer
{
	public static @Nonnull Diagram deployBrainRegionalModel(Diagram src, String name) throws Exception
	{
	    return deployBrainRegionalModel(src, name, true);
	}

    public static @Nonnull Diagram deployBrainRegionalModel(Diagram src, String name, boolean needLayout) throws Exception
    {
    	BrainRegionalModel regionalModel = BrainUtils.getRegionalModelNodes(src).get(0).getRole(BrainRegionalModel.class);
    	String regionalModelType = regionalModel.getRegionalModelType();
    	switch (regionalModelType)
    	{
    	    case BrainType.TYPE_REGIONAL_ROSSLER:
    	    	return fillDiagramWithEquationsRossler(src, name);
    	    case BrainType.TYPE_REGIONAL_EPILEPTOR:
    	    	return fillDiagramWithEquationsEpileptor(src, name);
    	    default:
    	    	return (new BrainDiagramType()).createDiagram(null, null, null);
    	}
    }
    
    public static Diagram fillDiagramWithEquationsRossler(Diagram diagram, String name) throws Exception
    {   
        String dName;
    	if (name.isEmpty())
    	{
    		String prefix = diagram.getName();
            String suffix = "regional_Rossler_equations";
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
    	int N = connectivityMatrix.length;
    	
    	BrainRegionalModel regionalModel = BrainUtils.getRegionalModelNodes(diagram).get(0).getRole(BrainRegionalModel.class);
    	RosslerRegionalModelProperties regionalModelProperties = (RosslerRegionalModelProperties)regionalModel.getRegionalModelProperties();
        
        int yGroupOffset = 80;
        int yLocalOffset = 50;
        Point point = new Point(0, 0);
        
        double[][] couplingMatrix = BrainUtils.getCouplingMatrix(connectivityMatrix);
        
        /*
         * Region dynamic
         * (x_i, y_i, z_i)
         */
        for (int i = 0; i < N; i++)
        {
        	String xi = "x_" + String.valueOf(i + 1);
        	String yi = "y_" + String.valueOf(i + 1);
        	String zi = "z_" + String.valueOf(i + 1);
        	
            String xFormula = "-omega*" + yi + "-" + zi;
            for (int j = 0; j < N; j ++) 
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
            
            // adding desynchronization signal
            xFormula += "+A_d*sin(2.0*pi*(time-tau_" + String.valueOf(i + 1) + ")/T_d)";
            
            String yFormula = "omega*" + xi + "+alpha*" + yi;
            String zFormula = "b+" + zi + "*(" + xi + "-gamma)";
            
            BrainUtils.createEquation("equation_" + xi, xi, xFormula, 
            		Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset + 10);
            
            BrainUtils.createEquation("equation_" + yi, yi, yFormula, Equation.TYPE_RATE,
            		diagramEq, point);
            point.translate(0, yLocalOffset);
            
            BrainUtils.createEquation("equation_" + zi, zi, zFormula, 
            		Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yGroupOffset);
        }
        
        /*
         * Synaptic strength
         * (eta, eta_eq)
         */
        String etaFormula = "(1.0 - eta)*(a_1*(eta - eta_eq)*(eta - eta_th) + b_1*u_exc*k_insensitive)";
        BrainUtils.createEquation("equation_eta", "eta", etaFormula, 
        		Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String etaEqFormula = "-a_2*eta_eq + b_2*(eta - eta_eq)";
        BrainUtils.createEquation("equation_eta_eq", "eta_eq", etaEqFormula, 
        		Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yGroupOffset);
        
        /*
         * EEG recordings 
         * (V_EEG)
         */
        String eegFormula = "";
        for (int i = 0; i < N; i++) 
        {
        	eegFormula += "+lambda_" + String.valueOf(i + 1) + "*x_" + String.valueOf(i + 1);
        }
        BrainUtils.createEquation("equation_EEG", "V_EEG", eegFormula, 
        		Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yGroupOffset);
        
        // epsilon_ji matrix
        for (int i = 0; i < N; i++)
        {        	
            for (int j = 0; j < N; j++)
            {
            	if (j == i || !(couplingMatrix[j][i] > 0.0) )
            	{
            		continue;
            	}
            	else 
            	{
            		/*
            		 * coupling factor epsilon_ji(eta):
            		 * epsilon_ji = epsilonmin_ji + epsilon0_ji*eta
            		 */
            		String epsilonFormula = "epsilonmin_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1) + "+epsilon0_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1) + "*eta"; 
            		
            		BrainUtils.createEquation("equation_epsilon_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1), "epsilon_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1), epsilonFormula,
                            Equation.TYPE_SCALAR, diagramEq, point);
            		point.translate(0, yLocalOffset);
            	}
            }
        }
        point.translate(0, yGroupOffset);
        
        // a formal mechanism to stop a seizure
        BrainUtils.createEquation("time_counter_equation", "time_counter", "-1.0",
    			Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        BrainUtils.createEquation("time_insensitive_equation", "time_insensitive", "-1.0",
    			Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        BrainUtils.createEvent("threshold_reached", "eta>eta_th", new Assignment[] {new Assignment( "time_counter", "counter_duration" )},
    			diagramEq, point);
        point.translate(0, yLocalOffset + 30);
        
        BrainUtils.createEvent("counter_ended", "time_counter<=0.0", new Assignment[] { new Assignment("eta", "0.0"), new Assignment("eta_eq", "0.0"),  new Assignment("time_insensitive", "insensitive_duration"), new Assignment("k_insensitive", "0.0") },
    			diagramEq, point);
        point.translate(0, yLocalOffset + 90);
        
        
        boolean portsFlag = regionalModelProperties.getPortsFlag();
        if (portsFlag)
        {
            BrainUtils.createEvent("insensitive_ended", "time_insensitive<=0.0", new Assignment[] { new Assignment( "k_insensitive", "1.0" ), new Assignment( "eta_eq", "0.0" )},
        			diagramEq, point);
            point.translate(0, yLocalOffset + 40);
        	
            String variableName = "u_exc";
            BrainUtils.createPort(variableName, variableName, Type.TYPE_INPUT_CONNECTION_PORT,
            		diagramEq, point);
            point.translate(0, yLocalOffset + 10);
        }
        else
        {
            BrainUtils.createEvent("insensitive_ended", "time_insensitive<=0.0", new Assignment[] {new Assignment( "k_insensitive", "1.0" ), new Assignment( "eta_eq", "0.1" )},
        			diagramEq, point);
            point.translate(0, yLocalOffset + 40);
        	
            // parameters for 31 regions
            double excStrength = 8.0; 
            double excDutyCycle = 1.0/3.0;
            double excPeriod = 90.0; 
            double excTimeMax = 2000.0; 
            double initialTimeRest = excPeriod - excPeriod * excDutyCycle;
            
            // parameters for 3 regions
            //double excStrength = 4.0;
            //double excDutyCycle = 0.2;
            //double excPeriod = 700.0; 
            //double excTimeMax = 20000.0; 
            //double initialTimeRest = 700.0 
            
            setExcitationSignal(diagramEq, initialTimeRest, excStrength, excDutyCycle, excPeriod, excTimeMax, new Point(600, 70));
            //setExcitationSignalByEvents(diagramEq, initialTimeRest, excStrength, excDutyCycle, excPeriod, excTimeMax, new Point(1000, 0));
        }
        
        setInitialValuesRossler(diagramEq, connectivityMatrix, regionalModelProperties);
        
        // Simulator settings
        JavaSimulationEngine se = new JavaSimulationEngine();
        se.setDiagram(diagramEq);
        se.setCompletionTime(2000.0);
        se.setTimeIncrement(0.1);
        se.setSolver(new JVodeSolver());
        diagramEq.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, se));
        
        setPlotsRossler(diagramEq);
        
      	return diagramEq;
    }
    
    public static Diagram fillDiagramWithEquationsEpileptor(Diagram diagram, String name) throws Exception
    {
    	String dName;
    	if (name.isEmpty())
    	{
    		String prefix = diagram.getName();
            String suffix = "regional_epileptor_equations";
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
    	int N = connectivityMatrix.length;
    	
    	double[][] delayMatrix = BrainUtils.getDelayMatrix(diagram);
    			
    	BrainRegionalModel regionalModel = BrainUtils.getRegionalModelNodes(diagram).get(0).getRole(BrainRegionalModel.class);
    	EpileptorRegionalModelProperties regionalModelProperties = (EpileptorRegionalModelProperties)regionalModel.getRegionalModelProperties();
        
    	boolean modification = regionalModelProperties.getModification();
    	
    	List<Integer> healthyZoneNodesNumbers = new ArrayList<Integer>();
        List<Integer> propagationZoneNodesNumbers = new ArrayList<Integer>();
        List<Integer> epileptogenicZoneNodesNumbers = new ArrayList<Integer>();
         
        if (N == 2)
        {
        	healthyZoneNodesNumbers.add(2);
        	epileptogenicZoneNodesNumbers.add(1);
        }
        else if (N == 28)
        {
//        	for (int i = 1; i <= 18; i++)
//         	{
//         		healthyZoneNodesNumbers.add(i);
//         	}
//         	propagationZoneNodesNumbers.addAll(Arrays.asList(19, 20, 21, 22, 26, 27, 28));
         	epileptogenicZoneNodesNumbers.addAll(Arrays.asList(23, 24, 25));
         	
          	for (int i = 1; i <= N; i++)
         	{
         		if (!epileptogenicZoneNodesNumbers.contains(i))
         		{
         			healthyZoneNodesNumbers.add(i);
         		}
         	}
        }
        else if (N == 84)
        {
        	epileptogenicZoneNodesNumbers.add(64);
         	for (int i = 1; i <= N; i++)
         	{
         		if (!epileptogenicZoneNodesNumbers.contains(i))
         		{
         			healthyZoneNodesNumbers.add(i);
         		}
         	}
        }
        else
        {
         	for (int i = 1; i <= N; i ++)
         	{
         		healthyZoneNodesNumbers.add(i);
         	}
        }
    	
        int yGroupOffset = 100;
        int yLocalOffset = 60;
        Point point = new Point(0, 0);

        BrainUtils.createNote("Time unit in the Epileptor model corresponds to 0.02 s of real time.", new Dimension(225, 40), 
        		diagramEq, point);
        point.translate(0, yLocalOffset - 15);
        BrainUtils.createEquation("time_in_sec", "time_sec", "time/50.0",
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 15);
        BrainUtils.createEquation("time_in_min", "time_min", "time_sec/60.0",
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset + 15);
        
        String f1Name = "f1";
        String f1Formula = "function f1(x1,x2,z) = piecewise( x1 < 0.0 => x1^3.0 - 3.0*x1^2.0; (x2 - 0.6*(z - 4.0)^2.0)*x1 )";
        BrainUtils.createFunction(f1Name, f1Formula,
        		diagramEq, point);
        point.translate(0, yLocalOffset + 10);
        
        String f2Name = "f2";
        String f2Formula = "function f2(x1,x2) = piecewise( x2 < (-0.25) => 0.0; 6.0*(x2+0.25) )";
        BrainUtils.createFunction(f2Name, f2Formula,
        		diagramEq, point);
        point.translate(0, yLocalOffset);
        
        double excStrength = 0.5; 
        double excDutyCycle = 0.5;
        double excPeriod = 2.0; 
        double excTimeMax = 20000.0; 
        double initialTimeRest = excPeriod - excPeriod * excDutyCycle;
        setExcitationSignal(diagramEq, initialTimeRest, excStrength, excDutyCycle, excPeriod, excTimeMax, point);
        point.translate(0, 2 * yGroupOffset);
        
        
        /*
         * Region dynamic
         * (x1_i, y1_i, z_i, x2_i, y2_i, g_i)
         */
        BrainUtils.createNote("Region dynamic (x1_i, y1_i, z_i, x2_i, y2_i, g_i)", new Dimension(300, 30), 
        		diagramEq, point);
        point.translate(0, yLocalOffset - 10);
        for (int i = 0; i < N; i++)
        {
        	String x1I = "x1_" + String.valueOf(i + 1);
        	String y1I = "y1_" + String.valueOf(i + 1);
        	String zI = "z_" + String.valueOf(i + 1);
        	String x2I = "x2_" + String.valueOf(i + 1);
        	String y2I = "y2_" + String.valueOf(i + 1);
        	String gI = "g_" + String.valueOf(i + 1);
        	
        	String y0I = "y0_" + String.valueOf(i + 1);
        	String x0I = "x0_" + String.valueOf(i + 1);
        	
            String x1Formula = y1I + "-f1(" + x1I + "," + x2I + "," + zI + ")" + "-" + zI + "+I1";
            String y1Formula = "(1.0 / tau1)*(" + y0I + "-5.0*" + x1I + "^2.0-" + y1I + ")";
            
            String h;
            if (modification) 
            {
            	h = x0I + "+3.0/(1.0+exp(-(" + x1I + "+0.5)/0.1))";
            }
            else 
            {
            	h = "4.0*(" + x1I + "-" + x0I + ")";
            }
            String zFormula = "(1/tau0)*(" + h + "-" + zI + ")"; // from article
            //String zFormula = "piecewise(" + x1i + "<0.0 => (1/tau0)*(4.0*(" + x1i + "-x0)-" + zi + "-0.1*" + zi + "^7.0); (1/tau0)*(4.0*(" + x1i + "-x0)-" + zi + "))"; // from virtual brain project
            String x2Formula = "-" + y2I + "+" + x2I + "-" + x2I + "^3.0" + "+I2+2.0*" + gI + "-0.3*(" + zI + "-3.5)";
            String y2Formula = "(1.0/tau2)*(-" + y2I + "+f2(" + x1I + "," + x2I + "))";
            String gFormula = "-gamma*(" + gI + "-0.1*" + x1I + ")" ;
            
            String rI = "r_" + String.valueOf(i + 1);
            for (int j = 0; j < N; j ++) 
            {
            	if (j == i || !(connectivityMatrix[j][i] > 0.0) ) 
            	{
            		continue;
            	}
            	
            	else 
            	{
            		String cJI = "C_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1);
            		String x1J = "x1_" + String.valueOf(j + 1);
            		
            		// add coupling term
            		if (delayMatrix == null)
            		{
            			//zFormula += "-(W/tau0)*" + cJI + "*(" + x1J + "-" + x1I + ")";
            			zFormula += "-(W/tau0)*" + cJI + "*" + rI + "*(" + x1J + "-" + x1I + ")";
            		}
            		else
            		{
            			String tauJI = "tau_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1);
            			//zFormula += "-(W/tau0)*" + cJI + "*(delay(" + x1J + "," + tauJI + ")-" + x1I + ")";
            			zFormula += "-(W/tau0)*" + cJI + "*" + rI + "*(delay(" + x1J + "," + tauJI + ")-" + x1I + ")";
            		}
            	}
            }
            
//            if (epileptogenicZoneNodesNumbers.contains(i + 1))
//            {
            	String uExcI = "u_exc_" + String.valueOf(i + 1);
            	zFormula += "-" + uExcI + "/tau0";
//            }
            
            x2Formula += "+noise(0.0025)";
            y2Formula += "+noise(0.0025)";
            
            BrainUtils.createEquation("equation_" + x1I, x1I, x1Formula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset);
            
            BrainUtils.createEquation("equation_" + y1I, y1I, y1Formula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset);
            
            BrainUtils.createEquation("equation_" + zI, zI, zFormula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset);
            if (modification)
            {
            	point.translate(0, 20);
            }
            
            BrainUtils.createEquation("equation_" + x2I, x2I, x2Formula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset);
            
            BrainUtils.createEquation("equation_" + y2I, y2I, y2Formula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset);
            
            BrainUtils.createEquation("equation_" + gI, gI, gFormula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yGroupOffset);
        }
        
        /*
         * LFP recordings
         * (LFP_i)
         * Variables of interest to be used by monitors: -x1_i + x2_i or +x1_i + x2_i
         */
        boolean distinctLFP = true; // show LFP signals for distinct regions on one plot
        if (distinctLFP) 
        {
            for (int i = 0; i < N; i++) 
            {
            	point.translate(0, yLocalOffset);
          	    String lfpIFormula = "-x1_" + String.valueOf(i + 1) + "+x2_" + String.valueOf(i + 1) + "-3.7*" + String.valueOf(i); // offset for graph with all channels 
          	    //String lfpIFormula = "+x1_" + String.valueOf(i + 1) + "+x2_" + String.valueOf(i + 1) + "-3.7*" + String.valueOf(i); // offset for graph with all channels 
          	    //lfpFormula = "+x1_" + String.valueOf(i + 1) + "+x2_" + String.valueOf(i + 1); // no offset
                BrainUtils.createEquation("equation_LFP", "LFP_" + String.valueOf(i + 1), lfpIFormula,
                		Equation.TYPE_SCALAR, diagramEq, point);
            }
        }
        point.translate(0, yGroupOffset);
        
        boolean portsFlag = regionalModelProperties.getPortsFlag();
        if (portsFlag)
        {
        	point = new Point(850, 0);
        	for (int i = 0; i < N; i++)
        	{
        		String uExcI = "u_exc_" + String.valueOf(i + 1);
        		String x1I = "x1_" + String.valueOf(i + 1);
        		
                BrainUtils.createPort(uExcI, uExcI, Type.TYPE_INPUT_CONNECTION_PORT,
                		diagramEq, point);
                point.translate(100, 0);
                
                String wI = "W_" + String.valueOf(i + 1);
                BrainUtils.createPort(wI, wI, Type.TYPE_INPUT_CONNECTION_PORT,
                		diagramEq, point);
                point.translate(80, 0);
                
                BrainUtils.createPort(x1I, x1I, Type.TYPE_OUTPUT_CONNECTION_PORT,
                		diagramEq, new Point(point.x + 80, point.y));
                point.translate(-180, yLocalOffset - 20);
        	}
        	
            point = new Point(1250, 0);
            String wFormula = "(W_0 / " + String.valueOf(N) + ")*(";
            for (int i = 0; i < N; i++)
            {
            	String wI = "W_" + String.valueOf(i + 1);
            	wFormula += "+" + wI;
            }
            wFormula += ")";
            BrainUtils.createEquation("global_connectivity_strength", "W", wFormula,
            		Equation.TYPE_SCALAR, diagramEq, point);
            point.translate(0, yLocalOffset);
          
	      	for (int i = 0; i < N; i++)
	      	{
	      		String x1I = "x1_" + String.valueOf(i + 1);
	      		String refractoryCounterI = "refractory_counter_" + String.valueOf(i + 1);
	      		String rI = "r_" + String.valueOf(i + 1);
	      		
	      		Double refractoryTime = 60.0 * 50.0;
	            BrainUtils.createEvent("region_" + String.valueOf(i + 1) + "_refractory_period_started", x1I + " <= -1.0", new Assignment[] {new Assignment(refractoryCounterI, String.valueOf(refractoryTime)), new Assignment(rI, "0.0")},
	            		diagramEq, new Point(point.x, point.y));
	            point.translate(0, yGroupOffset + 5);
	            
	            diagramEq.getRole(EModel.class).declareVariable(refractoryCounterI, -2.0);
	            
	            BrainUtils.createEquation(refractoryCounterI + "_eq", refractoryCounterI, "-1.0",
	            		Equation.TYPE_RATE, diagramEq, point);
	            point.translate(0, yGroupOffset - 40);
	            
	            BrainUtils.createEvent("region_" + String.valueOf(i + 1) + "_refractory_period_ended", refractoryCounterI + " <= 0.0", new Assignment[] {new Assignment(rI, "1.0")},
	            		diagramEq, new Point(point.x, point.y));
	            point.translate(0, yGroupOffset);
	      	}
        }
        
        setInitialValuesEpileptor(diagramEq, connectivityMatrix, delayMatrix, regionalModelProperties, healthyZoneNodesNumbers, propagationZoneNodesNumbers, epileptogenicZoneNodesNumbers);
        
        //Simulator settings
        JavaSdeSimulationEngine se = new JavaSdeSimulationEngine();
        se.setDiagram(diagramEq);
        if (modification) 
        {
        	 se.setCompletionTime(20000.0);
        }
        else 
        {
        	 se.setCompletionTime(6000.0);
        }
        se.setTimeIncrement(1.0);
        se.setSolver(new EulerStochastic());
        diagramEq.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, se));
        
        setPlotsEpileptor(diagramEq, N, healthyZoneNodesNumbers, propagationZoneNodesNumbers, epileptogenicZoneNodesNumbers);
        
        return diagramEq;
    }
   
    private static void setInitialValuesRossler(Diagram diagram, double[][] connectivityMatrix, RosslerRegionalModelProperties regionalModelProperties) 
    {	
    	int N = connectivityMatrix.length;
    	
        double omega = regionalModelProperties.getOmega();
        double alpha = regionalModelProperties.getAlpha();
        double b = regionalModelProperties.getB();
        double gamma = regionalModelProperties.getGamma();
        
        double a_1 = regionalModelProperties.getA1();
        double b_1 = regionalModelProperties.getB1();
        double a_2 = regionalModelProperties.getA2();
        double b_2 = regionalModelProperties.getB2();
        double eta_th = regionalModelProperties.getEtaTh();
   
        boolean portsFlag = regionalModelProperties.getPortsFlag();
        
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
        BrainUtils.setInitialValue(diagram, "eta_th", eta_th);
        BrainUtils.setInitialValue(diagram, "eta_eq", eta_eq);
        
        BrainUtils.setInitialValue(diagram, "counter_duration", counter_duration);
        BrainUtils.setInitialValue(diagram, "insensitive_duration", insensitive_duration);
        BrainUtils.setInitialValue(diagram, "k_insensitive", k_insensitive);
    
        
        for (int i = 0; i < N; i++) 
        {
            BrainUtils.setInitialValue(diagram, "lambda_" + String.valueOf(i + 1), 1.0 / N);
        }
        
        ArrayList<ArrayList<Integer>> clusters = BrainUtils.getClusters(connectivityMatrix);
        for (int j = 0; j < N; j++)
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
        for (int i = 0; i < N; i++) 
        {
        	for (int j = 0; j < N; j++) 
        	{
                BrainUtils.setInitialValue(diagram, "epsilonmin_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1), 0.0);
                
            	// set low coupling strength for use in composite diagram
            	double couplingStrength = portsFlag ? couplingMatrix[j][i] / 10.0 : couplingMatrix[j][i];
            	BrainUtils.setInitialValue(diagram, "epsilon0_" + String.valueOf(j + 1) + "_" + String.valueOf(i + 1), couplingStrength);
        	}
        }
        
        // these counters should only be triggered after a seizure
        double time_counter = Double.MAX_VALUE;
        double time_insensitive = Double.MAX_VALUE;
        BrainUtils.setInitialValue(diagram, "time_counter", time_counter);
        BrainUtils.setInitialValue(diagram, "time_insensitive", time_insensitive);
    }

    private static void setInitialValuesEpileptor(Diagram diagram, double[][] connectivityMatrix, double[][] delayMatrix, EpileptorRegionalModelProperties regionalModelProperties,
    		List<Integer> healthyZoneNumbers, List<Integer> propagationZoneNumbers, List<Integer> epileptogenicZoneNumbers) 
    {
    	int N = connectivityMatrix.length;
    	
    	boolean modification = regionalModelProperties.getModification();
    	double w = regionalModelProperties.getW();
    	double x0 = regionalModelProperties.getX0();
    	double y0 = regionalModelProperties.getY0();
    	double tau0 = regionalModelProperties.getTau0();
    	double tau1 = regionalModelProperties.getTau1();
    	double tau2 = regionalModelProperties.getTau2();
    	double I1 = regionalModelProperties.getI1();
        double I2 = regionalModelProperties.getI2();
        double gamma = regionalModelProperties.getGamma();
        
        // default parameters
        double x1 = 0.0;
        double y1 = -5.0;
        double z = 3.0;
        double x2 = 0.0;
        double y2 = 0.0;
        double g = 0.0;

//      // from Epileptor 2014 supplementary material
//    	double x1 = 0.022;
//    	double y1 = 0.91;
//    	double z = 3.84;
//    	double x2 = -1.11;
//    	double y2 = 0.73;
//    	double g = 0.0;
        
//    	// good fit for article about system with 2 Epileptors
//    	double x1 = -1.8;
//    	double y1 = -15.5;
//    	double z = 3.5;
//    	double x2 = -0.95;
//    	double y2 = 0.0;
//    	double g = -0.18;
    	
//    	// from 2022 article with connectivity and delay matrix for 84 parcels
//    	double x1 = -1.6;
//    	double y1 = -12.0;
//    	double z = 3.2;
//    	double x2 = 0.0;
//    	double y2 = 0.0;
//    	double g = -160;
    	
        for (int i = 1; i <= N; i++) 
        {
        	String x1i = "x1_" + String.valueOf(i);
        	String y1i = "y1_" + String.valueOf(i);
        	String zi = "z_" + String.valueOf(i);
        	String x2i = "x2_" + String.valueOf(i);
        	String y2i = "y2_" + String.valueOf(i);
        	String gi = "g_" + String.valueOf(i);
   
        	String y0i = "y0_" + String.valueOf(i);
        	String x0i = "x0_" + String.valueOf(i);
        	
        	if (N == 2)
        	{
        		// for system with 2 regions
            	x1 = -1.8;
            	y1 = -15.5;
            	z = 3.5;
            	x2 = -0.95;
            	y2 = 0.0;
            	g = -0.18;
        		
            	if (healthyZoneNumbers.contains(i)) 
            	{
            		BrainUtils.setInitialValue(diagram, x0i, modification ? 3.2 : -2.2);
            	}
            	else if (epileptogenicZoneNumbers.contains(i))
            	{
            		BrainUtils.setInitialValue(diagram, x0i, modification ? 2.5 : -1.6);
            	}
            	
            	w = 0.6;
        	}
        	else if (N == 28)
        	{
        		/* 
        		 * This is distribution of regions by zones
        		 * from the 2017 epileptor article
        		 */
            	x1 = -1.8;
            	y1 = -15.5;
            	z = 3.5;
            	x2 = -0.95;
            	y2 = 0.0;
            	g = -0.18;
        		
//            	if (healthyZoneNumbers.contains(i)) // healthy zone, blue
//        		{
//        			//BrainUtils.setInitialValue(diagram, x0i, -2.6); 
//        			BrainUtils.setInitialValue(diagram, x0i, -2.6); 
//        		}
//        		else if (epileptogenicZoneNumbers.contains(i)) // epileptogenic zone, yellow
//        		{
//        			//BrainUtils.setInitialValue(diagram, x0i, x0); 
//        			BrainUtils.setInitialValue(diagram, x0i, -1.6);
//        		}
//        		else if (propagationZoneNumbers.contains(i)) // propagation zone, red
//        		{
//        			if (i == 22 || i == 27) 
//        			{
//        				//BrainUtils.setInitialValue(diagram, x0i, -1.8);
//        				BrainUtils.setInitialValue(diagram, x0i, -2.05);
//        			}
//        			else if (i == 19 || i == 21 || i == 28)
//        			{
//        				//BrainUtils.setInitialValue(diagram, x0i, -2.0);
//        				BrainUtils.setInitialValue(diagram, x0i, -2.15);
//        			}
//        			else if (i == 20 || i == 26) 
//        			{
//        				//BrainUtils.setInitialValue(diagram, x0i, -2.2);
//        				BrainUtils.setInitialValue(diagram, x0i, -2.3);
//        			}
//        		}
            	
          
        		if (epileptogenicZoneNumbers.contains(i)) // epileptogenic zone
        		{
        			BrainUtils.setInitialValue(diagram, x0i, -1.6);
        		}
        		else // other nodes
        		{
        			//BrainUtils.setInitialValue(diagram, x0i, -2.2); // whole-network seizure spread 
        			//BrainUtils.setInitialValue(diagram, x0i, -2.4); // only 1 node outside of EZ has been recruited 
        			BrainUtils.setInitialValue(diagram, x0i, -2.3); // 1-3 nodes outside of EZ have been recruited 
        		}
        	}
        	else if (N == 84)
        	{
//        		/*
//        		 * 84 parcels from 2022 article
//        		 */
//            	x1 = -1.6;
//            	y1 = -12.0;
//            	z = 3.2;
//            	x2 = 0.0;
//            	y2 = 0.0;
//            	g = -160;
//            	
//            	//w = 1.6; // A from 2022 article with connectivity and delay matrices for 84 parcels
//            	w = 0.9; // C from 2022 article with connectivity and delay matrices for 84 parcels
//            	
            	tau0 = 6667.0; // from 2022 article with connectivity and delay matrices for 84 parcels
        		
        		/* 
        		 * This is distribution of regions by zones
        		 * from the 2017 epileptor article
        		 */
            	x1 = -1.8;
            	y1 = -15.5;
            	z = 3.5;
            	x2 = -0.95;
            	y2 = 0.0;
            	g = -0.18;
        		
        		if (healthyZoneNumbers.contains(i))
        		{
        			BrainUtils.setInitialValue(diagram, x0i, -1.6);
        		}
        		else if (epileptogenicZoneNumbers.contains(i))
        		{
        			//BrainUtils.setInitialValue(diagram, x0i, -2.3); // A
        			BrainUtils.setInitialValue(diagram, x0i, -2.1); // C
        		}
        	}
        	else 
        	{
        		BrainUtils.setInitialValue(diagram, x0i, x0);
        	}
        	
        	BrainUtils.setInitialValue(diagram, x1i, x1);
        	BrainUtils.setInitialValue(diagram, y1i, y1);
        	BrainUtils.setInitialValue(diagram, zi, z);
        	BrainUtils.setInitialValue(diagram, x2i, x2);
        	BrainUtils.setInitialValue(diagram, y2i, y2);
        	BrainUtils.setInitialValue(diagram, gi, g);
        	BrainUtils.setInitialValue(diagram, y0i, y0);
        	
        	BrainUtils.setInitialValue(diagram, "r_" + String.valueOf(i), 1.0);
        }
        
        BrainUtils.setInitialValue(diagram, "W_0", w);
        
        BrainUtils.setInitialValue(diagram, "W", w);
        BrainUtils.setInitialValue(diagram, "tau0", tau0);
        BrainUtils.setInitialValue(diagram, "tau1", tau1);
        BrainUtils.setInitialValue(diagram, "tau2", tau2);
        BrainUtils.setInitialValue(diagram, "I1", I1);
        BrainUtils.setInitialValue(diagram, "I2", I2);
        BrainUtils.setInitialValue(diagram, "gamma", gamma);
        
        for (int i = 0; i < N; i++) 
        {
        	for (int j = 0; j < N; j++) 
        	{
        		String cIJ = "C_" + String.valueOf(i + 1) + "_" + String.valueOf(j + 1);
        		
            	BrainUtils.setInitialValue(diagram, cIJ, connectivityMatrix[i][j]);
        	}
        }
        
        if (delayMatrix != null)
        {
            for (int i = 0; i < N; i++) 
            {
            	for (int j = 0; j < N; j++) 
            	{
            		String tauIJ = "tau_" + String.valueOf(i + 1) + "_" + String.valueOf(j + 1); 
            		
                	//BrainUtils.setInitialValue(diagram, tauIJ, delayMatrix[i][j]);
            		BrainUtils.setInitialValue(diagram, tauIJ, delayMatrix[i][j] * 50.0); // from seconds to epileptor model time units.
            	}
            }
        }
        
        for (int i = 0; i < N; i++) 
        {
        	BrainUtils.setInitialValue(diagram, "lambda_" + String.valueOf(i + 1), 1.0 / N);
        }
        
        // add synaptic strength from Rossler model
        double a_1 = 0.5;
        double b_1 = 0.005;
        double a_2 = 0.0013;
        double b_2 = 0.005;
        double eta_th = 0.5;
        double eta_eq = 0.1;
        double counter_duration = 2500.0;
        double insensitive_duration = 2500.0;
        double k_insensitive = 1.0;
        
        BrainUtils.setInitialValue(diagram, "a_1", a_1);
        BrainUtils.setInitialValue(diagram, "b_1", b_1);
        BrainUtils.setInitialValue(diagram, "a_2", a_2);
        BrainUtils.setInitialValue(diagram, "b_2", b_2);
        BrainUtils.setInitialValue(diagram, "eta_th", eta_th);
        BrainUtils.setInitialValue(diagram, "eta_eq", eta_eq);
        
        BrainUtils.setInitialValue(diagram, "counter_duration", counter_duration);
        BrainUtils.setInitialValue(diagram, "insensitive_duration", insensitive_duration);
        BrainUtils.setInitialValue(diagram, "k_insensitive", k_insensitive);
        
        // these counters should only be triggered after a seizure
        double time_counter = Double.MAX_VALUE;
        double time_insensitive = Double.MAX_VALUE;
        BrainUtils.setInitialValue(diagram, "time_counter", time_counter);
        BrainUtils.setInitialValue(diagram, "time_insensitive", time_insensitive); 
    }
    
	/*
	 * method creates pulse train excitation signal with given parameters using function
	 * excitation signal also can be set using cellular level models.
	 */
    private static void setExcitationSignal(Diagram diagram, double initialTimeRest, double excStrength, double excDutyCycle, double excPeriod, double excTimeMax, Point point) throws Exception
    {
        double excDuration = excPeriod * excDutyCycle;
        
        String rectangularPulseFunctionName = "pulse";
        String rectangularPulseFunctionFormula = "function pulse(x, A, T, Tp) = piecewise( abs(mod(x + T/2, T) - T/2) <= Tp/2 => A; 0.0 )";
        BrainUtils.createFunction(rectangularPulseFunctionName, rectangularPulseFunctionFormula,
        		diagram, point);
        
        String uExcFormula = "piecewise( (time > initialTimeRest) && (time < excTimeMax) => pulse(time - initialTimeRest - excDuration/2, excStrength, excPeriod, excDuration); 0.0 )";
        BrainUtils.createEquation("equation_u_exc", "u_exc", uExcFormula,
    			Equation.TYPE_SCALAR, diagram, new Point(point.x, point.y + 80));
        
        BrainUtils.setInitialValue(diagram, "initialTimeRest", initialTimeRest);
        BrainUtils.setInitialValue(diagram, "excStrength", excStrength);
        BrainUtils.setInitialValue(diagram, "excPeriod", excPeriod);
        BrainUtils.setInitialValue(diagram, "excDuration", excDuration);
        BrainUtils.setInitialValue(diagram, "excTimeMax", excTimeMax);
    }
    
	/*
	 * method creates pulse train excitation signal with given parameters using events
	 * note that the diagram has a limit on the number of events
	 * but excitation signal can be set using cellular level models.
	 */
    private static void setExcitationSignalByEvents(Diagram diagram, double initialTimeRest, double excStrength, double excDutyCycle, double excPeriod, double excTimeMax, Point point) throws Exception
    {
        // excitation signal switcher
        String excFormula = "k_exc*power_exc";
        BrainUtils.createEquation("u_exc_switcher", "u_exc", excFormula,
    			Equation.TYPE_SCALAR, diagram, point);
        point.translate(0, 80);
    	
        Assignment[] assignmentExcitation = new Assignment[] {new Assignment("k_exc", "1.0")};
        Assignment[] assignmentRest = new Assignment[] {new Assignment("k_exc", "0.0")};
        
        BrainUtils.setInitialValue(diagram, "power_exc", excStrength);
        
        double excDuration = excPeriod * excDutyCycle;
        for (double t = initialTimeRest; t <= excTimeMax; t += excPeriod)
        {
            BrainUtils.createEvent("exc_signal_event_start", "time>=" + String.valueOf(t), assignmentExcitation,
        			diagram, point);
        	BrainUtils.createEvent("exc_signal_event_stop", "time>=" + String.valueOf(t + excDuration), assignmentRest,
        			diagram, new Point(point.x + 200, point.y));
        	point.translate(0, 60);
        }
    }
   
    private static void setPlotsRossler(Diagram diagram) 
    {	
    	EModel emodel = diagram.getRole(EModel.class);
    	PlotsInfo plotsInfo = new PlotsInfo(emodel);
    	
        PlotInfo varPlotsExcitationSignal = new PlotInfo();
        varPlotsExcitationSignal.setTitle("Excitation signal");
        
        PlotInfo varPlotsSynapticStrength = new PlotInfo();
        varPlotsSynapticStrength.setTitle("Synaptic strength");
        
        PlotInfo varPlotsEegRecording = new PlotInfo();
        varPlotsEegRecording.setTitle("EEG Recording");
        
        plotsInfo.setPlots( new PlotInfo[] {varPlotsExcitationSignal, varPlotsSynapticStrength, varPlotsEegRecording});

        List<Curve> curvesExcitationSignal = new ArrayList<Curve>();
        curvesExcitationSignal.add(new Curve("", "u_exc", "u_exc", emodel));
        
        List<Curve> curvesSynapticStrength = new ArrayList<Curve>();
        curvesSynapticStrength.add(new Curve("", "eta", "eta", emodel));
        
        List<Curve> curvesEegRecording = new ArrayList<Curve>();
        curvesEegRecording.add(new Curve("", "V_EEG", "V_EEG", emodel));
        
        varPlotsExcitationSignal.setYVariables(curvesExcitationSignal.stream().toArray( Curve[]::new));
        varPlotsSynapticStrength.setYVariables(curvesSynapticStrength.stream().toArray( Curve[]::new));
        varPlotsEegRecording.setYVariables(curvesEegRecording.stream().toArray(Curve[]::new));
        DiagramUtility.setPlotsInfo(diagram, plotsInfo);
    }
    
    private static void setPlotsEpileptor(Diagram diagram, int N, List<Integer> healthyZoneNodesNumbers, List<Integer> propagationZoneNodesNumbers, List<Integer> epileptogenicZoneNodesNumbers) 
    {	
    	EModel emodel = diagram.getRole(EModel.class);
    	PlotsInfo plotsInfo = new PlotsInfo(emodel);
    	
    	Pen penBlack = new Pen(1, Color.black);
    	Pen penRed = new Pen(1, Color.red);
    	Pen penYellow = new Pen(1, Color.yellow);
    	Pen pen = new Pen();
    	
        PlotInfo varPlotsFirstHalf = new PlotInfo();
        varPlotsFirstHalf.setTitle("Regions 1-" + (int)Math.floor(N / 2));
        
        PlotInfo varPlotsSecondHalf = new PlotInfo();
        varPlotsSecondHalf.setTitle("Regions " + (int)Math.floor(N / 2 + 1) + "-" + N);
        
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
        
        List<String> varsToPlot = new ArrayList<String>();
        for (int i = 1; i <= N; i++) 
        {
        	varsToPlot.add("LFP_" + String.valueOf(i));
        }

        for (int i = 0; i < varsToPlot.size(); i++)
        {
        	String var = varsToPlot.get(i);
        	Curve curve = new Curve("", var, var, emodel);
        	
        	if (healthyZoneNodesNumbers.contains(i + 1))
        	{
        		pen = penBlack;
        	}
        	else if (propagationZoneNodesNumbers.contains(i + 1))
        	{
        		pen = penRed;
        	}
        	else if (epileptogenicZoneNodesNumbers.contains(i + 1))
        	{
        		pen = penYellow;
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
        
        varPlotsFirstHalf.setYVariables(curvesFirstHalf.stream().toArray(Curve[]::new));
        varPlotsSecondHalf.setYVariables(curvesSecondHalf.stream().toArray(Curve[]::new));
        DiagramUtility.setPlotsInfo(diagram, plotsInfo);
    }
}