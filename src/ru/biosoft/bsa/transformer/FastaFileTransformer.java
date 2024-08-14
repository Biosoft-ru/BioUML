package ru.biosoft.bsa.transformer;

import java.io.File;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.generic.PriorityTransformer;
import ru.biosoft.access.FileEntryCollection2;
import ru.biosoft.access.JDBM2Index;
import ru.biosoft.bsa.SequenceImporter;

public class FastaFileTransformer extends AbstractFileTransformer<FastaSequenceCollection> implements PriorityTransformer
{
    @Override
    public FastaSequenceCollection load(File input, String name, DataCollection<FastaSequenceCollection> origin) throws Exception
    {
        return (FastaSequenceCollection)SequenceImporter.createElement( origin, name, input, SequenceImporter.FASTA_FORMAT );
    }
    
    @Override
    public void save(File output, FastaSequenceCollection element) throws Exception
    {
        FileEntryCollection2 fec = ((FileEntryCollection2)element.getPrimaryCollection());
        ApplicationUtils.linkOrCopyFile( output, fec.getFile(), null );
    }

    @Override
    public Class<? extends FastaSequenceCollection> getOutputType()
    {
        return FastaSequenceCollection.class;
    }

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement output)
    {
        return 1;
    }

    @Override
    public int getOutputPriority(String name)
    {
        if(name.endsWith( ".fa" ) || name.endsWith( ".fasta" ) || name.endsWith( ".fna" ))
            return 2;
        return 0;
    }
}
