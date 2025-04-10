package ru.biosoft.bsastats;

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.WritableTrack;

/**
 * @author lan
 *
 */
@ClassIcon("resources/ProcessReads.gif")
public class ProcessTasks extends AnalysisMethodSupport<ProcessTasksParameters>
{
    public ProcessTasks(DataCollection<?> origin, String name)
    {
        super( origin, name, new ProcessTasksParameters() );
    }

    @Override
    public Object[] justAnalyzeAndPut() throws Exception
    {
        ProgressIterator<?> inputIterator;
        SiteWriter outputWriter;
        ru.biosoft.access.core.DataElement[] results = null;
        log.info( "Initializing..." );
        if( parameters.getLibraryType().equals( ProcessTasksParameters.LIBRARY_TYPE_SINGLE_END ) )
        {
            if( parameters.getInputType().equals( ProcessTasksParameters.INPUT_TYPE_FASTQ ) )
            {
                inputIterator = new FastqReadingIterator( getFileForPath( parameters.getSingleEndFastq() ), parameters.getEncoding(), false );

                DataElementPath output = parameters.getSingleEndFastqOutput();
                File file = getFileForPath( output );

                outputWriter = new FastqSiteWriter( file, EncodingSelector.ENCODING_TO_OFFSET.get( parameters.getEncoding() ) );

                results = new ru.biosoft.access.core.DataElement[] {new FileDataElement( output.getName(), output.optParentCollection(), file )};

            }
            else if( parameters.getInputType().equals( ProcessTasksParameters.INPUT_TYPE_SOLID ) )
            {
                inputIterator = new SolidReadingIterator( getFileForPath( parameters.getSingleEndCSFasta() ),
                        getFileForPath( parameters.getSingleEndQual() ), false, false );

                DataElementPath csFasta = parameters.getSingleEndCSFastaOutput();
                File csFastaFile = getFileForPath( csFasta );

                DataElementPath qual = parameters.getSingleEndQualOutput();
                File qualFile = getFileForPath( qual );

                outputWriter = new SolidSiteWriter( csFastaFile, qualFile );

                results = new ru.biosoft.access.core.DataElement[] {new FileDataElement( csFasta.getName(), csFasta.optParentCollection(), csFastaFile ),
                        new FileDataElement( qual.getName(), qual.optParentCollection(), qualFile )};
            }
            else
                throw new IllegalArgumentException("Illegal value for input type: "+parameters.getInputType());
        }
        else if( parameters.getLibraryType().equals( ProcessTasksParameters.LIBRARY_TYPE_PAIRED_END ) )
        {
            if( parameters.getInputType().equals( ProcessTasksParameters.INPUT_TYPE_FASTQ ) )
            {
                FastqReadingIterator firstIterator = new FastqReadingIterator( getFileForPath( parameters.getPairedEndFastqFirst() ),
                        parameters.getEncoding(), false );
                FastqReadingIterator secondIterator = new FastqReadingIterator( getFileForPath( parameters.getPairedEndFastqSecond() ),
                        parameters.getEncoding(), false );
                inputIterator = new PairedSiteIterator( firstIterator, secondIterator );

                DataElementPath firstOutput = parameters.getPairedEndFastqFirstOutput();
                DataElementPath secondOutput = parameters.getPairedEndFastqSecondOutput();

                File firstFile = getFileForPath( firstOutput );
                File secondFile = getFileForPath( secondOutput );

                outputWriter = new PairedFastqWriter( firstFile, secondFile, EncodingSelector.ENCODING_TO_OFFSET.get( parameters
                        .getEncoding() ) );

                results = new ru.biosoft.access.core.DataElement[] {new FileDataElement( firstOutput.getName(), firstOutput.optParentCollection(), firstFile ),
                        new FileDataElement( secondOutput.getName(), secondOutput.optParentCollection(), secondFile )};

            }
            else if( parameters.getInputType().equals( ProcessTasksParameters.INPUT_TYPE_SOLID ) )
            {
                SolidReadingIterator firstIterator = new SolidReadingIterator( getFileForPath( parameters.getPairedEndCSFastaFirst() ),
                        getFileForPath( parameters.getPairedEndQualFirst() ), false, false );
                SolidReadingIterator secondIterator = new SolidReadingIterator( getFileForPath( parameters.getPairedEndCSFastaSecond() ),
                        getFileForPath( parameters.getPairedEndQualSecond() ), false, false );
                inputIterator = new PairedSiteIterator( firstIterator, secondIterator );

                DataElementPath csFastaFirst = parameters.getPairedEndCSFastaFirstOutput();
                File csFastaFirstFile = getFileForPath( csFastaFirst );
                DataElement csFastaFirstResult = new FileDataElement( csFastaFirst.getName(), csFastaFirst.optParentCollection(),
                        csFastaFirstFile );

                DataElementPath qualFirst = parameters.getPairedEndQualFirstOutput();
                File qualFirstFile = getFileForPath( qualFirst );
                DataElement qualFirstResult = new FileDataElement( qualFirst.getName(), qualFirst.optParentCollection(), qualFirstFile );

                DataElementPath csFastaSecond = parameters.getPairedEndCSFastaSecondOutput();
                File csFastaSecondFile = getFileForPath( csFastaSecond );
                DataElement csFastaSecondResult = new FileDataElement( csFastaSecond.getName(), csFastaSecond.optParentCollection(),
                        csFastaSecondFile );

                DataElementPath qualSecond = parameters.getPairedEndQualSecondOutput();
                File qualSecondFile = getFileForPath( qualFirst );
                DataElement qualSecondResult = new FileDataElement( qualSecond.getName(), qualSecond.optParentCollection(), qualSecondFile );

                outputWriter = new PairedSolidWriter( csFastaFirstFile, qualFirstFile, csFastaSecondFile, qualSecondFile );
                results = new ru.biosoft.access.core.DataElement[] {csFastaFirstResult, qualFirstResult, csFastaSecondResult, qualSecondResult};
            }
            else
                throw new IllegalArgumentException("Illegal value for input type: "+parameters.getInputType());
        }
        else
            throw new IllegalArgumentException("Illegal value for library type: "+parameters.getLibraryType());

        int total = 0, removed = 0, modified = 0;
        log.info( "Processing..." );
        while( inputIterator.hasNext() )
        {
            Object task = inputIterator.next();
            total++;

            Object initialTask = task;
            for( TaskProcessor processor : parameters.getTaskProcessors() )
                if( processor.isEnabled() )
                {
                    if( parameters.getLibraryType().equals( ProcessTasksParameters.LIBRARY_TYPE_PAIRED_END ) )
                    {
                        PairedTask pairedTask = (PairedTask)task;
                        Task first = processor.process( pairedTask.first );
                        Task second = processor.process( pairedTask.second );
                        if( first == null || second == null )
                            task = null;
                        else if( first != pairedTask.first || second != pairedTask.second )
                            task = new PairedTask( first, second );
                    }
                    else
                    {
                        task = processor.process( (Task)task );
                    }
                    if( task == null )
                        break;
                }

            if( task != null )
            {
                outputWriter.write( task );
                if( task != initialTask )
                    modified++;
            }
            else
                removed++;

            jobControl.setPreparedness( (int) ( inputIterator.getProgress() * 100 ) );
            if( jobControl.isStopped() )
            {
                outputWriter.destroy();
                return null;
            }
        }
        outputWriter.close();
        
        for( TaskProcessor processor : parameters.getTaskProcessors() )
            if( processor.isEnabled() )
            {
                processor.finalizeProcessing();
            }

        for( ru.biosoft.access.core.DataElement res : results )
            CollectionFactoryUtils.save(res);

        log.info( "Of " + total + " reads " + removed + " were removed, " + modified + " were modified" );

        return results;
    }
    
