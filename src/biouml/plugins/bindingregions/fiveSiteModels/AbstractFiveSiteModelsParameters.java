package biouml.plugins.bindingregions.fiveSiteModels;

import java.util.Objects;

import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.gtrd.utils.SiteModelUtils;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.table.columnbeans.ColumnNameSelector;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/***
 * 
 * @author lan
 *
 */

public abstract class AbstractFiveSiteModelsParameters extends AbstractAnalysisParameters
{
    private BasicGenomeSelector dbSelector;
    private DataElementPath trackPath;
    private DataElementPath matrixPath;
    private boolean aroundSummit;
    private int minRegionLength = 300;
    private String[] siteModelTypes = SiteModelUtils.getAvailableSiteModelTypes();
    private String siteModelType = SiteModelUtils.IPS_MODEL;
    private Species species = Species.getDefaultSpecies(null);
    private int window = IPSSiteModel.DEFAULT_WINDOW;
    private DataElementPath pathToTableWithSequenceSample;
    private String nameOfTableColumnWithSequenceSample;
    private boolean areBothStrands;
    private int bestSitesPercentage = 15;
    private DataElementPath outputPath;
    // 29.04.22
    private double pValue;
    
    public AbstractFiveSiteModelsParameters()
    {
        setDbSelector(new BasicGenomeSelector());
    }
    
    @PropertyName(MessageBundle.PN_DB_SELECTOR)
    @PropertyDescription(MessageBundle.PD_DB_SELECTOR)
    public BasicGenomeSelector getDbSelector()
    {
        return dbSelector;
    }
    public void setDbSelector(BasicGenomeSelector dbSelector)
    {
        Object oldValue = this.dbSelector;
        this.dbSelector = dbSelector;
        dbSelector.setParent(this);
        firePropertyChange("dbSelector", oldValue, dbSelector);
    }
    
    @PropertyName(MessageBundle.PN_SITE_MODEL_TYPES)
    @PropertyDescription(MessageBundle.PD_SITE_MODEL_TYPES)
    public String[] getSiteModelTypes()
    {
        return siteModelTypes;
    }
    public void setSiteModelTypes(String[] siteModelTypes)
    {
        Object oldValue = this.siteModelTypes;
        this.siteModelTypes = siteModelTypes;
        firePropertyChange("siteModelTypes", oldValue, siteModelTypes);
    }
    
    @PropertyName(MessageBundle.PN_SITE_MODEL_TYPE)
    @PropertyDescription(MessageBundle.PD_SITE_MODEL_TYPE)
    public String getSiteModelType()
    {
        return siteModelType;
    }
    public void setSiteModelType(String siteModelType)
    {
        Object oldValue = this.siteModelType;
        this.siteModelType = siteModelType;
        firePropertyChange("*", oldValue, siteModelType);
    }
    
    // 24.03.22
    @PropertyName(MessageBundle.PN_TRACK_PATH_WITH_RA_SCORES_)
    @PropertyDescription(MessageBundle.PD_TRACK_PATH_WITH_RA_SCORES_)
    public DataElementPath getTrackPath()
    {
        return trackPath;
    }
    public void setTrackPath(DataElementPath trackPath)
    {
        Object oldValue = this.trackPath;
        this.trackPath = trackPath;
        if( ! Objects.equals(oldValue, trackPath) )
        {
            Track t = trackPath.optDataElement(Track.class);
            if( t != null )
                getDbSelector().setFromTrack(t);
        }
        firePropertyChange("trackPath", oldValue, trackPath);
    }

    @PropertyName(MessageBundle.PN_MATRIX_PATH)
    @PropertyDescription(MessageBundle.PD_MATRIX_PATH)
    public DataElementPath getMatrixPath()
    {
        return matrixPath;
    }
    public void setMatrixPath(DataElementPath matrixPath)
    {
        Object oldValue = this.matrixPath;
        this.matrixPath = matrixPath;
        firePropertyChange("matrixPath", oldValue, matrixPath);
    }

    @PropertyName(MessageBundle.PN_AROUND_SUMMIT)
    @PropertyDescription(MessageBundle.PD_AROUND_SUMMIT)
    public boolean isAroundSummit()
    {
        return aroundSummit;
    }
    public void setAroundSummit(boolean aroundSummit)
    {
        Object oldValue = this.aroundSummit;
        this.aroundSummit = aroundSummit;
        firePropertyChange("aroundSummit", oldValue, aroundSummit);
    }
    
