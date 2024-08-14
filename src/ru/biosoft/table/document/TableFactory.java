package ru.biosoft.table.document;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import com.developmentontheedge.application.ApplicationDocument;
import com.developmentontheedge.application.DocumentFactory;

public class TableFactory implements DocumentFactory
{
    protected static final Logger log = Logger.getLogger( TableFactory.class.getName() );

    public static final String DOCUMENT_CLASS_NAME = "documentClassName";

    @Override
    public ApplicationDocument createDocument()
    {
        return new TableDocument( null );
    }

    @Override
    public ApplicationDocument openDocument(String name)
    {
        try
        {
            DataCollection<DataElement> dc = DataElementPath.create(name).getDataCollection();
            String documentClassName = dc.getInfo().getProperty( DOCUMENT_CLASS_NAME );
            if( documentClassName != null )
            {
                Class<? extends ApplicationDocument> documentClass = ClassLoading.loadSubClass( documentClassName, ApplicationDocument.class );
                return documentClass.getConstructor( ru.biosoft.access.core.DataCollection.class ).newInstance( dc );
            }
            return new TableDocument(dc);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Unable to open document: "+ExceptionRegistry.log(e));
            return null;
        }
    }
}
