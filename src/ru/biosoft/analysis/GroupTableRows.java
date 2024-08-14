package ru.biosoft.analysis;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@ClassIcon("resources/GroupTableRows.png")
public class GroupTableRows extends AnalysisMethodSupport<GroupTableRows.Parameters>
{

    public GroupTableRows(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection input = parameters.getInputTable().getDataElement(TableDataCollection.class);

        jobControl.pushProgress( 0, 50 );
        DataType columnType = input.getColumnModel().getColumn( parameters.getColumnName() ).getType();
        final Map<Object, List<RowDataElement>> groups = FilterDuplicateRows.groupTableRows( input, parameters.getColumnName(), jobControl );
        jobControl.popProgress();

        final TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        result.getColumnModel().addColumn( parameters.getColumnName(), columnType );
        result.getColumnModel().addColumn( "Input table IDs", StringSet.class );
        result.getColumnModel().addColumn( "Count", Integer.class );

        jobControl.pushProgress( 50, 95 );
        AtomicInteger autoID = new AtomicInteger();
        jobControl.forCollection( groups.keySet(), id -> {
            int count = groups.get( id ).size();
            Set<String> ids = groups.get( id ).stream().map( rde -> rde.getName() ).collect( Collectors.toSet() );
            String autoIDStr = Integer.toString( autoID.incrementAndGet() );
            TableDataCollectionUtils.addRow( result, autoIDStr, new Object[] {id, new StringSet( ids ), count}, true );
            return true;
        } );
        jobControl.popProgress();

        result.finalizeAddition();
        parameters.getOutputTable().save( result );
        return result;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTable, outputTable;
        private String columnName;

        @PropertyName ( "Input table" )
        @PropertyDescription ( "Input table" )
        public DataElementPath getInputTable()
        {
            return inputTable;
        }
        public void setInputTable(DataElementPath inputTable)
        {
            Object oldValue = this.inputTable;
            this.inputTable = inputTable;
            firePropertyChange( "inputTable", oldValue, inputTable );
        }
        @PropertyName ( "Output table" )
        @PropertyDescription ( "Resulting table" )
        public DataElementPath getOutputTable()
        {
            return outputTable;
        }
        public void setOutputTable(DataElementPath outputTable)
        {
            Object oldValue = this.outputTable;
            this.outputTable = outputTable;
            firePropertyChange( "outputTable", oldValue, outputTable );
        }

        @PropertyName ( "Group column" )
        @PropertyDescription ( "Rows will be grouped by this column" )
        public String getColumnName()
        {
            return columnName;
        }
        public void setColumnName(String columnName)
        {
            Object oldValue = this.columnName;
            this.columnName = columnName;
            firePropertyChange( "columnName", oldValue, columnName );
        }


    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            property( "inputTable" ).inputElement( TableDataCollection.class ).add();

            add( ColumnNameSelector.registerSelector( "columnName", beanClass, "inputTable", false ) );

            property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$inputTable$ grouped" ).add();
        }
    }

}
