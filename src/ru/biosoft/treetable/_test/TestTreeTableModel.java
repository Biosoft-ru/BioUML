package ru.biosoft.treetable._test;

import com.developmentontheedge.beans.model.Property;

import junit.framework.TestCase;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.classification.ClassificationUnitAsVector;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.treetable.TreeTableElement;
import ru.biosoft.treetable.TreeTableModel;

/**
 * @author lan
 *
 */
public class TestTreeTableModel extends TestCase
{
    public void testTreeTableModel() throws Exception
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        ClassificationUnitAsVector treeRoot = new ClassificationUnitAsVector(vdc, "class", null, null, null);
        vdc.put(treeRoot);
        ClassificationUnitAsVector class1 = new ClassificationUnitAsVector(treeRoot, "class1", null, null, null);
        treeRoot.put(class1);
        ClassificationUnitAsVector class12 = new ClassificationUnitAsVector(class1, "class12", null, null, null);
        class1.put(class12);
        ClassificationUnitAsVector class2 = new ClassificationUnitAsVector(treeRoot, "class2", null, null, null);
        treeRoot.put(class2);
        ClassificationUnitAsVector class23 = new ClassificationUnitAsVector(class2, "class23", null, null, null);
        class2.put(class23);
        
        TableDataCollection table = new StandardTableDataCollection(vdc, "table");
        vdc.put(table);
        table.getColumnModel().addColumn("testCol", String.class);
        TableDataCollectionUtils.addRow(table, "class1", new Object[] {"test1"});
        TableDataCollectionUtils.addRow(table, "class12", new Object[] {"test12"});
        TableDataCollectionUtils.addRow(table, "class2", new Object[] {"test2"});
        
        TreeTableElement tte = new TreeTableElement("treeTable", vdc);
        vdc.put(tte);
        tte.setTreePath(treeRoot.getCompletePath());
        tte.setTableScript( "Packages.ru.biosoft.access.core.CollectionFactory.getDataCollection('test/table')" );
        tte.setHideBranchesAbsentInTable(true);
        assertEquals(table, tte.getTable());
        
        TreeTableModel model = new TreeTableModel(tte);
        assertEquals(treeRoot.getCompletePath(), model.getRoot());
        assertEquals(2, model.getChildCount(treeRoot.getCompletePath()));
        assertEquals(class1.getCompletePath(), model.getChild(treeRoot.getCompletePath(), 0));
        assertEquals(1, model.getChildCount(class1.getCompletePath()));
        assertEquals(0, model.getChildCount(class2.getCompletePath()));
        assertEquals(2, model.getColumnCount());
        assertEquals("testCol", model.getColumn(1).getName());
        assertEquals("testCol", model.getColumnName(1));
        assertEquals("ID", model.getColumnName(0));
        
        assertFalse(model.isCellEditable(class1.getCompletePath(), 1));
        assertEquals("class1", model.getValueAt(class1.getCompletePath(), 0));
        assertEquals(Property.class, model.getColumnClass(1));
        assertEquals("test12", ((Property)model.getValueAt(class12.getCompletePath(), 1)).getValue());
        assertEquals("class", model.getValueAt(treeRoot.getCompletePath(), 0));
        assertNull(model.getValueAt(treeRoot.getCompletePath(), 1));
    }
}
