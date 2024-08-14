package ru.biosoft.bsa.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.standard.type.Species;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.MergedTrack;
import ru.biosoft.bsa.ShrinkedTrack;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.transformer.SiteModelTransformer;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ObjectCache;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.TransformedIterator;

@ClassIcon( "resources/site_search_optimization.gif" )
public class OptimizeSiteSearchAnalysis extends AnalysisMethodSupport<OptimizeSiteSearchAnalysisParameters>
{
    // Model weights will be multiplied by this number in order to convert them to integers
    private static final int SCORE_MULTIPLIER = 10000;
    private static final String[] TRACK_NAME = new String[]{"yes","no"};
    private static final int YES = 0, NO = 1;
    private static final int MIN_WIDTH = 300;
    
    public OptimizeSiteSearchAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, JavaScriptBSA.class, new OptimizeSiteSearchAnalysisParameters());
    }

    private static class SiteInfo implements Comparable<SiteInfo>
    {
        int weight, from, to;
        String sequence;
        
        public SiteInfo(String sequence, int from, int to, int weight)
        {
            this.sequence = sequence;
            this.from = from;
            this.to = to;
            this.weight = weight;
        }
        
        public SiteInfo(Site site)
        {
            DynamicProperty weightProperty = site.getProperties().getProperty(Site.SCORE_PROPERTY);
            if(weightProperty != null)
                weight = (int)(((Float)weightProperty.getValue())*SCORE_MULTIPLIER);
            from = site.getFrom();
            to = site.getTo();
            sequence = site.getOriginalSequence().getName();
        }
        @Override
        public int compareTo(SiteInfo s)
        {
            int result = sequence.compareTo(s.sequence);
            if(result != 0) return result;
            return from-s.from;
        }
        
        @Override
        public String toString()
        {
            return sequence+"\t"+from+"\t"+to+"\t"+weight;
        }
    }
    
    /**
     * Disk-swapped container for SiteInfo objects sorted by model
     * Not thread-safe
     * @author lan
     */
    private static class DiskSwappedSitesMap extends AbstractMap<SiteModel, List<SiteInfo>>
    {
        private static final int MAX_MEMORY_SITES = 1000000;
        private File directory;
        private final Map<SiteModel, Integer> modelFileNames = new HashMap<>();
        private Map<SiteModel, List<SiteInfo>> data = new HashMap<>();
        private int memorySites = 0;
        private boolean diskCacheUsed = false;
        private final ObjectCache<String> sequenceNames = new ObjectCache<>();
        
        @Override
        protected void finalize()
        {
            close();
        }
        
        private void flush() throws IOException
        {
            if(!diskCacheUsed)
            {
                directory = TempFiles.dir("optimizeSiteSearch");
                diskCacheUsed = true;
            }
            for(Entry<SiteModel, List<SiteInfo>> entry: data.entrySet())
            {
                File file = new File(directory, String.valueOf(modelFileNames.get(entry.getKey())));
                
                try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)))
                {
                    for(SiteInfo site: entry.getValue())
                    {
                        writer.write(site.toString());
                        writer.newLine();
                    }
                }
                entry.getValue().clear();
            }
            memorySites = 0;
        }

        private List<SiteInfo> load(SiteModel key)
        {
            List<SiteInfo> result = new ArrayList<>();
            File file = new File(directory, String.valueOf(modelFileNames.get(key)));
            try(BufferedReader reader = ApplicationUtils.utfReader( file ))
            {
                String line;
                while( reader.ready() && ( line = reader.readLine() ) != null )
                {
                    int pos1 = 0, pos2 = line.indexOf('\t', pos1);
                    String sequence = sequenceNames.get(line.substring(pos1, pos2));
                    pos1 = pos2+1; pos2 = line.indexOf('\t', pos1);
                    int from = Integer.parseInt(line.substring(pos1, pos2));
                    pos1 = pos2+1; pos2 = line.indexOf('\t', pos1);
                    int to = Integer.parseInt(line.substring(pos1, pos2));
                    pos1 = pos2+1;
                    int weight = Integer.parseInt(line.substring(pos1));
                    result.add(new SiteInfo(sequence, from, to, weight));
                }
            }
            catch(Exception e)
            {
            }
            Collections.sort(result);
            return result;
        }

        public void close()
        {
            if(directory != null) ApplicationUtils.removeDir(directory);
        }
        
        public void add(SiteModel model, SiteInfo site) throws IOException
        {
            if(!modelFileNames.containsKey(model))
            {
                modelFileNames.put(model, modelFileNames.size());
                data.put(model, new ArrayList<SiteInfo>());
            }
            data.get(model).add(site);
            memorySites++;
            if(memorySites >= MAX_MEMORY_SITES)
            {
                flush();
            }
        }
        
        public void finalizeAddition() throws IOException
        {
            if(diskCacheUsed)
            {
                flush();
                data = null;
            }
            else
            {
                for(List<SiteInfo> list: data.values())
                {
                    Collections.sort(list);
                }
            }
        }
        
        @Override
        public Set<Entry<SiteModel, List<SiteInfo>>> entrySet()
        {
            return new AbstractSet<Entry<SiteModel,List<SiteInfo>>>()
            {
                @Override
                public Iterator<Entry<SiteModel, List<SiteInfo>>> iterator()
                {
                    return new TransformedIterator<SiteModel, Entry<SiteModel,List<SiteInfo>>>(modelFileNames.keySet().iterator())
                    {
                        @Override
                        protected Entry<SiteModel, List<SiteInfo>> transform(final SiteModel key)
                        {
                            return new SimpleImmutableEntry<>( key, get( key ) );
                        }
                    };
                }

                @Override
                public int size()
                {
                    return DiskSwappedSitesMap.this.size();
                }
            };
        }

        @Override
        public int size()
        {
            return modelFileNames.size();
        }

        @Override
        public List<SiteInfo> get(Object key)
        {
            if(diskCacheUsed)
            {
                return load((SiteModel)key);
            }
            return data.get(key);
        }

        @Override
        public Set<SiteModel> keySet()
        {
            return modelFileNames.keySet();
        }
    }
    
    private static class SiteModelInfo
    {
        int bestCutoff;
        double bestPvalue;
        Interval bestInterval = null;
        List<SiteInfo>[] promoters = null;
        int[] counts = null;
        int[] totals = null;
        int[] scores = null;

        public SiteModelInfo(double bestPvalue)
        {
            super();
            this.bestCutoff = -1;
            this.bestPvalue = bestPvalue;
        }
        
        public boolean isValid()
        {
            return this.bestCutoff != -1 && this.bestInterval != null;
        }
    }

    private Interval getPromoterWindow(SiteSearchTrackInfo trackInfo)
    {
        Integer from = null, to = null;
        Track intervals = trackInfo.getIntervals();
        if(intervals instanceof DataCollection)
        {
            AnalysisParameters parameters = AnalysisParametersFactory.read(intervals);
            if(parameters instanceof GeneSetToTrackParameters)
            {
                from = ((GeneSetToTrackParameters)parameters).getFrom();
                to = ((GeneSetToTrackParameters)parameters).getTo();
            }
        }
        if(from == null)
        {
            Iterator<Site> iterator = trackInfo.getTrackIterator();
            from = 0;
            to = 0;
            while(iterator.hasNext())
                to = Math.max(to, iterator.next().getLength());
        }
        return new Interval(from, to);
    }
    
    private List<Interval> getIntervals(SiteSearchTrackInfo trackInfo)
    {
        Interval promoterWindow = getPromoterWindow(trackInfo);
        if(this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_BOTH)
                || this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_WINDOW))
        {
            List<Interval> result = new ArrayList<>();
            int windowWidth = promoterWindow.getTo() - promoterWindow.getFrom();
            int parts = windowWidth / 100, tail = windowWidth % 100, index = 0, width = MIN_WIDTH;
            if (tail != 0) parts += 1;
            int nIntervals = parts * (parts + 1) / 2;
            for( ;; width += 100 )
            {
                int count = 0, left = promoterWindow.getFrom(), right;
                for( ; ( right = left + width ) <= promoterWindow.getTo() || ( tail != 0 && ( right -= 100 - tail ) <= promoterWindow.getTo() ); index++, left += 100 )
                {
                    if( index >= nIntervals )
                        break;
                    result.add(new Interval(left, right));
                    count++;
                }
                if( count == 0 )
                    break;
            }
            return result;
        }
        return Collections.singletonList(promoterWindow);
    }
    
    private Map<SiteModel, List<SiteInfo>> getAllSites(SiteSearchTrackInfo trackInfo) throws IOException
    {
        DiskSwappedSitesMap result = new DiskSwappedSitesMap();
        Iterator<Site> iterator = trackInfo.getTrackIterator();
        while(iterator.hasNext())
        {
            Site site = iterator.next();
            DynamicProperty siteModelProperty = site.getProperties().getProperty("siteModel");
            if(siteModelProperty == null || siteModelProperty.getValue() == null || !(siteModelProperty.getValue() instanceof SiteModel)) continue;
            SiteModel model = (SiteModel)siteModelProperty.getValue();
            result.add(model, new SiteInfo(site));
        }
        result.finalizeAddition();
        return result;
    }
    
    private void saveSites(SiteSearchTrackInfo trackInfo, Map<String, SiteModelInfo> modelsInfo, @Nonnull WritableTrack output, int num)
            throws Exception
    {
        Iterator<Site> iterator = trackInfo.getTrackIterator();
        while(iterator.hasNext())
        {
            Site site = iterator.next();
            DynamicProperty siteModelProperty = site.getProperties().getProperty("siteModel");
            if(siteModelProperty == null || siteModelProperty.getValue() == null || !(siteModelProperty.getValue() instanceof SiteModel)) continue;
            SiteModel model = (SiteModel)siteModelProperty.getValue();
            SiteModelInfo modelInfo = modelsInfo.get(model.getName());
            if(modelInfo == null || !modelInfo.isValid()) continue;
            double score = site.getScore();
            if(score*SCORE_MULTIPLIER<modelInfo.bestCutoff) continue;
            if(this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_BOTH)
                    || this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_WINDOW))
            {
                if(!isSiteOnPromoters(site, modelInfo.promoters[num])) continue;
            }
            output.addSite( site );
        }
        output.finalizeAddition();
        CollectionFactoryUtils.save(output);
    }
    
    private boolean isSiteOnPromoters(Site site, List<SiteInfo> promoters)
    {
        return Collections.binarySearch(promoters, new SiteInfo(site), (s1, s2) -> {
            int seqCompare = s1.sequence.compareTo(s2.sequence);
            // sites lying on different sequences
            if(seqCompare != 0) return seqCompare;
            // one of sites completely encloses another -- consider this as a hit
            if((s1.from <= s2.from && s1.to >= s2.to) || (s1.from >= s2.from && s1.to <= s2.to)) return 0;
            return s1.from-s2.from;
        })>=0;
    }
    
    protected WritableTrack createOutputTrack(SiteSearchTrackInfo input, DataElementPath output) throws Exception
    {
        if(output == null) return null;
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, output.getName());
        if(input.getIntervals() != null)
            properties.put(SiteSearchAnalysis.INTERVALS_COLLECTION_PROPERTY, DataElementPath.create(input.getIntervals()).toString());
        properties.put(SiteSearchAnalysis.SEQUENCES_COLLECTION_PROPERTY, DataElementPath.create(input.getSequencesDC()).toString());
        properties.put(Track.SEQUENCES_COLLECTION_PROPERTY, DataElementPath.create(input.getSequencesDC()).toString());
        SiteSearchAnalysis.serializeSequencesList(properties, input.getSeqList());
        properties.put(SiteSearchAnalysis.TOTAL_LENGTH_PROPERTY, String.valueOf(input.getTotalLength()));
        WritableTrack track = new SqlTrack(output.optParentCollection(), properties);
        return track;
    }
    
    private boolean isSiteAfterPromoter(SiteInfo curPromoter, SiteInfo info)
    {
        return (curPromoter.to < info.to && curPromoter.sequence.equals(info.sequence)) || curPromoter.sequence.compareTo(info.sequence) < 0;
    }

    private boolean isSiteOnPromoter(SiteInfo curPromoter, SiteInfo info)
    {
        return curPromoter.from <= info.from && curPromoter.to >= info.to && curPromoter.sequence.equals(info.sequence);
    }
    
    @SuppressWarnings ( "unchecked" )
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        validateParameters();
        Track inputs[] = new Track[2];
        inputs[YES] = parameters.getInYesTrack().getDataElement(Track.class);
        inputs[NO] = parameters.getInNoTrack().getDataElement(Track.class);
        SiteSearchTrackInfo[] trackInfo = new SiteSearchTrackInfo[2];
        Map<String, SiteModelInfo> modelsInfo = new HashMap<>();
        jobControl.setPreparedness(1);
        Map<SiteModel,List<SiteInfo>>[] sites = new Map[2];
        Set<SiteModel> models = new HashSet<>();
        int trackNum;
        for(trackNum=0; trackNum<2; trackNum++)
        {
            jobControl.setPreparedness(trackNum*5);
            log.info("Preparing "+TRACK_NAME[trackNum]+" sites...");
            try
            {
                trackInfo[trackNum] = new SiteSearchTrackInfo(inputs[trackNum]);
            }
            catch(Exception e)
            {
                throw new ParameterNotAcceptableException(e, parameters, "in"+TextUtil.ucFirst(TRACK_NAME[trackNum])+"Track");
            }
            sites[trackNum] = getAllSites(trackInfo[trackNum]);
            models.addAll(sites[trackNum].keySet());
        }
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
        
        SiteModel[] modelsArray = models.toArray(new SiteModel[models.size()]);
        log.info("Optimizing...");
        jobControl.setPreparedness(10);
        List<Interval> intervalsList = getIntervals(trackInfo[YES]);
        for(int j=0; j<intervalsList.size(); j++)
        {
            Interval interval = intervalsList.get(j);
            int totalLength[] = new int[2];
            List<SiteInfo> intervalTracks[] = new List[2];
            for(trackNum=0; trackNum<2; trackNum++)
            {
                intervalTracks[trackNum] = new ArrayList<>();
                Iterator<Site> iterator;
                if( trackInfo[trackNum].getIntervals() == null
                        && this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_CUTOFF) )
                {
                    iterator = trackInfo[trackNum].getIntervalsIterator();
                }
                else
                {
                    Track track = new MergedTrack(this.parameters.getOptimizationType().equals(
                            OptimizeSiteSearchAnalysisParameters.OPTIMIZE_CUTOFF) ? trackInfo[trackNum].getIntervals() : new ShrinkedTrack(
                            trackInfo[trackNum].getIntervals(), interval.getFrom(), interval.getTo()));
                    iterator = trackInfo[trackNum].getAnyTrackIterator(track);
                }
                while(iterator.hasNext())
                {
                    Site s = iterator.next();
                    intervalTracks[trackNum].add(new SiteInfo(s));
                    totalLength[trackNum]+=s.getLength();
                }
                Collections.sort(intervalTracks[trackNum]);
            }
            
            for(int i=0; i<modelsArray.length; i++)
            {
                SiteModel model = modelsArray[i];
                List<SiteInfo>[] modelSites = new List[]{sites[YES].get(model),sites[NO].get(model)};
                if(!modelsInfo.containsKey(model.getName()))
                {
                    SiteModelInfo modelInfo = new SiteModelInfo(parameters.getPvalueCutoff());
                    modelsInfo.put(model.getName(), modelInfo);
                    int[] scoresArray;
                    if(this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_BOTH)
                            || this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_CUTOFF))
                    {
                        scoresArray = StreamEx.of( modelSites ).nonNull().flatMap( List::stream ).mapToInt( info -> info.weight ).sorted()
                                .toArray();
                    } else
                    {
                        scoresArray = new int[] {0};
                    }
                    modelInfo.scores = scoresArray;
                }
                SiteModelInfo modelInfo = modelsInfo.get(model.getName());
                
                int[] counts = new int[modelInfo.scores.length*2];
        
                for(trackNum = 0; trackNum < 2; trackNum ++)
                {
                    if(modelSites[trackNum] != null)
                    {
                        Iterator<SiteInfo> promoterIterator = intervalTracks[trackNum].iterator();
                        SiteInfo curPromoter = null;
                        for(SiteInfo info: modelSites[trackNum])
                        {
                            while(curPromoter == null || isSiteAfterPromoter(curPromoter, info))
                            {
                                if( !promoterIterator.hasNext() )
                                {
                                    curPromoter = null;
                                    break;
                                }
                                curPromoter = promoterIterator.next();
                            }
                            if(curPromoter == null) break;
                            if(isSiteOnPromoter(curPromoter, info))
                            {
                                for(int scoreNum = 0; scoreNum < modelInfo.scores.length && info.weight>=modelInfo.scores[scoreNum]; scoreNum++)
                                    counts[scoreNum*2+trackNum]++;
                            }
                        }
                    }
                }
                for(int scoreNum = 0; scoreNum < modelInfo.scores.length; scoreNum++)
                {
                    if(counts[scoreNum*2+YES]==0 && counts[scoreNum*2+NO]==0) continue;
                    double[] prob = Stat.cumulativeBinomialFast(counts[scoreNum*2+YES] + counts[scoreNum*2+NO], counts[scoreNum*2+YES], (float)totalLength[YES]
                            / ( totalLength[YES] + totalLength[NO] ));
                    double pvalue = Math.min(prob[0], prob[1]);
                    if(pvalue < modelInfo.bestPvalue)
                    {
                        modelInfo.bestCutoff = modelInfo.scores[scoreNum];
                        modelInfo.bestPvalue = pvalue;
                        modelInfo.bestInterval = interval;
                        modelInfo.promoters = intervalTracks;
                        modelInfo.counts = new int[]{counts[scoreNum*2+YES], counts[scoreNum*2+NO]};
                        modelInfo.totals = totalLength;
                    }
                }
                jobControl.setPreparedness(60*(i+j*modelsArray.length)/modelsArray.length/intervalsList.size()+10);
                if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
            }
        }
        for(trackNum = 0; trackNum < 2; trackNum++)
        {
            jobControl.setPreparedness(70+trackNum*10);
            if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
            WritableTrack output = createOutputTrack(trackInfo[trackNum], trackNum==YES?parameters.getOutYesTrack():parameters.getOutNoTrack());
            if(output == null) continue;
            log.info("Writing "+TRACK_NAME[trackNum]+" track...");
            saveSites(trackInfo[trackNum], modelsInfo, output, 0);
        }
        jobControl.setPreparedness(90);
        log.info("Writing summary...");
        TableDataCollection resTable = TableDataCollectionUtils.createTableDataCollection(parameters.getOutSummaryTable());
        ColumnModel columnModel = resTable.getColumnModel();
        columnModel.addColumn("Yes density per 1000bp", Double.class);
        columnModel.addColumn("No density per 1000bp", Double.class);
        columnModel.addColumn("Yes-No ratio", Double.class);
        if(this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_BOTH)
                || this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_CUTOFF))
            columnModel.addColumn("Model cutoff", Double.class);
        if(this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_BOTH)
                || this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_WINDOW))
        {
            columnModel.addColumn("From", Integer.class);
            columnModel.addColumn("To", Integer.class);
        }
        columnModel.addColumn("P-value", Double.class);
        for(Entry<String, SiteModelInfo> entry: modelsInfo.entrySet())
        {
            Object[] values = new Object[columnModel.getColumnCount()];
            SiteModelInfo modelInfo = entry.getValue();
            if(!modelInfo.isValid()) continue;
            int yesCount = modelInfo.counts[0];
            int noCount = modelInfo.counts[1];
            int colNum = 0;
            values[colNum++] = (double)yesCount/modelInfo.totals[YES]*1000;
            values[colNum++] = (double)noCount/modelInfo.totals[NO]*1000;
            if(noCount != 0)
            {
                double ratio = ((Double)values[0])/((Double)values[1]);
                if(parameters.isOverrepresentedOnly() && ratio <= 1) continue;
                values[colNum++] = ratio;
            } else
            {
                values[colNum++] = null;
                if(parameters.isOverrepresentedOnly()) continue;
            }
            
            if(this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_BOTH)
                    || this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_CUTOFF))
                values[colNum++] = ((double)modelInfo.bestCutoff)/SCORE_MULTIPLIER;
            if(this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_BOTH)
                    || this.parameters.getOptimizationType().equals(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_WINDOW))
            {
                values[colNum++] = modelInfo.bestInterval.getFrom();
                values[colNum++] = modelInfo.bestInterval.getTo();
            }
            try
            {
                double[] prob = Stat.cumulativeBinomialFast(yesCount + noCount, yesCount, (float)modelInfo.totals[YES]
                        / ( modelInfo.totals[YES] + modelInfo.totals[NO] ));
                values[colNum++] = Math.min(prob[0], prob[1]);
            }
            catch( Exception e )
            {
            }
            TableDataCollectionUtils.addRow(resTable, entry.getKey(), values);
        }
        TableDataCollectionUtils.setSortOrder(resTable, "Yes-No ratio", false);
        if(inputs[YES] instanceof SqlTrack)
        {
            String siteModelsPath = ((SqlTrack)inputs[YES]).getInfo().getProperty(SqlTrack.DE_PROPERTY_COLLECTION_PREFIX+"siteModel");
            if(siteModelsPath != null)
            {
                DataCollection<SiteModel> optimizedProfile = createOptimizedProfile( modelsInfo, siteModelsPath );
                if(optimizedProfile != null)
                {
                    log.info( "Creating profile..." );
                    jobControl.setPreparedness( 95 );
                    resTable.getInfo().getProperties().setProperty( SiteSearchResult.PROFILE_PROPERTY,
                            optimizedProfile.getCompletePath().toString() );
                    try
                    {
                        if( parameters.getOutYesTrack() != null )
                            parameters.getOutYesTrack().getDataElement( SqlTrack.class ).getInfo().getProperties()
                                    .setProperty( SqlTrack.DE_PROPERTY_COLLECTION_PREFIX + "siteModel",
                                            optimizedProfile.getCompletePath().toString() );
                        if( parameters.getOutNoTrack() != null )
                            parameters.getOutNoTrack().getDataElement( SqlTrack.class ).getInfo().getProperties().setProperty(
                                    SqlTrack.DE_PROPERTY_COLLECTION_PREFIX + "siteModel", optimizedProfile.getCompletePath().toString() );
                    }
                    catch( Exception e )
                    {
                    }
                }
                else
                    resTable.getInfo().getProperties().setProperty(SiteSearchResult.PROFILE_PROPERTY, siteModelsPath);

            }
            resTable.getInfo().getProperties()
                    .setProperty(DataCollectionUtils.SPECIES_PROPERTY, Species.getDefaultSpecies((DataCollection<?>)inputs[YES]).getLatinName());
        }
        resTable.getInfo().setNodeImageLocation(getClass(), "resources/sitessummary.gif");
        if(parameters.getOutYesTrack() != null)
            resTable.getInfo().getProperties().setProperty(SiteSearchReport.YES_TRACK_PROPERTY, parameters.getOutYesTrack().toString());
        if(parameters.getOutNoTrack() != null)
            resTable.getInfo().getProperties().setProperty(SiteSearchReport.NO_TRACK_PROPERTY, parameters.getOutNoTrack().toString());
        parameters.getOutSummaryTable().save(resTable);
        return null;
    }

    private DataCollection<SiteModel> createOptimizedProfile(Map<String, SiteModelInfo> modelsInfo, String siteModelsPath)
    {
        if( parameters.getOutProfile() == null )
            return null;
        if( parameters.getOptimizationType().equals( OptimizeSiteSearchAnalysisParameters.OPTIMIZE_WINDOW ) )
            return null;
        DataElementPath profilePath = DataElementPath.create( siteModelsPath );
        DataCollection<SiteModel> profile = profilePath.getDataCollection( SiteModel.class );
        DataCollection<SiteModel> result;
        try
        {
            result = SiteModelTransformer.createCollection(parameters.getOutProfile());
        }
        catch( Exception e )
        {
            return null;
        }
        for( Entry<String, SiteModelInfo> entry : modelsInfo.entrySet() )
        {
            SiteModelInfo modelInfo = entry.getValue();
            if( !modelInfo.isValid() )
                continue;
            SiteModel clone = null;
            try
            {
                SiteModel siteModel = profile.get( entry.getKey() );
                clone = siteModel.clone( result, entry.getKey() );
            }
            catch( Exception e )
            {
            }

            if( clone != null && modelInfo.bestCutoff > 0 )
            {
                clone.setThreshold( ( (double)modelInfo.bestCutoff ) / SCORE_MULTIPLIER );
                result.put( clone );
            }
        }
        CollectionFactoryUtils.save( result );
        return result;
    }
}
