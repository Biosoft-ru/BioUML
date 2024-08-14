package biouml.plugins.chemoinformatics._test;

import biouml.plugins.chemoinformatics.SDFImporter;
import biouml.plugins.chemoinformatics.SDFImporter.ImportProperties;
import biouml.standard.type.Structure;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.graphics.View;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;

public class TestSDFImporter extends AbstractBioUMLTest
{
    public void testImport() throws Exception
    {
        FolderVectorCollection folder = new FolderVectorCollection( "folder", null );
        CollectionFactory.registerRoot( folder );
        try(TempFile file = TempFiles.file( "", TestSDFImporter.class.getResourceAsStream( "aspirin.sdf" ) ))
        {
            SDFImporter importer = new SDFImporter();
            assertTrue(importer.accept( folder, file ) >= DataElementImporter.ACCEPT_MEDIUM_PRIORITY);
            ImportProperties properties = (ImportProperties)importer.getProperties( folder, file, "aspirin" );
            assertTrue(properties.getPossibleKeys().has( "PUBCHEM_IUPAC_NAME" ));
            properties.setKey( "PUBCHEM_IUPAC_NAME" );
            properties.setCreateTable( true );
            FolderVectorCollection result = (FolderVectorCollection)importer.doImport( folder, file, "aspirin", null, null );
            assertEquals(2, result.getSize());
            Structure structure = (Structure)result.get( "2-acetyloxybenzoic acid" );
            assertEquals("1.2", structure.getAttributes().getValueAsString( "PUBCHEM_XLOGP3" ));
            assertTrue(structure.getData().startsWith( "2244" ));
            TableDataCollection table = (TableDataCollection)result.get( SDFImporter.SUMMARY_TABLE_NAME );
            assertEquals(1, table.getSize());
            RowDataElement row = table.get( "2-acetyloxybenzoic acid" );
            assertEquals("C9H8O4", row.getValue( "PUBCHEM_MOLECULAR_FORMULA" ));
            assertTrue(row.getValue( SDFImporter.STRUCTURE_COLUMN ) instanceof View);
        }
    }
}
