package ru.biosoft.galaxy;

import java.io.File;
import java.util.stream.Stream;

public class GalaxyDistFiles
{
    
    private static final String MIGRATED_TOOLS_CONF_XML = "migrated_tools_conf.xml";
    private static final String SHED_TOOL_CONF_XML = "shed_tool_conf.xml";
    private static final String TOOL_CONF_XML = "tool_conf.xml";
    
    private static final String SHED_TOOL_DATA_TABLE_CONF_XML = "shed_tool_data_table_conf.xml";
    private static final String TOOL_DATA_TABLE_CONF_XML = "tool_data_table_conf.xml";
    
    private static final String DATA_TYPES_CONF = "datatypes_conf.xml";
    
    private static final String TOOL_PATH = "tools";
    private static final String TOOL_DATA_PATH = "tool-data";
    private static final String LIB_PATH = "lib";
    private static final String EGGS_PATH = "eggs";
    private static final String TEST_DATA_PATH = "test-data";

    private File root;
    
    public GalaxyDistFiles(File root)
    {
        this.root = root;
    }
    
    public File getRootFolder() { return root; }
    
    public File getToolsFolder() { return new File(root, TOOL_PATH); }
    
    public File getToolDataFolder() { return new File(root, TOOL_DATA_PATH); }
    
    public File getLibFolder() { return new File(root, LIB_PATH); }
    
    public File getEggsFolder() { return new File(root, EGGS_PATH); }
    
    public File getVEnvFolder() { return new File(root, ".venv"); }
    
    public File getTestDataFolder() { return new File(root, TEST_DATA_PATH); }
    
    public File getConfigFolder() { return new File(root, "config");  }
    
    public File getToolConfXML() { return searchConfFile( TOOL_CONF_XML ); }
    
    public File getShedToolConfXML() { return searchConfFile( SHED_TOOL_CONF_XML ); }
    
    public File getMigratedToolsConfXML() { return searchConfFile( MIGRATED_TOOLS_CONF_XML ); }
    
    public File[] getToolConfFiles()
    {
        return Stream.of( getToolConfXML(), getShedToolConfXML(), getMigratedToolsConfXML() )
               .filter( File::exists ).toArray( File[]::new );
    }
    
    public File getDataTypesConfXML() { return searchConfFile( DATA_TYPES_CONF ); }

    public File getToolDataTableConfXML() { return searchConfFile( TOOL_DATA_TABLE_CONF_XML ); }
    public File getShedToolDataTableConfXML() { return searchConfFile( SHED_TOOL_DATA_TABLE_CONF_XML ); }
    public File[] getToolDataConfFiles()
    {
        return Stream.of( getToolDataTableConfXML(), getShedToolDataTableConfXML() )
               .filter( File::exists ).toArray( File[]::new );
    }
    
    private File searchConfFile(String name)
    {
        File res =  new File(root, name);
        if(res.exists())
            return res;
        
        res = new File(getConfigFolder(), name);
        if(res.exists())
            return res;
        
        res = new File(getConfigFolder(), name + ".sample");
        if(res.exists())
            return res;
        
        res = new File(getConfigFolder(), name + ".main");
        if(res.exists())
            return res;

        return new File(root, name);
    }

}
