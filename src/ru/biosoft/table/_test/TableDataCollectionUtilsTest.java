package ru.biosoft.table._test;

import ru.biosoft.table.TableDataCollectionUtils;
import junit.framework.TestCase;

public class TableDataCollectionUtilsTest extends TestCase
{
    public void testGetStringDescription()
    {
        assertEquals("", TableDataCollectionUtils.getStringDescription( null ));
        assertEquals("2-4", TableDataCollectionUtils.getStringDescription( 1, 2, 3 ));
        assertEquals("2-4,6", TableDataCollectionUtils.getStringDescription( 1, 2, 3, 5 ));
        assertEquals("2,3,6,7,9", TableDataCollectionUtils.getStringDescription( 5, 2, 1, 8, 6 ));
    }
}
