package ru.biosoft.bsa.analysis.maos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.IntervalMap;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;

public class TSSCollection
{
    public final int upstream;
    public final int downstream;
    private List<TSS> tssList = new ArrayList<>();
    private IntervalMap<TSS> byPromoter = new IntervalMap<>();
    
    public TSSCollection(int upstream, int downstream)
    {
        this.upstream = upstream;
        this.downstream = downstream;
    }

    public void add(TSS tss)
    {
        tssList.add( tss );
        Interval promoter = tss.getPromoter( upstream, downstream );
        byPromoter.add( promoter.getFrom(), promoter.getTo(), tss );
    }
    
    public List<TSS> getAll()
    {
        return tssList;
    }
    
    public Collection<TSS> getOverlapping(int from, int to)
    {
        return byPromoter.getIntervals( from, to );
    }
    
    public TSSCollection subset(Interval interval)
    {
        TSSCollection result = new TSSCollection( upstream, downstream );
        for(TSS tss : getOverlapping( interval.getFrom(), interval.getTo() ))
            result.add( tss );
        return result;
    }
    
    public TSSCollection translateTo(Interval interval, int start)
    {
        TSSCollection result = new TSSCollection( upstream, downstream );
        for(TSS tss : tssList)
        {
            TSS relTSS = tss.translateToInterval( interval, start );
            result.add( relTSS );
        }
        return result;
    }
    
    public TSSCollection getReverseComplement(Interval interval, int start)
    {
        TSSCollection result = new TSSCollection( upstream, downstream );
        for(TSS tss : tssList)
        {
            TSS rc = tss.getReverseComplement( interval, start );
            result.add( rc );
        }
        return result;
    }
    
    public static TSSCollection loadPromoters(Parameters parameters, DataElementPath chrPath)
    {
        TSSCollection result = new TSSCollection(parameters.getTargetGeneTSSUpstream(), parameters.getTargetGeneTSSDownstream()-1);// -1 to count first transcribed nucleotide as downstream
        DataElementPath ensemblPath = parameters.getEnsemblPath();
        if(ensemblPath.exists())
        {
            Track transcriptsTrack = ensemblPath.getChildPath( "Tracks", "Transcripts" ).getDataElement( Track.class );
            Sequence chrSeq = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
            DataCollection<Site> transcriptSites = transcriptsTrack.getSites( chrPath.toString(), chrSeq.getStart(), chrSeq.getStart() + chrSeq.getLength() );
            for(Site transcriptSite : transcriptSites)
            {
                String transcriptId = (String)transcriptSite.getProperties().getProperty( "id" ).getValue();
                int tssPos = transcriptSite.getStart();
                TSS tss = new TSS( tssPos, transcriptSite.getStrand() == StrandType.STRAND_PLUS, transcriptId, null);
                result.add( tss );
            }

        }
        result.addGeneSymbol();
        return result;
    }
    
    private void addGeneSymbol()
    {
        String[] transcriptIds = new String[tssList.size()];
        for(int i = 0; i < tssList.size(); i++)
            transcriptIds[i] = tssList.get( i ).transcriptId;
        Properties inputProps = new Properties();
        inputProps.setProperty( BioHub.TYPE_PROPERTY, "Transcripts: Ensembl" );
        inputProps.setProperty( BioHub.SPECIES_PROPERTY, "Homo sapiens" );
        Properties outputProps = new Properties();
        outputProps.setProperty( BioHub.TYPE_PROPERTY, "Genes: Gene symbol" );
        outputProps.setProperty( BioHub.SPECIES_PROPERTY, "Homo sapiens" );
        Map<String, String[]> transcript2GeneSymbol = BioHubRegistry.getReferences( transcriptIds, inputProps, outputProps, null );
        for(TSS tss : tssList)
        {
            String[] symbols = transcript2GeneSymbol.get( tss.transcriptId );
            if(symbols != null && symbols.length > 0)
                tss.geneSymbol = symbols[0];
        }
    }
}
