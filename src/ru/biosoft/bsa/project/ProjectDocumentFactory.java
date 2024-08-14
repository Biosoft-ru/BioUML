package ru.biosoft.bsa.project;

import com.developmentontheedge.application.ApplicationDocument;
import com.developmentontheedge.application.DocumentFactory;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.AnnotatedSequence;

public class ProjectDocumentFactory implements DocumentFactory
{
    @Override
    public ApplicationDocument createDocument()
    {
        return null;
    }

    @Override
    public ApplicationDocument openDocument(String name)
    {
        ApplicationDocument document = null;
        DataElement de = CollectionFactory.getDataElement(name);
        if( de instanceof AnnotatedSequence )
        {
            try
            {
                document = new ProjectDocument(ProjectAsLists.createProjectByMap(null, de.getOrigin().getName(), (AnnotatedSequence)de), true);
            }
            catch( Exception e )
            {
            }
        }
        else if( de instanceof Project )
        {
            document = new ProjectDocument((Project)de, false);
        }
        return document;
    }
}
