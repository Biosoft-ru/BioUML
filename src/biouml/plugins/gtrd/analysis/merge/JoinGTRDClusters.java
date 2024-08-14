package biouml.plugins.gtrd.analysis.merge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import biouml.plugins.gtrd.analysis.merge.MergePeakCallers.Caller;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.SortSqlTrack;
import ru.biosoft.util.bean.BeanInfoEx2;

public class JoinGTRDClusters extends AnalysisMethodSupport<JoinGTRDClusters.Parameters>
{

    public JoinGTRDClusters(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElementPath outFolder = parameters.getOutputFolder();
        if(!outFolder.exists())
        {
            DataCollectionUtils.createFoldersForPath( outFolder );
            DataCollectionUtils.createSubCollection( outFolder );
        }
        
        Map<String, String> tfNames = loadTFNames();
        Map<String, String> tfClasses = loadTFClasses();
        
        Map<Caller, SqlTrack> unionTracks = new HashMap<>();
        for(Caller caller : Caller.values())
        {
            DataElementPath path = outFolder.getChildPath( "all " + caller + " clusters" );
            SqlTrack t;
            if(path.exists()) 
            	t = path.getDataElement( SqlTrack.class );
            else
            {
            	t = SqlTrack.createTrack( path, null, parameters.getEnsembl().getPrimarySequencesPath() );
            	addOpenWithTracks( t );
            	path.save( t );
            }
            unionTracks.put( caller, t );
        }
        
        DataElementPath unionMetaTrackPath = outFolder.getChildPath( "all meta clusters" );
        SqlTrack unionMetaTrack;
        if(unionMetaTrackPath.exists())
        	unionMetaTrack = unionMetaTrackPath.getDataElement( SqlTrack.class );
        else
        {
        	unionMetaTrack = SqlTrack.createTrack( unionMetaTrackPath, null, parameters.getEnsembl().getPrimarySequencesPath() );
        	addOpenWithTracks( unionMetaTrack );
        	unionMetaTrackPath.save( unionMetaTrack );
        }

        if( !parameters.isDoOnlySort() )
        {

        	ru.biosoft.access.core.DataElementPath byTF = outFolder.getChildPath( "By TF" );
        	if(!byTF.exists())
        		DataCollectionUtils.createSubCollection( byTF );

        	jobControl.pushProgress( 0, 90 );
        	jobControl.forCollection( parameters.getInputFolder().getChildren(), path -> {
        		String uniprotId = path.getName();

        		String tfName = tfNames.get( uniprotId );
        		if(tfName == null)
        			throw new RuntimeException("Unknown uniprot id " + uniprotId);

        		String tfClass = tfClasses.get( uniprotId );

        		for(Caller caller : Caller.values()) {
        			SqlTrack target = unionTracks.get( caller );
        			ru.biosoft.access.core.DataElementPath sourcePath = path.getChildPath( caller.toString() + " clusters" );
        			if(!sourcePath.exists()) {
        				log.warning( "Not found " + sourcePath );
        				continue;
        			}
        			Track source = sourcePath.getDataElement( Track.class );
        			for(Site s : source.getAllSites())
        				addSite( s, target, uniprotId, tfName, tfClass );

        		}

        		SqlTrack metaClusters = path.getChildPath( "meta clusters" ).getDataElement( SqlTrack.class );
        		for(Site s : metaClusters.getAllSites())
        			addSite( s, unionMetaTrack, uniprotId, tfName, tfClass );

        		return true;
        	} );
        	for(SqlTrack t : unionTracks.values())
        		t.finalizeAddition();
        	unionMetaTrack.finalizeAddition();
        }
        else
        {
        	jobControl.pushProgress( 0, 90 );
        }
        jobControl.popProgress();
        
        jobControl.pushProgress( 90, 100 );
        log.info( "Sorting tracks" );
        for(SqlTrack t : unionTracks.values())
            SortSqlTrack.sortSqlTrack( t, true );
        SortSqlTrack.sortSqlTrack( unionMetaTrack, true );
        jobControl.popProgress();
        
        return unionMetaTrack;
    }

