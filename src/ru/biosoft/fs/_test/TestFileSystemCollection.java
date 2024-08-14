package ru.biosoft.fs._test;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.fs.FileSystemCollection;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.access.ViewDataElement;
import ru.biosoft.util.TempFiles;

public class TestFileSystemCollection extends AbstractBioUMLTest
{
    private void testAnyFileSystem(Properties properties) throws Exception
    {
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "test" );
        FileSystemCollection fsc = new FileSystemCollection( null, properties );
        
        assertTrue(fsc.isAcceptable( TextDataElement.class ));
        assertTrue(fsc.isAcceptable( ViewDataElement.class ));
        assertTrue(fsc.isAcceptable( FileSystemCollection.class ));
        
        CollectionFactory.registerRoot( fsc );
        DataElementPath path = DataElementPath.create("test/some/data/folders");
        DataCollectionUtils.createFoldersForPath( path.getChildPath( "" ) );
        assertTrue(fsc.contains( "some" ));
        DataElement folder = path.getDataElement();
        assertTrue(DataCollectionUtils.checkPrimaryElementType( folder, FileSystemCollection.class ));
        
        DataCollection<DataElement> data = path.getParentCollection();
        data.getInfo().getProperties().setProperty( "test", "testVal" );
        CollectionFactoryUtils.save( data );
        
        fsc.removeFromCache( "some" );
        folder = path.getDataElement();
        assertTrue(DataCollectionUtils.checkPrimaryElementType( folder, FileSystemCollection.class ));
        DataCollection<DataElement> data2 = path.getParentCollection();
        assertTrue(data != data2);
        assertEquals("testVal", data2.getInfo().getProperty( "test" ));
        
        fsc.remove( "some" );
        boolean catched = false;
        try
        {
            // Now this element is invalid as we deleted its parent
            ((FileSystemCollection)folder).getNameList();
        }
        catch( RepositoryException e )
        {
            catched = true;
        }
        assertTrue(catched);
        TextDataElement tde = new TextDataElement( "test.txt", fsc, "Test content" );
        CollectionFactoryUtils.save( tde );
        fsc.removeFromCache( "test.txt" );
        TextDataElement restored = DataElementPath.create("test/test.txt").getDataElement( TextDataElement.class );
        assertFalse(tde == restored);
        assertEquals(tde.getContent(), restored.getContent());
        
        FileSystemCollection subCollection = (FileSystemCollection)DataCollectionUtils.createSubCollection( DataElementPath.create( "test/sub" ) );
        String[] folderTypes = FileSystemCollection.getAvailableTypes( subCollection ).toArray( String[]::new );
        assertEquals(1, folderTypes.length);
        assertEquals(fsc.getElementType( "sub" ), folderTypes[0]);
        CompositeView view = new CompositeView();
        view.add( new BoxView( new Pen( 1 ), new Brush( Color.BLACK ), new Rectangle( 0, 0, 100, 100 ) ) );
        ViewDataElement vde = new ViewDataElement( "test.view", subCollection, view );
        CollectionFactoryUtils.save( vde );
        fsc.removeFromCache( "sub" );
        FileSystemCollection subCollection2 = (FileSystemCollection)fsc.get( "sub" );
        assertFalse(subCollection == subCollection2);
        ViewDataElement vde2 = (ViewDataElement)subCollection2.get( "test.view" );
        assertFalse(vde == vde2);
        assertEquals(vde.getView(), vde2.getView());
        
        assertTrue( FileSystemCollection.getAvailableTypes( vde ).has( "BioUML drawing" ) );
        subCollection2.setElementType( "test.view", "Plain text" );
        TextDataElement viewAsText = (TextDataElement)subCollection2.get( "test.view" );
        assertTrue(viewAsText.getContent().startsWith( "{" ));
    }
    
    public void testMemoryFileSystem() throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( FileSystemCollection.FILE_SYSTEM_PROPERTY, "memory" );
        testAnyFileSystem( properties );
    }
    
    public void testLocalFileSystem() throws Exception
    {
        File dir = TempFiles.dir( "fstest" );
        try
        {
            Properties properties = new Properties();
            properties.setProperty( FileSystemCollection.FILE_SYSTEM_PROPERTY, "local" );
            properties.setProperty( FileSystemCollection.FILE_SYSTEM_PROPERTIES_PREFIX+DataCollectionConfigConstants.FILE_PATH_PROPERTY, dir.toString() );
            testAnyFileSystem( properties );
        }
        finally
        {
            ApplicationUtils.removeDir( dir );
        }
    }
}
