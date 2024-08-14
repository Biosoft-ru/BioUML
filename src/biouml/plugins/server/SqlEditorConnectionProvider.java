package biouml.plugins.server;

import java.util.Map;

import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;

public interface SqlEditorConnectionProvider
{
    public Map<String, ColumnModel> getTablesStructure()  throws Exception;
    public void fillResultTable(String query, TableDataCollection tableDataCollection) throws Exception;
    public String getServerHost();
    public void close();
    public String getInfo();
}
