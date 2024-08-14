package ru.biosoft.access._test;

import java.io.File;
import java.util.Iterator;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;

/**
 * @todo implement and derive all file antry data collections from this class
 */
abstract public class FileEntryCollectionTest extends DataCollectionTest
{
    /**
     * @todo
     */
    @Override
    protected String getOriginalName()
    {
        return "";
    }

    public void restoreFiles()  throws Exception
    {
        /** @todo change delimiter */
        String fileName = properties.getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, ".")  + '/' +
                          properties.getProperty(DataCollectionConfigConstants.FILE_PROPERTY);

        // restore data file and all other files
        ApplicationUtils.copyFile(fileName, fileName+".orig");

        // delete index file
        String idName = properties.getProperty("entry.id");
        if( idName.equalsIgnoreCase(">") )
            idName = "id";

        File indexFile = new File(fileName+"."+idName);
        indexFile.delete();
        //assertTrue("index \"" + indexFile.getAbsolutePath() + "\" is not deleted!!!",!indexFile.exists());
        indexFile = new File(fileName+".id");
        indexFile.delete();
        //assertTrue("index file (.id) is not deleted!!!",!indexFile.exists());
    }

     public void testPutRemoveUseIndex() throws Exception
     {
        checkDataCollection();

        Iterator<DataElement> i = dataCollection.iterator();
        while( i.hasNext() )
            i.next();
        testPutRemove();
     }

     public void testPutRemoveWithVetoUseIndex() throws Exception
     {
        checkDataCollection();

        Iterator<DataElement> i = dataCollection.iterator();
        while( i.hasNext() )
            i.next();

        testPutRemoveWithVeto();
     }
}
