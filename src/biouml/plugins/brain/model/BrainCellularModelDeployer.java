package biouml.plugins.brain.model;

import java.awt.Color;
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
import biouml.plugins.brain.diagram.BrainCellularModel;
import biouml.plugins.brain.diagram.BrainDiagramType;
import biouml.plugins.brain.diagram.BrainType;
import biouml.plugins.brain.diagram.BrainUtils;
import biouml.plugins.brain.model.cellular.Epileptor2CellularModelProperties;
import biouml.plugins.brain.model.cellular.Epileptor2OxygenCellularModelProperties;
import biouml.plugins.brain.model.cellular.MinimalCellularModelProperties;
import biouml.plugins.brain.model.cellular.OxygenCellularModelProperties;
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
 * Equations deployer for cellular brain models.
 */
public class BrainCellularModelDeployer
{
	public static @Nonnull Diagram deployBrainCellularModel(Diagram src, String name) throws Exception
	{
	    return deployBrainCellularModel(src, name, true);
	}

    public static @Nonnull Diagram deployBrainCellularModel(Diagram src, String name, boolean needLayout) throws Exception
    {
    	BrainCellularModel cellularModel = BrainUtils.getCellularModelNodes(src).get(0).getRole(BrainCellularModel.class);
    	String cellularModelType = cellularModel.getCellularModelType();
    	switch (cellularModelType)
    	{
    	    case BrainType.TYPE_CELLULAR_EPILEPTOR2:
    	    	return fillDiagramWithEquationsEpileptor2(src, name);
    	    case BrainType.TYPE_CELLULAR_OXYGEN:
    	    	return fillDiagramWithEquationsOxygen(src, name);
    	    case BrainType.TYPE_CELLULAR_MINIMAL:
    	    	return fillDiagramWithEquationsMinimal(src, name);
    	    case BrainType.TYPE_CELLULAR_EPILEPTOR2_WITH_OXYGEN:
    	    	return fillDiagramWithEquationsEpileptor2Oxygen(src, name);
    	    default:
    	    	return (new BrainDiagramType()).createDiagram(null, null, null);
    	}
    }
    
