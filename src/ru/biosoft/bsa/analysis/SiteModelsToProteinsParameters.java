package ru.biosoft.bsa.analysis;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.analysis.AbstractTableConverterParameters;
import ru.biosoft.analysis.aggregate.NumericAggregator;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import biouml.standard.type.Species;

public class SiteModelsToProteinsParameters extends AbstractTableConverterParameters
{
    private static final long serialVersionUID = 1L;

    private static final String P_VALUE_COLUMN = "P-value";
    private DataElementPath sitesCollection, siteModelsCollection;
    private String[] models;
    
    public SiteModelsToProteinsParameters()
    {
        setSpecies(Species.getDefaultSpecies(null));
        setTargetType(ReferenceTypeRegistry.getDefaultReferenceType().getDisplayName());
        setAggregator(NumericAggregator.createInstance("extreme"));
    }

    @PropertyName("Sites table")
    @PropertyDescription("Select table with the results of \"Site search on gene set\". Such table contains site model ID in each row.")
    public DataElementPath getSitesCollection()
    {
        return sitesCollection;
    }

    public void setSitesCollection(DataElementPath sitesCollection)
    {
        Object oldValue = this.sitesCollection;
        this.sitesCollection = sitesCollection;
        firePropertyChange("sitesCollection", oldValue, sitesCollection);
        if(sitesCollection != null)
        {
            TableDataCollection table = sitesCollection.optDataElement(TableDataCollection.class);
            setSpecies(Species.getDefaultSpecies(table));
            if(getColumnName().equals(ColumnNameSelector.NONE_COLUMN) && table != null
                    && table.getColumnModel().hasColumn(P_VALUE_COLUMN))
            {
                setColumnName(P_VALUE_COLUMN);
            }
        }
    }

    @PropertyName("Profile")
    @PropertyDescription("Select the profile that was used for site search. In most of the cases, profile is selected automatically.")
    public DataElementPath getSiteModelsCollection()
    {
        return siteModelsCollection;
    }

    public void setSiteModelsCollection(DataElementPath siteModelsCollection)
    {
        Object oldValue = this.siteModelsCollection;
        this.siteModelsCollection = siteModelsCollection;
        firePropertyChange("siteModelsCollection", oldValue, siteModelsCollection);
    }

    public DataElementPath getDefaultProfile()
    {
        try
        {
            return DataElementPath.create(getSitesCollection().getDataCollection().getInfo().getProperty(SiteSearchResult.PROFILE_PROPERTY));
        }
        catch( Exception e )
        {
            return null;
        }
    }

    /**
     * If set, then only specified models will be taken into analysis
     */
    public void setModels(String[] models)
    {
        Object oldValue = this.models;
        this.models = models;
        firePropertyChange("models", oldValue, models);
    }

    public String[] getModels()
    {
        return models;
    }
}
