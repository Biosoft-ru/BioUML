package ru.biosoft.table.export;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.Column;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.table.columnbeans.ColumnsEditor;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

/**
 * This is new-style exporter of common DataElementExporter interface
 * @author lan
 */
public class TableElementExporter implements DataElementExporter
{
    protected TableExporterProperties properties = null;
    protected TableExportTransformer transformer = null;

    @Override
    public int accept(DataElement de)
    {
        if( de instanceof TableDataCollection )
            return DataElementExporter.ACCEPT_MEDIUM_PRIORITY;
        return DataElementExporter.ACCEPT_UNSUPPORTED;
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( TableDataCollection.class );
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file) throws Exception
    {
        doExport(de, file, null);
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file, FunctionJobControl jobControl) throws Exception
    {
        TableDataCollection tdc = (TableDataCollection)de;
        prepareProperties( tdc );
        
        ColumnModel model = tdc.getColumnModel();
        if(!properties.getSortOrder().equals( SortOrderSelector.NOT_SORTED ))
        {
            String columnName = properties.getSortColumn();
            if( columnName != null && !columnName.isEmpty() )
            {
                int columnNumber = model.getColumnIndex( columnName );
                tdc.sortTable( columnNumber, properties.getSortOrder().equals( SortOrderSelector.ASCENDING ) );
            }
        }
        
        try (OutputStream os = new FileOutputStream( file ))
        {
            transformer.setOutputStream( os );
            transformer.setDataElement( de );
            transformer.writeHeader( de.getName() );
            Column[] columns = properties.getColumns();
            if( properties.isIncludeHeaders() )
            {
                transformer.writeColumnSectionStart();
                if( properties.isIncludeIds() )
                {
                    transformer.writeColumnTitle( "ID" );
                    transformer.writeColumnTitleSeparator();
                }
                boolean first = true;
                for( Column column : columns )
                {
                    if( first )
                        first = false;
                    else
                        transformer.writeColumnTitleSeparator();
                    transformer.writeColumnTitle( column.getNewName() );
                }
                transformer.writeColumnSectionEnd();
            }
            transformer.writeDataSectionStart();
            if( properties.getLastRow() > tdc.getSize() )
                properties.setLastRow( tdc.getSize() );
            int rowNumber = 0;
            int size = properties.getLastRow();
            for( RowDataElement rde : tdc )
            {
                rowNumber++;
                if( rowNumber < properties.getFirstRow() )
                    continue;
                if( rowNumber > properties.getLastRow() )
                    break;
                if( properties.isIncludeIds() )
                {
                    transformer.writeData( rde.getName() );
                    transformer.writeDataSeparator();
                }
                boolean first = true;
                for( Column column : columns )
                {
                    Object value = rde.getValues()[model.getColumnIndex( column.getName() )];
                    if( first )
                        first = false;
                    else
                        transformer.writeDataSeparator();
                    transformer.writeData( ( value == null ? "" : value.toString().replaceAll( "[\t\n]", " " ) ) );
                }
                transformer.writeLineSeparator();
                if( jobControl != null && rowNumber % 1000 == 0 )
                {
                    jobControl.setPreparedness( (int)Math.floor( rowNumber * 100. / size ) );
                    if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    {
                        file.delete();
                        return;
                    }
                }
            }
            transformer.writeFooter();
        }
    }

    protected void prepareProperties(TableDataCollection tdc)
    {
        if(properties == null || properties.getTable() != tdc)
        {
            properties = new TableExporterProperties();
            properties.setTable(tdc);
        }
    }
    
    @Override
    public Object getProperties(DataElement de, File file)
    {
        if(properties != null)
            return properties;
        if(!(de instanceof TableDataCollection)) return null;
        properties = new TableExporterProperties();
        properties.setTable((TableDataCollection)de);
        return properties;
    }

    @Override
    public boolean init(Properties properties)
    {
        if( properties.getProperty(DataElementExporterRegistry.SUFFIX).equals("txt") )
        {
            transformer = new TabSeparatedExportTransformer();
            return true;
        }
        if( properties.getProperty(DataElementExporterRegistry.SUFFIX).equals("csv") )
        {
            transformer = new CSVExportTransformer();
            return true;
        }
        if( properties.getProperty(DataElementExporterRegistry.SUFFIX).equals("html") )
        {
            transformer = new HTMLExportTransformer();
            return true;
        }
        return false;
    }
    
    public static class TableExporterProperties extends Option implements PropertyChangeListener
    {
        int firstRow, lastRow;
        Column[] columns;
        private TableDataCollection table;
        boolean includeIds;
        boolean includeHeaders = true;
        String sortOrder = SortOrderSelector.NOT_SORTED;
        String sortColumn;
        
        private TableExporterProperties()
        {
        }
        
        private void setTable(TableDataCollection table)
        {
            this.table = table;
            firstRow = 1;
            lastRow = table.getSize();
            includeIds = !table.getInfo().getProperties().getProperty(TableDataCollection.GENERATED_IDS, "").equals("true");
            Column[] columns = new Column[table.getColumnModel().getColumnCount()];
            for(int i=0; i<columns.length; i++) columns[i] = new Column(this, table.getColumnModel().getColumn(i).getName());
            setColumns(columns);
        }
        
        public TableDataCollection getTable()
        {
            return table;
        }
        
        public DataElementPath getTablePath()
        {
            return DataElementPath.create( table );
        }
        
