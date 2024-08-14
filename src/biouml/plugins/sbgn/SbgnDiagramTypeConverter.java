package biouml.plugins.sbgn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeConverterSupport;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.DAEModelUtilities;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.dynamics.util.EModelHelper;
import biouml.standard.diagram.Bus;
import biouml.standard.diagram.CompositeModelUtility;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Unit;
import ru.biosoft.graphics.Brush;

/**
 * Convert diagrams into SbgnCompositeDiagramType
 * This should be main diagram type, all other ODE-type diagrams can be converted to SBGNDiagramType
 */
public class SbgnDiagramTypeConverter extends DiagramTypeConverterSupport
{
    Map<String, String> nameMapping = new HashMap<>();
    Map<String, String> pathMapping = new HashMap<>();

    private Map<String, String> generateNameMapping(Diagram diagram)
    {
        Set<String> names = diagram.recursiveStream().select( Node.class ).map( n -> n.getName() ).toSet();

        Map<String, String> result = new HashMap<>();
        for( Node node : diagram.recursiveStream().select( Node.class ) )
        {
            String name = node.getName();
            String newName = castStringToSId( name );

            if( newName.equals( name ) ) //name was good enough            
                continue;

            while( names.contains( newName ) )
                newName = newName + "_";

            result.put( name, newName );
            names.add( newName );

        }
        return result;
    }

    @Override
    protected Diagram convert(DiagramType diagramType, Diagram diagram) throws Exception
    {
        nameMapping = generateNameMapping( diagram );

        Diagram newDiagram = diagramType.createDiagram( diagram.getOrigin(), diagram.getName(), null );
        processCompartment( newDiagram, newDiagram, diagram, Node.class );
        processCompartment( newDiagram, newDiagram, diagram, Edge.class );

        EModel emodel = newDiagram.getRole( EModel.class );
        Map<VariableRole, List<Node>> clusters = DiagramUtility.getBuses( diagram ).groupingBy( n -> (VariableRole)n.getRole() );
        for( Entry<VariableRole, List<Node>> cluster : clusters.entrySet() )
        {
            Bus newBus = new Bus( cluster.getKey().getName().substring( 1 ), false );
            newBus.setColor( ((Brush)cluster.getKey().getDiagramElement().getAttributes().getValue( "color" )).getColor());
            for( Node node : cluster.getValue() )
            {
                Node copy = newDiagram.findNode( node.getCompleteNameInDiagram() );
                emodel.getVariables().remove( copy.getRole( VariableRole.class ).getName() );
                copy.setRole( newBus );
                newBus.addNode( copy );
            }
        }
        
        PlotsInfo info = DiagramUtility.getPlotsInfo( diagram );
        if( info != null )
            DiagramUtility.setPlotsInfo( newDiagram, info.clone( newDiagram.getRole( EModel.class ) ) );
        
        EModel oldEmodel = diagram.getRole( EModel.class );
        for (Variable var: oldEmodel.getParameters())
            emodel.put( (Variable)var.clone( ) );
        
        for (Unit unit: oldEmodel.getUnits().values())        
            emodel.addUnit( unit.clone( null, unit.getName() ) );        
        
        DiagramInfo diagramInfo = (DiagramInfo)diagram.getKernel();
        newDiagram.setKernel( diagramInfo.clone( newDiagram, newDiagram.getName() ) );
        fixSubDiagrams(newDiagram);
        return newDiagram;
    }

