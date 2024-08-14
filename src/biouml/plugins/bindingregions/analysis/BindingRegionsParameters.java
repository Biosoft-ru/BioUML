package biouml.plugins.bindingregions.analysis;

import one.util.streamex.IntStreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;
import ru.biosoft.util.OptionEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.editors.StringTagEditor;

/**
 * @author yura
 *
 */
public class BindingRegionsParameters extends AbstractAnalysisParameters
{
    private static final String[] MODES = {
                                           //"0. Preliminary identification of the auxiliary information about genome (creation of tables: '_chromosomeGaps', '_chromosomeLengths', '_genes', 'summaryOfGenes')",
                                           "0. -> GatheringGenomeStatistics",
                                           //"1. Summary of non-merged ChIP-Seq tracks (creation of tables: 'summaryOfTracks', 'summaryOfCellLines', 'summaryOfTfClasses', 'countsOfTfClassesInCellLines')",
                                           "1. -> NonmergedChIPSeqSummary",
                                           // "2. Create cell-specific tracks with merged binding regions (creation of tables: 'bindingRegions_in_[CellLine*]')"
                                           "2. -> MergeCellLineBindingRegions",
                                           // "3. Summary of binding regions, density of overlaps (creation of tables: 'summaryOfBindingRegions', 'densityOfBindingRegionOverlaps', 'chart_densityOfBindingRegionOverlaps')",
                                           "3. -> BindingRegionsSummary",
                                           "4. Relationship between maximal IpsScores and {numbers of overlaps, length of binding regions} (creation of tables: 'charts_densitiesOfIpsScores')",
                                           // "5. Mixture of normal components for maximal IPS scores predicted in binding regions (singleTrack) of given tfClass (creation of tables: 'chart_normalMixtureForMaximalIpsScores')",
                                           "5. -> NormalComponentsMixture",
                                           // "6. Calculation of matrix by using mixture of normal components for maximal IPS scores",
                                           "6. -> CreateMatrixByMixture",
                                           // "7. Identification of cis-modules1",
                                           "7. -> CisModuleIdentification",
                                           // "8. Identification of cis-modules2",
                                           "8. -> CisModuleIdentification",
                                           "9. ",
                                           "10. ",
                                           "11. Prediction of sites by given matrix in binding regions of given tfClass (creation of tables: 'matrixName_in_tfClass')",
                                           "12. Move sites from files to tracks",
                                           "13. print sequences for Maxim",
                                           "14. ",
                                           // "15. Count olig frequencies in binding regions",
                                           "15. -> CountOligoFrequencies",
                                           //"16. Correlation between common score and IPS-score for best IPS-sites in single Chip-Seq track",
                                           "16. -> CommonAndIPSCorrelation",
                                           "17. ROC-curves: 5 siteModels; merged binding regions for single tfClass; union of sequences with best sites",
                                           // "18. IPS-prediction and tag density",
                                           "18. -> IPSPrediction",
                                           "19. ",
                                           //"20. ROC-curves: influence of common score threshold on IPSSiteModel (or logIPSSiteModel) in merged binding regions",
                                           "20. -> IPSROCCurve",
                                           "21. ",
                                           "22. ",
                                           // "23. Distinct TfClasses: Identification of their binding regions as cisModules2 of their repeated tracks",
                                           "23. -> DistinctTFClasses",
                                           "24. ROC-curves: 5 siteModels; number of overlaps of binding regions of the same tfClass; summit(yes/no)",
                                           "25. ROC-curves: 5 siteModels; sequences with identical sites in chip-seq peaks; summit(yes/no); matrix derivation",
                                           "26. ",
                                           // "27. Distribution of 5 MACS characteristics",
                                           "27. -> ChIPSeqCharacteristicsDistribution",
                                           "28. ",
                                           "29. Comparison of MACS and SISSRs (lengths and numbers of TF-binding regions",
                                           // "30. Matrix derivation: mixture of normal components; chip-seq peaks track; summit(yes/no); IPSSiteModel",
                                           "30. -> CreateMatrixByChIPSeqMixture",
                                           "31. Identification of cis-modules with given pattern (cis-modules3)",
                                           "32. Calculation of frequencies of tfClasses in all cisModules2 and cisModules2 that located near given genes",
                                           "33. Identification of cis-modules2 with given subset of tfClasses"};
    private String mode = MODES[0];
    private ExpParameters1 expParameters1 = new ExpParameters1();
    private ExpParameters2 expParameters2 = new ExpParameters2();
    private ExpParameters3 expParameters3 = new ExpParameters3();
    private ExpParameters4 expParameters4 = new ExpParameters4();
    private ExpParameters5 expParameters5 = new ExpParameters5();
    private ExpParameters6 expParameters6 = new ExpParameters6();
    private ExpParameters7 expParameters7 = new ExpParameters7();
    private ExpParameters8 expParameters8 = new ExpParameters8();
    private ExpParameters9 expParameters9 = new ExpParameters9();
    private ExpParameters10 expParameters10 = new ExpParameters10();
    private ExpParameters11 expParameters11 = new ExpParameters11();
    private ExpParameters12 expParameters12 = new ExpParameters12();
    private ExpParameters13 expParameters13 = new ExpParameters13();
    private ExpParameters14 expParameters14 = new ExpParameters14();
    private ExpParameters15 expParameters15 = new ExpParameters15();
    private ExpParameters16 expParameters16 = new ExpParameters16();
    private ExpParameters17 expParameters17 = new ExpParameters17();
    private ExpParameters18 expParameters18 = new ExpParameters18();
    private ExpParameters19 expParameters19 = new ExpParameters19();
    private ExpParameters20 expParameters20 = new ExpParameters20();
    private ExpParameters21 expParameters21 = new ExpParameters21();
    private ExpParameters22 expParameters22 = new ExpParameters22();
    private ExpParameters23 expParameters23 = new ExpParameters23();
    private ExpParameters24 expParameters24 = new ExpParameters24();
    private ExpParameters25 expParameters25 = new ExpParameters25();
    private ExpParameters27 expParameters27 = new ExpParameters27();
    private ExpParameters28 expParameters28 = new ExpParameters28();
    private ExpParameters29 expParameters29 = new ExpParameters29();
    private ExpParameters30 expParameters30 = new ExpParameters30();


