package biouml.plugins.bionetgen.diagram;

import java.beans.PropertyChangeEvent;

import javax.annotation.CheckForNull;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.graph.Graph;
import ru.biosoft.graphics.editor.SelectionManager;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.bionetgen.bnglparser.BNGStart;
import biouml.plugins.bionetgen.bnglparser.BionetgenParser;
import biouml.workbench.graph.DiagramToGraphTransformer;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;

public class Bionetgen
{
    private static final String TITLE_PROPERTY = "title";
    private static final String REVERSIBLE_PROPERTY = "reversible";
    private static final String COMMENT_PROPERTY = "comment";
    private static final String FORMULA_PROPERTY = "formula";
    private static final String VARIABLE_PROPERTY = "variable";
    private static final String CONSTANT_PROPERTY = "constant";
    private static final String INITIAL_VALUE_PROPERTY = "initialValue";

    protected Logger log = Logger.getLogger(Bionetgen.class.getName());

    protected Diagram diagram;
    protected BNGStart bngStart;
    protected BionetgenAstUpdater diagramToAST;

    public final static String BIONETGEN_ATTR = "Bionetgen";
    public final static String BIONETGEN_LINK = "Link with bionetgen";

    public Bionetgen(Diagram diagram)
    {
        this.diagram = diagram;
        this.diagramToAST = new BionetgenAstUpdater();
        diagramToAST.setDiagram(diagram);
        if( diagram != null )
            bngStart = diagramToAST.getAST(diagram);
        else
            bngStart = new BNGStart(BionetgenParser.JJTSTART);
    }

    public Diagram generateDiagram(BNGStart start) throws Exception
    {
        Diagram newDiagram = BionetgenDiagramGenerator.generateDiagram( start, diagram, diagram == null || diagram.getSize() == 0 );
        if( diagram != null && newDiagram != null )
        {
            reApplyLayout(diagram, newDiagram);
        }
        else
        {
            log.log(Level.SEVERE, "Diagram is not ready");
        }
        this.diagram = newDiagram;
        diagramToAST.setDiagram(newDiagram);
        bngStart = start;
        return newDiagram;
    }

    public Diagram generateDiagram(String text) throws Exception
    {
        BNGStart start = BionetgenAstCreator.generateAstFromText(text);
        Diagram newDiagram = generateDiagram(start);
        diagram = newDiagram;
        diagramToAST.setDiagram(newDiagram);
        bngStart = start;
        return newDiagram;
    }

    public String generateText()
    {
        return new BionetgenTextGenerator(bngStart).generateText();
    }

    private void reApplyLayout(Diagram diagramFrom, Diagram diagramTo)
    {
        Graph oldGraph = DiagramToGraphTransformer.generateGraph(diagramFrom, diagramFrom.getType().getSemanticController().getFilter());
        Graph graph = DiagramToGraphTransformer.generateGraph(diagramTo, diagramTo.getType().getSemanticController().getFilter());
        DiagramToGraphTransformer.reApplyLayout(oldGraph, graph);
        DiagramToGraphTransformer.applyLayout(graph, diagramTo);
        for( DiagramElement de : diagramTo )
        {
            if( de instanceof Edge && !de.isFixed() )
                diagramTo.getType().getSemanticController().recalculateEdgePath((Edge)de);
            de.setFixed(false);
        }
        diagramTo.setPathLayouter( diagramFrom.getPathLayouter() );
    }

    public String updateText(DataCollectionEvent e) throws Exception
    {
        DataElement de;
        switch( e.getType() )
        {
            case DataCollectionEvent.ELEMENT_ADDED:
                de = e.getDataElement();
                if( de instanceof DiagramElement )
                    diagramToAST.addElement(bngStart, (DiagramElement)de);
                else if( de instanceof Variable )
                    diagramToAST.addElement(bngStart, (Variable)de);
                break;
            case DataCollectionEvent.ELEMENT_REMOVED:
                de = e.getOldElement();
                if( de instanceof DiagramElement )
                    diagramToAST.removeElement(bngStart.getModel(), (DiagramElement)de);
                else if( de instanceof Variable )
                    diagramToAST.removeElement((Variable)de);
                break;
            default:
                break;
        }
        return generateText();
    }

