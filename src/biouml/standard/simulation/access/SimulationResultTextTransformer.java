package biouml.standard.simulation.access;

import java.util.Iterator;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.access.support.SetPropertyCommand;
import ru.biosoft.access.support.TagCommand;
import biouml.standard.simulation.SimulationResult;

public class SimulationResultTextTransformer extends BeanInfoEntryTransformer<SimulationResult>
{
    @Override
    public Class<SimulationResult> getOutputType()
    {
        return SimulationResult.class;
    }

    @Override
    public void init(DataCollection primaryCollection, DataCollection transformedCollection)
    {
        super.init(primaryCollection, transformedCollection);

        Iterator<TagCommand> i = commands.values().iterator();
        while( i.hasNext() )
        {
            ( (SetPropertyCommand)i.next() ).setIndent(6);
        }

        addCommand(new InitialValuesCommand(this));
        VariablesCommand vc = new VariablesCommand(this);
        addCommand(vc);
        addCommand(new ResultValuesCommand(this, vc));
    }
}
