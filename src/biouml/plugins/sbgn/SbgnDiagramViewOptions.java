package biouml.plugins.sbgn;

import java.awt.Color;
import java.awt.Dimension;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.ColorUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.standard.diagram.CompositeDiagramViewOptions;

/**
 * 
 * @author Ilya
 * For the most part copied from "sbml-sbgn.xml" notation
 */
@SuppressWarnings ( "serial" )
public class SbgnDiagramViewOptions extends CompositeDiagramViewOptions
{
    public static final int MINIMAL_REACTION_SIZE = 15;  

    private boolean hideDegradation = false;
    private boolean addSourceSink = true;
    private boolean orientedReactions = false;
    private boolean autoDeleteReactions = false;
    private boolean autoMoveReactions = false;
    private boolean shrinkNodeTitleSize = false;
    
    private int logicalSize = 30;
    private int reactionSize = 15;
    
    private ColorFont portTitleFont = new ColorFont("Arial", 0, 18, ColorUtils.parseColor("#7A378B"));
    private ColorFont cloneFont = new ColorFont("Arial", 0, 12, Color.white);
    private ColorFont customTitleFont = new ColorFont("Arial", 1, 18, Color.black);
    private Dimension sourceSinkSize = new Dimension(30,30);
    private final Dimension unitOfInformationSize = new Dimension(20, 20);
    private final Dimension variableSize = new Dimension(20, 20);
    
    private Brush inputPortBrush = new Brush(Color.green);
    private Brush outputPortBrush = new Brush(Color.red);
    private Brush contactPortBrush = new Brush(Color.gray);
    private Brush sourceSinkBrush = new Brush(ColorUtils.parseColor("#E15064"));  
    private Brush phenotypeBrush = new Brush(ColorUtils.parsePaint("#F0F0FF:#C8B4FF"));
    private Brush edgeTipBrush = new Brush(ColorUtils.parseColor("#787878"));
    private Brush complexBrush = new Brush(ColorUtils.parsePaint("#5ABEC8:#3CB4BE"));
    private Brush cloneBrush = new Brush(Color.gray);
    private Brush macromoleculeBrush = new Brush( ColorUtils.parsePaint( "#EEFCE3:#B7E884" ) );
    private Brush nucleicBrush = new Brush(ColorUtils.parsePaint("#B4FFFF:#A0F0F0"));
    private Brush perturbingBrush = new Brush(ColorUtils.parsePaint("#FFB4FF:#F0A0F0"));
    private Brush unspecifiedBrush = new Brush(ColorUtils.parsePaint("#F5E164:#F5C80A"));
    private Brush simpleChemicalBrush = new Brush(ColorUtils.parsePaint("#FFEDF5:#BE5A96:45"));
    private Brush compartmentBrush = new Brush(ColorUtils.parseColor("#F0F0F0"));

    private final Brush unitOfInformationBrush = new Brush(Color.white);

    private Pen edgePen = new Pen(1, ColorUtils.parseColor("#646464"));
    private Pen compartmentPen = new Pen(4, ColorUtils.parseColor("#969696"));
    
    private boolean bioUMLPorts = false;


    public SbgnDiagramViewOptions()
    {
        super(null);
        showReactionName = false;
        autoLayout = true;
        noteLinkPen = new Pen();
    }
    
    @PropertyName("Macromolecule brush")
    @PropertyDescription("Macromolecule brush.")
    public Brush getMacromoleculeBrush()
    {
        return macromoleculeBrush;
    }

    public void setMacromoleculeBrush(Brush macromoleculeBrush)
    {
        Object oldValue = this.macromoleculeBrush;
        this.macromoleculeBrush = macromoleculeBrush;
        firePropertyChange("macromoleculeBrush", oldValue, macromoleculeBrush);
    }

    @PropertyName("Nucleic acid brush")
    @PropertyDescription("Nucleic acid feature brush.")
    public Brush getNucleicBrush()
    {
        return nucleicBrush;
    }

    public void setNucleicBrush(Brush nucleicBrush)
    {
        Object oldValue = this.nucleicBrush;
        this.nucleicBrush = nucleicBrush;
        firePropertyChange("nucleicBrush", oldValue, nucleicBrush);
    }

