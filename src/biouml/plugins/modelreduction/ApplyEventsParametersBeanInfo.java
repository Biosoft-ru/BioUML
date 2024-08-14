package biouml.plugins.modelreduction;

import biouml.model.Diagram;
import biouml.standard.simulation.SimulationResult;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ApplyEventsParametersBeanInfo extends BeanInfoEx2<ApplyEventsParameters>
{
    public ApplyEventsParametersBeanInfo()
    {
        super(ApplyEventsParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "diagramPath" ).inputElement( Diagram.class ).add();
        property( "tablePath" ).inputElement( TableDataCollection.class ).add();
        property( "resultPath" ).outputElement( SimulationResult.class ).add();
        add( "engineWrapper" );
    }
}