package biouml.plugins.ensembl.analysis.mutationeffect;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.VCFSqlTrack;
import ru.biosoft.bsa.analysis.maos.Parameters;
import ru.biosoft.bsa.analysis.maos.ResultHandler;
import ru.biosoft.bsa.analysis.maos.SummaryStatistics;
import ru.biosoft.table.TableDataCollection;

public class ResultHandlerBySNP extends ResultHandler
{

    private Map<String, List<Site>> sitesByChr;
    public ResultHandlerBySNP(Parameters parameters, Logger analysisLog)
    {
        super( parameters, analysisLog );
    }

    public void setSites(Map<String, List<Site>> sitesByChr)
    {
        this.sitesByChr = sitesByChr;
    }

    @Override
    public void init()
    {
        DataElementPath chromosomesPath = parameters.getChromosomesPath();
        siteGainTrack = SqlTrack.createTrack( parameters.getSiteGainTrack(), null, chromosomesPath );
        siteLossTrack = SqlTrack.createTrack( parameters.getSiteLossTrack(), null, chromosomesPath );
        importantMutationsTrack = SqlTrack.createTrack( parameters.getImportantMutationsTrack(), null, chromosomesPath, VCFSqlTrack.class );
        summary = new SummaryStatistics( parameters.getSiteModelCollection(),
                ( (ParametersBySNP)parameters ).getSnpTable().getDataElement( TableDataCollection.class ).getSize() );
        createOutputTable();
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

        for( String chr : sitesByChr.keySet() )
        {
            for( Site site : sitesByChr.get( chr ) )
            {
                if( importantSiteIds.contains( site.getName() ) )
                    importantMutationsTrack.addSite( site );
            }
        }
        importantMutationsTrack.finalizeAddition();
        parameters.getImportantMutationsTrack().save( importantMutationsTrack );
    }

}
