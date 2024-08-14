package biouml.plugins.gtrd.analysis;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import ru.biosoft.jobcontrol.JobControl;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteIntervalsMap;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.SqlColumnModel;
import ru.biosoft.table.SqlTableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

public class PrepareSearchByRegulation extends AnalysisMethodSupport<PrepareSearchByRegulation.Parameters>
{

    public PrepareSearchByRegulation(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final Track geneTrack = parameters.getEnsembl().getGenesTrack();
        log.info("Reading genes...");
        jobControl.pushProgress(0, 10);
        final Map<String, SiteIntervalsMap> genesMap = new HashMap<>();
        jobControl.forCollection(parameters.getEnsembl().getPrimarySequencesPath().getChildren(), chrPath -> {
            try
            {
                Sequence sequence = chrPath.getDataElement(AnnotatedSequence.class).getSequence();
                DataCollection<Site> genes = geneTrack.getSites(chrPath.toString(), sequence.getStart(), sequence.getLength());
                if(genes != null && genes.getSize() > 0) genesMap.put(chrPath.getName(), new SiteIntervalsMap(genes));
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            return true;
        });
        jobControl.popProgress();
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;

        SqlTrack input = parameters.getAllPeaksPath().getDataElement(SqlTrack.class);

        log.info("Creating gene table...");
        jobControl.pushProgress(10, 70);
        final SqlTableDataCollection outputTable = (SqlTableDataCollection)TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        SqlColumnModel cm = outputTable.getColumnModel();
        cm.addColumn( "SiteID", Integer.class );
        cm.addColumn( "GeneID", String.class );
        cm.addColumn( "Distance", Integer.class );
        final AtomicInteger nextRowID = new AtomicInteger( 1 );
        jobControl.forCollection( DataCollectionUtils.asCollection( input.getAllSites(), Site.class ), site -> {
            SiteIntervalsMap chromosomeGenes = genesMap.get(site.getOriginalSequence().getName());
            if(chromosomeGenes != null)
            {
                for(Site gene : chromosomeGenes.getIntervals(site.getFrom()-parameters.getGeneMaxDistance(), site.getTo()+parameters.getGeneMaxDistance()) )
                {
                    int distance = Math.max( 0, Math.max( site.getFrom() - gene.getTo(), gene.getFrom() - site.getTo() ) );

                    RowDataElement row = new RowDataElement( String.valueOf( nextRowID.getAndIncrement() ), outputTable );
                    row.setValues( new Object[] {site.getName(), gene.getProperties().getValueAsString( "id" ), distance} );
                    outputTable.addRow( row );
                }
            }
            return true;
        } );
        outputTable.finalizeAddition();
        parameters.getOutputTable().save( outputTable );
        jobControl.popProgress();

        log.info( "Indexing table" );
        jobControl.pushProgress( 70, 100 );
        Connection con = outputTable.getConnection();
        SqlUtil.execute( con, "ALTER TABLE " + SqlUtil.quoteIdentifier( outputTable.getTableId() ) + " ADD UNIQUE INDEX(SiteID, GeneID), ADD INDEX(GeneID, Distance)" );
        SqlUtil.execute( con, "ANALYZE TABLE " + SqlUtil.quoteIdentifier( outputTable.getTableId() ) );
        jobControl.popProgress();

        return new Object[] {outputTable};
    }


    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath allPeaksPath;
        private EnsemblDatabase ensembl = EnsemblDatabaseSelector.getDefaultEnsembl();
        private int geneMaxDistance = 50000;

        private DataElementPath outputTable;

        public DataElementPath getAllPeaksPath()
        {
            return allPeaksPath;
        }

        public void setAllPeaksPath(DataElementPath allPeaksPath)
        {
            Object oldValue = this.allPeaksPath;
            this.allPeaksPath = allPeaksPath;
            firePropertyChange( "allPeaksPath", oldValue, allPeaksPath );
        }

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

        public int getGeneMaxDistance()
        {
            return geneMaxDistance;
        }

        public void setGeneMaxDistance(int geneMaxDistance)
        {
            Object oldValue = this.geneMaxDistance;
            this.geneMaxDistance = geneMaxDistance;
            firePropertyChange( "geneMaxDistance", oldValue, geneMaxDistance );
        }

        public DataElementPath getOutputTable()
        {
            return outputTable;
        }

        public void setOutputTable(DataElementPath outputTable)
        {
            Object oldValue = this.outputTable;
            this.outputTable = outputTable;
            firePropertyChange( "outputTable", oldValue, outputTable );
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
            property( "allPeaksPath" ).inputElement( SqlTrack.class ).add();
            add("ensembl");
            add("geneMaxDistance");
            property( "outputTable" ).outputElement( SqlTableDataCollection.class ).add();
        }
    }
}
