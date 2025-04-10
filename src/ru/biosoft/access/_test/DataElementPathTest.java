package ru.biosoft.access._test;

import java.util.Arrays;

import junit.framework.TestCase;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.WildcardPathSet;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementNotFoundException;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.util.TextUtil;

public class DataElementPathTest extends TestCase
{
    public void testConstruction() throws Exception
    {
        assertNull(DataElementPath.create((String)null));
        assertNull(DataElementPath.create((DataElement)null));
        DataCollection<DataElement> vdc = DataElementPath.create("test").getDataCollection();
        DataCollection<DataElement> vdc2 = (DataCollection<DataElement>)vdc.get("test2");
        DataElement de = vdc2.get("test3");
        assertEquals("test/test2", DataElementPath.create("test/test2").toString());
        assertEquals("test", DataElementPath.create(vdc).toString());
        assertEquals(DataElementPath.create("test/test2"), DataElementPath.create(vdc2));
        assertEquals(DataElementPath.create("test/test2/test3"), DataElementPath.create(de));
        DataElementPath path = DataElementPath.create(vdc, "new");
        assertEquals("test/new", path.toString());
        assertEquals(path, TextUtil.fromString(DataElementPath.class, TextUtil.toString(path)));
        DataElement orphan = new TextDataElement("test4", null);
        assertEquals("test4", DataElementPath.create(orphan).toString());
        assertEquals("test/test2/test3", DataElementPath.create("test", "test2", "test3").toString());
        assertNull(DataElementPath.create(null, "test"));
    }
    
    public void testBasics() throws Exception
    {
        DataElementPath path = DataElementPath.create("a/b/c");
        assertEquals(path, path);
        assertFalse(path.equals(null));
        assertFalse(path.equals("a/b/c"));
        DataElementPath path2 = DataElementPath.create("a/b2/c");
        assertTrue(path2.compareTo(path)>0);
        
        assertTrue(DataElementPath.create("").isEmpty());
        assertFalse(DataElementPath.create("test").isEmpty());
        
        assertFalse(DataElementPath.create("test/test1").exists());
        assertTrue(DataElementPath.create("test/test2").exists());
        assertFalse(DataElementPath.create("test2/test").exists());
        assertFalse(DataElementPath.create("test/").exists());
        DataElementPathSet set = new DataElementPathSet();
        set.add(DataElementPath.create("test/test2"));
        DataElementPathSet set2 = new DataElementPathSet();
        set2.add(DataElementPath.create("test/test3"));
        assertEquals(set, DataElementPath.create("test").getChildren());
        assertFalse(set2.equals(DataElementPath.create("test").getChildren()));
        boolean catched = false;
        try
        {
            DataElementPath.create("test2").getChildren();
        }
        catch( DataElementNotFoundException e1 )
        {
            catched = true;
        }
        assertTrue(catched);
        
        ru.biosoft.access.core.DataElementPath[] childrenArray = DataElementPath.create("test").getChildrenArray();
        assertEquals(1, childrenArray.length);
        assertEquals(DataElementPath.create("test/test2"), childrenArray[0]);
        
        assertNull(DataElementPath.create("test/qqq/test3").getDescriptor());
        assertEquals(TextDataElement.class, DataElementPath.create("test/test2/test3").getDescriptor().getType());
        
        assertEquals(DataElementPath.create("test/test1"), DataElementPath.create("test/test1").uniq());
        assertFalse(DataElementPath.create("test/test2").uniq().exists());
        
        assertTrue(DataElementPath.create("test/test1").isAncestorOf(DataElementPath.create("test/test1/test2")));
        assertTrue(DataElementPath.create("test/test1").isAncestorOf(DataElementPath.create("test/test1")));
        assertFalse(DataElementPath.create("test/test1").isAncestorOf(DataElementPath.create("test/test2/test1")));
        assertTrue(DataElementPath.create("test/test1").isDescendantOf(DataElementPath.create("test")));
        assertFalse(DataElementPath.create("test/test1").isDescendantOf(DataElementPath.create("test1")));
        assertFalse(DataElementPath.create("test/test1").isDescendantOf(DataElementPath.create("test/test1/test2/test3")));
        assertTrue(DataElementPath.create("test/test1").isSibling(DataElementPath.create("test/test1")));
        assertTrue(DataElementPath.create("test/test1").isSibling(DataElementPath.create("test/test2")));
        assertFalse(DataElementPath.create("test/test1").isSibling(DataElementPath.create("test/test1/test2")));
        assertFalse(DataElementPath.create("test1/test1").isSibling(DataElementPath.create("test2/test1")));
        assertEquals("test3/test4", DataElementPath.create("test/test2/test3/test4").getPathDifference(DataElementPath.create("test/test2")));
        
        assertNull(DataElementPath.create("test/").optDataElement());
        assertNull(DataElementPath.create("test1/test2").optDataElement());
        
        assertEquals(DataElementPath.create("test/test2"), DataElementPath.create("test/test1").getRelativePath("../test2"));
        assertEquals(TextDataElement.class, DataElementPath.create("test/test1").getRelativePath("test3/../.././test2/test3").getDataElement().getClass());
        assertEquals("test2", DataElementPath.create("test/test1").getRelativePath("../test2").getName());
        
        DataCollection<?> origin = DataElementPath.create("test/test2").optDataCollection();
        TextDataElement de = new TextDataElement("savetest", origin);
        path = DataElementPath.create(de);
        assertNull(path.optDataElement());
        catched = false;
        try
        {
            path.getSiblingPath("test3").save(de);
        }
        catch( DataElementPutException e )
        {
            catched = true;
        }
        assertTrue(catched);
        path.save(de);
        assertEquals(de, path.optDataElement());
        path.remove();
        assertNull(path.optDataElement());
    }
    
