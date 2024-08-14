package biouml.plugins.gtrd.master.analyses.metacluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import biouml.plugins.gtrd.ATACExperiment;
import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.FAIREExperiment;
import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.MNaseExperiment;
import biouml.plugins.gtrd.access.BigBedFiles;
import biouml.plugins.gtrd.access.BigBedFiles.VersionParseResult;
import biouml.plugins.gtrd.master.MasterTrack;
import biouml.plugins.gtrd.master.analyses.ExportMasterTrack;
import biouml.plugins.gtrd.master.index.OverlapUtils;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.GenomeLocation;
import biouml.plugins.gtrd.master.sites.HistoryEntry;
import biouml.plugins.gtrd.master.sites.MasterSite;
import biouml.plugins.gtrd.master.sites.MasterSite.Status;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.chipseq.GEMPeak;
import biouml.plugins.gtrd.master.sites.chipseq.MACS2ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.chipseq.PICSPeak;
import biouml.plugins.gtrd.master.sites.chipseq.SISSRSPeak;
import biouml.standard.type.Species;
import gnu.trove.list.array.TDoubleArrayList;
import one.util.streamex.EntryStream;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.bsa.track.big.BigWigTrack;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class UpdateMasterTrack extends AnalysisMethodSupport<UpdateMasterTrack.Parameters>
{
    private Metadata metadata;
    
    public UpdateMasterTrack(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        MasterTrack old = null;
        if(parameters.getMasterTrackPath() != null)
            old = parameters.getMasterTrackPath().getDataElement( MasterTrack.class );
        
        if(parameters.getNewMetadataPath() != null)
        {
            log.info( "Loading metadata from " + parameters.getNewMetadataPath() );
            metadata = parameters.getNewMetadataPath().getDataElement( Metadata.class );
            log.info( "Done loading metadata" );
        }
        else
        {
            if(old == null)
                throw new Exception("Either masterTrack or newMetadataPath should be specified");
            log.info( "Using metadata from " + parameters.getMasterTrackPath() );
            metadata = old.getMetadata();
        }
        metadata = new Metadata(metadata);
        metadata.buildInfo.clearAnnotation();
        
        
        DataElementPath seqBase;
        if(old != null)
        {
            seqBase = DataElementPath.create( old.getInfo().getProperty( Track.SEQUENCES_COLLECTION_PROPERTY ) );
        }
        else
        {
            Species organism = Species.getSpecies( metadata.tf.organism ); 
            seqBase = EnsemblDatabaseSelector.getDefaultEnsembl( organism ).getPrimarySequencesPath();
        }
        MasterTrack result = ExportMasterTrack.createMasterTrack( parameters.getResultingMasterTrack(), seqBase );
        
        Map<String, List<MasterSite>> masterSites;
        jobControl.pushProgress( 0, 30 );
        if( parameters.isRecomputeClusters() )
        {
            log.info( "Computing master sites" );
            metadata.buildInfo.chipSeqPeaksByPeakCaller.clear();
            masterSites = mkMetaClusters();
            sortByFrom( masterSites );
            log.info( "Done computing master sites" );
        }
        else
        {
            if(old == null)
                throw new Exception("Master track should be specified when recomputeClusters=false");
            log.info( "Will not recompute master sites, reading old" );
            masterSites = readMasterSites( old );
            log.info( "Done reading old master sites" );
        }
        jobControl.popProgress();
        
        if( parameters.isAddAnnotation() )
        {
            jobControl.pushProgress( 30, 60 );
            log.info( "Adding annotation to master sites" );
            annotateMasterSites( masterSites );
            log.info( "Done adding annotation to master sites" );
            jobControl.popProgress();
        }

        if( parameters.isRemapIdentifiers() )
        {
            if(old == null)
                throw new Exception("masterTrack shoudl be specied when remapIdentifiers=true");
            jobControl.pushProgress( 60, 80 );
            log.info( "Mapping master sites to stable identifiers" );
            remapIdentifiers( masterSites, old, result );
            log.info( "Done mapping master sites to stable identifiers" );
            jobControl.popProgress();
        } else
        {
            assignNewIdentifiers(masterSites);
        }

        jobControl.pushProgress( 80, 100 );
        log.info("Writing master track");
        Map<String, Integer> chromSizes;
        if(old != null)
            chromSizes = old.getChromSizes();
        else
        {
            chromSizes = new HashMap<>();
            for(AnnotatedSequence chr : seqBase.getDataCollection( AnnotatedSequence.class ))
                chromSizes.put(chr.getName(), chr.getSequence().getLength());
        }
        saveMasterTrack( result, masterSites, chromSizes );
        log.info("Done writing master track");
        jobControl.popProgress();
        
        return result;
    }

    private Map<String, List<ChIPSeqPeak>> filterUnmappableRegions(Map<String, List<ChIPSeqPeak>> peaks) throws IOException
    {
        Map<String, List<ChIPSeqPeak>> result = new HashMap<>();
        
        Map<String, Integer> readLengthTable = new HashMap<>();
        for(RowDataElement row : parameters.getReadLengthTable().getDataElement( TableDataCollection.class ))
        {
            String readsId = row.getName();
            int readLength = (Integer)row.getValues()[0];
            readLengthTable.put( readsId, readLength );
        }
        
        BigWigTrack mappabilityTrack = parameters.getMappabilityWig().getDataElement( BigWigTrack.class );
        Set<ChIPseqExperiment> unknownReadLength = new HashSet<>();
        
        int peaksRemoved = 0;
        int totalPeaks = 0;
        
        for(String chr : peaks.keySet())
        {
            float[] mappabilityProfile = mappabilityTrack.loadProfile( chr );
            for(ChIPSeqPeak peak : peaks.get( chr ))
            {
                totalPeaks++;
                int minReadLength = Integer.MAX_VALUE;
                for(String readsId : peak.getExp().getReadsIds())
                {
                    if(readLengthTable.containsKey( readsId ))
                    {
                        int readLength = readLengthTable.get( readsId );
                        if(readLength < minReadLength)
                            minReadLength = readLength;
                    }
                }
                if(minReadLength == Integer.MAX_VALUE)
                    unknownReadLength.add( peak.getExp() );
                else
                {
                    int unmappablePositions = 0;
                    int center = peak.getFrom() + peak.getSummit();
                    for( int x = Math.max( 1, center - 500); x <= center + 500 && x - 1 < mappabilityProfile.length; x++ )
                    {
                        int l = (int)mappabilityProfile[x - 1];//length of read to be uniquely mappable at this position
                        if( l > minReadLength )
                            unmappablePositions++;
                    }

                    if( unmappablePositions >= parameters.getMaxUnmappablePositions() )
                    {
                        peaksRemoved++;
                        continue;
                    }
                }
                result.computeIfAbsent( chr, k->new ArrayList<>() ).add( peak );
            }
        }
        
        if(!unknownReadLength.isEmpty())
        {
            log.warning( unknownReadLength.size() + " experiments with unknown read length");
        }
        log.info( "Remove " + peaksRemoved + " of " + totalPeaks + " in unmappable regions" );
        
        return result;
    }

    private void assignNewIdentifiers(Map<String, List<MasterSite>> masterSites)
    {
        int nextId = 1;
        for(List<MasterSite> chrList : masterSites.values())
            for(MasterSite ms : chrList)
            {
                ms.setId( nextId++ );
                ms.setVersion( 1 );
            }
    }

    public void saveMasterTrack(MasterTrack result, Map<String, List<MasterSite>> metaClusters, Map<String, Integer> chromSizes) throws IOException
    {
        List<MasterSite> allMetaClusters = metaClusters.values().stream().flatMap( l->l.stream() ).collect( Collectors.toList() );
        Collections.sort( allMetaClusters, GenomeLocation.ORDER_BY_LOCATION );
        result.writeMetadata( metadata );
        result.write( allMetaClusters, chromSizes );
        parameters.getResultingMasterTrack().save( result );
    }
    
    private void remapIdentifiers(Map<String, List<MasterSite>> allNewClusters, MasterTrack oldMT, MasterTrack newMT) throws IOException
    {
        log.info( "Reading old metatrack" );
        Map<String, List<MasterSite>> allOldClusters = readMasterSites( oldMT );
        log.info( "Done reading old metatrack" );
        
        int nextId = 1;
        for(List<MasterSite> l : allOldClusters.values())
            for(MasterSite ms : l)
                if(nextId < ms.getId())
                    nextId = ms.getId();
        nextId++;
        
        
        for( String chr : allNewClusters.keySet() )
        {
            List<MasterSite> newClusters = allNewClusters.get( chr );//assume sorted by from
            
            if(newClusters.stream().anyMatch( ms->ms.getStatus() == Status.RETIRED ))
            {
                log.warning( "New master track contains RETIRED sites, all of them will be removed" );
                newClusters = newClusters.stream().filter( ms->ms.getStatus() != Status.RETIRED ).collect( Collectors.toList() );
            }
            
            for( MasterSite ms : newClusters )
                ms.setId( -1 );

            List<MasterSite> oldClusters = allOldClusters.get( chr );
            if( oldClusters != null )
            {
                Map<MasterSite,MasterSite> old2New = new HashMap<>();
                Collections.sort( oldClusters, Comparator.comparing( ms->ms.getTo() ) );
                OverlapUtils.mapOverlapping( newClusters, oldClusters, 50, newSite->Collectors.<MasterSite>minBy( Comparator.comparing( oldSite->distance(oldSite,newSite) ) ),
                        (ms, optClosest) -> {
                    if( optClosest.isPresent() )
                    {
                        MasterSite closest = optClosest.get();
                        int distance = distance(ms, closest);
                        
                        MasterSite prevNew = old2New.get( closest );
                        if(prevNew == null || distance(prevNew, closest) > distance)
                            old2New.put( closest, ms );
                    }
                } );

                
                old2New.forEach( (o,n) -> {
                    n.setId( o.getId() );
                    n.setVersion( o.getVersion() );
                    //increment version if coordinates have changed
                    if(n.getFrom() != o.getFrom() || n.getTo() != o.getTo())
                    {
                        n.setVersion( o.getVersion() + 1 );
                        HistoryEntry history = new HistoryEntry();
                        history.setVersion( o.getVersion() );
                        history.setFrom( o.getFrom() );
                        history.setTo( o.getTo() );
                        history.setRelease( oldMT.getMetadata().getVersion() );
                        n.getHistory().add( history );
                    }
                } );
                
              //add old sites that absent in new track as retired
                for(MasterSite oldCluster : oldClusters)
                    if(!old2New.containsKey( oldCluster ))
                    {
                        MasterSite retired = new MasterSite();
                        retired.setId( oldCluster.getId() );
                        retired.setVersion( oldCluster.getVersion() );
                        retired.setStatus( Status.RETIRED );
                        retired.setOrigin( newMT );
                        retired.setChr( oldCluster.getChr() );
                        retired.setFrom( oldCluster.getFrom() );
                        retired.setTo( oldCluster.getTo() );
                        retired.setSummit( oldCluster.getSummit() );
                        retired.setReliabilityLevel( oldCluster.getReliabilityLevel() );
                        retired.setReliabilityScore( oldCluster.getReliabilityScore() );
                        if(oldCluster.getStatus() != Status.RETIRED)//become retired
                        {
                            HistoryEntry history = new HistoryEntry();
                            history.setVersion( retired.getVersion() );
                            history.setFrom( retired.getFrom() );
                            history.setTo( retired.getTo() );
                            history.setRelease( oldMT.getMetadata().getVersion() );
                            retired.getHistory().add( history );
                        }
                        newClusters.add( retired );
                    }
            }
            
            //Assign new identifiers to new sites
            for(MasterSite ms : newClusters)
                if(ms.getId() == -1)
                {
                    ms.setId( nextId++ );
                    ms.setVersion( 1 );
                }
        }
        
        metadata.setVersion(oldMT.getMetadata().getVersion() + 1);
    }

    private static Map<String, List<MasterSite>> readMasterSites(MasterTrack masterTrack) throws IOException
    {
        Map<String, List<MasterSite>> result = new HashMap<>();
        for(String chr : masterTrack.getChromosomes())
            result.put(chr, masterTrack.query( chr ));
        return result;
    }
    
    private static int distance(MasterSite a, MasterSite b)
    {
        return Math.abs( a.getFrom() + a.getSummit() - b.getFrom() - b.getSummit() );
    }
    

    private void annotateMasterSites(Map<String, List<MasterSite>> masterSites) throws IOException
    {
        if( parameters.getChipExoPeaks() != null )
        {
            log.info( "Adding ChIP-exo peaks" );
            int n = 0;
            for( ChIPexoExperiment exp : metadata.chipExoExperiments.values() )
            {
                for( String peakCaller : exp.getPeakCallers() )
                {
                    String peaksId = exp.getPeakId();
                    DataElementPath path = parameters.getChipExoPeaks().getChildPath( peakCaller.toUpperCase(), peaksId + ".bb" );
                    if( !path.exists() )
                        continue;
                    n += addAnnotation( masterSites, path, ms -> ms.getChipExoPeaks() );
                    metadata.buildInfo.addChipExoPeaks( peakCaller, exp.getPeakId() );
                }
            }
            log.info( "Added " + n + " ChIP-exo peaks" );
        }
        
        if( parameters.getDnaseSeqPeaks() != null )
        {
            log.info( "Adding DNase open chromatin peaks" );
            int n = 0;
            for( DNaseExperiment exp : metadata.dnaseExperiments.values() )
            {
                for( String peakCaller : DNaseExperiment.OPEN_CHROMATIN_PEAK_CALLERS )
                {
                    for( String peakRep : exp.getPeakRepIds() )
                    {
                        DataElementPath path = parameters.getDnaseSeqPeaks().getChildPath( peakCaller.toUpperCase(), peakRep + ".bb" );
                        if( !path.exists() )
                            continue;
                        n += addAnnotation( masterSites, path, ms -> ms.getDnasePeaks() );
                        metadata.buildInfo.addDNasePeaks( peakCaller, peakRep );
                    }
                }
            }
            log.info( "Added " + n + " DNase peaks" );
        }
        
        if( parameters.getDnaseClusters() != null )
        {
            log.info( "Adding DNase open chromatin clusters" );
            int n = 0;
            for( String peakCaller : DNaseExperiment.OPEN_CHROMATIN_PEAK_CALLERS )
            {
                //DNase-seq_from_cell_id_2865_MACS2.v31.bb
                DataElementPath folder = parameters.getDnaseClusters().getChildPath( peakCaller.toUpperCase() );
                if(!folder.exists())
                    continue;
                Map<String, VersionParseResult> latestFiles = BigBedFiles.findLatestVersions( folder.getDataCollection().getNameList() );

                for( CellLine cell : metadata.cells.values() )
                {
                    String base = "DNase-seq_from_cell_id_" + cell.getName() + "_" + peakCaller.toUpperCase();
                    VersionParseResult latest = latestFiles.get( base );
                    if(latest == null)
                        continue;
                    DataElementPath path = parameters.getDnaseClusters().getChildPath( peakCaller.toUpperCase(), latest.name);
                    n += addAnnotation( masterSites, path, ms -> ms.getDnaseClusters() );
                    metadata.buildInfo.addDNaseClusters( peakCaller, cell.getName() + ".v" + latest.version );
                }
            }
            log.info( "Added " + n + " DNase open chromatin clusters" );
        }
        
        if(parameters.getFootprints() != null)
        {
            int n = 0;
            log.info( "Adding DNase footprints" );
            for(DNaseExperiment exp : metadata.dnaseExperiments.values())
            {
                for(String peakCaller : DNaseExperiment.FOOTPRINT_PEAK_CALLERS )
                {
                    for(String peakRep : exp.getPeakRepIds())
                    {
                        String folder = peakCaller.replaceAll( "^wellington_", "" ).toUpperCase();
                        DataElementPath path = parameters.getFootprints().getChildPath( folder, peakRep + ".bb" );
                        if(!path.exists())
                            continue;
                        n += addAnnotation( masterSites, path, ms->ms.getDnaseFootprints() );
                        metadata.buildInfo.addFootprints( peakCaller, peakRep );
                    }
                }
            }
            log.info( "Added " + n + " DNase footprints" );
        }
        
        if(parameters.getFootprintClusters() != null)
        {
            int n = 0;
            log.info( "Adding DNase footprint clusters" );
            for( String peakCaller : DNaseExperiment.FOOTPRINT_PEAK_CALLERS )
            {
                //DNase-seq_from_cell_id_2865_WELLINGTON_MACS2.v31.bb
                DataElementPath folder = parameters.getFootprintClusters().getChildPath( peakCaller.toUpperCase() );
                if(!folder.exists())
                    continue;
                Map<String, VersionParseResult> latestFiles = BigBedFiles.findLatestVersions( folder.getDataCollection().getNameList() );

                for( CellLine cell : metadata.cells.values() )
                {
                    String base = "DNase-seq_from_cell_id_" + cell.getName() + "_" + peakCaller.toUpperCase();
                    VersionParseResult latest = latestFiles.get( base );
                    if(latest == null)
                        continue;
                    
                    DataElementPath path = parameters.getFootprintClusters().getChildPath( peakCaller.toUpperCase(), latest.name );
                    if(!path.exists())
                        continue;
                    n += addAnnotation( masterSites, path, ms->ms.getFootprintClusters());
                    metadata.buildInfo.addFootprintClusters( peakCaller, cell.getName() + ".v" + latest.version );
                    
                }
            }
            log.info( "Added " + n + " DNase footprint clusters" );
        }
        
        if( parameters.getAtacClusters() != null )
        {
            log.info( "Adding ATAC-seq open chromatin clusters" );
            int n = 0;
            for( String peakCaller : ATACExperiment.PEAK_CALLERS )
            {
                DataElementPath folder = parameters.getAtacClusters().getChildPath( peakCaller.toUpperCase() );
                if(!folder.exists())
                    continue;
                Map<String, VersionParseResult> latestFiles = BigBedFiles.findLatestVersions( folder.getDataCollection().getNameList() );

                for( CellLine cell : metadata.cells.values() )
                {
                    //ATAC-seq_from_cell_id_2865_MACS2.v1.bb
                    String base = "ATAC-seq_from_cell_id_" + cell.getName() + "_" + peakCaller.toUpperCase();
                    VersionParseResult latest = latestFiles.get( base );
                    if(latest == null)
                        continue;
                    
                    DataElementPath path = parameters.getAtacClusters().getChildPath( peakCaller.toUpperCase(), latest.name );
                    if( !path.exists() )
                        continue;
                    n += addAnnotation( masterSites, path, ms -> ms.getAtacClusters() );
                    metadata.buildInfo.addAtacClusters( peakCaller, cell.getName() + ".v" + latest.version );
                }
            }
            log.info( "Added " + n + " ATAC-seq clusters" );
        }
        
        if( parameters.getFaireClusters() != null )
        {
            log.info( "Adding FAIRE-seq open chromatin clusters" );
            int n = 0;
            
            for( String peakCaller : FAIREExperiment.PEAK_CALLERS )
            {
                DataElementPath folder = parameters.getFaireClusters().getChildPath( peakCaller.toUpperCase() );
                if(!folder.exists())
                    continue;
                Map<String, VersionParseResult> latestFiles = BigBedFiles.findLatestVersions( folder.getDataCollection().getNameList() );

                for( CellLine cell : metadata.cells.values() )
                {
                    //FAIRE-seq_from_cell_id_2865_MACS2.v1.bb
                    
                    String base = "FAIRE-seq_from_cell_id_" + cell.getName() + "_" + peakCaller.toUpperCase();
                    VersionParseResult latest = latestFiles.get( base );
                    if(latest == null)
                        continue;
                    DataElementPath path = parameters.getFaireClusters().getChildPath( peakCaller.toUpperCase(), latest.name );
                    if( !path.exists() )
                        continue;
                    n += addAnnotation( masterSites, path, ms -> ms.getFaireClusters() );
                    metadata.buildInfo.addFaireClusters( peakCaller, cell.getName() + ".v" + latest.version );
                }
            }
            log.info( "Added " + n + " FAIRE-seq clusters" );
        }
        
        if( parameters.getMnaseSeqPeaks() != null )
        {
            log.info( "Adding MNase-seq peaks" );
            int n = 0;
            for( MNaseExperiment exp : metadata.mnaseExperiments.values() )
            {
                for( String peakCaller : exp.getPeakCallers() )
                {
                    String peaksId = exp.getPeakId();
                    DataElementPath path = parameters.getMnaseSeqPeaks().getChildPath( peakCaller.toUpperCase(), peaksId + ".bb" );
                    if( !path.exists() )
                        continue;
                    n += addAnnotation( masterSites, path, ms -> ms.getMnasePeaks() );
                    metadata.buildInfo.addMNasePeaks( peakCaller, peaksId );
                }
            }
            log.info("Added " + n + " MNase-seq peaks");
        }
        
        if( parameters.getHistoneModificationPeaks() != null )
        {
            log.info( "Adding histone modification peaks" );
            int n = 0;
            for( HistonesExperiment exp : metadata.histoneExperiments.values() )
            {
                for( String peakCaller : exp.getPeakCallers() )
                {
                    String peaksId = exp.getPeakId();
                    DataElementPath path = parameters.getHistoneModificationPeaks().getChildPath( peakCaller.toUpperCase(), peaksId + ".bb" );
                    if( !path.exists() )
                        continue;
                    n += addAnnotation( masterSites, path, ms -> ms.getHistonesPeaks() );
                    metadata.buildInfo.addHistonePeaks( peakCaller, peaksId );
                }
            }
            log.info( "Added " + n + " histone modification peaks" );
        }
        
        if( parameters.getHistoneModificationClusters() != null )
        {
            log.info( "Adding histone modification clusters" );
            int n = 0;

            
            final Pattern NAME_PATTERN = Pattern.compile( "([^_]*)_ChIP-seq_HM_from_cell_id_([^_]*)_([^_.]*)([.]v[0-9]+)?.bb" );
            for( String peakCaller : HistonesExperiment.PEAK_CALLERS )
            {
                //H4K20me1_ChIP-seq_HM_from_cell_id_887_MACS2.v1.bb
                DataElementPath folder = parameters.getHistoneModificationClusters().getChildPath( peakCaller.toUpperCase() );
                if(!folder.exists())
                    continue;
                Map<String, VersionParseResult> latestFiles = BigBedFiles.findLatestVersions( folder.getDataCollection().getNameList() );
                
                
                for(VersionParseResult latest : latestFiles.values())
                {
                    
                    Matcher matcher = NAME_PATTERN.matcher( latest.name );
                    if(!matcher.matches())
                    {
                        log.warning( "Invalid track name: " + latest.name );
                        continue;
                    }
                    String modification = matcher.group(1);
                    String cellId = matcher.group(2);
                    if(!metadata.cells.containsKey( cellId ))//Add only experiments with matching cell
                        continue;
                    DataElementPath trackPath = folder.getChildPath( latest.name );
                    if(!matcher.group(3).equalsIgnoreCase( peakCaller) )
                    {
                        log.warning( "Wrong peak caller for: " + trackPath );
                        continue;
                    }
                    
                    n += addAnnotation( masterSites, trackPath, ms -> ms.getHistonesClusters() );
                    metadata.buildInfo.addHistoneClusters( peakCaller, modification, cellId, latest.version );
                }
            }
            log.info("Added " + n + " histone modification clusters");
        }

        if(parameters.getMotifsFolder() != null)
        {
            log.info("Adding motifs");
            int n = 0;
            for(DataElementPath siteModel : metadata.siteModels)
            {
                DataElementPath path = parameters.getMotifsFolder().getChildPath( siteModel.getName() + ".bb" );
                if(!path.exists())
                    continue;
                n += addAnnotation(masterSites, path, ms->ms.getMotifs());
                metadata.buildInfo.motifs.add(siteModel.getName());
            }
            log.info("Added " + n + " motifs");
        }
    }
    
    /**
     * @return number of added annotations
     */
    private <T extends Site> int addAnnotation(Map<String, List<MasterSite>> masterSites,
            DataElementPath annotationPath,
            Function<MasterSite, List<T>> targetListExtractor) throws IOException
    {
        AtomicInteger addedAnnotations = new AtomicInteger();
        BigBedTrack<T> annotationTrack = annotationPath.getDataElement(BigBedTrack.class);
        for(String chr : masterSites.keySet())
        {
            List<MasterSite> masterSitesOnChr = masterSites.get( chr );//assume sorted
            List<T> annotationOnChr = annotationTrack.query( chr );
            Collections.sort( annotationOnChr, Comparator.comparing( site->site.getFrom() ) );
            OverlapUtils.mapOverlapping( masterSitesOnChr, annotationOnChr, 0, (masterSite, annotationSite)->{
                List<T> targetList = targetListExtractor.apply(masterSite);
                targetList.add(annotationSite);
                addedAnnotations.incrementAndGet();
            } );
        }
        return addedAnnotations.intValue();
    }

    private Map<String, List<MasterSite>> mkMetaClusters() throws IOException
    {
        Map<String, List<Cluster>> clusters = new HashMap<>();
        for( String peakCaller : ChIPseqExperiment.PEAK_CALLERS )
        {
            Map<String, List<Cluster>> callerClusters = mkClusters( peakCaller );
            for( String chr : callerClusters.keySet() )
            {
                List<Cluster> allChrClusters = clusters.get( chr );
                if( allChrClusters == null )
                    clusters.put( chr, allChrClusters = new ArrayList<>() );
                allChrClusters.addAll( callerClusters.get( chr ) );
            }
        }
        return mergeClusters( clusters );
    }

    private Map<String, List<MasterSite>> mergeClusters(Map<String, List<Cluster>> clusters)
    {
        jobControl.pushProgress( 0, 20 );
        log.info( "Sorting peak caller specific clusters by centers" );
        sortByCenter( clusters );
        jobControl.popProgress();

        jobControl.pushProgress( 20, 50 );
        log.info( "Clustering centers" );
        Map<String, int[]> centers = fetchCenters( clusters );
        Map<String, Interval[]> groups = getClusters( centers );
        jobControl.popProgress();

        jobControl.pushProgress( 50, 95 );
        log.info( "Computing meta clusters" );
        Map<String, List<MasterSite>> result = computeMetaClusters( clusters, groups );
        jobControl.popProgress();
        
        jobControl.pushProgress(95, 100);
        if(parameters.isExcludeMetaClustersWithoutControl())
        {
            log.info( "Removing metaclusters supported by noctrl experiments only" );
            result = filterMetaclustersWithoutControl( result );
        }
        jobControl.popProgress();
        
        
        return result;
    }

    private static final Map<String, Integer> PEAK_CALLER_PRIORITY = new HashMap<>();
    static
    {
        PEAK_CALLER_PRIORITY.put( SISSRSPeak.PEAK_CALLER, 1 );
        PEAK_CALLER_PRIORITY.put( MACS2ChIPSeqPeak.PEAK_CALLER, 2 );
        PEAK_CALLER_PRIORITY.put( PICSPeak.PEAK_CALLER, 3 );
        PEAK_CALLER_PRIORITY.put( GEMPeak.PEAK_CALLER, 4 );
    }

    private Map<String, List<MasterSite>> computeMetaClusters(Map<String, List<Cluster>> clusters, Map<String, Interval[]> groups)
    {
        Map<String, List<MasterSite>> result = new HashMap<>();

        for( String chr : groups.keySet() )
        {
            Interval[] chrGroups = groups.get( chr );
            List<Cluster> chrClusters = clusters.get( chr );
            ArrayList<MasterSite> chrResult = new ArrayList<>();
            for( Interval group : chrGroups )
            {
                if(group.getLength() <= 1)
                    continue;//meta cluster should be supported by more then one peak caller
                Cluster bestCluster = chrClusters.get( group.getFrom() );
                int bestPrioroty = PEAK_CALLER_PRIORITY.get( bestCluster.getPeakCaller() );
                for( int i = group.getFrom() + 1; i <= group.getTo(); i++ )
                {
                    Cluster cluster = chrClusters.get( i );
                    int priority = PEAK_CALLER_PRIORITY.get( cluster.getPeakCaller() );
                    if( priority > bestPrioroty
                            || ( priority == bestPrioroty && cluster.getPeaks().size() > bestCluster.getPeaks().size() ) )
                    {
                        bestCluster = cluster;
                        bestPrioroty = priority;
                    }
                }

                MasterSite ms = new MasterSite();
                ms.setChr( bestCluster.getChr() );
                ms.setFrom( bestCluster.getFrom() );
                ms.setTo( bestCluster.getTo() );
                ms.setSummit( bestCluster.getSummit() );

                for( int i = group.getFrom(); i <= group.getTo(); i++ )
                {
                    List<ChIPSeqPeak> peaks = chrClusters.get( i ).getPeaks();
                    ms.getChipSeqPeaks().addAll( peaks );
                }
                chrResult.add( ms );
            }
            chrResult.trimToSize();
            result.put(chr, chrResult);
        }
        return result;
    }

    private Map<String, List<Cluster>> mkClusters(String peakCaller) throws IOException
    {
        jobControl.pushProgress( 0, 80 );
        log.info( "Loading ChIP-seq peaks" );
        Map<String, List<ChIPSeqPeak>> peaks = loadChIPSeqPeaks( peakCaller );
        
        if(parameters.isFilterUnmappable())
        {
            log.info( "Filtering unmappable" );
            peaks = filterUnmappableRegions(peaks);
        }
        jobControl.popProgress();

        jobControl.pushProgress( 80, 83 );
        log.info( "Sorting by peak centers" );
        sortByCenter( peaks );
        jobControl.popProgress();

        jobControl.pushProgress( 83, 85 );
        log.info( "Clustering site centers" );
        Map<String, int[]> allCenters = fetchCenters( peaks );
        Map<String, Interval[]> allClusters = getClusters( allCenters );
        jobControl.popProgress();

        jobControl.pushProgress( 85, 90 );
        log.info( "Estimating peak center standard deviation" );
        double sd = estimateGlobalSD( allCenters, allClusters );
        log.info( "Global SD = " + sd );
        jobControl.popProgress();

        jobControl.pushProgress( 90, 95 );
        log.info( "Computing clusters" );
        Map<String, List<Cluster>> result = computeClusters( allClusters, allCenters, sd, peaks );
        jobControl.popProgress();
        
        jobControl.pushProgress( 95, 100 );
        if(parameters.isExcludeClustersWithoutControl())
        {
            log.info( "Removing " + peakCaller + " clusters supported only by peaks without chip control." );
            result = filterClustersWithoutControl(result);
        }
        jobControl.popProgress();
        return result;
    }


    private Map<String, List<Cluster>> filterClustersWithoutControl(Map<String, List<Cluster>> clusters)
    {
        Map<String, List<Cluster>> result = new HashMap<>();
        for(String chr : clusters.keySet())
        {
            List<Cluster> filtered = new ArrayList<>();
            result.put(chr, filtered);
            for(Cluster c : clusters.get( chr ))
            {
                boolean hasCtrl = false;
                for(ChIPSeqPeak peak : c.getPeaks())
                {
                    if(peak.getExp().getControlId() != null)
                    {
                        hasCtrl = true;
                        break;
                    }
                }
                if(hasCtrl)
                    filtered.add( c );
            }
        }
        return result;
    }
    
    private Map<String, List<MasterSite>> filterMetaclustersWithoutControl(Map<String, List<MasterSite>> clusters)
    {
        Map<String, List<MasterSite>> result = new HashMap<>();
        for(String chr : clusters.keySet())
        {   
            List<MasterSite> filtered = new ArrayList<>();
            result.put(chr, filtered);
            for(MasterSite c : clusters.get( chr ))
            {
                boolean hasCtrl = false;
                for(ChIPSeqPeak peak : c.getChipSeqPeaks())
                {
                    if(peak.getExp().getControlId() != null)
                    {
                        hasCtrl = true;
                        break;
                    }
                }
                if(hasCtrl)
                    filtered.add( c );
            }
        }
        return result;
    }

    private Map<String, List<ChIPSeqPeak>> loadChIPSeqPeaks(String peakCaller) throws IOException
    {
        Map<String, List<ChIPSeqPeak>> result = new HashMap<>();

        int countAll = 0;
        int countWithCtrl = 0;
        int trackCount = 0;
        for( ChIPseqExperiment exp : metadata.chipSeqExperiments.values() )
        {
            if(exp.isControlExperiment())
                continue;
            
            countAll++;
            if(exp.getControlId() != null)
                countWithCtrl++;
            
            if(parameters.isExcludeExperimentsWithoutControl() && exp.getControlId() == null)
                continue;
            
            BigBedTrack<? extends ChIPSeqPeak> peaksTrack = parameters.getChipSeqPeaks().getChildPath( peakCaller.toUpperCase(), exp.getPeakId() + ".bb" )
                    .optDataElement( BigBedTrack.class );
            //Save information about used files somewhere
            if( peaksTrack == null )
                continue;

            trackCount++;
            metadata.buildInfo.addChipSeqPeaks( peakCaller, exp.getPeakId() );
            for( String chr : peaksTrack.getChromosomes() )
            {
                List<? extends ChIPSeqPeak> chrPeaks = peaksTrack.query( chr );
                result.computeIfAbsent( chr, x -> new ArrayList<>() ).addAll( chrPeaks );
            }
        }
        log.info( countWithCtrl + " experiments have control and " + (countAll-countWithCtrl) + " do not have control" );
        log.info( "Using " + trackCount  + " tracks");
        if(parameters.isExcludeExperimentsWithoutControl())
            log.info( "Using only experiments having control" );
        else
            log.info( "Using all experiments" );
        return result;
    }

    private static <T extends GenomeLocation> void sortByCenter(Map<String, List<T>> peaks)
    {
        sort(peaks, Comparator.comparing( x -> x.getFrom() + x.getSummit() ) );
    }
    
    private static <T extends GenomeLocation> void sortByFrom(Map<String, List<T>> peaks)
    {
        sort(peaks, Comparator.comparing( x -> x.getFrom() ) );
    } 
    
    private static <T extends GenomeLocation> void sort(Map<String, List<T>> peaks, Comparator<T> cmp)
    {
        for( List<T> l : peaks.values() )
            Collections.sort( l, cmp );
    }
    
    private static final int DEFAULT_SITEMODEL_WIDTH = 20;
    private Map<String, List<Cluster>> computeClusters(Map<String, Interval[]> allClusters, Map<String, int[]> allCenters, double globalSD,
            Map<String, List<ChIPSeqPeak>> allPeaks)
    {
        Map<String, List<Cluster>> result = new HashMap<>();

        int siteModelWidth = metadata.siteModels.stream().mapToInt( path -> path.getDataElement( SiteModel.class ).getLength() ).max()
                .orElse( DEFAULT_SITEMODEL_WIDTH );

        for( Map.Entry<String, Interval[]> e : allClusters.entrySet() )
        {
            String chr = e.getKey();
            Interval[] chrClusters = e.getValue();
            List<ChIPSeqPeak> peaks = allPeaks.get( chr );
            int[] centers = allCenters.get( chr );
            ArrayList<Cluster> chrResult = new ArrayList<>();
            for( Interval cluster : chrClusters )
            {
                double median;
                if( cluster.getLength() % 2 == 0 )
                    median = ( centers[cluster.getCenter()] + centers[cluster.getCenter() + 1] ) / 2.0;
                else
                    median = centers[cluster.getCenter()];
                double sd = globalSD;
                if( cluster.getLength() > 1 )
                {
                    double mean = 0;
                    for( int i = cluster.getFrom(); i <= cluster.getTo(); i++ )
                        mean += centers[i];
                    mean /= cluster.getLength();
                    sd = 0;
                    for( int i = cluster.getFrom(); i <= cluster.getTo(); i++ )
                        sd += ( centers[i] - mean ) * ( centers[i] - mean );
                    sd = Math.sqrt( sd / ( cluster.getLength() - 1 ) );
                }

                double w = siteModelWidth / 2.0 + 2 * sd / Math.sqrt( cluster.getLength() );

                int from = (int)Math.floor( median - w );
                int to = (int)Math.ceil( median + w );
                int summit = (int)Math.round( median ) - from;

                Cluster c = new Cluster();
                c.setChr( chr );
                c.setFrom( from );
                c.setTo( to );
                c.setSummit( summit );
                c.setPeaks( peaks.subList( cluster.getFrom(), cluster.getTo() + 1 ) );
                chrResult.add( c );
            }
            chrResult.trimToSize();
            result.put( chr, chrResult );
        }
        return result;

    }

    private static <T extends GenomeLocation> Map<String, int[]> fetchCenters(Map<String, List<T>> allPeaksOfPeakCaller)
    {
        Map<String, int[]> result = new HashMap<>();
        for( String chr : allPeaksOfPeakCaller.keySet() )
        {
            List<T> peaks = allPeaksOfPeakCaller.get( chr );
            int[] centers = new int[peaks.size()];
            for( int i = 0; i < peaks.size(); i++ )
            {
                T peak = peaks.get( i );
                centers[i] = peak.getFrom() + peak.getSummit();
            }
            result.put( chr, centers );
        }
        return result;
    }

    private Map<String, Interval[]> getClusters(Map<String, int[]> allCenters)
    {
        return EntryStream.of( allCenters ).mapValues( this::findClusters ).toMap();
    }

    private Interval[] findClusters(int[] centers)
    {
        if( centers.length == 0 )
            return new Interval[0];
        List<Interval> result = new ArrayList<>();
        int i = 0;
        for( int j = 1; j < centers.length; j++ )
            if( centers[j] - centers[j - 1] > parameters.getMaxDistance() )
            {
                result.add( new Interval( i, j - 1 ) );
                i = j;
            }
        result.add( new Interval( i, centers.length - 1 ) );
        return result.toArray( new Interval[0] );
    }

    private static final double DEFAULT_SD = 20;
    private double estimateGlobalSD(Map<String, int[]> allCenters, Map<String, Interval[]> allClusters)
    {
        TDoubleArrayList sdList = new TDoubleArrayList();
        for( Map.Entry<String, Interval[]> e : allClusters.entrySet() )
        {
            String chr = e.getKey();
            Interval[] chrClusters = e.getValue();
            int[] centers = allCenters.get( chr );
            for( Interval cluster : chrClusters )
            {
                if( cluster.getLength() <= 1 )
                    continue;
                double mean = 0;
                for( int i = cluster.getFrom(); i <= cluster.getTo(); i++ )
                    mean += centers[i];
                mean /= cluster.getLength();
                double sd = 0;
                for( int i = cluster.getFrom(); i <= cluster.getTo(); i++ )
                    sd += ( centers[i] - mean ) * ( centers[i] - mean );
                sd = Math.sqrt( sd / ( cluster.getLength() - 1 ) );
                sdList.add( sd );
            }
        }


        if( sdList.isEmpty() )
            return DEFAULT_SD;
        sdList.sort();
        double median = sdList.get( sdList.size() / 2 );

        return median;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath masterTrackPath;
        private DataElementPath newMetadataPath;

        private DataElementPath chipSeqPeaks = DataElementPath.create( "databases/GTRD/Data/bigBed/Homo sapiens/ChIP-seq/Peaks" );
        private DataElementPath chipExoPeaks = DataElementPath.create( "databases/GTRD/Data/bigBed/Homo sapiens/ChIP-exo/Peaks" );
        private DataElementPath dnaseSeqPeaks = DataElementPath.create( "databases/GTRD/Data/bigBed/Homo sapiens/DNase-seq/Peaks" );
        private DataElementPath dnaseClusters = DataElementPath.create( "databases/GTRD/Data/bigBed/Homo sapiens/DNase-seq/Clusters" );
        private DataElementPath footprints = DataElementPath.create( "databases/GTRD/Data/bigBed/Homo sapiens/DNase-seq/Peaks" );
        private DataElementPath footprintClusters = DataElementPath.create( "databases/GTRD/Data/bigBed/Homo sapiens/DNase-seq/Clusters" );
        private DataElementPath atacClusters = DataElementPath.create( "databases/GTRD/Data/bigBed/Homo sapiens/ATAC-seq/Clusters" );
        private DataElementPath faireClusters = DataElementPath.create( "databases/GTRD/Data/bigBed/Homo sapiens/FAIRE-seq/Clusters" );
        private DataElementPath mnaseSeqPeaks = DataElementPath.create( "databases/GTRD/Data/bigBed/Homo sapiens/MNase-seq/Peaks" );
        private DataElementPath histoneModificationPeaks = DataElementPath.create( "databases/GTRD/Data/bigBed/Homo sapiens/ChIP-seq_HM/Peaks" );
        private DataElementPath histoneModificationClusters = DataElementPath.create( "databases/GTRD/Data/bigBed/Homo sapiens/ChIP-seq_HM/Clusters" );
        

        private DataElementPath motifsFolder;

        private DataElementPath resultingMasterTrack;


        private int maxDistance = 50;
        
        private boolean recomputeClusters = true;
        private boolean excludeExperimentsWithoutControl = false;
        private boolean excludeClustersWithoutControl = false;
        private boolean excludeMetaClustersWithoutControl = false;
        private boolean addAnnotation = true;
        private boolean remapIdentifiers = true;
        
        public boolean isRecomputeClusters()
        {
            return recomputeClusters;
        }
        public void setRecomputeClusters(boolean recomputeClusters)
        {
            boolean oldValue = this.recomputeClusters;
            this.recomputeClusters = recomputeClusters;
            firePropertyChange( "recomputeClusters", oldValue, recomputeClusters );
        }
        public boolean willNotRecomputeClusters()//for bean info hidden method
        {
            return !isRecomputeClusters();
        }
        
        public boolean isExcludeExperimentsWithoutControl()
        {
            return excludeExperimentsWithoutControl;
        }
        public void setExcludeExperimentsWithoutControl(boolean excludeExperimentsWithoutControl)
        {
            boolean oldValue = this.excludeExperimentsWithoutControl;
            this.excludeExperimentsWithoutControl = excludeExperimentsWithoutControl;
            firePropertyChange( "excludeExperimentsWithoutControl", oldValue, excludeExperimentsWithoutControl );
        }

        public boolean isExcludeClustersWithoutControl()
        {
            return excludeClustersWithoutControl;
        }
        public void setExcludeClustersWithoutControl(boolean excludeClustersWithoutControl)
        {
            boolean oldValue = this.excludeClustersWithoutControl;
            this.excludeClustersWithoutControl = excludeClustersWithoutControl;
            firePropertyChange( "excludeClustersWithoutControl", oldValue, excludeClustersWithoutControl );
        }
        
        
        public boolean isExcludeMetaClustersWithoutControl()
        {
            return excludeMetaClustersWithoutControl;
        }
        public void setExcludeMetaClustersWithoutControl(boolean excludeMetaClustersWithoutControl)
        {
            boolean oldValue = this.excludeMetaClustersWithoutControl;
            this.excludeMetaClustersWithoutControl = excludeMetaClustersWithoutControl;
            firePropertyChange( "excludeMetaClustersWithoutControl", oldValue, excludeMetaClustersWithoutControl );
        }
        public boolean isAddAnnotation()
        {
            return addAnnotation;
        }
        public void setAddAnnotation(boolean addAnnotation)
        {
            boolean oldValue = this.addAnnotation;
            this.addAnnotation = addAnnotation;
            firePropertyChange( "addAnnotation", oldValue, addAnnotation );
        }
        public boolean willNotAddAnnotation()
        {
            return !isAddAnnotation();
        }
        public boolean isRemapIdentifiers()
        {
            return remapIdentifiers;
        }
        public void setRemapIdentifiers(boolean remapIdentifiers)
        {
            boolean oldValue = this.remapIdentifiers;
            this.remapIdentifiers = remapIdentifiers;
            firePropertyChange( "remapIdentifiers", oldValue, remapIdentifiers );
        }
        
        
        private boolean filterUnmappable;
        public boolean isFilterUnmappable()
        {
            return filterUnmappable;
        }
        public void setFilterUnmappable(boolean filterUnmappable)
        {
            boolean oldValue = this.filterUnmappable;
            this.filterUnmappable = filterUnmappable;
            firePropertyChange( "filterUnmappable", oldValue, filterUnmappable );
        }
        public boolean willNotFilterUnmappable() {
            return!isFilterUnmappable();
        }
        

        private DataElementPath mappabilityWig;
        public DataElementPath getMappabilityWig()
        {
            return mappabilityWig;
        }
        public void setMappabilityWig(DataElementPath mappabilityWig)
        {
            DataElementPath oldValue = this.mappabilityWig;
            this.mappabilityWig = mappabilityWig;
            firePropertyChange( "mappabilityWig", oldValue, mappabilityWig );
        }
        
        private DataElementPath readLengthTable;
        public DataElementPath getReadLengthTable()
        {
            return readLengthTable;
        }
        public void setReadLengthTable(DataElementPath readLengthTable)
        {
            Object oldValue = this.readLengthTable;
            this.readLengthTable = readLengthTable;
            firePropertyChange( "readLengthTable", oldValue, readLengthTable );
        }
        
        private int maxUnmappablePositions = 250;
        public int getMaxUnmappablePositions()
        {
            return maxUnmappablePositions;
        }
        public void setMaxUnmappablePositions(int maxUnmappablePositions)
        {
            int oldValue = this.maxUnmappablePositions;
            this.maxUnmappablePositions = maxUnmappablePositions;
            firePropertyChange( "maxUnmappablePositions", oldValue, maxUnmappablePositions );
        }
        public DataElementPath getMasterTrackPath()
        {
            return masterTrackPath;
        }
        public void setMasterTrackPath(DataElementPath masterTrackPath)
        {
            Object oldValue = this.masterTrackPath;
            this.masterTrackPath = masterTrackPath;
            firePropertyChange( "masterTrackPath", oldValue, masterTrackPath );
        }

        public DataElementPath getNewMetadataPath()
        {
            return newMetadataPath;
        }
        public void setNewMetadataPath(DataElementPath newMetadataPath)
        {
            Object oldValue = this.newMetadataPath;
            this.newMetadataPath = newMetadataPath;
            firePropertyChange( "newMetadataPath", oldValue, newMetadataPath );
        }

        public DataElementPath getChipSeqPeaks()
        {
            return chipSeqPeaks;
        }
        public void setChipSeqPeaks(DataElementPath chipSeqPeaks)
        {
            Object oldValue = this.chipSeqPeaks;
            this.chipSeqPeaks = chipSeqPeaks;
            firePropertyChange( "chipSeqPeaks", oldValue, chipSeqPeaks );
        }
        public DataElementPath getChipExoPeaks()
        {
            return chipExoPeaks;
        }
        public void setChipExoPeaks(DataElementPath chipExoPeaks)
        {
            Object oldValue = this.chipExoPeaks;
            this.chipExoPeaks = chipExoPeaks;
            firePropertyChange( "chipExoPeaks", oldValue, chipExoPeaks );
        }
        public DataElementPath getDnaseSeqPeaks()
        {
            return dnaseSeqPeaks;
        }
        public void setDnaseSeqPeaks(DataElementPath dnaseSeqPeaks)
        {
            Object oldValue = this.dnaseSeqPeaks;
            this.dnaseSeqPeaks = dnaseSeqPeaks;
            firePropertyChange( "dnaseSeqPeaks", oldValue, dnaseSeqPeaks );
        }
        

        public DataElementPath getDnaseClusters()
        {
            return dnaseClusters;
        }
        public void setDnaseClusters(DataElementPath dnaseClusters)
        {
            Object oldValue = this.dnaseClusters;
            this.dnaseClusters = dnaseClusters;
            firePropertyChange( "dnaseClusters", oldValue, dnaseClusters );
        }
        
        public DataElementPath getFootprints()
        {
            return footprints;
        }
        public void setFootprints(DataElementPath footprints)
        {
            Object oldValue = this.footprints;
            this.footprints = footprints;
            firePropertyChange( "footprints", oldValue, footprints );
        }
        
        public DataElementPath getFootprintClusters()
        {
            return footprintClusters;
        }
        public void setFootprintClusters(DataElementPath footprintClusters)
        {
            Object oldValue = this.footprintClusters;
            this.footprintClusters = footprintClusters;
            firePropertyChange( "footprintClusters", oldValue, footprintClusters );
        }

        public DataElementPath getAtacClusters()
        {
            return atacClusters;
        }
        public void setAtacClusters(DataElementPath atacClusters)
        {
            Object oldValue = this.atacClusters;
            this.atacClusters = atacClusters;
            firePropertyChange( "atacClusters", oldValue, atacClusters );
        }
        
        public DataElementPath getFaireClusters()
        {
            return faireClusters;
        }
        public void setFaireClusters(DataElementPath faireClusters)
        {
            Object oldValue = this.faireClusters;
            this.faireClusters = faireClusters;
            firePropertyChange( "faireClusters", oldValue, faireClusters );
        }
        public DataElementPath getMnaseSeqPeaks()
        {
            return mnaseSeqPeaks;
        }
        public void setMnaseSeqPeaks(DataElementPath mnaseSeqPeaks)
        {
            Object oldValue = this.mnaseSeqPeaks;
            this.mnaseSeqPeaks = mnaseSeqPeaks;
            firePropertyChange( "mnaseSeqPeaks", oldValue, mnaseSeqPeaks );
        }
        public DataElementPath getHistoneModificationPeaks()
        {
            return histoneModificationPeaks;
        }
        public void setHistoneModificationPeaks(DataElementPath histoneModificationPeaks)
        {
            Object oldValue = this.histoneModificationPeaks;
            this.histoneModificationPeaks = histoneModificationPeaks;
            firePropertyChange( "histoneModificationPeaks", oldValue, histoneModificationPeaks );
        }
        public DataElementPath getHistoneModificationClusters()
        {
            return histoneModificationClusters;
        }
        public void setHistoneModificationClusters(DataElementPath histoneModificationClusters)
        {
            Object oldValue = this.histoneModificationClusters;
            this.histoneModificationClusters = histoneModificationClusters;
            firePropertyChange( "histoneModificationClusters", oldValue, histoneModificationClusters );
        }
        
        public DataElementPath getMotifsFolder()
        {
            return motifsFolder;
        }
        public void setMotifsFolder(DataElementPath motifsFolder)
        {
            Object oldValue = this.motifsFolder;
            this.motifsFolder = motifsFolder;
            firePropertyChange( "motifsFolder", oldValue, motifsFolder );
        }


        public int getMaxDistance()
        {
            return maxDistance;
        }
        public void setMaxDistance(int maxDistance)
        {
            int oldValue = this.maxDistance;
            this.maxDistance = maxDistance;
            firePropertyChange( "maxDistance", oldValue, maxDistance );
        }
        public DataElementPath getResultingMasterTrack()
        {
            return resultingMasterTrack;
        }
        public void setResultingMasterTrack(DataElementPath resultingMasterTrack)
        {
            Object oldValue = this.resultingMasterTrack;
            this.resultingMasterTrack = resultingMasterTrack;
            firePropertyChange( "resultingMasterTrack", oldValue, resultingMasterTrack );
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
            property( "masterTrackPath" ).inputElement( MasterTrack.class ).canBeNull().add();
            property( "newMetadataPath" ).inputElement( Metadata.class ).canBeNull().add();
            
            property("recomputeClusters").add();
            property("excludeExperimentsWithoutControl").add();
            property("excludeClustersWithoutControl").hidden( "isExcludeExperimentsWithoutControl" ).add();
            property("excludeMetaClustersWithoutControl").hidden("isExcludeExperimentsWithoutControl").add();
            property( "chipSeqPeaks" ).inputElement( ru.biosoft.access.core.DataCollection.class ).hidden( "willNotRecomputeClusters" ).add();
            
            property("filterUnmappable").add();
            property("mappabilityWig").inputElement( BigWigTrack.class ).hidden("willNotFilterUnmappable").add();
            property("readLengthTable").inputElement( TableDataCollection.class ).hidden("willNotFilterUnmappable").add();
            property("maxUnmappablePositions").hidden("willNotFilterUnmappable").add();
            
            property("addAnnotation").add();
            property( "chipExoPeaks" ).inputElement( ru.biosoft.access.core.DataCollection.class ).hidden( "willNotAddAnnotation" ).canBeNull().add();
            property( "dnaseSeqPeaks" ).inputElement( ru.biosoft.access.core.DataCollection.class ).hidden( "willNotAddAnnotation" ).canBeNull().add();
            property( "dnaseClusters" ).inputElement( ru.biosoft.access.core.DataCollection.class ).hidden( "willNotAddAnnotation" ).canBeNull().add();
            property( "footprints" ).inputElement( ru.biosoft.access.core.DataCollection.class ).hidden( "willNotAddAnnotation" ).canBeNull().add();
            property( "footprintClusters" ).inputElement( ru.biosoft.access.core.DataCollection.class ).hidden( "willNotAddAnnotation" ).canBeNull().add();
            property( "atacClusters" ).inputElement( ru.biosoft.access.core.DataCollection.class ).hidden( "willNotAddAnnotation" ).canBeNull().add();
            property( "faireClusters" ).inputElement( ru.biosoft.access.core.DataCollection.class ).hidden( "willNotAddAnnotation" ).canBeNull().add();
            property( "mnaseSeqPeaks" ).inputElement( ru.biosoft.access.core.DataCollection.class ).hidden( "willNotAddAnnotation" ).canBeNull().add();
            property( "histoneModificationPeaks" ).inputElement( ru.biosoft.access.core.DataCollection.class ).hidden( "willNotAddAnnotation" ).canBeNull().add();
            property( "histoneModificationClusters" ).inputElement( ru.biosoft.access.core.DataCollection.class ).hidden( "willNotAddAnnotation" ).canBeNull().add();
            property( "motifsFolder" ).inputElement( ru.biosoft.access.core.DataCollection.class ).hidden( "willNotAddAnnotation" ).canBeNull().add();
            
            property("remapIdentifiers").add();

            property( "resultingMasterTrack" ).outputElement( MasterTrack.class ).add();
        }
    }
}
