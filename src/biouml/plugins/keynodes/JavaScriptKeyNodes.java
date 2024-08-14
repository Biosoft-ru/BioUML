package biouml.plugins.keynodes;

import biouml.plugins.keynodes.biohub.KeyNodesHub;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.plugins.javascript.JSAnalysis;
import ru.biosoft.plugins.javascript.JSProperty;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.table.TableDataCollection;

public class JavaScriptKeyNodes extends JavaScriptHostObjectBase
{
    public JavaScriptKeyNodes()
    {
    }
    
    /**
     * Performs master regulator node analysis
     * @param sourcePath - collection containing input molecules
     * @param outputName - name of output table with complete path
     * @param direction - search direction
     * @param maxRadius - maximal search radius
     * @param bioHub - name of search hub
     * @param calculatingFDR - true, if need to calculate FDR
     * @throws Exception
    */
    @JSAnalysis(KeyNodeAnalysis.class)
    public void regulatorNodes(
            @JSProperty("sourcePath") TableDataCollection molecules,
            @JSProperty("outputTable") String outputName,
            @JSProperty("direction") String direction,
            @JSProperty("maxRadius") int maxRadius,
            @JSProperty("bioHub") String bioHub,
            @JSProperty("calculatingFDR") boolean calculateFDR,
            @JSProperty("penalty") double penalty) throws Exception
    {
        KeyNodeAnalysis analysis  = new KeyNodeAnalysis(null, "");
        KeyNodeAnalysisParameters parameters = analysis.getParameters();
        parameters.setSource(molecules);
        parameters.setOutputTable(DataElementPath.create(outputName));
        parameters.setDirection(direction);
        parameters.setMaxRadius(maxRadius);
        parameters.setPenalty(penalty);
        BioHubInfo hub = BioHubRegistry.getBioHubInfo(bioHub);
        if(hub != null && hub.getBioHub() instanceof KeyNodesHub)
            parameters.setBioHub(hub);
        parameters.setCalculatingFDR(calculateFDR);
        analysis.justAnalyzeAndPut();
    }
    
    @JSAnalysis(SaveNetworkAnalysis.class)
    public void saveNetworks(
            @JSProperty("knResultPath") DataCollection<?> keyNodesResult,
            @JSProperty("rankColumn") String rankColumn,
            @JSProperty("numTopRanking") int nTopRanking,
            @JSProperty("outputPath") String outputPath) throws Exception
    {
        SaveNetworkAnalysis analysis = new SaveNetworkAnalysis(null, "");
        SaveNetworkAnalysisParameters parameters = analysis.getParameters();
        parameters.setKnResultPath(DataElementPath.create(keyNodesResult));
        parameters.setRankColumn(rankColumn);
        parameters.setNumTopRanking(nTopRanking);
        parameters.setOutputPath(DataElementPath.create(outputPath));
        analysis.validateParameters();
        analysis.justAnalyzeAndPut();
    }
}
