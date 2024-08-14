package biouml.plugins.bindingregions.cisregmodule;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.standard.type.Species;

/**
 * @author yura
 *
 */
public class CisRegModuleParameters extends AbstractAnalysisParameters
{
    private int mode = 3;
    private ExpParameters expParameters = new ExpParameters();
    
    private DataElementPath chipSeqPeaksPath;
    private DataElementPath sequencePath;
    private DataElementPath cisRegModuleTable;
    private int minimalNumberOfOverlaps = 2;
    private Species specie = Species.getDefaultSpecies(null);
    
    
    public static class ExpParameters extends OptionEx
    {
        private DataElementPath sequencePath;
        private DataElementPath trackPath;
        
        @PropertyName( "Sequence path" )
        @PropertyDescription( "Sequence path" )
        
        public DataElementPath getSequencePath()
        {
            return sequencePath;
        }
        public void setSequencePath(DataElementPath sequencePath)
        {
            Object oldValue = this.sequencePath;
            this.sequencePath = sequencePath;
            firePropertyChange("sequencePath", oldValue, sequencePath);
        }
        
        public DataElementPath getTrackPath()
        {
            return trackPath;
        }
        public void setTrackPath(DataElementPath trackPath)
        {
            Object oldValue = this.trackPath;
            this.trackPath = trackPath;
            firePropertyChange("trackPath", oldValue, trackPath);
        }
    }
    
    public static class ExpParametersBeanInfo extends BeanInfoEx2<ExpParameters>
    {
        public ExpParametersBeanInfo()
        {
            super(ExpParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            property( "sequencePath" ).inputElement( AnnotatedSequence.class ).add();
        }
    }

    public int getMode()
    {
        return mode;
    }

    public void setMode(int mode)
    {
        Object oldValue = this.mode;
        this.mode = mode;
        firePropertyChange("*", oldValue, mode);
    }
    
    public ExpParameters getExpParameters()
    {
        return expParameters;
    }
    
    public void setExpParameters(ExpParameters expParameters)
    {
        Object oldValue = this.expParameters;
        this.expParameters = expParameters;
        firePropertyChange("expParameters", oldValue, expParameters);
    }
    
    public boolean isExpParametersHidden()
    {
        return getMode() != 1;
    }
    
///////////////////////////////////////////////////////////////// my old parameters
    
    public DataElementPath getChipSeqPeaksPath()
    {
        return chipSeqPeaksPath;
    }

    public void setChipSeqPeaksPath(DataElementPath chipSeqPeaksPath)
    {
        Object oldValue = this.chipSeqPeaksPath;
        this.chipSeqPeaksPath = chipSeqPeaksPath;
        firePropertyChange("chipSeqPeaksPath", oldValue, chipSeqPeaksPath);
    }

    public DataElementPath getSequencePath()
    {
        return sequencePath;
    }

    public void setSequencePath(DataElementPath sequencePath)
    {
        Object oldValue = this.sequencePath;
        this.sequencePath = sequencePath;
        firePropertyChange("sequencePath", oldValue, sequencePath);
    }

    public int getMinimalNumberOfOverlaps()
    {
        return minimalNumberOfOverlaps;
    }


    public void setMinimalNumberOfOverlaps(int minimalNumberOfOverlaps)
    {
        Object oldValue = this.minimalNumberOfOverlaps;
        this.minimalNumberOfOverlaps = minimalNumberOfOverlaps;
        firePropertyChange("minimalNumberOfOverlaps", oldValue, minimalNumberOfOverlaps);
    }

    
    public Species getSpecie()
    {
        return specie;
    }

    public void setSpecie(Species specie)
    {
        Object oldValue = this.specie;
        this.specie = specie;
        firePropertyChange("specie", oldValue, specie);
    }

    public DataElementPath getCisRegModuleTable()
    {
        return cisRegModuleTable;
    }

    public DataElementPath getCisRegModuleTableSummaryPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "summaryOnBindingRegions");
    }
    
    public DataElementPath getKolmogorovSmirnovUniformityPvaluesTablePath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "kolmogorovSmirnovUniformityPvalues");
    }
    
    public DataElementPath getOverlapsDensityTablePath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "overlapsDensity");
    }

    public DataElementPath getCisModulesTablePath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "cisRegulatoryModules");
    }
    
    public DataElementPath getSummaryOnGenesOverlappedWithCisModulesPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "summaryOnGenesOverlappedWithCisModules");
    }
    
    public DataElementPath getSummaryOnCisModulesOverlappedWithGenesPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "summaryOnCisModulesOverlappedWithGenes");
    }
    
    public DataElementPath getTfClassesInCisModulesOfDifferentTypesPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "tfClassesInCisModulesOfDifferentTypes");
    }
    
    public DataElementPath getKolmogorovSmirnovExponentialityPvaluesTablePath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "kolmogorovSmirnovExponentialityPvalues");
    }

    public DataElementPath getFrequenciesOfTfClassesInCisModulesPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "frequenciesOfTfClassesInCisModules");
    }
 
    public DataElementPath getChisquaredTestForTfClassesIndependencePath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "chisquaredTestForTfClassesIndependence");
    }
    
    public DataElementPath getTimesWaitingForBithOrDeathPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "timesWaitingForBithOrDeath");
    }
    
    public DataElementPath genesSummaryPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "summaryOnGenes");
    }
    
    public DataElementPath histogramsOfTimesWaitingForBithPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "histogramsOfTimesWaitingForBith");
    }
    
    public DataElementPath getExponentiality_KS_OfTimesWaitingForBirthPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "exponentiality_KS_OfTimesWaitingForBirth");
    }
    
    public DataElementPath getExponentiality_KS_OfTimesWaitingForDeathPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "exponentiality_KS_OfTimesWaitingForDeath");
    }

    public DataElementPath getChiSquaredExponentialityOfTimesWaitingForBirthPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "chiSquaredExponentialityOfTimesWaitingForBirth");
    }

    public DataElementPath getChiSquaredExponentialityOfTimesWaitingForDeathPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "chiSquaredExponentialityOfTimesWaitingForDeath");
    }

    public DataElementPath exponentialMixtureForWaitingTimesPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "exponentialMixtureForWaitingTimes");
    }
    
    public DataElementPath summaryOnOverlapsOfBindingRegionsOfSameTfClassPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "summaryOnOverlapsOfBindingRegionsOfSameTfClass");
    }
 
    public DataElementPath populationSizesAndTheirProbabilitiesPath()
    {
        return DataElementPath.create(cisRegModuleTable.optParentCollection(), cisRegModuleTable.getName() + "populationSizesAndTheirProbabilities");
    }
    
    public void setCisRegModuleTable(DataElementPath cisRegModuleTable)
    {
        Object oldValue = this.cisRegModuleTable;
        this.cisRegModuleTable = cisRegModuleTable;
        firePropertyChange("chisRegModuleTable", oldValue, cisRegModuleTable);
    }

    
}