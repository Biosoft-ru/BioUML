package ru.biosoft.table.access;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Logger;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.table.FileTableDataCollection;
import ru.biosoft.table.TableDataCollection;

/**
 * Simple transformer for {@link FileTableDataCollection}
 */
public class TableDataFileTransformer extends AbstractFileTransformer<FileTableDataCollection>
{
    protected Logger log = Logger.getLogger( TableDataCollection.class.getName() );
    
    @Override
    public Class<FileDataElement> getInputType()
    {
        return FileDataElement.class;
    }

    @Override
    public Class<FileTableDataCollection> getOutputType()
    {
        return FileTableDataCollection.class;
    }

    @Override
    public FileTableDataCollection load(File input, String name, DataCollection<FileTableDataCollection> origin) throws Exception
    {
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, name);
        properties.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, input.getParent()+File.separator);
        properties.put(DataCollectionConfigConstants.FILE_PROPERTY, input.getName());

        return new FileTableDataCollection(origin, properties);
    }

    @Override
    public void save(File output, FileTableDataCollection element) throws Exception
    {
        TableDataEntryTransformer transformer = new TableDataEntryTransformer(false);
        transformer.addCommand(new FileDataTagCommand(TableDataTagCommand.DATA_TAG, transformer, element));
        try(Writer fileWriter = new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8))
        {
            fileWriter.write("ID\t");
            fileWriter.write(element.getName());
            fileWriter.write(TableDataEntryTransformer.endl);
            transformer.writeObject(element, fileWriter);
            fileWriter.write("//");
            fileWriter.write(TableDataEntryTransformer.endl);
        }
    }
}
