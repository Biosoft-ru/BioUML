
package ru.biosoft.access.search._test;

import junit.framework.TestCase;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.search.DataSearch;

public class DataSearchTest extends TestCase
{
    private static final String PATH = "../data";
    public DataSearchTest(String name)
    {
        super(name);
    }

    public void testDataSearch() throws Exception
    {
        CollectionFactory.createRepository(PATH);
        DataCollection dc = CollectionFactory.getDataCollection("databases/KEGG pathways/Data/compound");
        DataSearch dataSearch = new DataSearch("Data Search", dc);
        dataSearch.pack();
        dataSearch.show();
    }
}
