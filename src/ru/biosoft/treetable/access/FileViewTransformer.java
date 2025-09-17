package ru.biosoft.treetable.access;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import ru.biosoft.access.file.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.treetable.TreeTableElement;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * TODO: this transformer should be replaced with FileEntryTransformer + BeanInfoEntryTransformer
 */
public class FileViewTransformer extends AbstractFileTransformer<TreeTableElement>
{
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*(.+)");
    private static final Pattern SECTION_PATTERN = Pattern.compile("\\[(.+)\\]");

    @Override
    public Class<TreeTableElement> getOutputType()
    {
        return TreeTableElement.class;
    }

    private void readSectionsFormat(String fileData, TreeTableElement treeTable) throws Exception
    {
        BufferedReader br = new BufferedReader(new StringReader(fileData));
        String line;
        StringBuilder script = new StringBuilder();
        String section = null;
        DataElementPath treePath = null;
        DataElementPath tablePath = null;
        boolean hideAbsentBranches = false;
        while( ( line = br.readLine() ) != null )
        {
            line = line.trim();
            Matcher matcher = SECTION_PATTERN.matcher(line);
            if(matcher.matches())
            {
                section = matcher.group(1);
            } else
            {
                if("Script".equals(section))
                {
                    script.append(line).append("\n");
                } else if("TreeTable".equals(section))
                {
                    matcher = KEY_VALUE_PATTERN.matcher(line);
                    if(matcher.matches())
                    {
                        String key = matcher.group(1);
                        String value = matcher.group(2);
                        switch( key )
                        {
                            case "TreePath":
                                treePath = DataElementPath.create(value);
                                break;
                            case "TablePath":
                                tablePath = DataElementPath.create(value);
                                break;
                            case "HideAbsentBranches":
                                hideAbsentBranches = Boolean.parseBoolean(value);
                                break;
                            default:
                                // unknown parameter: skip
                                break;
                        }
                    }
                }
            }
        }
        treeTable.setHideBranchesAbsentInTable(hideAbsentBranches);
        if(treePath == null)
            throw new Exception("TreePath absent in [TreeTable] section");
        treeTable.setTreePath(treePath);
        if(tablePath != null)
        {
            if(script.length() > 0)
                throw new Exception("Both TablePath and [Script] section appear");
            treeTable.setTableScript("data.get(\'"+StringEscapeUtils.escapeEcmaScript(tablePath.toString())+"\');");
        } else
        {
            if(script.length() == 0)
                throw new Exception("Either TablePath or [Script] section must appear");
            treeTable.setTableScript(script.toString());
        }
    }

    @Override
    public TreeTableElement load(File input, String name, DataCollection<TreeTableElement> origin) throws Exception
    {
        String fileData = ApplicationUtils.readAsString(input);
        
        TreeTableElement result = new TreeTableElement(name, origin);
        if(fileData.startsWith("[TreeTable]"))
        {
            try
            {
                readSectionsFormat(fileData, result);
            }
            catch( Exception e )
            {
                throw new Exception(input+": Invalid file format: "+e.getMessage());
            }
            return result;
        }

        // Old format goes here
        try (BufferedReader br = new BufferedReader( new StringReader( fileData ) ))
        {
            String line;
            while( ( line = br.readLine() ) != null )
            {
                if( line.startsWith( "TP " ) )
                {
                    result.setTreePath( DataElementPath.create( line.substring( 3 ).trim() ) );
                }
                else if( line.startsWith( "HD " ) )
                {
                    if( line.substring( 3 ).trim().equalsIgnoreCase( "true" ) )
                        result.setHideBranchesAbsentInTable( true );
                }
                else if( line.startsWith( "TS " ) )
                {
                    String oldScript = result.getTableScript();
                    if( oldScript == null )
                    {
                        result.setTableScript( line.substring( 3 ).trim() );
                    }
                    else
                    {
                        result.setTableScript( oldScript + "\n" + line.substring( 3 ).trim() );
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void save(File output, TreeTableElement element) throws Exception
    {
        throw new UnsupportedOperationException();
    }
}
