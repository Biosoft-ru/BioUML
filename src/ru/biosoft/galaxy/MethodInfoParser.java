package ru.biosoft.galaxy;

import static ru.biosoft.util.XmlUtil.getAttributeOrText;
import static ru.biosoft.util.XmlUtil.getChildElement;
import static ru.biosoft.util.XmlUtil.getTextContent;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.galaxy.filters.Filter;
import ru.biosoft.galaxy.filters.FilterFactory;
import ru.biosoft.galaxy.filters.SourceStaticFilter;
import ru.biosoft.galaxy.parameters.ArrayParameter;
import ru.biosoft.galaxy.parameters.BooleanParameter;
import ru.biosoft.galaxy.parameters.ConditionalParameter;
import ru.biosoft.galaxy.parameters.ConfigParameter;
import ru.biosoft.galaxy.parameters.DataColumnParameter;
import ru.biosoft.galaxy.parameters.FileParameter;
import ru.biosoft.galaxy.parameters.FloatParameter;
import ru.biosoft.galaxy.parameters.HiddenParameter;
import ru.biosoft.galaxy.parameters.MetaParameter;
import ru.biosoft.galaxy.parameters.MultiFileParameter;
import ru.biosoft.galaxy.parameters.Parameter;
import ru.biosoft.galaxy.parameters.SelectParameter;
import ru.biosoft.galaxy.parameters.StringParameter;
import ru.biosoft.galaxy.preprocess.Preprocessor;
import ru.biosoft.galaxy.validators.Validator;
import ru.biosoft.galaxy.validators.ValidatorFactory;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.XmlStream;
import ru.biosoft.util.XmlUtil;

/**
 * Parser for galaxy method XML file
 */
public class MethodInfoParser
{
    protected static final String DESCRIPTION_ELEMENT = "description";
    protected static final String COMMAND_ELEMENT = "command";
    protected static final String INPUTS_ELEMENT = "inputs";
    protected static final String OUTPUTS_ELEMENT = "outputs";
    protected static final String TESTS_ELEMENT = "tests";
    protected static final String TEST_ELEMENT = "test";
    protected static final String PARAM_ELEMENT = "param";
    protected static final String REPEAT_ELEMENT = "repeat";
    protected static final String CONDITIONAL_ELEMENT = "conditional";
    protected static final String PAGE_ELEMENT = "page";
    protected static final String WHEN_ELEMENT = "when";
    protected static final String DATA_ELEMENT = "data";
    protected static final String OUTPUT_ELEMENT = "output";
    protected static final String HELP_ELEMENT = "help";
    protected static final String CONFIGFILES_ELEMENT = "configfiles";
    protected static final String CONFIGFILE_ELEMENT = "configfile";
    protected static final String OPTION_ELEMENT = "option";
    protected static final String OPTIONS_ELEMENT = "options";
    protected static final String PARAM_TRANSLATIONS_ELEMENT = "request_param_translation";
    protected static final String PARAM_TRANSLATION_ELEMENT = "request_param";
    protected static final String VALUE_TRANSLATION_ELEMENT = "value_translation";
    protected static final String APPEND_PARAMETERS_ELEMENT = "append_param";
    protected static final String VALUE_ELEMENT = "value";
    protected static final String DISPLAY_ELEMENT = "display";
    protected static final String COLUMN_ELEMENT = "column";
    protected static final String FILTER_ELEMENT = "filter";
    protected static final String VALIDATOR_ELEMENT = "validator";
    protected static final String METADATA_ELEMENT = "metadata";
    protected static final String EXTRA_FILES_ELEMENT = "extra_files";

