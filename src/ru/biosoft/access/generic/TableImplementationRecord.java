package ru.biosoft.access.generic;

import one.util.streamex.StreamEx;

import ru.biosoft.table.FileTableDataCollection;
import ru.biosoft.table.SqlTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class TableImplementationRecord
{
    
    public static final String SQL_TABLES_IMPL = "SQL";
    public static final String FILE_TABLES_IMPL = "File";

    private static TableImplementationRecord[] TABLE_IMPLEMENTATIONS = {
            new TableImplementationRecord( FILE_TABLES_IMPL, FileTableDataCollection.class ),
            new TableImplementationRecord( SQL_TABLES_IMPL, SqlTableDataCollection.class ),
    };

    private String name;
    private Class<? extends TableDataCollection> tableClass;
    
    private TableImplementationRecord(String name, Class<? extends TableDataCollection> tableClass)
    {
        super();
        this.name = name;
        this.tableClass = tableClass;
    }
    
    public Class<? extends TableDataCollection> getTableClass()
    {
        return tableClass;
    }

    @Override
    public String toString()
    {
        return name;
    }

    public static TableImplementationRecord getPreferedTableImplementation(String name)
    {
        return StreamEx.of( TABLE_IMPLEMENTATIONS ).findAny( t -> t.toString().equals( name ) ).orElse( TABLE_IMPLEMENTATIONS[0] );
    }

    public static class TableImplementationSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return getTableImplementations();
        }
    }

    public static TableImplementationRecord[] getTableImplementations()
    {
        return TABLE_IMPLEMENTATIONS.clone();
    }
}