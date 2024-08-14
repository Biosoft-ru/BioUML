package ru.biosoft.bsa.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.DynamicProperty;

@ClassIcon ( "resources/sitessummary.gif" )
public class SiteSearchSummary extends AnalysisMethodSupport<SiteSearchSummaryParameters>
{
    public static final String SITES_DENSITY_PER_1000BP = "Sites density per 1000bp";
    public static final String P_VALUE_COLUMN = "P-value";

    public SiteSearchSummary(DataCollection<?> origin, String name)
    {
        super(origin, name, JavaScriptBSA.class, new SiteSearchSummaryParameters());
    }

    @Override
    protected AnalysisJobControl createJobControl()
    {
        return new SiteSearchSummaryJobControl();
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        log.info("Initializing...");
        Track yesTrack = parameters.getYesTrackPath().getDataElement(Track.class);
        SiteSearchTrackInfo yesTrackInfo, noTrackInfo = null;
        try
        {
            yesTrackInfo = new SiteSearchTrackInfo(yesTrack);
        }
        catch(Exception e)
        {
            throw new ParameterNotAcceptableException(e, parameters, "yesTrackPath");
        }
        if(jobControl.isStopped()) return null;
        if(parameters.getNoTrackPath() != null)
        {
            Track noTrack = parameters.getNoTrackPath().getDataElement(Track.class);
            try
            {
                noTrackInfo = new SiteSearchTrackInfo(noTrack);
            }
            catch(Exception e)
            {
                throw new ParameterNotAcceptableException(e, parameters, "noTrackPath");
            }
        }
        if(jobControl.isStopped()) return null;
        log.info("Generating result...");
        DataElementPath outputPath = parameters.getOutputPath();
        TableDataCollection resTable = TableDataCollectionUtils.createTableDataCollection(outputPath);
        jobControl.setPreparedness(SiteSearchSummaryJobControl.INITIALIZED);
        generateSiteSearchSummary(yesTrackInfo, noTrackInfo, resTable, parameters.isOverrepresentedOnly());
        if(jobControl.isStopped())
        {
            outputPath.remove();
            return null;
        }
        TableDataCollectionUtils.setSortOrder(resTable, P_VALUE_COLUMN, true);
        if(yesTrack instanceof SqlTrack)
        {
            String siteModelsPath = ((SqlTrack)yesTrack).getInfo().getProperty(SqlTrack.DE_PROPERTY_COLLECTION_PREFIX+"siteModel");
            if(siteModelsPath != null)
                resTable.getInfo().getProperties().setProperty(SiteSearchResult.PROFILE_PROPERTY, siteModelsPath);
            resTable.getInfo().getProperties()
                    .setProperty(DataCollectionUtils.SPECIES_PROPERTY, Species.getDefaultSpecies((DataCollection<?>)yesTrack).getLatinName());
        }
        resTable.getInfo().setNodeImageLocation(getClass(), "resources/sitessummary.gif");
        resTable.getInfo().getProperties().setProperty(SiteSearchReport.YES_TRACK_PROPERTY, parameters.getYesTrackPath().toString());
        if(parameters.getNoTrackPath() != null)
            resTable.getInfo().getProperties().setProperty(SiteSearchReport.NO_TRACK_PROPERTY, parameters.getNoTrackPath().toString());
        outputPath.save(resTable);
        jobControl.setPreparedness(SiteSearchSummaryJobControl.WRITING_COMPLETE);
        return resTable;
    }
    
    private Map<String, Integer> countSites(SiteSearchTrackInfo trackInfo)
    {
        Map<String, Integer> result = new HashMap<>();
        Iterator<Site> iterator = trackInfo.getTrackIterator();
        while(iterator.hasNext())
        {
            Site site = iterator.next();
            DynamicProperty modelProperty = site.getProperties().getProperty(SiteModel.SITE_MODEL_PROPERTY);
            if(modelProperty == null || modelProperty.getValue() == null || !(modelProperty.getValue() instanceof SiteModel)) continue;
            SiteModel model = (SiteModel)modelProperty.getValue();
            result.put(model.getName(), result.containsKey(model.getName())?result.get(model.getName())+1:1);
            ((SiteSearchSummaryJobControl)jobControl).nextItem();
        }
        return result;
    }
    
    private void generateSiteSearchSummary(SiteSearchTrackInfo yesTrackInfo, SiteSearchTrackInfo noTrackInfo, TableDataCollection resTable, boolean overrepresentedOnly)
    {
        ((SiteSearchSummaryJobControl)jobControl).setTotalLength(yesTrackInfo.getSitesCount()+(noTrackInfo==null?0:noTrackInfo.getSitesCount()));
        Map<String, Integer> yesSitesCount = countSites(yesTrackInfo);
        Map<String, Integer> noSitesCount = noTrackInfo == null?null:countSites(noTrackInfo);
        jobControl.setPreparedness(SiteSearchSummaryJobControl.SITES_COUNT_COMPLETE);
        if(noTrackInfo == null)
        {
            resTable.getColumnModel().addColumn(SITES_DENSITY_PER_1000BP, Double.class);
        } else
        {
            resTable.getColumnModel().addColumn("Yes density per 1000bp", Double.class);
            resTable.getColumnModel().addColumn("No density per 1000bp", Double.class);
            resTable.getColumnModel().addColumn("Yes-No ratio", Double.class);
            resTable.getColumnModel().addColumn(P_VALUE_COLUMN, Double.class);
        }
        for( Map.Entry<String, Integer> entry : yesSitesCount.entrySet() )
        {
            String matrix = entry.getKey();
            Object[] values = new Object[noTrackInfo == null?1:4];
            int yesCount = entry.getValue();
            int noCount = noSitesCount == null || !noSitesCount.containsKey(matrix)?0:noSitesCount.get(matrix);
            values[0] = (double)yesCount/yesTrackInfo.getTotalLength()*1000;
            if(noTrackInfo != null)
            {
                values[1] = (double)noCount/noTrackInfo.getTotalLength()*1000;
                if(noCount != 0)
                {
                    values[2] = ((Double)values[0])/((Double)values[1]);
                    if(overrepresentedOnly && ((Double)values[2]) <= 1) continue;
                } else
                {
                    values[2] = null;
                    if(overrepresentedOnly) continue;
                }
                try
                {
                    double[] prob = Stat.cumulativeBinomial(yesCount + noCount, yesCount, (float)yesTrackInfo.getTotalLength()
                            / ( yesTrackInfo.getTotalLength() + noTrackInfo.getTotalLength() ));
                    values[3] = Math.min(prob[0], prob[1]);
                }
                catch( Exception e )
                {
                }
            }
            TableDataCollectionUtils.addRow(resTable, matrix, values);
            if(jobControl.isStopped()) return;
        }
    }

    public class SiteSearchSummaryJobControl extends AnalysisJobControl
    {
        public static final int INITIALIZED = 5;
        public static final int SITES_COUNT_COMPLETE = 75;
        public static final int WRITING_COMPLETE = 100;
        private long nTrack = 0;
        private long totalLength = 0;

        public SiteSearchSummaryJobControl()
        {
            super(SiteSearchSummary.this);
        }
        
        public void setTotalLength(long totalLength)
        {
            this.totalLength = totalLength;
        }
        
        public void nextItem()
        {
            this.nTrack++;
            setPreparedness((int)((SITES_COUNT_COMPLETE-INITIALIZED)*nTrack/totalLength+INITIALIZED));
        }
    }
}
