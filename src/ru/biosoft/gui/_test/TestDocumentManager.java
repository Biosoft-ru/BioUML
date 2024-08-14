package ru.biosoft.gui._test;

import com.developmentontheedge.application.DocumentFactory;

import ru.biosoft.access.TextDataElement;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.gui.DefaultDocumentFactory;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.plugins.javascript.JSElement;
import ru.biosoft.plugins.javascript.document.JSDocument;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.document.TableFactory;
import ru.biosoft.workbench.documents.TextDocument;

/**
 * @author lan
 *
 */
public class TestDocumentManager extends AbstractBioUMLTest
{
    public void testDocumentFactory()
    {
        DocumentManager manager = new DocumentManager(null);
        DocumentFactory factory = manager.getDocumentFactory(JSElement.class);
        assertTrue(factory instanceof DefaultDocumentFactory);
        assertTrue(factory.createDocument() instanceof JSDocument);
        factory = manager.getDocumentFactory(TextDataElement.class);
        assertTrue(factory instanceof DefaultDocumentFactory);
        assertTrue(factory.createDocument() instanceof TextDocument);
        factory = manager.getDocumentFactory(StandardTableDataCollection.class);
        assertTrue(factory instanceof TableFactory);
    }
}
