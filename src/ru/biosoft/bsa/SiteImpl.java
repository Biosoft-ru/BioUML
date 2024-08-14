package ru.biosoft.bsa;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.MutableDataElement;
import ru.biosoft.bsa.analysis.FrequencyMatrix;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.Option;

/**
 * Default site interface implementation.
 *
 * @pending fire propertyChange events.
 * @pending clone - whether all properties are cloned
 * @pending URL related issues
 */
public class SiteImpl extends Option implements Site, MutableDataElement
{
    ///////////////////////////////////////////////////////////////////
    // Constructors
    //

    // PENDING: should be removed
    public SiteImpl(DataCollection<?> parent, String name,
                    int start, int length, int strand, Sequence sequence)
    {
        this(parent, name, TYPE_MISC_FEATURE, BASIS_USER, start, length, PRECISION_EXACTLY,
             strand, sequence, new DynamicPropertySetAsMap());
    }

    public SiteImpl(DataCollection<?> parent, String name, String type, int basis,
                    int start, int length, int strand, Sequence sequence)
    {
        this(parent, name, type, basis, start, length, PRECISION_EXACTLY,
             strand, sequence, new DynamicPropertySetAsMap());
    }

    public SiteImpl(DataCollection<?> parent, String name, String type, int basis,
                    int start, int length, int precision, int strand, Sequence sequence,
                    DynamicPropertySet properties)
    {
        this(parent, name, type, basis,
             start, length, precision, strand, sequence,
             null, properties);
    }

    public SiteImpl(DataCollection<?> parent, String name, String type, int basis,
                    int start, int length, int precision, int strand, Sequence sequence,
                    String comment, DynamicPropertySet properties)
    {
        this.name = name;
        this.origin = parent;
        if(origin instanceof Option)
                setParent((Option)origin);

        this.type = type;
        this.basis = basis;
        this.start = start;
        this.length = length;
        this.strand = strand;
        this.precision = precision;
        this.sequence = sequence;
        this.properties = properties;
        this.comment = comment;
    }

    @Override
    public String toString()
    {
        return getType() + "[" + getFrom() + "-" + getTo() + "]";
    }

    ///////////////////////////////////////////////////////////////////
    // DataCollection issues
    //

    protected String name;
    @Override
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        String oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    private DataCollection<?> origin;
    @Override
    public DataCollection<?> getOrigin()
    {
        return origin;
    }
    public void setOrigin(DataCollection<?> origin)
    {
        DataCollection<?> oldValue = this.origin;
        if((oldValue == null && origin != null) || (oldValue != null && !oldValue.equals(origin)))
        {
            this.origin = origin;
            firePropertyChange("origin", oldValue, origin);
            updateSequence();
            validate();
        }
    }

    /** Used in BeanInfo to enable/disable some properties editing. */
    protected boolean editable;
    public boolean isNotEditable()
    {
        return !editable;
    }
    public void setEditable(boolean editable)
    {
        this.editable = editable;
    }

    public SiteImpl clone(DataCollection<?> origin)
    {
        return new SiteImpl(origin, getName(), type, basis, start, length,
                            precision, strand, sequence, comment, (DynamicPropertySet)properties.clone());
    }

    ///////////////////////////////////////////////////////////////////
    // Site interface implementation
    //