    public String getMode()
    {
        return mode;
    }

    public int getModeIndex()
    {
        return IntStreamEx.ofIndices( MODES, mode::equals ).findAny().orElse( -1 );
    }

    public void setMode(String mode)
    {
        Object oldValue = this.mode;
        this.mode = mode;
        firePropertyChange("*", oldValue, mode);
    }

/////////////////////////

    public static class ExpParameters1 extends OptionEx
    {
        private DataElementPath pathToGenomeSequences;

        @PropertyName("Path to Genome Sequences")
        @PropertyDescription("Path to Genome Sequences")

        public DataElementPath getPathToGenomeSequences()
        {
            return pathToGenomeSequences;
        }
        public void setPathToGenomeSequences(DataElementPath pathToGenomeSequences)
        {
            Object oldValue = this.pathToGenomeSequences;
            this.pathToGenomeSequences = pathToGenomeSequences;
            firePropertyChange("pathToGenomeSequences", oldValue, pathToGenomeSequences);
        }
    }

    public static class ExpParameters1BeanInfo extends BeanInfoEx
    {
        public ExpParameters1BeanInfo()
        {
            super( ExpParameters1.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( DataElementPathEditor.registerInput( "pathToGenomeSequences", beanClass, SequenceCollection.class ) );
        }
    }

    public ExpParameters1 getExpParameters1()
    {
        return expParameters1;
    }

    public void setExpParameters1(ExpParameters1 expParameters1)
    {
        Object oldValue = this.expParameters1;
        this.expParameters1 = expParameters1;
        firePropertyChange("expParameters1", oldValue, expParameters1);
    }

    public boolean isExpParameters1Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 0:
            case 1:
            case 2:
            case 12:
            case 13:
            case 16:
            case 18:
            case 23:
            case 24:
            case 30: result = false;
        }
        return result;
    }

//////////////////////////////

    public static class ExpParameters2 extends OptionEx
    {
        private DataElementPath pathToOutputs;

        @PropertyName("Path to Outputs")
        @PropertyDescription("Path to Outputs")

        public DataElementPath getPathToOutputs()
        {
            return pathToOutputs;
        }
        public void setPathToOutputs(DataElementPath pathToOutputs)
        {
            Object oldValue = this.pathToOutputs;
            this.pathToOutputs = pathToOutputs;
            firePropertyChange("pathToOutputs", oldValue, pathToOutputs);
        }
    }

    public static class ExpParameters2BeanInfo extends BeanInfoEx
    {
        public ExpParameters2BeanInfo()
        {
            super( ExpParameters2.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerOutput("pathToOutputs", beanClass, FolderCollection.class));
        }
    }

    public ExpParameters2 getExpParameters2()
    {
        return expParameters2;
    }

    public void setExpParameters2(ExpParameters2 expParameters2)
    {
        Object oldValue = this.expParameters2;
        this.expParameters2 = expParameters2;
        firePropertyChange("expParameters2", oldValue, expParameters2);
    }

    public boolean isExpParameters2Hidden()
    {
        boolean result = false;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 6:
            case 30: result = true;
        }
        return result;
    }

//////////////////////////////

    public static class ExpParameters3 extends OptionEx
    {
        private DataElementPath pathToInputTracks;

        @PropertyName("Path to input tracks")
        @PropertyDescription("Path to input tracks")

        public DataElementPath getPathToInputTracks()
        {
            return pathToInputTracks;
        }
        public void setPathToInputTracks(DataElementPath pathToInputTracks)
        {
            Object oldValue = this.pathToInputTracks;
            this.pathToInputTracks = pathToInputTracks;
            firePropertyChange("pathToInputTracks", oldValue, pathToInputTracks);
        }
    }

    public static class ExpParameters3BeanInfo extends BeanInfoEx
    {
        public ExpParameters3BeanInfo()
        {
            super( ExpParameters3.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInputChild("pathToInputTracks", beanClass, SqlTrack.class));
        }
    }

    public ExpParameters3 getExpParameters3()
    {
        return expParameters3;
    }

    public void setExpParameters3(ExpParameters3 expParameters3)
    {
        Object oldValue = this.expParameters3;
        this.expParameters3 = expParameters3;
        firePropertyChange("expParameters3", oldValue, expParameters3);
    }

    public boolean isExpParameters3Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 1:
            case 2:
            case 7:
            case 8:
            case 12:
            case 18:
            case 23:
            case 24:
            case 29: result = false;
        }
        return result;
    }

