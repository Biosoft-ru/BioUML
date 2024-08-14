package ru.biosoft.bsa.analysis.maos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;

public class SummaryStatistics
{
    private int variantCount;
    private List<String> siteModelNames;
    private Map<String, Integer> siteGains = new HashMap<>();
    private Map<String, Integer> siteLosses = new HashMap<>();

    public SummaryStatistics(SiteModelCollection siteModels, Track vcfTrack)
    {
        siteModelNames = siteModels.getNameList();
        variantCount = vcfTrack.getAllSites().getSize();
    }

    public SummaryStatistics(SiteModelCollection siteModels, int variantCount)
    {
        siteModelNames = siteModels.getNameList();
        this.variantCount = variantCount;
    }

    public void siteGain(SiteModel siteModel)
    {
        siteGains.merge( siteModel.getName(), 1, Integer::sum );
    }
    public void siteLoss(SiteModel siteModel)
    {
        siteLosses.merge( siteModel.getName(), 1, Integer::sum );
    }

    public TableDataCollection makeTable(DataElementPath path)
    {
        TableDataCollection res = TableDataCollectionUtils.createTableDataCollection( path );
        res.getColumnModel().addColumn( "Site gains per mutation", DataType.Float );
        res.getColumnModel().addColumn( "Site losses per mutation", DataType.Float );
        for(String name : siteModelNames)
        {
            double siteGainFreq = siteGains.getOrDefault( name, 0 ) / (double)variantCount;
            double siteLossFreq = siteLosses.getOrDefault( name, 0 ) / (double)variantCount;
            TableDataCollectionUtils.addRow( res, name, new Object[] {siteGainFreq, siteLossFreq}, true );
        }
        res.finalizeAddition();
        path.save( res );
        return res;
    }
}