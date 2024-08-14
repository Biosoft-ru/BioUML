package ru.biosoft.access.generic._test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.generic.GenericDataCollection;

public class TestLargeGDC extends AbstractBioUMLTest
{
    private static final int SIZE = 3000;
    private static final String REPOSITORY_PATH = "../data/test/ru/biosoft/access/generic";
    private static final String FOLDER_PATH = REPOSITORY_PATH + "/large/";
    private static final Set<Path> IGNORE = Stream.of( "", "default.config", "large.format.config" )
            .map( s -> FOLDER_PATH + s ).map( Paths::get ).collect( Collectors.toSet() );

    private GenericDataCollection gdc;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        clearFolder();
        CollectionFactory.createRepository( REPOSITORY_PATH );
        gdc = DataElementPath.create( "generic/large" ).getDataElement( GenericDataCollection.class );
        for( int i = 0; i < SIZE; i++ )
        {
            DataElementPath path = DataElementPath.create( "generic/large/" + i );
            DataCollectionUtils.createSubCollection( path );
        }
    }

    public void testIterate()
    {
        Iterator<DataElement> it = gdc.iterator();
        int n = 0;
        while( it.hasNext() )
        {
            DataElement de = it.next();
            assertNotNull( de );
            n++;
        }
        assertEquals( SIZE, n );
    }

    @Override
    protected void tearDown() throws Exception
    {
        gdc.close();
        super.tearDown();
        clearFolder();
    }

    private void clearFolder() throws IOException
    {
        Path path = Paths.get( FOLDER_PATH );
        Files.walkFileTree( path, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if( !IGNORE.contains( file ) )
                    Files.delete( file );
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                if( !IGNORE.contains( dir ) )
                    Files.delete( dir );
                return FileVisitResult.CONTINUE;
            }
        } );
    }
}
