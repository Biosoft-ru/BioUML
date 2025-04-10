package biouml.plugins.helpgenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.equinox.app.IApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.XmlStream;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.workbench.ConsoleApplicationSupport;

/**
 * @author lan
 *
 */
public class HelpGenerator extends ConsoleApplicationSupport
{
    public static final String HELP_PATH = "help/analyses";
    
    private Document contents;
    private final Map<String, Document> groupDocuments = new HashMap<>();
    private final Map<String, Element> groupDocumentElements = new HashMap<>();
    private final Map<String, Element> tocGroups = new HashMap<>();
    private Element contentsList;
    private Document toc;
    private Element tocList;

    /**
     * @author lan
     *
     */
    private static final class DescriptionParserCallback extends HTMLEditorKit.ParserCallback
    {
        Document doc;
        private int listId = 1;
        private boolean insidePre = false;
        
        private static enum ElementType
        {
            NONE, TEXT, PARA, TABLE
        }
        
        private static class ElementInfo
        {
            private final Element element;
            private final Tag t;
            private final ElementType type;
            
            public ElementInfo(Element element, Tag t, ElementType type)
            {
                this.element = element;
                this.t = t;
                this.type = type;
            }

            public Element getElement()
            {
                return element;
            }

            public Tag getTag()
            {
                return t;
            }

            public boolean isText()
            {
                return type == ElementType.TEXT;
            }

            public boolean isPara()
            {
                return type == ElementType.PARA;
            }
            
            public boolean isTable()
            {
                return type == ElementType.TABLE;
            }
        }
        
        List<ElementInfo> tags = new ArrayList<>();
        private final URL base;
        private final String name;
        
        private ElementInfo getLastText()
        {
            for(int i=tags.size()-1; i>=0; i--)
            {
                ElementInfo elementInfo = tags.get(i);
                if(elementInfo.isTable()) return null;
                if(elementInfo.isText()) return elementInfo;
            }
            return null;
        }
        
        private ElementInfo getLastPara()
        {
            for(int i=tags.size()-1; i>=0; i--)
            {
                ElementInfo elementInfo = tags.get(i);
                if(elementInfo.isTable()) return null;
                if(elementInfo.isPara()) return elementInfo;
            }
            return null;
        }
        
        private ElementInfo getLastTag(Tag t)
        {
            for(int i=tags.size()-1; i>=0; i--)
            {
                ElementInfo elementInfo = tags.get(i);
                if(elementInfo.getTag() == t) return elementInfo;
            }
            return null;
        }
        
        private void closeTo(ElementInfo elementInfo)
        {
            if(elementInfo == null) return;
            for(int i=tags.size()-1; i>=0; i--)
            {
                if(tags.get(i)==elementInfo)
                {
                    tags.subList(i, tags.size()).clear();
                }
            }
        }
        
        private void closeToExcluding(ElementInfo elementInfo)
        {
            for(int i=tags.size()-1; i>=0; i--)
            {
                if(tags.get(i)==elementInfo)
                {
                    tags.subList(i+1, tags.size()).clear();
                }
            }
        }
        
        private void closePara()
        {
            closeTo(getLastPara());
        }
        
        private void closeText()
        {
            closeTo(getLastText());
        }
        
        private Element getLastElement()
        {
            return tags.get(tags.size()-1).getElement();
        }
        
        private ElementInfo addElement(Element element, Tag t, ElementType type)
        {
            getLastElement().appendChild(element);
            ElementInfo elementInfo = new ElementInfo(element, t, type);
            tags.add(elementInfo);
            return elementInfo;
        }
        
        public DescriptionParserCallback(Document doc, Element startTag, URL base, String name)
        {
            this.doc = doc;
            this.base = base;
            this.name = name;
            tags.add(new ElementInfo(startTag, HTML.Tag.BODY, ElementType.NONE));
        }
        
