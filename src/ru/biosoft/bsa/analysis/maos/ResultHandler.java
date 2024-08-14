package ru.biosoft.bsa.analysis.maos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.VCFSqlTrack;
import ru.biosoft.bsa.analysis.WeightMatrixModel;
import ru.biosoft.bsa.analysis.maos.coord_mapping.CoordinateMapping;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;

public class ResultHandler implements IResultHandler
{
    private static final String SCORE_DIFFERENCE = "Score difference";
    private static final String P_VALUE_LOG10_FOLD_CHANGE = "p-value log10 fold change";
    
    protected SummaryStatistics summary;
    protected TableDataCollection summaryTable;
    protected SqlTrack siteGainTrack, siteLossTrack;
    
    protected Set<String> importantSiteIds = new HashSet<>();
    protected SqlTrack importantMutationsTrack;
    
    protected TableDataCollection outputTable;
    private int outputTableRowCount;
    protected Parameters parameters;
    
    protected Logger analysisLog;
    
    protected boolean showMotifLogo = true;
    protected boolean showAlignment = true;
    
    public ResultHandler(Parameters parameters, Logger analysisLog)
    {
        this.parameters = parameters;
        this.analysisLog = analysisLog;
    }
    
    @Override
    public void init()
    {
        Track vcf = parameters.getVcfTrackDataElement();
        DataElementPath chromosomesPath = parameters.getChromosomesPath();
        siteGainTrack = SqlTrack.createTrack( parameters.getSiteGainTrack(), vcf, chromosomesPath );
        siteLossTrack = SqlTrack.createTrack( parameters.getSiteLossTrack(), vcf, chromosomesPath );
        importantMutationsTrack = SqlTrack.createTrack( parameters.getImportantMutationsTrack(), vcf, chromosomesPath, VCFSqlTrack.class );
        summary = new SummaryStatistics( parameters.getSiteModelCollection(), vcf );
        createOutputTable();
    }
    
    protected void createOutputTable()
    {
        outputTable = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        ColumnModel cm = outputTable.getColumnModel();
        cm.addColumn( "Site model", DataType.Text );
        cm.addColumn( "Site gemomic position", DataType.Text );
        cm.addColumn( "Event type", DataType.Text );
        cm.addColumn( "Mutations", DataType.Text );
        if(showAlignment)
            cm.addColumn( "Alignment", DataType.Text );
        if(showMotifLogo)
            cm.addColumn( "Motif logo", CompositeView.class );
        cm.addColumn( "Score on reference", DataType.Float );
        cm.addColumn( "Score on alternative", DataType.Float );
        cm.addColumn( SCORE_DIFFERENCE, DataType.Float );
        
        
        if( parameters.isPValueMode() )
        {
            cm.addColumn( "p-value on reference", DataType.Float );
            cm.addColumn( "p-value on alternative", DataType.Float );
            cm.addColumn( P_VALUE_LOG10_FOLD_CHANGE, DataType.Float );
        }
        cm.addColumn( "Target genes", DataType.Text );
    }

    @Override
    public void siteMutationEvent(SiteMutation sm)
    {
        Site s = sm.createReferenceSite( parameters.isPValueMode() );

        SiteModel model = sm.model;
        if(sm.isSiteLoss()) // site loss
        {
            if(s != null)
                siteLossTrack.addSite( s );
            summary.siteLoss( model );
        } else // site gain
        {
            if(s != null)
                siteGainTrack.addSite( s );
            summary.siteGain( model );
        }
        
        for(Variation v : sm.variations)
            importantSiteIds.add( v.id );
        
        List<Object> rowValues = createOutTableRow( sm, s, model );
        TableDataCollectionUtils.addRow( outputTable, String.valueOf(++outputTableRowCount), rowValues.toArray(), true );
    }


    protected List<Object> createOutTableRow(SiteMutation sm, Site s, SiteModel model)
    {
        List<Object> rowValues = new ArrayList<>();
        rowValues.add( model.getName() );
        String genomicPos = s == null ? "" : s.getOriginalSequence().getName() + ":" + s.getFrom() + "-" + s.getTo() + ":" + (s.getStrand()==StrandType.STRAND_PLUS ? "+" : "-");
        rowValues.add( genomicPos );
        rowValues.add( sm.isSiteLoss() ? "loss" : "gain");
        rowValues.add( Stream.of( sm.variations ).map( Object::toString ).collect( Collectors.joining( "|" ) ) );
        if(showAlignment)
            rowValues.add( createAlignment(sm.refSeq, sm.refPos, sm.altSeq, sm.altPos, model.getLength(), sm.ref2alt, sm.alt2ref) );
        if(showMotifLogo)
            rowValues.add( createLogo(model) );
        rowValues.add( sm.refScore );
        rowValues.add( sm.altScore );
        rowValues.add( sm.scoreDiff );
        if(parameters.isPValueMode())
        {
            rowValues.add( sm.refPValue );
            rowValues.add( sm.altPValue );
            rowValues.add( sm.pValueLogFC );
        }
        
        
        String targetGenes;
        Interval siteInterval = sm.getSiteInterval();
        if(siteInterval == null)
        {
            targetGenes = "";
        }
        else
        {
            if(parameters.isOneNearestTargetGene())
            {
                targetGenes = sm.genes.getAll().stream()
                    .min( Comparator.comparingInt( tss->Math.abs( tss.getPosRelativeToTSS( siteInterval ) ) ) )
                    .map(  tss->tss.getRelativePosDescription( siteInterval ))
                    .orElseGet( ()->"" );
            }
            else
            {
                targetGenes = sm.genes.getAll().stream()
                    .sorted( Comparator.comparingInt( tss->tss.getPosRelativeToTSS( siteInterval ) ) )
                    .map(  tss->tss.getRelativePosDescription( siteInterval ))
                    .collect( Collectors.joining( ", " ) );
            }
        }
        rowValues.add( targetGenes );
        
        
        return rowValues;
    }

