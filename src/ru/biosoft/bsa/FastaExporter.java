package ru.biosoft.bsa;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;

public class FastaExporter implements DataElementExporter
{
    public static class FastaExporterProperties extends Option
    {
        private static final long serialVersionUID = 1L;
        int nucleotidesPerLine = 60;
        int nucleotidesPerSection = 10;

        public int getNucleotidesPerLine()
        {
            return nucleotidesPerLine;
        }

        public void setNucleotidesPerLine(int nucleotidesPerLine)
        {
            Object oldValue = this.nucleotidesPerLine;
            this.nucleotidesPerLine = nucleotidesPerLine;
            firePropertyChange("nucleotidesPerLine", oldValue, nucleotidesPerLine);
        }

        public int getNucleotidesPerSection()
        {
            return nucleotidesPerSection;
        }

        public void setNucleotidesPerSection(int nucleotidesPerSection)
        {
            Object oldValue = this.nucleotidesPerSection;
            this.nucleotidesPerSection = nucleotidesPerSection;
            firePropertyChange("nucleotidesPerSection", oldValue, nucleotidesPerSection);
        }
    }
    
    public static class FastaExporterPropertiesBeanInfo extends BeanInfoEx
    {
        public FastaExporterPropertiesBeanInfo()
        {
            super(FastaExporterProperties.class, MessageBundle.class.getName());
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("nucleotidesPerLine", beanClass), getResourceString("PN_FASTA_NUCLEOTIDES_PER_LINE"), getResourceString("PD_FASTA_NUCLEOTIDES_PER_LINE"));
            add(new PropertyDescriptorEx("nucleotidesPerSection", beanClass), getResourceString("PN_FASTA_NUCLEOTIDES_PER_SECTION"), getResourceString("PD_FASTA_NUCLEOTIDES_PER_SECTION"));
        }
    }
    
    private FastaExporterProperties properties = new FastaExporterProperties();

    @Override
    public int accept(DataElement de)
    {
        if( ( de instanceof AnnotatedSequence )
                || ( ( de instanceof DataCollection ) && ( (DataCollection)de ).getSize() > 0 && AnnotatedSequence.class
                        .isAssignableFrom( ( (DataCollection)de ).getDataElementType()) ) )
        {
            return DataElementExporter.ACCEPT_HIGH_PRIORITY;
        }
        return DataElementExporter.ACCEPT_UNSUPPORTED;
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file) throws Exception
    {
        doExport(de, file, null);
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file,
            FunctionJobControl jobControl) throws Exception
    {
        if(jobControl != null)
        {
            jobControl.functionStarted();
        }
        int size;
        Iterable<AnnotatedSequence> iterable;
        if(de instanceof AnnotatedSequence)
        {
            iterable = Collections.singletonList((AnnotatedSequence)de);
            size = 1;
        } else
        {
            DataCollection<AnnotatedSequence> dc = (DataCollection<AnnotatedSequence>)de;
            iterable = dc;
            size = dc.getSize();
        }
        try (PrintWriter pw = new PrintWriter( file ))
        {
            int n = 0;
            for( AnnotatedSequence seq : iterable )
            {
                n++;
                Sequence sequence = seq.getSequence();
                pw.println( ">" + seq.getName() );
                int length = sequence.getLength();
                boolean lineStarted = false;
                for( int i = 1; i <= length; i++ )
                {
                    pw.print( (char)sequence.getLetterAt( i ) );
                    lineStarted = true;
                    if( properties.getNucleotidesPerLine() > 0 && i % ( properties.getNucleotidesPerLine() ) == 0 )
                    {
                        pw.println();
                        lineStarted = false;
                    }
                    else if( properties.getNucleotidesPerSection() > 0
                            && ( properties.getNucleotidesPerLine() == 0 ? i : i % ( properties.getNucleotidesPerLine() ) )
                                    % properties.getNucleotidesPerSection() == 0 )
                        pw.print( " " );
                }
                if( lineStarted )
                    pw.println();
                if( jobControl != null )
                {
                    if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    {
                        file.delete();
                        return;
                    }
                    jobControl.setPreparedness( (int) ( ( (long)n * 100 ) / size ) );
                }
            }
        }
        if(jobControl != null)
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
    }

    @Override
    public FastaExporterProperties getProperties(DataElement de, File file)
    {
        return properties;
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList(  DataCollection.class, AnnotatedSequence.class );
    }
}