//////////////////////////////

    public static class ExpParameters4 extends OptionEx
    {
        private Species specie = Species.getDefaultSpecies(null);

        @PropertyName("Specie")
        @PropertyDescription("Specie")

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
    }

    public static class ExpParameters4BeanInfo extends BeanInfoEx
    {
        public ExpParameters4BeanInfo()
        {
            super( ExpParameters4.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementComboBoxSelector.registerSelector("specie", beanClass, Species.SPECIES_PATH));
        }
    }

    public ExpParameters4 getExpParameters4()
    {
        return expParameters4;
    }

    public void setExpParameters4(ExpParameters4 expParameters4)
    {
        Object oldValue = this.expParameters4;
        this.expParameters4 = expParameters4;
        firePropertyChange("expParameters4", oldValue, expParameters4);
    }

    public boolean isExpParameters4Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 1:
            case 2:
            case 12:
            case 18:
            case 23:
            case 24:
            case 29: result = false;
        }
        return result;
    }

/////////////////

    public static class ExpParameters5 extends OptionEx
    {
        private DataElementPath pathToAuxiliaryTables;

        @PropertyName("Path to Auxiliary Tables")
        @PropertyDescription("Path to Auxiliary Tables")

        public DataElementPath getPathToAuxiliaryTables()
        {
            return pathToAuxiliaryTables;
        }
        public void setPathToAuxiliaryTables(DataElementPath pathToAuxiliaryTables)
        {
            Object oldValue = this.pathToAuxiliaryTables;
            this.pathToAuxiliaryTables = pathToAuxiliaryTables;
            firePropertyChange("pathToAuxiliaryTables", oldValue, pathToAuxiliaryTables);
        }
    }

    public static class ExpParameters5BeanInfo extends BeanInfoEx
    {
        public ExpParameters5BeanInfo()
        {
            super( ExpParameters5.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInput("pathToAuxiliaryTables", beanClass, SequenceCollection.class));
        }
    }

    public ExpParameters5 getExpParameters5()
    {
        return expParameters5;
    }

    public void setExpParameters5(ExpParameters5 expParameters5)
    {
        Object oldValue = this.expParameters5;
        this.expParameters5 = expParameters5;
        firePropertyChange("expParameters5", oldValue, expParameters5);
    }

    public boolean isExpParameters5Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 1:
            case 2:
            case 3:
            case 12:
            case 13:
            case 32:
            case 33: result = false;
        }
        return result;
    }

/////////////////////

    public static class ExpParameters6 extends OptionEx
    {
        private DataElementPath pathToSingleTrack;

        @PropertyName("Path to Single Input Track")
        @PropertyDescription("Path to Single Input Track")

        public DataElementPath getPathToSingleTrack()
        {
            return pathToSingleTrack;
        }
        public void setPathToSingleTrack(DataElementPath pathToSingleTrack)
        {
            Object oldValue = this.pathToSingleTrack;
            this.pathToSingleTrack = pathToSingleTrack;
            firePropertyChange("pathToSingleTrack", oldValue, pathToSingleTrack);
        }
    }

    public static class ExpParameters6BeanInfo extends BeanInfoEx
    {
        public ExpParameters6BeanInfo()
        {
            super( ExpParameters6.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
//          add(DataElementPathEditor.registerInput("pathToSingleTrack", beanClass, SqlTrack.class));
            add(DataElementPathEditor.registerOutput("pathToSingleTrack", beanClass, SqlTrack.class));
        }
    }

    public ExpParameters6 getExpParameters6()
    {
        return expParameters6;
    }

    public void setExpParameters6(ExpParameters6 expParameters6)
    {
        Object oldValue = this.expParameters6;
        this.expParameters6 = expParameters6;
        firePropertyChange("expParameters6", oldValue, expParameters6);
    }

    public boolean isExpParameters6Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 3:
            case 4:
            case 5:
            case 6:
            case 11:
            case 15:
            case 16:
            case 17:
            case 20:
            case 27:
            case 32:
            case 33: result = false;
        }
        return result;
    }

