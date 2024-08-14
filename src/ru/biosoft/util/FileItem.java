package ru.biosoft.util;

import java.io.File;
import java.net.URI;

/**
 * Helper class to support file uploads from web via PropertyInspector
 */
public class FileItem extends File
{
    private String originalName;
    private String id;

    public FileItem(File file, String name)
    {
        super(file, name);
    }

    public FileItem(String path, String name)
    {
        super(path, name);
    }

    public FileItem(String name)
    {
        super(name);
    }

    public FileItem(URI uri)
    {
        super(uri);
    }
    
    public FileItem(File file)
    {
        super(file, "");
    }

    public String getDisplayName()
    {
        return originalName == null?getName():getOriginalName().replaceFirst("^.+[\\\\\\/]", "");
    }
    
    public String getSuffix()
    {
        String name = getDisplayName();
        int pos = name.indexOf(".");
        if(pos > 0) return name.substring(pos+1);
        return "";
    }
    
    public String getNameWithoutSuffix()
    {
        String name = getDisplayName();
        int pos = name.indexOf(".");
        if(pos > 0) return name.substring(0, pos);
        return name;
    }

    public String getOriginalName()
    {
        return originalName;
    }

    public void setOriginalName(String originalName)
    {
        this.originalName = originalName;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }
}
