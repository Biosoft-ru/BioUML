package biouml.standard.simulation.access.stochastic;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ru.biosoft.access.file.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.file.FileTypePriority;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.PriorityTransformer;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.access.support.SetPropertyCommand;
import ru.biosoft.access.support.TagCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.StochasticSimulationResult;
import biouml.standard.simulation.access.InitialValuesCommand;
import biouml.standard.simulation.access.ResultValuesCommand;
import biouml.standard.simulation.access.VariablesCommand;

public class StochasticSimulationResultTransformer extends AbstractFileTransformer<SimulationResult> implements PriorityTransformer
{
    private final BeanInfoEntryTransformer<SimulationResult> primaryTransformer = new BeanInfoEntryTransformer<SimulationResult>()
    {
        @Override
        public Class getOutputType()
        {
            return StochasticSimulationResult.class;
        }

        @Override
        public void init(DataCollection primaryCollection, DataCollection transformedCollection)
        {
            super.init( primaryCollection, transformedCollection );
            Iterator<TagCommand> i = commands.values().iterator();
            while( i.hasNext() )
            {
                ( (SetPropertyCommand)i.next() ).setIndent( 6 );
            }

            addCommand( new InitialValuesCommand( this ) );
            VariablesCommand vc = new VariablesCommand( this );
            addCommand( vc );
            addCommand( new ResultValuesCommand( this, vc ) );
            addCommand( new Q1ValuesCommand( this, vc ) );
            addCommand( new Q2ValuesCommand( this, vc ) );
            addCommand( new Q3ValuesCommand( this, vc ) );
        }
    };

    @Override
    public Class<StochasticSimulationResult> getOutputType()
    {
        return StochasticSimulationResult.class;
    }

    @Override
    public void init(DataCollection<FileDataElement> primaryCollection, DataCollection<SimulationResult> transformedCollection)
    {
        super.init( primaryCollection, transformedCollection );
        primaryTransformer.init( primaryCollection, transformedCollection );
    }

    @Override
    public SimulationResult load(File input, String name, DataCollection<SimulationResult> origin) throws Exception
    {
        StochasticSimulationResult sr = new StochasticSimulationResult( origin, name );
        try (FileReader reader = new FileReader( input ))
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
            fw.write( primaryTransformer.getStartTag() + "    " + element.getName() + TagEntryTransformer.endl );
            primaryTransformer.writeObject( element, fw );
        }
    }

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement output)
    {
        if( output instanceof StochasticSimulationResult)
            return 2;
        return -1;
    }

    @Override
    public int getOutputPriority(String name)
    {
        return 0;
    }
}