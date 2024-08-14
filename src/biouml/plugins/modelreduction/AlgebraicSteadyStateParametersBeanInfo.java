package biouml.plugins.modelreduction;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Event;
import biouml.standard.simulation.SimulationResult;

public class AlgebraicSteadyStateParametersBeanInfo extends BeanInfoEx2<AlgebraicSteadyStateParameters>
{
    public AlgebraicSteadyStateParametersBeanInfo()
    {
        super(AlgebraicSteadyStateParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "inputPath" ).inputElement( Diagram.class ).add();
        add( "outputType", OutputTypeEditor.class );
        property( "outputTable" ).outputElement( TableDataCollection.class ).hidden( "isOutputTableHidden" ).add();
        property( "outputSimulationResult" ).outputElement( SimulationResult.class ).hidden( "isOutputSimulationResultHidden" ).add();
        addExpert("onlyConstantParameters");
        property( "events" ).canBeNull().expert().editor( EventEditor.class ).add();
        addWithTags( "solverName", AlgebraicSteadyStateParameters.availableSolvers );
        add("solver");
    }
    
    public static class OutputTypeEditor extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return new Object[] {AlgebraicSteadyStateParameters.OUTPUT_TABLE_TYPE, AlgebraicSteadyStateParameters.OUTPUT_SIMULATION_RESULT_TYPE};
        }
    }

    //TODO: events order
    public static class EventEditor extends GenericMultiSelectEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            DataElementPath path = ( (AlgebraicSteadyStateParameters)getBean() ).getInputPath();
            if( path == null )
                return new String[] {};
            DataElement de = path.optDataElement();
            if( ! ( de instanceof Diagram ) )
                return new String[] {};
            Diagram diagram = (Diagram)de;
            EModel emodel = diagram.getRole(EModel.class);
            return StreamEx.of( emodel.getEvents() ).map( Event::getDiagramElement ).map( DiagramElement::getName ).toArray( String[]::new );
        }
    }
}