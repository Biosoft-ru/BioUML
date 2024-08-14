package ru.biosoft.table.access;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.access.support.TagCommand;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;

public class TableDataEntryTransformer extends BeanInfoEntryTransformer<StandardTableDataCollection>
{
    private final List<String> TAGS = new ArrayList<>();

    private String delimitersPriority[] = {"\t", " "};

    public TableDataEntryTransformer()
    {
        this(true);
    }

    public TableDataEntryTransformer(boolean addDataCommand)
    {
        super();

        setDelimitersPriority(delimitersPriority);

        initCommonCommands();
        if( addDataCommand )
        {
            super.addCommand(new TableDataTagCommand(TableDataTagCommand.DATA_TAG, this));
        }
    }

    protected void initCommonCommands()
    {
        TAGS.add(TableDataTagCommand.ID_TAG);
        TAGS.add(TableDataTagCommand.ORGANISM_SPECIES_TAG);
        TAGS.add(TableDataTagCommand.PLATFORM_TAG);
        TAGS.add(TableDataTagCommand.EXPERIMENT_TITLE_TAG);
        TAGS.add(TableDataTagCommand.DESCRIPTION_TAG);
        TAGS.add(TableDataTagCommand.INFO_TAG);
        TAGS.add(TableDataTagCommand.SAMPLES_TAG);
        TAGS.add(TableDataTagCommand.COLUMN_EXPRESSIONS_TAG);
        TAGS.add(TableDataTagCommand.PROPERTIES_TAG);
        TAGS.add(TableDataTagCommand.GROUPS_TAG);

        for( String tag : TAGS )
        {
            super.addCommand(new TableDataTagCommand(tag, this));
        }
    }

    @Override
    public Class<StandardTableDataCollection> getOutputType()
    {
        return StandardTableDataCollection.class;
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
            if( name.equals(TableDataTagCommand.DATA_TAG) )
            {
                writer.write(name);
                TableDataCollection me = getProcessedObject();

                for( RowDataElement rowDataElement: me )
                {
                    writer.write(endl);
                    StringBuffer buffer = new StringBuffer();
                    TableDataTagCommand.writeDataLine(rowDataElement, buffer);
                    writer.write(buffer.toString());
                }
                writer.write(endl);
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
}