    public String updateText(PropertyChangeEvent e) throws Exception
    {
        String propertyName = e.getPropertyName();
        if( INITIAL_VALUE_PROPERTY.equals(propertyName) )
        {
            Variable var = (Variable)e.getSource();
            diagramToAST.changeInitialValue(var, bngStart.getModel());
        }
        else if( CONSTANT_PROPERTY.equals(propertyName) )
        {
            if( e.getSource() instanceof VariableRole )
            {
                Node node = (Node) ( (VariableRole)e.getSource() ).getDiagramElement();
                diagramToAST.changeSeedSpeciesConstancy(node, Boolean.parseBoolean(e.getNewValue().toString()));
            }
        }
        else if( VARIABLE_PROPERTY.equals(propertyName) )
        {
            if( e.getSource() instanceof Equation )
                diagramToAST.changeVariableName(e, bngStart.getModel());
        }
        else if( FORMULA_PROPERTY.equals(propertyName) )
        {
            if( e.getSource() instanceof Equation )
            {
                DiagramElement de = ( (Equation)e.getSource() ).getDiagramElement();
                diagramToAST.changeFormula(de);
            }
        }
        else if( COMMENT_PROPERTY.equals(propertyName) )
        {
            Option de = (Option)e.getSource();
            diagramToAST.changeComment(de, e.getNewValue().toString(), bngStart);
        }
        else if( REVERSIBLE_PROPERTY.equals(propertyName) )
        {
            diagramToAST.changeReversible((Node)e.getSource(), Boolean.valueOf(e.getNewValue().toString()));
        }
        else if( propertyName != null && propertyName.startsWith("attributes/") )
        {
            String attrName = propertyName.substring("attributes/".length());
            if( BionetgenConstants.GRAPH_ATTR.equals(attrName) )
            {
                DiagramElement de = (DiagramElement)e.getSource();
                diagramToAST.changeGraph(de, e.getOldValue().toString(), e.getNewValue().toString(), bngStart.getModel());
            }
            else if( BionetgenConstants.MOLECULE_ATTR.equals(attrName) )
            {
                DiagramElement de = (DiagramElement)e.getSource();
                diagramToAST.changeMolecule(de, e.getOldValue().toString(), e.getNewValue().toString(), bngStart.getModel());
            }
            else if( BionetgenConstants.MATCH_ONCE_ATTR.equals(attrName) )
            {
                DiagramElement de = (DiagramElement)e.getSource();
                diagramToAST.changeMatchOnce(de, Boolean.valueOf(e.getNewValue().toString()));
            }
            else if( BionetgenConstants.CONTENT_ATTR.equals(attrName) )
            {
                Node node = (Node)e.getSource();
                diagramToAST.changeContentAttribute(node, (String[])e.getNewValue());
            }
            else if( BionetgenConstants.ADDITION_ATTR.equals(attrName) )
            {
                Node node = (Node)e.getSource();
                if( BionetgenUtils.isReaction(node) )
                    diagramToAST.changeAdditionAttribute(node, (String[])e.getNewValue());
            }
            else if( BionetgenConstants.REVERSIBLE_ATTR.equals(attrName) )
            {
                Node node = (Node)e.getSource();
                if( BionetgenUtils.isReaction(node) )
                    diagramToAST.changeReversible(node, Boolean.valueOf(e.getNewValue().toString()));
            }
            else if( BionetgenConstants.FORWARD_RATE_ATTR.equals(attrName) || BionetgenConstants.BACKWARD_RATE_ATTR.equals(attrName)
                    || BionetgenConstants.RATE_LAW_TYPE_ATTR.equals(attrName) )
            {
                Node node = (Node)e.getSource();
                if( BionetgenUtils.isReaction(node) )
                    diagramToAST.changeRateLaw(node, attrName, e);
            }
            else if( BionetgenConstants.GENERATE_NETWORK_ATTR.equals(attrName) || BionetgenConstants.SIMULATE_ODE_ATTR.equals(attrName)
                    || BionetgenConstants.SIMULATE_SSA_ATTR.equals(attrName) || BionetgenConstants.SIMULATE_ATTR.equals(attrName) )
            {
                Diagram diagram = (Diagram)e.getSource();
                diagramToAST.changeActionAttribute(diagram, (DynamicProperty)e.getNewValue());
            }
            else if( BionetgenConstants.IS_SEED_SPECIES_ATTR.equals( attrName ) )
            {
                Node node = (Node)e.getSource();
                diagramToAST.changeTypeAttribute( node, Boolean.valueOf( e.getOldValue().toString() ),
                        Boolean.valueOf( e.getNewValue().toString() ), bngStart.getModel() );
            }
            else if( BionetgenConstants.MOLECULE_TYPE_ATTR.equals(attrName) )
            {
                Node node = (Node)e.getSource();
                if( BionetgenUtils.isMoleculeType(node) )
                    diagramToAST.changeMoleculeType(node, e.getOldValue().toString(), e.getNewValue().toString(), bngStart.getModel());
            }
            else if( BionetgenConstants.LABEL_ATTR.equals(attrName) )
            {
                if( e.getSource() instanceof Node )
                    diagramToAST.changeLabel( (Node)e.getSource(), e.getNewValue().toString() );
            }
        }
        else if( TITLE_PROPERTY.equals(propertyName) )
        {
            DiagramElement de = (DiagramElement)e.getSource();
            if( BionetgenUtils.isMoleculeComponent(de) )
                diagramToAST.changeMoleculeComponent(de, e.getOldValue().toString(), bngStart.getModel());
            else if( BionetgenUtils.isObservable(de) )
                diagramToAST.changeObservableName(de, e.getNewValue().toString());
        }
        return generateText();
    }

    public @CheckForNull String highlight(ViewPaneEvent e)
    {
        ViewPane vp = e.getViewPane();
        SelectionManager sm = vp.getSelectionManager();
        if( sm.getSelectedViewCount() != 1 )
            return null;
        final Object de = sm.getSelectedView(0).getModel();
        if( de instanceof DiagramElement )
        {
            diagramToAST.highlight((DiagramElement)de);
            return generateText();
        }
        return null;
    }
}
