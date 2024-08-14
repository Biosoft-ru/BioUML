package ru.biosoft.gui;

import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.swing.SwingUtilities;

import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.ExtensionRegistrySupport;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationDocument;
import com.developmentontheedge.application.ApplicationFrame;
import com.developmentontheedge.application.DocumentFactory;

/**
 * Special document manager that supports sharing of View/EditorParts between several documents.
 * For example if an application contains one instance of specific EditorPart it can be shared by
 * several documents, the editor part will show the content of active document.
 */
public class DocumentManager extends com.developmentontheedge.application.DocumentManager
{
    protected static final Logger log = Logger.getLogger( DocumentManager.class.getName() );

    private static class DocumentFactoryRegistry extends ExtensionRegistrySupport<DocumentFactory>
    {
        public DocumentFactoryRegistry()
        {
            super( "ru.biosoft.workbench.documentFactory", "key" );
        }

        @Override
        protected DocumentFactory loadElement(IConfigurationElement element, String keyClassName) throws Exception
        {
            Class<? extends DataElement> elementClass = getClassAttribute( element, "key", DataElement.class );
            String className = element.getAttribute( "class" );
            if( className != null )
            {
                return ClassLoading.loadSubClass( className, element.getNamespaceIdentifier(), DocumentFactory.class ).newInstance();
            }
            Class<? extends Document> documentClass = getClassAttribute( element, "documentClass", Document.class );
            return new DefaultDocumentFactory( documentClass, elementClass );
        }
    }

    private final DocumentFactoryRegistry registry = new DocumentFactoryRegistry();

    public static void setActiveDocument(Document document, Component child)
    {
        Window window = SwingUtilities.getWindowAncestor( child );
        if( window instanceof ApplicationFrame )
            ( (ApplicationFrame)window ).getDocumentManager().setActiveDocument( document );
    }

    public static void setActiveRepositoryDocument(Document document, Component child)
    {
        Window window = SwingUtilities.getWindowAncestor( child );
        if( window instanceof ApplicationFrame )
            ( (ApplicationFrame)window ).getDocumentManager().setActiveRepositoryDocument( document );
    }

    ////////////////////////////////////////////////////////////////////////////
    public DocumentManager(DocumentFactory documentFactory)
    {
        super( documentFactory );
    }

    public DocumentFactory getDocumentFactory(Class<? extends DataElement> objectClass)
    {
        Class<?> curClass = objectClass;
        Set<Class<?>> interfaces = new HashSet<>();
        while( curClass != null )
        {
            DocumentFactory factory = registry.getExtension( curClass.getName() );
            if( factory != null )
                return factory;
            interfaces.addAll( Arrays.asList( curClass.getInterfaces() ) );
            curClass = curClass.getSuperclass();
        }
        return StreamEx.of( interfaces ).map( iClass -> registry.getExtension( iClass.getName() ) ).nonNull().findFirst().orElse( null );
    }

    /**
     * Opens document using default document factory for given element
     * @param de
     * @return created document or null in case of error
     */
    public Document openDocument(DataElement de)
    {
        return openDocument( DataElementPath.create( de ).getDescriptor().getType(), de );
    }

    /**
     * Opens document using default document factory for given element
     * @param de
     * @return created document or null in case of error
     */
    public Document openDocument(Class<? extends DataElement> baseClass, DataElement de)
    {
        Document curDocument = Document.getCurrentDocument();
        if( curDocument != null && curDocument.getModel().equals( de ) )
        {
            curDocument.update();
            return curDocument;
        }
        DocumentFactory factory = getDocumentFactory( baseClass );
        if( factory == null )
            return null;
        Document document;
        try
        {
            document = (Document)factory.openDocument( DataElementPath.create( de ).toString() );
        }
        catch( Throwable e )
        {
            log.log(Level.SEVERE,  "Unable to open document: " + ExceptionRegistry.log( e ) );
            return null;
        }
        if( document == null )
        {
            log.log(Level.SEVERE,  "Unable to open document" );
            return null;
        }
        if( curDocument != null && curDocument.getModel().equals( document.getModel() ) )
        {
            curDocument.update();
            return curDocument;
        }
        GUI.getManager().addDocument( document );
        getDocumentViewAccessProvider().enableDocumentActions( true );
        return document;
    }

    public StreamEx<DocumentFactory> documentFactories()
    {
        return registry.stream();
    }

    @Override
    public void setActiveDocument(ApplicationDocument document)
    {
        //System.out.println("Set active document: " + (document == null ? null : document.getDisplayName()) );
        if( activeDocument == document )
            return;

        Object oldDocument = activeDocument;
        activeDocument = document;

        if( oldDocument instanceof Document )
            ( (Document)oldDocument ).setActive( false );

        if( activeDocument instanceof Document )
            ( (Document)activeDocument ).setActive( true );
    }

    protected List<ViewPart> editorList = new ArrayList<>();
    public List<ViewPart> getEditorList()
    {
        return editorList;
    }

    /** Registers editor part that can be shared by several documents. */
    public void registerEditorPart(ViewPart viewPart)
    {
        editorList.add( viewPart );
    }

    public static @Nonnull DocumentManager getDocumentManager()
    {
        try
        {
            com.developmentontheedge.application.DocumentManager documentManager = Application.getApplicationFrame().getDocumentManager();
            if( documentManager instanceof DocumentManager )
                return (DocumentManager)documentManager;
        }
        catch( Exception e )
        {
        }
        throw new UnsupportedOperationException();
    }

    public static @Nonnull DocumentViewAccessProvider getDocumentViewAccessProvider()
    {
        return GUI.getManager().getDocumentViewAccessProvider();
    }

    @Override
    public DocumentFactory getDocumentFactory()
    {
        throw new InternalException( "getDocumentFactory() without arguments called: use getDocumentFactory(Class)" );
    }
}
