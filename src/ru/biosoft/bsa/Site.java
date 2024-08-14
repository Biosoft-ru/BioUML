
package ru.biosoft.bsa;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.core.DataElement;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.util.bean.StaticDescriptor;

/**
 * General sequence site definition.
 *
 * @pending whether it should be type of standard BioUML module
 * @pending URLEntity
 */
@PropertyName("site")
public interface Site extends DataElement, SiteType, Basis, StrandType, Precision
{
    public static final String SCORE_PROPERTY = "score";
    public static final PropertyDescriptor SCORE_PD = StaticDescriptor.create(SCORE_PROPERTY);
    
    /**
     * Returns the site type.
     *
     * @return the site type.
     * @see SiteType
     */
    public String getType();

    /**
     * Returns the site basis.
     *
     * @return the site basis.
     * @see Basis
     */
    public int getBasis();

    /**
     * Returns the site start position .
     *
     * @return the site start position (first position is 1).
     */
    public int getStart();

    /**
     * Returns the site length.
     *
     * @return the site length.
     */
    public int getLength();

    /**
     * Returns the site left position .
     *
     * @return the site left position (first position is 1).
     */
    public int getFrom();

    /**
     * Returns the site right position .
     *
     * @return the site right position (first position is 1).
     */
    public int getTo();
    
    /**
     * @return the Interval of the site
     */
    public Interval getInterval();

    /**
     * Returns the site boundaries precision.
     *
     * @return the site boundaries precision.
     * @see Precision
     */
    public int getPrecision();

    /**
     * Returns the site strand.
     *
     * @return the site strand.
     */
    public int getStrand();

    /**
     * Returns the site sequence.
     *
     * @return the site sequence.
     * @see Sequence
     */
    public Sequence getSequence();

    /**
     * Returns original sequence on which site is located
     */
    public Sequence getOriginalSequence();

    /**
     * Get comment for site
     * @return comment
     */
    public String getComment();
    
    /**
     * Get score for site
     * @return score
     */
    public double getScore();

    /**
     * Returns a dynamic set of properties associated with the site.
     *
     * @return a dynamic set of properties associated with the site.
     */
    public DynamicPropertySet getProperties();
}
