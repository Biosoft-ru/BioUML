package biouml.plugins.ensembl.analysis;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.SiteIntervalsMap;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.StaticDescriptor;
import biouml.plugins.ensembl.JavaScriptEnsembl;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.JobControl;

/**
 * @author lan
 *
 */
@ClassIcon("resources/annotate-track-with-genes.gif")
public class AnnotateTrackWithGenes extends AnalysisMethodSupport<AnnotateTrackWithGenesParameters>
{
    private static final PropertyDescriptor GENES_DESCRIPTOR = StaticDescriptor.create("Genes");
    private static final PropertyDescriptor IDS_DESCRIPTOR = StaticDescriptor.create( "id" );
    private static final PropertyDescriptor NAMES_DESCRIPTOR = StaticDescriptor.create( "gene" );

    public AnnotateTrackWithGenes(DataCollection<?> origin, String name)
    {
        super(origin, name, JavaScriptEnsembl.class, new AnnotateTrackWithGenesParameters());
    }

    @Override
    public Track justAnalyzeAndPut() throws Exception
    {
        DataElementPath ensemblPath = TrackUtils.getEnsemblPath(parameters.getSpecies(), parameters.getOutputTrack());
        final DataElementPath sequencesPath = TrackUtils.getPrimarySequencesPath(ensemblPath);
        DataCollection<AnnotatedSequence> sequencesDC = sequencesPath.getDataCollection(AnnotatedSequence.class);
        DataElementPath geneTrackPath = ensemblPath.getChildPath("Tracks", "Genes");
        final Track geneTrack = geneTrackPath.getDataElement(Track.class);
        log.info("Reading genes...");
        jobControl.pushProgress(0, 30);
        final Map<String, SiteIntervalsMap> genesMap = new HashMap<>();
        jobControl.forCollection(sequencesDC.getNameList(), sequenceName -> {
            DataElementPath fullSequencePath = sequencesPath.getChildPath(sequenceName);
            try
            {
                Sequence sequence = fullSequencePath.getDataElement(AnnotatedSequence.class).getSequence();
                DataCollection<Site> genes = geneTrack.getSites( fullSequencePath.toString(), sequence.getStart(), sequence.getLength() );
                if( genes != null && genes.getSize() > 0 )
                    genesMap.put( sequenceName, new SiteIntervalsMap( genes ) );
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, ExceptionRegistry.log( e ) );
                return true;
            }
            return true;
        });
        jobControl.popProgress();
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
        jobControl.pushProgress(30, 100);
        log.info("Initializing resulting track...");
        Track input = parameters.getInputTrack().getDataElement(Track.class);
        final SqlTrack result = SqlTrack.createTrack(parameters.getOutputTrack(), input, input.getClass());
        final int fivePrimeFlankSize = Math.abs(parameters.getFrom());
        final int threePrimeFlankSize = Math.abs(parameters.getTo());
        final int maxAddition = Math.max(fivePrimeFlankSize, threePrimeFlankSize);
        log.info("Annotating...");
        jobControl.forCollection(DataCollectionUtils.asCollection(input.getAllSites(), Site.class),
                element -> {
                    List<String> names = new ArrayList<>();
                    List<String> ids = new ArrayList<>();
                    List<String> annotations = new ArrayList<>();
                    try
                    {
                        SiteIntervalsMap chromosomeGenes = genesMap.get(element.getOriginalSequence().getName());
                        if(chromosomeGenes != null)
                        {
                            Collection<Site> sites = chromosomeGenes.getIntervals(element.getFrom()-maxAddition, element.getTo()+maxAddition);
                            for(Site gene: sites)
                            {
                                int fivePrime = 0, threePrime = 0, exons = 0, introns = 0;
                                Interval fivePrimeInterval = (new Interval(-fivePrimeFlankSize,-1)).translateFromSite(gene);
                                Interval threePrimeInterval = (new Interval(gene.getLength(), gene.getLength()+threePrimeFlankSize-1)).translateFromSite(gene);
                                Interval geneInterval = new Interval(gene.getFrom(), gene.getTo());
                                Interval siteInterval = new Interval(element.getFrom(), element.getTo());
                                if(fivePrimeFlankSize > 0 && fivePrimeInterval.intersects(siteInterval))
                                    fivePrime++;
                                if(geneInterval.intersects(siteInterval))
                                {
                                    Object exonsObj = gene.getProperties().getValue("exons");
                                    if(exonsObj != null)
                                    {
                                        Interval prevExon = null;
                                        for( String blockStr : TextUtil.split( exonsObj.toString(), ';' ) )
                                        {
                                            Interval exon;
                                            try
                                            {
                                                exon = new Interval(blockStr);
                                            }
                                            catch( Exception e1 )
                                            {
                                                continue;
                                            }
                                            if(prevExon != null)
                                            {
                                                Interval intron = new Interval(prevExon.getTo()+1, exon.getFrom()-1).translateFromSite(gene);
                                                if(intron.intersects(siteInterval))
                                                    introns++;
                                            }
                                            prevExon = exon;
                                            exon = exon.translateFromSite(gene);
                                            if(exon.intersects(siteInterval))
                                                exons++;
                                        }
                                    }
                                }
                                if(threePrimeFlankSize > 0 && threePrimeInterval.intersects(siteInterval))
                                    threePrime++;
                                StringBuilder locations = new StringBuilder();
                                if(fivePrime > 0) locations.append("5' + ");
                                if(exons > 1) locations.append(exons+" exons + ");
                                else if(exons == 1) locations.append("exon + ");
                                if(introns > 1) locations.append(introns+" introns + ");
                                else if(introns == 1) locations.append("intron + ");
                                if(threePrime > 0) locations.append("3' + ");
                                if(locations.length() > 0)
                                {
                                    locations.delete(locations.length()-3, locations.length());
                                    annotations.add(gene.getProperties().getValue("symbol")+" ("+locations+")");
                                }
                                String id = gene.getProperties().getValueAsString( "id" );
                                if( id != null )
                                {
                                    String name = gene.getProperties().getValueAsString( "Name" );
                                    if( name == null || name.isEmpty() )
                                        name = id;
                                    ids.add( id );
                                    names.add( name );
                                }
                            }
                        }
                    }
                    catch( Exception e2 )
                    {
                        log.log( Level.SEVERE, "While adding site " + element.getName() + ": " + e2.getMessage() );
                    }
                    try
                    {
                        boolean noAnnotation = annotations.isEmpty();
                        boolean noId = ids.isEmpty();
                        if( noAnnotation && noId )
                            result.addSite(element);
                        else
                        {
                            Site newSite = new SiteImpl(result, null, element.getType(), element.getBasis(), element.getStart(),
                                    element.getLength(), element.getPrecision(), element.getStrand(), element.getOriginalSequence(), element
                                            .getComment(), (DynamicPropertySet)element.getProperties().clone());

                            if( !noAnnotation )
                            {
                                Collections.sort( annotations );
                                newSite.getProperties()
                                        .add( new DynamicProperty( GENES_DESCRIPTOR, String.class, String.join( ", ", annotations ) ) );
                            }
                            if( !noId )
                            {
                                newSite.getProperties()
                                        .add( new DynamicProperty( IDS_DESCRIPTOR, String.class, String.join( ", ", ids ) ) );
                                newSite.getProperties()
                                        .add( new DynamicProperty( NAMES_DESCRIPTOR, String.class, String.join( ", ", names ) ) );
                            }

                            result.addSite(newSite);
                        }
                    }
                    catch( Exception e3 )
                    {
                        log.log( Level.SEVERE, "While adding site " + element.getName() + ": " + e3.getMessage() );
                    }
                    return true;
                });
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
        {
            parameters.getOutputTrack().remove();
            return null;
        }
        result.finalizeAddition();
        log.info("Track created ("+result.getAllSites().getSize()+" sites)");
        jobControl.popProgress();
        return result;
    }
}