/////////////////////

    public static class ExpParameters7 extends OptionEx
    {
        private String tfClass;

        @PropertyName("tfClass")
        @PropertyDescription("Class of Transcription Factor")

        public String getTfClass()
        {
            return tfClass;
        }
        public void setTfClass(String tfClass)
        {
            Object oldValue = this.tfClass;
            this.tfClass = tfClass;
            firePropertyChange("tfClass", oldValue, tfClass);
        }
    }

    public static class ExpParameters7BeanInfo extends BeanInfoEx
    {
        public ExpParameters7BeanInfo()
        {
            super( ExpParameters7.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "tfClass" );
        }
    }

    public ExpParameters7 getExpParameters7()
    {
        return expParameters7;
    }

    public void setExpParameters7(ExpParameters7 expParameters7)
    {
        Object oldValue = this.expParameters7;
        this.expParameters7 = expParameters7;
        firePropertyChange("expParameters7", oldValue, expParameters7);
    }

    public boolean isExpParameters7Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 4:
            case 5:
            case 6:
            case 11:
            case 17:
            case 18:
            case 20:
            case 24: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters8 extends OptionEx
    {
        private DataElementPath pathToSiteModel;

        @PropertyName("Path to SiteModel")
        @PropertyDescription("Path to SiteModel")

        public DataElementPath getPathToSiteModel()
        {
            return pathToSiteModel;
        }
        public void setPathToSiteModel(DataElementPath pathToSiteModel)
        {
            Object oldValue = this.pathToSiteModel;
            this.pathToSiteModel = pathToSiteModel;
            firePropertyChange("pathToSiteModel", oldValue, pathToSiteModel);
        }
    }

    public static class ExpParameters8BeanInfo extends BeanInfoEx
    {
        public ExpParameters8BeanInfo()
        {
            super( ExpParameters8.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInput("pathToSiteModel", beanClass, SiteModel.class));
        }
    }

    public ExpParameters8 getExpParameters8()
    {
        return expParameters8;
    }

    public void setExpParameters8(ExpParameters8 expParameters8)
    {
        Object oldValue = this.expParameters8;
        this.expParameters8 = expParameters8;
        firePropertyChange("expParameters8", oldValue, expParameters8);
    }

    public boolean isExpParameters8Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 13: result = false;
        }
        return result;
    }

//////////////////////////////

    public static class ExpParameters9 extends OptionEx
    {
        private DataElementPath pathToInputs;

        @PropertyName("Path to Inputs")
        @PropertyDescription("Path to Inputs")

        public DataElementPath getPathToInputs()
        {
            return pathToInputs;
        }
        public void setPathToInputs(DataElementPath pathToInputs)
        {
            Object oldValue = this.pathToInputs;
            this.pathToInputs = pathToInputs;
            firePropertyChange("pathToInputs", oldValue, pathToInputs);
        }
    }

    public static class ExpParameters9BeanInfo extends BeanInfoEx
    {
        public ExpParameters9BeanInfo()
        {
            super( ExpParameters9.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerOutput("pathToInputs", beanClass, FolderCollection.class));
        }
    }

    public ExpParameters9 getExpParameters9()
    {
        return expParameters9;
    }

    public void setExpParameters9(ExpParameters9 expParameters9)
    {
        Object oldValue = this.expParameters9;
        this.expParameters9 = expParameters9;
        firePropertyChange("expParameters9", oldValue, expParameters9);
    }

    public boolean isExpParameters9Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 12: result = false;
        }
        return result;
    }

//////////////////////////////

    public static class ExpParameters10 extends OptionEx
    {
        private DataElementPath pathToMatrices;

        @PropertyName("Path to Matrices")
        @PropertyDescription("Path to Matrices")

        public DataElementPath getPathToMatrices()
        {
            return pathToMatrices;
        }
        public void setPathToMatrices(DataElementPath pathToMatrices)
        {
            Object oldValue = this.pathToMatrices;
            this.pathToMatrices = pathToMatrices;
            firePropertyChange("pathToMatrices", oldValue, pathToMatrices);
        }
    }

    public static class ExpParameters10BeanInfo extends BeanInfoEx
    {
        public ExpParameters10BeanInfo()
        {
            super( ExpParameters10.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
//            add(DataElementPathEditor.registerInput("pathToMatrices", beanClass, GenericDataCollection.class));
            add(DataElementPathEditor.registerInput("pathToMatrices", beanClass, WeightMatrixCollection.class));
        }
    }

    public ExpParameters10 getExpParameters10()
    {
        return expParameters10;
    }

    public void setExpParameters10(ExpParameters10 expParameters10)
    {
        Object oldValue = this.expParameters10;
        this.expParameters10 = expParameters10;
        firePropertyChange("expParameters10", oldValue, expParameters10);
    }

    public boolean isExpParameters10Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 13: result = false;
        }
        return result;
    }

//////////////////////////////

    public static class ExpParameters11 extends OptionEx
    {
        private DataElementPath pathToMatrix;

        @PropertyName("Path to Matrix")
        @PropertyDescription("Path to Matrix")

        public DataElementPath getPathToMatrix()
        {
            return pathToMatrix;
        }
        public void setPathToMatrix(DataElementPath pathToMatrix)
        {
            Object oldValue = this.pathToMatrix;
            this.pathToMatrix = pathToMatrix;
            firePropertyChange("pathToMatrix", oldValue, pathToMatrix);
        }
    }

    public static class ExpParameters11BeanInfo extends BeanInfoEx
    {
        public ExpParameters11BeanInfo()
        {
            super( ExpParameters11.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInput("pathToMatrix", beanClass, FrequencyMatrix.class));
        }
    }

    public ExpParameters11 getExpParameters11()
    {
        return expParameters11;
    }

    public void setExpParameters11(ExpParameters11 expParameters11)
    {
        Object oldValue = this.expParameters11;
        this.expParameters11 = expParameters11;
        firePropertyChange("expParameters11", oldValue, expParameters11);
    }

    public boolean isExpParameters11Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 4:
            case 5: result = false;
                    break;
            case 6: boolean b = getExpParameters20().getIsInitialMatrixGivenByConsensus();
                    if( b == false )
                        result = false;
                    break;
            case 11:
            case 16:
            case 17:
            case 18:
            case 20:
            case 24: result = false;
        }
        return result;
    }