        @Override
        public void handleStartTag(Tag t, MutableAttributeSet a, int pos)
        {
            if(t == HTML.Tag.HEAD || t == HTML.Tag.BODY || t == HTML.Tag.HTML) return;
            if(t.isBlock())
            {
                if(t != HTML.Tag.DIV) closePara();
                if(t==HTML.Tag.H1)
                {
                    addElement(createElement(doc, "para", "styleclass", "Heading1"), t, ElementType.PARA);
                } else if(t==HTML.Tag.H2)
                {
                    addElement(createElement(doc, "para", "styleclass", "Heading2"), t, ElementType.PARA);
                } else if(t==HTML.Tag.H3 || t==HTML.Tag.H4 || t==HTML.Tag.H5 || t==HTML.Tag.H6)
                {
                    addElement(createElement(doc, "para", "styleclass", "Heading3"), t, ElementType.PARA);
                } else if(t==HTML.Tag.UL)
                {
                    Element element = createElement(doc, "list", "type", "ul", "listtype", "bullet", "formatstring", "o",
                            "format-charset", "SYMBOL_CHARSET", "levelreset", "true", "legalstyle", "false", "id",
                            String.valueOf(listId++), "style", "font-family:Symbol; font-size:10pt; color:#000000;");
                    long level = StreamEx.of( tags ).filter( this::isListTag ).count();
                    if(level > 0)
                        element.setAttribute("level", String.valueOf(level));
                    addElement(element, t, ElementType.NONE);
                } else if(t==HTML.Tag.OL)
                {
                    Element element = createElement(doc, "list", "type", "ol", "listtype", "decimal", "formatstring", "%0:s.",
                            "format-charset", "SYMBOL_CHARSET", "levelreset", "true", "legalstyle", "false", "id",
                            String.valueOf(listId++), "style", "font-family:Arial; font-size:10pt; color:#000000;");
                    long level = StreamEx.of( tags ).filter( this::isListTag ).count();
                    if(level > 0)
                        element.setAttribute("level", String.valueOf(level));
                    addElement(element, t, ElementType.NONE);
                } else if(t==HTML.Tag.LI)
                {
                    addElement(createNormalElement(doc, "li"), t, ElementType.PARA);
                } else if(t==HTML.Tag.PRE)
                {
                    insidePre = true;
                    addElement(createElement(doc, "para", "styleclass", "Code Example"), t, ElementType.PARA);
                } else if(t==HTML.Tag.DIV)
                {
                    getCurrentPara();
                    getLastElement().appendChild(doc.createElement("br"));
                } else if(t==HTML.Tag.TABLE)
                {
                    addElement(createNormalElement(doc, "para"), t, ElementType.PARA);
                    addElement(createElement(doc, "table", "styleclass", "Default"), null, ElementType.TABLE);
                } else if(t==HTML.Tag.TR)
                {
                    addElement(createElement(doc, "tr", "style", "vertical-align:middle"), t, ElementType.TABLE);
                } else if(t==HTML.Tag.TD || t==HTML.Tag.TH)
                {
                    Element element = createElement(doc, "td");
                    Object colspanObj = a.getAttribute(HTML.Attribute.COLSPAN);
                    Object rowspanObj = a.getAttribute(HTML.Attribute.ROWSPAN);
                    if(colspanObj != null) element.setAttribute("colspan", colspanObj.toString());
                    if(rowspanObj != null) element.setAttribute("rowspan", rowspanObj.toString());
                    addElement(element, t, ElementType.TABLE);
                } else
                {
                    addElement(createNormalElement(doc, "para"), t, ElementType.PARA);
                }
            } else
            {
                if(t==HTML.Tag.B || t==HTML.Tag.STRONG)
                {
                    addElement(
                            createElement(doc, "text", "styleclass", getCurrentPara().getElement().getAttribute("styleclass"), "translate",
                                    "true", "style", "font-weight:bold;"), t, ElementType.TEXT);
                } else if(t==HTML.Tag.I || t==HTML.Tag.EM)
                {
                    addElement(
                            createElement(doc, "text", "styleclass", getCurrentPara().getElement().getAttribute("styleclass"), "translate",
                                    "true", "style", "font-style:italic;"), t, ElementType.TEXT);
                } else if(t==HTML.Tag.CODE || t==HTML.Tag.TT)
                {
                    addElement(createElement(doc, "text", "styleclass", "Code Example", "translate", "true"), t, ElementType.TEXT);
                } else if(t==HTML.Tag.SUP)
                {
                    addElement(
                            createElement(doc, "text", "styleclass", getCurrentPara().getElement().getAttribute("styleclass"), "translate",
                                    "true", "style", "font-size:7pt; vertical-align:super;"), t, ElementType.TEXT);
                } else if(t==HTML.Tag.SUB)
                {
                    addElement(
                            createElement(doc, "text", "styleclass", getCurrentPara().getElement().getAttribute("styleclass"), "translate",
                                    "true", "style", "font-size:7pt; vertical-align:sub;"), t, ElementType.TEXT);
                } else if(t==HTML.Tag.A)
                {
                    Object hrefObj = a.getAttribute(HTML.Attribute.HREF);
                    if(hrefObj != null)
                    {
                        String href = hrefObj.toString();
                        if(href.startsWith("http://") || href.startsWith("https://"))
                        {
                            Element element = createElement(doc, "link", "styleclass", getCurrentPara().getElement().getAttribute("styleclass"),
                                    "translate", "true", "target", "_blank", "href", href, "defaultstyle", "true", "type", "weblink",
                                    "displaytype", "text");
                            addElement(element, t, ElementType.TEXT);
                        }
                    }
                }
            }
        }

