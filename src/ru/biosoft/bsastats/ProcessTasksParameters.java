package ru.biosoft.bsastats;

import java.beans.IntrospectionException;
import java.util.Iterator;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.util.BeanUtil;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
public class ProcessTasksParameters extends AbstractAnalysisParameters
{

    public static final String LIBRARY_TYPE_SINGLE_END = "Single end";
    public static final String LIBRARY_TYPE_PAIRED_END = "Paired end";
    static final String[] LIBRARY_TYPES = new String[] {LIBRARY_TYPE_SINGLE_END, LIBRARY_TYPE_PAIRED_END};
    private String libraryType = LIBRARY_TYPES[0];

    public static final String INPUT_TYPE_FASTQ = "FastQ";
    public static final String INPUT_TYPE_SOLID = "Solid csfasta + qual";
    static final String[] INPUT_TYPES = new String[] {INPUT_TYPE_FASTQ, INPUT_TYPE_SOLID};
    private String inputType = INPUT_TYPES[0];

    private String encoding = EncodingSelector.ENCODING_TO_OFFSET.keySet().iterator().next();;

    private DataElementPath singleEndFastq;
    private DataElementPath singleEndFastqOutput;

    private DataElementPath singleEndCSFasta;
    private DataElementPath singleEndQual;
    private DataElementPath singleEndCSFastaOutput;
    private DataElementPath singleEndQualOutput;

    private DataElementPath pairedEndFastqFirst;
    private DataElementPath pairedEndFastqSecond;
    private DataElementPath pairedEndFastqFirstOutput;
    private DataElementPath pairedEndFastqSecondOutput;

    private DataElementPath pairedEndCSFastaFirst;
    private DataElementPath pairedEndQualFirst;
    private DataElementPath pairedEndCSFastaSecond;
    private DataElementPath pairedEndQualSecond;
    private DataElementPath pairedEndCSFastaFirstOutput;
    private DataElementPath pairedEndQualFirstOutput;
    private DataElementPath pairedEndCSFastaSecondOutput;
    private DataElementPath pairedEndQualSecondOutput;

    private DynamicPropertySet taskProcessorsDPS;

    public ProcessTasksParameters()
    {
        try
        {
            setTaskProcessorsDPS( BeanUtil.createDPSForBeans( new TrimLowQuality(), new CutAdapters(),  new FilterByLength(), new FilterByQuality() ) );
        }
        catch( IntrospectionException e )
        {
            throw new RuntimeException(e);
        }
    }

    @PropertyName ( "Library type" )
    @PropertyDescription ( "Type of DNA library" )
    public String getLibraryType()
    {
        return libraryType;
    }
    public void setLibraryType(String libraryType)
    {
        Object oldValue = this.libraryType;
        this.libraryType = libraryType;
        firePropertyChange( "*", oldValue, libraryType );
    }

    @PropertyName ( "Input type" )
    @PropertyDescription ( "Type of input files" )
    public String getInputType()
    {
        return inputType;
    }
    public void setInputType(String inputType)
    {
        Object oldValue = this.inputType;
        this.inputType = inputType;
        firePropertyChange( "*", oldValue, inputType );
    }

    @PropertyName ( "Quality encoding" )
    @PropertyDescription ( "This specifies how phred quality values are encoded in the FASTQ file. In most of the cases system detects this value automatically. You may change it manually if auto-detection worked incorrectly." )
    public String getEncoding()
    {
        return encoding;
    }
    public void setEncoding(String encoding)
    {
        Object oldValue = this.encoding;
        this.encoding = encoding;
        firePropertyChange( "encoding", oldValue, encoding );
    }

    public boolean isEncodingHidden()
    {
        return inputType.equals( INPUT_TYPE_SOLID );
    }

    @PropertyName ( "Input fastq file" )
    @PropertyDescription ( "Input fastq file" )
    public DataElementPath getSingleEndFastq()
    {
        return singleEndFastq;
    }
    public void setSingleEndFastq(DataElementPath singleEndFastq)
    {
        Object oldValue = this.singleEndFastq;
        this.singleEndFastq = singleEndFastq;
        firePropertyChange( "singleEndFastq", oldValue, singleEndFastq );
        try
        {
            encoding = EncodingSelector.detectEncoding( singleEndFastq );
        }
        catch( Exception e )
        {
        }
    }