    @PropertyName(MessageBundle.PN_MIN_REGION_LENGTH)
    @PropertyDescription(MessageBundle.PD_MIN_REGION_LENGTH)
    public int getMinRegionLength()
    {
        return minRegionLength;
    }
    public void setMinRegionLength(int minRegionLength)
    {
        Object oldValue = this.minRegionLength;
        this.minRegionLength = minRegionLength;
        firePropertyChange("minRegionLength", oldValue, minRegionLength);
    }

    @PropertyName(MessageBundle.PN_SPECIES)
    @PropertyDescription(MessageBundle.PD_SPECIES)
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
    
    @PropertyName(MessageBundle.PN_WINDOW_SIZE)
    @PropertyDescription(MessageBundle.PD_WINDOW_SIZE)
    public int getWindow()
    {
        return window;
    }
    public void setWindow(int window)
    {
        Object oldValue = this.window;
        this.window = window;
        firePropertyChange("window", oldValue, window);
    }
    
    // 29.04.22
    @PropertyName(MessageBundle.PN_P_VALUE_THRESHOLD)
    @PropertyDescription(MessageBundle.PD_P_VALUE_THRESHOLD)
    public double getPValue()
    {
        return pValue;
    }
    public void setPValue(double pValue)
    {
        Object oldValue = this.pValue;
        this.pValue = pValue;
        firePropertyChange("pValue", oldValue, pValue);
    }
    
    @PropertyName(MessageBundle.PN_PATH_TO_TABLE_SEQUENCE_SAMPLE)
    @PropertyDescription(MessageBundle.PD_PATH_TO_TABLE_SEQUENCE_SAMPLE)
    public DataElementPath getPathToTableWithSequenceSample()
    {
        return pathToTableWithSequenceSample;
    }
    public void setPathToTableWithSequenceSample(DataElementPath pathToTableWithSequenceSample)
    {
        Object oldValue = this.pathToTableWithSequenceSample;
        this.pathToTableWithSequenceSample = pathToTableWithSequenceSample;
        firePropertyChange("pathToTableWithSequenceSample", oldValue, pathToTableWithSequenceSample);
        setNameOfTableColumnWithSequenceSample(ColumnNameSelector.getColumn(pathToTableWithSequenceSample, getNameOfTableColumnWithSequenceSample()));
    }
    
    @PropertyName(MessageBundle.PN_NAME_OF_TABLE_COLUMN_WITH_SEQUENCE_SAMPLE)
    @PropertyDescription(MessageBundle.PD_NAME_OF_TABLE_COLUMN_WITH_SEQUENCE_SAMPLE)
    public String getNameOfTableColumnWithSequenceSample()
    {
        return nameOfTableColumnWithSequenceSample;
    }
    public void setNameOfTableColumnWithSequenceSample(String nameOfTableColumnWithSequenceSample)
    {
        Object oldValue = this.nameOfTableColumnWithSequenceSample;
        this.nameOfTableColumnWithSequenceSample = nameOfTableColumnWithSequenceSample;
        firePropertyChange("nameOfTableColumnWithSequenceSample", oldValue, nameOfTableColumnWithSequenceSample);
    }
    
    @PropertyName(MessageBundle.PN_ARE_BOTH_STRANDS)
    @PropertyDescription(MessageBundle.PD_ARE_BOTH_STRANDS)
    public boolean getAreBothStrands()
    {
        return areBothStrands;
    }
    public void setAreBothStrands(boolean areBothStrands)
    {
        Object oldValue = this.areBothStrands;
        this.areBothStrands = areBothStrands;
        firePropertyChange("areBothStrands", oldValue, areBothStrands);
    }

    @PropertyName ( MessageBundle.PN_BEST_SITES_PERCENTAGE )
    @PropertyDescription ( MessageBundle.PD_BEST_SITES_PERCENTAGE )
    public int getBestSitesPercentage()
    {
        return bestSitesPercentage;
    }
    public void setBestSitesPercentage(int bestSitesPercentage)
    {
        Object oldValue = this.bestSitesPercentage;
        this.bestSitesPercentage = bestSitesPercentage;
        firePropertyChange("bestSitesPercentage", oldValue, bestSitesPercentage);
    }

    @PropertyName(MessageBundle.PN_OUTPUT_PATH)
    @PropertyDescription(MessageBundle.PD_OUTPUT_PATH)
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }
    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange("outputPath", oldValue, outputPath);
    }
    
    public boolean isWindowHidden()
    {
        String siteModelType = getSiteModelType();
        return( ! siteModelType.equals(SiteModelUtils.IPS_MODEL) && ! siteModelType.equals(SiteModelUtils.LOG_IPS_MODEL));
    }
    
    public static class SiteModelTypeEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return SiteModelUtils.getAvailableSiteModelTypes();
        }
    }
}