    protected static final String ID_ATTR = "id";
    protected static final String NAME_ATTR = "name";
    protected static final String TOOL_TYPE = "tool_type";
    protected static final String VALUE_ATTR = "value";
    protected static final String FILE_ATTR = "file";
    protected static final String INTERPRETER_ATTR = "interpreter";
    protected static final String TYPE_ATTR = "type";
    protected static final String DBKEY_ATTR = "dbkey";
    protected static final String COMPARE_ATTR = "compare";
    protected static final String DIFF_ATTR = "lines_diff";
    protected static final String DELTA_ATTR = "delta";
    protected static final String HELP_ATTR = "help";
    protected static final String LABEL_ATTR = "label";
    protected static final String TITLE_ATTR = "title";
    protected static final String MULTIPLE_ATTR = "multiple";
    protected static final String DATA_REF_ATTR = "data_ref";
    protected static final String FORMAT_ATTR = "format";
    protected static final String FILE_TYPE_ATTR = "ftype";
    protected static final String SORT_ATTR = "sort";
    protected static final String GALAXY_NAME_ATTR = "galaxy_name";
    protected static final String REMOTE_NAME_ATTR = "remote_name";
    protected static final String DEFAULT_VALUE_ATTR = "missing";
    protected static final String GALAXY_VALUE_ATTR = "galaxy_value";
    protected static final String REMOTE_VALUE_ATTR = "remote_value";
    protected static final String ACTION_ATTR = "action";
    protected static final String APPEND_SEPARATOR_ATTR = "separator";
    protected static final String APPEND_FIRST_SEPARATOR_ATTR = "first_separator";
    protected static final String APPEND_JOIN_ATTR = "join";
    protected static final String INDEX_ATTR = "index";
    protected static final String FROM_FILE_ATTR = "from_file";
    protected static final String STARTS_WITH_ATTR = "startswith";
    protected static final String METADATA_SOURCE_ATTR = "metadata_source";
    protected static final String NUMERICAL_ATTR = "numerical";
    protected static final String FROM_WORK_DIR_ATTR = "from_work_dir";
    protected static final String FILTER_EXPRESSIONS_ATTR = "filter_expressions";
    protected static final String ACTIONS_ATTR = "actions";
    private static final String REQUIREMENTS_ELEMENT = "requirements";
    private static final String REQUIREMENT_ELEMENT = "requirement";

    protected static final Logger log = Logger.getLogger(MethodInfoParser.class.getName());
    /**
     * Parse galaxy tool XML info
     * @param tool
     * @return
     */
    
