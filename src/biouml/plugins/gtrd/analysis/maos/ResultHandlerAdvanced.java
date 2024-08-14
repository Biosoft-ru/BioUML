package biouml.plugins.gtrd.analysis.maos;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.maos.ResultHandler;
import ru.biosoft.bsa.analysis.maos.SiteMutation;
import ru.biosoft.table.ColumnEx;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;

public class ResultHandlerAdvanced extends ResultHandler
{
    private static final String FINAL_SCORE_COLUMN = "Final score";
    
    private Set<SiteReference> peaksUnique = new HashSet<>();
    private SqlTrack selectedPeaksTrack;

    public ResultHandlerAdvanced(AdvancedParameters parameters, Logger analysisLog)
    {
        super( parameters, analysisLog );
    }
    
    @Override
    public void init()
    {
        super.init();
        Track vcf = parameters.getVcfTrackDataElement();
        DataElementPath chromosomesPath = parameters.getChromosomesPath();
        selectedPeaksTrack = SqlTrack.createTrack( getParameters().getSelectedGTRDPeaks(), vcf, chromosomesPath );
    }
    
    protected AdvancedParameters getParameters()
    {
        return (AdvancedParameters)parameters;
    }
    
    @Override
    public void siteMutationEvent(SiteMutation sm)
    {
        super.siteMutationEvent( sm );
        SiteMutationAdvanced sma = (SiteMutationAdvanced)sm;
        for(GTRDPeak peak : sma.peaks)
            peaksUnique.add( peak.getOriginalPeak() );
    }
    
    @Override
    protected void createOutputTable()
    {
        super.createOutputTable();
        ColumnModel cm = outputTable.getColumnModel();
        TableColumn col = cm.addColumn( "TFClass", DataType.Text );
        col.setValue( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, "Categories: GTRD" );
        col = cm.addColumn( "TFUniprot", DataType.Text );
        col.setValue( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, "Proteins: UniProt" );
        cm.addColumn( "GTRD cell", DataType.Text );
        cm.addColumn( "GTRD treatment", DataType.Text );
        cm.addColumn( "GTRD peak count", DataType.Integer );
        cm.addColumn( "Avg. peak score", DataType.Float );
        cm.addColumn( FINAL_SCORE_COLUMN, DataType.Float );
        if(getParameters().isAddTranspathAnnotation())
        {
            col = new TableColumn( "Transpath pathways", String.class );
            col.setValue( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, "Pathways: Transpath" );
            col.setValue( ColumnEx.DISPLAY_TITLE, "true" );
            cm.addColumn( col );
        }
        if(getParameters().isAddPsdAnnotation())
        {
            col = new TableColumn( "HumanPSD disease", String.class );
            col.setValue( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, "Disease: MeSH" );
            col.setValue( ColumnEx.DISPLAY_TITLE, "true" );
            cm.addColumn( col );
        }
    }
    
    @Override
    protected List<Object> createOutTableRow(SiteMutation sm, Site s, SiteModel model)
    {
        SiteMutationAdvanced sma = (SiteMutationAdvanced)sm;
        List<Object> row = super.createOutTableRow( sm, s, model );
        row.add( sma.tfClass );
        row.add( String.join( ", ", sma.tfUniprots) );
        row.add( sma.cell );
        row.add( sma.treatment );
        row.add( sma.peaks.length );
        row.add( sma.avgPeakScore );
        row.add( sma.finalScore );
        if(getParameters().isAddTranspathAnnotation())
            row.add( String.join( ", ",  Arrays.asList( sma.pathways )  ) );
        if(getParameters().isAddPsdAnnotation())
            row.add( String.join( ", ",  Arrays.asList( sma.diseaseList )  ) );
        return row;
    }
    
    @Override
    protected void setOutTableSortOrder()
    {
        TableDataCollectionUtils.setSortOrder( outputTable, FINAL_SCORE_COLUMN, false );
    }
    
    @Override
    public void finish() throws Exception
    {
        super.finish();
        
        for(SiteReference siteRef : peaksUnique)
        {
            Site peak = siteRef.getSite();
            selectedPeaksTrack.addSite( peak );    
        }
        
        selectedPeaksTrack.finalizeAddition();
        addOpenWithTracks();
        getParameters().getSelectedGTRDPeaks().save( selectedPeaksTrack );
    }
    
    protected void addOpenWithTracks()
    {
        Properties properties = selectedPeaksTrack.getInfo().getProperties();
        String otherTracks = String.join( ";",
                parameters.getEnsemblPath().getChildPath( "Tracks", "Genes" ).toString(),
                parameters.getImportantMutationsTrack().toString(),
                parameters.getSiteGainTrack().toString(),
                parameters.getSiteLossTrack().toString() );
        properties.setProperty( Track.OPEN_WITH_TRACKS, otherTracks );
    }

    
    @Override
    public Object[] getResults()
    {
        Object[] results = super.getResults();
        Object[] newResults = new Object[results.length + 1];
        System.arraycopy( results, 0, newResults, 0, results.length );
        newResults[results.length] = selectedPeaksTrack;
        return newResults;
    }
}
