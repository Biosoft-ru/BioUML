package ru.biosoft.treetable._test;

import java.io.File;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.treetable.TreeTableElement;
import ru.biosoft.treetable.access.FileViewTransformer;

/**
 * @author lan
 *
 */
public class TestTreeTableTransformer extends AbstractBioUMLTest
{
    public void testTreeTableTransformerOldFormat() throws Exception
    {
        TreeTableElement element = readFile("treeTableOldFormat.txt");
        assertEquals(DataElementPath.create("databases/Test"), element.getTreePath());
        assertTrue(element.isHideBranchesAbsentInTable());
        assertEquals("var a = null;\na;", element.getTableScript());
    }

    public void testTreeTableTransformerNewFormat() throws Exception
    {
        TreeTableElement element = readFile("treeTableNewFormat.txt");
        assertEquals(DataElementPath.create("databases/Test"), element.getTreePath());
        assertFalse(element.isHideBranchesAbsentInTable());
        assertEquals("data.get('databases\\/Test\\/Table');", element.getTableScript());
    }

    public void testTreeTableTransformerNewFormatScript() throws Exception
    {
        TreeTableElement element = readFile("treeTableNewFormatScript.txt");
        assertEquals(DataElementPath.create("databases/Test"), element.getTreePath());
        assertTrue(element.isHideBranchesAbsentInTable());
        assertEquals("var a = null;\na;\n", element.getTableScript());
    }

    private TreeTableElement readFile(String fileName) throws Exception
    {
        File file = new File(TestTreeTableTransformer.class.getResource(fileName).toURI());
        FileViewTransformer transformer = new FileViewTransformer();
        transformer.init(null, null);
        FileDataElement fde = new FileDataElement(file.getName(), null, file);
        TreeTableElement de = transformer.transformInput(fde);
        assertNotNull(de);
        return de;
    }
}