    public static GalaxyMethodInfo parseTool(Element tool, DataCollection parent, File path, String id)
    {
        Preprocessor preprocessor = new Preprocessor(path);
        preprocessor.run(tool);

        String name = tool.getAttribute(ID_ATTR);
        String title = tool.getAttribute(NAME_ATTR);
        String toolType = tool.getAttribute(TOOL_TYPE);
        String shortDescription = null;
        Element descriptionElement = getChildElement(tool, DESCRIPTION_ELEMENT);
        if( descriptionElement != null )
        {
            shortDescription = getTextContent(descriptionElement);
        }
        String description = shortDescription;
        Element helpElement = getChildElement(tool, HELP_ELEMENT);
        if( helpElement != null )
        {
            description = getTextContent(helpElement);
        }
        GalaxyMethodInfo info;
        if( toolType != null && toolType.equals("data_source") )
        {
            DataSourceMethodInfo dsInfo = new DataSourceMethodInfo(name, title, description, parent);
            Element paramTranslationsElement = getChildElement(tool, PARAM_TRANSLATIONS_ELEMENT);
            if( paramTranslationsElement != null )
                parseTranslationRules(paramTranslationsElement, dsInfo);
            Element inputsElement = getChildElement(tool, INPUTS_ELEMENT);
            if( inputsElement != null )
            {
                if( inputsElement.hasAttribute(ACTION_ATTR) )
                    dsInfo.setAction(inputsElement.getAttribute(ACTION_ATTR));
                Element displayElement = getChildElement(inputsElement, DISPLAY_ELEMENT);
                if( displayElement != null )
                    dsInfo.setUrlTitle(getTextContent(displayElement).replace("$GALAXY_URL", ""));
            }
            info = dsInfo;
        }
        else
            info = new GalaxyMethodInfo(name, title, description, parent);
        
        info.setId(id);
        info.setShortDescription( shortDescription );
        
        Element commandElement = getChildElement(tool, COMMAND_ELEMENT);
        if( commandElement == null )
            return null;
        String commandText = getTextContent(commandElement);
        String interpreter = commandElement.getAttribute(INTERPRETER_ATTR);
        info.setCommand(new Command(interpreter, commandText, path, info));
        
        Element requirementsElement = getChildElement(tool, REQUIREMENTS_ELEMENT);
        if( requirementsElement != null )
            for(Element element : XmlUtil.elements( requirementsElement, REQUIREMENT_ELEMENT ))
                try
                {
                    info.addRequirement( new Requirement( element ) );
                }
                catch( IllegalArgumentException e )
                {
                    log.warning( "Galaxy xml file " + path + ": " + e.getMessage() );
                }

        Element inputsElement = getChildElement(tool, INPUTS_ELEMENT);
        if( inputsElement == null )
            throw new RuntimeException("Element \"" + INPUTS_ELEMENT + "\" not found");
        Map<String, Parameter> inputParams = parseParameterTag(info, inputsElement);
        if(inputParams.isEmpty() && info instanceof DataSourceMethodInfo)
        {
            inputParams.put("GALAXY_URL", new HiddenParameter(false, "$baseurl$/galaxy?"+DataSourceURLBuilder.PARAMS_TEMPLATE));
        }
        EntryStream.of( inputParams ).forKeyValue( info.getParameters()::put );
        
        Element configfilesElement = getChildElement(tool, CONFIGFILES_ELEMENT);
        if( configfilesElement != null )
        {
            for(Element node : XmlUtil.elements(configfilesElement, CONFIGFILE_ELEMENT) )
            {
                String pName = node.getAttribute(NAME_ATTR);
                Parameter p = parseConfigfileParameter(info, node);
                info.getParameters().put(pName, p);
            }
        }

        Element outputsElement = getChildElement(tool, OUTPUTS_ELEMENT);
        if( outputsElement != null )
        {
            for( Element node : XmlUtil.elements(outputsElement, DATA_ELEMENT) )
            {
                String pName = node.getAttribute(NAME_ATTR);
                Parameter p = parseOutputParameter(info, node);
                info.getParameters().put(pName, p);
            }
        }

        Element testsElement = getChildElement(tool, TESTS_ELEMENT);
        if( testsElement != null )
        {
            for( Element node : XmlUtil.elements(testsElement, TEST_ELEMENT) )
                info.addTest(node);
        }

        return info;
    }

    protected static void parseTranslationRules(Element parentElement, DataSourceMethodInfo info)
    {
        for( Element node : XmlUtil.elements(parentElement, PARAM_TRANSLATION_ELEMENT) )
        {
            TranslationRule rule = parseTranslationRule(node);
            if(rule != null) info.addTranslationRule(rule);
        }
    }

    protected static TranslationRule parseTranslationRule(Element element)
    {
        String localName = element.getAttribute(GALAXY_NAME_ATTR);
        String remoteName = element.getAttribute(REMOTE_NAME_ATTR);
        String defaultValue = element.getAttribute(DEFAULT_VALUE_ATTR);
        if(localName.isEmpty() || remoteName.isEmpty()) return null;
        TranslationRule rule = new TranslationRule(remoteName, localName, defaultValue);
        Element valueTranslationElement = getChildElement(element, VALUE_TRANSLATION_ELEMENT);
        if(valueTranslationElement != null)
        {
            for( Element valueElement : XmlUtil.elements(valueTranslationElement, VALUE_ELEMENT) )
            {
                String from = valueElement.getAttribute(REMOTE_VALUE_ATTR);
                String to = valueElement.getAttribute(GALAXY_VALUE_ATTR);
                if(!from.isEmpty() && !to.isEmpty())
                    rule.addTranslation(from, to);
            }
        }
        Element appendParamElement = getChildElement(element, APPEND_PARAMETERS_ELEMENT);
        if(appendParamElement != null)
        {
            rule.setAppendOptions(appendParamElement.getAttribute(APPEND_SEPARATOR_ATTR), appendParamElement.getAttribute(APPEND_FIRST_SEPARATOR_ATTR), appendParamElement.getAttribute(APPEND_JOIN_ATTR));
            for( Element valueElement : XmlUtil.elements(appendParamElement, VALUE_ELEMENT) )
            {
                String name = valueElement.getAttribute(NAME_ATTR);
                String value = valueElement.getAttribute(DEFAULT_VALUE_ATTR);
                rule.addAppendParameter(name, value);
            }
        }
        return rule;
    }