    @PropertyName ( "Output fastq file" )
    @PropertyDescription ( "Output fastq file" )
    public DataElementPath getSingleEndFastqOutput()
    {
        return singleEndFastqOutput;
    }
    public void setSingleEndFastqOutput(DataElementPath singleEndFastqOutput)
    {
        Object oldValue = this.singleEndFastqOutput;
        this.singleEndFastqOutput = singleEndFastqOutput;
        firePropertyChange( "singleEndFastqOutput", oldValue, singleEndFastqOutput );
    }

    public boolean isSingleEndFastqHidden()
    {
        return ! ( libraryType.equals( LIBRARY_TYPE_SINGLE_END ) && inputType.equals( INPUT_TYPE_FASTQ ) );
    }


    @PropertyName ( "Input csfasta file" )
    @PropertyDescription ( "Input csfasta file" )
    public DataElementPath getSingleEndCSFasta()
    {
        return singleEndCSFasta;
    }
    public void setSingleEndCSFasta(DataElementPath singleEndCSFasta)
    {
        Object oldValue = this.singleEndCSFasta;
        this.singleEndCSFasta = singleEndCSFasta;
        firePropertyChange( "singleEndCSFasta", oldValue, singleEndCSFasta );
    }

    @PropertyName ( "Input qual file" )
    @PropertyDescription ( "Input qual file" )
    public DataElementPath getSingleEndQual()
    {
        return singleEndQual;
    }
    public void setSingleEndQual(DataElementPath singleEndQual)
    {
        Object oldValue = this.singleEndQual;
        this.singleEndQual = singleEndQual;
        firePropertyChange( "singleEndQual", oldValue, singleEndQual );
    }

    @PropertyName ( "Output csfasta file" )
    @PropertyDescription ( "Output csfasta file" )
    public DataElementPath getSingleEndCSFastaOutput()
    {
        return singleEndCSFastaOutput;
    }
    public void setSingleEndCSFastaOutput(DataElementPath singleEndCSFastaOutput)
    {
        Object oldValue = this.singleEndCSFastaOutput;
        this.singleEndCSFastaOutput = singleEndCSFastaOutput;
        firePropertyChange( "singleEndCSFastaOutput", oldValue, singleEndCSFastaOutput );
    }

    @PropertyName ( "Output qual file" )
    @PropertyDescription ( "Output qual file" )
    public DataElementPath getSingleEndQualOutput()
    {
        return singleEndQualOutput;
    }
    public void setSingleEndQualOutput(DataElementPath singleEndQualOutput)
    {
        Object oldValue = this.singleEndQualOutput;
        this.singleEndQualOutput = singleEndQualOutput;
        firePropertyChange( "singleEndQualOutput", oldValue, singleEndQualOutput );
    }

    public boolean isSingleEndCSFastaHidden()
    {
        return ! ( libraryType.equals( LIBRARY_TYPE_SINGLE_END ) && inputType.equals( INPUT_TYPE_SOLID ) );
    }

    @PropertyName ( "Input first fastq file" )
    @PropertyDescription ( "Input first fastq file" )
    public DataElementPath getPairedEndFastqFirst()
    {
        return pairedEndFastqFirst;
    }
    public void setPairedEndFastqFirst(DataElementPath pairedEndFastqFirst)
    {
        Object oldValue = this.pairedEndFastqFirst;
        this.pairedEndFastqFirst = pairedEndFastqFirst;
        firePropertyChange( "pairedEndFastqFirst", oldValue, pairedEndFastqFirst );
        try
        {
            encoding = EncodingSelector.detectEncoding( pairedEndFastqFirst );
        }
        catch( Exception e )
        {
        }
    }

