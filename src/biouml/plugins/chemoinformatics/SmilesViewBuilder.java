package biouml.plugins.chemoinformatics;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.beans.PropertyChangeEvent;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.DiagramViewOptions;
import biouml.model.Node;
import biouml.model.NodeViewBuilder;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.standard.diagram.PathwayDiagramType;
import biouml.standard.type.CDKRenderer;
import biouml.standard.type.Structure;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.CompositeView;

public class SmilesViewBuilder extends NodeViewBuilder
{
    public static final String SMILES_STRUCTURE = "smilesStructure";
    private JavaScriptCDK helper = new JavaScriptCDK();



    @Override
    public @Nonnull CompositeView createNodeView(Node node, DiagramViewOptions options, Graphics g)
    {
        try
        {
            String formula = node.getAttributes().getValueAsString(SMILES_STRUCTURE);
            Structure structure = helper.fromSMILES(formula);
            CompositeView view = CDKRenderer.createStructureView(structure, structure.getImageSize(), (Graphics2D)g);
            view.setModel(node);
            view.setActive(true);

            view.setLocation(node.getLocation());
            node.setView(view);
            return view;
        }
        catch( Exception e )
        {
            ExceptionRegistry.log(e);
            return null;
        }
    }


    @Override
    public boolean isApplicable(Node node)
    {
        String formula = node.getAttributes().getValueAsString(SMILES_STRUCTURE);
        if( formula != null )
            return true;

        return false;
    }

    @Override
    public boolean isApplicable(Diagram diagram)
    {
        DiagramType type = diagram.getType();
        if( type == null )
            return false;
        if( type instanceof PathwayDiagramType || type instanceof SbgnDiagramType )
            return true;
        return false;
    }


    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( evt.getPropertyName().equals("attributes/" + SMILES_STRUCTURE) && evt.getSource() instanceof Node )
        {
            Node node = (Node)evt.getSource();
            if( !isApplicable(node) )
            {
                DynamicProperty dp = node.getAttributes().getProperty(SMILES_STRUCTURE);
                if( dp != null )
                    dp.setValue(evt.getOldValue());
            }
        }
    }
}