    @PropertyName("Perturbing agent brush")
    @PropertyDescription("Perturbing agent brush.")
    public Brush getPerturbingBrush()
    {
        return perturbingBrush;
    }

    public void setPerturbingBrush(Brush perturbingBrush)
    {
        Object oldValue = this.perturbingBrush;
        this.perturbingBrush = perturbingBrush;
        firePropertyChange("perturbingBrush", oldValue, perturbingBrush);
    }

    @PropertyName("Unspecified brush")
    @PropertyDescription("Unspecified entity brush.")
    public Brush getUnspecifiedBrush()
    {
        return unspecifiedBrush;
    }

    public void setUnspecifiedBrush(Brush unspecifiedBrush)
    {
        Object oldValue = this.unspecifiedBrush;
        this.unspecifiedBrush = unspecifiedBrush;
        firePropertyChange("unspecifiedBrush", oldValue, unspecifiedBrush);
    }

    @PropertyName("Simple chemical brush")
    @PropertyDescription("Simple chemical brush.")
    public Brush getSimpleChemicalBrush()
    {
        return simpleChemicalBrush;
    }

    public void setSimpleChemicalBrush(Brush simpleChemicalBrush)
    {
        Object oldValue = this.simpleChemicalBrush;
        this.simpleChemicalBrush = simpleChemicalBrush;
        firePropertyChange("simpleChemicalBrush", oldValue, simpleChemicalBrush);
    }

    @PropertyName("Compartment pen")
    @PropertyDescription("Pen for compartment border.")
    public Pen getCompartmentPen()
    {
        return compartmentPen;
    }

    public void setCompartmentPen(Pen compartmentPen)
    {
        Object oldValue = this.compartmentPen;
        this.compartmentPen = compartmentPen;
        firePropertyChange("compartmentPen", oldValue, compartmentPen);
    }

    @PropertyName("Compartment brush")
    @PropertyDescription("Pen for compartment border.")
    public Brush getCompartmentBrush()
    {
        return compartmentBrush;
    }

    public void setCompartmentBrush(Brush compartmentBrush)
    {
        Object oldValue = this.compartmentBrush;
        this.compartmentBrush = compartmentBrush;
        firePropertyChange("compartmentBrush", oldValue, compartmentBrush);
    }

    public Dimension getVariableSize()
    {
        return variableSize;
    }

    private final Brush variableBrush = new Brush(Color.white);
    public Brush getVariableBrush()
    {
        return variableBrush;
    }

    public Dimension getUnitOfInformationSize()
    {
        return unitOfInformationSize;
    }

    public Brush getUnitOfInformationBrush()
    {
        return unitOfInformationBrush;
    }

    @PropertyName("Input port brush")
    @PropertyDescription("Input port brush.")
    public Brush getInputPortBrush()
    {
        return inputPortBrush;
    }

    public void setInputPortBrush(Brush inputPortBrush)
    {
        Object oldValue = this.inputPortBrush;
        this.inputPortBrush = inputPortBrush;
        firePropertyChange("inputPortBrush", oldValue, inputPortBrush);
    }

    @PropertyName("Output port brush")
    @PropertyDescription("Output port brush.")
    public Brush getOutputPortBrush()
    {
        return outputPortBrush;
    }

    public void setOutputPortBrush(Brush outputPortBrush)
    {
        Object oldValue = this.outputPortBrush;
        this.outputPortBrush = outputPortBrush;
        firePropertyChange("outputPortBrush", oldValue, outputPortBrush);
    }

    @PropertyName("Contact port brush")
    @PropertyDescription("Contact port brush.")
    public Brush getContactPortBrush()
    {
        return contactPortBrush;
    }

    public void setContactPortBrush(Brush contactPortBrush)
    {
        Object oldValue = this.contactPortBrush;
        this.contactPortBrush = contactPortBrush;
        firePropertyChange("contactPortBrush", oldValue, contactPortBrush);
    }

    @PropertyName("Port title font")
    @PropertyDescription("Port title font.")
    public ColorFont getPortTitleFont()
    {
        return portTitleFont;
    }