    @PropertyName ( "Input second fastq file" )
    @PropertyDescription ( "Input second fastq file" )
    public DataElementPath getPairedEndFastqSecond()
    {
        return pairedEndFastqSecond;
    }
    public void setPairedEndFastqSecond(DataElementPath pairedEndFastqSecond)
    {
        Object oldValue = this.pairedEndFastqSecond;
        this.pairedEndFastqSecond = pairedEndFastqSecond;
        firePropertyChange( "pairedEndFastqSecond", oldValue, pairedEndFastqSecond );
    }

    @PropertyName ( "Output first fastq file" )
    @PropertyDescription ( "Output first fastq file" )
    public DataElementPath getPairedEndFastqFirstOutput()
    {
        return pairedEndFastqFirstOutput;
    }
    public void setPairedEndFastqFirstOutput(DataElementPath pairedEndFastqFirstOutput)
    {
        Object oldValue = this.pairedEndFastqFirstOutput;
        this.pairedEndFastqFirstOutput = pairedEndFastqFirstOutput;
        firePropertyChange( "pairedEndFastqFirstOutput", oldValue, pairedEndFastqFirstOutput );
    }

    @PropertyName ( "Output second fastq file" )
    @PropertyDescription ( "Output second fastq file" )
    public DataElementPath getPairedEndFastqSecondOutput()
    {
        return pairedEndFastqSecondOutput;
    }
    public void setPairedEndFastqSecondOutput(DataElementPath pairedEndFastqSecondOutput)
    {
        Object oldValue = this.pairedEndFastqSecondOutput;
        this.pairedEndFastqSecondOutput = pairedEndFastqSecondOutput;
        firePropertyChange( "pairedEndFastqSecondOutput", oldValue, pairedEndFastqSecondOutput );
    }

    public boolean isPairedEndFastqHidden()
    {
        return ! ( libraryType.equals( LIBRARY_TYPE_PAIRED_END ) && inputType.equals( INPUT_TYPE_FASTQ ) );
    }

    @PropertyName ( "Input first csfasta file" )
    @PropertyDescription ( "Input first csfasta file" )
    public DataElementPath getPairedEndCSFastaFirst()
    {
        return pairedEndCSFastaFirst;
    }
    public void setPairedEndCSFastaFirst(DataElementPath pairedEndCSFastaFirst)
    {
        Object oldValue = this.pairedEndCSFastaFirst;
        this.pairedEndCSFastaFirst = pairedEndCSFastaFirst;
        firePropertyChange( "pairedEndCSFastaFirst", oldValue, pairedEndCSFastaFirst );
    }

    @PropertyName ( "Input first qual file" )
    @PropertyDescription ( "Input first qual file" )
    public DataElementPath getPairedEndQualFirst()
    {
        return pairedEndQualFirst;
    }
    public void setPairedEndQualFirst(DataElementPath pairedEndQualFirst)
    {
        Object oldValue = this.pairedEndQualFirst;
        this.pairedEndQualFirst = pairedEndQualFirst;
        firePropertyChange( "pairedEndQualFirst", oldValue, pairedEndQualFirst );
    }

    @PropertyName ( "Input second csfasta file" )
    @PropertyDescription ( "Input second csfasta file" )
    public DataElementPath getPairedEndCSFastaSecond()
    {
        return pairedEndCSFastaSecond;
    }
    public void setPairedEndCSFastaSecond(DataElementPath pairedEndCSFastaSecond)
    {
        Object oldValue = this.pairedEndCSFastaSecond;
        this.pairedEndCSFastaSecond = pairedEndCSFastaSecond;
        firePropertyChange( "pairedEndCSFastaSecond", oldValue, pairedEndCSFastaSecond );
    }

    @PropertyName ( "Input second qual file" )
    @PropertyDescription ( "Input second qual file" )
    public DataElementPath getPairedEndQualSecond()
    {
        return pairedEndQualSecond;
    }
    public void setPairedEndQualSecond(DataElementPath pairedEndQualSecond)
    {
        Object oldValue = this.pairedEndQualSecond;
        this.pairedEndQualSecond = pairedEndQualSecond;
        firePropertyChange( "pairedEndQualSecond", oldValue, pairedEndQualSecond );
    }

