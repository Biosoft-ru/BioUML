package biouml.plugins.gtrd.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import biouml.plugins.gtrd.ChIPseqExperiment;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.analysis.SortSqlTrack;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.bean.BeanInfoEx2;

public class JoinGTRDTracks extends AnalysisMethodSupport<JoinGTRDTracks.Parameters>
{

    public JoinGTRDTracks(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        if( !parameters.getOutputFolder().exists() )
            DataCollectionUtils.createSubCollection( parameters.getOutputFolder() );
        final DataCollection<DataElement> outputFolder = parameters.getOutputFolder().getDataCollection();
        final List<SqlTrack> results = new ArrayList<>();

        jobControl.pushProgress( 0, 70 );
        jobControl.forCollection(
                DataCollectionUtils.asCollection( parameters.getExperimentsPath(), ChIPseqExperiment.class ),
                exp -> {
                    if(!parameters.getEnsembl().getSpecie().getLatinName().equals( exp.getSpecie().getLatinName() ))
                        return true;
                    if(exp.isControlExperiment())
                        return true;
                    DataElementPath peaksPath = parameters.getPathForDataSet( exp.getPeak() );
                    if( !peaksPath.exists() )
                        return true;
                    try
                    {
                        String name = exp.getSpecie().getLatinName() + " " + parameters.getDataSet();
                        if( ! ( outputFolder.get( name ) instanceof WritableTrack ) )
                        {
                            Properties properties = new Properties();
                            properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
                            properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY,
                                    parameters.getEnsembl().getPrimarySequencesPath().toString() );
                            String unmappableTrackPath = "databases/GTRD/Data/generic/mappability/" + parameters.getEnsembl().getSpecie().getLatinName() + " unmappable 50";
                            properties.setProperty( Track.OPEN_WITH_TRACKS, parameters.getEnsembl().getGenesTrack().getCompletePath() + ";" + unmappableTrackPath );
                            SqlTrack t = new SqlTrack(outputFolder, properties);
                            outputFolder.put( t );
                            results.add( t );
                        }
                        WritableTrack out = (WritableTrack)outputFolder.get( name );
                        Track in = peaksPath.getDataElement( Track.class );
                        for(Site s : in.getAllSites())
                        {
                            SiteImpl copy = ((SiteImpl)s).clone(null);
                            copy.getProperties().add( new DynamicProperty( "experiment", String.class, exp.getName() ) );
                            copy.getProperties().add( new DynamicProperty( "uniprotId", String.class, exp.getTfUniprotId() ) );
                            copy.getProperties().add( new DynamicProperty( "tfTitle", String.class, exp.getTfTitle() ) );
                            
                            if(exp.getTfClassId() != null)
                                copy.getProperties().add( new DynamicProperty( "tfClassId", String.class, exp.getTfClassId() ) );
                            
                            copy.getProperties().add( new DynamicProperty( "antibody", String.class, exp.getAntibody() ) );
                            copy.getProperties().add( new DynamicProperty( "cellLine", String.class, exp.getCell().getTitle()) );
                            copy.getProperties().add( new DynamicProperty( "treatment", String.class, exp.getTreatment()) );
                            copy.setType( exp.getTfTitle() );
                            
                            copy.getProperties().remove( "name" );//skip useless property of PICS sites
                            out.addSite( copy );
                        }
                    }
                    catch( Exception e )
                    {
                        throw ExceptionRegistry.translateException( e );
                    }

                    return true;
                } );

        for(WritableTrack t : results)
        {
            t.finalizeAddition();
            t.getCompletePath().save( t );
        }
        
        jobControl.popProgress();

        log.info( "Sorting tracks" );
        jobControl.pushProgress( 70, 80 );
        for(SqlTrack t : results)
            SortSqlTrack.sortSqlTrack( t, true );
        jobControl.popProgress();
        
        log.info( "Indexing tracks" );
        jobControl.pushProgress( 80, 100 );
        for(SqlTrack t : results)
            SqlUtil.execute( t.getConnection(), "ALTER TABLE " + SqlUtil.quoteIdentifier( t.getTableId() ) + " ADD INDEX(prop_uniprotId), ADD INDEX(prop_cellLine)" );
        jobControl.popProgress();
        
        return results.toArray();
    }
    
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath experimentsPath, outputFolder;
        private String dataSet = "macs2";
        private DataElementPath peaksFolder;

        public DataElementPath getOutputFolder()
        {
            return outputFolder;
        }

        public void setOutputFolder(DataElementPath outputFolder)
        {
            Object oldValue = this.outputFolder;
            this.outputFolder = outputFolder;
            firePropertyChange( "outputFolder", oldValue, outputFolder );
        }

        public DataElementPath getExperimentsPath()
        {
            return experimentsPath;
        }

        public void setExperimentsPath(DataElementPath experimentsPath)
        {
            Object oldValue = this.experimentsPath;
            this.experimentsPath = experimentsPath;
            firePropertyChange( "experimentsPath", oldValue, experimentsPath );
        }
        
        public String getDataSet()
        {
            return dataSet;
        }

        public void setDataSet(String dataSet)
        {
            Object oldValue = this.dataSet;
            this.dataSet = dataSet;
            firePropertyChange( "dataSet", oldValue, dataSet );
        }
       
        private EnsemblDatabase ensembl = EnsemblDatabaseSelector.getDefaultEnsembl();
        public EnsemblDatabase getEnsembl()
        {
            return ensembl;
        }

        public void setEnsembl(EnsemblDatabase ensembl)
        {
            Object oldValue = this.ensembl;
            this.ensembl = ensembl;
            firePropertyChange( "ensembl", oldValue, ensembl );
        }

        public DataElementPath getPeaksFolder()
        {
            return peaksFolder;
        }
        public void setPeaksFolder(DataElementPath peaksFolder)
        {
            Object oldValue = this.peaksFolder;
            this.peaksFolder = peaksFolder;
            firePropertyChange( "peaksFolder", oldValue, peaksFolder );
        }

        public DataElementPath getPathForDataSet(DataElementPath path)
        {
            String peakId = path.getName();
            return peaksFolder.getChildPath( dataSet ).getChildPath( peakId );
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
            super.initProperties();
            add( DataElementPathEditor.registerInputChild( "experimentsPath", beanClass, ChIPseqExperiment.class ) );
            property("peaksFolder").inputElement( FolderCollection.class ).add();
            add( "dataSet", DataSetEditor.class );
            add( "ensembl" );
            property( "outputFolder" ).outputElement( FolderCollection.class ).add();
        }
    }
    
    public static class DataSetEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[] {"macs2", "sissrs", "gem", "pics"};
        }
    }


}
