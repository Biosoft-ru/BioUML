package ru.biosoft.bsa.transformer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.file.FileBasedCollection;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.generic.PriorityTransformer;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.util.TempFiles;

public class BAMFileTransformer extends AbstractFileTransformer<BAMTrack> implements PriorityTransformer
{

    @Override
    public Class<? extends BAMTrack> getOutputType()
    {
        return BAMTrack.class;
    }

    @Override
    public BAMTrack load(File input, String name, DataCollection<BAMTrack> origin) throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( DataCollectionConfigConstants.FILE_PROPERTY, input.getAbsolutePath() );
        return new BAMTrack( origin, properties );
    }

    @Override
    public void save(File output, BAMTrack element) throws Exception
    {
        ApplicationUtils.linkOrCopyFile( output, element.getBAMFile(), null );
    }

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement output)
    {
        return 1;
    }

    @Override
    public int getOutputPriority(String name)
    {
        if(name.toLowerCase().endsWith( ".bam" ))
            return 2;
        return 0;
    }
    
    @Override
    public FileDataElement transformOutput(BAMTrack output) throws Exception
    {
        File dir = TempFiles.dir("transform");
        try
        {
            File file = new File(dir, output.getName());
            save(file, output);
            if(!file.exists())
                throw new FileNotFoundException(file.toString());
            FileDataElement fde = new FileDataElement(output.getName(), getPrimaryCollection().cast( FileBasedCollection.class ));
            ApplicationUtils.linkOrCopyFile(fde.getFile(), file, null);
            File bai = output.getIndexFile();
            if(bai.exists()) {
                File bai_output = BAMTrack.getIndexFile( fde.getFile() );
            	ApplicationUtils.linkOrCopyFile(bai_output, bai, null);
            }
            return fde;
        }
        finally
        {
            ApplicationUtils.removeDir(dir);
        }
    }

}