    protected void processCompartment(Diagram diagram, Compartment newCompartment, Compartment oldCompartment,
            Class<? extends DiagramElement> deType)
    {
        for( DiagramElement de : oldCompartment.stream().select( deType ) )
        {
            try
            {
                DiagramElement[] newElements = convertDiagramElement( de, diagram );
                if( newElements != null )
                {
                    for( DiagramElement newElement : newElements )
                    {
                        newCompartment.put( newElement );
                        if( ( newElement instanceof Compartment ) && ( de instanceof Compartment ) )
                        {
                            processCompartment( diagram, (Compartment)newElement, (Compartment)de, deType );
                        }
                    }
                }
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "Cannot convert element: " + de.getName(), e );
            }
        }
    }

    @Override
    public DiagramElement[] convertDiagramElement(DiagramElement de, Diagram diagram) throws Exception
    {
        Compartment parent;
        Compartment oldParent = (Compartment)de.getOrigin();
        if( oldParent instanceof Diagram )
        {
            parent = diagram;
        }
        else
        {
            DiagramElement parentDe = diagram.findDiagramElement( oldParent.getCompleteNameInDiagram() );
            parent = ( parentDe == null ) ? null : ( (Compartment)parentDe );
        }
        if( de instanceof SubDiagram )
        {
            SubDiagram oldSubDiagram = (SubDiagram)de;
            Diagram oldInnerDiagram = oldSubDiagram.getDiagram();
            Diagram newInnerDiagram = null;
            if( oldInnerDiagram.getType() instanceof SbgnDiagramType )
            {
                newInnerDiagram = oldInnerDiagram.clone( null, oldInnerDiagram.getName() );
            }
            else
            {
                newInnerDiagram = new SbgnDiagramTypeConverter().convert( oldSubDiagram.getDiagram(), SbgnCompositeDiagramType.class );
            }
            SubDiagram newSubDiagram = oldSubDiagram.clone( diagram, oldSubDiagram.getName() );
            newSubDiagram.setDiagram( newInnerDiagram );           
            return new DiagramElement[] {newSubDiagram};
        }
        else if( de instanceof Node )
        {
            if( de.getRole() instanceof SimpleTableElement )
            {
                DAEModelUtilities.processSimpleTableElement( diagram, (Node)de );
            }
            else
            {
                String name = de.getName();
                DiagramElement newNode = de.clone( parent, name );
                if (newNode instanceof Compartment)
                    ( (Compartment)newNode ).clear();
                return new DiagramElement[] {newNode};
            }
        }
        else if( de instanceof Edge )
        {
            Node oldInput = ( (Edge)de ).getInput();
            Node oldOutput = ( (Edge)de ).getOutput();
            String inputPath = oldInput.getCompleteNameInDiagram();
            String outputPath = oldOutput.getCompleteNameInDiagram();
            DiagramElement newInput = diagram.findDiagramElement( inputPath );
            DiagramElement newOutput = diagram.findDiagramElement( outputPath );
            if( ( newInput != null ) && ( newOutput != null ) )
            {
                DiagramElement newEdge = new Edge( parent, ( (Edge)de ).getKernel(), (Node)newInput, (Node)newOutput );
                if( de.getRole() != null )
                    newEdge.setRole( de.getRole().clone( newEdge ) );
                return new DiagramElement[] {newEdge};
            }
        }
        return null;
    }
    
    private void fixSubDiagrams(Diagram diagram) throws Exception
    {
        for( SubDiagram subDiagram : diagram.recursiveStream().select( SubDiagram.class ) )
        {
            String name = subDiagram.getName();
            String newName = castStringToSId( name );
            if( ! ( newName.equals( name ) ) )
            {
                SubDiagram newSubDiagram = subDiagram.clone( subDiagram.getCompartment(), newName );
                CompositeModelUtility.replaceSubDiagram( subDiagram, newSubDiagram );
                subDiagram = newSubDiagram;
            }

            DynamicProperty dp = subDiagram.getAttributes().getProperty( "Time scale" );
            if( dp != null )
            {
                if( ( (Double)dp.getValue() ).doubleValue() == 1.0 )
                    subDiagram.getAttributes().remove( "Time scale" );
                else
                {
                    EModel newEmodel = diagram.getRole( EModel.class );
                    String varName = EModelHelper.generateUniqueVariableName( newEmodel, "timeScale_" + subDiagram.getName() );
                    newEmodel.declareVariable( varName, dp.getValue() );
                    newEmodel.getVariable( varName ).setConstant( true );
                    subDiagram.getAttributes().add( new DynamicProperty( "Time scale", String.class, varName ) );
                }
            }
        }
    }
    
    private String castStringToSId(String input)
    {
        String result = input.replaceAll("\\W", "_");
        if( result.matches("\\d\\w*") )
        {
            result = "_" + result;
        }
        return result;
    }

    protected boolean isElementAvailable(DiagramElement de, Object[] classes)
    {
        Class<? extends Base> type = de.getKernel().getClass();
        for( Object obj : classes )
        {
            if( obj instanceof Class && ( (Class<?>)obj ).isAssignableFrom( type ) )
            {
                return true;
            }
        }
        return false;
    }
}
