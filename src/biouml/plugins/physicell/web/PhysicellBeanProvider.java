package biouml.plugins.physicell.web;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.physicell.CellDefinitionProperties;
import biouml.plugins.physicell.MulticellEModel;
import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;

public class PhysicellBeanProvider implements BeanProvider
{

    @Override
    public Object getBean(String path)
    {
        DataElementPath fullPath = DataElementPath.create( path );
        String propName = fullPath.getName();
        String elemName = fullPath.getParentPath().getName();
        Diagram diagram = WebDiagramsProvider.getDiagram( fullPath.getParentPath().getParentPath().toString(), false );

        if( diagram == null )
            return null;
        MulticellEModel model = diagram.getRole( MulticellEModel.class );
        if( model == null )
            return null;

        if( propName.equals( "domain" ) )
            return model.getDomain();
        if( propName.equals( "user_parameters" ) )
            return model.getUserParmeters();
        if( propName.equals( "initial_condition" ) )
            return model.getInitialCondition();
        if( propName.equals( "report_properties" ) )
            return model.getReportProperties();
        if( propName.equals( "options" ) )
            return model.getOptions();

        DiagramElement de = diagram.get( elemName );
        if( de == null || de.getRole() == null || ! ( de.getRole() instanceof CellDefinitionProperties ) )
            return null;

        CellDefinitionProperties elem = (CellDefinitionProperties)de.getRole();

        if( propName.equals( "cycle" ) )
            return elem.getCycleProperties();
        else if( propName.equals( "death" ) )
            return elem.getDeathProperties();
        else if( propName.equals( "volume" ) )
            return elem.getVolumeProperties();
        else if( propName.equals( "mechanics" ) )
            return elem.getMechanicsProperties();
        else if( propName.equals( "motility" ) )
            return elem.getMotilityProperties();
        else if( propName.equals( "secretion" ) )
            return elem.getSecretionsProperties();
        else if( propName.equals( "interactions" ) )
            return elem.getInteractionsProperties();
        else if( propName.equals( "transformations" ) )
            return elem.getTransformationsProperties();
        else if( propName.equals( "custom_data" ) )
            return elem.getCustomDataProperties();
        else if( propName.equals( "functions" ) )
            return elem.getFunctionsProperties();
        else if( propName.equals( "intracellular" ) )
            return elem.getIntracellularProperties();
        return null;
    }

}
