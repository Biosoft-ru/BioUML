package biouml.standard.simulation.access;

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
import biouml.standard.simulation.SimulationResult;

public class SimulationResultTransformer extends AbstractFileTransformer<SimulationResult>
{
    private final BeanInfoEntryTransformer<SimulationResult> primaryTransformer = new BeanInfoEntryTransformer<SimulationResult>()
    {
        @Override
        public Class getOutputType()
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
    };
    
    @Override
    public Class<SimulationResult> getOutputType()
    {
        return SimulationResult.class;
    }

    @Override
    public void init(DataCollection<FileDataElement> primaryCollection, DataCollection<SimulationResult> transformedCollection)
    {
        super.init(primaryCollection, transformedCollection);
        primaryTransformer.init(primaryCollection, transformedCollection);
    }

    @Override
    public SimulationResult load(File input, String name, DataCollection<SimulationResult> origin) throws Exception
    {
        SimulationResult sr = new SimulationResult(origin, name);
        try( FileReader reader = new FileReader( input ) )
        {
            primaryTransformer.readObject( sr, reader );
        }
        return sr;
    }
    
    @Override
    public void save(File output, SimulationResult element) throws Exception
    {
        try (FileWriter fw = new FileWriter( output ))
        {
            fw.write(primaryTransformer.getStartTag() + "    " + element.getName() + TagEntryTransformer.endl);
            primaryTransformer.writeObject(element, fw);
        }
    }
}