        public int getFirstRow()
        {
            return firstRow;
        }
        public void setFirstRow(int firstRow)
        {
            this.firstRow = firstRow;
        }
        public int getLastRow()
        {
            return lastRow;
        }
        public void setLastRow(int lastRow)
        {
            this.lastRow = lastRow;
        }
        public Column[] getColumns()
        {
            return columns;
        }
        public void setColumns(Column[] columns)
        {
            this.columns = columns;
            for(Column column: columns)
            {
                column.removePropertyChangeListener(this);
                column.addPropertyChangeListener(this);
            }
            firePropertyChange("*", null, null);
        }
        public String calcColumnName(Integer index, Object column)
        {
            return ( (Column)column ).getName();
        }

        @Override
        public void propertyChange(PropertyChangeEvent event)
        {
            if(event.getSource() instanceof Column)
            {
                for(int i=0; i<columns.length; i++)
                {
                    if(((Column)event.getSource()).getName().equals(columns[i].getName()))
                    {
                        columns[i] = (Column)event.getSource();
                        break;
                    }
                }
            }
        }

        public boolean isIncludeIds()
        {
            return includeIds;
        }

        public void setIncludeIds(boolean includeIds)
        {
            Object oldValue = this.includeIds;
            this.includeIds = includeIds;
            firePropertyChange("includeIds", oldValue, includeIds);
        }

        public boolean isIncludeHeaders()
        {
            return includeHeaders;
        }

        public void setIncludeHeaders(boolean includeHeaders)
        {
            Object oldValue = this.includeHeaders;
            this.includeHeaders = includeHeaders;
            firePropertyChange("includeHeaders", oldValue, includeHeaders);
        }

        public String getSortOrder()
        {
            return sortOrder;
        }

        public void setSortOrder(String sortOrder)
        {
            Object oldValue = this.sortOrder;
            this.sortOrder = sortOrder;
            firePropertyChange( "sortOrder", oldValue, sortOrder );
        }

        public String getSortColumn()
        {
            return sortColumn;
        }

        public void setSortColumn(String sortColumn)
        {
            Object oldValue = this.sortColumn;
            this.sortColumn = sortColumn;
            firePropertyChange( "sortColumn", oldValue, sortColumn );
        }
        
        
    }
    
    public static class TableExporterMessageBundle extends ListResourceBundle
    {
        @Override
        protected Object[][] getContents() { return new Object[][] {
            { "CN_CLASS", "Export parameters"},
            { "CD_CLASS", "Export parameters"},
            { "PN_COLUMN_NEW_NAME", "new names"},
            { "PD_COLUMN_NEW_NAME", "new names"},
            { "PN_COLUMNS", "Columns"},
            { "PD_COLUMNS", "Columns"},
            { "PN_COLUMN_NAME", "name"},
            { "PD_COLUMN_NAME", "name"},
            { "PN_FIRST_ROW", "First row"},
            { "PD_FIRST_ROW", "Export from this row"},
            { "PN_LAST_ROW", "Last row"},
            { "PD_LAST_ROW", "Export up to this row"},
            { "PN_INCLUDE_IDS", "Include ids"},
            { "PD_INCLUDE_IDS", "Row ID column will be included"},
            { "PN_INCLUDE_HEADERS", "Include headers"},
            { "PD_INCLUDE_HEADERS", "Headers will be exported too"},
            { "PN_SORT_ORDER", "Sort order" },
            { "PD_SORT_ORDER", "Order of table rows" },
            { "PN_SORT_COLUMN", "Sort column" },
            { "PD_SORT_COLUMN", "Column used for sorting table rows" },
        }; }
    }

    public static class TableExporterPropertiesBeanInfo extends BeanInfoEx
    {
        public TableExporterPropertiesBeanInfo()
        {
            super(TableExporterProperties.class, TableExporterMessageBundle.class.getName());
            beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
            beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
        }
        
        @Override
        public void initProperties() throws Exception
        {
            addHidden( new PropertyDescriptor( "tablePath", beanClass, "getTablePath", null ) );
            add(new PropertyDescriptorEx("firstRow", beanClass), getResourceString("PN_FIRST_ROW"), getResourceString("PD_FIRST_ROW"));
            add(new PropertyDescriptorEx("lastRow", beanClass), getResourceString("PN_LAST_ROW"), getResourceString("PD_LAST_ROW"));
            PropertyDescriptorEx pde = new PropertyDescriptorEx("columns", beanClass);
            pde.setChildDisplayName(beanClass.getMethod("calcColumnName", new Class[] {Integer.class, Object.class}));
            pde.setPropertyEditorClass(ColumnsEditor.class);
            pde.setValue("useAllColumns", false);
            add(pde, getResourceString("PN_COLUMNS"), getResourceString("PD_COLUMNS"));
            add(new PropertyDescriptorEx("includeIds", beanClass), getResourceString("PN_INCLUDE_IDS"), getResourceString("PD_INCLUDE_IDS"));
            add(new PropertyDescriptorEx("includeHeaders", beanClass), getResourceString("PN_INCLUDE_HEADERS"), getResourceString("PD_INCLUDE_HEADERS"));
            
            pde = new PropertyDescriptorEx( "sortOrder", beanClass );
            pde.setPropertyEditorClass( SortOrderSelector.class );
            add( pde, getResourceString( "PN_SORT_ORDER" ), getResourceString( "PD_SORT_ORDER" ) );

            add( ColumnNameSelector.registerSelector( "sortColumn", beanClass, "tablePath", false ), getResourceString( "PN_SORT_COLUMN" ),
                    getResourceString( "PD_SORT_COLUMN" ) );
        }
    }
    
    public static class SortOrderSelector extends GenericComboBoxEditor
    {
        public static final String NOT_SORTED = "Not sorted";
        public static final String ASCENDING = "Ascending";
        public static final String DESCENDING = "Descending";

        @Override
        protected String[] getAvailableValues()
        {
            return new String[] {NOT_SORTED, ASCENDING, DESCENDING};
        }
    }
}
