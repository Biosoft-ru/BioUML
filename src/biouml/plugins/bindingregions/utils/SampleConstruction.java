
package biouml.plugins.bindingregions.utils;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.Maps;

/**
 * @author yura
 *
 */
public class SampleConstruction
{
    private final Map<String, TDoubleList> nameAndSample;
    public String commonName;
    
    public SampleConstruction(Map<String, TDoubleList> nameAndSample, String commonName)
    {
        this.nameAndSample = nameAndSample;
        this.commonName = commonName;
    }
 
    public SampleConstruction(TableDataCollection table, String commonName, String equalityColumnName, int equality, String inequalityColumnName, int minimalSize)
    {
        List<String> columnNames = selectColumnNames(table, commonName);
        this.nameAndSample = getSamples(table, columnNames, equalityColumnName, equality, inequalityColumnName, minimalSize);
        this.commonName = commonName;
    }

    public SampleComparison transformToSampleComparison()
    {
        Map<String, double[]> map = Maps.transformValues( nameAndSample, TDoubleList::toArray );
        return new SampleComparison(map, commonName);
    }
    
    private Map<String, TDoubleList> getNameAndSample()
    {
        return nameAndSample;
    }
    
    /***
     * Selection of column names that contain specific 'subname'
     * @param table
     * @param subname
     * @return column names that contain specific subname
     */
    private List<String> selectColumnNames(TableDataCollection table, String subname)
    {
        return table.columns().map( TableColumn::getName ).filter( name -> name.contains( subname ) ).toList();
    }
    
    /***
     * Read samples from given table columns under 2 additional conditions:
     * a) elements in other column with the name 'equalityColumnName' must be equal to integer value 'equality';
     * b) elements in other column with the name 'inequalityColumnName' must be greater or equal to integer value 'minimalSize';
     * @param table
     * @param columnNameSubset - names of columns that contain elements of samples;
     * @param equalityColumnName
     * @param equality
     * @param inequalityColumnName
     * @param minimalSize
     * @return Map with samples; all samples have the same size
     */
    private static Map<String, TDoubleList> getSamples(TableDataCollection table, List<String> columnNameSubset, String equalityColumnName, int equality, String inequalityColumnName, int minimalSize)
    {
        if( table == null ) return null;
        Map<String, TDoubleList> result = new HashMap<>();
        int m = columnNameSubset.size();
        int[] columnIndexes = new int[m];
        int equalityIndex = table.getColumnModel().getColumnIndex(equalityColumnName);
        int inequalityIndex = table.getColumnModel().getColumnIndex(inequalityColumnName);
        if( equalityIndex < 0 || inequalityIndex < 0 ) return null;
        for( int i = 0; i < m; i++ )
        {
            columnIndexes[i] = table.getColumnModel().getColumnIndex(columnNameSubset.get(i));
            if( columnIndexes[i] < 0 ) return null;
        }
        for( int j = 0; j < table.getSize(); j++ )
        {
            if( equality != (Integer)table.getValueAt(j, equalityIndex) ) continue;
            if( (Integer)table.getValueAt(j, inequalityIndex) < minimalSize ) continue;
            for( int i = 0; i < m; i++ )
            {
                String columnName = columnNameSubset.get(i);
                TDoubleList list = result.containsKey(columnName) ? result.get(columnName) : new TDoubleArrayList();
                list.add(((Number)table.getValueAt(j, columnIndexes[i])).doubleValue());
                result.put(columnName, list);
            }
        }
        if( result.isEmpty() ) return null;
        else return result;
    }

    public void subjointSamples(TableDataCollection table, String equalityColumnName, int equality, String inequalityColumnName, int minimalSize)
    {
        SampleConstruction newSC = new SampleConstruction(table, this.commonName, equalityColumnName, equality, inequalityColumnName, minimalSize);
        Map<String, TDoubleList> map = newSC.getNameAndSample();
        if( map != null )
            for( Entry<String, TDoubleList> entry : nameAndSample.entrySet() )
                entry.getValue().addAll(map.get(entry.getKey()));
    }
}
