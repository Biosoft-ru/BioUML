package biouml.plugins.kegg.type.access;

import ru.biosoft.access.support.DividedLineTagCommand;
import biouml.standard.type.Protein;

public class SysnameTagCommand extends DividedLineTagCommand<Protein>
{
    public SysnameTagCommand(String tag, EnzymeTransformer transformer)
    {
        super(tag, transformer);
    }


    @Override
    public void startTag(String tag)
    {

    }

    @Override
    public void addLine(String line)
    {
        if (line == null || line.length() == 0)
            return;

        Protein enzyme = transformer.getProcessedObject();
        enzyme.setCompleteName(line);
    }

    @Override
    public void endTag(String tag)
    {

    }

    @Override
    public String getTaggedValue()
    {
        StringBuffer value = new StringBuffer(tag);
        Protein enzyme = transformer.getProcessedObject();
        value.append('\t').append(enzyme.getCompleteName());
        return value.toString();
    }

    @Override
    public String getTaggedValue(String tag)
    {
        throw new UnsupportedOperationException();
    }
}