//////////////////////////////

    public static class ExpParameters12 extends OptionEx
    {
        private int minimalNumberOfOverlaps = 2;

        @PropertyName("Minimal Number of Overlaps")
        @PropertyDescription("Minimal Number of Overlaps")

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
    }

    public static class ExpParameters12BeanInfo extends BeanInfoEx
    {
        public ExpParameters12BeanInfo()
        {
            super( ExpParameters12.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "minimalNumberOfOverlaps" );
        }
    }

    public ExpParameters12 getExpParameters12()
    {
        return expParameters12;
    }

    public void setExpParameters12(ExpParameters12 expParameters12)
    {
        Object oldValue = this.expParameters12;
        this.expParameters12 = expParameters12;
        firePropertyChange("expParameters12", oldValue, expParameters12);
    }

    public boolean isExpParameters12Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 7:
            case 8:
            case 23:
            case 24:
            case 33: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters13 extends OptionEx
    {
        private int numberOfMixtureComponents;

        @PropertyName("Number of mixture components")
        @PropertyDescription("Number of mixture components")

        public int getNumberOfMixtureComponents()
        {
            return numberOfMixtureComponents;
        }
        public void setNumberOfMixtureComponents(int numberOfMixtureComponents)
        {
            Object oldValue = this.numberOfMixtureComponents;
            this.numberOfMixtureComponents = numberOfMixtureComponents;
            firePropertyChange("numberOfMixtureComponents", oldValue, numberOfMixtureComponents);
        }
    }

    public static class ExpParameters13BeanInfo extends BeanInfoEx
    {
        public ExpParameters13BeanInfo()
        {
            super( ExpParameters13.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "numberOfMixtureComponents" );
        }
    }

    public ExpParameters13 getExpParameters13()
    {
        return expParameters13;
    }

    public void setExpParameters13(ExpParameters13 expParameters13)
    {
        Object oldValue = this.expParameters13;
        this.expParameters13 = expParameters13;
        firePropertyChange("expParameters13", oldValue, expParameters13);
    }

    public boolean isExpParameters13Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 5:
            case 10: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters14 extends OptionEx
    {
        private double pValue;

        @PropertyName("p_value")
        @PropertyDescription("p_value")

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
    }

    public static class ExpParameters14BeanInfo extends BeanInfoEx
    {
        public ExpParameters14BeanInfo()
        {
            super( ExpParameters14.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "pValue" );
        }
    }

    public ExpParameters14 getExpParameters14()
    {
        return expParameters14;
    }

    public void setExpParameters14(ExpParameters14 expParameters14)
    {
        Object oldValue = this.expParameters14;
        this.expParameters14 = expParameters14;
        firePropertyChange("expParameters14", oldValue, expParameters14);
    }

    public boolean isExpParameters14Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 5:
            case 6:
            case 30: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters15 extends OptionEx
    {
        private DataElementPath pathToCollectionOfCreatingMatrices;

        @PropertyName("Path to Collection of creating matrices")
        @PropertyDescription("Path to Collection of creating matrices")

        public DataElementPath getPathToCollectionOfCreatingMatrices()
        {
            return pathToCollectionOfCreatingMatrices;
        }
        public void setPathToCollectionOfCreatingMatrices(DataElementPath pathToCollectionOfCreatingMatrices)
        {
            Object oldValue = this.pathToCollectionOfCreatingMatrices;
            this.pathToCollectionOfCreatingMatrices = pathToCollectionOfCreatingMatrices;
            firePropertyChange("pathToCollectionOfCreatingMatrices", oldValue, pathToCollectionOfCreatingMatrices);
        }
    }

    public static class ExpParameters15BeanInfo extends BeanInfoEx
    {
        public ExpParameters15BeanInfo()
        {
            super( ExpParameters15.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerOutput("pathToCollectionOfCreatingMatrices", beanClass, WeightMatrixCollection.class));
        }
    }

    public ExpParameters15 getExpParameters15()
    {
        return expParameters15;
    }

    public void setExpParameters15(ExpParameters15 expParameters15)
    {
        Object oldValue = this.expParameters15;
        this.expParameters15 = expParameters15;
        firePropertyChange("expParameters15", oldValue, expParameters15);
    }

    public boolean isExpParameters15Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 6:
            case 30: result = false;
        }
        return result;
    }

