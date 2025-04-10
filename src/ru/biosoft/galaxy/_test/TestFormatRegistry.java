package ru.biosoft.galaxy._test;


import java.io.File;
import java.util.Properties;

import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.galaxy.FormatRegistry;
import ru.biosoft.galaxy.FormatRegistry.ExporterResult;
import ru.biosoft.table.TableCSVImporter;
import ru.biosoft.table.TableCSVImporter.NullImportProperties;
import ru.biosoft.table.export.TableElementExporter;
import ru.biosoft.table.export.TableElementExporter.TableExporterProperties;

public class TestFormatRegistry extends AbstractBioUMLTest
{

    private static final String REPOSITORY_PATH = "../data/test/ru/biosoft/galaxy";

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.createRepository( REPOSITORY_PATH );
    }

    public void testGetImporter()
    {
        DataElementPath targetPath = DataElementPath.create( "galaxy/tables/1.tabular" );
        DataElementPath sourcePath = DataElementPath.create( "galaxy/files/1.tabular" );
        File file = sourcePath.getDataElement(FileDataElement.class).getFile();

        Properties options = new Properties();
        options.setProperty( "format.commentString", "#" );
        options.setProperty( "format.headerRow", "1" );
        options.setProperty( "format.dataRow", "2" );

        DataElementImporter importer = FormatRegistry.getImporter( "tabular", targetPath, file, options );
        assertNotNull( importer );
        assertTrue( importer instanceof TableCSVImporter );

        TableCSVImporter.NullImportProperties importProperties = (NullImportProperties)importer.getProperties(
                targetPath.optParentCollection(), file, targetPath.getName() );
        assertEquals( "#", importProperties.getCommentString() );
        assertEquals( Integer.valueOf( 1 ), importProperties.getHeaderRow() );
        assertEquals( Integer.valueOf( 2 ), importProperties.getDataRow() );
    }

    public void testGetExporter()
    {
        DataElementPath sourcePath = DataElementPath.create( "galaxy/tables/1.tabular" );
        DataElementPath targetPath = DataElementPath.create( "galaxy/files/1.tabular" );
        File file = targetPath.getDataElement(FileDataElement.class).getFile();
        Properties options = new Properties();
        options.setProperty( "format.includeIds", "false" );
        options.setProperty( "format.includeHeaders", "true" );

        ExporterResult exporterInfo = FormatRegistry.getExporter( "tabular", sourcePath, options );
        assertNotNull( exporterInfo );
        assertEquals( "tabular", exporterInfo.galaxyFormat );

        DataElementExporter exporter = exporterInfo.exporter;
        assertTrue( exporter instanceof TableElementExporter );

        TableExporterProperties exporterOptions = (TableExporterProperties)exporter.getProperties( sourcePath.optDataElement(), file );
        assertFalse( exporterOptions.isIncludeIds() );
        assertTrue( exporterOptions.isIncludeHeaders() );

    }
}
