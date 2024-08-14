package ru.biosoft.bsa.analysis;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.SiteModelUtils;
import biouml.standard.type.Species;

@SuppressWarnings ( "serial" )
public class GeneSetSiteSearchParameters extends AbstractAnalysisParameters
{
    private Integer from, to;
    private Double pvalueCutoff = 0.1;
    private Boolean optimizeCutoff = true;
    private Boolean optimizeWindow = false;
    private boolean deleteNonOptimized = true;
    private Species species;

    private DataElementPath profilePath, yesSetPath, noSetPath;
    private DataElementPath outputPath;
    private boolean overrepresentedOnly = true;

    public GeneSetSiteSearchParameters()
    {
        from = -1000;
        to = 100;
        setSpecies( Species.getDefaultSpecies( null ) );

        setProfilePath( getDefaultProfile() );
    }

    public DataElementPath getDefaultProfile()
    {
        return SiteModelUtils.getDefaultProfile();
    }

    public DataCollection<?> getProfile()
    {
        return profilePath == null ? null : profilePath.optDataCollection();
    }

    public void setProfile(DataCollection<?> profile)
    {
        setProfilePath( DataElementPath.create( profile ) );
    }

    public Integer getFrom()
    {
        return from;
    }
    public void setFrom(Integer from)
    {
        Object oldValue = this.from;
        this.from = from;
        firePropertyChange( "from", oldValue, this.from );
    }
    public Integer getTo()
    {
        return to;
    }
    public void setTo(Integer to)
    {
        Object oldValue = this.to;
        this.to = to;
        firePropertyChange( "to", oldValue, this.to );
    }
    public DataCollection<?> getYesSet()
    {
        return yesSetPath == null ? null : yesSetPath.optDataCollection();
    }

    public void setYesSet(DataCollection<?> yesSet)
    {
        setYesSetPath( DataElementPath.create( yesSet ) );
    }

    public DataCollection<?> getNoSet()
    {
        return noSetPath == null ? null : noSetPath.optDataCollection();
    }

    public void setNoSet(DataCollection<?> noSet)
    {
        setNoSetPath( DataElementPath.create( noSet ) );
    }

    public DataElementPath getProfilePath()
    {
        return profilePath;
    }

    public void setProfilePath(DataElementPath profilePath)
    {
        Object oldValue = this.profilePath;
        this.profilePath = profilePath;
        firePropertyChange( "profilePath", oldValue, this.profilePath );
    }

    public DataElementPath getYesSetPath()
    {
        return yesSetPath;
    }

    public void setYesSetPath(DataElementPath yesSetPath)
    {
        Object oldValue = this.yesSetPath;
        this.yesSetPath = yesSetPath;
        firePropertyChange( "yesSetPath", oldValue, this.yesSetPath );
        if( yesSetPath != null )
            setSpecies( Species.getDefaultSpecies( yesSetPath.optDataCollection() ) );
    }

    public DataElementPath getNoSetPath()
    {
        return noSetPath;
    }

    public void setNoSetPath(DataElementPath noSetPath)
    {
        Object oldValue = this.noSetPath;
        this.noSetPath = noSetPath;
        firePropertyChange( "noSetPath", oldValue, this.noSetPath );
    }

    public Boolean getOptimizeCutoff()
    {
        return optimizeCutoff;
    }

    public void setOptimizeCutoff(Boolean optimizeCutoff)
    {
        Object oldValue = this.optimizeCutoff;
        this.optimizeCutoff = optimizeCutoff;
        firePropertyChange( "optimizeCutoff", oldValue, this.optimizeCutoff );
    }

    public Double getPvalueCutoff()
    {
        return pvalueCutoff;
    }

    public void setPvalueCutoff(Double pvalueCutoff)
    {
        Object oldValue = this.pvalueCutoff;
        this.pvalueCutoff = pvalueCutoff;
        firePropertyChange( "pvalueCutoff", oldValue, this.pvalueCutoff );
    }

    public Boolean getOptimizeWindow()
    {
        return optimizeWindow;
    }

    public void setOptimizeWindow(Boolean optimizeWindow)
    {
        Object oldValue = this.optimizeWindow;
        this.optimizeWindow = optimizeWindow;
        firePropertyChange( "optimizeWindow", oldValue, this.optimizeWindow );
    }

    public Species getSpecies()
    {
        return species;
    }

    public void setSpecies(Species species)
    {
        Object oldValue = this.species;
        this.species = species;
        firePropertyChange( "species", oldValue, this.species );
    }

    public DataElementPath getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange( "outputPath", oldValue, this.outputPath );
    }

    public void setOverrepresentedOnly(boolean overrepresentedOnly)
    {
        Object oldValue = this.overrepresentedOnly;
        this.overrepresentedOnly = overrepresentedOnly;
        firePropertyChange( "overrepresentedOnly", oldValue, this.overrepresentedOnly );
    }

    public boolean isOverrepresentedOnly()
    {
        return overrepresentedOnly;
    }

    @Override
    public @Nonnull String[] getInputNames()
    {
        return new String[] {"yesSetPath", "noSetPath"};
    }

    @Override
    public @Nonnull String[] getOutputNames()
    {
        return new String[] {"outputPath"};
    }

    public boolean isDeleteNonOptimized()
    {
        return deleteNonOptimized;
    }

    public void setDeleteNonOptimized(boolean deleteNonOptimized)
    {
        Object oldValue = this.deleteNonOptimized;
        this.deleteNonOptimized = deleteNonOptimized;
        firePropertyChange( "deleteNonOptimized", oldValue, deleteNonOptimized );
    }

    public boolean isOptimizationOptionsHidden()
    {
        return !getOptimizeCutoff() && !getOptimizeWindow();
    }
}
