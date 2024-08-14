package ru.biosoft.galaxy.filters;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.galaxy.GalaxyDataCollection;
import ru.biosoft.util.XmlUtil;

/**
 * @author lan
 *
 */
public class DataTable
{
    private static Logger log = Logger.getLogger(DataTable.class.getName());
    private String name;
    private String type = "tabular";
    private String commentChar = "#";
    private String separatorChar = "\t+";
    private boolean loaded = false;
    private Element element;
    private List<String> columns;
    private List<String[]> content = new ArrayList<>();
    
    private static String getAttribute(Element element, String name, String defaultValue)
    {
        return element.hasAttribute(name)?element.getAttribute(name):defaultValue;
    }
    
    private synchronized void load()
    {
        if(loaded) return;
        loaded = true;
        NodeList files = element.getElementsByTagName("file");
        for(Element file : XmlUtil.elements(files))
        {
            String path = file.getAttribute("path");
            try
            {
                File tableFile = new File(path);
                if(!tableFile.isAbsolute())
                    tableFile = new File(GalaxyDataCollection.getGalaxyDistFiles().getRootFolder(), path);
                try(BufferedReader reader = ApplicationUtils.asciiReader( tableFile ))
                {
                    String line;
                    while( ( line = reader.readLine() ) != null )
                    {
                        line = line.trim();
                        if(line.isEmpty() || line.startsWith(commentChar)) continue;
                        content.add(line.split(separatorChar));
                    }
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Unable to read file "+path, e);
            }
        }
    }
    
    public List<String> getColumns()
    {
        return Collections.unmodifiableList(columns);
    }
    
    public List<String[]> getContent()
    {
        load();
        return Collections.unmodifiableList(content);
    }

    /**
     * Construct from DOM element
     */
    DataTable(Element element)
    {
        name = element.getAttribute("name");
        type = getAttribute(element, "type", type);
        commentChar = getAttribute(element, "comment_char", commentChar);
        separatorChar = getAttribute(element, "separator", separatorChar);
        NodeList list = element.getElementsByTagName("columns");
        if(list.getLength() > 0)
        {
            Element columnsElement = (Element)list.item(0);
            columns = new ArrayList<>(Arrays.asList(columnsElement.getTextContent().trim().split(",\\s*")));
        }
        this.element = element;
    }
    
    DataTable(String name, List<String> columns, List<String[]> content)
    {
        this.name = name;
        this.columns = columns;
        this.content = content;
        this.loaded = true;
    }

    public String getName()
    {
        return name;
    }
}