    protected static Map<String, Parameter> parseParameterTag(GalaxyMethodInfo info, Element parentElement)
    {
        Map<String, Parameter> result = new LinkedHashMap<>();
        for( Element element : XmlUtil.elements(parentElement) )
        {
            if( element.getNodeName().equals(PAGE_ELEMENT) )
            {
                result.putAll(parseParameterTag(info, element));
                continue;
            }
            String pName = element.getAttribute(NAME_ATTR);
            Parameter param = null;
            if( element.getNodeName().equals(PARAM_ELEMENT) )
                param = parseInputParameter(info, element);
            else if( element.getNodeName().equals(REPEAT_ELEMENT) )
                param = parseInputRepeat(info, element);
            else if( element.getNodeName().equals(CONDITIONAL_ELEMENT) )
                param = parseInputConditional(info, element);
            
            if( param != null )
               result.put(pName, param);
        }
        return result;
    }

    // TODO: support dynamic options in tests
    protected static GalaxyMethodTest parseTest(Element test, GalaxyMethodInfo methodInfo)
    {
        GalaxyMethodTest testInfo = new GalaxyMethodTest();
        
        testInfo.setParameters(methodInfo.getParameters().clone());
        
        for( Element param : XmlUtil.elements(test, PARAM_ELEMENT) )
        {
            String name = param.getAttribute(NAME_ATTR);
            String value = param.hasAttribute(VALUE_ATTR)?param.getAttribute(VALUE_ATTR):null;
            
            Parameter p = testInfo.getParameters().getParameter(name);
            if(p == null)
            {
                log.warning("Can not find parameter '" + name + "' for " +methodInfo.getName() + ", ignoring");
                continue;
            }
            
            p.setValueFromTest(value);
            
            if( param.hasAttribute(DBKEY_ATTR) )
            {
                String dbKey = param.getAttribute(DBKEY_ATTR);
                testInfo.getAttributes().put(DBKEY_ATTR, dbKey);
                p.getMetadata().put(DBKEY_ATTR, new MetaParameter(DBKEY_ATTR, dbKey, null, null));
            }
            
            if( param.hasAttribute(FILE_TYPE_ATTR) && (p instanceof FileParameter))
                ((FileParameter)p).setExtension(param.getAttribute(FILE_TYPE_ATTR));
            
            for(Element meta : XmlUtil.elements(param, METADATA_ELEMENT))
            {
                String metadataName = meta.getAttribute(NAME_ATTR);
                String metadataValue = meta.getAttribute(VALUE_ATTR);
                p.getMetadata().put(metadataName, new MetaParameter(metadataName, metadataValue, null, null));
            }
        }
        
        for( Element element : XmlUtil.elements(test, OUTPUT_ELEMENT) )
            parseOutputParameterFromTest(element, testInfo);
        
        return testInfo;
    }
    
    private static FileParameter parseOutputParameterFromTest(Element element, GalaxyMethodTest testInfo)
    {
        String name = element.getAttribute(NAME_ATTR);
        String file = element.getAttribute(FILE_ATTR);

        ResultComparator comparator = getComparator(element);
        
        FileParameter param = (FileParameter)testInfo.getParameters().get(name);
        if(param == null)
        {
            //look for first output parameter
            for( Map.Entry<String, Parameter> e : testInfo.getParameters().entrySet() )
                if( e.getValue().isOutput() )
                {
                    param = (FileParameter)e.getValue();
                    name = e.getKey();
                    break;
                }
        }
        if(param == null)
        {
            param = new FileParameter(true);
            param.setContainer(testInfo.getParameters());
            testInfo.getParameters().put(name, param);
        }
        testInfo.setComparator(comparator, name);
        param.setValueFromTest(file);
        
        for(Element extraElement : XmlUtil.elements(element, EXTRA_FILES_ELEMENT))
        {
            FileParameter extraParam = parseOutputParameterFromTest(extraElement, testInfo);
            if(param.getAttributes().get(EXTRA_FILES_ELEMENT) == null)
                param.getAttributes().put(EXTRA_FILES_ELEMENT, new ArrayList<FileParameter>());
            List<FileParameter> extraParameters = (List<FileParameter>)param.getAttributes().get(EXTRA_FILES_ELEMENT);
            extraParameters.add(extraParam);
        }
        
        return param;
    }
    
