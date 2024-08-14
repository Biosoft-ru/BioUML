package biouml.plugins.keynodes;

import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;

/**
 * @author Ilya
 */
@ClassIcon ( "resources/regulator-target-search.gif" )
public class EnhanceScore extends AnalysisMethodSupport<EnhanceScoreParameters>
{
    public EnhanceScore(DataCollection<?> origin, String name)
    {
        super(origin, name, new EnhanceScoreParameters());
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection input = getParameters().getKeyNodeTable().getDataElement(TableDataCollection.class);
        TableDataCollection selected = getParameters().getSelectedProteins().getDataElement(TableDataCollection.class);
        DataElementPath path = getParameters().getResult();
        TableDataCollection result = input.clone(path.getParentCollection(), path.getName());
        String column = getParameters().getColumn();
        double enhancement = getParameters().getEnhancement();

        Set<String> selectedNodes = StreamEx.of(selected.getNameList()).toSet();
        List<String> selectedFromTable = StreamEx.of(result.getNameList()).filter(n -> selectedNodes.contains(n)).toList();

        if( selectedFromTable.size() == 0 )
            log.warning("No matched keys found, table will be not changed!");
        else
            log.info("Number of table rows to be enhanced: " + selectedFromTable.size());
        
        for( String key : selectedFromTable )
        {
            RowDataElement rde = result.get(key);
            double obj = Double.parseDouble( rde.getValue( column ).toString() );
            rde.setValue(column, obj + enhancement);
            result.put(rde);
        }

        result.getOrigin().put(result);
        return result;
    }
}
