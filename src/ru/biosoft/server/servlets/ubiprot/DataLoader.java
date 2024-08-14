package ru.biosoft.server.servlets.ubiprot;

import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.standard.type.Protein;

public class DataLoader
{
    private static final PropertyDescriptor PROTEIN_TYPE_DESCRIPTOR = StaticDescriptor.create("proteinType");
    
    protected String sourcePath;
    public DataLoader(String sourcePath)
    {
        this.sourcePath = sourcePath;
    }

    public UbiprotDiagramDescription getDiagramDescription(String lineNumber, String organism)
    {
        int pos = 0;
        try
        {
            pos = Integer.parseInt(lineNumber);
        }
        catch( Exception e )
        {
            return null;
        }
        String line = getLine(organism, pos);
        if( line != null )
        {
            UbiprotDiagramDescription result = new UbiprotDiagramDescription();
            String[] values = line.split("\t");
            if( values.length > 1 )
                result.setSubstrate(getProtein(values[0], values[1], null));

            if( values.length > 2 )
                result.setPreModification(values[2].trim());

            if( values.length > 4 )
                result.setPostModification(values[4].trim());

            List<Protein> e1_1 = getProteinArray(values, 8, 9, "catalytic");
            List<Protein> e1_2 = getProteinArray(values, 11, 12, "adaptor");
            List<Protein> e1_3 = getProteinArray(values, 14, 15, "partner");
            result.setE1(combineProteinLists(Arrays.asList(e1_1, e1_2, e1_3)));

            List<Protein> e2_1 = getProteinArray(values, 17, 18, "catalytic");
            List<Protein> e2_2 = getProteinArray(values, 20, 21, "adaptor");
            List<Protein> e2_3 = getProteinArray(values, 23, 24, "partner");
            result.setE2(combineProteinLists(Arrays.asList(e2_1, e2_2, e2_3)));

            List<Protein> e3_1 = getProteinArray(values, 26, 27, "simple");
            List<Protein> e3_5 = getProteinArray(values, 28, 28, "constant module");
            List<Protein> e3_2 = getProteinArray(values, 30, 31, "adaptor");
            List<Protein> e3_3 = getProteinArray(values, 33, 34, "target recognizing");
            List<Protein> e3_4 = getProteinArray(values, 36, 37, "partner");
            result.setE3(combineProteinLists(Arrays.asList(e3_1, e3_2, e3_3, e3_4, e3_5)));

            if( values.length > 40 )
                result.setDub(getProtein(values[39], values[40], null));
            if( values.length > 46 )
                result.setUbp(getProtein(values[45], values[46], null));

            return result;
        }

        return null;
    }

    protected Protein[] combineProteinLists(List<List<Protein>> proteinLists)
    {
        return StreamEx.of( proteinLists ).nonNull().flatMap( List::stream ).toArray( Protein[]::new );
    }

    protected List<Protein> getProteinArray(String[] values, int namePos, int titlePos, String type)
    {
        if( namePos != -1 && values.length > titlePos && values[namePos].length() > 0 )
        {
            String[] names = values[namePos].split("\\+");
            String[] titles = values[titlePos].split("\\+");
            return StreamEx.zip( names, titles, (name, title) -> getProtein( name, title, type ) ).toList();
        }
        return Collections.emptyList();
    }

    protected Protein getProtein(String name, String title, String type)
    {
        if( name.length() == 0 )
            return null;
        Protein result = new Protein(null, name);
        result.setTitle(title);
        result.setComment(name);
        try
        {
            if( type != null )
            {
                result.getAttributes().add(new DynamicProperty(PROTEIN_TYPE_DESCRIPTOR, String.class, type));
            }
        }
        catch( Exception e )
        {
        }
        return result;
    }

    protected Map<String, List<String>> linesMap = new HashMap<>();
    protected String getLine(String organism, int number)
    {
        if( !linesMap.containsKey(organism) )
        {
            readFile(organism);
        }

        List<String> lines = linesMap.get(organism);
        if( number >= lines.size() )
            return null;

        return lines.get(number);
    }

    protected void readFile(String organism)
    {
        List<String> lines = new ArrayList<>();
        URL url = this.getClass().getClassLoader().getResource( sourcePath + "diagrams_" + organism + ".txt" );
        try (BufferedReader in = new BufferedReader( new InputStreamReader( url.openStream() ) ))
        {
            String line;
            while( ( line = in.readLine() ) != null )
            {
                lines.add(line);
            }
            linesMap.put(organism, lines);
        }
        catch( Exception e )
        {
        }
    }
}