    private Object createLogo(SiteModel model)
    {
        if(model instanceof WeightMatrixModel)
            return ((WeightMatrixModel)model).getView();
        return new CompositeView();//empty
    }

    private String createAlignment(Sequence refSeq, int refPos, Sequence altSeq, int altPos, int length, CoordinateMapping ref2alt, CoordinateMapping alt2ref)
    {
        StringBuilder refSiteSeq = new StringBuilder();
        StringBuilder altSiteSeq = new StringBuilder();
        
        if(refPos == -1)
        {
            for(int i = 0; i < length; i++)
            {
                refSiteSeq.append( "-" );
                char c = (char)altSeq.getLetterAt( altPos + i );
                altSiteSeq.append( Character.toUpperCase( c ) );
            }
        } else if(altPos == -1)
        {
            for(int i = 0; i < length; i++)
            {
                char c = (char)refSeq.getLetterAt( refPos + i );
                refSiteSeq.append( Character.toUpperCase( c ) );
                altSiteSeq.append( "-" );
            }
        }
        else
        {
            int refL = refPos;
            int refR = refPos + length - 1;
            int altL = altPos;
            int altR = altPos + length - 1;

            for( int i = refPos; i < refPos + length; i++ )
            {
                int alt = ref2alt.get( i );
                if( alt != -1 )
                {
                    if( alt < altL )
                        altL = alt;
                    else if( alt > altR )
                        altR = alt;
                }
            }
            
            for( int i = altPos; i < altPos + length; i++)
            {
                int ref = alt2ref.get( i );
                if(ref != -1)
                {
                    if(ref < refL)
                        refL = ref;
                    else if(ref > refR)
                        refR = ref;
                }
            }
            
            int xRef=  refL;
            int xAlt = altL;
            while(xRef <= refR || xAlt <= altR)
            {
                while(xRef <= refR && ref2alt.get( xRef ) == -1)
                {
                    char c = (xRef < refPos || xRef >= refPos + length) ? '.' :(char)refSeq.getLetterAt( xRef );
                    refSiteSeq.append( Character.toUpperCase( c ) );
                    altSiteSeq.append( '-' );
                    xRef++;
                }
                while(xAlt <= altR && alt2ref.get( xAlt ) == -1)
                {
                    refSiteSeq.append( '-' );
                    char c = (xAlt < altPos || xAlt >= altPos + length) ? '.' :(char)altSeq.getLetterAt( xAlt );
                    altSiteSeq.append( Character.toUpperCase( c ) );
                    xAlt++;
                }
                if(xRef <= refR )
                {
                    if(xAlt > altR)
                        throw new AssertionError();
                    if(ref2alt.get( xRef ) != xAlt || alt2ref.get( xAlt ) != xRef)
                        throw new AssertionError();
                    
                    char cRef = (char)refSeq.getLetterAt( xRef );
                    char cAlt = (char)altSeq.getLetterAt( xAlt );
                    cRef = Character.toLowerCase( cRef );
                    cAlt = Character.toLowerCase( cAlt );
                    if(cRef != cAlt)
                    {
                        cRef = Character.toUpperCase( cRef );
                        cAlt = Character.toUpperCase( cAlt );
                    }
                    
                    if(xRef < refPos || xRef >= refPos + length)
                        cRef = '.';
                    if(xAlt < altPos || xAlt >= altPos + length)
                        cAlt = '.';
                        
                    refSiteSeq.append( cRef );
                    xRef++;
                    
                    
                    altSiteSeq.append( cAlt );
                    xAlt++;
                }
            }
        }
        
        return  "<pre style='font-family:monospace;font-size:2em;'>" + refSiteSeq + "\n" + altSiteSeq + "</pre>";
    }

    @Override
    public void finish() throws Exception
    {
        siteGainTrack.finalizeAddition();
        siteLossTrack.finalizeAddition();
        parameters.getSiteGainTrack().save( siteGainTrack );
        parameters.getSiteLossTrack().save( siteLossTrack );

        outputTable.finalizeAddition();
        setOutTableSortOrder();
        parameters.getOutputTable().save( outputTable );
        
        summaryTable = summary.makeTable( parameters.getSummaryTable() );
        
        Track inputVcf = parameters.getVcfTrackDataElement();
        DataCollection<Site> vcfSites  = inputVcf.getAllSites();
        for(String siteId : importantSiteIds)
        {
            Site site = vcfSites.get( siteId );
            importantMutationsTrack.addSite( site );
        }
        importantMutationsTrack.finalizeAddition();
        parameters.getImportantMutationsTrack().save( importantMutationsTrack );
    }
    
    protected void setOutTableSortOrder()
    {
        if(parameters.isPValueMode())
            TableDataCollectionUtils.setSortOrder( outputTable, P_VALUE_LOG10_FOLD_CHANGE, false );
        else
            TableDataCollectionUtils.setSortOrder( outputTable, SCORE_DIFFERENCE, false );
    }
    
    @Override
    public Object[] getResults()
    {
        return new Object[] {siteGainTrack, siteLossTrack, importantMutationsTrack, summaryTable, outputTable};
    }
    
}