//////////////////////////////

    public static class ExpParameters16 extends OptionEx
    {
        private String baseNameOfCreatingMatrices;

        @PropertyName("Base Name of Creating Matrices")
        @PropertyDescription("Base Name of Creating Matrices")

        public String getBaseNameOfCreatingMatrices()
        {
            return baseNameOfCreatingMatrices;
        }
        public void setBaseNameOfCreatingMatrices(String baseNameOfCreatingMatrices)
        {
            Object oldValue = this.baseNameOfCreatingMatrices;
            this.baseNameOfCreatingMatrices = baseNameOfCreatingMatrices;
            firePropertyChange("baseNameOfCreatingMatrices", oldValue, baseNameOfCreatingMatrices);
        }
    }

    public static class ExpParameters16BeanInfo extends BeanInfoEx
    {
        public ExpParameters16BeanInfo()
        {
            super( ExpParameters16.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "baseNameOfCreatingMatrices" );
        }
    }

    public ExpParameters16 getExpParameters16()
    {
        return expParameters16;
    }

    public void setExpParameters16(ExpParameters16 expParameters16)
    {
        Object oldValue = this.expParameters16;
        this.expParameters16 = expParameters16;
        firePropertyChange("expParameters16", oldValue, expParameters16);
    }

    public boolean isExpParameters16Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 6:
            case 30: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters17 extends OptionEx
    {
        private int maximalNumberOfIterations;

        @PropertyName("Maximal Number of Iterations")
        @PropertyDescription("Maximal Number of Iterations")

        public int getMaximalNumberOfIterations()
        {
            return maximalNumberOfIterations;
        }
        public void setMaximalNumberOfIterations(int maximalNumberOfIterations)
        {
            Object oldValue = this.maximalNumberOfIterations;
            this.maximalNumberOfIterations = maximalNumberOfIterations;
            firePropertyChange("maximalNumberOfIterations", oldValue, maximalNumberOfIterations);
        }
    }

    public static class ExpParameters17BeanInfo extends BeanInfoEx
    {
        public ExpParameters17BeanInfo()
        {
            super( ExpParameters17.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "maximalNumberOfIterations" );
        }
    }

    public ExpParameters17 getExpParameters17()
    {
        return expParameters17;
    }

    public void setExpParameters17(ExpParameters17 expParameters17)
    {
        Object oldValue = this.expParameters17;
        this.expParameters17 = expParameters17;
        firePropertyChange("expParameters17", oldValue, expParameters17);
    }

    public boolean isExpParameters17Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 6:
            case 30: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters18 extends OptionEx
    {
        private int numberOfMatricesWillBeConstructed;

        @PropertyName("Number of Matrices Will Be Constructed")
        @PropertyDescription("Number of Matrices Will Be Constructed")

        public int getNumberOfMatricesWillBeConstructed()
        {
            return numberOfMatricesWillBeConstructed;
        }
        public void setNumberOfMatricesWillBeConstructed(int numberOfMatricesWillBeConstructed)
        {
            Object oldValue = this.numberOfMatricesWillBeConstructed;
            this.numberOfMatricesWillBeConstructed = numberOfMatricesWillBeConstructed;
            firePropertyChange("numberOfMatricesWillBeConstructed", oldValue, numberOfMatricesWillBeConstructed);
        }
    }

    public static class ExpParameters18BeanInfo extends BeanInfoEx
    {
        public ExpParameters18BeanInfo()
        {
            super( ExpParameters18.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "numberOfMatricesWillBeConstructed" );
        }
    }

    public ExpParameters18 getExpParameters18()
    {
        return expParameters18;
    }

    public void setExpParameters18(ExpParameters18 expParameters18)
    {
        Object oldValue = this.expParameters18;
        this.expParameters18 = expParameters18;
        firePropertyChange("expParameters18", oldValue, expParameters18);
    }

    public boolean isExpParameters18Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 6:
            case 30: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters19 extends OptionEx
    {
        private String consensus;

        @PropertyName("consensus")
        @PropertyDescription("consensus")

        public String getConsensus()
        {
            return consensus;
        }
        public void setConsensus(String consensus)
        {
            Object oldValue = this.consensus;
            this.consensus = consensus;
            firePropertyChange("consensus", oldValue, consensus);
        }
    }

    public static class ExpParameters19BeanInfo extends BeanInfoEx
    {
        public ExpParameters19BeanInfo()
        {
            super( ExpParameters19.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "consensus" );
        }
    }

    public ExpParameters19 getExpParameters19()
    {
        return expParameters19;
    }

    public void setExpParameters19(ExpParameters19 expParameters19)
    {
        Object oldValue = this.expParameters19;
        this.expParameters19 = expParameters19;
        firePropertyChange("expParameters19", oldValue, expParameters19);
    }

    public boolean isExpParameters19Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 6: boolean b = getExpParameters20().getIsInitialMatrixGivenByConsensus();
                    if( b == true )
                        result = false;
                    break;
            case 30: result = false;

        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters20 extends OptionEx
    {
        private boolean isInitialMatrixGivenByConsensus = true;

        @PropertyName("isInitialMatrixGivenByConsensus")
        @PropertyDescription("is initial matrix given by consensus? (true or false)")

        public boolean getIsInitialMatrixGivenByConsensus()
        {
            return isInitialMatrixGivenByConsensus;
        }
        public void setIsInitialMatrixGivenByConsensus(boolean isInitialMatrixGivenByConsensus)
        {
            Object oldValue = this.isInitialMatrixGivenByConsensus;
            this.isInitialMatrixGivenByConsensus = isInitialMatrixGivenByConsensus;
            firePropertyChange("isInitialMatrixGivenByConsensus", oldValue, isInitialMatrixGivenByConsensus);
        }
    }

    public static class ExpParameters20BeanInfo extends BeanInfoEx
    {
        public ExpParameters20BeanInfo()
        {
            super( ExpParameters20.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "isInitialMatrixGivenByConsensus" );
        }
    }

    public ExpParameters20 getExpParameters20()
    {
        return expParameters20;
    }

    public void setExpParameters20(ExpParameters20 expParameters20)
    {
        Object oldValue = this.expParameters20;
        this.expParameters20 = expParameters20;
        firePropertyChange("*", oldValue, expParameters20);
    }

    public boolean isExpParameters20Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 6: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters21 extends OptionEx
    {
        private int oligLength;

        @PropertyName("oligLength")
        @PropertyDescription("length of oligs")

        public int getOligLength()
        {
            return oligLength;
        }
        public void setOligLength(int oligLength)
        {
            Object oldValue = this.oligLength;
            this.oligLength = oligLength;
            firePropertyChange("oligLength", oldValue, oligLength);
        }
    }

    public static class ExpParameters21BeanInfo extends BeanInfoEx
    {
        public ExpParameters21BeanInfo()
        {
            super( ExpParameters21.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "oligLength" );
        }
    }

    public ExpParameters21 getExpParameters21()
    {
        return expParameters21;
    }

    public void setExpParameters21(ExpParameters21 expParameters21)
    {
        Object oldValue = this.expParameters21;
        this.expParameters21 = expParameters21;
        firePropertyChange("expParameters21", oldValue, expParameters21);
    }

    public boolean isExpParameters21Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 15: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters22 extends OptionEx
    {
        private double ipsThreshold;

        @PropertyName("IPS threshold")
        @PropertyDescription("IPS threshold")

        public double getIpsThreshold()
        {
            return ipsThreshold;
        }
        public void setIpsThreshold(double ipsThreshold)
        {
            Object oldValue = this.ipsThreshold;
            this.ipsThreshold = ipsThreshold;
            firePropertyChange("ipsThreshold", oldValue, ipsThreshold);
        }
    }

    public static class ExpParameters22BeanInfo extends BeanInfoEx
    {
        public ExpParameters22BeanInfo()
        {
            super( ExpParameters22.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "ipsThreshold" );
        }
    }

    public ExpParameters22 getExpParameters22()
    {
        return expParameters22;
    }

    public void setExpParameters22(ExpParameters22 expParameters22)
    {
        Object oldValue = this.expParameters22;
        this.expParameters22 = expParameters22;
        firePropertyChange("expParameters22", oldValue, expParameters22);
    }

    public boolean isExpParameters22Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 11: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters23 extends OptionEx
    {
        private int percentageOfBestSites;

        @PropertyName("Percentage of Best Sites")
        @PropertyDescription("Percentage of Best Sites")

        public int getPercentageOfBestSites()
        {
            return percentageOfBestSites;
        }
        public void setPercentageOfBestSites(int percentageOfBestSites)
        {
            Object oldValue = this.percentageOfBestSites;
            this.percentageOfBestSites = percentageOfBestSites;
            firePropertyChange("percentageOfBestSites", oldValue, percentageOfBestSites);
        }
    }

    public static class ExpParameters23BeanInfo extends BeanInfoEx
    {
        public ExpParameters23BeanInfo()
        {
            super( ExpParameters23.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "percentageOfBestSites" );
        }
    }

    public ExpParameters23 getExpParameters23()
    {
        return expParameters23;
    }

    public void setExpParameters23(ExpParameters23 expParameters23)
    {
        Object oldValue = this.expParameters23;
        this.expParameters23 = expParameters23;
        firePropertyChange("expParameters23", oldValue, expParameters23);
    }

    public boolean isExpParameters23Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 17: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters24 extends OptionEx
    {
        private boolean toRemoveAlu;

        @PropertyName("toRemoveAlu")
        @PropertyDescription("Do you want to remove Alu-repeats? (true or false)")

        public boolean getToRemoveAlu()
        {
            return toRemoveAlu;
        }
        public void setToRemoveAlu(boolean toRemoveAlu)
        {
            Object oldValue = this.toRemoveAlu;
            this.toRemoveAlu = toRemoveAlu;
            firePropertyChange("toRemoveAlu", oldValue, toRemoveAlu);
        }
    }

    public static class ExpParameters24BeanInfo extends BeanInfoEx
    {
        public ExpParameters24BeanInfo()
        {
            super( ExpParameters24.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "toRemoveAlu" );
        }
    }

    public ExpParameters24 getExpParameters24()
    {
        return expParameters24;
    }

    public void setExpParameters24(ExpParameters24 expParameters24)
    {
        Object oldValue = this.expParameters24;
        this.expParameters24 = expParameters24;
        firePropertyChange("*", oldValue, expParameters24);
    }

    public boolean isExpParameters24Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 17: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters25 extends OptionEx
    {
        private boolean isLogTransformation;

        @PropertyName("isLogTransformation")
        @PropertyDescription("Do you want to consider log-models? (true or false)")

        public boolean getIsLogTransformation()
        {
            return isLogTransformation;
        }
        public void setIsLogTransformation(boolean isLogTransformation)
        {
            Object oldValue = this.isLogTransformation;
            this.isLogTransformation = isLogTransformation;
            firePropertyChange("isLogTransformation", oldValue, isLogTransformation);
        }
    }

    public static class ExpParameters25BeanInfo extends BeanInfoEx
    {
        public ExpParameters25BeanInfo()
        {
            super( ExpParameters25.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "isLogTransformation" );
        }
    }

    public ExpParameters25 getExpParameters25()
    {
        return expParameters25;
    }

    public void setExpParameters25(ExpParameters25 expParameters25)
    {
        Object oldValue = this.expParameters25;
        this.expParameters25 = expParameters25;
        firePropertyChange("*", oldValue, expParameters25);
    }

    public boolean isExpParameters25Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 20: result = false;
        }
        return result;
    }

