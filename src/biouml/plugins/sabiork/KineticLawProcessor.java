package biouml.plugins.sabiork;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ru.biosoft.util.XmlStream;
import biouml.model.dynamics.Variable;

public class KineticLawProcessor
{
    protected static final Logger log = Logger.getLogger(KineticLawProcessor.class.getName());

    public static final String PARAMETER_ELEMENT = "Parameter";
    public static final String PARAMETER_NAME_ELEMENT = "ParameterName";
    public static final String SPECIES_ELEMENT = "Species";
    public static final String START_VALUE_ELEMENT = "StartValue";

    protected String[] kineticLaw;
    protected List<Variable> parameters;
    protected List<Variable> variables;

    public KineticLawProcessor(String kineticLaw)
    {
        this.kineticLaw = splitKineticLaw(kineticLaw);
        this.parameters = new ArrayList<>();
        this.variables = new ArrayList<>();
    }

    public String getKineticLaw()
    {
        StringBuffer result = new StringBuffer();
        for( String s : kineticLaw )
        {
            result.append(s);
        }
        return result.toString();
    }

    public Variable[] getParameters()
    {
        return parameters.toArray(new Variable[parameters.size()]);
    }

    public Variable[] getVariables()
    {
        return variables.toArray(new Variable[parameters.size()]);
    }

    public void parseParameters(String xml)
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = null;

            xml = xml.replaceAll("[\\x01\\x02\\x03\\x04\\x05\\x06\\x0b\\x0c\\x0f\\x12\\x14\\x16\\x92\\x1a\\x1c\\x1e\\xff]", "?");
            InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

            doc = builder.parse(stream);
            readParameters(doc);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Parse parameters error: " + e.getMessage());
        }
    }

    protected void readParameters(Document doc)
    {
        Element root = doc.getDocumentElement();
        XmlStream.elements( root, PARAMETER_ELEMENT ).forEach( this::readParameter );
    }

    protected void readParameter(Element parameter)
    {
        Element[] pName = getElements(parameter, PARAMETER_NAME_ELEMENT);
        if( pName.length == 0 )
        {
            log.log(Level.SEVERE, "Can not find " + PARAMETER_NAME_ELEMENT + " element");
            return;
        }
        Element element = pName[0];
        String parameterName = element.getTextContent();
        parameterName = parameterName.trim();
        Element[] species = getElements(parameter, SPECIES_ELEMENT);
        if( species.length > 0 )
        {
            String specieName = species[0].getTextContent().trim();
            if( specieName.startsWith("[") )
            {
                //delete "[...]" from specie name
                int end = specieName.indexOf("]");
                specieName = specieName.substring(end + 1, specieName.length()).trim();
            }

            Element[] initialValues = getElements(parameter, START_VALUE_ELEMENT);
            String initialValue = initialValues[0].getTextContent().trim();
            if( initialValue != null )
            {
                if( initialValue.equals("null") )
                {
                    initialValue = "1.0";
                }
            }

            if( specieName != null && !specieName.equals("null") )
            {
                //this variable should be replaced
                replaceFormulaElement(parameterName, specieName);
                Variable variable = new Variable(specieName, null, null);
                variable.setInitialValue(Double.parseDouble(initialValue));
                variables.add(variable);
            }
            else
            {
                //add to parameters
                if( initialValue != null )
                {
                    Variable variable = new Variable(parameterName, null, null);
                    variable.setInitialValue(Double.parseDouble(initialValue));
                    parameters.add(variable);
                }
            }
        }
    }

    public static Element[] getElements(Element element, String childName)
    {
        return XmlStream.elements( element, childName ).toArray( Element[]::new );
    }

    protected String[] splitKineticLaw(String kineticLaw)
    {
        List<String> result = new ArrayList<>();
        int start = 0;
        int end;
        while( ( end = signIndex(kineticLaw, start) ) != -1 )
        {
            result.add(kineticLaw.substring(start, end));
            start = end;
            end = start + 1;
            result.add(kineticLaw.substring(start, end));
            start = end;
        }
        result.add(kineticLaw.substring(start, kineticLaw.length()));

        return result.toArray(new String[result.size()]);
    }

    private final String[] signs = new String[] {"+", "-", "*", "/", "^", "(", ")"};

    public boolean isSign(String str)
    {
        for( String sign : signs )
        {
            if( str.equals(sign) )
            {
                return true;
            }
        }
        return false;
    }

    private int signIndex(String string, int startPos)
    {
        int result = -1;
        for( String sign : signs )
        {
            int tmp = string.indexOf(sign, startPos);
            if( result == -1 || ( tmp != -1 && tmp < result ) )
            {
                result = tmp;
            }
        }
        return result;
    }

    protected void replaceFormulaElement(String oldName, String newName)
    {
        for( int i = 0; i < kineticLaw.length; i++ )
        {
            if( kineticLaw[i].trim().equals(oldName) )
            {
                kineticLaw[i] = newName;
            }
        }
    }

    public String[] elements()
    {
        return kineticLaw;
    }
}
