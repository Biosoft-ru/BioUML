package biouml.plugins.microarray;

import java.util.ArrayList;
import java.util.List;

import biouml.standard.type.TableExperiment;

import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.access.support.TagCommand;

public class MicroarrayEntryTransformer extends BeanInfoEntryTransformer<TableExperiment>
{
    private final List<String> TAGS = new ArrayList<>();

    private static final String delimitersPriority[] = {"\t", " "};

    public MicroarrayEntryTransformer()
    {
        super();

        setDelimitersPriority(delimitersPriority);

        TAGS.add(MicroarrayTagCommand.ID_TAG);
        TAGS.add(MicroarrayTagCommand.DESCRIPTION_TAG);
        TAGS.add(MicroarrayTagCommand.PARAMETERS_DESCRIPTION_TAG);
        TAGS.add(MicroarrayTagCommand.PARAMETERS_TAG);

        for( String tag : TAGS )
        {
            super.addCommand(new MicroarrayTagCommand(tag, this));
        }
    }

    @Override
    public Class<TableExperiment> getOutputType()
    {
        return TableExperiment.class;
    }

    @Override
    public void addCommand(TagCommand command)
    {
        for( String tag : TAGS )
        {
            if( tag.equals(command.getTag()) )
            {
                super.addCommand(command);
            }
        }
    }
}
