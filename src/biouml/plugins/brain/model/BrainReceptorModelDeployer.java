package biouml.plugins.brain.model;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.VariableRole;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.plot.PlotVariable;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.util.DiagramXmlConstants;
import biouml.plugins.brain.diagram.BrainDiagramType;
import biouml.plugins.brain.diagram.BrainReceptorModel;
import biouml.plugins.brain.diagram.BrainType;
import biouml.plugins.brain.diagram.BrainUtils;
import biouml.plugins.brain.model.receptor.AmpaReceptorModelProperties;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Type;
import ru.biosoft.util.DPSUtils;

/*
 * Deployer for receptor brain models.
 */
public class BrainReceptorModelDeployer
{
	public static @Nonnull Diagram deployBrainReceptorModel(Diagram src, String name) throws Exception
	{
	    return deployBrainReceptorModel(src, name, true);
	}

    public static @Nonnull Diagram deployBrainReceptorModel(Diagram src, String name, boolean needLayout) throws Exception
    {
    	BrainReceptorModel receptorModel = BrainUtils.getReceptorModelNodes(src).get(0).getRole(BrainReceptorModel.class);
    	String receptorModelType = receptorModel.getReceptorModelType();
    	switch (receptorModelType)
    	{
    	    case BrainType.TYPE_RECEPTOR_AMPA:
    	    	String ampaModelType = ((AmpaReceptorModelProperties)receptorModel.getReceptorModelProperties()).getModelType();
    	    	if (ampaModelType.equals(AmpaReceptorModelProperties.MODEL_SBML))
    	    	{
    	    		return fillSbmlModelAmpa(src, name);
    	    	}
    	    	else if (ampaModelType.equals(AmpaReceptorModelProperties.MODEL_MATH))
    	    	{
    	    		return fillDiagramWithEquationsAmpa(src, name);
    	    	}
    	    	else
    	    	{
    	    		return (new BrainDiagramType()).createDiagram(null, null, null);
    	    	}
    	    default:
    	    	return (new BrainDiagramType()).createDiagram(null, null, null);
    	}
    }
    
