package ru.biosoft.analysis.diagram;

import ru.biosoft.util.ConstantResourceBundle;

/**
 * @author anna
 */
public class MessageBundle extends ConstantResourceBundle
{
    /* DiagramExtensionAnalysis constants */
    public static final String CN_CLASS = "Extend diagram";
    public static final String CD_CLASS = "Extend diagram";
    
    public static final String PN_INPUT_DIAGRAM = "Diagram";
    public static final String PD_INPUT_DIAGRAM = "Input the diagram to enrich";
    public static final String PN_OUTPUT_DIAGRAM = "Output name";
    public static final String PD_OUTPUT_DIAGRAM = "Output diagram name";
    public static final String PN_ITERATION = "Steps";
    public static final String PD_ITERATION = "Specify the number of extension steps";
    public static final String PN_REACTIONS_ONLY = "Add only reactions";
    public static final String PD_REACTIONS_ONLY = "Do not add other reaction participants not presented on diagram";
    
    
    /* JoinDiagram analysis constants */
    public static final String CN_JD_CLASS = "Join diagrams";
    public static final String CD_JD_CLASS = "Join diagrams";
    
    public static final String PN_INPUT_DIAGRAMS = "Diagrams";
    public static final String PD_INPUT_DIAGRAMS = "List of diagrams to join";
    
    public static final String PN_LAYOUTER = "Layouter";
    public static final String PD_LAYOUTER = "Use selected layouter to layout joined diagram";
}
