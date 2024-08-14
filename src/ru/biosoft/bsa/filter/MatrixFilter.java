package ru.biosoft.bsa.filter;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.filter.MutableFilter;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.TranscriptionFactor;
import ru.biosoft.bsa.TransfacTranscriptionFactor;

import ru.biosoft.jobcontrol.FunctionJobControl;
import com.developmentontheedge.beans.Option;

@SuppressWarnings ( "serial" )
public class MatrixFilter extends MutableFilter<FrequencyMatrix>
{
    protected NameFilter nameFilter;
    protected TranscriptionFactorFilter transcriptionFactorFilter;

    public MatrixFilter(DataElementPath classificationsRoot)
    {
        this(null,null,classificationsRoot);
    }

    public MatrixFilter( FunctionJobControl jobControl, DataElementPath classificationsRoot )
    {
        this(null,jobControl,classificationsRoot);
    }

    public MatrixFilter(Option parent,FunctionJobControl jobControl, DataElementPath classificationsRoot)
    {
        super(parent);
        
        nameFilter = new MatrixIDFilter();

        if( jobControl!=null )
            jobControl.setPreparedness( jobControl.getPreparedness()+(int)((100-jobControl.getPreparedness())*0.3) );

        if(classificationsRoot != null)
            transcriptionFactorFilter = new TranscriptionFactorFilter(jobControl, classificationsRoot);
    }

    /** @todo implement */
    @Override
    public boolean isAcceptable(FrequencyMatrix de)
    {
        if(nameFilter.isEnabled() && !nameFilter.isAcceptable(de))
            return false;

        if(transcriptionFactorFilter == null || !transcriptionFactorFilter.isEnabled())
            return true;

        BindingElement be = de.getBindingElement();
        if(be == null) return false;
        for(TranscriptionFactor tf: be)
        {
            if(tf instanceof TransfacTranscriptionFactor && transcriptionFactorFilter.isAcceptable((TransfacTranscriptionFactor)tf))
                return true;
        }
        return false;
    }
}
