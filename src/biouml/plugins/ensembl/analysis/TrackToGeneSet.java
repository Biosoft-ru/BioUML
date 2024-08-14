package biouml.plugins.ensembl.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;

import biouml.plugins.ensembl.JavaScriptEnsembl;
import biouml.plugins.ensembl.analysis.SiteData.Location;
import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.TextUtil;

// TODO: Add Javascript method
@ClassIcon("resources/track-to-gene-set.gif")
public class TrackToGeneSet extends AnalysisMethodSupport<TrackToGeneSetParameters>
{
    private int fivePrimeFlankSize;
    private int threePrimeFlankSize;
    private int maxAdditionalSize;

    public TrackToGeneSet(DataCollection origin, String name)
    {
        super(origin, name, JavaScriptEnsembl.class, new TrackToGeneSetParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
        checkNotEmpty("sourcePaths");
        checkNotEmpty("resultTypes");
    }
    
    private static class SiteIndex
    {
        Site site;
        int index;

        public SiteIndex(Site site, int index)
        {
            super();
            this.site = site;
            this.index = index;
        }
    }
    
    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        validateParameters();
        DataElementPath ensemblPath = TrackUtils.getEnsemblPath(parameters.getSpecies(), parameters.getDestPath());
        final DataElementPath sequencesPath = TrackUtils.getPrimarySequencesPath(ensemblPath);
        DataCollection<?> sequencesDC = sequencesPath.getDataElement(DataCollection.class);
        final Track geneTrack = ensemblPath.getChildPath("Tracks", "Genes").getDataElement(Track.class);
        fivePrimeFlankSize = Math.abs(parameters.getFrom());
        threePrimeFlankSize = Math.abs(parameters.getTo());
        maxAdditionalSize = Math.max(fivePrimeFlankSize, threePrimeFlankSize);
        log.info("Mapping to genes...");
        jobControl.pushProgress(0, 100);
        final TableDataCollection resTable = TableDataCollectionUtils.createTableDataCollection(parameters.getDestPath());
        final ColumnModel columnModel = resTable.getColumnModel();
        columnModel.addColumn("Gene symbol", String.class);
        for(SiteAggregator resultType: parameters.getResultTypes())
        {
            for(DataElementPath path: parameters.getSourcePaths())
            {
                columnModel.addColumn(columnModel.generateUniqueColumnName(path.getName()+": "+resultType), resultType.getType());
            }
        }
        
        jobControl.forCollection(sequencesDC.getNameList(), sequenceName -> {
            DataElementPath fullSequencePath = sequencesPath.getChildPath(sequenceName);
            try
            {
                Sequence sequence = fullSequencePath.getDataElement(AnnotatedSequence.class).getSequence();
                final DataCollection<Site> genes = geneTrack.getSites(fullSequencePath.toString(), sequence.getStart(), sequence.getLength());
                final SortedMap<Integer, List<SiteIndex>> trackIntervals = new TreeMap<>();
                jobControl.setPreparedness(20);
                int i=0;
                for(DataElementPath path: parameters.getSourcePaths())
                {
                    Track track = path.getDataElement(Track.class);
                    DataCollection<Site> sites = track.getSites(fullSequencePath.toString(), sequence.getStart(), sequence.getLength());
                    for(Site site: sites)
                    {
                        int siteCenter = (site.getFrom()+site.getTo())/2;
                        trackIntervals.computeIfAbsent( siteCenter, sc -> new ArrayList<>() ).add( new SiteIndex( site, i ) );
                    }
                    i++;
                }
                jobControl.setPreparedness(40);
                jobControl.pushProgress(40, 100);
                jobControl.forCollection(DataCollectionUtils.asCollection(genes, Site.class), gene -> processGene(gene, trackIntervals, resTable));
                jobControl.popProgress();
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, e.getMessage(), e);
                return true;
            }
            return true;
        });
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
        {
            parameters.getDestPath().remove();
            return null;
        }
        jobControl.popProgress();
        resTable.finalizeAddition();
        resTable.setReferenceType(ReferenceTypeRegistry.getReferenceType(EnsemblGeneTableType.class).toString());
        CollectionFactoryUtils.save(resTable);
        return resTable;
    }

    private boolean processGene(Site gene, SortedMap<Integer, List<SiteIndex>> trackIntervals, TableDataCollection resTable)
    {
        Interval fullGeneInterval = (new Interval(-fivePrimeFlankSize, gene.getLength()+threePrimeFlankSize-1)).translateFromSite(gene);
        Interval fivePrimeInterval = (new Interval(-fivePrimeFlankSize,-1)).translateFromSite(gene);
        Interval threePrimeInterval = (new Interval(gene.getLength(), gene.getLength()+threePrimeFlankSize-1)).translateFromSite(gene);
        Interval geneInterval = gene.getInterval();
        List<List<SiteData>> results = null;
        for( Entry<Integer, List<SiteIndex>> entry : trackIntervals.subMap(fullGeneInterval.getFrom(), fullGeneInterval.getTo())
                .entrySet() )
        {
            Interval siteInterval = new Interval(entry.getKey(), entry.getKey());
            SiteData siteData = null;
            if(fivePrimeInterval.intersects(siteInterval))
            {
                siteData = new SiteData(Location.FIVE_PRIME, gene.getStrand(), -Math.abs(siteInterval.getFrom() - gene.getStart()));
            } else
            {
                if(threePrimeInterval.intersects(siteInterval))
                {
                    //int geneEnd = gene.getStrand()==StrandType.STRAND_MINUS?gene.getFrom():gene.getTo();
                    siteData = new SiteData(Location.THREE_PRIME, gene.getStrand(), Math.abs(siteInterval.getFrom() - gene.getStart()));
                }
                else
                {
                    if(geneInterval.intersects(siteInterval))
                    {
                        siteData = new SiteData(Location.GENE, gene.getStrand(), Math.abs(siteInterval.getFrom() - gene.getStart()));
                        Object exonsObj = gene.getProperties().getValue("exons");
                        if(exonsObj != null)
                        {
                            // TODO: intron-relative coordinates
                            siteData = new SiteData(Location.INTRON, gene.getStrand(), Math.abs(siteInterval.getFrom() - gene.getStart()));
                            for( String blockStr : TextUtil.split( exonsObj.toString(), ';' ) )
                            {
                                Interval interval;
                                try
                                {
                                    interval = new Interval(blockStr);
                                }
                                catch( IllegalArgumentException e )
                                {
                                    continue;
                                }
                                interval = interval.translateFromSite(gene);
                                if(interval.intersects(siteInterval))
                                {
                                    siteData = new SiteData(Location.EXON, gene.getStrand(),
                                            Math.abs(siteInterval.getFrom() - gene.getStart()));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if(siteData != null)
            {
                if( results == null )
                    results = new ArrayList<>();
                for(SiteIndex siteIndex: entry.getValue())
                {
                    while(results.size() <= siteIndex.index) results.add(new ArrayList<SiteData>());
                    List<SiteData> siteList = results.get(siteIndex.index);
                    siteList.add(siteData);
                }
            }
        }
        
        if(results == null && parameters.isAllGenes())
            results = Collections.emptyList();
            
        if(results != null)
        {
            Object idObj = gene.getProperties().getValue("id");
            if(idObj == null) return true;
            String id = idObj.toString();
            Object symbol = gene.getProperties().getValue("symbol");
            if(symbol == null) symbol = id;
            Object[] values = new Object[resTable.getColumnModel().getColumnCount()];
            values[0] = symbol;
            int col = 1;
            for(SiteAggregator resultType: parameters.getResultTypes())
            {
                for(int i=0; i<parameters.getSourcePaths().size(); i++)
                {
                    values[col] = resultType.aggregate(gene, fivePrimeFlankSize, threePrimeFlankSize, results.size() <= i? null:results.get(i));
                    col++;
                }
            }
            RowDataElement rde = new RowDataElement(id, resTable);
            rde.setValues(values);
            try
            {
                resTable.addRow(rde);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return true;
    }
}
