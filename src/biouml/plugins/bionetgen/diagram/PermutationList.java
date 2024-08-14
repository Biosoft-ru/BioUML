package biouml.plugins.bionetgen.diagram;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class PermutationList<T> extends AbstractList<List<T>>
{
    private int size;
    private final List<List<T>> data;

    public PermutationList(List<List<T>> data)
    {
        size = 1;
        this.data = data;
        for( List<T> dimension : data )
        {
            size *= dimension.size();
        }
    }

    @Override
    public List<T> get(int index)
    {
        List<T> result = new ArrayList<>( data.size() );
        for( List<T> dimension : data )
        {
            result.add(dimension.get(index % dimension.size()));
            index /= dimension.size();
        }
        return result;
    }

    @Override
    public int size()
    {
        return size;
    }
}