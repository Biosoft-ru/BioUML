package ru.biosoft.analysis;

import static ru.biosoft.table.TableDataCollectionUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.analysis.aggregate.NumericSelector;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.Clazz;
import ru.biosoft.util.TextUtil;

/**
 * Supporting class for TableConverter-like analyses
 * @author lan
 */
public abstract class TableConverterSupport<T extends AbstractTableConverterParameters> extends AnalysisMethodSupport<T>
{
    public TableConverterSupport(DataCollection<?> origin, String name, T parameters)
    {
        super(origin, name, parameters);
    }
    
    abstract protected String getSourceColumnName();

    protected Map<String, Set<String>> revertReferences(Map<String, String[]> references)
    {
        if(references == null) return null;
        return EntryStream.of( references ).invert().nonNullKeys().flatMapKeys( Arrays::stream ).groupingTo( TreeSet::new );
    }

    protected void fillTable(final TableDataCollection source, Map<String, Set<String>> revReferences, final TableDataCollection result) throws Exception
    {
        final ColumnModel oldCm = source.getColumnModel();
        final ColumnModel newCm = result.getColumnModel();
        initColumns(source, oldCm, newCm);
        jobControl.forCollection(revReferences.entrySet(), element -> {
            List<RowDataElement> rows = new ArrayList<>();
            for(String rowID: element.getValue())
            {
                try
                {
                    rows.add(source.get(rowID));
                }
                catch( Exception e )
                {
                }
            }
            int index = oldCm.optColumnIndex(parameters.getColumnName());
            Object[] values;
            if(parameters.isColumnSpecified() && index >= 0)
            {
                int rowIdx = ( (NumericSelector)parameters.getAggregator() ).select( getValuesSupplier( rows, index ).get()
                        .mapToDouble( Clazz.of( Number.class ).toDouble( Number::doubleValue, Double.NaN ) ).toArray() );
                RowDataElement row = rows.get(rowIdx);
                if(parameters.isOutputSourceIds())
                    values = StreamEx.of(row.getValues()).prepend( row.getName() ).toArray();
                else
                    values = row.getValues();
            } else
            {
                int j=0;
                values = new Object[newCm.getColumnCount()];
                if(parameters.isOutputSourceIds())
                    values[j++] = String.join(ID_SEPARATOR, element.getValue());
                for(int colNum = 0; colNum < oldCm.getColumnCount(); colNum++)
                {
                    TableColumn column = oldCm.getColumn(colNum);
                    if( !isTypeSupported( column.getValueClass() ) )
                        continue;
                    TableColumn newColumn = newCm.getColumn(j);
                    values[j] = mergeValues( getValuesSupplier( rows, colNum ), column, newColumn );
                    j++;
                }
            }
            TableDataCollectionUtils.addRow(result, element.getKey(), values, true);
            return true;
        });
        result.finalizeAddition();
    }

    protected Supplier<StreamEx<Object>> getValuesSupplier(List<RowDataElement> rows, int colNum)
    {
        return () -> StreamEx.of( rows ).map( row -> row.getValues()[colNum] );
    }

    protected Object mergeValues(Supplier<StreamEx<Object>> valuesSupplier, TableColumn column, TableColumn newColumn)
    {
        Class<?> valueClass = column.getValueClass();
        Object value = null;
        if( valueClass == String.class )
        {
            value = valuesSupplier.get().select( String.class ).filter( TextUtil::nonEmpty )
                    .flatMap( val -> StreamEx.split( val, ID_SEPARATOR_CHAR ) ).map( String::trim ).sorted().distinct()
                    .joining( ID_SEPARATOR );
        }
        else if( valueClass == StringSet.class )
        {
            value = valuesSupplier.get().select( StringSet.class ).flatMap( Set::stream ).sorted( String::compareTo )
                    .toCollection( StringSet::new );
        }
        else if( column.getType().isNumeric() )
        {
            value = newColumn.getType().convertValue(
                    parameters.getAggregator().aggregate(
                            valuesSupplier.get().mapToDouble( Clazz.of( Number.class ).toDouble( Number::doubleValue, Double.NaN ) )
                                    .toArray() ) );
        }
        else if( valueClass == DataElementPathSet.class )
        {
            value = valuesSupplier.get().select( DataElementPathSet.class ).flatMap( DataElementPathSet::stream )
                    .toCollection( DataElementPathSet::new );
        }
        else if( valueClass == ru.biosoft.access.core.DataElementPath.class )
        {
            value = valuesSupplier.get().select( DataElementPath.class ).toCollection( DataElementPathSet::new );
        }
        return value;
    }

    private void initColumns(TableDataCollection source, ColumnModel oldCm, ColumnModel newCm)
    {
        if( parameters.isOutputSourceIds() )
        {
            TableColumn idColumn = newCm.addColumn( oldCm.generateUniqueColumnName( getSourceColumnName() ), String.class );
            if( source.getReferenceType() != null )
                idColumn.setValue( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, source.getReferenceType() );
        }
        for( TableColumn tc : oldCm )
        {
            Class<?> valueClass = tc.getValueClass();
            if( !isTypeSupported( valueClass ) && !parameters.isColumnSpecified() )
            {
                log.warning("Column '" + tc.getDisplayName() + "' has type " + tc.getType()
                        + " which cannot be merged: it will be removed from output.");
                continue;
            }
            TableColumn column = newCm.addColumn(newCm.cloneTableColumn(tc));
            if( valueClass == Integer.class && ! ( parameters.getAggregator() instanceof NumericSelector ) )
                column.setValueClass( Double.class );
            else if( valueClass == ru.biosoft.access.core.DataElementPath.class )
                column.setValueClass( DataElementPathSet.class );
            else
                column.setValueClass( valueClass );
        }
    }

    private boolean isTypeSupported(Class<?> type)
    {
        return type == String.class || type == Integer.class || type == Double.class || type == StringSet.class
                || type == ru.biosoft.access.core.DataElementPath.class || type == DataElementPathSet.class;
    }
}