//////////////////////////////

    public static class ExpParameters27 extends OptionEx
    {
        private boolean isAroundSummit;

        @PropertyName("isAroundSummit")
        @PropertyDescription("is around summit? (true or false)")

        public boolean getIsAroundSummit()
        {
            return isAroundSummit;
        }
        public void setIsAroundSummit(boolean isAroundSummit)
        {
            Object oldValue = this.isAroundSummit;
            this.isAroundSummit = isAroundSummit;
            firePropertyChange("isAroundSummit", oldValue, isAroundSummit);
        }
    }

    public static class ExpParameters27BeanInfo extends BeanInfoEx
    {
        public ExpParameters27BeanInfo()
        {
            super( ExpParameters27.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "isAroundSummit" );
        }
    }

    public ExpParameters27 getExpParameters27()
    {
        return expParameters27;
    }

    public void setExpParameters27(ExpParameters27 expParameters27)
    {
        Object oldValue = this.expParameters27;
        this.expParameters27 = expParameters27;
        firePropertyChange("*", oldValue, expParameters27);
    }

    public boolean isExpParameters27Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 24:
            case 30: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters28 extends OptionEx
    {
        private DataElementPath pathToChipSeqTrack;

        @PropertyName("Path to ChIP-Seq Track")
        @PropertyDescription("Path to ChIP-Seq Track")

        public DataElementPath getPathToChipSeqTrack()
        {
            return pathToChipSeqTrack;
        }
        public void setPathToChipSeqTrack(DataElementPath pathToChipSeqTrack)
        {
            Object oldValue = this.pathToChipSeqTrack;
            this.pathToChipSeqTrack = pathToChipSeqTrack;
            firePropertyChange("pathToChipSeqTrack", oldValue, pathToChipSeqTrack);
        }
    }

    public static class ExpParameters28BeanInfo extends BeanInfoEx
    {
        public ExpParameters28BeanInfo()
        {
            super( ExpParameters28.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInput("pathToChipSeqTrack", beanClass, SqlTrack.class));
        }
    }

    public ExpParameters28 getExpParameters28()
    {
        return expParameters28;
    }

    public void setExpParameters28(ExpParameters28 expParameters28)
    {
        Object oldValue = this.expParameters28;
        this.expParameters28 = expParameters28;
        firePropertyChange("expParameters28", oldValue, expParameters28);
    }

    public boolean isExpParameters28Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 30: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters29 extends OptionEx
    {
        private int minimalLengthOfSequenceRegion = 300;

        @PropertyName("minimal length of sequence region")
        @PropertyDescription("minimal length of sequence region")

        public int getMinimalLengthOfSequenceRegion()
        {
            return minimalLengthOfSequenceRegion;
        }
        public void setMinimalLengthOfSequenceRegion(int minimalLengthOfSequenceRegion)
        {
            Object oldValue = this.minimalLengthOfSequenceRegion;
            this.minimalLengthOfSequenceRegion = minimalLengthOfSequenceRegion;
            firePropertyChange("minimalLengthOfSequenceRegion", oldValue, minimalLengthOfSequenceRegion);
        }
    }

    public static class ExpParameters29BeanInfo extends BeanInfoEx
    {
        public ExpParameters29BeanInfo()
        {
            super( ExpParameters29.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "minimalLengthOfSequenceRegion" );
        }
    }

    public ExpParameters29 getExpParameters29()
    {
        return expParameters29;
    }

    public void setExpParameters29(ExpParameters29 expParameters29)
    {
        Object oldValue = this.expParameters29;
        this.expParameters29 = expParameters29;
        firePropertyChange("expParameters29", oldValue, expParameters29);
    }

    public boolean isExpParameters29Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 16:
            case 24:
            case 30: result = false;
        }
        return result;
    }