    private static ResultComparator getComparator(Element param)
    {
        String compare = param.getAttribute(COMPARE_ATTR);
        if( compare == null || compare.length() == 0 )
            compare = "diff";
        int difference = 0;
        try { difference = Integer.parseInt( param.getAttribute(DIFF_ATTR)); } catch( NumberFormatException e ){}
        try { difference = Integer.parseInt( param .getAttribute(DELTA_ATTR)); } catch( NumberFormatException e ){}
        boolean sort = Boolean.valueOf(param.getAttribute(SORT_ATTR));
        return new ResultComparator(compare, difference, sort);
    }

    protected static Parameter parseInputParameter(GalaxyMethodInfo info, Element element)
    {
        Parameter result;
        String type = element.getAttribute(TYPE_ATTR);
        if( type.equals("data") )
        {
            result = new FileParameter(false);
            fillFormatAttributes( element, result );
            if(element.hasAttribute("needs_metadata"))
              result.getAttributes().put("needs_metadata", Boolean.valueOf(element.getAttribute("needs_meatadata")));
        }
        else if( type.equals("data-multi") )
        {
            result = new MultiFileParameter(false);
            fillFormatAttributes( element, result );
            if(element.hasAttribute("needs_metadata"))
                result.getAttributes().put("needs_metadata", Boolean.valueOf(element.getAttribute("needs_meatadata")));
        }
        else if( type.equals("boolean"))
        {
            result = new BooleanParameter(false);
            if(element.hasAttribute("truevalue"))
                ((BooleanParameter)result).setTrueValue(element.getAttribute("truevalue"));
            if(element.hasAttribute("falsevalue"))
                ((BooleanParameter)result).setFalseValue(element.getAttribute("falsevalue"));
            
            for(String attr : new String[] {"checked", "selected"})
                if(element.hasAttribute(attr))
                {
                    String defaultValue = element.getAttribute(attr);
                    boolean checked = defaultValue.isEmpty() || Boolean.valueOf(defaultValue);
                    result.getAttributes().put("value", String.valueOf(checked) );
                    result.setValue(String.valueOf(checked));
                    break;
                }
        } else if( type.equals("select") || type.equals("drill_down") )
        {
            result = new SelectParameter(false);
        } else if( type.equals("data_column"))
        {
            result = new DataColumnParameter(false);
            String numerical = element.getAttribute(NUMERICAL_ATTR);
            if( Boolean.valueOf(numerical) )
            {
                result.getAttributes().put("numerical", true);
                ((DataColumnParameter)result).setNumerical(true);
            }
        } else if( type.equals("hidden") || type.equals("baseurl"))
        {
            result = new HiddenParameter(false);
        } else if( type.equals("float"))
        {
            result = new FloatParameter();
        }
        else
        {
            result = new StringParameter(false);
        }
        result.getAttributes().put("type", type);
        
        for( String attr : new String[] {"default", "value"} )
            if( element.hasAttribute(attr) )
            {
                String value = element.getAttribute(attr);
                result.getAttributes().put("value", value);
                result.setValue(value);
            }
        
        fillLabelAndHelp(result, element);
        result.getAttributes().put("optional", Boolean.valueOf(element.getAttribute("optional")));
        
        if(type.equals("baseurl"))
        {
            result.setValue("$baseurl$/galaxy?"+DataSourceURLBuilder.PARAMS_TEMPLATE);
        }
        
        for(Element validatorElement : XmlUtil.elements(element, VALIDATOR_ELEMENT))
        {
            Validator validator = ValidatorFactory.createValidator(validatorElement, result);
            if(validator != null)
                result.addValidator(validator);
        }
        
        if( result instanceof SelectParameter )
        {
            String multiple = element.getAttribute(MULTIPLE_ATTR);
            if( Boolean.valueOf(multiple) )
            {
                result.getAttributes().put("multiple", true);
                ((SelectParameter)result).setMultiple(true);
            }
        }
        if( type.equals("select") )
        {
            SelectParameter selectParameter = (SelectParameter)result;
            Element optionsElement = getChildElement(element, OPTIONS_ELEMENT);
            if(optionsElement != null)
            {
                for( Element columnElement : XmlUtil.elements(optionsElement, COLUMN_ELEMENT) )
                    selectParameter.addColumnIndex(columnElement.getAttribute(NAME_ATTR), Integer.parseInt(columnElement
                            .getAttribute(INDEX_ATTR)));
                
                Filter sourceFilter = FilterFactory.createFilter(optionsElement, selectParameter);
                if(sourceFilter != null)
                    selectParameter.addFilter(sourceFilter);
                for(Element filterElement : XmlUtil.elements(optionsElement, FILTER_ELEMENT))
                {
                    Filter filter = FilterFactory.createFilter(filterElement, selectParameter);
                    if(filter != null)
                        selectParameter.addFilter(filter);
                }
            } else
            {
                SourceStaticFilter filter = new SourceStaticFilter();
                filter.init(element, selectParameter);
                List<String> selection = new ArrayList<>();
                for( Element node : XmlUtil.elements(element, OPTION_ELEMENT) )
                {
                    String val = node.getAttribute(VALUE_ATTR);
                    String title = getTextContent(node);
                    if( val != null && title != null && title.length() > 0 )
                    {
                        filter.addOption(title, val);
                    }
                    if(node.hasAttribute("selected") && Boolean.valueOf(node.getAttribute("selected")))
                    {
                        selection.add(val);
                    }
                }
                selectParameter.addFilter(filter);
                if(!selection.isEmpty())
                    result.setValue(String.join(",", selection));
            }
        }
        if( type.equals("drill_down") )
        {
            SelectParameter selectParameter = (SelectParameter)result;
            SourceStaticFilter filter = new SourceStaticFilter();
            filter.init(element, selectParameter);
            String fromFile = element.getAttribute(FROM_FILE_ATTR);
            Element optionsElement = null;
            if( !fromFile.isEmpty() )
            {
                try
                {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(new File(GalaxyDataCollection.getGalaxyDistFiles().getToolDataFolder(), fromFile));
                    optionsElement = getChildElement(doc.getDocumentElement(), OPTIONS_ELEMENT);
                }
                catch( Exception e )
                {
                }
            } else
            {
                optionsElement = getChildElement(element, OPTIONS_ELEMENT);
            }
            if(optionsElement != null)
            {
                parseDrillDownOptions(filter, optionsElement, 0);
            }
            selectParameter.addFilter(filter);
        }
        if( type.equals("data_column"))
        {
            String dataRef = element.getAttribute(DATA_REF_ATTR);
            result.getAttributes().put("dataRef", dataRef);
        }
        return result;
    }

