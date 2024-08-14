package com.developmentontheedge.application;

public interface DocumentFactory
{
    public ApplicationDocument   createDocument();
    public ApplicationDocument   openDocument(String name);
}
