package biouml.standard.simulation.plot.access;

import java.util.Iterator;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.access.support.SetPropertyCommand;
import ru.biosoft.access.support.TagCommand;
import biouml.standard.simulation.plot.Plot;

public class PlotTextTransformer extends BeanInfoEntryTransformer<Plot>
{
    @Override
    public Class<Plot> getOutputType()
    {
        return Plot.class;
    }

    @Override
    public void init(DataCollection primaryCollection, DataCollection transformedCollection)
    {
        super.init(primaryCollection, transformedCollection);

        Iterator<TagCommand> i = commands.values().iterator();
        while (i.hasNext()) {
            ( (SetPropertyCommand) i.next()).setIndent(6);
        }

        addCommand(new SeriesCommand(this));
    }
}
