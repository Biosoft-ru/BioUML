package ru.biosoft.galaxy;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Compare Galaxy tool result file with template file using specified compare method.
 * See http://wiki.g2.bx.psu.edu/Admin/Tools/Writing%20Tests for details
 * Possible comparators: contains,diff,re_match,re_search,sim_size,startswith
 */
public class ResultComparator
{
    public static final String COMPARE_SCRIPT = "Compare.py";
    private String type;
    private int difference;
    private boolean sort;

    public ResultComparator(String type, int difference, boolean sort)
    {
        this.type = type;
        this.difference = difference;
        this.sort = sort;
    }

    public boolean compare(File file1, File file2, Writer errors) throws Exception
    {
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(COMPARE_SCRIPT);
        command.add(file1.getAbsolutePath());
        command.add(file2.getAbsolutePath());
        command.add(getAttributesAsJSON());
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        GalaxyFactory.setupPythonEnvironment( processBuilder.environment() );
        processBuilder.directory(GalaxyFactory.getScriptPath());

        Process proc = processBuilder.start();
        proc.waitFor();
        if(proc.exitValue() == 0)
            return true;
        
        IOUtils.copy(proc.getErrorStream(), errors);
        return false;
    }
    
    private String getAttributesAsJSON() throws JSONException
    {
        JSONObject attributes = new JSONObject();
        attributes.put("sort", sort);
        attributes.put("compare", type);
        attributes.put("lines_diff", difference);
        attributes.put("delta", difference);
        return attributes.toString();
    }

    @Override
    public String toString()
    {
        return type + "(" + difference + ")" + (sort?" sorted":"");
    }
}
