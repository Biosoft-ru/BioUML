package ru.biosoft.analysis;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.BeanUtil;


public class HypergeometricAnalysisParameters extends UpDownIdentificationParameters
{
    private Boolean isDetailed = false;
    private Boolean isControlAveraged = true;
    private Double bv = 1.0;

    private DataElementPath matchingCollection;
    private String newKeySource;

    public HypergeometricAnalysisParameters()
    {
        matchingCollection = null;
        addPropertyChangeListener(event -> {
            if( event.getPropertyName().equals("annotationCollection") )
            {
                try
                {
                    DataCollection<?> dataCollection = getMatchingCollection();
                    DataElement dataElement = dataCollection.iterator().next();
                    String newKey = BeanUtil.getPropertiesList(dataElement)[0];
                    setNewKeySource(newKey);
                }
                catch( Exception ex )
                {
                }
            }
        });
    }
    public void setMatchingCollectionPath(DataElementPath collection)
    {
        Object oldValue = this.matchingCollection;
        this.matchingCollection = collection;
        firePropertyChange("annotationCollection", oldValue, this.matchingCollection);
    }
    public DataElementPath getMatchingCollectionPath()
    {
        return matchingCollection;
    }

    public void setMatchingCollection(DataCollection collection)
    {
        setMatchingCollectionPath(collection.getCompletePath());
    }
    public DataCollection getMatchingCollection()
    {
        return ( matchingCollection != null ) ? matchingCollection.optDataCollection() : null;
    }

    public String getNewKeySource()
    {
        return newKeySource;
    }
    public void setNewKeySource(String field)
    {
        String oldVal = newKeySource;
        newKeySource = field;
        firePropertyChange("newKeySource", oldVal, field);
        firePropertyChange("*", null, null);
    }


    public void setControlAveraged(Boolean isControlAveraged)
    {
        Boolean oldValue = this.isControlAveraged;
        this.isControlAveraged = isControlAveraged;
        firePropertyChange("isControlAveraged", oldValue, isControlAveraged);
    }
    public Boolean isControlAveraged()
    {
        return this.isControlAveraged;
    }

    public Boolean isDetailed()
    {
        return this.isDetailed;
    }
    public void setDetailed(Boolean isDetailed)
    {
        Boolean oldValue = this.isDetailed;
        this.isDetailed = isDetailed;
        firePropertyChange("isDetailed", oldValue, isDetailed);
    }

    public Double getBv()
    {
        return this.bv;
    }
    public void setBv(Double bv)
    {
        Double oldValue = this.bv;
        this.bv = bv;
        firePropertyChange("bv", oldValue, isDetailed);
    }
}
