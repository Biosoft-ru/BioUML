package ru.biosoft.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.aggregate.NumericAggregator;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@ClassIcon("resources/join-table.gif")
public class JoinTable extends AnalysisMethodSupport<JoinTableParameters>
{
    public JoinTable(DataCollection<?> origin, String name) throws Exception
    {
        super(origin, name, new JoinTableParameters());
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        validateParameters();
        TableDataCollection leftTable = parameters.getLeftGroup().getTable();
        TableDataCollection rightTable = parameters.getRightGroup().getTable();
        int joinType = parameters.getJoinTypeForAnalysis();
        DataElementPath output = parameters.getOutput();
        String[] leftColumns = parameters.getLeftGroup().getNames();
        String[] newLeftColumns = parameters.getLeftGroup().getNewNames();
        String[] rightColumns = parameters.getRightGroup().getNames();
        String[] newRightColumns = parameters.getRightGroup().getNewNames();

        boolean mergeColumns = parameters.isMergeColumns();
        NumericAggregator aggregator = null;
        String[] commonColumns;
        String[] commonColumns1 = null;
        String[] commonColumns2 = null;
        if(mergeColumns)
        {
            commonColumns = getCommonColumns(newLeftColumns, newRightColumns);
            if(commonColumns.length > 0)
            {
                int cl = commonColumns.length;
                String[] leftWOCommon = new String[leftColumns.length - cl];
                String[] rightWOCommon = new String[rightColumns.length - cl];
                String[] newLeftWOCommon = new String[leftColumns.length - cl];
                String[] newRightWOCommon = new String[rightColumns.length - cl];
                commonColumns1 = new String[cl];
                commonColumns2 = new String[cl];
                resolveCommonColumns(leftColumns, newLeftColumns, commonColumns, leftWOCommon, newLeftWOCommon, commonColumns1);
                resolveCommonColumns(rightColumns, newRightColumns, commonColumns, rightWOCommon, newRightWOCommon, commonColumns2);
                leftColumns = leftWOCommon;
                newLeftColumns = newLeftWOCommon;
                rightColumns = rightWOCommon;
                newRightColumns = newRightWOCommon;
                aggregator = parameters.getAggregator();
            }
        }
        else
        {
            //temporal fix just for web version where we can not rename columns during analysis
            resolveEqualNames(newLeftColumns, newRightColumns, " " + leftTable.getName(), " " + rightTable.getName());
            commonColumns = new String[0];
            commonColumns1 = new String[0];
            commonColumns2 = new String[0];
        }

        TableDataCollection result = TableDataCollectionUtils.join(joinType, leftTable, rightTable, output, leftColumns, rightColumns,
                newLeftColumns, newRightColumns, commonColumns, commonColumns1, commonColumns2, aggregator);
        if(jobControl.isStopped())
        {
            parameters.getOutput().remove();
            return null;
        }
        DataCollectionUtils.copyPersistentInfo(result, leftTable);
        return result;
    }


    public void resolveEqualNames(String[] names1, String[] names2, String suffix1, String suffix2)
    {
        for( int i = 0; i < names1.length; i++ )
        {
            for( int j = 0; j < names2.length; j++ )
            {
                if( names1[i].equalsIgnoreCase(names2[j]) )
                {
                    names1[i] += suffix1;
                    names2[j] += suffix2;
                }
            }
        }
    }
    
    private String[] getCommonColumns(String[] names1, String[] names2)
    {
        List<String> names = new ArrayList<>(Arrays.asList(names1));
        names.retainAll( Arrays.asList( names2 ) );
        return names.toArray( new String[names.size()] );
    }

    /**
     * Separate old column names into two tables: common (with other table) and unique
     * @param namesOldAll - all names old
     * @param namesNewAll - all names new (may be, renamed)
     * @param namesNewCommon - common new names
     * @param namesOld - own old names - empty array, will be filled
     * @param namesNew - own new names - empty array, will be filled
     * @param namesOldCommon - old names, mapped to namesNewCommon  - empty array, will be filled
     */
    private void resolveCommonColumns(String[] namesOldAll, String[] namesNewAll, String[] namesNewCommon, String[] namesOld, String[] namesNew,
            String[] namesOldCommon)
    {
        Set<String> c = new HashSet<>();
        for( int j = 0; j < namesNewCommon.length; j++ )
        {
            for( int i = 0; i < namesOldAll.length; i++ )
            {
                if( namesNewAll[i].equalsIgnoreCase(namesNewCommon[j]) )
                {
                    namesOldCommon[j] = namesOldAll[i];
                    c.add(namesOldAll[i]);
                }
            }
        }
        int j = 0;
        for( int i = 0; i < namesOldAll.length; i++ )
        {
            if( !c.contains(namesOldAll[i]) )
            {
                namesOld[j] = namesOldAll[i];
                namesNew[j] = namesNewAll[i];
                j++;
            }
        }
    }

}
