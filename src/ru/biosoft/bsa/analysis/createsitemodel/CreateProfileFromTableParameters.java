package ru.biosoft.bsa.analysis.createsitemodel;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.analysis.SiteSearchResult;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class CreateProfileFromTableParameters extends AbstractAnalysisParameters
{
    private static final String CUTOFFS_COLUMN = "Model cutoff";
    private DataElementPath table, profile, outputProfile;
    private String thresholdsColumn = ColumnNameSelector.NONE_COLUMN;

    @PropertyName("Input table")
    @PropertyDescription("Table containing site models as row names")
    public DataElementPath getTable()
    {
        return table;
    }
    
    public void setTable(DataElementPath table)
    {
        Object oldValue = this.table;
        this.table = table;
        firePropertyChange("table", oldValue, table);
        if(table != null)
        {
            TableDataCollection tdc = table.optDataElement(TableDataCollection.class);
            if(getThresholdsColumn().equals(ColumnNameSelector.NONE_COLUMN) && tdc != null
                    && tdc.getColumnModel().hasColumn(CUTOFFS_COLUMN))
            {
                setThresholdsColumn(CUTOFFS_COLUMN);
            }
        }
    }

    @PropertyName("Reference profile")
    @PropertyDescription("Profile to copy the values from")
    public DataElementPath getProfile()
    {
        return profile;
    }
    
    public void setProfile(DataElementPath profile)
    {
        Object oldValue = this.profile;
        this.profile = profile;
        firePropertyChange("profile", oldValue, profile);
    }

    @PropertyName("Cutoffs column")
    @PropertyDescription("Column containing cutoff values (use 'none' to copy cutoffs from the profile)")
    public String getThresholdsColumn()
    {
        return thresholdsColumn;
    }

    public void setThresholdsColumn(String thresholdsColumn)
    {
        Object oldValue = this.thresholdsColumn;
        this.thresholdsColumn = thresholdsColumn;
        firePropertyChange("thresholdsColumn", oldValue, thresholdsColumn);
    }

    @PropertyName("Output profile")
    @PropertyDescription("Specify the path whether to store output profile")
    public DataElementPath getOutputProfile()
    {
        return outputProfile;
    }

    public void setOutputProfile(DataElementPath outputProfile)
    {
        Object oldValue = this.outputProfile;
        this.outputProfile = outputProfile;
        firePropertyChange("outputProfile", oldValue, outputProfile);
    }

    public DataElementPath getDefaultProfile()
    {
        try
        {
            return DataElementPath.create(getTable().getDataCollection().getInfo().getProperty(SiteSearchResult.PROFILE_PROPERTY));
        }
        catch( Exception e )
        {
            return null;
        }
    }
}