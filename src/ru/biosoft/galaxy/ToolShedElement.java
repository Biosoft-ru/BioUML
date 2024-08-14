package ru.biosoft.galaxy;

import org.w3c.dom.Element;

import ru.biosoft.util.XmlUtil;

public class ToolShedElement
{
    private String toolShed, repositoryName, repositoryOwner, revision, id, version;
    
    public ToolShedElement(Element root)
    {
        toolShed = getChildText( root, "tool_shed" );
        repositoryName = getChildText( root, "repository_name" );
        repositoryOwner = getChildText( root, "repository_owner" );
        revision = getChildText( root, "installed_changeset_revision" );
        id = getChildText( root, "id" );
        version = getChildText( root, "version" );
    }

    public String getToolShed()
    {
        return toolShed;
    }

    public String getRepositoryName()
    {
        return repositoryName;
    }

    public String getRepositoryOwner()
    {
        return repositoryOwner;
    }

    public String getRevision()
    {
        return revision;
    }

    public String getId()
    {
        return id;
    }

    public String getVersion()
    {
        return version;
    }
    
    private String getChildText(Element root, String name)
    {
        Element child = XmlUtil.getChildElement( root, name );
        if(child == null)
            return "";
        return XmlUtil.getTextContent( child );
    }
}
