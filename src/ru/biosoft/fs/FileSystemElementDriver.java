package ru.biosoft.fs;

import java.io.File;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataElement;

public interface FileSystemElementDriver
{
    public boolean isLeaf(ElementInfo info);
    
    public DataElement create(FileSystemCollection parent, ElementInfo info, File data) throws Exception;
    
    public ElementInfo save(DataElement element, File data) throws Exception;

    boolean isSupported(Class<? extends DataElement> clazz);

    public StreamEx<String> getAvailableTypes(ElementInfo info);

    public ElementInfo updateInfoForType(ElementInfo elementInfo, String type);

    String getCurrentType(ElementInfo info);

    public Class<? extends DataElement> detectClass(ElementInfo elementInfo);
}
