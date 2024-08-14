package ru.biosoft.galaxy.parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import ru.biosoft.galaxy.filters.Filter;

public class SelectParameter extends ParameterSupport
{
    protected static final Logger log = Logger.getLogger( SelectParameter.class.getName() );

    private static final BiFunction<List<String[]>, Filter, List<String[]>> FILTER_FUNCTION = (list, filter) -> filter.filter( list );

    protected boolean multiple;
    protected Set<String> selected = new LinkedHashSet<>();
    protected Map<String, Integer> columnNames = new HashMap<>();
    protected List<Filter> filters = new ArrayList<>();
    protected int independentFilters = 0;
    protected List<String[]> cachedOptions = null;

    public SelectParameter(boolean output, boolean multiple)
    {
        super( output );
        this.multiple = multiple;
    }

    public SelectParameter(boolean output)
    {
        this( output, false );
        fields.put( "value", new JSONArray() );
    }

    public void addColumnIndex(String columnId, int index)
    {
        columnNames.put( columnId, index );
    }

    public int getColumnIndex(String columnId)
    {
        if( columnNames.containsKey( columnId ) )
            return columnNames.get( columnId );
        if( columnId.equals( "name" ) )
            return 0;
        if( columnId.equals( "value" ) )
            return 1;
        return Integer.parseInt( columnId );
    }

    public void addFilter(Filter filter)
    {
        if( filters.size() == independentFilters )
        {
            String[] dependencies = filter.getDependencies();
            if( dependencies == null || dependencies.length == 0 )
                independentFilters++;
        }
        filters.add( filter );
    }

    public String[] getDependencies()
    {
        return StreamEx.of( filters ).map( Filter::getDependencies ).nonNull().flatMap( Arrays::stream ).distinct().toArray( String[]::new );
    }

    public boolean isMultiple()
    {
        return multiple;
    }

    public void setMultiple(boolean multiple)
    {
        this.multiple = multiple;
    }

    private List<String[]> getAllOptions()
    {
        if( cachedOptions == null )
        {
            cachedOptions = IntStreamEx.range( independentFilters ).mapToObj( filters::get ).foldLeft( new ArrayList<>(), FILTER_FUNCTION );
        }
        return IntStreamEx.range( independentFilters, filters.size() ).mapToObj( filters::get ).foldLeft( cachedOptions, FILTER_FUNCTION );
    }

    public Map<String, String> getOptions()
    {
        Map<String, String> result = new LinkedHashMap<>();
        int nameColumn = getColumnIndex( "name" );
        int valueColumn = getColumnIndex( "value" );
        List<String[]> options = getAllOptions();
        for( String[] option : options )
            result.put( option[valueColumn], option[nameColumn] );
        return result;
    }
    
    private StreamEx<String> values(String value)
    {
        if( multiple )
            return value.isEmpty() ? StreamEx.empty() : StreamEx.split(value, ',');
        else
            return StreamEx.of( value );
    }

    public void setValueValidated(String value)
    {
        Map<String, String> options = getOptions();
        selected.clear();
        values(value).filter( options::containsKey ).forEach( selected::add );
        updateFields();
    }

    @Override
    public void setValue(String value)
    {
        selected.clear();
        values(value).forEach( selected::add );
        updateFields();
    }

    /**
     * @param value     name or value of option for single select,
     *                  option names or values separated by comma for multiple select
     */
    @Override
    public void setValueFromTest(String value)
    {
        selected.clear();
        values(value).forEach( this::addValueFromTest );
        updateFields();
    }

    protected void addValueFromTest(String value)
    {
        Map<String, String> options = getOptions();
        if( !options.containsKey( value ) )
            value = StreamEx.ofKeys( options, value::equals ).findFirst().orElse( value );
        selected.add( value );
    }

    protected void updateFields()
    {
        if( multiple )
        {
            JSONArray jsonArray = new JSONArray( selected );
            fields.put( "value", jsonArray );

            int valueColumn = getColumnIndex( "value" );
            List<String[]> selectedRows = StreamEx.of( getAllOptions() ).filter( row -> selected.contains( row[valueColumn] ) ).toList();

            Map<String, JSONArray> columnValues = EntryStream.of( columnNames )
                .mapValues( idx -> StreamEx.of( selectedRows ).map( row -> row[idx] ).toList() )
                .mapValues( JSONArray::new )
                .toMap();
            fields.put( "fields", new JSONObject( columnValues ) );
        }
        else if( !selected.isEmpty() )
        {
            String selectedValue = selected.iterator().next();
            fields.put( "value", selectedValue );

            int valueColumn = getColumnIndex( "value" );
            String[] selectedRow = StreamEx.of( getAllOptions() ).findFirst( row -> row[valueColumn].equals( selectedValue ) )
                    .orElse( null );
            if( selectedRow == null )
            {
                log.warning( "invalid value '" + selectedValue + "' for select parameter" );
                return;
            }
            Map<String, String> columns = EntryStream.of( columnNames ).mapValues( idx -> selectedRow[idx] ).toMap();
            fields.put( "fields", new JSONObject( columns ) );
        }
    }

    @Override
    protected void doCloneParameter(ParameterSupport clone)
    {
        super.doCloneParameter( clone );
        SelectParameter result = (SelectParameter)clone;
        for( Filter filter : filters )
            result.filters.add( filter.clone( result ) );
        result.columnNames = new HashMap<>( columnNames );
        result.independentFilters = independentFilters;
        result.cachedOptions = cachedOptions;
        result.selected = new LinkedHashSet<>( selected );
    }

    @Override
    public Parameter cloneParameter()
    {
        SelectParameter result = new SelectParameter( output, multiple );
        doCloneParameter( result );
        return result;
    }

    @Override
    public String toString()
    {
        return String.join( ",", selected );
    }
}