    public void setPortTitleFont(ColorFont portTitleFont)
    {
        Object oldValue = this.portTitleFont;
        this.portTitleFont = portTitleFont;
        firePropertyChange("portTitleFont", oldValue, portTitleFont);
    }

    @PropertyName("Complex brush")
    @PropertyDescription("Complex brush.")
    public Brush getComplexBrush()
    {
        return complexBrush;
    }

    public void setComplexBrush(Brush complexBrush)
    {
        Object oldValue = this.complexBrush;
        this.complexBrush = complexBrush;
        firePropertyChange("complexBrush", oldValue, complexBrush);
    }

    @PropertyName("Clone label font")
    @PropertyDescription("Clone label font.")
    public ColorFont getCloneFont()
    {
        return cloneFont;
    }

    public void setCloneFont(ColorFont cloneFont)
    {
        Object oldValue = this.cloneFont;
        this.cloneFont = cloneFont;
        firePropertyChange("cloneFont", oldValue, cloneFont);
    }

    @PropertyName("Clone brush")
    @PropertyDescription("Clone marker brush.")
    public Brush getCloneBrush()
    {
        return cloneBrush;
    }

    public void setCloneBrush(Brush cloneBrush)
    {
        Object oldValue = this.cloneBrush;
        this.cloneBrush = cloneBrush;
        firePropertyChange("cloneBrush", oldValue, cloneBrush);
    }

    @PropertyName("Node title font")
    @PropertyDescription("Node title font.")
    public ColorFont getCustomTitleFont()
    {
        return customTitleFont;
    }

    public void setCustomTitleFont(ColorFont customTitleFont)
    {
        Object oldValue = this.customTitleFont;
        this.customTitleFont = customTitleFont;
        firePropertyChange("customTitleFont", oldValue, customTitleFont);
    }
    
    @PropertyName("Aux nodes title font")
    @PropertyDescription("Title font for aux nodes: variable, info, note, etc.. .")
    @Override
    public ColorFont getNodeTitleFont()
    {
        return nodeTitleFont;
    }

    @Override
    public void setNodeTitleFont(ColorFont nodeTitleFont)
    {
        Object oldValue = this.nodeTitleFont;
        this.nodeTitleFont = nodeTitleFont;
        firePropertyChange("nodeTitleFont", oldValue, nodeTitleFont);
    }

    @PropertyName("Source/sink brush")
    @PropertyDescription("Source/sink brush.")
    public Brush getSourceSinkBrush()
    {
        return sourceSinkBrush;
    }

    public void setSourceSinkBrush(Brush sourceSinkBrush)
    {
        Object oldValue = this.sourceSinkBrush;
        this.sourceSinkBrush = sourceSinkBrush;
        firePropertyChange("sourceSinkBrush", oldValue, sourceSinkBrush);
    }

    @PropertyName("Source/sink size")
    @PropertyDescription("Source/sink size.")
    public Dimension getSourceSinkSize()
    {
        return sourceSinkSize;
    }

    public void setSourceSinkSize(Dimension sourceSinkSize)
    {
        Object oldValue = this.sourceSinkSize;
        this.sourceSinkSize = sourceSinkSize;
        firePropertyChange("sourceSinkSize", oldValue, sourceSinkSize);
    }

    @PropertyName("Phenotype brush")
    @PropertyDescription("Phenotype brush.")
    public Brush getPhenotypeBrush()
    {
        return phenotypeBrush;
    }

    public void setPhenotypeBrush(Brush phenotypeBrush)
    {
        Object oldValue = this.phenotypeBrush;
        this.phenotypeBrush = phenotypeBrush;
        firePropertyChange("phenotypeBrush", oldValue, phenotypeBrush);
    }

    @PropertyName("Oriented reactions")
    @PropertyDescription("If true then reaction will have orientation (according to SBGN spec).")
    public boolean isOrientedReactions()
    {
        return orientedReactions;
    }