    public void testCommonPrefix()
    {
        assertEquals( DataElementPath.create( "a/b" ),
                DataElementPath.create( "a/b/c/d" ).getCommonPrefix( DataElementPath.create( "a/b/ce/f/g/h" ) ) );
    }

    public void testEscape() throws Exception
    {
        assertEquals(DataElementPath.escapeName("asfsafads"), "asfsafads");
        assertEquals(DataElementPath.unescapeName("asfsafads"), "asfsafads");
        assertEquals(DataElementPath.escapeName("asfs/afads"), "asfs\\safads");
        assertEquals(DataElementPath.escapeName("asfs/af/ads"), "asfs\\saf\\sads");
        assertEquals(DataElementPath.unescapeName("asfs\\safads"), "asfs/afads");
        assertEquals(DataElementPath.unescapeName("asfs\\saf\\sads"), "asfs/af/ads");
        assertEquals(DataElementPath.unescapeName("a\\\\sfs\\saf\\sads"), "a\\sfs/af/ads");
        assertEquals(DataElementPath.unescapeName(DataElementPath.escapeName("\\s\\s\\sas\\\\fs\\safads\\s")), "\\s\\s\\sas\\\\fs\\safads\\s");
    }
    
    public void testParent() throws Exception
    {
        DataElementPath path = DataElementPath.create("data/test/foo/bar");
        assertEquals(path.getName(), "bar");
        assertEquals(path.getParentPath().toString(), "data/test/foo");
        assertEquals(path.getParentPath().getName(), "foo");
        assertEquals(path.getParentPath().getParentPath().toString(), "data/test");
        assertEquals(path.getParentPath().getParentPath().getName(), "test");
        assertEquals(path.getParentPath().getParentPath().getParentPath().toString(), "data");
        assertEquals(path.getParentPath().getParentPath().getParentPath().getName(), "data");
        assertEquals(path.getParentPath().getParentPath().getParentPath().getParentPath().toString(), "");
        
        path = DataElementPath.create("\\sdata/te\\sst/foo/\\sba\\\\r\\s");
        assertEquals(path.getName(), "/ba\\r/");
        assertEquals(path.getParentPath().toString(), "\\sdata/te\\sst/foo");
        assertEquals(path.getParentPath().getName(), "foo");
        assertEquals(path.getParentPath().getParentPath().toString(), "\\sdata/te\\sst");
        assertEquals(path.getParentPath().getParentPath().getName(), "te/st");
        assertEquals(path.getParentPath().getParentPath().getParentPath().toString(), "\\sdata");
        assertEquals(path.getParentPath().getParentPath().getParentPath().getName(), "/data");
    }
    
