package com.developmentontheedge.application;

import java.io.File;
import java.io.IOException;

public class TestRepository
{
    private String basePath = null;
    
    private static TestRepository instance = null;
    
    private TestRepository()
    {
        if ("Linux".equals(System.getProperty("os.name")))
            basePath = "/mnt/lachesis/disk_E/BioUML TestRepository/";
        else
            basePath = "E:/BioUML TestRepository/";
    }

    public static synchronized TestRepository getInstance()
    {
        if (instance == null)
            instance = new TestRepository();
        return instance;
    }
    
    public File getFileCopy(String neededPath, String repositoryRelativePath) throws IOException
    {
        File dst = new File(neededPath);
        if (dst.exists())
            return dst;
        ApplicationUtils.copyFile(dst, getFile(repositoryRelativePath));
        return dst;
    }

    public File getFile(String repositoryRelativePath)
    {
        return new File(basePath + repositoryRelativePath);
    }
}