    public static File getFileForPath(DataElementPath path)
    {
        DataElement de = path.optDataElement();
        if( de instanceof FileDataElement )
        {
            return ( (FileDataElement)de ).getFile();
        }
        return DataCollectionUtils.getChildFile( path.optParentCollection(), path.getName() );
    }

    public interface SiteWriter<T>
    {
        void write(T t) throws Exception;
        void close() throws Exception;
        void destroy();
    }

    public static class FastqSiteWriter implements SiteWriter<Task>
    {
        private PrintWriter pw;
        private File file;
        private byte qualOffset;

        public FastqSiteWriter(File f, byte qualOffset) throws Exception
        {
            this.file = f;
            this.qualOffset = qualOffset;
            pw = new PrintWriter( f );
        }

        @Override
        public void write(Task t) throws Exception
        {
            String[] lines = ( (String)t.getData() ).split( "\n" );
            pw.println( lines[0] );
            pw.println( new String( t.getSequence() ) );
            pw.println( lines[2] );
            byte[] quals = new byte[t.getQuality().length];
            for( int i = 0; i < quals.length; i++ )
                quals[i] = (byte) ( qualOffset + t.getQuality()[i] );
            pw.println( new String( quals ) );
        }

        @Override
        public void close()
        {
            pw.close();
        }

