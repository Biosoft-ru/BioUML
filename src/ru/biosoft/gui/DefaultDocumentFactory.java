package ru.biosoft.gui;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import com.developmentontheedge.application.ApplicationDocument;
import com.developmentontheedge.application.DocumentFactory;

/**
 * @author lan
 *
 */
public class DefaultDocumentFactory implements DocumentFactory
{
    private @Nonnull Class<? extends Document> documentClass;
    private @Nonnull Class<? extends DataElement> elementClass;

    public DefaultDocumentFactory(@Nonnull Class<? extends Document> documentClass, @Nonnull Class<? extends DataElement> elementClass)
    {
        this.elementClass = elementClass;
        this.documentClass = documentClass;
    }

    @Override
    public ApplicationDocument createDocument()
    {
        try
        {
            return documentClass.getConstructor(elementClass).newInstance(new Object[] {null});
        }
        catch( Throwable t )
        {
            return null;
        }
    }

    @Override
    public ApplicationDocument openDocument(String name)
    {
        try
        {
            DataElement de = DataElementPath.create(name).getDataElement(elementClass);
            return documentClass.getConstructor(elementClass).newInstance(de);
        }
        catch( Throwable t )
        {
            throw ExceptionRegistry.translateException(t);
        }
    }
}
