package ru.biosoft.bsa.transformer;

import java.io.File;
import java.util.Properties;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.Entry;
import ru.biosoft.access.FileEntryCollection2;
import ru.biosoft.access.JDBM2Index;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.generic.PriorityTransformer;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.SequenceImporter;
import ru.biosoft.bsa.GenbankSequenceCollection;

public class GenbankFileTransformer extends AbstractFileTransformer<GenbankSequenceCollection> implements PriorityTransformer
{

    @Override
    public GenbankSequenceCollection load(File input, String name, DataCollection<GenbankSequenceCollection> origin) throws Exception
    {
       return (GenbankSequenceCollection)SequenceImporter.createElement( origin, name, input, SequenceImporter.GB_FORMAT );
    }
    

    @Override
    public void save(File output, GenbankSequenceCollection element) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<? extends GenbankSequenceCollection> getOutputType()
    {
        return GenbankSequenceCollection.class;
    }
    

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement output)
    {
        return -1;
    }

    @Override
    public int getOutputPriority(String name)
    {
        if(name.toLowerCase().endsWith( ".gb" ))
            return 2;
        return 0;
    }
}