/////////////////////////////////////////////////

    public static class ExpParameters30 extends OptionEx
    {
        private DataElementPath pathToInputTracks2;

        @PropertyName("Path to input tracks2")
        @PropertyDescription("Path to input tracks2")

        public DataElementPath getPathToInputTracks2()
        {
            return pathToInputTracks2;
        }
        public void setPathToInputTracks2(DataElementPath pathToInputTracks2)
        {
            Object oldValue = this.pathToInputTracks2;
            this.pathToInputTracks2 = pathToInputTracks2;
            firePropertyChange("pathToInputTracks2", oldValue, pathToInputTracks2);
        }
    }

    public static class ExpParameters30BeanInfo extends BeanInfoEx
    {
        public ExpParameters30BeanInfo()
        {
            super( ExpParameters30.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInputChild("pathToInputTracks2", beanClass, SqlTrack.class));
        }
    }

    public ExpParameters30 getExpParameters30()
    {
        return expParameters30;
    }

    public void setExpParameters30(ExpParameters30 expParameters30)
    {
        Object oldValue = this.expParameters30;
        this.expParameters30 = expParameters30;
        firePropertyChange("expParameters30", oldValue, expParameters30);
    }

    public boolean isExpParameters30Hidden()
    {
        boolean result = true;
        int index = getModeIndex();
        switch(index)
        {
            default: break;
            case 1:
            case 2:
            case 7:
            case 8:
            case 12:
            case 18:
            case 23:
            case 24:
            case 29: result = false;
        }
        return result;
    }

//////////////////////////////


    public static class ModesEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return MODES;
        }
    }
}