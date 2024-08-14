package ru.biosoft.fs;

import java.io.File;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.FolderCollection;

public class FolderDriver implements FileSystemElementDriver
{
    @Override
    public boolean isLeaf(ElementInfo info)
    {
        return false;
    }

    @Override
    public DataElement create(FileSystemCollection parent, ElementInfo info, File file)
    {
        FileSystemCollection fsc = new FileSystemCollection( parent, info.getName() );
        info.initCollectionProperties(fsc);
        return fsc;
    }

    @Override
    public ElementInfo save(DataElement element, File data) throws Exception
    {
        // Just create new ElementInfo
        FileSystemCollection fsc = (FileSystemCollection)element;
        return new ElementInfo( fsc );
    }

    @Override
    public boolean isSupported(Class<? extends DataElement> clazz)
    {
        return clazz == FileSystemCollection.class || clazz == FolderCollection.class;
    }

    @Override
    public StreamEx<String> getAvailableTypes(ElementInfo info)
    {
        return StreamEx.of( "Folder" );
    }

    @Override
    public ElementInfo updateInfoForType(ElementInfo elementInfo, String type)
    {
        return elementInfo;
    }

    @Override
    public String getCurrentType(ElementInfo info)
    {
        return "Folder";
    }

    @Override
    public Class<? extends DataElement> detectClass(ElementInfo elementInfo)
    {
        return FileSystemCollection.class;
    }
}
