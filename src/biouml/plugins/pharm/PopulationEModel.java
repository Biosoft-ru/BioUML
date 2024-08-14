package biouml.plugins.pharm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.EModel;

@SuppressWarnings ( "serial" )
public class PopulationEModel extends EModel
{
    public static final String POPULATION_EMODEL_TYPE = "Population Model";
    
    public PopulationEModel(DiagramElement diagramElement)
    {
        super( diagramElement );
        fillVariables( (Diagram)diagramElement );
    }
    
    private void fillVariables(Diagram diagram)
    {
        Properties props = new Properties();
        props.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, PopulationVariable.class.getName());
        props.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "PopulationVariables");
        
        populationVariables = new VectorDataCollection<>(diagram.getName()
                + " populationvariables(vectorDC)", null, props);
        diagram.stream(Node.class).map( Node::getRole ).select( PopulationVariable.class ).forEach( populationVariables::put );
    }
    
    @Override
    public String getType()
    {
        return POPULATION_EMODEL_TYPE;
    }
    
    DataCollection<PopulationVariable> populationVariables;
    
    public DataCollection<PopulationVariable> getPopulationVariables()
    {
        return populationVariables;
    }
    
    Map<String, String> populationToStructuralVariableName;
    
    public String getStructuralVariableName(String populationVariableName)
    {
        return populationToStructuralVariableName.get(populationVariableName);
    }
    
    public List<PopulationVariable> getFitVariables()
    {
        populationToStructuralVariableName = new HashMap<>();
        List<PopulationVariable> result = new ArrayList<>();
        for (PopulationVariable var: populationVariables)
        {
            Node node = (Node)var.getDiagramElement();
            for (Edge e: node.getEdges())
            {
                Node otherNode = e.getOtherEnd( node );
                if (Util.isPort( otherNode))
                {
                    String type = otherNode.getAttributes().getValueAsString( "type" );
                    if (ParameterProperties.PARAMETER_TYPE.equals( type ))
                        result.add( var );
                    populationToStructuralVariableName.put(var.getName(), otherNode.getAttributes().getValueAsString("parameterName"));
                }
            }
        }

         return result;
    }
       
    
    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        if( !notificationEnabled )
            return;

        super.elementAdded( e );

        DataElement de = e.getDataElement();
        if( de instanceof Node && ((Node)de).getRole() instanceof PopulationVariable )
        {
            populationVariables.put( (PopulationVariable ) ((Node)de).getRole() );
            firePropertyChange( "*", null, null );
        }
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        if( !notificationEnabled )
            return;

        super.elementRemoved( e );

        if( elementToRemove instanceof Node && ((Node)elementToRemove).getRole() instanceof PopulationVariable )
        {
            populationVariables.put( (PopulationVariable )((Node)elementToRemove).getRole());
            firePropertyChange( "*", null, null );
        }
    }
    
    @Override
    public Role clone(DiagramElement de)
    {
        PopulationEModel emodel = new PopulationEModel( de );
        doClone( emodel );
        return emodel;
    }
}