    public static Diagram fillDiagramWithEquationsEpileptor2(Diagram diagram, String name) throws Exception
    {
    	String dName;
    	if (name.isEmpty())
    	{
    		String prefix = diagram.getName();
            String suffix = "cellular_epileptor2_equations";
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

        BrainCellularModel cellularModel = BrainUtils.getCellularModelNodes(diagram).get(0).getRole(BrainCellularModel.class);
    	Epileptor2CellularModelProperties cellularModelProperties = (Epileptor2CellularModelProperties)cellularModel.getCellularModelProperties();
        
        int yGroupOffset = 120;
        int yLocalOffset = 60;
        Point point = new Point(0, 0);
        
        // extracellular K dynamic
        String KoFormula = "(K_bath - K_o)/tau_K - 2*gamma*I_pump + dK_o*FR";
        BrainUtils.createEquation("Extracellular_K", "K_o", KoFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // intracellular Na dynamic
        String NaiFormula = "(Na_i0 - Na_i)/tau_Na - 3*I_pump + dNa_i*FR";
        BrainUtils.createEquation("Intracellular_Na", "Na_i", NaiFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // membrane depolarization dynamic, tau_M = C / gL
        //String VFormula = "1000.0*((-V + u)/tau_M)";
        String VFormula = "(-V + u)/tau_M";
        BrainUtils.createEquation("Membrane_depolarization", "V", VFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // synaptic resource dynamic
        String SRFormula = "(1.0 - SR)/tau_D - SF*dSR*SR*FR";
        BrainUtils.createEquation("Synaptic_resource", "SR", SRFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // firing rate dynamic
        String frFormula = "piecewise( V > V_th => FR_max*(2.0/(1.0 + exp(-2.0*(V - V_th)/k_FR)) - 1.0); 0.0 )";
        BrainUtils.createEquation("Firing_rate", "FR", frFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yGroupOffset);
        
        // Input current dynamic
        String uFormula = "g_K_l*(V_K - V_K0) + SF*G_syn*FR*(SR - 0.5) + SF*sigma*noise(1.0)/sqrt(1000.0)";
        BrainUtils.createEquation("Input_current", "u", uFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // Reversal potential of K formula
        String vKFormula = "26.6*ln(K_o/130.0)";
        BrainUtils.createEquation("Reversal_potential", "V_K", vKFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // Na/K pump activity
        String iPumpFormula = "ro/((1.0 + exp(3.5 - K_o))*(1.0 + exp((25.0 - Na_i)/3.0)))";
        BrainUtils.createEquation("Na_K_pump", "I_pump", iPumpFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yGroupOffset);
        
        // seizures start when level of K in external bath increase
        point = new Point(500, 0);
        BrainUtils.createEvent("seizures_start", "time>time_start", new Assignment[] {new Assignment("K_bath", "8.5")},
    			diagramEq, point);
        point.translate(0, yGroupOffset);
        
    	String neuronObserverType = cellularModelProperties.getNeuronObserverType();
        if (neuronObserverType.equals(Epileptor2CellularModelProperties.OBSERVER_QIF))
        {
        	String UFormula = "(1.0/C_U) * (g_U*(U - U_rest)*(U - U_th) + g_L*u)";
            BrainUtils.createEquation("Membrane_potential", "U", UFormula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset);
            
            BrainUtils.createEvent("Max_membrane_potential_reached", "U>U_peak", new Assignment[] {new Assignment("U", "U_reset")},
        			diagramEq, point);
            point.translate(0, yGroupOffset);
        }
        else if (neuronObserverType.equals(Epileptor2CellularModelProperties.OBSERVER_AQIF))
        {
        	String UFormula = "(1.0/C_U) * (g_U*(U - U_rest)*(U - U_th) - w + g_L*u + I_a)";
            BrainUtils.createEquation("Membrane_potential", "U", UFormula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset);
            
           	String wFormula = "-w/tau_w";
            BrainUtils.createEquation("Adaptation_current", "w", wFormula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset);
            
            BrainUtils.createEvent("max_membrane_potential_reached", "U>U_peak", new Assignment[] {new Assignment("U", "U_reset"), new Assignment("w", "w + dw")},
        			diagramEq, point);
            point.translate(0, yGroupOffset);
        }
        
        boolean portsFlag = cellularModelProperties.getPortsFlag();
        if (portsFlag)
        {           	
            // variable for use in composite diagram as excitation signal
        	//String nuNormFormula = "(nu/100.0)*4.0"; //too low. Cellular signal may not cause regional seizure.
            //String nuNormFormula = "(nu/100.0)*8.0"; //too much. Even noise can trigger regional seizure. 
        	String frNormFormula = "(FR/100.0)*5.0"; //seems OK.

            BrainUtils.createEquation("FR_norm", "FR_norm", frNormFormula,
                    Equation.TYPE_SCALAR, diagramEq, point);
            BrainUtils.createPort("FR_norm", "FR_norm", Type.TYPE_OUTPUT_CONNECTION_PORT,
            		diagramEq, new Point(point.x + 150, point.y));
            point.translate(0, yLocalOffset - 10);

            BrainUtils.createPort("low_excitability_flag", "low_excitability_flag", Type.TYPE_OUTPUT_CONNECTION_PORT,
            		diagramEq, point);
            point.translate(0, yLocalOffset - 20);
            
            BrainUtils.createPort("SR", "SR", Type.TYPE_OUTPUT_CONNECTION_PORT,
            		diagramEq, point);
            point.translate(0, yLocalOffset - 20);
            
            BrainUtils.createPort("SF", "SF", Type.TYPE_INPUT_CONNECTION_PORT,
            		diagramEq, point);
            point.translate(0, yLocalOffset - 20);
               
            BrainUtils.createPort("x1", "x1", Type.TYPE_INPUT_CONNECTION_PORT,
            		diagramEq, point);
            point.translate(0, yLocalOffset);
          
            BrainUtils.createEvent("healthy_regional_seizure_started", "epileptogenicity_flag == 0.0 && x1 > -1.0", new Assignment[] {new Assignment("K_bath", "8.5")},
        			diagramEq, new Point(point.x, point.y));
            point.translate(0, yLocalOffset + 20);
            BrainUtils.createEvent("healthy_regional_seizure_ended", "epileptogenicity_flag == 0.0 && x1 <= -1.0", new Assignment[] {new Assignment("K_bath", "3.0")},
        			diagramEq, new Point(point.x, point.y));
            point.translate(0, yLocalOffset + 20);
            
            BrainUtils.createEvent("healthy_K_conductance_increased_by_default", "epileptogenicity_flag == 0.0 && K_o < 7.0", new Assignment[] {new Assignment("tau_K", "2.5")},
        			diagramEq, new Point(point.x, point.y));
            point.translate(0, yLocalOffset + 20);
            BrainUtils.createEvent("helathy_K_conductance_decreased_during_seizure", "epileptogenicity_flag == 0.0 && K_o >= 7.0", new Assignment[] {new Assignment("tau_K", "100.0")},
        			diagramEq, new Point(point.x, point.y));
            point.translate(0, yLocalOffset + 20);

            BrainUtils.createEvent("healthy_low_excitability", "epileptogenicity_flag == 0.0 && SF <= 0.9", new Assignment[] {new Assignment("low_excitability_flag", "1.0")},
        			diagramEq, new Point(point.x, point.y));
            point.translate(0, yLocalOffset + 20);
            BrainUtils.createEvent("epileptogenic_low_excitability", "epileptogenicity_flag == 1.0 && SF <= 1.05", new Assignment[] {new Assignment("low_excitability_flag", "1.0"), new Assignment("K_bath", "3.0")},
        			diagramEq, new Point(point.x, point.y)); 
            point.translate(0, yLocalOffset + 20);
        }
        
        setInitialValuesEpileptor2(diagramEq, cellularModelProperties);
        
        //Simulator settings
        JavaSdeSimulationEngine se = new JavaSdeSimulationEngine();
        se.setDiagram(diagramEq);
        se.setCompletionTime(2000.0);
        se.setTimeIncrement(0.1);
        se.setSolver(new EulerStochastic());
        diagramEq.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, se));
        
        setPlotsEpileptor2(diagramEq);
        
      	return diagramEq;
    }
    
    public static Diagram fillDiagramWithEquationsOxygen(Diagram diagram, String name) throws Exception
    {
    	String dName;
    	if (name.isEmpty())
    	{
    		String prefix = diagram.getName();
    		String suffix = "cellular_oxygen_equations";
            dName = suffix.isEmpty() ? prefix : prefix + "_" + suffix;
    	}
    	else
    	{
    		dName = name;
    	}
        Diagram diagramEq = (new BrainDiagramType()).createDiagram(diagram.getOrigin(), dName, new DiagramInfo(dName));
        
        if (diagram.getComment() != null)
        {
            diagramEq.setComment(diagram.getComment());
        }
    	
        BrainCellularModel cellularModel = BrainUtils.getCellularModelNodes(diagram).get(0).getRole(BrainCellularModel.class);
    	OxygenCellularModelProperties cellularModelProperties = (OxygenCellularModelProperties)cellularModel.getCellularModelProperties();
        
        int yGroupOffset = 120;
        int yLocalOffset = 60;
        Point point = new Point(0, 0);
        
        // membrane potential dynamic
        String VFormula = "1.0/C*(I_ext - I_Na - I_K - I_Cl)";
        BrainUtils.createEquation("Membrane_potential", "V", VFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // extracellular K dynamic
        String KoFormula = "0.001*(gamma*beta*I_K - 2.0*beta*I_pump - I_diff - I_glia - 2.0*I_gliapump)";
        BrainUtils.createEquation("Extracellular_K", "K_o", KoFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // intracellular Na dynamic
        String NaiFormula = "0.001*(-gamma*I_Na - 3.0*I_pump)";
        BrainUtils.createEquation("Intracellular_Na", "Na_i", NaiFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // oxygen dynamic
        String O2Formula = "0.001*(-alpha*lambda*(I_pump + I_gliapump) + eps_O*(O2_bath - O2_o))";
        BrainUtils.createEquation("oxygen", "O2_o", O2Formula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // extracellular Na dynamic
        String NaoFormula = "144.0 - beta*(Na_i - 18.0)";
        BrainUtils.createEquation("Extracellular_Na", "Na_o", NaoFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        
        // intracellular K dynamic
        String KiFormula = "140.0 + (18.0 - Na_i)";
        BrainUtils.createEquation("Intracellular_K", "K_i", KiFormula,
                Equation.TYPE_SCALAR, diagramEq, new Point(point.x + 200, point.y));
        point.translate(0, yLocalOffset);
        
        // Na current dynamic
        String INaFormula = "g_Na*m^3*h*(V - E_Na) + g_NaL*(V - E_Na)";
        BrainUtils.createEquation("Na_current", "I_Na", INaFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // K current dynamic
        String IKFormula = "g_K*n^4*(V - E_K) + g_KL*(V - E_K)";
        BrainUtils.createEquation("K_current", "I_K", IKFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // Cl current dynamic
        String IClFormula = "g_ClL*(V - E_Cl)";
        BrainUtils.createEquation("Cl_current", "I_Cl", IClFormula,
                Equation.TYPE_SCALAR, diagramEq,  point);
        point.translate(0, yLocalOffset);
        
        // Na/K pump activity
        String IPumpFormula = "ro/((1.0 + exp((25.0 - Na_i)/3.0))*(1.0 + exp(5.5 - K_o)))";
        BrainUtils.createEquation("Na_K_pump", "I_pump", IPumpFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        
        // pump rate dynamic
        String roFormula = "ro_max/(1.0 + exp((20.0 - O2_o)/3.0))";
        BrainUtils.createEquation("pump_rate", "ro", roFormula,
                Equation.TYPE_SCALAR, diagramEq, new Point(point.x + 350, point.y));
        point.translate(0, yLocalOffset + 30);
        
        // glial Na/K pump activity
        String IGliaPumpFormula = "1.0/3.0*(ro/((1.0 + exp((25.0 - Na_gi)/3.0))*(1.0 + exp(5.5 - K_o))))";
        BrainUtils.createEquation("Na_K_glial_pump", "I_gliapump", IGliaPumpFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset + 30);
        
        // glial uptake dynamic
        String IGliaFormula = "G_glia/(1.0 + exp((18.0 - K_o)/2.5))";
        BrainUtils.createEquation("Glial_uptake", "I_glia", IGliaFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset + 30);
        
        // lateral diffusion dynamic
        String IDiffFormula = "eps_K*(K_o - K_bath)";
        BrainUtils.createEquation("Lateral_diffusion", "I_diff", IDiffFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset + 30);
        
        point = new Point(point.x + 600, 0);
        
        // Na activation particles
        String mFormula = "alpha_m*(1.0 - m) - beta_m*m";
        BrainUtils.createEquation("Na_activation", "m", mFormula,
                Equation.TYPE_RATE, diagramEq, point);
        
        // Na reversal potential
        String ENaFormula = "26.64*ln(Na_o/Na_i)";
        BrainUtils.createEquation("Na_reversal_potential", "E_Na", ENaFormula,
                Equation.TYPE_SCALAR, diagramEq, new Point(point.x + 250, point.y));
        point.translate(0, yLocalOffset);
        
        // Na inactivation particles
        String hFormula = "alpha_h*(1.0 - h) - beta_h*h";
        BrainUtils.createEquation("Na_inactivation", "h", hFormula,
                Equation.TYPE_RATE, diagramEq, point);
        
        // K reversal potential
        String EKFormula = "26.64*ln(K_o/K_i)";
        BrainUtils.createEquation("K_reversal_potential", "E_K", EKFormula,
                Equation.TYPE_SCALAR, diagramEq, new Point(point.x + 250, point.y));
        point.translate(0, yLocalOffset);
        
        // K activation particles
        String nFormula = "alpha_n*(1.0 - n) - beta_n*n";
        BrainUtils.createEquation("K_activation", "n", nFormula,
                Equation.TYPE_RATE, diagramEq, point);
        
        // Cl reversal potential
        String EClFormula = "26.64*ln(Cl_i/Cl_o)";
        BrainUtils.createEquation("Cl_reversal_potential", "E_Cl", EClFormula,
                Equation.TYPE_SCALAR, diagramEq, new Point(point.x + 250, point.y));
        point.translate(0, yLocalOffset);
        
        
        // opening and closing rate constants of the ion channel state transitions
        String alphaMFormula = "0.32*(54.0 + V)/(1.0 - exp(-(V + 54.0)/4.0))";
        BrainUtils.createEquation("alpham", "alpha_m", alphaMFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String betaMFormula = "0.28*(V + 27.0)/(exp((V + 27.0)/5.0) - 1.0)";
        BrainUtils.createEquation("betam", "beta_m", betaMFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String alphaHFormula = "0.128*exp(-(50.0 + V)/18)";
        BrainUtils.createEquation("alphah", "alpha_h", alphaHFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String betaHFormula = "4.0/(1.0 + exp(-(V + 27.0)/5.0))";
        BrainUtils.createEquation("betah", "beta_h", betaHFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String alphaNFormula = "0.032*(V + 52.0)/(1.0 - exp(-(V + 52.0)/5.0))";
        BrainUtils.createEquation("alphan", "alpha_n", alphaNFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String betaNFormula = "0.5*exp(-(57.0 + V)/40.0)";
        BrainUtils.createEquation("betan", "beta_n", betaNFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // seizures start when level of K in external bath increase
        point = new Point(1200, 0);
        BrainUtils.createEvent("seizures_start", "time>time_start", new Assignment[] {new Assignment( "K_bath", "8.5" )},
    			diagramEq, point);
        point.translate(0, yGroupOffset);
        
        String O2RatePumpFormula = "alpha*lambda*(I_pump + I_gliapump)";
        BrainUtils.createEquation("O2_rate_by_pump", "O2_rate_pump", O2RatePumpFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String O2RateBathFormula = "eps_O*(O2_bath - O2_o)";
        BrainUtils.createEquation("O2_rate_from_bath", "O2_rate_bath", O2RateBathFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        
        setInitialValuesOxygen(diagramEq, cellularModelProperties);
        
        //Simulator settings
        JavaSimulationEngine se = new JavaSimulationEngine();
        se.setDiagram(diagramEq);
        se.setCompletionTime(100000.0);
        se.setTimeIncrement(1.0);
        se.setSolver(new JVodeSolver());
        diagramEq.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, se));
        
        setPlotsOxygen(diagramEq);
        
      	return diagramEq;
    }
    
    public static Diagram fillDiagramWithEquationsMinimal(Diagram diagram, String name) throws Exception
    {
    	String dName;
    	if (name.isEmpty())
    	{
    		String prefix = diagram.getName();
            String suffix = "cellular_minimal_equations";
            dName = suffix.isEmpty() ? prefix : prefix + "_" + suffix;
    	}
    	else
    	{
    		dName = name;
    	}
        Diagram diagramEq = (new BrainDiagramType()).createDiagram(diagram.getOrigin(), dName, new DiagramInfo(dName));

        if (diagram.getComment() != null)
        {
            diagramEq.setComment(diagram.getComment());
        }
    	
        BrainCellularModel cellularModel = BrainUtils.getCellularModelNodes(diagram).get(0).getRole(BrainCellularModel.class);
    	MinimalCellularModelProperties cellularModelProperties = (MinimalCellularModelProperties)cellularModel.getCellularModelProperties();
        
        int yGroupOffset = 120;
        int yLocalOffset = 60;
        Point point = new Point(1200, 0);
        
        // n_inf formula
        String nInfFuncName = "n_inf";
        String nInfFuncFormula = "function n_inf(V) = 1.0/(1.0 + exp((-19.0 - V)/18.0))";
        BrainUtils.createFunction(nInfFuncName, nInfFuncFormula,
        		diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // m_inf formula
        String mInfFuncName = "m_inf";
        String mInfFuncFormula = "function m_inf(V) = 1.0/(1.0 + exp((-24.0 - V)/12.0))";
        BrainUtils.createFunction(mInfFuncName, mInfFuncFormula,
        		diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // n formula
        String hFuncName = "h";
        String hFuncFormula = "function h(n) = 1.1 - 1.0/(1.0 + exp(-8.0*(n - 0.4)))";
        BrainUtils.createFunction(hFuncName, hFuncFormula,
        		diagramEq, point);
        point.translate(0, yLocalOffset);
        
        point = new Point(0, 0);
        
        // membrane potential dynamic
        String VFormula = "-(1.0/C_m)*(I_Cl + I_Na + I_K + I_pump)";
        BrainUtils.createEquation("Membrane_potential", "V", VFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // K conductance gating variable dynamic
        String nFormula = "(n_inf(V) - n)/tau_n";
        BrainUtils.createEquation("K_gating_vatiable", "n", nFormula,
                Equation.TYPE_RATE, diagramEq, point);
        
        // K conductance gating variable initial assignment
        String ninitFormula = "n_inf(-78.0)";
        BrainUtils.createEquation("n_initial", "n", ninitFormula,
                Equation.TYPE_INITIAL_ASSIGNMENT, diagramEq, new Point(point.x + 200, point.y));
        point.translate(0, yLocalOffset);
        
        // intracellular K variation dynamic (change from initial)
        String DKiFormula = "-gamma/omega_i*(I_K - 2.0*I_pump)";
        BrainUtils.createEquation("Intracellular_K_variation", "DK_i", DKiFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // extracellular K buffering by external bath
        String KgFormula = "eps*(K_bath - K_o)";
        BrainUtils.createEquation("K_buffering", "K_g", KgFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yGroupOffset);
        
        // intracellular Na variation
        String DNaiFormula = "-DK_i";
        BrainUtils.createEquation("Nai_variation", "DNa_i", DNaiFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 30);
        
        // extracellular Na variation
        String DNaoFormula = "-beta*DNa_i";
        BrainUtils.createEquation("Nao_variation", "DNa_o", DNaoFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 30);
        
        // extracellular K variation
        String DKoFormula = "-beta*DK_i";
        BrainUtils.createEquation("Ko_variation", "DK_o", DKoFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 30);
        
        // intracellular K
        String KiFormula = "K_i0 + DK_i";
        BrainUtils.createEquation("Intracellular_K", "K_i", KiFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 30);
        
        // intracellular Na
        String NaiFormula = "Na_i0 + DNa_i";
        BrainUtils.createEquation("Intracellular_Na", "Na_i", NaiFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 30);
        
        // extracellular Na
        String NaoFormula = "Na_o0 + DNa_o";
        BrainUtils.createEquation("Extracellular_Na", "Na_o", NaoFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 30);
        
        // extracellular K
        String KoFormula = "K_o0 + DK_o + K_g";
        BrainUtils.createEquation("Extracellular_K", "K_o", KoFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 30);

        point = new Point(600, 0);
        
        // Na current
        String INaFormula = "(g_Na_l + g_Na*m_inf(V)*h(n))*(V - 26.64*ln(Na_o/Na_i))";
        BrainUtils.createEquation("Na_current", "I_Na", INaFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // K current
        String IKFormula = "(g_K_l + g_K*n)*(V - 26.64*ln(K_o/K_i))";
        BrainUtils.createEquation("K_current", "I_K", IKFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // Cl current
        String IClFormula = "g_Cl*(V + 26.64*ln(Cl_o/Cl_i))";
        BrainUtils.createEquation("Cl_current", "I_Cl", IClFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
      
        // Na/K pump activity
        String IpumpFormula = "ro*(1.0/(1.0 + exp((21.0 - Na_i)/2.0)))*(1.0/(1.0 + exp(5.5 - K_o)))";
        BrainUtils.createEquation("Na_K_pump", "I_pump", IpumpFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        
        setInitialValuesMinimal(diagramEq, cellularModelProperties);
        
        //Simulator settings
        JavaSimulationEngine se = new JavaSimulationEngine();
        se.setDiagram(diagramEq);
        se.setCompletionTime(4000.0);
        se.setTimeIncrement(0.1);
        se.setSolver(new JVodeSolver());
        diagramEq.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, se));
        
        setPlotsMinimal(diagramEq);
        
      	return diagramEq;
    }
    
    public static Diagram fillDiagramWithEquationsEpileptor2Oxygen(Diagram diagram, String name) throws Exception
    {
    	String dName;
    	if (name.isEmpty())
    	{
    		String prefix = diagram.getName();
            String suffix = "cellular_epileptor2+oxygen_equations";
            dName = suffix.isEmpty() ? prefix : prefix + "_" + suffix;
    	}
    	else
    	{
    		dName = name;
    	}
        Diagram diagramEq = (new BrainDiagramType()).createDiagram(diagram.getOrigin(), dName, new DiagramInfo(dName));
        
        if (diagram.getComment() != null)
        {
            diagramEq.setComment(diagram.getComment());
        }
    	
        BrainCellularModel cellularModel = BrainUtils.getCellularModelNodes(diagram).get(0).getRole(BrainCellularModel.class);
    	Epileptor2OxygenCellularModelProperties cellularModelProperties = (Epileptor2OxygenCellularModelProperties)cellularModel.getCellularModelProperties();
    	
        int yGroupOffset = 120;
        int yLocalOffset = 60;
        Point point = new Point(0, 0);
        
        // extracellular K dynamic
        String KoFormula = "(K_bath - K_o)/tau_K - 2*gamma*I_pump + dK_o*FR";
        BrainUtils.createEquation("Extracellular_K", "K_o", KoFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // intracellular Na dynamic
        String NaiFormula = "(Na_i0 - Na_i)/tau_Na - 3*I_pump + dNa_i*FR";
        BrainUtils.createEquation("Intracellular_Na", "Na_i", NaiFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // membrane depolarization dynamic
        String VFormula = "(-V + u)/tau_M";
        BrainUtils.createEquation("Membrane_depolarization", "V", VFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // synaptic resource dynamic
        String SRFormula = "(1.0 - SR)/tau_D - SF*dSR*SR*FR";
        BrainUtils.createEquation("Synaptic_resource", "SR", SRFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // firing rate dynamic
        String FRFormula = "piecewise( V > V_th => FR_max*(2.0/(1.0 + exp(-2.0*(V - V_th)/k_FR)) - 1.0); 0.0 )";
        BrainUtils.createEquation("Firing_rate", "FR", FRFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yGroupOffset);
        
        // Input current dynamic
        String uFormula = "g_K_l*(V_K - V_K0) + SF*G_syn*FR*(SR - 0.5) + SF*sigma*noise(1.0)/sqrt(1000.0)";
        BrainUtils.createEquation("Input_current", "u", uFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // Reversal potential of K formula
        String VKFormula = "26.6*ln(K_o/130.0)";
        BrainUtils.createEquation("Reversal_potential", "V_K", VKFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // seizures start when level of K in external bath increase
        point = new Point(500, 0);
        BrainUtils.createEvent("epileptogenic_seizures_started", "time>time_start", new Assignment[] {new Assignment("K_bath", "8.5")},
    			diagramEq, point);
        point.translate(0, yLocalOffset + 50);
        
        // extracellular O2 dynamic
        String O2oFormula = "-alpha*lambda*(I_pump + I_gliapump) + eps_O*(O2_bath - O2_o)";
        BrainUtils.createEquation("Extracellular_O2", "O2_o", O2oFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        // Na/K pump activity
        String IPumpFormula = "ro/((1.0 + exp(3.5 - K_o))*(1.0 + exp((25.0 - Na_i)/3.0)))";
        BrainUtils.createEquation("Na_K_pump", "I_pump", IPumpFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        
        // pump rate dynamic
        String roFormula = "ro_max/(1.0 + exp((20.0 - O2_o)/3.0))";
        BrainUtils.createEquation("pump_rate", "ro", roFormula,
                Equation.TYPE_SCALAR, diagramEq, new Point(point.x + 400, point.y));
        point.translate(0, yLocalOffset + 30);
        
        // Na/K glia pump activity
        String IGliaPumpFormula = "1.0/3.0*(ro/((1.0 + exp(3.5 - K_o))*(1.0 + exp((25.0 - Na_gi)/3.0))))";
        BrainUtils.createEquation("Na_K_glial_pump", "I_gliapump", IGliaPumpFormula,
                Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yGroupOffset);
        
    	String neuronObserverType = cellularModelProperties.getNeuronObserverType();
        if (neuronObserverType.equals(Epileptor2CellularModelProperties.OBSERVER_QIF))
        {
        	String UFormula = "(1.0/C_U) * (g_U*(U - U_rest)*(U - U_th) + g_L*u)";
            BrainUtils.createEquation("Membrane_potential", "U", UFormula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset);
            
            BrainUtils.createEvent("Max_membrane_potential_reached", "U>U_peak", new Assignment[] {new Assignment("U", "U_reset")},
        			diagramEq, point);
            point.translate(0, yGroupOffset);
        }
        else if (neuronObserverType.equals(Epileptor2CellularModelProperties.OBSERVER_AQIF))
        {
        	String UFormula = "(1.0/C_U) * (g_U*(U - U_rest)*(U - U_th) - w + g_L*u + I_a)";
            BrainUtils.createEquation("Membrane_potential", "U", UFormula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset);
            
           	String wFormula = "-w/tau_w";
            BrainUtils.createEquation("Adaptation_current", "w", wFormula,
                    Equation.TYPE_RATE, diagramEq, point);
            point.translate(0, yLocalOffset);
            
            BrainUtils.createEvent("max_membrane_potential_reached", "U>U_peak", new Assignment[] {new Assignment("U", "U_reset"), new Assignment("w", "w + dw")},
        			diagramEq, point);
            point.translate(0, yGroupOffset);
        }
        
        boolean portsFlag = cellularModelProperties.getPortsFlag();
        if (portsFlag)
        {          
        	String frNormFormula = "(FR/100.0)*A_exc";
        	//String frNormFormula = "(FR/100.0)*A_exc*SF";
        	
            BrainUtils.createEquation("FR_norm", "FR_norm", frNormFormula,
                    Equation.TYPE_SCALAR, diagramEq, point);
            BrainUtils.createPort("FR_norm", "FR_norm", Type.TYPE_OUTPUT_CONNECTION_PORT,
            		diagramEq, new Point(point.x + 170, point.y));
            point.translate(0, yLocalOffset - 10);
            
            BrainUtils.createPort("SR", "SR", Type.TYPE_OUTPUT_CONNECTION_PORT,
            		diagramEq, point);
            point.translate(0, yLocalOffset - 20);
            
            BrainUtils.createPort("SF", "SF", Type.TYPE_INPUT_CONNECTION_PORT,
            		diagramEq, point);
            point.translate(0, yLocalOffset - 20);
               
            BrainUtils.createPort("x1", "x1", Type.TYPE_INPUT_CONNECTION_PORT,
            		diagramEq, point);
            point.translate(0, yLocalOffset);
            
            String interpolationFormula = "function interpolate(x1, x2, y1, y2, x) = (y2 - y1)*((x - x1)/(x2 - x1)) + y1";
            BrainUtils.createFunction("interpolate", interpolationFormula,
            		diagramEq, point);
            point.translate(0, yLocalOffset);
            
//            String sfNormFormula = "piecewise( SF > 0.85 => 1.0; "
//            		+ "interpolate(0.75, 0.85, 0.0, 1.0, SF) )";
//            String sfNormFormula = "piecewise( SF > 0.8 => 1.0; "
//            		+ "interpolate(0.7, 0.8, 0.0, 1.0, SF) )";
//            String sfNormFormula = "piecewise( SF > 0.825 => 1.0; "
//            		+ "interpolate(0.75, 0.825, 0.0, 1.0, SF) )";
            String sfNormFormula = "piecewise( SF > 0.85 => 1.0; "
            		+ "interpolate(0.8, 0.85, 0.0, 1.0, SF) )";
            
//            String sfNormFormula = "piecewise( SF > 0.9 => 1.0; "
//            		+ "interpolate(0.8, 0.9, 0.0, 1.0, SF) )";
//            String sfNormFormula = "piecewise( SF > 1.0 => 1.0; "
//            		+ "interpolate(0.8, 1.0, 0.0, 1.0, SF) )";
            BrainUtils.createEquation("SF_norm", "SF_norm", sfNormFormula,
                    Equation.TYPE_SCALAR, diagramEq, point);
            point.translate(0, yLocalOffset);
            
            BrainUtils.createPort("SF_norm", "SF_norm", Type.TYPE_OUTPUT_CONNECTION_PORT,
            		diagramEq, point);
            point.translate(0, yLocalOffset);
          
            BrainUtils.createEvent("healthy_regional_seizure_started", "epileptogenicity_flag == 0.0 && x1 > -1.0", new Assignment[] {new Assignment("K_bath", "8.5")},
        			diagramEq, new Point(point.x, point.y));
            point.translate(0, yLocalOffset + 20);
            BrainUtils.createEvent("healthy_regional_seizure_ended", "epileptogenicity_flag == 0.0 && x1 <= -1.0", new Assignment[] {new Assignment("K_bath", "3.0")},
        			diagramEq, new Point(point.x, point.y));
            point.translate(0, yLocalOffset + 20);
            
//            BrainUtils.createEvent("healthy_K_conductance_increased_by_default", "epileptogenicity_flag == 0.0 && K_o < 7.0", new Assignment[] {new Assignment("tau_K", "2.5")},
//        			diagramEq, new Point(point.x, point.y));
            
//            BrainUtils.createEvent("healthy_K_conductance_increased_by_default", "epileptogenicity_flag == 0.0 && K_o < 7.0 && x1 <= -1.0", new Assignment[] {new Assignment("tau_K", "2.5")},
//        			diagramEq, new Point(point.x, point.y));
//            point.translate(0, yLocalOffset + 20);
//            BrainUtils.createEvent("helathy_K_conductance_decreased_during_seizure", "epileptogenicity_flag == 0.0 && K_o >= 7.0", new Assignment[] {new Assignment("tau_K", "100.0")},
//        			diagramEq, new Point(point.x, point.y));
//            point.translate(0, yLocalOffset + 20);
            
            BrainUtils.createEvent("healthy_K_conductance_increased_by_default", "epileptogenicity_flag == 0.0 && K_o < 8.0 && x1 <= -1.0", new Assignment[] {new Assignment("tau_K", "2.5")},
        			diagramEq, new Point(point.x, point.y));
            point.translate(0, yLocalOffset + 20);
            BrainUtils.createEvent("helathy_K_conductance_decreased_during_seizure", "epileptogenicity_flag == 0.0 && K_o >= 8.0", new Assignment[] {new Assignment("tau_K", "100.0")},
        			diagramEq, new Point(point.x, point.y));
            point.translate(0, yLocalOffset + 20);
            
            BrainUtils.createEvent("epileptogenic_seizures_ended", "epileptogenicity_flag == 1.0 && time > time_end", new Assignment[] {new Assignment("K_bath", "3.0")},
            		diagramEq, new Point(point.x, point.y)); 
            point.translate(0, yLocalOffset + 20);
        }
       
        setInitialValuesEpileptor2Oxygen(diagramEq, cellularModelProperties);
        
        //Simulator settings
        JavaSdeSimulationEngine se = new JavaSdeSimulationEngine();
        se.setDiagram(diagramEq);
        se.setCompletionTime(2000.0);
        se.setTimeIncrement(0.1);
        se.setSolver(new EulerStochastic());
        diagramEq.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, se));
        
        setPlotsEpileptor2Oxygen(diagramEq);
        
      	return diagramEq;
    }
    
    private static void setInitialValuesEpileptor2(Diagram diagram, Epileptor2CellularModelProperties cellularModelProperties) 
    {	
    	// parameter values
    	double sf = cellularModelProperties.getSf();
    	
    	double tauK = cellularModelProperties.getTauK();
    	double tauNa = cellularModelProperties.getTauNa();
    	double tauM = cellularModelProperties.getTauM();
    	double tauD = cellularModelProperties.getTauD();
    	double dKo = cellularModelProperties.getDKo();
    	double dNai = cellularModelProperties.getDNai();
    	double dSR = cellularModelProperties.getDSR();
    	double gamma = cellularModelProperties.getGamma();
    	double ro = cellularModelProperties.getRo();
    	double vTh = cellularModelProperties.getVTh();
    	double FRMax = cellularModelProperties.getFRMax();
    	double kFR = cellularModelProperties.getKFR();
    	double gKL = cellularModelProperties.getGKL();
    	double gSyn = cellularModelProperties.getGSyn();
    	double sigma = cellularModelProperties.getSigma();
    	
    	// parameters for neuron observer
    	String neuronObserverType = cellularModelProperties.getNeuronObserverType();
        double cU = cellularModelProperties.getCU();
        double gU = cellularModelProperties.getGU();
        double gL = cellularModelProperties.getGL();
        double URest = cellularModelProperties.getURest();
        double UTh = cellularModelProperties.getUTh();
        double UPeak = cellularModelProperties.getUPeak();
        double UReset = cellularModelProperties.getUReset();
        // parameters for adaptation current in aQIF neuron observer
        double iA = 116.0;
        double tauW = 0.2;
        double dw = 100.0;
    	
        // seizures onset time
    	double timeStart = 50.0;
    	
    	BrainUtils.setInitialValue(diagram, "SF", sf);
    	
    	BrainUtils.setInitialValue(diagram, "tau_K", tauK, "s");
    	BrainUtils.setInitialValue(diagram, "tau_Na", tauNa, "s");
    	BrainUtils.setInitialValue(diagram, "tau_M", tauM, "s");
    	BrainUtils.setInitialValue(diagram, "tau_D", tauD);
    	BrainUtils.setInitialValue(diagram, "dK_o", dKo, "mM");
    	BrainUtils.setInitialValue(diagram, "dNa_i", dNai, "mM");
    	BrainUtils.setInitialValue(diagram, "dSR", dSR);
    	BrainUtils.setInitialValue(diagram, "gamma", gamma, "volume ratio");
    	BrainUtils.setInitialValue(diagram, "ro", ro, "mM/s");
    	BrainUtils.setInitialValue(diagram, "V_th", vTh, "mV");
    	BrainUtils.setInitialValue(diagram, "FR_max", FRMax, "Hz");
    	BrainUtils.setInitialValue(diagram, "k_FR", kFR, "mV");
    	BrainUtils.setInitialValue(diagram, "g_K_l", gKL, "nS");
    	BrainUtils.setInitialValue(diagram, "G_syn", gSyn, "mv*s");
    	BrainUtils.setInitialValue(diagram, "sigma", sigma, "mV");
    	
    	BrainUtils.setInitialValue(diagram, "C_U", cU, "nF");
    	BrainUtils.setInitialValue(diagram, "g_U", gU, "nS/mV");
    	BrainUtils.setInitialValue(diagram, "g_L", gL, "nS");
    	BrainUtils.setInitialValue(diagram, "U_rest", URest, "mV");
    	BrainUtils.setInitialValue(diagram, "U_th", UTh, "mV");
    	BrainUtils.setInitialValue(diagram, "U_peak", UPeak, "mV");
    	BrainUtils.setInitialValue(diagram, "U_reset", UReset, "mV");
    	
    	BrainUtils.setInitialValue(diagram, "I_a", iA, "pA");
    	BrainUtils.setInitialValue(diagram, "tau_w", tauW, "s");
    	BrainUtils.setInitialValue(diagram, "dw", dw, "pA");
    	
    	BrainUtils.setInitialValue(diagram, "time_start", timeStart, "s");
    	
        // initial variable values
        double Kbath = 3.0;
        double Ko = 3.0;
        double Nai = 10.0;
        double SR = 1.0;
        double vK = -100.253;
        
        double U = -70.0;
        
    	BrainUtils.setInitialValue(diagram, "K_bath", Kbath, "mM");
    	BrainUtils.setInitialValue(diagram, "K_o", Ko, "mM");
    	BrainUtils.setInitialValue(diagram, "Na_i", Nai, "mM");
    	BrainUtils.setInitialValue(diagram, "Na_i0", Nai, "mM");
    	BrainUtils.setInitialValue(diagram, "SR", SR);
    	BrainUtils.setInitialValue(diagram, "V_K0", vK, "mV");
    	
    	BrainUtils.setInitialValue(diagram, "U", U, "mV");
    }
    
    private static void setInitialValuesOxygen(Diagram diagram, OxygenCellularModelProperties cellularModelProperties) 
    {
    	// parameter values
    	double c = cellularModelProperties.getC();
    	double iExt = cellularModelProperties.getIExt();
    	double gNa = cellularModelProperties.getGNa();
    	double gK = cellularModelProperties.getGK();
    	double gNaL = cellularModelProperties.getGNaL();
    	double gKL = cellularModelProperties.getGKL();
    	double gClL = cellularModelProperties.getGClL();
    	double gamma = cellularModelProperties.getGamma();
    	double beta = cellularModelProperties.getBeta();
    	double roMax = cellularModelProperties.getRoMax();
    	double gGlia = cellularModelProperties.getGGlia();
    	double epsK = cellularModelProperties.getEpsK();
    	double epsO = cellularModelProperties.getEpsO();
    	double alpha = cellularModelProperties.getAlpha();
    	double lambda = cellularModelProperties.getLambda();
    	double timeStart = 10000.0;
    	
    	BrainUtils.setInitialValue(diagram, "C", c);
    	BrainUtils.setInitialValue(diagram, "I_ext", iExt);
    	BrainUtils.setInitialValue(diagram, "g_Na", gNa);
    	BrainUtils.setInitialValue(diagram, "g_K", gK);
    	BrainUtils.setInitialValue(diagram, "g_NaL", gNaL);
    	BrainUtils.setInitialValue(diagram, "g_KL", gKL);
    	BrainUtils.setInitialValue(diagram, "g_ClL", gClL);
    	BrainUtils.setInitialValue(diagram, "gamma", gamma);
    	BrainUtils.setInitialValue(diagram, "beta", beta);
    	BrainUtils.setInitialValue(diagram, "ro_max", roMax);
    	BrainUtils.setInitialValue(diagram, "G_glia", gGlia);
    	BrainUtils.setInitialValue(diagram, "eps_K", epsK);
    	BrainUtils.setInitialValue(diagram, "eps_O", epsO);
    	BrainUtils.setInitialValue(diagram, "alpha", alpha);
    	BrainUtils.setInitialValue(diagram, "lambda", lambda);
    	BrainUtils.setInitialValue(diagram, "time_start", timeStart);
        
    	// initial variable values
        double Kbath = 8.5; // no seizures if Kbath=4.0
        double O2bath = 32.0;
        double Ko = 4.0;
        double Nai = 18.0;
        double Nagi = 18.0;
        double Cli = 6.0;
        double Clo = 130.0;
        double O2o = 28.0;
        double V = -68.0;
        double m = 0.0084;
        double h = 0.998;
        double n = 0.024;
        
    	BrainUtils.setInitialValue(diagram, "K_bath", Kbath);
    	BrainUtils.setInitialValue(diagram, "O2_bath", O2bath);
    	BrainUtils.setInitialValue(diagram, "K_o", Ko);
    	BrainUtils.setInitialValue(diagram, "Na_i", Nai);
    	BrainUtils.setInitialValue(diagram, "Na_gi", Nagi);
    	BrainUtils.setInitialValue(diagram, "Cl_i", Cli);
    	BrainUtils.setInitialValue(diagram, "Cl_o", Clo);
    	BrainUtils.setInitialValue(diagram, "O2_o", O2o);
    	BrainUtils.setInitialValue(diagram, "V", V);
    	BrainUtils.setInitialValue(diagram, "m", m);
    	BrainUtils.setInitialValue(diagram, "h", h);
    	BrainUtils.setInitialValue(diagram, "n", n);
    }
    
    private static void setInitialValuesMinimal(Diagram diagram, MinimalCellularModelProperties cellularModelProperties) 
    {
    	// parameter values
    	double kBath = cellularModelProperties.getKBath();
    	double cM = cellularModelProperties.getCM();
    	double tauN = cellularModelProperties.getTauN();
    	double gCl = cellularModelProperties.getGCl();
    	double gK = cellularModelProperties.getGK();
    	double gNa = cellularModelProperties.getGNa();
    	double gKL = cellularModelProperties.getGKL();
    	double gNaL = cellularModelProperties.getGNaL();
    	double omegaI = cellularModelProperties.getOmegaI();
    	double omegaO = cellularModelProperties.getOmegaO();
    	double beta = cellularModelProperties.getBeta();
    	double gamma = cellularModelProperties.getGamma();
    	double eps = cellularModelProperties.getEps();
    	double ro = cellularModelProperties.getRo();
    	
    	BrainUtils.setInitialValue(diagram, "K_bath", kBath);
    	BrainUtils.setInitialValue(diagram, "C_m", cM);
    	BrainUtils.setInitialValue(diagram, "tau_n", tauN);
    	BrainUtils.setInitialValue(diagram, "g_Cl", gCl);
    	BrainUtils.setInitialValue(diagram, "g_K", gK);
    	BrainUtils.setInitialValue(diagram, "g_Na", gNa);
    	BrainUtils.setInitialValue(diagram, "g_K_l", gKL);
    	BrainUtils.setInitialValue(diagram, "g_Na_l", gNaL);
    	BrainUtils.setInitialValue(diagram, "omega_i", omegaI);
    	BrainUtils.setInitialValue(diagram, "omega_o", omegaO);
    	BrainUtils.setInitialValue(diagram, "beta", beta);
    	BrainUtils.setInitialValue(diagram, "gamma", gamma);
    	BrainUtils.setInitialValue(diagram, "eps", eps);
    	BrainUtils.setInitialValue(diagram, "ro", ro);
    	
        // initial variable values
        double V = -78.0;
        double Ko0 = 4.8;
        double Nao0 = 138.0;
        double Clo0 = 112.0;
        double Ki0 = 140.0;
        double Nai0 = 16.0;
        double Cli0 = 5.0;
        
    	BrainUtils.setInitialValue(diagram, "V", V);
    	BrainUtils.setInitialValue(diagram, "K_o0", Ko0);
    	BrainUtils.setInitialValue(diagram, "Na_o0", Nao0);
    	BrainUtils.setInitialValue(diagram, "Cl_o0", Clo0);
    	BrainUtils.setInitialValue(diagram, "K_i0", Ki0);
    	BrainUtils.setInitialValue(diagram, "Na_i0", Nai0);
    	BrainUtils.setInitialValue(diagram, "Cl_i0", Cli0);
    	
    	BrainUtils.setInitialValue(diagram, "K_o", Ko0);
    	BrainUtils.setInitialValue(diagram, "Na_o", Nao0);
    	BrainUtils.setInitialValue(diagram, "Cl_o", Clo0);
    	BrainUtils.setInitialValue(diagram, "K_i", Ki0);
    	BrainUtils.setInitialValue(diagram, "Na_i", Nai0);
    	BrainUtils.setInitialValue(diagram, "Cl_i", Cli0);
    }
    
    private static void setInitialValuesEpileptor2Oxygen(Diagram diagram, Epileptor2OxygenCellularModelProperties cellularModelProperties) 
    {    	
    	// parameter values
    	double sf = cellularModelProperties.getSf();

    	double tauK = cellularModelProperties.getTauK();
    	double tauNa = cellularModelProperties.getTauNa();
    	double tauM = cellularModelProperties.getTauM();
    	double tauD = cellularModelProperties.getTauD();
    	double dKo = cellularModelProperties.getDKo();
    	double dNai = cellularModelProperties.getDNai();
    	double dSR = cellularModelProperties.getDSR();
    	double gamma = cellularModelProperties.getGamma();
    	double roMax = cellularModelProperties.getRoMax();
    	double vTh = cellularModelProperties.getVTh();
    	double FRMax = cellularModelProperties.getFRMax();
    	double kFR = cellularModelProperties.getKFR();
    	double gKL = cellularModelProperties.getGKL();
    	double gSyn = cellularModelProperties.getGSyn();
    	double sigma = cellularModelProperties.getSigma();
    	double alpha = cellularModelProperties.getAlpha();
    	double lambda = cellularModelProperties.getLambda();
    	double epsO = cellularModelProperties.getEpsO();
    	
    	String neuronObserverType = cellularModelProperties.getNeuronObserverType();
        double cU = cellularModelProperties.getCU();
        double gU = cellularModelProperties.getGU();
        double gL = cellularModelProperties.getGL();
        double URest = cellularModelProperties.getURest();
        double UTh = cellularModelProperties.getUTh();
        double UPeak = cellularModelProperties.getUPeak();
        double UReset = cellularModelProperties.getUReset();
        // parameters for adaptation current in aQIF neuron observer
        double iA = 116.0;
        double tauW = 0.2;
        double dw = 100.0;
    	
        // seizures start and end time
    	double timeStart = 50.0;
    	double timeEnd = 700.0;
    	
    	BrainUtils.setInitialValue(diagram, "SF", sf);
    	
    	BrainUtils.setInitialValue(diagram, "tau_K", tauK);
    	BrainUtils.setInitialValue(diagram, "tau_Na", tauNa);
    	BrainUtils.setInitialValue(diagram, "tau_M", tauM);
    	BrainUtils.setInitialValue(diagram, "tau_D", tauD);
    	BrainUtils.setInitialValue(diagram, "dK_o", dKo);
    	BrainUtils.setInitialValue(diagram, "dNa_i", dNai);
    	BrainUtils.setInitialValue(diagram, "dSR", dSR);
    	BrainUtils.setInitialValue(diagram, "gamma", gamma);
    	BrainUtils.setInitialValue(diagram, "ro_max", roMax);
    	BrainUtils.setInitialValue(diagram, "V_th", vTh);
    	BrainUtils.setInitialValue(diagram, "FR_max", FRMax);
    	BrainUtils.setInitialValue(diagram, "k_FR", kFR);
    	BrainUtils.setInitialValue(diagram, "g_K_l", gKL);
    	BrainUtils.setInitialValue(diagram, "g_L", gL);
    	BrainUtils.setInitialValue(diagram, "G_syn", gSyn);
    	BrainUtils.setInitialValue(diagram, "sigma", sigma);
    	BrainUtils.setInitialValue(diagram, "alpha", alpha);
    	BrainUtils.setInitialValue(diagram, "lambda", lambda);
    	BrainUtils.setInitialValue(diagram, "eps_O", epsO);
    	
    	BrainUtils.setInitialValue(diagram, "C_U", cU, "nF");
    	BrainUtils.setInitialValue(diagram, "g_U", gU, "nS/mV");
    	BrainUtils.setInitialValue(diagram, "g_L", gL, "nS");
    	BrainUtils.setInitialValue(diagram, "U_rest", URest, "mV");
    	BrainUtils.setInitialValue(diagram, "U_th", UTh, "mV");
    	BrainUtils.setInitialValue(diagram, "U_peak", UPeak, "mV");
    	BrainUtils.setInitialValue(diagram, "U_reset", UReset, "mV");
    	
    	BrainUtils.setInitialValue(diagram, "I_a", iA, "pA");
    	BrainUtils.setInitialValue(diagram, "tau_w", tauW, "s");
    	BrainUtils.setInitialValue(diagram, "dw", dw, "pA");
    	
    	BrainUtils.setInitialValue(diagram, "time_start", timeStart, "s");
    	BrainUtils.setInitialValue(diagram, "time_end", timeEnd, "s");
    
        // initial variable values
        double Kbath = 3.0;
        double Ko = 3.0;
        double Nai = 10.0;
        double NaGi = 10.0;
        double SR = 1.0;
        double vK = -100.253;
        double O2bath = 32.0;
        double O2o = 28.0;
        
        double U = -70.0;
        
    	BrainUtils.setInitialValue(diagram, "K_bath", Kbath);
    	BrainUtils.setInitialValue(diagram, "K_o", Ko);
    	BrainUtils.setInitialValue(diagram, "Na_i", Nai);
    	BrainUtils.setInitialValue(diagram, "Na_i0", Nai);
    	BrainUtils.setInitialValue(diagram, "Na_gi", NaGi);
    	BrainUtils.setInitialValue(diagram, "SR", SR);
    	BrainUtils.setInitialValue(diagram, "V_K0", vK);
    	BrainUtils.setInitialValue(diagram, "O2_bath", O2bath);
    	BrainUtils.setInitialValue(diagram, "O2_o", O2o);
    	
    	BrainUtils.setInitialValue(diagram, "U", U, "mV");
    }

    private static void setPlotsEpileptor2(Diagram diagram) 
    {	
    	EModel emodel = diagram.getRole(EModel.class);
    	PlotsInfo plotsInfo = new PlotsInfo(emodel);
    	
        PlotInfo varPlotsFiringRate = new PlotInfo();
        varPlotsFiringRate.setTitle("Firing rate");
        varPlotsFiringRate.setXTitle("time (s)");
        varPlotsFiringRate.setYTitle("FR (Hz)");
        
        PlotInfo varPlotsDepolarization = new PlotInfo();
        varPlotsDepolarization.setTitle("Depolarization");
        varPlotsDepolarization.setXTitle("time (s)");
        varPlotsDepolarization.setYTitle("V (mV)");
        
        PlotInfo varPlotsSynapticResource = new PlotInfo();
        varPlotsSynapticResource.setTitle("Synaptic resource");
        varPlotsSynapticResource.setXTitle("time (s)");
        varPlotsSynapticResource.setYTitle("SR");
        
        PlotInfo varPlotsIonicConcentrations = new PlotInfo();
        varPlotsIonicConcentrations.setTitle("Ionic concentrations");
        varPlotsIonicConcentrations.setXTitle("time (s)");
        varPlotsIonicConcentrations.setYTitle("Na_i, K_o (mM)");
        
        PlotInfo varPlotsPumpActivity = new PlotInfo();
        varPlotsPumpActivity.setTitle("Na-K pump activity");
        varPlotsPumpActivity.setXTitle("time (s)");
        varPlotsPumpActivity.setYTitle("I_pump (mWs)");
        
        PlotInfo varPlotsMembranePotential = new PlotInfo();
        varPlotsMembranePotential.setTitle("Membrane potential");
        varPlotsMembranePotential.setXTitle("time (s)");
        varPlotsMembranePotential.setYTitle("U (mV)");
       
        plotsInfo.setPlots(new PlotInfo[] {varPlotsFiringRate, varPlotsDepolarization, varPlotsSynapticResource, varPlotsIonicConcentrations, varPlotsPumpActivity, varPlotsMembranePotential});

        List<Curve> curvesFiringRate = new ArrayList<Curve>();
        curvesFiringRate.add(new Curve("", "FR", "FR", emodel));
        
        List<Curve> curvesDepolarization = new ArrayList<Curve>();
        curvesDepolarization.add(new Curve( "", "V", "V", emodel));
        
        List<Curve> curvesSynapticResource = new ArrayList<Curve>();
        curvesSynapticResource.add(new Curve("", "SR", "SR", emodel));
        
        List<Curve> curvesIonicConcentrations = new ArrayList<Curve>();
        curvesIonicConcentrations.add(new Curve("", "K_o", "K_o", emodel));
        curvesIonicConcentrations.add(new Curve("", "Na_i", "Na_i", emodel));
        
        List<Curve> curvesPumpActivity = new ArrayList<Curve>();
        curvesPumpActivity.add(new Curve("", "I_pump", "I_pump", emodel));
        
        List<Curve> curvesMembranePotential = new ArrayList<Curve>();
        Curve membranePotentialCurve = new Curve("", "U", "U", emodel);
        membranePotentialCurve.setPen(new Pen(1, Color.red));
        curvesMembranePotential.add(membranePotentialCurve);
        
        varPlotsFiringRate.setYVariables(curvesFiringRate.stream().toArray(Curve[]::new));
        varPlotsDepolarization.setYVariables(curvesDepolarization.stream().toArray(Curve[]::new));
        varPlotsSynapticResource.setYVariables(curvesSynapticResource.stream().toArray(Curve[]::new));
        varPlotsIonicConcentrations.setYVariables(curvesIonicConcentrations.stream().toArray(Curve[]::new));
        varPlotsPumpActivity.setYVariables(curvesPumpActivity.stream().toArray(Curve[]::new));
        varPlotsMembranePotential.setYVariables(curvesMembranePotential.stream().toArray(Curve[]::new));
        DiagramUtility.setPlotsInfo(diagram, plotsInfo);
    }
    
    private static void setPlotsOxygen(Diagram diagram) 
    {
    	EModel emodel = diagram.getRole(EModel.class);
    	PlotsInfo plotsInfo = new PlotsInfo(emodel);
    	
        PlotInfo varPlotsMembranePotential = new PlotInfo();
        varPlotsMembranePotential.setTitle("Membrane potential");
        varPlotsMembranePotential.setXTitle("time (ms)");
        varPlotsMembranePotential.setYTitle("V (mV)");
        
        PlotInfo varPlotsKConcentration = new PlotInfo();
        varPlotsKConcentration.setTitle("Extracellular K concentration");
        varPlotsKConcentration.setXTitle("time (ms)");
        varPlotsKConcentration.setYTitle("K_o (mM)");
        
        PlotInfo varPlotsO2Concentration = new PlotInfo();
        varPlotsO2Concentration.setTitle("Extracellular O2 concentration");
        varPlotsO2Concentration.setXTitle("time (ms)");
        varPlotsO2Concentration.setYTitle("O2_o (mg/L)");
        
        PlotInfo varPlotsO2Rate = new PlotInfo();
        varPlotsO2Rate.setTitle("O2 consumption rate");
        varPlotsO2Rate.setXTitle("time (ms)");
        varPlotsO2Rate.setYTitle("O2_rate (mg/L/s)");
       
        
        plotsInfo.setPlots(new PlotInfo[] {varPlotsMembranePotential, varPlotsKConcentration, varPlotsO2Concentration, varPlotsO2Rate});

        List<Curve> curvesMembranePotential = new ArrayList<Curve>();
        curvesMembranePotential.add(new Curve("", "V", "V", emodel));
        
        List<Curve> curvesKConcentration = new ArrayList<Curve>();
        curvesKConcentration.add(new Curve("", "K_o", "K_o", emodel));
        
        List<Curve> curvesO2Concentration = new ArrayList<Curve>();
        curvesO2Concentration.add(new Curve("", "O2_o", "O2_o", emodel));
        
        List<Curve> curvesO2Rate = new ArrayList<Curve>();
        curvesO2Rate.add(new Curve("", "O2_rate_pump", "O2_rate_pump", emodel));
        curvesO2Rate.add(new Curve("", "O2_rate_bath", "O2_rate_bath", emodel));
        
        varPlotsMembranePotential.setYVariables(curvesMembranePotential.stream().toArray(Curve[]::new));
        varPlotsKConcentration.setYVariables(curvesKConcentration.stream().toArray(Curve[]::new));
        varPlotsO2Concentration.setYVariables(curvesO2Concentration.stream().toArray(Curve[]::new));
        varPlotsO2Rate.setYVariables(curvesO2Rate.stream().toArray(Curve[]::new));
        DiagramUtility.setPlotsInfo(diagram, plotsInfo);
    }
    
    private static void setPlotsMinimal(Diagram diagram) 
    {
    	EModel emodel = diagram.getRole(EModel.class);
    	PlotsInfo plotsInfo = new PlotsInfo(emodel);
    	
        PlotInfo varPlotsMembranePotential = new PlotInfo();
        varPlotsMembranePotential.setTitle("Membrane potential");
        varPlotsMembranePotential.setXTitle("time (ms)");
        varPlotsMembranePotential.setYTitle("V (mV)");
        
        plotsInfo.setPlots(new PlotInfo[] {varPlotsMembranePotential});

        List<Curve> curvesMembranePotential = new ArrayList<Curve>();
        curvesMembranePotential.add(new Curve("", "V", "V", emodel));
        
        varPlotsMembranePotential.setYVariables(curvesMembranePotential.stream().toArray(Curve[]::new));
        DiagramUtility.setPlotsInfo(diagram, plotsInfo);
    }
    
    private static void setPlotsEpileptor2Oxygen(Diagram diagram) 
    {	
    	EModel emodel = diagram.getRole(EModel.class);
    	PlotsInfo plotsInfo = new PlotsInfo(emodel);
    	
        PlotInfo varPlotsFiringRate = new PlotInfo();
        varPlotsFiringRate.setTitle("Firing rate");
        varPlotsFiringRate.setXTitle("time (s)");
        varPlotsFiringRate.setYTitle("FR (Hz)");
        
        PlotInfo varPlotsDepolarization = new PlotInfo();
        varPlotsDepolarization.setTitle("Depolarization");
        varPlotsDepolarization.setXTitle("time (s)");
        varPlotsDepolarization.setYTitle("V (mV)");
        
        PlotInfo varPlotsSynapticResource = new PlotInfo();
        varPlotsSynapticResource.setTitle("Synaptic resource");
        varPlotsSynapticResource.setXTitle("time (s)");
        varPlotsSynapticResource.setYTitle("SR");
        
        PlotInfo varPlotsIonicConcentrations = new PlotInfo();
        varPlotsIonicConcentrations.setTitle("Ionic concentrations");
        varPlotsIonicConcentrations.setXTitle("time (s)");
        varPlotsIonicConcentrations.setYTitle("Na_i, K_o (mM)");
        
        PlotInfo varPlotsPumpActivity = new PlotInfo();
        varPlotsPumpActivity.setTitle("Na-K pump activity");
        varPlotsPumpActivity.setXTitle("time (s)");
        varPlotsPumpActivity.setYTitle("I_pump (mWs)");
        
        PlotInfo varPlotsOxygenDynamics = new PlotInfo();
        varPlotsOxygenDynamics.setTitle("Oxygen dynamics");
        varPlotsOxygenDynamics.setXTitle("time (s)");
        varPlotsOxygenDynamics.setYTitle("O2_o (mg/L)");
        
        PlotInfo varPlotsMembranePotential = new PlotInfo();
        varPlotsMembranePotential.setTitle("Membrane potential");
        varPlotsMembranePotential.setXTitle("time (s)");
        varPlotsMembranePotential.setYTitle("U (mV)");
       
        plotsInfo.setPlots(new PlotInfo[] {varPlotsFiringRate, varPlotsDepolarization, varPlotsSynapticResource, 
        		varPlotsIonicConcentrations, varPlotsPumpActivity, varPlotsOxygenDynamics, varPlotsMembranePotential});

        List<Curve> curvesFiringRate = new ArrayList<Curve>();
        curvesFiringRate.add(new Curve("", "FR", "FR", emodel));
        
        List<Curve> curvesDepolarization = new ArrayList<Curve>();
        curvesDepolarization.add(new Curve("", "V", "V", emodel));
        
        List<Curve> curvesSynapticResource = new ArrayList<Curve>();
        curvesSynapticResource.add(new Curve("", "SR", "SR", emodel));
        
        List<Curve> curvesIonicConcentrations = new ArrayList<Curve>();
        curvesIonicConcentrations.add(new Curve("", "K_o", "K_o", emodel));
        curvesIonicConcentrations.add(new Curve("", "Na_i", "Na_i", emodel));
        
        List<Curve> curvesPumpActivity = new ArrayList<Curve>();
        curvesPumpActivity.add(new Curve("", "I_pump", "I_pump", emodel));
        
        List<Curve> curvesOxygenDynamics = new ArrayList<Curve>();
        curvesOxygenDynamics.add(new Curve("", "O2_o", "O2_o", emodel));
        
        List<Curve> curvesMembranePotential = new ArrayList<Curve>();
        Curve membranePotentialCurve = new Curve("", "U", "U", emodel);
        membranePotentialCurve.setPen(new Pen(1, Color.red));
        curvesMembranePotential.add(membranePotentialCurve);
        
        varPlotsFiringRate.setYVariables(curvesFiringRate.stream().toArray(Curve[]::new));
        varPlotsDepolarization.setYVariables(curvesDepolarization.stream().toArray(Curve[]::new));
        varPlotsSynapticResource.setYVariables(curvesSynapticResource.stream().toArray(Curve[]::new));
        varPlotsIonicConcentrations.setYVariables(curvesIonicConcentrations.stream().toArray(Curve[]::new));
        varPlotsPumpActivity.setYVariables(curvesPumpActivity.stream().toArray(Curve[]::new));
        varPlotsOxygenDynamics.setYVariables(curvesOxygenDynamics.stream().toArray(Curve[]::new));
        varPlotsMembranePotential.setYVariables(curvesMembranePotential.stream().toArray(Curve[]::new));
        DiagramUtility.setPlotsInfo(diagram, plotsInfo);
    }
}