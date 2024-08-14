package ru.biosoft.bsa.analysis.createsitemodel;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.SiteModelUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.standard.type.Species;

/**
 * @author lan
 *
 */
public class FilterProfileByGeneSetParameters extends AbstractAnalysisParameters
{
    private DataElementPath table, profile, output;
    private Species species = Species.getDefaultSpecies(null);
    
    public FilterProfileByGeneSetParameters()
    {
        setProfile(getDefaultProfile());
    }

    public DataElementPath getDefaultProfile()
    {
        return SiteModelUtils.getDefaultProfile();
    }
    
    @PropertyName("Gene set")
    @PropertyDescription("Table with genes to filter profile by")
    public DataElementPath getTable()
    {
        return table;
    }

    public void setTable(DataElementPath table)
    {
        Object oldValue = this.table;
        this.table = table;
        firePropertyChange("table", oldValue, table);
        setSpecies(Species.getDefaultSpecies(table == null ? null : table.optDataCollection()));
    }

    @PropertyName("Reference profile")
    @PropertyDescription("Site models from this profile which map to genes from gene set will be copied to the result")
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

    @PropertyName("Output path")
    @PropertyDescription("Where to store the result")
    public DataElementPath getOutput()
    {
        return output;
    }

    public void setOutput(DataElementPath output)
    {
        Object oldValue = this.output;
        this.output = output;
        firePropertyChange("output", oldValue, output);
    }

    @PropertyName ( "Species" )
    @PropertyDescription ( "Select species" )
    public Species getSpecies()
    {
        return species;
    }

    public void setSpecies(Species species)
    {
        Object oldValue = this.species;
        this.species = species;
        firePropertyChange("species", oldValue, species);
    }
}
