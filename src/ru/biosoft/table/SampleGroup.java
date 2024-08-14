package ru.biosoft.table;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.MutableDataElementSupport;

/**
 * Group of samples from microarray experiment.
 *
 * List of samples from a given microarray can be initialized by string pattern
 * (like pages in MS Word).
 *
 * Exact format for sampes pattern is:
 * <pre>
 * patter    :== value ( delimiter value)*
 * value     :== sample_id | sample_number | interval
 * interval  :== sample_number "-" sample_number
 * delimiter :== ',' |  ';'
 * </pre>
 *
 * @see Sample
 */
@SuppressWarnings ( "serial" )
public class SampleGroup extends MutableDataElementSupport
{
    public SampleGroup(DataCollection<?> origin, String name)
    {
        super(origin, name);
        samplesList = new ArrayList<>();
    }

    private String pattern;
    public String getPattern()
    {
        return pattern;
    }
    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    protected List<Sample> samplesList;
    public List<Sample> getSamplesList()
    {
        return samplesList;
    }
    
    protected String description;
    public String getDescription()
    {
        return description;
    }
    public void setDescription(String description)
    {
        this.description = description;
    }
}


