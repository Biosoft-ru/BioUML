package ru.biosoft.plugins.javascript.document;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;

import ru.biosoft.access.ClassLoading;

public class JSDocumentContextFactory extends ContextFactory
{
    public JSDocumentContextFactory()
    {
        initApplicationClassLoader(ClassLoading.getClassLoader());
    }
    
    public Object callInOwnContext(ContextAction action)
    {
        return call(action);
    }

    public Context getContext()
    {
        return enterContext();
    }
}