        private boolean isListTag(ElementInfo tag)
        {
            return tag.getTag() == HTML.Tag.UL || tag.getTag() == HTML.Tag.OL;
        }
        
        @Override
        public void handleEndTag(Tag t, int pos)
        {
            if(t==HTML.Tag.PRE) insidePre = false;
            if(t==HTML.Tag.DIV)
            {
                getCurrentPara();
                getLastElement().appendChild(doc.createElement("br"));
            }
            if(t==HTML.Tag.P)
            {
                closeTo(getLastPara());
            } else
            {
                closeTo(getLastTag(t));
            }
        }
        
        @Override
        public void handleSimpleTag(Tag t, MutableAttributeSet a, int pos)
        {
            if(t == HTML.Tag.BR)
            {
                getCurrentPara();
                getLastElement().appendChild(doc.createElement("br"));
            } else if(t==HTML.Tag.IMG)
            {
                getCurrentPara();
                Object srcObj = a.getAttribute(HTML.Attribute.SRC);
                if(srcObj != null)
                {
                    String src = srcObj.toString();
                    try
                    {
                        URL url = src.contains("://")?new URL(src):new URL(base, src);
                        URLConnection connection = url.openConnection();
                        String type = TextUtil2.split( connection.getContentType(), '/' )[1];
                        InputStream inputStream = connection.getInputStream();

                        String imgName = src.contains("/")?src.substring(src.lastIndexOf("/")):src;
                        imgName = imgName.replaceAll("\\W+", "-");
                        String fileName = name+"-"+imgName+"."+type;
                        File newFile = new File(HELP_PATH+"/Baggage", fileName);
                        ApplicationUtils.copyStream(new FileOutputStream(newFile), inputStream);
                        getLastElement().appendChild(createElement(doc, "image", "src", fileName, "scale", "100.00%", "styleclass", "Image Caption"));
                    }
                    catch( Exception e )
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        @Override
        public void handleEndOfLineString(String eol)
        {
        }

        @Override
        public void handleText(char[] data, int pos)
        {
            String str = new String(data);
            if(str.isEmpty()) return;
            ElementInfo lastText = getLastText();
            if(lastText == null)
            {
                ElementInfo lastPara = getCurrentPara();
                lastText = addElement(
                        createElement(doc, "text", "translate", "true", "styleclass", lastPara.getElement().getAttribute("styleclass")),
                        null, ElementType.TEXT);
            }
            if(insidePre)
            {
                str = str.replaceAll("  ", " \u00A0");
                boolean first = true;
                for(String subStr: str.split("\n"))
                {
                    if(!first) lastText.getElement().appendChild(doc.createElement("br"));
                    first = false;
                    lastText.getElement().appendChild(doc.createTextNode(subStr));
                }
            } else
            {
                lastText.getElement().appendChild(doc.createTextNode(str));
            }
        }

        /**
         * @return current paragraph (creates it if there's no one)
         */
        private ElementInfo getCurrentPara()
        {
            ElementInfo lastPara = getLastPara();
            if(lastPara == null)
            {
                Element para = createNormalElement(doc, "para");
                getLastElement().appendChild(para);
                lastPara = new ElementInfo(para, null, ElementType.PARA);
                tags.add(lastPara);
            }
            return lastPara;
        }
    }

    @Override
    public Object start(IApplicationContext arg0) throws Exception
    {
        try
        {
            CollectionFactory.createRepository("data");
            CollectionFactory.createRepository("data_resources");
            loadPreferences("preferences.xml");
            createContents();
            new File(HELP_PATH+"/Baggage/").mkdirs();
            new File(HELP_PATH+"/Topics/").mkdirs();
            new File(HELP_PATH+"/Maps/").mkdirs();
            for(String analysisName: AnalysisMethodRegistry.getAnalysisNamesWithGroup())
            {
                int pos = analysisName.indexOf("/");
                String groupName = analysisName.substring(0, pos);
                String analysis = analysisName.substring(pos+1);
                processAnalysis(groupName, AnalysisMethodRegistry.getMethodInfo(analysis));
            }
            writeXml(contents, new File(HELP_PATH+"/Topics/Analyses.xml"));
            writeXml(toc, new File(HELP_PATH+"/Maps/table_of_contents.xml"));
            for(Entry<String, Document> entry: groupDocuments.entrySet())
            {
                writeXml(entry.getValue(), new File(HELP_PATH+"/Topics/"+entry.getKey().replace(" ", "-")+".xml"));
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        return null;
    }
    
    private void processAnalysis(String groupName, AnalysisMethodInfo analysis) throws Exception
    {
        System.out.println(groupName + "/" + analysis.getName());
        File dir = new File(HELP_PATH+"/Topics/");
        dir.mkdirs();
        String html = analysis.getDescriptionHTML();
        Document doc = createDocument();
        Element topic = createTopic(doc);
        doc.appendChild(topic);
        Element title = createTitle(doc, analysis.getName());
        topic.appendChild(title);
        Element body = doc.createElement("body");
        topic.appendChild(body);
        String fileName = groupName.replace(" ", "-")+"-"+analysis.getName().replaceFirst("\\s*\\(\\w*\\*.+\\)", "").replace("/", ",").replace(" ", "-");
        HTMLEditorKit.ParserCallback callback = new DescriptionParserCallback(doc, body, analysis.getBase(), fileName);
        Reader reader = new StringReader(html);
        new ParserDelegator().parse(reader, callback, false);
        postprocessAnalysis(doc.getDocumentElement());
        
        addAnalysisToContents(groupName, analysis.getName(), fileName);
        writeXml(doc, new File(dir, fileName+".xml"));
        ApplicationUtils.writeString(new File(dir, fileName+".html"), html);
    }

    private void postprocessAnalysis(Element root)
    {
        NodeList childNodes = root.getChildNodes();
        for(int i=0; i<childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if(node instanceof Element)
            {
                postprocessAnalysis((Element)node);
                Element element = (Element)node;
                if(element.getTagName().equals("para") && element.getChildNodes().getLength() == 0)
                {
                    root.removeChild(node);
                    i--;
                }
                if(element.getTagName().equals("text") || element.getTagName().equals("link"))
                {
                    List<Element> elements = flattenTextElements((Element)node);
                    for(Element subElement: elements)
                    {
                        root.insertBefore(subElement, node);
                        i++;
                    }
                    root.removeChild(node);
                    i--;
                }
            }
        }
    }

    private List<Element> flattenTextElements(Element parent)
    {
        NodeList childNodes = parent.getChildNodes();
        List<Element> newNodes = new ArrayList<>();
        for(int i=0; i<childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            Element newNode = parent.getOwnerDocument().createElement(
                    node instanceof Element ? ( (Element)node ).getTagName() : parent.getTagName());
            if(!newNode.getTagName().equalsIgnoreCase("BR")) copyAttributes(newNode, parent);
            if(node instanceof Element)
            {
                Map<String, String> stylesMap = parseStyle(newNode);
                copyAttributes(newNode, (Element)node);
                stylesMap.putAll(parseStyle((Element)node));
                setStyle(newNode, stylesMap);
            }
            newNode.appendChild(parent.getOwnerDocument().createTextNode(node.getTextContent()));
            newNodes.add(newNode);
        }
        return newNodes;
    }

    private void copyAttributes(Element to, Element from)
    {
        XmlStream.attributes( from )
            .filterKeyValue( (name, value) -> !(name.equals("styleclass") && value.equals("Normal") && to.hasAttribute( "styleclass" )))
            .forKeyValue( to::setAttribute );
    }

    private Map<String, String> parseStyle(Element element)
    {
        return StreamEx.split(element.getAttribute( "style" ), ';')
                .map( style -> TextUtil2.split( style.trim(), ':' ) )
                .filter( fields -> fields.length >= 2 )
                .toMap( fields -> fields[0].trim(), fields -> fields[1].trim() );
    }
    
    private void setStyle(Element element, Map<String, String> style)
    {
        String styles = EntryStream.of( style ).map( entry -> entry.getKey() + ": " + entry.getValue() ).joining( "; " );
        if(styles.isEmpty()) element.removeAttribute("style");
        else element.setAttribute("style", styles);
    }

    /**
     * @return
     * @throws ParserConfigurationException
     */
    private Document createDocument() throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        return doc;
    }

    /**
     * @param doc
     * @param xml
     * @throws FileNotFoundException
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerConfigurationException
     * @throws TransformerException
     * @throws IOException
     */
    private void writeXml(Document doc, File xml) throws FileNotFoundException, TransformerFactoryConfigurationError,
            TransformerConfigurationException, TransformerException, IOException
    {
        DOMSource source = new DOMSource(doc);
        try (OutputStream stream = new FileOutputStream( xml ))
        {
            StreamResult result = new StreamResult( stream );
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty( javax.xml.transform.OutputKeys.MEDIA_TYPE, "text/xml" );
            transformer.setOutputProperty( javax.xml.transform.OutputKeys.INDENT, "yes" );
            transformer.transform( source, result );
        }
    }

    private void createContents() throws Exception
    {
        contents = createDocument();
        Element topic = createTopic(contents);
        contents.appendChild(topic);
        Element title = createTitle(contents, "Analyses");
        topic.appendChild(title);
        Element contentsBody = contents.createElement("body");
        topic.appendChild(contentsBody);
        Element header = createElement(contents, "para", "styleclass", "Heading1");
        Element headerText = createElement(contents, "text", "styleclass", "Heading1", "translate", "true");
        headerText.appendChild(contents.createTextNode("Analyses"));
        header.appendChild(headerText);
        contentsBody.appendChild(header);
        contentsList = createNormalElement(contents, "list", "type", "ul", "listtype", "bullet", "formatstring", "\u00B7",
                "format-charset", "SYMBOL_CHARSET", "levelreset", "true", "legalstyle", "false", "id", "1", "style",
                "font-family:Symbol; font-size:10pt; color:#000000;");
        contentsBody.appendChild(contentsList);
        
        toc = createDocument();
        Element mapElement = createElement(toc, "map", "xmlns:xsi", "http://www.w3.org/2001/XInclude");
        toc.appendChild(mapElement);
        tocList = createElement(toc, "topicref", "type", "topic", "build", "ALL", "icon", "0", "href", "Analyses");
        Element caption = createElement(toc, "caption", "translate", "true");
        caption.appendChild(toc.createTextNode("Analyses"));
        tocList.appendChild(caption);
        mapElement.appendChild(tocList);
    }
    
    private void createSubContents(String name) throws Exception
    {
        Document subContents = createDocument();
        Element topic = createTopic(subContents);
        subContents.appendChild(topic);
        Element title = createTitle(subContents, name);
        topic.appendChild(title);
        Element contentsBody = subContents.createElement("body");
        topic.appendChild(contentsBody);
        Element header = createElement(subContents, "para", "styleclass", "Heading1");
        Element headerText = createElement(subContents, "text", "styleclass", "Heading1", "translate", "true");
        headerText.appendChild(subContents.createTextNode(name));
        header.appendChild(headerText);
        contentsBody.appendChild(header);
        Element subContentsList = createNormalElement(subContents, "list", "type", "ul", "listtype", "bullet", "formatstring", "\u00B7",
                "format-charset", "SYMBOL_CHARSET", "levelreset", "true", "legalstyle", "false", "id", "1", "style",
                "font-family:Symbol; font-size:10pt; color:#000000;");
        contentsBody.appendChild(subContentsList);
        
        Element tocElement = createElement(toc, "topicref", "type", "topic", "build", "ALL", "icon", "0", "href", name.replace(" ", "-"));
        Element caption = createElement(toc, "caption", "translate", "true");
        caption.appendChild(toc.createTextNode(name));
        tocElement.appendChild(caption);
        tocList.appendChild(tocElement);
        
        groupDocuments.put(name, subContents);
        groupDocumentElements.put(name, subContentsList);
        tocGroups.put(name, tocElement);
    }

    private void addAnalysisToContents(String groupName, String name, String linkedFile) throws Exception
    {
        if(!groupDocuments.containsKey(groupName))
        {
            createSubContents(groupName);
            Element listItem = createNormalElement(contents, "li");
            Element link = createNormalElement(contents, "link", "displaytype", "text", "defaultstyle", "true", "type", "topiclink", "href",
                    groupName.replace(" ", "-"), "translate", "true");
            link.appendChild(contents.createTextNode(groupName));
            listItem.appendChild(link);
            contentsList.appendChild(listItem);
        }
        Element group = groupDocumentElements.get(groupName);
        Document contents = groupDocuments.get(groupName);
        Element listItem = createNormalElement(contents, "li");
        Element link = createNormalElement(contents, "link", "displaytype", "text", "defaultstyle", "true", "type", "topiclink", "href",
                linkedFile, "translate", "true");
        link.appendChild(contents.createTextNode(name));
        listItem.appendChild(link);
        group.appendChild(listItem);
        
        Element tocElement = createElement(toc, "topicref", "type", "topic", "build", "ALL", "icon", "0", "href", linkedFile);
        Element caption = createElement(toc, "caption", "translate", "true");
        caption.appendChild(toc.createTextNode(name));
        tocElement.appendChild(caption);
        tocGroups.get(groupName).appendChild(tocElement);
    }

    private static Element createElement(Document doc, String tagName, String... attributes)
    {
        Element result = doc.createElement(tagName);
        for(int i=0; i<attributes.length; i+=2)
        {
            result.setAttribute(attributes[i], attributes[i+1]);
        }
        return result;
    }

    private static Element createNormalElement(Document doc, String tagName, String... attributes)
    {
        Element result = createElement(doc, tagName, attributes);
        result.setAttribute("styleclass", "Normal");
        return result;
    }

    /**
     * @param doc
     * @param analysis
     * @return
     */
    private Element createTitle(Document doc, String titleString)
    {
        Element title = createElement(doc, "title", "translate", "true");
        title.appendChild(doc.createTextNode(titleString));
        return title;
    }

    /**
     * @param doc
     * @return
     */
    private Element createTopic(Document doc)
    {
        Element topic = createElement(doc, "topic", "template", "Default", "lasteditedby", "autogenerated", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", "xsi:noNamespaceSchemaLocation", "../helpproject.xsd");
        return topic;
    }
}
