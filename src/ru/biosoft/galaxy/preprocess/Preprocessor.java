package ru.biosoft.galaxy.preprocess;

import java.io.File;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ru.biosoft.util.XmlStream;
import ru.biosoft.util.XmlUtil;

public class Preprocessor
{
    private static Logger log = Logger.getLogger(Preprocessor.class.getName());

    private static final String MACROS_ELEMENT = "macros";
    private static final String MACRO_ELEMENT = "macro";
    private static final String IMPORT_ELEMENT = "import";

    private static final String TYPE_XML = "xml";
    private static final String TYPE_TOKEN = "token";


    /** Path to search for included files */
    private final File searchPath;

    public Preprocessor(File searchPath)
    {
        this.searchPath = searchPath;
    }

    /**
     * Preproces given xml element
     * @param input - xml element to preprocess, input will be modified by preprocessor
     */
    public void run(Element input)
    {
        Element macrosElement = XmlUtil.getChildElement(input, MACROS_ELEMENT);
        if( macrosElement == null )
            return;

        List<Element> topLevel = getTopLevelElements(macrosElement);

        Map<String, GalaxyMacro> macros = parseMacros(topLevel);
        GalaxyMacro.applyMacros(macros, input);

        Map<String, String> tokens = parseTokens(topLevel);
        applyTokens(tokens, input);
    }

    private Map<String, GalaxyMacro> parseMacros(List<Element> macrosElements)
    {
        Map<String, GalaxyMacro> result = new HashMap<>();

        for( Element macrosElem : macrosElements )
            for( Element macroElem : findMacrosByType(macrosElem, TYPE_XML) )
            {
                GalaxyMacro macro = new GalaxyMacro(macroElem);
                result.put(macro.getName(), macro);
            }
        for( GalaxyMacro macro : result.values() )
            macro.init(result);

        return result;
    }

    private Map<String, String> parseTokens(List<Element> macrosElements)
    {
        Map<String, String> result = new HashMap<>();

        for( Element macrosElem : macrosElements )
            for( Element tokenElem : findMacrosByType(macrosElem, TYPE_TOKEN) )
            {
                String tokenName = tokenElem.getAttribute("name");
                String tokenValue = XmlUtil.getTextContent(tokenElem);
                result.put(tokenName, tokenValue);
            }

        return result;
    }

    private List<Element> findMacrosByType(Element root, String type)
    {
        List<Element> result = new ArrayList<>();
        for( Element child : XmlUtil.elements(root, type) )
            result.add(child);
        for( Element macro : XmlUtil.elements(root, MACRO_ELEMENT) )
        {
            String curType = XmlUtil.getAttribute(macro, type, TYPE_XML);
            if( curType.equals(type) )
                result.add(macro);
        }
        return result;
    }

    private List<Element> getTopLevelElements(Element root)
    {
        List<Element> result = new ArrayList<>();
        result.add(root);
        for( Element importElem : XmlUtil.elements(root, IMPORT_ELEMENT) )
        {
            File file = new File(searchPath, importElem.getTextContent().trim());

            Document doc = null;
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.parse(file);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not parse " + file.getAbsolutePath(), e);
            }
            if( doc != null )
                result.addAll(getTopLevelElements(doc.getDocumentElement()));
        }
        return result;
    }

    private void applyTokens(Map<String, String> tokens, Element input)
    {
        if( input.hasChildNodes() )
            substituteTokens(tokens, input.getFirstChild());

        NamedNodeMap attributes = input.getAttributes();
        for( int i = 0; i < attributes.getLength(); i++ )
            substituteTokens(tokens, attributes.item(i));
        
        for(Element element : XmlStream.elements( input ))
            applyTokens(tokens, element);
    }

    private void substituteTokens(Map<String, String> tokens, Node node)
    {
        String text = node.getNodeValue();
        if(text == null)
            return;
        String newText = substituteTokens(tokens, text);
        if( !text.equals(newText) )
            node.setNodeValue(newText);
    }

    private String substituteTokens(Map<String, String> tokens, String text)
    {
        for( Map.Entry<String, String> token : tokens.entrySet() )
            text = text.replace(token.getKey(), token.getValue());
        return text;
    }

}