    public void setOrientedReactions(boolean orientedReactions)
    {
        Object oldValue = this.orientedReactions;
        this.orientedReactions = orientedReactions;
        firePropertyChange("orientedReactions", oldValue, orientedReactions);
    }

    @PropertyName("Autodelete reactions")
    @PropertyDescription("Delete whole reaction when any participant is deleted.")
    public boolean isAutoDeleteReactions()
    {
        return autoDeleteReactions;
    }

    public void setAutoDeleteReactions(boolean autodeleteReactions)
    {
        Object oldValue = this.autoDeleteReactions;
        this.autoDeleteReactions = autodeleteReactions;
        firePropertyChange("autodeleteReactions", oldValue, autodeleteReactions);
    }

    @PropertyName("Automove reactions")
    @PropertyDescription("Move reactions along with reaction participants.")
    public boolean isAutoMoveReactions()
    {
        return autoMoveReactions;
    }

    public void setAutoMoveReactions(boolean autoMoveReactions)
    {
        Object oldValue = this.autoMoveReactions;
        this.autoMoveReactions = autoMoveReactions;
        firePropertyChange("autoMoveReactions", oldValue, autoMoveReactions);
    }

    @PropertyName ( "Shrink node title" )
    @PropertyDescription ( "Shrink title of the node to its size (cut and add '...' if title is too long)." )
    public boolean isShrinkNodeTitleSize()
    {
        return shrinkNodeTitleSize;
    }

    public void setShrinkNodeTitleSize(boolean shrinkNodeTitleSize)
    {
        boolean oldValue = this.shrinkNodeTitleSize;
        this.shrinkNodeTitleSize = shrinkNodeTitleSize;
        firePropertyChange( "shrinkNodeTitleSize", oldValue, shrinkNodeTitleSize );
    }

    @PropertyName("Edge tip brush")
    @PropertyDescription("Edge tip brush.")
    public Brush getEdgeTipBrush()
    {
        return edgeTipBrush;
    }

    public void setEdgeTipBrush(Brush edgeTipBrush)
    {
        Object oldValue = this.edgeTipBrush;
        this.edgeTipBrush = edgeTipBrush;
        firePropertyChange("edgeTipBrush", oldValue, edgeTipBrush);
    }

    @PropertyName("Edge pen")
    @PropertyDescription("Pen for edges.")
    public Pen getEdgePen()
    {
        return edgePen;
    }

    public void setEdgePen(Pen edgePen)
    {
        Object oldValue = this.edgePen;
        this.edgePen = edgePen;
        firePropertyChange("edgePen", oldValue, edgePen);
    }

    @PropertyName("Add source/sink")
    @PropertyDescription("Automatically add sources and sinks to reaction without reactants or products.")
    public boolean isAddSourceSink()
    {
        return addSourceSink;
    }

    public void setAddSourceSink(boolean addSourceSink)
    {
        Object oldValue = this.addSourceSink;
        this.addSourceSink = addSourceSink;
        firePropertyChange("addSourceSink", oldValue, addSourceSink);
    }

    public int getLogicalSize()
    {
        return logicalSize;
    }

    public int getReactionSize()
    {
        return reactionSize;
    }
    
    public void setReactionSize(int size)
    {
        if (size < MINIMAL_REACTION_SIZE)
            return;
        Object oldValue = this.reactionSize;
        this.reactionSize = size;
        firePropertyChange("reactionSize", oldValue, reactionSize);
    }

    public boolean isHideDegradation()
    {
        return hideDegradation;
    }

    public void setHideDegradation(boolean hideDegradation)
    {
        Object oldValue = this.hideDegradation;
        this.hideDegradation = hideDegradation;
        firePropertyChange("hideDegradation", oldValue, hideDegradation);
    }

    @PropertyName ( "BioUML style Ports" )
    @PropertyDescription ( "BioUML style Ports." )
    public boolean isBioUMLPorts()
    {
        return bioUMLPorts;
    }

    public void setBioUMLPorts(boolean bioUMLPorts)
    {
        Object oldValue = this.bioUMLPorts;
        this.bioUMLPorts = bioUMLPorts;
        firePropertyChange( "bioUMLPorts", oldValue, bioUMLPorts );
    }
}