    public void testChild() throws Exception
    {
        DataElementPath path = DataElementPath.create("");
        DataElementPath child = path.getChildPath("data");
        assertEquals("data", child.toString());
        assertEquals("data/", child.getChildPath("").toString());
        DataElementPath grandChild = child.getChildPath("test");
        assertEquals(grandChild.toString(), "data/test");
        assertEquals(child.getParentPath(), path);
        assertEquals(grandChild.getParentPath(), child);
        
        path = DataElementPath.create("").getChildPath("/data", "te/st", "foo").getChildPath("/ba\\r/");
        assertEquals("\\sdata/te\\sst/foo/\\sba\\\\r\\s", path.toString());
    }
    
    public void testPathComponents() throws Exception
    {
        DataElementPath empty = DataElementPath.create("");
        assertEquals(0, empty.getDepth());
        assertEquals(0, empty.getPathComponents().length);
        DataElementPath path = DataElementPath.create("\\sdata/te\\sst/foo/\\sba\\\\r\\s");
        assertEquals(4, path.getDepth());
        String[] components = path.getPathComponents();
        assertEquals(components[0], "/data");
        assertEquals(components[1], "te/st");
        assertEquals(components[2], "foo");
        assertEquals(components[3], "/ba\\r/");
    }
    
    public void testDataElementPathSet() throws Exception
    {
        DataElementPathSet set = new DataElementPathSet();
        set.add(DataElementPath.create("test/a"));
        set.add(DataElementPath.create("test/b"));
        set.add(DataElementPath.create("test/c"));
        assertEquals(3, set.size());
        assertEquals(DataElementPath.create("test"), set.getPath());
        assertEquals(DataElementPath.create("test/a"), set.first());
        DataElementPathSet set2 = (DataElementPathSet)TextUtil.fromString(DataElementPathSet.class, TextUtil.toString(set));
        assertEquals(set, set2);
        assertEquals("c", set2.getNames()[2]);
        
        DataElementPathSet set3 = new DataElementPathSet(DataElementPath.create("test"), Arrays.asList("a","c","b"));
        assertEquals(set, set3);
        DataElementPathSet set4 = new DataElementPathSet(DataElementPath.create("test"), "c","b","a");
        assertEquals(set, set4);
        
        DataElementPathSet emptyPath = new DataElementPathSet();
        DataElementPathSet emptyPath2 = (DataElementPathSet)TextUtil.fromString(DataElementPathSet.class, TextUtil.toString(emptyPath));
        assertNull(emptyPath.first());
        assertEquals(emptyPath, emptyPath2);
        
        DataElementPathSet diffPaths = new DataElementPathSet();
        diffPaths.add(DataElementPath.create("test/a"));
        diffPaths.add(DataElementPath.create("test2/b"));
        diffPaths.add(DataElementPath.create("test3/c"));
        DataElementPathSet diffPaths2 = (DataElementPathSet)TextUtil.fromString(DataElementPathSet.class, TextUtil.toString(diffPaths));
        assertEquals(diffPaths, diffPaths2);
    }
    
    public void testWildcardPathSet() throws Exception
    {
        VectorDataCollection<TextDataElement> vdc = new VectorDataCollection<>("data");
        vdc.put( new TextDataElement("e1", vdc, "") );
        vdc.put( new TextDataElement("e2", vdc, "") );
        vdc.put( new TextDataElement("t3", vdc, "") );
        vdc.put( new TextDataElement("q4", vdc, "") );
        CollectionFactory.registerRoot( vdc );
        WildcardPathSet pathSet = new WildcardPathSet( "data/e*;?4" );
        assertEquals(3, pathSet.size());
        assertTrue(pathSet.contains( DataElementPath.create("data/e1") ));
        assertTrue(pathSet.contains( DataElementPath.create("data/e2") ));
        assertTrue(pathSet.contains( DataElementPath.create("data/q4") ));
    }

    @Override
    protected void setUp() throws Exception
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        VectorDataCollection<DataElement> vdc2 = new VectorDataCollection<>("test2", vdc, null);
        vdc.put(vdc2);
        DataElement de = new TextDataElement("test3", vdc2, "");
        vdc2.put(de);
    }
}
