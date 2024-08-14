package biouml.standard.simulation.plot.access;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.access.support.SetPropertyCommand;
import ru.biosoft.access.support.TagCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.standard.simulation.plot.Plot;

public class PlotTransformer extends AbstractFileTransformer<Plot>
{
    private BeanInfoEntryTransformer<Plot> primaryTransformer = new BeanInfoEntryTransformer<Plot>()
    {
        @Override
        public Class getOutputType()
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
    };
    
    @Override
    public Class getOutputType()
    {
        return Plot.class;
    }

    @Override
    public void init(DataCollection<FileDataElement> primaryCollection, DataCollection<Plot> transformedCollection)
    {
        super.init(primaryCollection, transformedCollection);
        primaryTransformer.init(primaryCollection, transformedCollection);
    }
    
    @Override
    public Plot load(File input, String name, DataCollection<Plot> origin) throws Exception
    {
        Plot pl = new Plot(origin, name);
        try( FileReader reader = new FileReader( input ) )
        {
            primaryTransformer.readObject( pl, reader );
        }
        return pl;
    }

    @Override
    public void save(File output, Plot element) throws Exception
    {
        try (FileWriter fw = new FileWriter( output ))
        {
            fw.write(primaryTransformer.getStartTag() + "    " + element.getName() + TagEntryTransformer.endl);
            primaryTransformer.writeObject(element, fw);
        }
    }
}