        @Override
        public void destroy()
        {
            pw.close();
            file.delete();
        }
    }

    private static class SolidSiteWriter implements SiteWriter<Task>
    {
        private PrintWriter csFastaWriter;
        private PrintWriter qualWriter;
        private File csFastaFile;
        private File qualFile;

        public SolidSiteWriter(File csFastaFile, File qualFile) throws Exception
        {
            this.csFastaFile = csFastaFile;
            this.qualFile = qualFile;
            csFastaWriter = new PrintWriter( csFastaFile );
            qualWriter = new PrintWriter( qualFile );
        }

        @Override
        public void write(Task t) throws Exception
        {
            String csFastaText = ( (String[])t.getData() )[0];
            String[] lines = csFastaText.split( "\n" );
            csFastaWriter.println( lines[0] );
            csFastaWriter.println( new String( t.getSequence() ) );

            String qualText = ( (String[])t.getData() )[1];
            lines = qualText.split( "\n" );
            qualWriter.println( lines[0] );
            qualWriter.println( StringUtils.join( ArrayUtils.toObject( t.getQuality() ), ' ' ) );
        }

        @Override
        public void close()
        {
            csFastaWriter.close();
            qualWriter.close();
        }

        @Override
        public void destroy()
        {
            close();
            csFastaFile.delete();
            qualFile.delete();
        }
    }

    private static class PairedTask
    {
        Task first, second;

        public PairedTask(Task first, Task second)
        {
            this.first = first;
            this.second = second;
        }
    }

    private static class PairedSiteWriter implements SiteWriter<PairedTask>
    {
        protected SiteWriter<Task> firstWriter;
        protected SiteWriter<Task> secondWriter;

        @Override
        public void write(PairedTask t) throws Exception
        {
            firstWriter.write( t.first );
            secondWriter.write( t.second );
        }

        @Override
        public void close() throws Exception
        {
            firstWriter.close();
            secondWriter.close();
        }

        @Override
        public void destroy()
        {
            firstWriter.destroy();
            secondWriter.destroy();
        }
    }

    private static class PairedFastqWriter extends PairedSiteWriter
    {
        public PairedFastqWriter(File firstFile, File secondFile, byte qualOffset) throws Exception
        {
            firstWriter = new FastqSiteWriter( firstFile, qualOffset );
            secondWriter = new FastqSiteWriter( secondFile, qualOffset );
        }
    }

    private static class PairedSolidWriter extends PairedSiteWriter
    {
        public PairedSolidWriter(File firstCSFastaFile, File firstQualFile, File secondCSFastaFile, File secondQualFile) throws Exception
        {
            firstWriter = new SolidSiteWriter( firstCSFastaFile, firstQualFile );
            secondWriter = new SolidSiteWriter( secondCSFastaFile, secondQualFile );
        }
    }

    private static class PairedSiteIterator implements ProgressIterator<PairedTask>
    {
        private ProgressIterator<Task> first, second;
        public PairedSiteIterator(ProgressIterator<Task> first, ProgressIterator<Task> second)
        {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean hasNext()
        {
            return first.hasNext() && second.hasNext();
        }

        @Override
        public PairedTask next()
        {
            return new PairedTask( first.next(), second.next() );
        }

        @Override
        public void remove()
        {
            first.remove();
            second.remove();
        }

        @Override
        public float getProgress()
        {
            return first.getProgress() + second.getProgress() / 2;
        }

    }

    private static class TrackWriter implements SiteWriter<Task>
    {
        private WritableTrack track;

        public TrackWriter(WritableTrack track)
        {
            this.track = track;
        }

        @Override
        public void write(Task t) throws Exception
        {
            track.addSite( (Site)t.getData() );
        }

        @Override
        public void close() throws Exception
        {
            track.finalizeAddition();
        }

        @Override
        public void destroy()
        {
            try
            {
                track.getOrigin().remove( track.getName() );
            }
            catch( Exception e )
            {
            }
        }
    }
}