    protected static void parseDrillDownOptions(SourceStaticFilter filter, Element optionsElement, int depth)
    {
        String prefix = TextUtil2.times( '-', depth )+" ";
        for( Element element : XmlUtil.elements(optionsElement, OPTION_ELEMENT) )
        {
            String title = element.getAttribute(NAME_ATTR);
            String value = element.getAttribute(VALUE_ATTR);
            if(!value.isEmpty() && !title.isEmpty())
            {
                filter.addOption(prefix+title, value);
            }
            parseDrillDownOptions(filter, element, depth+1);
        }
    }

    protected static Parameter parseInputRepeat(GalaxyMethodInfo info, Element element)
    {
        ArrayParameter result = new ArrayParameter(false);
        Map<String, Parameter> inputParams = parseParameterTag(info, element);
        result.getChildTypes().putAll(inputParams);
        result.getAttributes().put("label", element.getAttribute(TITLE_ATTR));
        return result;
    }

    protected static Parameter parseInputConditional(GalaxyMethodInfo info, Element element)
    {
        ConditionalParameter result = new ConditionalParameter(false);

        Element keyParamElement = getChildElement(element, PARAM_ELEMENT);
        result.setKeyParameterName( keyParamElement.getAttribute(NAME_ATTR));
        result.setKeyParameter(parseInputParameter(info, keyParamElement));
        
        for( Element node : XmlUtil.elements(element, WHEN_ELEMENT) )
        {
            String key = node.getAttribute(VALUE_ATTR);
            result.addKeyValue(key);
            Map<String, Parameter> inputParams = parseParameterTag(info, node);
            for( Map.Entry<String, Parameter> entry : inputParams.entrySet() )
            {
                result.addParameter(key, entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    protected static Parameter parseConfigfileParameter(GalaxyMethodInfo info, Element element)
    {
        String content = getTextContent(element);
        ConfigParameter result = new ConfigParameter(false);
        result.setFileContent(content);
        return result;
    }

    protected static Parameter parseOutputParameter(GalaxyMethodInfo info, Element element)
    {
        Parameter result = new FileParameter(true);
        fillFormatAttributes( element, result );
        if(result.getAttributes().get( FORMAT_ATTR ).equals("input"))
        {
            String source = null;
            if(element.hasAttribute(METADATA_SOURCE_ATTR))
                source = element.getAttribute(METADATA_SOURCE_ATTR);
            else
            {
                //Find the first parameter with 'format' attribute set
                source = StreamEx.ofKeys( info.getParameters(), param -> param.getAttributes().containsKey( FORMAT_ATTR ) )
                        .findFirst().orElse( null );
            }
            if(source != null)
            {
                Parameter parameter = info.getParameter(source);
                result.getAttributes().put("source", source);
                if( parameter != null && parameter.getAttributes().get(FORMAT_ATTR) != null )
                {
                    String format = parameter.getAttributes().get(FORMAT_ATTR).toString();
                    result.getAttributes().put( FORMAT_ATTR, format );
                }
            }
        }
        
        if(element.hasAttribute(FROM_WORK_DIR_ATTR))
            result.getAttributes().put(FROM_WORK_DIR_ATTR, element.getAttribute(FROM_WORK_DIR_ATTR));
        
        List<String> filterExpressions = new ArrayList<>();
        for(Element e : XmlUtil.elements(element, FILTER_ELEMENT))
            filterExpressions.add(getTextContent(e).replace('\n', ' ').replace('\r', ' '));
        result.getAttributes().put(FILTER_EXPRESSIONS_ATTR, filterExpressions);
        
        Element actionsElement = XmlUtil.getChildElement( element, "actions" );
        if(actionsElement != null)
            result.getAttributes().put( ACTIONS_ATTR, new Actions( actionsElement ) );
        
        fillLabelAndHelp(result, element);
        
        if(element.hasAttribute( OptionEx.TEMPLATE_PROPERTY ))
            result.getAttributes().put( OptionEx.TEMPLATE_PROPERTY, element.getAttribute( OptionEx.TEMPLATE_PROPERTY ) );
        
        return result;
    }

    private static void fillFormatAttributes(Element element, Parameter result)
    {
        String format = element.getAttribute(FORMAT_ATTR);
        result.getAttributes().put(FORMAT_ATTR, format);
        
        XmlStream.attributes( element ).filterKeys( name -> name.startsWith( FORMAT_ATTR+"." ) )
            .forKeyValue( result.getAttributes()::put );
    }
    
    protected static void fillLabelAndHelp(Parameter p, Element e)
    {
        p.getAttributes().put("help", getAttributeOrText(e, HELP_ATTR));
        p.getAttributes().put("label", getAttributeOrText(e, LABEL_ATTR));
    }
}