    public static Diagram fillSbmlModelAmpa(Diagram diagram, String name) throws Exception
    {
    	String dName;
    	if (name.isEmpty())
    	{
    		String prefix = diagram.getName();
            String suffix = "receptor_AMPA_sbml_model";
            dName = suffix.isEmpty() ? prefix : prefix + "_" + suffix;
    	}
    	else
    	{
    		dName = name;
    	}

        Diagram diagramSbgn = (new SbgnDiagramType()).createDiagram(diagram.getOrigin(), dName, new DiagramInfo(dName));

        if (diagram.getComment() != null)
        {
            diagramSbgn.setComment(diagram.getComment());
        }
        
        BrainReceptorModel receptorModel = BrainUtils.getReceptorModelNodes(diagram).get(0).getRole(BrainReceptorModel.class);
    	AmpaReceptorModelProperties receptorModelProperties = (AmpaReceptorModelProperties)receptorModel.getReceptorModelProperties();
    	String regimeType = receptorModelProperties.getRegimeType();
    	boolean portsFlag = receptorModelProperties.getPortsFlag();
    	
        int xCompartmentOffset = 60;
        int yCompartmentOffset = 60;
        
        int xComplexOffset = 60;
        int yComplexOffset = 230;
        
        Point point = new Point(0, 0);
        
        Compartment compartmentPsd = BrainUtils.createCompartment("PSD", "postsynaptic density (PSD)", 0.1257, "micrometers^2", new Dimension(520, 650), diagramSbgn, point);
        compartmentPsd.getRole(VariableRole.class).setConstant(true);
        point.translate(compartmentPsd.getShapeSize().width + xCompartmentOffset, 0);
        
        Compartment compartmentEsm = BrainUtils.createCompartment("ESM", "extrasynaptic membrane (ESM)", 1.257, "micrometers^2", new Dimension(250, 650), diagramSbgn, point);
        compartmentEsm.getRole(VariableRole.class).setConstant(true);
        point.translate(compartmentEsm.getShapeSize().width + xCompartmentOffset, 0); 

        Compartment compartmentDendrite = BrainUtils.createCompartment("dendrite", "dendritic cable", 1.0, "", new Dimension(250, 425), diagramSbgn, point);
        compartmentDendrite.getRole(VariableRole.class).setConstant(true);
        
        point = new Point(point.x / 2, compartmentEsm.getLocation().y + compartmentEsm.getShapeSize().height + yCompartmentOffset);
        Compartment compartmentIntracellular = BrainUtils.createCompartment("intracellular", "intracellular store", 1.0, "", new Dimension(500, 150), diagramSbgn, point);
        
        point = compartmentPsd.getLocation();
        point.translate(20, 40);
        Compartment complexQ1 = BrainUtils.createComplex("Q1", "Q1", 0.0, VariableRole.CONCENTRATION_TYPE, "", new Dimension(150, 130), compartmentPsd, point);
        point.translate(0, yComplexOffset);
        Compartment complexQ2a = BrainUtils.createComplex("Q2a", "Q2a", 160.0, VariableRole.CONCENTRATION_TYPE, "", new Dimension(150, 130), compartmentPsd, point);
        point.translate(0,  yComplexOffset);
        Compartment complexQ2b = BrainUtils.createComplex("Q2b", "Q2b", 0.0, VariableRole.CONCENTRATION_TYPE, "", new Dimension(150, 130), compartmentPsd, point);
        point = compartmentPsd.getLocation();
        point.translate(compartmentPsd.getShapeSize().width - complexQ1.getShapeSize().width - 20, 40);
        Compartment complexP1 = BrainUtils.createComplex("P1", "P1", 13.0, VariableRole.CONCENTRATION_TYPE, "", new Dimension(150, 90), compartmentPsd, point);
        point.translate(0, yComplexOffset);
        Compartment complexP2a = BrainUtils.createComplex("P2a", "P2a", 140.0, VariableRole.CONCENTRATION_TYPE, "", new Dimension(150, 90), compartmentPsd, point);
        point.translate(0,  yComplexOffset);
        Compartment complexP2b = BrainUtils.createComplex("P2b", "P2b", 0.0, VariableRole.CONCENTRATION_TYPE, "", new Dimension(150, 90), compartmentPsd, point);
        point = compartmentPsd.getLocation();
        point.translate(220, 180);
        Compartment complexL = BrainUtils.createComplex("L", "L", 0.0, VariableRole.CONCENTRATION_TYPE, "1/micrometers^2", new Dimension(100, 80), compartmentPsd, point);
        
        point = compartmentEsm.getLocation();
        point.translate(50, 40);
        Compartment complexR1 = BrainUtils.createComplex("R1", "R1", 13.0, VariableRole.CONCENTRATION_TYPE, "", new Dimension(150, 90), compartmentEsm, point);
        point.translate(0, yComplexOffset);
        Compartment complexR2a = BrainUtils.createComplex("R2a", "R2a", 7.5, VariableRole.CONCENTRATION_TYPE, "", new Dimension(150, 90), compartmentEsm, point);
        point.translate(0,  yComplexOffset);
        Compartment complexR2b = BrainUtils.createComplex("R2b", "R2b", 0.0, VariableRole.CONCENTRATION_TYPE, "", new Dimension(150, 90), compartmentEsm, point);
        
        point = compartmentDendrite.getLocation();
        point.translate(50, 40);
        Compartment complexD1 = BrainUtils.createComplex("D1", "D1", 10.0, VariableRole.CONCENTRATION_TYPE, "1/micrometers^2", new Dimension(150, 90), compartmentDendrite, point);
        complexD1.getRole(VariableRole.class).setConstant(true);
        point.translate(0,  yComplexOffset);
        Compartment complexD2 = BrainUtils.createComplex("D2", "D2", 0.0, VariableRole.CONCENTRATION_TYPE, "1/micrometers^2", new Dimension(150, 90), compartmentDendrite, point);
        complexD2.getRole(VariableRole.class).setConstant(true);
        
        point = compartmentIntracellular.getLocation();
        point.translate(20, 40);
        Compartment complexS2 = BrainUtils.createComplex("S2", "S2", 100.0, VariableRole.AMOUNT_TYPE, "", new Dimension(150, 90), compartmentIntracellular, point);
        complexS2.getRole(VariableRole.class).setConstant(true);
        point = compartmentIntracellular.getLocation();
        point.translate(compartmentIntracellular.getShapeSize().width - complexS2.getShapeSize().width - 20,  40);
        Compartment complexS1 = BrainUtils.createComplex("S1", "S1", 500.0, VariableRole.AMOUNT_TYPE, "", new Dimension(150, 90), compartmentIntracellular, point);
        
        BrainUtils.addEntitiesToComplex(Arrays.asList("GluR1", "GluR2", "PSD_95"), complexQ1, diagramSbgn, new Dimension(70, 40));
        BrainUtils.addEntitiesToComplex(Arrays.asList("GluR2", "GluR3", "PSD_95"), complexQ2a, diagramSbgn, new Dimension(70, 40));
        BrainUtils.addEntitiesToComplex(Arrays.asList("GluR2", "GluR3", "PSD_95"), complexQ2b, diagramSbgn, new Dimension(70, 40));
        BrainUtils.addEntitiesToComplex(Arrays.asList("GluR1", "GluR2"), complexP1, diagramSbgn, new Dimension(70, 40));
        BrainUtils.addEntitiesToComplex(Arrays.asList("GluR2", "GluR3"), complexP2a, diagramSbgn, new Dimension(70, 40));
        BrainUtils.addEntitiesToComplex(Arrays.asList("GluR2", "GluR3"), complexP2b, diagramSbgn, new Dimension(70, 40));
        BrainUtils.addEntitiesToComplex(Arrays.asList("PSD_95"), complexL, diagramSbgn, new Dimension(70, 40));
        
        BrainUtils.addEntitiesToComplex(Arrays.asList("GluR1", "GluR2"), complexR1, diagramSbgn, new Dimension(70, 40));
        BrainUtils.addEntitiesToComplex(Arrays.asList("GluR2", "GluR3"), complexR2a, diagramSbgn, new Dimension(70, 40));
        BrainUtils.addEntitiesToComplex(Arrays.asList("GluR2", "GluR3"), complexR2b, diagramSbgn, new Dimension(70, 40));
        
        BrainUtils.addEntitiesToComplex(Arrays.asList("GluR1", "GluR2"), complexD1, diagramSbgn, new Dimension(70, 40));
        BrainUtils.addEntitiesToComplex(Arrays.asList("GluR2", "GluR3"), complexD2, diagramSbgn, new Dimension(70, 40));
        
        BrainUtils.addEntitiesToComplex(Arrays.asList("GluR2", "GluR3"), complexS2, diagramSbgn, new Dimension(70, 40));
        BrainUtils.addEntitiesToComplex(Arrays.asList("GluR1", "GluR2"), complexS1, diagramSbgn, new Dimension(70, 40));
        
        SpecieReference[] specieReference;
        
        specieReference = new SpecieReference[] {createSpeciesReference("P1", SpecieReference.REACTANT), createSpeciesReference("L", SpecieReference.REACTANT), createSpeciesReference("Q1", SpecieReference.PRODUCT)};
        BrainUtils.createReaction(specieReference, "(alpha_1*$PSD.L*$PSD.P1 - beta_1*$PSD.Q1)*$PSD", diagramSbgn, new Point(260, 80));
        specieReference = new SpecieReference[] {createSpeciesReference("P2a", SpecieReference.REACTANT), createSpeciesReference("L", SpecieReference.REACTANT), createSpeciesReference("Q2a", SpecieReference.PRODUCT)};
        BrainUtils.createReaction(specieReference, "(alpha_2*$PSD.L*$PSD.P2a - beta_2a*$PSD.Q2a)*$PSD", diagramSbgn, new Point(260, 320));
        specieReference = new SpecieReference[] {createSpeciesReference("Q2a", SpecieReference.REACTANT), createSpeciesReference("Q2b", SpecieReference.PRODUCT)};
        BrainUtils.createReaction(specieReference, "(mu*$PSD.Q2a - nu*$PSD.Q2b)*$PSD", diagramSbgn, new Point(90, 440));
        specieReference = new SpecieReference[] {createSpeciesReference("P2a", SpecieReference.REACTANT), createSpeciesReference("P2b", SpecieReference.PRODUCT)};
        BrainUtils.createReaction(specieReference, "(mu*$PSD.P2a - nu*$PSD.P2b)*$PSD", diagramSbgn, new Point(420, 420));
        specieReference = new SpecieReference[] {createSpeciesReference("Q2b", SpecieReference.REACTANT), createSpeciesReference("P2b", SpecieReference.PRODUCT)};
        BrainUtils.createReaction(specieReference, "beta_2b*$PSD.Q2b*$PSD", diagramSbgn, new Point(260, 540));
        specieReference = new SpecieReference[] {createSpeciesReference("P1", SpecieReference.REACTANT), createSpeciesReference("R1", SpecieReference.PRODUCT)};
        BrainUtils.createReaction(specieReference, "h_1*($PSD.P1 - $ESM.R1)", diagramSbgn, new Point(540, 80));
        specieReference = new SpecieReference[] {createSpeciesReference("P2a", SpecieReference.REACTANT), createSpeciesReference("R2a", SpecieReference.PRODUCT)};
        BrainUtils.createReaction(specieReference, "h_2a*($PSD.P2a - $ESM.R2a)", diagramSbgn, new Point(540, 300));
        specieReference = new SpecieReference[] {createSpeciesReference("P2b", SpecieReference.REACTANT), createSpeciesReference("R2b", SpecieReference.PRODUCT)};
        BrainUtils.createReaction(specieReference, "h_2b*$PSD.P2b", diagramSbgn, new Point(540, 540));
        
        specieReference = new SpecieReference[] {createSpeciesReference("R1", SpecieReference.REACTANT), createSpeciesReference("D1", SpecieReference.PRODUCT)};
        BrainUtils.createReaction(specieReference, "omega_1*($ESM.R1 - $dendrite.D1)", diagramSbgn, new Point(850, 80));
        specieReference = new SpecieReference[] {createSpeciesReference("R2a", SpecieReference.REACTANT), createSpeciesReference("D2", SpecieReference.PRODUCT)};
        BrainUtils.createReaction(specieReference, "omega_2*($ESM.R2a - $dendrite.D2)", diagramSbgn, new Point(850, 300));
        specieReference = new SpecieReference[] {createSpeciesReference("R1", SpecieReference.REACTANT)};
        BrainUtils.createReaction(specieReference, "k_1*$ESM.R1*$ESM", diagramSbgn, new Point(570, 670));
        specieReference = new SpecieReference[] {createSpeciesReference("R2a", SpecieReference.REACTANT)};
        BrainUtils.createReaction(specieReference, "k_2*$ESM.R2a*$ESM", diagramSbgn, new Point(600, 670));
        specieReference = new SpecieReference[] {createSpeciesReference("R2b", SpecieReference.REACTANT)};
        BrainUtils.createReaction(specieReference, "1.0", diagramSbgn, new Point(700, 670));
        
        specieReference = new SpecieReference[] {createSpeciesReference("S2", SpecieReference.REACTANT), createSpeciesReference("P2a", SpecieReference.PRODUCT)};
        BrainUtils.createReaction(specieReference, "sigma_2", diagramSbgn, new Point(300, 780));
        specieReference = new SpecieReference[] {createSpeciesReference("S1", SpecieReference.REACTANT), createSpeciesReference("R1", SpecieReference.PRODUCT)};
        BrainUtils.createReaction(specieReference, "sigma_1", diagramSbgn, new Point(830, 670));
        specieReference = new SpecieReference[] {createSpeciesReference("S1", SpecieReference.PRODUCT)};
        BrainUtils.createReaction(specieReference, "delta_1", diagramSbgn, new Point(950, 670));

        int yGroupOffset = 80;
        int yLocalOffset = 40;
        
        point = compartmentDendrite.getLocation();
        point.translate(compartmentDendrite.getShapeSize().width + xCompartmentOffset, 0);
        
        String lFormula = "L_total - $Q1 - $Q2a - $Q2b";
        BrainUtils.createEquation("Concentration of free binding sites, L", "L", lFormula,
    			Equation.TYPE_SCALAR, diagramSbgn, point);
        point.translate(0, yLocalOffset);
        
        String lTotalFormula = "c*(kappa_1*$S1 - delta_1) -gamma*$L";
        BrainUtils.createEquation("L total equation", "L_total", lTotalFormula,
    			Equation.TYPE_RATE, diagramSbgn, point);
        point.translate(0, yGroupOffset);
        
        String sigma1Formula = "kappa_1*$S1";
        BrainUtils.createEquation("sigma1", "sigma_1", sigma1Formula,
    			Equation.TYPE_SCALAR, diagramSbgn, point);
        String sigma2Formula = "kappa_2*$S2";
        BrainUtils.createEquation("sigma2", "sigma_2", sigma2Formula,
    			Equation.TYPE_SCALAR, diagramSbgn, new Point(point.x + 150, point.y));
        point.translate(0, yLocalOffset);
        
        if (portsFlag)
        {	
        	BrainUtils.createEvent("start_LTP_epileptogenic", "epileptogenicity_flag == 1.0 && SR < 0.6", new Assignment[]
			{
			    new Assignment("mu", "0.0"), 
				new Assignment("gamma", "0.0"),
				new Assignment("alpha_1", "0.001"),
				new Assignment("kappa_1", "0.0556"),
				new Assignment("h_1", "0.01"),
				new Assignment("c", "0.65"),
			},
			diagramSbgn, point);
	        point.translate(0, 2 * yGroupOffset + 30);
	        
        	BrainUtils.createEvent("start_LTD_healthy", "epileptogenicity_flag == 0.0 && SR < 0.6", new Assignment[] 
			{
				 new Assignment("mu", "0.01"), 
				
				new Assignment("gamma", "0.001"),
				new Assignment("alpha_1", "1.0E-6"),
				new Assignment("kappa_1", "5.556E-4"),
				new Assignment("h_1", "0.001257"),
				new Assignment("c", "0.0")
			},
			diagramSbgn, point);
            point.translate(0, yGroupOffset);
        }
        else if (regimeType.equals(AmpaReceptorModelProperties.REGIME_LTD))
        {
        	BrainUtils.createEvent("Stop LTD", "time > 900.0", new Assignment[] {new Assignment("mu", "0.0"), new Assignment("gamma", "0.0")},
        			diagramSbgn, point);
            point.translate(0, yGroupOffset);
        }
        else if (regimeType.equals(AmpaReceptorModelProperties.REGIME_LTD_FOLLOWED_BY_LTP))
        {
        	BrainUtils.createEvent("Stop LTD 1", "time > 900.0", new Assignment[] {new Assignment("mu", "0.0"), new Assignment("gamma", "0.0")},
        			diagramSbgn, point);
            point.translate(0, yGroupOffset);
            
            BrainUtils.createEvent("Start LTD 2", "time > 3600.0", new Assignment[] {new Assignment("mu", "0.01"), new Assignment("gamma", "0.001")},
        			diagramSbgn, point);
            BrainUtils.createEvent("Stop LTD 2", "time > 4500.0", new Assignment[] {new Assignment("mu", "0.0"), new Assignment("gamma", "0.0")},
        			diagramSbgn, new Point(point.x + 150, point.y));
            point.translate(0, yGroupOffset);
            
            BrainUtils.createEvent("Start LTD 3", "time > 7200.0", new Assignment[] {new Assignment("mu", "0.01"), new Assignment("gamma", "0.001")},
        			diagramSbgn, point);
            BrainUtils.createEvent("Stop LTD 3", "time > 8100.0", new Assignment[] {new Assignment("mu", "0.0"), new Assignment("gamma", "0.0")},
         			diagramSbgn, new Point(point.x + 150, point.y));
            point.translate(0, yGroupOffset);
             
            BrainUtils.createEvent("Start LTP", "time > 10800.0", new Assignment[] {
            		new Assignment("alpha_1", "0.001"), 
            		new Assignment("kappa_1", "0.0556"),
            		new Assignment("h_1", "0.01"),
            		new Assignment("c", "0.325"),
            		new Assignment("gamma", "0.0")
            		},
          			diagramSbgn, new Point(point.x + 75, point.y));
        }
        
        point = compartmentDendrite.getLocation();
        point.translate(compartmentDendrite.getShapeSize().width + 500, 0);
        
        String totalPsdFormula = "($P1 + $Q1 + $P2a + $P2b + $Q2a + $Q2b)*$PSD";
        BrainUtils.createEquation("Total AMPA in PSD", "Total_PSD", totalPsdFormula,
    			Equation.TYPE_SCALAR, diagramSbgn, point);
        point.translate(0, yLocalOffset);
        
        String bound12PsdFormula = "$Q1*$PSD";
        BrainUtils.createEquation("Bound GluR1_2 in PSD", "Bound12_PSD", bound12PsdFormula,
    			Equation.TYPE_SCALAR, diagramSbgn, point);
        point.translate(0, yLocalOffset);
        
        String free12PsdFormula = "$P1*$PSD";
        BrainUtils.createEquation("Free GluR1_2 in PSD", "Free12_PSD", free12PsdFormula,
    			Equation.TYPE_SCALAR, diagramSbgn, point);
        point.translate(0, yLocalOffset);
        
        String bound23aPsdFormula = "$Q2a*$PSD";
        BrainUtils.createEquation("Bound GluR2_3_a in PSD", "Bound23a_PSD", bound23aPsdFormula,
    			Equation.TYPE_SCALAR, diagramSbgn, point);
        point.translate(0, yLocalOffset);
        
        String free23aPsdFormula = "$P2a*$PSD";
        BrainUtils.createEquation("Free GluR2_3_a in PSD", "Free23a_PSD", free23aPsdFormula,
    			Equation.TYPE_SCALAR, diagramSbgn, point);
        point.translate(0, yLocalOffset);
        
        String bound23bPsdFormula = "$Q2b*$PSD";
        BrainUtils.createEquation("Bound GluR2_3_a in PSD", "Bound23b_PSD", bound23bPsdFormula,
    			Equation.TYPE_SCALAR, diagramSbgn, point);
        point.translate(0, yLocalOffset);
        
        String free23bPsdFormula = "$P2b*$PSD";
        BrainUtils.createEquation("Free GluR2_3_b in PSD", "Free23b_PSD", free23bPsdFormula,
    			Equation.TYPE_SCALAR, diagramSbgn, point);
        point.translate(0, yLocalOffset);
        
        String BSitesPsdFormula = "L_total*$PSD";
        BrainUtils.createEquation("Binding sites in PSD", "BSites_PSD", BSitesPsdFormula,
    			Equation.TYPE_SCALAR, diagramSbgn, point);
        point.translate(0, yGroupOffset);
        
        String totalEsmFormula = "($R1 + $R2a)*$ESM";
        BrainUtils.createEquation("Total AMPA in ESM", "Total_ESM", totalEsmFormula,
    			Equation.TYPE_SCALAR, diagramSbgn, point);
        point.translate(0, yLocalOffset);
        
        String intra12Formula = "$S1";
        BrainUtils.createEquation("Intracellular GluR1/2", "Intra12", intra12Formula,
    			Equation.TYPE_SCALAR, diagramSbgn, point);
        point.translate(0, yGroupOffset);
        
        if (portsFlag)
        {   
            BrainUtils.createEquation("Total_PSD_max", "Total_PSD_max", "92.75",
                    Equation.TYPE_INITIAL_ASSIGNMENT, diagramSbgn, point);
            BrainUtils.createEquation("Total_PSD_0", "Total_PSD_0", "Total_PSD",
                    Equation.TYPE_INITIAL_ASSIGNMENT, diagramSbgn, new Point(point.x + 150, point.y));
//            BrainUtils.createEquation("Total_PSD_min", "Total_PSD_min", "19.0",
//                    Equation.TYPE_INITIAL_ASSIGNMENT, diagramSbgn, new Point(point.x + 2 * 150, point.y));
            BrainUtils.createEquation("Total_PSD_min", "Total_PSD_min", "26.0",
                    Equation.TYPE_INITIAL_ASSIGNMENT, diagramSbgn, new Point(point.x + 2 * 150, point.y));
            point.translate(0, yLocalOffset);
        	
            String interpolationFormula = "function interpolate(x1, x2, y1, y2, x) = (y2 - y1)*((x - x1)/(x2 - x1)) + y1";
            BrainUtils.createFunction("interpolate", interpolationFormula,
            		diagramSbgn, point);
            point.translate(0, yLocalOffset);

//            String sfFormula = "piecewise( Total_PSD >= Total_PSD_0 => interpolate(Total_PSD_0, Total_PSD_max, 0.85, 0.95, Total_PSD); "
//            		+ "interpolate(Total_PSD_min, Total_PSD_0, 0.75, 0.85, Total_PSD) )";
//            String sfFormula = "piecewise( Total_PSD >= Total_PSD_0 => interpolate(Total_PSD_0, Total_PSD_max, 0.8, 0.9, Total_PSD); "
//            		+ "interpolate(Total_PSD_min, Total_PSD_0, 0.7, 0.8, Total_PSD) )";
//            String sfFormula = "piecewise( Total_PSD >= Total_PSD_0 => interpolate(Total_PSD_0, Total_PSD_max, 0.825, 0.9, Total_PSD); "
//            		+ "interpolate(Total_PSD_min, Total_PSD_0, 0.75, 0.825, Total_PSD) )"; 
            String sfFormula = "piecewise( Total_PSD >= Total_PSD_0 => interpolate(Total_PSD_0, Total_PSD_max, 0.85, 0.9, Total_PSD); "
            		+ "interpolate(Total_PSD_min, Total_PSD_0, 0.8, 0.85, Total_PSD) )"; 
            
//            String sfFormula = "piecewise( Total_PSD >= Total_PSD_0 => interpolate(Total_PSD_0, Total_PSD_max, 0.9, 1.0, Total_PSD); "
//            		+ "interpolate(Total_PSD_min, Total_PSD_0, 0.8, 0.9, Total_PSD) )";
//            String sfFormula = "piecewise( Total_PSD >= Total_PSD_0 => interpolate(Total_PSD_0, Total_PSD_max, 1.0, 1.05, Total_PSD); "
//            		+ "interpolate(Total_PSD_min, Total_PSD_0, 0.8, 1.0, Total_PSD) )";
//            String sfFormula = "piecewise( Total_PSD >= Total_PSD_0 => interpolate(Total_PSD_0, Total_PSD_max, 1.0, 1.1, Total_PSD); "
            BrainUtils.createEquation("equation_SF", "SF", sfFormula,
        			Equation.TYPE_SCALAR, diagramSbgn, point);
            point.translate(0, yGroupOffset);
            
            BrainUtils.createPort("SF", "SF", Type.TYPE_OUTPUT_CONNECTION_PORT,
            		diagramSbgn, point);
            point.translate(0, yLocalOffset);
            
            BrainUtils.createPort("SR", "SR", Type.TYPE_INPUT_CONNECTION_PORT,
            		diagramSbgn, point);
            point.translate(0, yLocalOffset);
        }

        point = compartmentDendrite.getLocation();
        point.translate(compartmentDendrite.getShapeSize().width + 830, 70);
        
        String timeMinuteFormula = "time/60.0";
        BrainUtils.createEquation("time minute", "time_min", timeMinuteFormula,
    			Equation.TYPE_SCALAR, diagramSbgn, point);
        String timeHourFormula = "time_min/60.0";
        BrainUtils.createEquation("time hour", "time_hour", timeHourFormula,
    			Equation.TYPE_SCALAR, diagramSbgn, new Point(point.x + 120, point.y));
        
        setInitialValuesAmpa(diagramSbgn, receptorModelProperties);
        
        //Simulator settings
        JavaSimulationEngine se = new JavaSimulationEngine();
        se.setDiagram(diagramSbgn);
        double simulationTime;
        switch (regimeType)
        {
            case AmpaReceptorModelProperties.REGIME_NORMAL:
            	simulationTime = 30.0 * 60.0;
            	break;
            case AmpaReceptorModelProperties.REGIME_LTP:
            	simulationTime = 5.0 * 60.0;
            	break;
            case AmpaReceptorModelProperties.REGIME_LTD:
                simulationTime = 30.0 * 60.0;
                break;
            case AmpaReceptorModelProperties.REGIME_LTD_FOLLOWED_BY_LTP:
            	simulationTime = 210.0 * 60.0;
            	break;
            default:
            	simulationTime = 5.0 * 60.0;
        }
        se.setCompletionTime(simulationTime);
        se.setTimeIncrement(1.0);
        se.setSolver(new JVodeSolver());
        diagramSbgn.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, se));
        
        setPlotsAmpa(diagramSbgn);
        
      	return diagramSbgn;
    }
    
    public static Diagram fillDiagramWithEquationsAmpa(Diagram diagram, String name) throws Exception
    {
    	String dName;
    	if (name.isEmpty())
    	{
    		String prefix = diagram.getName();
            String suffix = "receptor_AMPA_math_model";
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
    	
        BrainReceptorModel receptorModel = BrainUtils.getReceptorModelNodes(diagram).get(0).getRole(BrainReceptorModel.class);
        AmpaReceptorModelProperties receptorModelProperties = (AmpaReceptorModelProperties)receptorModel.getReceptorModelProperties();
        String regimeType = receptorModelProperties.getRegimeType();
        boolean portsFlag = receptorModelProperties.getPortsFlag();

        int yGroupOffset = 80;
        int yLocalOffset = 60;
        Point point = new Point(0, 0);
        
        String P1Formula = "-alpha_1*L*P1 + beta_1*Q1 - h_1/A_PSD*(P1 - R1)";
        BrainUtils.createEquation("Free_GluR12", "P1", P1Formula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String P2aFormula = "-alpha_2*L*P2a + beta_2a*Q2a - h_2a/A_PSD*(P2a - R2) + sigma_2/A_PSD + nu*P2b - mu*P2a";
        BrainUtils.createEquation("Free_GluR23a", "P2a", P2aFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String P2bFormula = "beta_2b*Q2b - h_2b/A_PSD*P2b - nu*P2b + mu*P2a";
        BrainUtils.createEquation("Free_GluR23b", "P2b", P2bFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String Q1Formula = "alpha_1*L*P1 - beta_1*Q1";
        BrainUtils.createEquation("Bound_GluR12", "Q1", Q1Formula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset - 15);
        
        String Q2aFormula = "alpha_2*L*P2a - beta_2a*Q2a + nu*Q2b - mu*Q2a";
        BrainUtils.createEquation("Bound_GluR23a", "Q2a", Q2aFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset - 15);
        
        String Q2bFormula = "-beta_2b*Q2b - nu*Q2b + mu*Q2a";
        BrainUtils.createEquation("Bound_GluR23b", "Q2b", Q2bFormula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset - 15);
        
        String R1Formula = "h_1/A_ESM*(P1 - R1) - omega_1/A_ESM*(R1 - D1) - k_1*R1 + sigma_1/A_ESM";
        BrainUtils.createEquation("GluR12_ESM", "R1", R1Formula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String R2Formula = "h_2a/A_ESM*(P2a - R2) - omega_2/A_ESM*(R2 - D2) - k_2*R2";
        BrainUtils.createEquation("GluR23_ESM", "R2", R2Formula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String S1Formula = "-kappa_1*S1 + delta_1";
        BrainUtils.createEquation("GluR12_intra", "S1", S1Formula,
                Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String lFormula = "L_total - Q1 - Q2a - Q2b";
        BrainUtils.createEquation("Concentration_of_free_binding_sites", "L", lFormula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String lTotalFormula = "c*(kappa_1*S1 - delta_1) -gamma*L";
        BrainUtils.createEquation("Concentration_of_binding_sites", "L_total", lTotalFormula,
    			Equation.TYPE_RATE, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String sigma1Formula = "kappa_1*S1";
        BrainUtils.createEquation("GluR12_exocytosis_rate", "sigma_1", sigma1Formula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 20);
        
        String sigma2Formula = "kappa_2*S2";
        BrainUtils.createEquation("GluR23_exocytosis_rate", "sigma_2", sigma2Formula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        
        point = new Point(600, 0);
        
        String totalPsdFormula = "(P1 + Q1 + P2a + P2b + Q2a + Q2b)*A_PSD";
        BrainUtils.createEquation("Total AMPA in PSD", "Total_PSD", totalPsdFormula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 20);
        
        String bound12PsdFormula = "Q1*A_PSD";
        BrainUtils.createEquation("Bound GluR1_2 in PSD", "Bound12_PSD", bound12PsdFormula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 20);
        
        String free12PsdFormula = "P1*A_PSD";
        BrainUtils.createEquation("Free GluR1_2 in PSD", "Free12_PSD", free12PsdFormula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 20);
        
        String bound23aPsdFormula = "Q2a*A_PSD";
        BrainUtils.createEquation("Bound GluR2_3_a in PSD", "Bound23a_PSD", bound23aPsdFormula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 20);
        
        String free23aPsdFormula = "P2a*A_PSD";
        BrainUtils.createEquation("Free GluR2_3_a in PSD", "Free23a_PSD", free23aPsdFormula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 20);
        
        String bound23bPsdFormula = "Q2b*A_PSD";
        BrainUtils.createEquation("Bound GluR2_3_a in PSD", "Bound23b_PSD", bound23bPsdFormula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 20);
        
        String free23bPsdFormula = "P2b*A_PSD";
        BrainUtils.createEquation("Free GluR2_3_b in PSD", "Free23b_PSD", free23bPsdFormula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 20);
        
        String BSitesPsdFormula = "L_total*A_PSD";
        BrainUtils.createEquation("Binding sites in PSD", "BSites_PSD", BSitesPsdFormula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset);
        
        String totalEsmFormula = "(R1 + R2a)*A_ESM";
        BrainUtils.createEquation("Total AMPA in ESM", "Total_ESM", totalEsmFormula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yLocalOffset - 20);
        
        String intra12Formula = "S1";
        BrainUtils.createEquation("Intracellular GluR1/2", "Intra12", intra12Formula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        point.translate(0, yGroupOffset - 20);
        
        
        point = new Point(1000, 0);
        
        String timeMinuteFormula = "time/60.0";
        BrainUtils.createEquation("time minute", "time_min", timeMinuteFormula,
    			Equation.TYPE_SCALAR, diagramEq, point);
        String timeHourFormula = "time_min/60.0";
        BrainUtils.createEquation("time hour", "time_hour", timeHourFormula,
    			Equation.TYPE_SCALAR, diagramEq, new Point(point.x + 120, point.y));
        point.translate(0, yGroupOffset);
        
        if (portsFlag)        	
        {   
        	EModel emodel = diagramEq.getRole(EModel.class);
        	
            BrainUtils.createEquation("Total_PSD_max", "Total_PSD_max", "92.75",
                    Equation.TYPE_INITIAL_ASSIGNMENT, diagramEq, point);
            BrainUtils.createEquation("Total_PSD_0", "Total_PSD_0", "Total_PSD",
                    Equation.TYPE_INITIAL_ASSIGNMENT, diagramEq, new Point(point.x + 150, point.y));
            BrainUtils.createEquation("Total_PSD_min", "Total_PSD_min", "19.0",
                    Equation.TYPE_INITIAL_ASSIGNMENT, diagramEq, new Point(point.x + 2 * 150, point.y));
            point.translate(0, yLocalOffset);
        	
            String interpolationFormula = "function interpolate(x1, x2, y1, y2, x) = (y2 - y1)*((x - x1)/(x2 - x1)) + y1";
            BrainUtils.createFunction("interpolate", interpolationFormula,
            		diagramEq, point);
            point.translate(0, yLocalOffset);
            
            String sfFormula = "piecewise( Total_PSD >= Total_PSD_0 => interpolate(Total_PSD_0, Total_PSD_max, 1.0, 1.2, Total_PSD); "
            		+ "interpolate(Total_PSD_min, Total_PSD_0, 0.8, 1.0, Total_PSD) )";
            BrainUtils.createEquation("equation_SF", "SF", sfFormula,
        			Equation.TYPE_SCALAR, diagramEq, point);
            point.translate(0, yGroupOffset + 10);
            
            BrainUtils.createPort("SF", "SF", Type.TYPE_OUTPUT_CONNECTION_PORT,
            		diagramEq, point);
            point.translate(0, yLocalOffset  - 30);
            
//            BrainUtils.createPort("time_start", "time_start", Type.TYPE_INPUT_CONNECTION_PORT,
//            		diagramEq, point);
//            point.translate(0, yLocalOffset - 30);
            BrainUtils.createPort("SR", "SR", Type.TYPE_INPUT_CONNECTION_PORT,
            		diagramEq, point);
            point.translate(0, yLocalOffset - 30);
            
            emodel.declareVariable("K_bath", 0.0);
            BrainUtils.createPort("K_bath", "K_bath", Type.TYPE_INPUT_CONNECTION_PORT,
            		diagramEq, point);
            point.translate(0, yLocalOffset - 30);
        }
        
        point = new Point(1600, 0);
        
        if (regimeType.equals(AmpaReceptorModelProperties.REGIME_LTD))
        {
        	BrainUtils.createEvent("Stop LTD", "time > 900.0", new Assignment[] {new Assignment("mu", "0.0"), new Assignment("gamma", "0.0")},
        			diagramEq, point);
            point.translate(0, yGroupOffset);
        }
        else if (regimeType.equals(AmpaReceptorModelProperties.REGIME_LTD_FOLLOWED_BY_LTP))
        {
        	BrainUtils.createEvent("Stop LTD 1", "time > 900.0", new Assignment[] {new Assignment("mu", "0.0"), new Assignment("gamma", "0.0")},
        			diagramEq, point);
            point.translate(0, yGroupOffset);
            
            BrainUtils.createEvent("Start LTD 2", "time > 3600.0", new Assignment[] {new Assignment("mu", "0.01"), new Assignment("gamma", "0.001")},
        			diagramEq, point);
            BrainUtils.createEvent("Stop LTD 2", "time > 4500.0", new Assignment[] {new Assignment("mu", "0.0"), new Assignment("gamma", "0.0")},
        			diagramEq, new Point(point.x + 150, point.y));
            point.translate(0, yGroupOffset);
            
            BrainUtils.createEvent("Start LTD 3", "time > 7200.0", new Assignment[] {new Assignment("mu", "0.01"), new Assignment("gamma", "0.001")},
        			diagramEq, point);
            BrainUtils.createEvent("Stop LTD 3", "time > 8100.0", new Assignment[] {new Assignment("mu", "0.0"), new Assignment("gamma", "0.0")},
         			diagramEq, new Point(point.x + 150, point.y));
            point.translate(0, yGroupOffset);
             
            BrainUtils.createEvent("Start LTP", "time > 10800.0", new Assignment[] {
            		new Assignment("alpha_1", "0.001"), 
            		new Assignment("kappa_1", "0.0556"),
            		new Assignment("h_1", "0.01"),
            		new Assignment("c", "0.325"),
            		new Assignment("gamma", "0.0")
            		},
          			diagramEq, new Point(point.x + 75, point.y));
            
            point.translate(0, yGroupOffset);
        }
        
        setInitialValuesAmpa(diagramEq, receptorModelProperties);
        
        //Simulator settings
        JavaSimulationEngine se = new JavaSimulationEngine();
        se.setDiagram(diagramEq);
        double simulationTime;
        switch (regimeType)
        {
            case AmpaReceptorModelProperties.REGIME_NORMAL:
            	simulationTime = 30.0 * 60.0;
            	break;
            case AmpaReceptorModelProperties.REGIME_LTP:
            	simulationTime = 5.0 * 60.0;
            	break;
            case AmpaReceptorModelProperties.REGIME_LTD:
                simulationTime = 30.0 * 60.0;
                break;
            case AmpaReceptorModelProperties.REGIME_LTD_FOLLOWED_BY_LTP:
            	simulationTime = 210.0 * 60.0;
            	break;
            default:
            	simulationTime = 5.0 * 60.0;
        }
        se.setCompletionTime(simulationTime);
        se.setTimeIncrement(1.0);
        se.setSolver(new JVodeSolver());
        diagramEq.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, se));
        
        setPlotsAmpa(diagramEq);
        
      	return diagramEq;
    }
    
    protected static SpecieReference createSpeciesReference(String variable, String role)
    {
        SpecieReference specieReference = new SpecieReference(null, variable, role);
        specieReference.setSpecie(variable);
        return specieReference;
    } 
    
    private static void setInitialValuesAmpa(Diagram diagram, AmpaReceptorModelProperties receptorModelProperties) 
    {
    	// parameter values
    	double lTotal = receptorModelProperties.getLTotal();
        double alpha1 = receptorModelProperties.getAlpha1();
        double alpha2 = receptorModelProperties.getAlpha2();
        double beta1 = receptorModelProperties.getBeta1();
        double beta2a = receptorModelProperties.getBeta2a();
        double beta2b = receptorModelProperties.getBeta2b();
        double c = receptorModelProperties.getC();
        double delta1 = receptorModelProperties.getDelta1();
        double gamma = receptorModelProperties.getGamma();
        double h1 = receptorModelProperties.getH1();
        double h2a = receptorModelProperties.getH2a();
        double h2b = receptorModelProperties.getH2b();
        double k1 = receptorModelProperties.getK1();
        double k2 = receptorModelProperties.getK2();
        double kappa1 = receptorModelProperties.getKappa1();
        double kappa2 = receptorModelProperties.getKappa2();
        double mu = receptorModelProperties.getMu();
        double nu = receptorModelProperties.getNu();
        double omega1 = receptorModelProperties.getOmega1();
        double omega2 = receptorModelProperties.getOmega2();
        double sigma1 = receptorModelProperties.getSigma1();
        double sigma2 = receptorModelProperties.getSigma2();
        double aPsd = 0.1257;
        double aEsm = 1.257;
        
        BrainUtils.setInitialValue(diagram, "L_total", lTotal);
        BrainUtils.setInitialValue(diagram, "alpha_1", alpha1);
        BrainUtils.setInitialValue(diagram, "alpha_2", alpha2);
        BrainUtils.setInitialValue(diagram, "beta_1", beta1);
        BrainUtils.setInitialValue(diagram, "beta_2a", beta2a);
        BrainUtils.setInitialValue(diagram, "beta_2b", beta2b);
        BrainUtils.setInitialValue(diagram, "c", c);
        BrainUtils.setInitialValue(diagram, "delta_1", delta1);
        BrainUtils.setInitialValue(diagram, "gamma", gamma);
        BrainUtils.setInitialValue(diagram, "h_1", h1);
        BrainUtils.setInitialValue(diagram, "h_2a", h2a);
        BrainUtils.setInitialValue(diagram, "h_2b", h2b);
        BrainUtils.setInitialValue(diagram, "k_1", k1);
        BrainUtils.setInitialValue(diagram, "k_2", k2);
        BrainUtils.setInitialValue(diagram, "kappa_1", kappa1);
        BrainUtils.setInitialValue(diagram, "kappa_2", kappa2);
        BrainUtils.setInitialValue(diagram, "mu", mu);
        BrainUtils.setInitialValue(diagram, "nu", nu);
        BrainUtils.setInitialValue(diagram, "omega_1", omega1);
        BrainUtils.setInitialValue(diagram, "omega_2", omega2);
        BrainUtils.setInitialValue(diagram, "sigma_1", sigma1);
        BrainUtils.setInitialValue(diagram, "sigma_2", sigma2);
        BrainUtils.setInitialValue(diagram, "A_PSD", aPsd);
        BrainUtils.setInitialValue(diagram, "A_ESM", aEsm);
        
        // initial variable values
        double P1 = 13.0;
        double P2a = 140.0;
        double P2b = 0.0;
        double Q1 = 0.0;
        double Q2a = 160.0;
        double Q2b = 0.0;
        double R1 = 13.0;
        double R2 = 7.5;
        double D1 = 0.0;
        double D2 = 10.0;
        double S1 = 500.0;
        double S2 = 100.0;
        
    	BrainUtils.setInitialValue(diagram, "P1", P1);
    	BrainUtils.setInitialValue(diagram, "P2a", P2a);
    	BrainUtils.setInitialValue(diagram, "P2b", P2b);
    	BrainUtils.setInitialValue(diagram, "Q1", Q1);
    	BrainUtils.setInitialValue(diagram, "Q2a", Q2a);
    	BrainUtils.setInitialValue(diagram, "Q2b", Q2b);
    	BrainUtils.setInitialValue(diagram, "R1", R1);
    	BrainUtils.setInitialValue(diagram, "R2", R2);
    	BrainUtils.setInitialValue(diagram, "D1", D1);
    	BrainUtils.setInitialValue(diagram, "D2", D2);
    	BrainUtils.setInitialValue(diagram, "S1", S1);
    	BrainUtils.setInitialValue(diagram, "S2", S2);
    }

    private static void setPlotsAmpa(Diagram diagram) 
    {    	
    	EModel emodel = diagram.getRole(EModel.class);
    	PlotsInfo plotsInfo = new PlotsInfo(emodel);
    	
        PlotInfo varPlotsPsd = new PlotInfo();
        varPlotsPsd.setTitle("Receptors in PSD");
        varPlotsPsd.setXTitle("time (min)");
        varPlotsPsd.setYTitle("number of receptors");
        
        PlotInfo varPlotsEsmIntra = new PlotInfo();
        varPlotsEsmIntra.setTitle("Receptors in ESM and intracellular store");
        varPlotsEsmIntra.setXTitle("time (min)");
        varPlotsEsmIntra.setYTitle("number of receptors");
        
        plotsInfo.setPlots(new PlotInfo[] {varPlotsPsd, varPlotsEsmIntra});

        PlotVariable timeVariable = new PlotVariable("", "time_min", "time_min", emodel);
        
        List<Curve> curvesPsd = new ArrayList<Curve>();
        curvesPsd.add(new Curve("", "Total_PSD", "Total in PSD", emodel));
        curvesPsd.add(new Curve("", "Bound12_PSD", "Bound GluR1/2", emodel));
        curvesPsd.add(new Curve("", "Free12_PSD", "Free GluR1/2", emodel));
        curvesPsd.add(new Curve("", "Bound23a_PSD", "Bound GluR2/3/GRIP", emodel));
        curvesPsd.add(new Curve("", "Free23a_PSD", "Free GluR2/3/GRIP", emodel));
        curvesPsd.add(new Curve("", "Bound23b_PSD", "Bound GluR2/3/PICK", emodel));
        curvesPsd.add(new Curve("", "Free23b_PSD", "Free GluR2/3/PICK", emodel));
        curvesPsd.add(new Curve("", "BSites_PSD", "Binding sites", emodel));
        
        List<Curve> curvesEsmIntra = new ArrayList<Curve>();
        curvesEsmIntra.add(new Curve("", "Total_ESM", "Total in ESM", emodel));
        curvesEsmIntra.add(new Curve("", "Intra12", "Intracellular GluR1/2", emodel));
 
        varPlotsPsd.setXVariable(timeVariable);
        varPlotsPsd.setYVariables(curvesPsd.stream().toArray(Curve[]::new));
        varPlotsPsd.setYFrom(0.0);
        varPlotsPsd.setYTo(100.0);
        
        varPlotsEsmIntra.setXVariable(timeVariable);
        varPlotsEsmIntra.setYVariables(curvesEsmIntra.stream().toArray(Curve[]::new));
        varPlotsEsmIntra.setYFrom(0.0);
        varPlotsEsmIntra.setYTo(500.0);
        
        DiagramUtility.setPlotsInfo(diagram, plotsInfo);
    }
}