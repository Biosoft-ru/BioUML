package ru.biosoft.bsa;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;

import com.developmentontheedge.application.ApplicationUtils;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class SAMBAMTrackExporter implements DataElementExporter
{

    @Override
    public int accept(DataElement de)
    {
        if(de instanceof TrackRegion)
            de = ((TrackRegion)de).getTrack();
        if( de instanceof BAMTrack )
            return ACCEPT_HIGH_PRIORITY;
        return ACCEPT_UNSUPPORTED;
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( BAMTrack.class, TrackRegion.class );
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file) throws Exception
    {
        doExport(de, file, null);
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file, FunctionJobControl jobControl) throws Exception
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
        }
        try
        {
            if( de instanceof TrackRegion )
            {
                TrackRegion region = (TrackRegion)de;
                BAMTrack track = (BAMTrack)region.getTrack();

                try (SamReader reader = SamReaderFactory.makeDefault().open( track.getBAMFile() ))
                {
                    final SAMFileWriter writer = new SAMFileWriterFactory().makeBAMWriter( reader.getFileHeader(), true, file );

                    SAMRecordIterator it = reader.queryOverlapping( track.fromEnsembl( region.getSequenceName() ), region.getFrom(),
                            region.getTo() );
                    while( it.hasNext() )
                        writer.addAlignment( it.next() );

                    writer.close();
                    it.close();
                }
            }
            else
            {
                BAMTrack track = (BAMTrack)de;

                file.delete();
                ApplicationUtils.linkOrCopyFile(file, track.getBAMFile(), jobControl);

                if( track.getIndexFile().exists() )
                    ApplicationUtils.linkOrCopyFile(BAMTrack.getIndexFile(file), track.getIndexFile(), jobControl);
            }
        }
        catch( Exception e )
        {
            if( file != null )
                file.delete();
            if( jobControl != null )
            {
                jobControl.functionTerminatedByError(e);
            }
            else
                throw e;
        }
        if( jobControl != null )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

}
