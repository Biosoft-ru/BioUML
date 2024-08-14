package biouml.plugins.gtrd.analysis;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.MNaseExperiment;
import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.ChIPseqExperimentSQLTransformer;
import biouml.plugins.gtrd.analysis.OpenPerTFView.SpeciesSelector;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class OpenTrackForAllTF extends AnalysisMethodSupport<OpenTrackForAllTF.Parameters>
{
    private static final String TYPE_UNMAPPABLE_REGIONS = "Unmappable regions";
    private static final String TYPE_HOCOMOCO_WEAK_SITES = "HOCOMOCO (strong & weak sites)";
    private static final String TYPE_HOCOMOCO_STRONG_SITES = "HOCOMOCO (strong sites)";
    private static final String TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2 = "DNase footprints (DNase-seq; wellington_macs2)";
    private static final String TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2 = "DNase footprints (DNase-seq; wellington_hotspot2)";
    private static final String TYPE_OPEN_CHROMATIN_DNASE_MACS2 = "Open chromatin (DNase-seq; macs2)";
    private static final String TYPE_OPEN_CHROMATIN_DNASE_HOTSPOT2 = "Open chromatin (DNase-seq; hotspot2)";
    private static final String TYPE_OPEN_CHROMATIN_ATAC_MACS2 = "Open chromatin (ATAC-seq; macs2)";
    private static final String TYPE_OPEN_CHROMATIN_FAIRE_MACS2 = "Open chromatin (FAIRE-seq; macs2)";
    private static final String TYPE_HISTONE_MODIFICATIONS = "Histone Modifications (macs2)";
    private static final String TYPE_CHIPEXO_PEAKS_GEM = "ChIP-exo peaks (gem)";
    private static final String TYPE_CHIPEXO_PEAKS_PEAKZILLA = "ChIP-exo peaks (peakzilla)";
    private static final String TYPE_MNASE_NUCLEOSOMES = "MNase-seq nucleosomes (danpos2)";
    private static final String TYPE_OPEN_CHROMATIN_METACLUSTERS = "Open chromatin (meta clusters)";
    private static final String TYPE_ALLELE_SPECIFIC_BINDING_BY_TF = "Allele specific binding by TF";
    private static final String TYPE_ALLELE_SPECIFIC_BINDING_BY_CELL = "Allele specific binding by Cell Type";
    // TODO: add clusters by cell_line 
    private static String[] DATA_TYPES =  
    {
        "meta clusters",
        "sissrs clusters",
        "macs2 clusters",
        "gem clusters",
        "pics clusters",
        "sissrs peaks",
        "macs2 peaks",
        "gem peaks",
        "pics peaks",
        TYPE_HISTONE_MODIFICATIONS,
        TYPE_CHIPEXO_PEAKS_GEM,
        TYPE_CHIPEXO_PEAKS_PEAKZILLA,
        TYPE_OPEN_CHROMATIN_DNASE_MACS2,
        TYPE_OPEN_CHROMATIN_DNASE_HOTSPOT2,
        TYPE_OPEN_CHROMATIN_ATAC_MACS2,
        TYPE_OPEN_CHROMATIN_FAIRE_MACS2,
        TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2,
        TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2,
        TYPE_MNASE_NUCLEOSOMES,
        TYPE_HOCOMOCO_STRONG_SITES,
        TYPE_HOCOMOCO_WEAK_SITES,
        TYPE_UNMAPPABLE_REGIONS,
        TYPE_ALLELE_SPECIFIC_BINDING_BY_TF,
        TYPE_ALLELE_SPECIFIC_BINDING_BY_CELL,
        TYPE_OPEN_CHROMATIN_METACLUSTERS
    };

    public OpenTrackForAllTF(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    protected void writeProperties(DataElement de) throws Exception
    {
        //Don't write properties to results since we just return existing element
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        String dataType = parameters.getDataType();
        
        if(dataType.equals( TYPE_HISTONE_MODIFICATIONS ))
        {
            List<DataElement> results = new ArrayList<>();
            DataElementPath expDC = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/ChIP-seq HM experiments");
            for(String expId : parameters.getExperiments())
            {
            	HistonesExperiment exp = expDC.getChildPath( expId ).getDataElement( HistonesExperiment.class );
                DataElementPath peakPath = exp.getPeakByPeakCaller( "macs2" );
                DataElement de = peakPath.optDataElement( );
                if(de != null)
                	results.add( de );
            }
            return results.toArray();
        }
        
        if(dataType.equals( TYPE_CHIPEXO_PEAKS_GEM ))
        {
            List<DataElement> results = new ArrayList<>();
            DataElementPath expDC = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/ChIP-exo experiments");
            for(String expId : parameters.getExperiments())
            {
            	ChIPexoExperiment exp = expDC.getChildPath( expId ).getDataElement( ChIPexoExperiment.class );
            	ru.biosoft.access.core.DataElementPath peakPath = exp.getPeaksByPeakCaller( "gem" );
                DataElement de = peakPath.optDataElement( );
                if(de != null)
                	results.add( de );
            }
            return results.toArray();
        }
        
        if(dataType.equals( TYPE_CHIPEXO_PEAKS_PEAKZILLA ))
        {
            List<DataElement> results = new ArrayList<>();
            DataElementPath expDC = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/ChIP-exo experiments");
            for(String expId : parameters.getExperiments())
            {
            	ChIPexoExperiment exp = expDC.getChildPath( expId ).getDataElement( ChIPexoExperiment.class );
            	ru.biosoft.access.core.DataElementPath peakPath = exp.getPeaksByPeakCaller( "peakzilla" );
                DataElement de = peakPath.optDataElement( );
                if(de != null)
                	results.add( de );
            }
            return results.toArray();
        }
        
        if(dataType.equals( TYPE_MNASE_NUCLEOSOMES ))
        {
            List<DataElement> results = new ArrayList<>();
            DataElementPath expDC = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/MNase-seq experiments");
            for(String expId : parameters.getExperiments())
            {
            	MNaseExperiment exp = expDC.getChildPath( expId ).getDataElement( MNaseExperiment.class );
            	ru.biosoft.access.core.DataElementPath peakPath = exp.getPeaksByPeakCaller( "danpos2" );
                DataElement de = peakPath.optDataElement( );
                if(de != null)
                	results.add( de );
            }
            return results.toArray();
        }
        
        if(dataType.equals( TYPE_OPEN_CHROMATIN_DNASE_MACS2 ))
        {
            List<DataElement> results = new ArrayList<>();
            DataElementPath expDC = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/DNase experiments");
            for(String expId : parameters.getExperiments())
            {
                DNaseExperiment exp = expDC.getChildPath( expId ).getDataElement( DNaseExperiment.class );
                for(DataElementPath peakPath : exp.getMacsPeaks())
                {
                    DataElement de = peakPath.optDataElement( );
                    if(de != null)
                        results.add( de );
                }
            }
            return results.toArray();
        }
        if(dataType.equals( TYPE_OPEN_CHROMATIN_ATAC_MACS2 ))
        {
            List<DataElement> results = new ArrayList<>();
            DataElementPath expDC = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/ATAC-seq experiments");
            for(String expId : parameters.getExperiments())
            {
                DNaseExperiment exp = expDC.getChildPath( expId ).getDataElement( DNaseExperiment.class );
                for(DataElementPath peakPath : exp.getMacsPeaks())
                {
                    DataElement de = peakPath.optDataElement( );
                    if(de != null)
                        results.add( de );
                }
            }
            return results.toArray();
        }
        if(dataType.equals( TYPE_OPEN_CHROMATIN_FAIRE_MACS2 ))
        {
            List<DataElement> results = new ArrayList<>();
            DataElementPath expDC = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/FAIRE-seq experiments");
            for(String expId : parameters.getExperiments())
            {
                DNaseExperiment exp = expDC.getChildPath( expId ).getDataElement( DNaseExperiment.class );
                for(DataElementPath peakPath : exp.getMacsPeaks())
                {
                    DataElement de = peakPath.optDataElement( );
                    if(de != null)
                        results.add( de );
                }
            }
            return results.toArray();
        }
        else if(dataType.equals( TYPE_OPEN_CHROMATIN_DNASE_HOTSPOT2 ))
        {
            List<DataElement> results = new ArrayList<>();
            DataElementPath expDC = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/DNase experiments");
            for(String expId : parameters.getExperiments())
            {
                DNaseExperiment exp = expDC.getChildPath( expId ).getDataElement( DNaseExperiment.class );
                for(DataElementPath peakPath : exp.getHotspotPeaks())
                {
                    DataElement de = peakPath.optDataElement( );
                    if(de != null)
                        results.add( de );
                }
            }
            return results.toArray();
        }
        else if(dataType.equals( TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2 ))
        {
            List<DataElement> results = new ArrayList<>();
            DataElementPath expDC = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/DNase experiments");
            for(String expId : parameters.getExperiments())
            {
                DNaseExperiment exp = expDC.getChildPath( expId ).getDataElement( DNaseExperiment.class );
                for(DataElementPath peakPath : exp.getWellingtonPeaks( "macs2" ))
                {
                    DataElement de = peakPath.optDataElement( );
                    if(de != null)
                        results.add( de );
                }
            }
            return results.toArray();
        }
        else if(dataType.equals( TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2 ))
        {
            List<DataElement> results = new ArrayList<>();
            DataElementPath expDC = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/DNase experiments");
            for(String expId : parameters.getExperiments())
            {
                DNaseExperiment exp = expDC.getChildPath( expId ).getDataElement( DNaseExperiment.class );
                for(DataElementPath peakPath : exp.getWellingtonPeaks( "hotspot2" ))
                {
                    DataElement de = peakPath.optDataElement( );
                    if(de != null)
                        results.add( de );
                }
            }
            return results.toArray();
        }
        else if(dataType.equals( TYPE_HOCOMOCO_STRONG_SITES ))
        {
            String name = parameters.getOrganism().getCommonName().toLowerCase() + "_hocomoco_v11_pval=0.0001";
            DataElementPath path = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/generic/predicted sites/" + name );
            return path.optDataElement();
        }
        else if(dataType.equals( TYPE_HOCOMOCO_WEAK_SITES ))
        {
            String name = parameters.getOrganism().getCommonName().toLowerCase() + "_hocomoco_v11_pval=0.001";
            DataElementPath path = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/generic/predicted sites/" + name );
            return path.optDataElement();
        }
        else if(dataType.equals( TYPE_UNMAPPABLE_REGIONS ))
        {
            String name = parameters.getOrganism().getLatinName() + " unmappable 50";
            DataElementPath path = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/generic/mappability/" + name );
            return path.optDataElement();            
        }
        else if(dataType.equals( TYPE_OPEN_CHROMATIN_METACLUSTERS ))
        {
        	List<DataElement> result = new ArrayList<>();
            DataElementPath parentDir = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT 
            		+ "/clusters/DNase-seq clusters");
            for( String dnaseTrackName : parameters.getDnaseClustersTracks() )
            {
            	ru.biosoft.access.core.DataElementPath dePath = DataElementPath.create( parentDir + "/" + dnaseTrackName );
            	if( dePath.exists() )
            		result.add(dePath.optDataElement());
            }
            return result.toArray();       
        }
        else if(dataType.equals( TYPE_ALLELE_SPECIFIC_BINDING_BY_CELL ))
        {
            List<DataElement> result = new ArrayList<>();
            DataElementPath parentDir = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT 
            		+ "/generic/allele specific binding/cell_level");
            for( String asbTrackName : parameters.getAsbTracks() )
            {
            	ru.biosoft.access.core.DataElementPath dePath = DataElementPath.create( parentDir + "/" + asbTrackName );
            	if( dePath.exists() )
            		result.add(dePath.optDataElement());
            }
            return result.toArray();
        }
        else if(dataType.equals( TYPE_ALLELE_SPECIFIC_BINDING_BY_TF ))
        {
            List<DataElement> result = new ArrayList<>();
            DataElementPath parentDir = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT 
            		+ "/generic/allele specific binding/tf_level");
            for( String asbTrackName : parameters.getAsbTracks() )
            {
            	ru.biosoft.access.core.DataElementPath dePath = DataElementPath.create( parentDir + "/" + asbTrackName );
            	if( dePath.exists() )
            		result.add(dePath.optDataElement());
            }
            return result.toArray();
        }
        else
        {
            //ChIP-seq
            String[] parts = dataType.split( " " );
            String caller = parts[0];
            String type = parts[1];
            String species =  parameters.getOrganism().getLatinName();
            DataElementPath path;
            if(type.equals( "peaks" ))
            {
                path = DataElementPath.create( "databases/GTRD/Data/peaks/union" ).getChildPath( species + " " + caller );
            }
            else
            {
                if(!caller.equals( "meta" ))
                    caller = caller.toUpperCase();
                path = DataElementPath.create( "databases/GTRD/Data/clusters/" ).getChildPath( species,  "all " + caller + " clusters");
            }
            return path.getDataElement( Track.class );
        }            
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private Species organism = Species.getSpecies( "Homo sapiens" );
        @PropertyName ( "Organism" )
        public Species getOrganism()
        {
            return organism;
        }
        public void setOrganism(Species organism)
        {
            Species oldValue = this.organism;
            this.organism = organism;
            firePropertyChange( "organism", oldValue, organism );
            setDataType( DATA_TYPES[0] );
            if(isDNase() || isChIPexo() || isMNase() || isHistones() || isATAC() || isFAIRE())
            {
                setDefaultCell();
                resetExperiments();
            }
        }

        private String dataType = DATA_TYPES[0];
        @PropertyName ( "Data type" )
        public String getDataType()
        {
            return dataType;
        }
        public void setDataType(String dataType)
        {
            String oldValue = this.dataType;
            this.dataType = dataType;
            firePropertyChange( "dataType", oldValue, dataType );
            if(isDNase() || isChIPexo() || isMNase() || isHistones() || isATAC() || isFAIRE())
            {
                setDefaultCell();
                resetExperiments();
            }
        }
        
        private String cellLine;
        @PropertyName( "Cell/Tissue" )
        @PropertyDescription( "Cell or Tissue" )
        public String getCellLine()
        {
            return cellLine;
        }
        public void setCellLine(String cellLine)
        {
            Object oldValue = this.cellLine;
            this.cellLine = cellLine;
            firePropertyChange( "cellLine", oldValue, cellLine );
            setDefaultTreatment();
            resetExperiments();
        }
        public boolean isCellLineHidden()
        {
            return !isDNase() && !isChIPexo() && !isMNase() && !isHistones() && !isATAC() && !isFAIRE();
        }
        private void setDefaultCell()
        {
            CellLineSelector selector = new CellLineSelector();
            selector.setBean( this );
            String firstCell = selector.getAvailableValues()[0];
            setCellLine( firstCell );
        }
        
        private String treatment;
        
        @PropertyName("Conditions")
        @PropertyDescription("Conditions")
        public String getTreatment()
        {
            return treatment;
        }
        public void setTreatment(String treatment)
        {
            Object oldValue = this.treatment;
            this.treatment = treatment;
            firePropertyChange( "treatment", oldValue, treatment );
            resetExperiments();
        }
        public boolean isTreatmentHidden()
        {
            return !isDNase() && !isChIPexo() && !isMNase() && !isHistones() && !isATAC() && !isFAIRE();
        }
        public void setDefaultTreatment()
        {
            TreatmentSelector selector = new TreatmentSelector();
            selector.setBean( this );
            String firstTreatment = selector.getAvailableValues()[0];
            setTreatment( firstTreatment );
        }

        private String[] experiments = new String[0];
        @PropertyName("Experiments")
        @PropertyDescription("Experiments")
        public String[] getExperiments()
        {
            return experiments;
        }
        public void setExperiments(String[] experiments)
        {
            Object[] oldValue = this.experiments;
            this.experiments = experiments;
            firePropertyChange( "experiments", oldValue, experiments );
        }
        public boolean isExperimentsHidden()
        {
            return !isDNase() && !isChIPexo() && !isMNase() && !isHistones() && !isATAC() && !isFAIRE();
        }
        
        private String[] asbTracks = new String[0];
        @PropertyName("ASB Tracks")
        @PropertyDescription("ASB Tracks")
        public String[] getAsbTracks()
        {
            return asbTracks;
        }
        public void setAsbTracks(String[] asbTracks)
        {
            Object[] oldValue = this.asbTracks;
            this.asbTracks = asbTracks;
            firePropertyChange( "asbTracks", oldValue, asbTracks );
        }
        public boolean isASBTracksHidden()
        {
            return !isASB();
        }
        
        private String[] dnaseClustersTracks = new String[0];
        @PropertyName("Cell Type")
        @PropertyDescription("Cell Type")
        public String[] getDnaseClustersTracks()
        {
            return dnaseClustersTracks;
        }
        public void setDnaseClustersTracks(String[] dnaseClustersTracks)
        {
            Object[] oldValue = this.dnaseClustersTracks;
            this.dnaseClustersTracks = dnaseClustersTracks;
            firePropertyChange( "dnaseClustersTracks", oldValue, dnaseClustersTracks );
        }
        public boolean isDnaseClustersTracksHidden()
        {
            return !isDnaseClustersTracks();
        }
        
        private boolean isDNase()
        {
            return dataType.equals( TYPE_OPEN_CHROMATIN_DNASE_MACS2 ) 
            		|| dataType.equals( TYPE_OPEN_CHROMATIN_DNASE_HOTSPOT2 )
            		|| dataType.equals( TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2 )
            		|| dataType.equals( TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2 );
        }
        
        private boolean isChIPexo()
        {
            return dataType.equals( TYPE_CHIPEXO_PEAKS_GEM ) 
            		|| dataType.equals( TYPE_CHIPEXO_PEAKS_PEAKZILLA );
        }
        
        private boolean isATAC()
        {
            return dataType.equals( TYPE_OPEN_CHROMATIN_ATAC_MACS2 );
        }
        
        private boolean isFAIRE()
        {
            return dataType.equals( TYPE_OPEN_CHROMATIN_FAIRE_MACS2 );
        }
        
        private boolean isMNase()
        {
            return dataType.equals( TYPE_MNASE_NUCLEOSOMES );
        }
        
        private boolean isHistones()
        {
            return dataType.equals( TYPE_HISTONE_MODIFICATIONS );
        }
        
        private boolean isASB()
        {
        	return dataType.equals( TYPE_ALLELE_SPECIFIC_BINDING_BY_CELL )
        			|| dataType.equals( TYPE_ALLELE_SPECIFIC_BINDING_BY_TF);
        }
        
        private boolean isDnaseClustersTracks()
        {
        	return dataType.equals( TYPE_OPEN_CHROMATIN_METACLUSTERS );
        }
        
        private void resetExperiments()
        {
            ExperimentsEditor selector = new ExperimentsEditor();
            selector.setBean( this );
            String[] values = selector.getAvailableValues();
            if(values.length == 0)
                this.experiments = new String[0];
            else
                this.experiments = new String[]{values[0]};
        }

    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "organism" ).editor( SpeciesSelector.class ).hideChildren().add();
            property( "dataType" ).editor( DataTypeSelector.class ).add();
            addHidden( "cellLine", CellLineSelector.class, "isCellLineHidden" );
            property("treatment").editor( TreatmentSelector.class ).hidden( "isTreatmentHidden" ).add();
            addHidden( "experiments", ExperimentsEditor.class, "isExperimentsHidden" );
            addHidden( "asbTracks", ASBTracksEditor.class , "isASBTracksHidden" );
            addHidden( "dnaseClustersTracks", DNaseClustersTracksEditor.class , "isDnaseClustersTracksHidden" );
        }
    }
    
    public static class DataTypeSelector extends GenericComboBoxEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            ComponentModel model = ComponentFactory.getModel( getBean() );
            Species organism = (Species)model.findProperty( "organism" ).getValue();
            boolean dnaseAvailable = isDNaseAvailable( organism );
            boolean atacAvailable = isATACAvailable( organism );
            boolean faireAvailable = isFAIREAvailable( organism );
            boolean chipexoAvailable = isChIPexoAvailable( organism );
            boolean mnaseAvailable = isMNaseAvailable( organism );
            boolean histAvailable = isHistonesAvailable( organism );
            boolean hocomocoAvailable = isHOCOMOCOAvailable( organism );
            boolean alleleSpecificBindingAvailable = isASBAvailable( organism );
            boolean dnaseClustersAvailable = isDNaseClusters( organism );
            List<String> result = new ArrayList<>();
            for(String type : DATA_TYPES)
            {
                if( type.equals( TYPE_OPEN_CHROMATIN_DNASE_MACS2 ) 
                		|| type.equals( TYPE_OPEN_CHROMATIN_DNASE_HOTSPOT2 )
                		|| type.equals( TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2 )
                		|| type.equals( TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2 ) )
                {
                    if(dnaseAvailable)
                        result.add( type );        
                }
                if( type.equals( TYPE_OPEN_CHROMATIN_ATAC_MACS2 ) )
                {
                    if(atacAvailable)
                        result.add( type );        
                }
                if( type.equals( TYPE_OPEN_CHROMATIN_FAIRE_MACS2 ) )
                {
                    if(faireAvailable)
                        result.add( type );        
                }
                else if( type.equals( TYPE_CHIPEXO_PEAKS_GEM ) 
                		|| type.equals( TYPE_CHIPEXO_PEAKS_PEAKZILLA ) )
                {
                    if(chipexoAvailable)
                        result.add( type );        
                }
                else if( type.equals( TYPE_HISTONE_MODIFICATIONS ) )
                {
                    if(histAvailable)
                        result.add( type );        
                }
                else if( type.equals( TYPE_MNASE_NUCLEOSOMES ) )
                {
                    if(mnaseAvailable)
                        result.add( type );        
                }
                else if(type.equals( TYPE_HOCOMOCO_STRONG_SITES ) || type.equals( TYPE_HOCOMOCO_WEAK_SITES ))
                {
                    if(type.equals( TYPE_HOCOMOCO_WEAK_SITES ))
                        continue;//Currently not available
                    if(hocomocoAvailable)
                        result.add( type );
                }
                else if(type.equals( TYPE_ALLELE_SPECIFIC_BINDING_BY_CELL ) || type.equals( TYPE_ALLELE_SPECIFIC_BINDING_BY_TF ))
                {
                	if( alleleSpecificBindingAvailable )
                		result.add( type );
                }
                else if(type.equals( TYPE_OPEN_CHROMATIN_METACLUSTERS ))
                {
                	if( dnaseClustersAvailable )
                		result.add( type );
                }
                else
                    result.add( type );
            }
            return result.toArray( new String[0] );
        }
        
        private boolean isDNaseAvailable(Species species)
        {
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            return SqlUtil.hasResult( con, "SELECT id FROM dnase_experiments WHERE organism=" + SqlUtil.quoteString( species.getLatinName() ) );
        }
        
        private boolean isATACAvailable(Species species)
        {
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            return SqlUtil.hasResult( con, "SELECT id FROM atac_experiments WHERE organism=" + SqlUtil.quoteString( species.getLatinName() ) );
        }
        
        private boolean isFAIREAvailable(Species species)
        {
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            return SqlUtil.hasResult( con, "SELECT id FROM faire_experiments WHERE organism=" + SqlUtil.quoteString( species.getLatinName() ) );
        }
        
        private boolean isMNaseAvailable(Species species)
        {
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            return SqlUtil.hasResult( con, "SELECT id FROM mnase_experiments WHERE organism=" + SqlUtil.quoteString( species.getLatinName() ) );
        }
        
        private boolean isChIPexoAvailable(Species species)
        {
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            return SqlUtil.hasResult( con, "SELECT id FROM chipexo_experiments WHERE specie=" + SqlUtil.quoteString( species.getLatinName() ) );
        }
        
        private boolean isHistonesAvailable(Species species)
        {
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            return SqlUtil.hasResult( con, "SELECT id FROM hist_experiments WHERE specie=" + SqlUtil.quoteString( species.getLatinName() ) );
        }
        
        private boolean isHOCOMOCOAvailable(Species organism)
        {
            return organism.getLatinName().equals( "Homo sapiens" ) || organism.getLatinName().equals( "Mus musculus" ); 
        }
        
        private boolean isASBAvailable(Species organism)
        {
        	return organism.getLatinName().equals( "Homo sapiens" );
        }
        private boolean isDNaseClusters(Species organism)
        {
        	return organism.getLatinName().equals( "Homo sapiens" );
        }
    }
    
    private static String getPeakTypeByDataType(String dataType)
    {
        if(dataType.equals( TYPE_OPEN_CHROMATIN_DNASE_MACS2 ))
            return "macs2";
        else if(dataType.equals( TYPE_OPEN_CHROMATIN_DNASE_HOTSPOT2 ))
            return "hotspot2";
        else if(dataType.equals( TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2 ))
            return "wellington_macs2";
        else if(dataType.equals( TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2 ))
            return "wellington_hotspot2";
        else
            return null;        
    }
    
    private static String getExpTableByDataType(String dataType)
    {
    	if( dataType.equals( TYPE_OPEN_CHROMATIN_DNASE_MACS2 ) 
        		|| dataType.equals( TYPE_OPEN_CHROMATIN_DNASE_HOTSPOT2 )
        		|| dataType.equals( TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2 )
        		|| dataType.equals( TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2 ) )
    		return "dnase_experiments";
    	return null;
    }
    
    private static class DataTypeInfo
    {
    	private String peaksFinishedTable, expTable, peakType;
    	
    	public DataTypeInfo( String dataType )
    	{
    		expTable = "dnase_experiments";
    		if(dataType.equals( TYPE_OPEN_CHROMATIN_DNASE_MACS2 ))
    		{
    			expTable = "dnase_experiments";
    			peaksFinishedTable = "dnase_peaks_finished";
    			peakType = "macs2";
    		}
    		else if(dataType.equals( TYPE_OPEN_CHROMATIN_DNASE_HOTSPOT2 ))
    		{
    			expTable = "dnase_experiments";
    			peaksFinishedTable = "dnase_peaks_finished";
    			peakType = "hotspot2";
    		}
    		else if(dataType.equals( TYPE_OPEN_CHROMATIN_ATAC_MACS2 ))
    		{
    			expTable = "atac_experiments";
    			peaksFinishedTable = "atac_peaks_finished";
    			peakType = "macs2";
    		}
    		else if(dataType.equals( TYPE_OPEN_CHROMATIN_FAIRE_MACS2 ))
    		{
    			expTable = "faire_experiments";
    			peaksFinishedTable = "atac_peaks_finished";
    			peakType = "macs2";
    		}
    		else if(dataType.equals( TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2 ))
    		{
    			expTable = "dnase_experiments";
    			peaksFinishedTable = "dnase_peaks_finished";
    			peakType = "wellington_macs2";
    		}
    		else if(dataType.equals( TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2 ))
    		{
    			expTable = "dnase_experiments";
    			peaksFinishedTable = "dnase_peaks_finished";
    			peakType = "wellington_hotspot2";
    		}
    		else if( dataType.equals( TYPE_CHIPEXO_PEAKS_GEM ) )
    		{
    			expTable = "chipexo_experiments";
    			peaksFinishedTable = "chipexo_peaks_finished";
    			peakType = "gem";
    		}
    		else if( dataType.equals( TYPE_CHIPEXO_PEAKS_PEAKZILLA ) )
    		{
    			expTable = "chipexo_experiments";
    			peaksFinishedTable = "chipexo_peaks_finished";
    			peakType = "peakzilla";
    		}
    		else if( dataType.equals( TYPE_HISTONE_MODIFICATIONS ) )
    		{
    			expTable = "hist_experiments";
    			peaksFinishedTable = "hist_peaks_finished";
    			peakType = "macs2";
    		}
    		else if( dataType.equals( TYPE_MNASE_NUCLEOSOMES ) )
    		{
    			expTable = "mnase_experiments";
    			peaksFinishedTable = "mnase_peaks_finished";
    			peakType = "danpos2";
    		}
    	}
    	
    	public String getPeaksFinishedTable() {
			return peaksFinishedTable;
		}

		public String getExpTable() {
			return expTable;
		}

		public String getPeakType() {
			return peakType;
		}

    }
    
    public static class CellLineSelector extends GenericComboBoxEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            ComponentModel model = ComponentFactory.getModel( getBean() );
            Species organism = (Species)model.findProperty( "organism" ).getValue();
            String dataType = (String)model.findProperty( "dataType" ).getValue();
            String organismColumnName = getOrganismColumnName(dataType);
            DataTypeInfo dataTypeInfo = new DataTypeInfo( dataType );
            String peakType = dataTypeInfo.getPeakType();
            String expTable = dataTypeInfo.getExpTable();
            String peaksFinishedTable = dataTypeInfo.getPeaksFinishedTable();
            if(peakType == null || expTable == null || peaksFinishedTable == null)
                return new String[0];

            String query = "SELECT DISTINCT cells.title FROM " + expTable + " e JOIN cells on(e.cell_id=cells.id)"
                    + " JOIN " + peaksFinishedTable + " ON(e.id=exp_id AND peak_type=" + SqlUtil.quoteString( peakType ) + ")"
                    + " WHERE e." + organismColumnName + "=" + SqlUtil.quoteString( organism.getLatinName() )
                    + "ORDER BY 1";
                        
            return SqlUtil.stringStream( con, query ).nonNull().toArray(String[]::new);
        }
    }
    
    public static class TreatmentSelector extends GenericComboBoxEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            ComponentModel model = ComponentFactory.getModel( getBean() );
            Species organism = (Species)model.findProperty( "organism" ).getValue();
            String cell = (String)model.findProperty( "cellLine" ).getValue();
            String dataType = (String)model.findProperty( "dataType" ).getValue();
            String organismColumnName = getOrganismColumnName(dataType);
            DataTypeInfo dataTypeInfo = new DataTypeInfo( dataType );
            String peakType = dataTypeInfo.getPeakType();
            String expTable = dataTypeInfo.getExpTable();
            String peaksFinishedTable = dataTypeInfo.getPeaksFinishedTable();
            if(peakType == null || expTable == null || peaksFinishedTable == null)
                return new String[0];

            String query = "SELECT DISTINCT treatment FROM " + expTable + " e join cells on(cells.id=cell_id)"
                    + " JOIN " + peaksFinishedTable + " ON(e.id=exp_id AND peak_type=" + SqlUtil.quoteString( peakType ) + ")"
                    + " WHERE e." + organismColumnName + "=" + SqlUtil.quoteString( organism.getLatinName() )
                    + " AND cells.title=" + SqlUtil.quoteString( cell )
                    + " ORDER BY 1";
            
            
            return SqlUtil.stringStream( con, query ).nonNull().toArray(String[]::new);
        }
    }
    
    private static String getOrganismColumnName(String dataType)
    {
    	String organismColumnName = "organism";
        if( dataType.equals(TYPE_HISTONE_MODIFICATIONS) || dataType.equals(TYPE_CHIPEXO_PEAKS_GEM) || dataType.equals(TYPE_CHIPEXO_PEAKS_PEAKZILLA) )
        	organismColumnName = "specie";
        return organismColumnName;
    }
    
    public static class ExperimentsEditor extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            ComponentModel model = ComponentFactory.getModel( getBean() );
            Species organism = (Species)model.findProperty( "organism" ).getValue();
            String cell = (String)model.findProperty( "cellLine" ).getValue();
            String treatment = (String)model.findProperty( "treatment" ).getValue();
            String dataType = (String)model.findProperty( "dataType" ).getValue();
            String organismColumnName = getOrganismColumnName(dataType);
            DataTypeInfo dataTypeInfo = new DataTypeInfo( dataType );
            String peakType = dataTypeInfo.getPeakType();
            String expTable = dataTypeInfo.getExpTable();
            String peaksFinishedTable = dataTypeInfo.getPeaksFinishedTable();
            if(peakType == null || expTable == null || peaksFinishedTable == null)
                return new String[0];

            String query = "SELECT e.id FROM " + expTable + " e join cells on(e.cell_id=cells.id)"
                    + " JOIN " + peaksFinishedTable + " ON(e.id=exp_id AND peak_type=" + SqlUtil.quoteString( peakType ) + ")"
                    + " WHERE " + organismColumnName + "=" + SqlUtil.quoteString( organism.getLatinName() )
                    + " AND cells.title=" + SqlUtil.quoteString( cell )
                    + " AND treatment=" + SqlUtil.quoteString( treatment )
                    + " ORDER BY 1";
           
            return SqlUtil.stringStream( con, query ).nonNull().toArray(String[]::new);
        }
    }
    
    public static class ASBTracksEditor extends GenericMultiSelectEditor
    {
    	@Override
    	protected String[] getAvailableValues()
        {
    		List<String> result = new ArrayList<String>();
    		ComponentModel model = ComponentFactory.getModel( getBean() );
    		String dataType = (String)model.findProperty( "dataType" ).getValue();
    		ru.biosoft.access.core.DataElementPath parentDir = null;
    		if( dataType.equals( TYPE_ALLELE_SPECIFIC_BINDING_BY_CELL ) )
    			parentDir = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT 
    					+ "/generic/allele specific binding/cell_level");
    		else if( dataType.equals( TYPE_ALLELE_SPECIFIC_BINDING_BY_TF ) )
    			parentDir = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT 
    					+ "/generic/allele specific binding/tf_level");
    		if( parentDir != null )
    			for( DataElementPath path : parentDir.getChildren())
    				result.add( path.getName() );
    		return result.toArray( new String[ result.size() ] );
        }
    	
    }
    
    public static class DNaseClustersTracksEditor extends GenericMultiSelectEditor
    {
    	@Override
    	protected String[] getAvailableValues()
        {
    		List<String> result = new ArrayList<String>();
    		ru.biosoft.access.core.DataElementPath parentDir = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT 
    					+ "/clusters/DNase-seq clusters");
    		for( DataElementPath path : parentDir.getChildren())
    			result.add( path.getName() );
    		return result.toArray( new String[ result.size() ] );
        }
    	
    }
}