    private void addOpenWithTracks(SqlTrack t)
    {
        Properties properties = t.getInfo().getProperties();
        String unmappableTrackPath = "databases/GTRD/Data/generic/mappability/" + parameters.getEnsembl().getSpecie().getLatinName() + " unmappable 50";
        properties.setProperty( Track.OPEN_WITH_TRACKS, parameters.getEnsembl().getGenesTrack().getCompletePath() + ";" + unmappableTrackPath );
    }

    private void addSite(Site s, SqlTrack track, String uniprotId, String tfName, String tfClass)
    {
        SiteImpl copy = ((SiteImpl)s).clone(null);
        copy.getProperties().add( new DynamicProperty( "uniprotId", String.class, uniprotId ) );
        if(tfClass != null)
            copy.getProperties().add( new DynamicProperty( "tfClassId", String.class, tfClass ) );
        copy.getProperties().add( new DynamicProperty( "tfTitle", String.class, tfName ) );
        copy.setType( tfName );
        track.addSite( copy );
    }
    
    private Map<String, String> loadTFNames() throws SQLException
    {
        Map<String, String> result = new HashMap<>();
        DataCollection<DataElement> dc = parameters.getInputFolder().getDataCollection();
        Connection con = SqlConnectionPool.getConnection( dc );
        String species = parameters.getEnsembl().getSpecie().getLatinName();
        try(PreparedStatement ps = con.prepareStatement( "SELECT id,gene_name FROM uniprot WHERE species=?" ))
        {
            ps.setString( 1, species );
            try( ResultSet rs = ps.executeQuery() )
            {
                while( rs.next() )
                {
                    String uniprotId = rs.getString( 1 );
                    String geneName = rs.getString( 2 );
                    result.put( uniprotId, geneName );
                }
            }
        }
        return result;
    }
    
    private Map<String, String> loadTFClasses() throws SQLException
    {
        Map<String, String> result = new HashMap<>();
        DataCollection<DataElement> dc = parameters.getInputFolder().getDataCollection();
        Connection con = SqlConnectionPool.getConnection( dc );
        String species = parameters.getEnsembl().getSpecie().getLatinName();
        try(PreparedStatement ps = con.prepareStatement( "SELECT id,cached_tf_class FROM uniprot WHERE species=? AND NOT(isNULL(cached_tf_class))" ))
        {
            ps.setString( 1, species );
            try( ResultSet rs = ps.executeQuery() )
            {
                while( rs.next() )
                {
                    String uniprotId = rs.getString( 1 );
                    String tfClass = rs.getString( 2 );
                    result.put( uniprotId, tfClass );
                }
            }
        }
        return result;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputFolder;
        @PropertyName("Input folder")
        @PropertyDescription("The folder produced by 'Make meta tracks' analysis")
        public DataElementPath getInputFolder()
        {
            return inputFolder;
        }

        public void setInputFolder(DataElementPath inputFolder)
        {
            Object oldValue = this.inputFolder;
            this.inputFolder = inputFolder;
            firePropertyChange( "inputFolder", oldValue, inputFolder );
        }
        
        private EnsemblDatabase ensembl = EnsemblDatabaseSelector.getDefaultEnsembl();
        @PropertyName("Ensembl")
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
        
        private DataElementPath outputFolder;
        @PropertyName("Output folder")
        @PropertyDescription("Output folder")
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
        
        private boolean doOnlySort = false;
        @PropertyName("Do only sorting of existing tracks?")
        @PropertyDescription("Do only sorting of existing tracks?")
		public boolean isDoOnlySort() {
			return doOnlySort;
		}

		public void setDoOnlySort(boolean doOnlySort) {
			this.doOnlySort = doOnlySort;
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
            property( "inputFolder" ).inputElement( FolderCollection.class ).add();
            add( "ensembl" );
            property( "outputFolder" ).outputElement( FolderCollection.class ).add();
            add("doOnlySort");
        }
    }
}
