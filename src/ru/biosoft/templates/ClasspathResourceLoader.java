package ru.biosoft.templates;

import java.io.InputStream;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

public class ClasspathResourceLoader extends ResourceLoader
{
    @Override
    public long getLastModified(Resource resource)
    {
        return 0;
    }

    @Override
    public InputStream getResourceStream(String source) throws ResourceNotFoundException
    {
        return ClasspathResourceLoader.class.getResourceAsStream(source);
    }

    @Override
    public void init(ExtendedProperties configuration)
    {
        
    }

    @Override
    public boolean isSourceModified(Resource resource)
    {
        return false;
    }

}