    protected String type;
    @Override
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
        //String oldType = type;
        //firePropertyChange("type", oldType, type);
    }

    protected int basis;
    @Override
    public int getBasis()
    {
        return basis;
    }
    public void setBasis(int basis)
    {
        int oldValue = this.basis;
        this.basis = basis;
        firePropertyChange( "basis", oldValue, basis );
    }

    /** The site start position (first position is 1). */
    protected int start;
    @Override
    public int getStart()
    {
        return start;
    }
    public void setStart(int start)
    {
        // validation
        this.start = start;
        firePropertyChange("start", null, null); // to update
        validate();
        //        if (start < 1)
        //            start = 1;
        //        int seqLen = sequence.getLength();
        //        if (start > seqLen)
        //            start = seqLen;
        //
        //        if (this.start != start)
        //        {
        //            int oldValue = this.start;
        //            this.start = start;
        //            firePropertyChange( "start", new Integer( oldValue ), new Integer( start ) );
        //            firePropertyChange( "from", null, null ); // to update
        //            firePropertyChange( "to", null, null ); // to update
        //        }
    }

    protected void validate()
    {
        int sequenceLength = sequence.getLength();
        if(start < 1)
        {
            start = 1;
            firePropertyChange("start", null, null);
        }
        else if(start > sequenceLength)
        {
            start = sequenceLength;
            firePropertyChange("start", null, null);
        }
        if(length < 1)
        {
            length = 1;
            firePropertyChange("length", null, null);
        }
        else if(strand == Site.STRAND_PLUS)
        {
            if((start + length - 1) > sequenceLength)
            {
                length = sequenceLength - start + 1;
                firePropertyChange("length", null, null);
            }
        }
        else if(strand == Site.STRAND_MINUS)
        {
            if((start - length) < 0)
            {
                length = start;
                firePropertyChange("length", null, null);
            }
        }

        firePropertyChange("from", null, null); // to update
        firePropertyChange("to", null, null); // to update
        //firePropertyChange("start", null, null ); // to update
        //firePropertyChange("length", null, null ); // to update
        //firePropertyChange("strand", null, null ); // to update

        updateSequence();

    }

    /** The site length. */
    protected int length;
    @Override
    public int getLength()
    {
        return length;
    }
    public void setLength(int length)
    {
        this.length = length;
        firePropertyChange("length", null, null); // to update
        validate();
    }

    @Override
    public int getFrom()
    {
        if(strand != StrandType.STRAND_MINUS)
            return start;
        return start - length + 1;
    }

    /**
     * to allow setting from property at creating new site
     *
     * @pending we do not consider circular sequences
     */
    public void setFrom(int from)
    {
        if(from < 1)
            from = 1;

        int to = getTo();
        if(strand == StrandType.STRAND_MINUS)
            start = from;
        else
            start = from - length + 1;
        firePropertyChange("start", null, null); // to update

        length = to - from + 1;
        firePropertyChange("length", null, null); // to update

        validate();
    }

    @Override
    public int getTo()
    {
        return getFrom() + length - 1;
    }

    public void setTo(int to)
    {
        if(strand == Site.STRAND_MINUS)
            setStart(to - length);
        else
            setLength(to - getFrom() + 1);
        //        else if (strand == Site.STRAND_PLUS)
        //            setStart((getStart() - (getLength()) - 1));
    }

    @Override
    public Interval getInterval()
    {
        return new Interval(getFrom(), getTo());
    }

    protected int precision;
    @Override
    public int getPrecision()
    {
        return precision;
    }
    public void setPrecision(int precision)
    {
        int oldValue = this.precision;
        this.precision = precision;
        firePropertyChange( "precision", oldValue, precision );
    }

    /** The site strand. */
    public int strand;
    @Override
    public int getStrand()
    {
        return strand;
    }
    public void setStrand(int strand)
    {
        int oldStrand = this.strand;
        this.strand = strand;
        firePropertyChange("strand", null, null);

        if( oldStrand == strand )
            return;
        else if( strand == Site.STRAND_MINUS )
            setStart( getStart() + getLength() - 1 );
        else if( oldStrand == Site.STRAND_MINUS )
            setStart( getStart() - getLength() + 1 );
    }


    /** The site sequence. */
    protected Sequence sequence;
    protected Sequence sequenceRegion = null;

    @Override
    public Sequence getSequence()
    {
        if(sequenceRegion == null)
        {
            if(sequence == null)
                if(getOrigin() != null)
                    sequence = ((AnnotatedSequence)getOrigin()).getSequence();

            if(sequence != null)
            {
                int sequenceStart = 1;
                try
                {
                    int TSS = (Integer)getProperties().getProperty("TSS").getValue();
                    sequenceStart = getStrand()==STRAND_MINUS?TSS-getTo():getFrom()-TSS;
                }
                catch(Exception e)
                {
                }
                sequenceRegion = new SequenceRegion( sequence, start, length, sequenceStart, strand == Site.STRAND_MINUS, false );
            }
        }
        return sequenceRegion;
    }

    protected Sequence canonicSequenceRegion = null;
    /**
     * @todo comment
     */
    public Sequence getCanonicSequence()
    {
        if(canonicSequenceRegion == null)
        {
            if(sequence == null)
                if(getOrigin() != null)
                    sequence = ((AnnotatedSequence)getOrigin()).getSequence();

            if(sequence != null)
                canonicSequenceRegion = new SequenceRegion( sequence, getFrom(), length, false, false );
        }
        return canonicSequenceRegion;
    }

    protected void updateSequence()
    {
        Sequence oldSequence = getSequence();
        sequenceRegion = null;
        firePropertyChange("sequence", oldSequence, sequenceRegion);

        Sequence oldCanonicSequence = getCanonicSequence();
        canonicSequenceRegion = null;
        firePropertyChange("canonicSequence", oldCanonicSequence, canonicSequenceRegion);
        //firePropertyChange( "canonicSequence/length", null, null );
    }

    private String comment;
    @Override
    public String getComment()
    {
        if(comment == null) comment = "";
        return comment;
    }
    /**
     * Set comment
     * @param comment a comment
     */
    public void setComment(String comment)
    {
        String oldType = this.comment;
        this.comment = comment;
        firePropertyChange("comment", oldType, comment);
    }

    private DynamicPropertySet properties = null;
    @Override
    final public DynamicPropertySet getProperties()
    {
        if(properties == null)
            properties = new DynamicPropertySetAsMap();

        return properties;
    }

    @Override
    public Sequence getOriginalSequence()
    {
        return sequence;
    }
    
    public String getSequenceName()
    {
        return sequence == null?null:sequence.getName();
    }
    
    @Override
    public double getScore()
    {
        Object obj = getProperties().getProperty(SCORE_PROPERTY).getValue();
        if(obj instanceof Number) return ((Number)obj).doubleValue();
        if(obj instanceof String) return Double.parseDouble((String)obj);
        throw new UnsupportedOperationException("No score for this site");
    }
    
    public String getModel()
    {
        Object obj = getProperties().getValue("siteModel");
        if(obj instanceof FrequencyMatrix) return ((FrequencyMatrix)obj).getName();
        if(obj instanceof SiteModel) return ((SiteModel)obj).getName();
        if(obj instanceof String) return (String)obj;
        throw new UnsupportedOperationException("No model for this site");
    }
}
