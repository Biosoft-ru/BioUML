package ru.biosoft.bsa;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import com.developmentontheedge.beans.annot.PropertyName;

/** @todo Comment. */
@ClassIcon("resources/sequence.gif")
@PropertyName("sequence")
public interface AnnotatedSequence extends DataCollection<Track>
{
    String SITE_SEQUENCE_PROPERTY = "site-sequence";
    
    public Sequence getSequence();
    
    public DynamicPropertySet getProperties();
}