    @PropertyName ( "Output first csfasta file" )
    @PropertyDescription ( "Output first csfasta file" )
    public DataElementPath getPairedEndCSFastaFirstOutput()
    {
        return pairedEndCSFastaFirstOutput;
    }
    public void setPairedEndCSFastaFirstOutput(DataElementPath pairedEndCSFastaFirstOutput)
    {
        Object oldValue = this.pairedEndCSFastaFirstOutput;
        this.pairedEndCSFastaFirstOutput = pairedEndCSFastaFirstOutput;
        firePropertyChange( "pairedEndCSFastaFirstOutput", oldValue, pairedEndCSFastaFirstOutput );
    }

    @PropertyName ( "Output first qual file" )
    @PropertyDescription ( "Output first qual file" )
    public DataElementPath getPairedEndQualFirstOutput()
    {
        return pairedEndQualFirstOutput;
    }
    public void setPairedEndQualFirstOutput(DataElementPath pairedEndQualFirstOutput)
    {
        Object oldValue = this.pairedEndQualFirstOutput;
        this.pairedEndQualFirstOutput = pairedEndQualFirstOutput;
        firePropertyChange( "pairedEndQualFirstOutput", oldValue, pairedEndQualFirstOutput );
    }

    @PropertyName ( "Output second csfasta file" )
    @PropertyDescription ( "Output second csfasta file" )
    public DataElementPath getPairedEndCSFastaSecondOutput()
    {
        return pairedEndCSFastaSecondOutput;
    }
    public void setPairedEndCSFastaSecondOutput(DataElementPath pairedEndCSFastaSecondOutput)
    {
        Object oldValue = this.pairedEndCSFastaSecondOutput;
        this.pairedEndCSFastaSecondOutput = pairedEndCSFastaSecondOutput;
        firePropertyChange( "pairedEndCSFastaSecondOutput", oldValue, pairedEndCSFastaSecondOutput );
    }

    @PropertyName ( "Output second qual file" )
    @PropertyDescription ( "Output second qual file" )
    public DataElementPath getPairedEndQualSecondOutput()
    {
        return pairedEndQualSecondOutput;
    }
    public void setPairedEndQualSecondOutput(DataElementPath pairedEndQualSecondOutput)
    {
        Object oldValue = this.pairedEndQualSecondOutput;
        this.pairedEndQualSecondOutput = pairedEndQualSecondOutput;
        firePropertyChange( "pairedEndQualSecondOutput", oldValue, pairedEndQualSecondOutput );
    }

    public boolean isPairedEndCSFastaHidden()
    {
        return ! ( libraryType.equals( LIBRARY_TYPE_PAIRED_END ) && inputType.equals( INPUT_TYPE_SOLID ) );
    }

    public TaskProcessor[] getTaskProcessors()
    {
        TaskProcessor[] result = new TaskProcessor[taskProcessorsDPS.size()];
        Iterator<DynamicProperty> propertyIterator = taskProcessorsDPS.propertyIterator();
        int i = 0;
        while(propertyIterator.hasNext())
        {
            DynamicProperty dp = propertyIterator.next();
            result[i++] = (TaskProcessor)dp.getValue();
        }
        return result;
    }

    @PropertyName ( "Processors" )
    @PropertyDescription ( "Read processors to apply" )
    public DynamicPropertySet getTaskProcessorsDPS()
    {
        return taskProcessorsDPS;
    }
    
    public void setTaskProcessorsDPS(DynamicPropertySet taskProcessorsDPS)
    {
        DynamicPropertySet oldValue = this.taskProcessorsDPS;
        this.taskProcessorsDPS = taskProcessorsDPS;
        if(oldValue != null)
            oldValue.forEach( dp -> ((TaskProcessor)dp.getValue()).setParent( null ));
        if(taskProcessorsDPS != null)
            taskProcessorsDPS.forEach( dp -> ((TaskProcessor)dp.getValue()).setParent( this ));
        firePropertyChange( "taskProcessorsDPS", oldValue, taskProcessorsDPS );
    }
}
