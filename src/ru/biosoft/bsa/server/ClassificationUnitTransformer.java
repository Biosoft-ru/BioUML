package ru.biosoft.bsa.server;

import java.io.StringWriter;
import java.util.Properties;

import biouml.plugins.server.access.AccessProtocol;
import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.Entry;
import ru.biosoft.bsa.Const;
import ru.biosoft.bsa.classification.ClassificationUnit;
import ru.biosoft.util.ExProperties;

/**
 * @author lan
 *
 */
public class ClassificationUnitTransformer extends AbstractTransformer<Entry, ClassificationUnit>
{
    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    @Override
    public Class<ClassificationUnit> getOutputType()
    {
        return ClassificationUnit.class;
    }

    @Override
    public ClassificationUnit transformInput(Entry input) throws Exception
    {
        Properties properties = new ExProperties();
        properties.load(input.getReader());
        return new ClientClassificationUnit(getTransformedCollection(), properties);
    }

    @Override
    public Entry transformOutput(ClassificationUnit output) throws Exception
    {
        Properties properties = new ExProperties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, output.getName());
        if(output.getClassNumber() != null)
            properties.setProperty(Const.NUMBER_PROPERTY, output.getClassNumber());
        if(output.getClassName() != null)
            properties.setProperty(Const.CLASSNAME_PROPERTY, output.getClassName());
        if(output.getDescription() != null)
            properties.setProperty(Const.DESCRIPTION_PROPERTY, output.getDescription());
        if(output.getLevel() != null)
            properties.setProperty(Const.LEVEL_PROPERTY, output.getLevel());
        properties.setProperty(AccessProtocol.TEXT_TRANSFORMER_NAME, getClass().getName());
        StringWriter s = new StringWriter();
        properties.store(s, "");
        return new Entry(getPrimaryCollection(), output.getName(), s.toString());
    }
}
