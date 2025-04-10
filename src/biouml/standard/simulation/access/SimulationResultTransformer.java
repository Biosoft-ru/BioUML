package biouml.standard.simulation.access;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Iterator;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.access.support.SetPropertyCommand;
import ru.biosoft.access.support.TagCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import ru.biosoft.table.access.TableDataTagCommand;
import biouml.standard.simulation.SimulationResult;
import one.util.streamex.DoubleStreamEx;

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
        
        @Override
        public void writeObject(Object obj, Writer writer) throws Exception
        {
            processedObject = obj;

            String startTag = getStartTag();
            TagCommand command;
            for(String name: tagOrder)
            {
                if( name.equals(startTag) )
                    continue;

                command = commands.get(name);
                if( name.equals("VL") )
                {
                    SimulationResult sr = getProcessedObject();
                    double[] times = sr.getTimes();
                    double[][] values = sr.getValues();
                    
                    if (values == null || values.length == 0 || values[0].length == 0)
                        return;

                    for( int timeSliceNumber = 0; timeSliceNumber < times.length; timeSliceNumber++ )
                    {
                        writer.write(DoubleStreamEx.of( values[timeSliceNumber] ).prepend( times[timeSliceNumber] ).joining( "\t", "VL    ", "" ));
                        writer.write(endl);
                    }
                }
                else
                {
                    String str = command.getTaggedValue();
                    if( str != null )
                    {
                        writer.write(str);
                        if( !str.endsWith(endl) )
                            writer.write(endl);
                    }
                }
            }

            processedObject = null;
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
