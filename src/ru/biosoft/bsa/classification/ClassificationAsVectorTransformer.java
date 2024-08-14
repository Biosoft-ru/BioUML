package ru.biosoft.bsa.classification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.Writer;
import java.util.StringTokenizer;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.application.ApplicationUtils;

/**
 * Format is:
 *
 *
 */
public class ClassificationAsVectorTransformer extends AbstractFileTransformer<ClassificationUnit>
{
    @Override
    public Class<ClassificationUnit> getOutputType()
    {
        return ClassificationUnit.class;
    }

    protected ClassificationUnitAsVector parseLine(DataCollection<ClassificationUnit> parent, String line) throws Exception
    {
        StringTokenizer tokens = new StringTokenizer(line, "|", true);

        // parse parent name
        String parentName = tokens.nextToken();
        if( !parentName.equals("|") )
        {
            if( parentName.startsWith("/") )
                parentName = parentName.substring(1);

            parent = getChild(parent, parentName);
            tokens.nextToken();
        }

        String name = tokens.nextToken();
        tokens.nextToken();

        ClassificationUnitAsVector result = new ClassificationUnitAsVector(parent, name, parseProperty(tokens), // classNumber
                parseProperty(tokens), // className
                parseProperty(tokens) // description
        );

        String attr;
        while( ( attr = parseProperty(tokens) ) != null )
        {
            int ind = attr.indexOf(':');
            if( ind != -1 )
            {
                result.getAttributes().add(new DynamicProperty(attr.substring(0, ind), String.class, attr.substring(ind + 1).trim()));
            }
        }
        return result;
    }

    protected String parseProperty(StringTokenizer tokens)
    {
        String value = null;
        if( tokens.hasMoreTokens() )
            value = tokens.nextToken();

        if( "|".equals(value) )
            value = null;
        else if( tokens.hasMoreTokens() )
            tokens.nextToken();

        return value;
    }

    protected void writeClassification(Writer writer, ClassificationUnit classification, int rootLen) throws Exception
    {
        if( classification.getParent() != null )
        {
            String parentName = classification.getParent().getCompletePath().toString();
            if( parentName.length() >= rootLen )
                parentName = parentName.substring(rootLen);
            writer.write(parentName);
        }
        writer.write('|');

        writer.write(classification.getName());
        writer.write('|');

        if( classification.getClassNumber() != null )
            writer.write(classification.getClassNumber());
        writer.write('|');

        if( classification.getClassName() != null )
            writer.write(classification.getClassName());

        if( classification.getDescription() != null )
        {
            writer.write('|');
            writer.write(classification.getDescription());
        }

        for(DynamicProperty dp : classification.getAttributes())
        {
            if( String.class.isAssignableFrom(dp.getType()) )
            {
                writer.write('|');
                writer.write(dp.getName());
                writer.write(':');
                writer.write((String)dp.getValue());
            }
        }

        writer.write('\n');

        for( int i = 0; i < classification.getSize(); i++ )
            writeClassification(writer, classification.getChild(i), rootLen);
    }

    public static DataCollection<ClassificationUnit> getChild(DataCollection<ClassificationUnit> parent, String relativeName) throws Exception
    {
        String[] pathComponents = DataElementPath.create(relativeName).getPathComponents();
        for( String pathComponent : pathComponents )
        {
            ClassificationUnit child = parent.get(pathComponent);
            if( child != null )
                parent = child;
        }
        return parent;
    }

    @Override
    public ClassificationUnit load(File input, String name, DataCollection<ClassificationUnit> origin) throws Exception
    {
        try(BufferedReader reader = ApplicationUtils.asciiReader( input ))
        {
            ClassificationUnit root = parseLine( origin, reader.readLine() );

            String line = null;
            while( ( line = reader.readLine() ) != null )
                parseLine( root, line );
            return root;
        }
    }

    @Override
    public void save(File output, ClassificationUnit element) throws Exception
    {
        try(BufferedWriter writer = ApplicationUtils.asciiWriter( output ))
        {
            ClassificationUnit classification = element;
            writeClassification(writer, classification, classification.getCompletePath().toString().length());
        }
    }
}
