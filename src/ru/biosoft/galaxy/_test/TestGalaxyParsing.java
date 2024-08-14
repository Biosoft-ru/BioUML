package ru.biosoft.galaxy._test;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.model.ArrayProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import ru.biosoft.galaxy.GalaxyAnalysisParameters.SelectorOption;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.galaxy.GalaxyMethodInfo;
import ru.biosoft.galaxy.MethodInfoParser;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class TestGalaxyParsing extends TestCase
{
    private static final String SIMPLE_TOOL = "<tool id=\"echo\" name=\"Echo\">"
            + "    <description>Echo method short description </description>"
            + "    <command interpreter=\"python\">echo.py $input $test</command>"
            + "    <inputs>"
            + "        <param name=\"input\" type=\"data\" format=\"fasta\" label=\"Fasta file\"/>"
            + "        <param name=\"test\" type=\"text\" value=\"default\" label=\"Test string field\" help=\"Try to set empty\"/>"
            + "    </inputs>"
            + "</tool>";

    public void testParserBasic() throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(SIMPLE_TOOL)));
        Element tool = doc.getDocumentElement();
        GalaxyMethodInfo methodInfo = MethodInfoParser.parseTool(tool, null, null, "echo");
        assertEquals("echo", methodInfo.getName());
        assertEquals( "Echo", methodInfo.getDisplayName() );
        assertEquals("python", methodInfo.getCommand().getInterpreter());
        assertEquals("echo.py $input $test", methodInfo.getCommand().getCommand());
        
        AnalysisMethod method = methodInfo.createAnalysisMethod();
        assertNotNull(method);
        assertEquals( "Echo method short description", method.getDescription().trim() );
        AnalysisParameters parameters = method.getParameters();
        assertNotNull(parameters);
        
        ComponentModel model = ComponentFactory.getModel(parameters);
        assertEquals(2, model.getPropertyCount());
        Property property = model.getPropertyAt(0);
        assertEquals("input", property.getName());
        assertEquals("Fasta file", property.getDisplayName());
        assertEquals(DataElementPathEditor.class, property.getPropertyEditorClass());
        property = model.getPropertyAt(1);
        assertEquals("test", property.getName());
        assertEquals("Try to set empty", property.getShortDescription());
        assertEquals("default", property.getValue());
    }
    
    private static final String OPTIONAL_PARAMETER = "<tool id=\"echo\" name=\"Echo\">"
            + "    <description></description>"
            + "    <command interpreter=\"python\">echo.py $input $test</command>"
            + "    <inputs>"
            + "        <param name=\"optional\" type=\"data\" format=\"fasta\" optional=\"true\"/>"
            + "        <param name=\"required\" type=\"data\" format=\"fasta\" optional=\"false\"/>"
            + "    </inputs>"
            + "</tool>";
    
    public void testOptionalProperty() throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Element tool = builder.parse(new InputSource(new StringReader(OPTIONAL_PARAMETER))).getDocumentElement();
        GalaxyMethodInfo methodInfo = MethodInfoParser.parseTool(tool, null, null, "echo");
        ComponentModel model = ComponentFactory.getModel(methodInfo.createAnalysisMethod().getParameters());
        Property property = model.getPropertyAt(0);
        assertTrue(property.getBooleanAttribute(BeanInfoConstants.CAN_BE_NULL));
        property = model.getPropertyAt(1);
        assertFalse(property.getBooleanAttribute(BeanInfoConstants.CAN_BE_NULL));
    }
    
    private static final String OUTPUT_TEST = "<tool id=\"echo\" name=\"Echo\">"
            + "    <description></description>"
            + "    <command interpreter=\"python\">echo.py $input $test</command>"
            + "    <inputs>"
            + "        <param name=\"input\" type=\"data\" format=\"fasta\"/>"
            + "    </inputs>"
            + "    <outputs>"
            + "        <data format=\"tar\" name=\"htmlOut\" label=\"GenomeQC.html\"/>"
            + "    </outputs>"
            + "</tool>";

    public void testOutputParameter() throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Element tool = builder.parse(new InputSource(new StringReader(OUTPUT_TEST))).getDocumentElement();
        GalaxyMethodInfo methodInfo = MethodInfoParser.parseTool(tool, null, null, "echo");
        ComponentModel model = ComponentFactory.getModel(methodInfo.createAnalysisMethod().getParameters());
        assertEquals(2, model.getPropertyCount());
        Property property = model.getPropertyAt(1);
        assertEquals("htmlOut", property.getName());
        assertEquals(DataElementPathEditor.class, property.getPropertyEditorClass());
    }
    
    private static final String PARAMETER_TYPES = "<tool id=\"echo\" name=\"Echo\">"
            + "    <description></description>"
            + "    <command interpreter=\"python\">echo.py $input $test</command>"
            + "    <inputs>"
            + "        <param name=\"input_data\" type=\"data\" format=\"fasta\"/>"
            + "        <param name=\"input_select\" type=\"select\">"
            + "            <option value=\"test1\">Test 1</option>"
            + "            <option value=\"test2\" selected=\"True\">Test 2</option>"
            + "            <option value=\"test3\">Test 3</option>"
            + "        </param>"
            + "        <param name=\"input_select_multi\" type=\"select\" multiple=\"true\">"
            + "            <option value=\"test1\">Test 1</option>"
            + "            <option value=\"test2\" selected=\"True\">Test 2</option>"
            + "            <option value=\"test3\" selected=\"True\">Test 3</option>"
            + "        </param>"
            + "        <param name=\"input_boolean_checked\" type=\"boolean\" checked=\"True\" truevalue=\"true\" falsevalue=\"false\"/>"
            + "        <param name=\"input_boolean\" type=\"boolean\" truevalue=\"true\" falsevalue=\"false\"/>"
            + "        <param name=\"input_data_multi\" type=\"data-multi\" format=\"fasta\"/>"
            + "        <param name=\"input_hidden\" type=\"hidden\" value=\"hiddenvalue\"/>"
            + "        <param name=\"input_text\" type=\"text\" value=\"test text\"/>"
            + "        <param name=\"input_integer\" type=\"integer\" value=\"5\"/>"
            + "        <param name=\"input_float\" type=\"float\" value=\"6.0\"/>"
            + "        <repeat name=\"repeat_block\">"
            + "            <param name=\"input_repeat_int\" type=\"integer\" value=\"5\"/>"
            + "            <param name=\"input_repeat_float\" type=\"float\" value=\"6.0\"/>"
            + "        </repeat>"
            + "        <conditional name=\"input_condition\">"
            + "            <param name=\"condition\" type=\"select\">"
            + "                <option value=\"text\">Text</option>"
            + "                <option value=\"int\" selected=\"True\">int</option>"
            + "                <option value=\"float\">float</option>"
            + "            </param>"
            + "            <when value=\"text\">"
            + "                <param name=\"cond\" type=\"text\" value=\"cond text\"/>"
            + "            </when>"
            + "            <when value=\"int\">"
            + "                <param name=\"cond\" type=\"integer\" value=\"8\"/>"
            + "            </when>"
            + "            <when value=\"float\">"
            + "                <param name=\"cond\" type=\"float\" value=\"7.0\"/>"
            + "            </when>"
            + "        </conditional>"
            + "    </inputs>"
            + "</tool>";

    public void testParameterTypes() throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Element tool = builder.parse(new InputSource(new StringReader(PARAMETER_TYPES))).getDocumentElement();
        GalaxyMethodInfo methodInfo = MethodInfoParser.parseTool(tool, null, null, "echo");
        AnalysisParameters parameters = methodInfo.createAnalysisMethod().getParameters();
        ComponentModel model = ComponentFactory.getModel(parameters);
        Property property = model.getPropertyAt(0);
        assertEquals("input_data", property.getName());
        assertEquals(DataElementPath.class, property.getValueClass());
        assertEquals(DataElementPathEditor.class, property.getPropertyEditorClass());
        assertTrue(property.getBooleanAttribute(DataElementPathEditor.ELEMENT_MUST_EXIST));
        assertFalse(property.getBooleanAttribute(DataElementPathEditor.PROMPT_OVERWRITE));
        
        property = model.getPropertyAt(1);
        assertEquals("input_select", property.getName());
        assertEquals("Test 2", property.getValue().toString());
        assertTrue(GenericComboBoxEditor.class.isAssignableFrom(property.getPropertyEditorClass()));
        GenericComboBoxEditor editor = (GenericComboBoxEditor)property.getPropertyEditorClass().newInstance();
        editor.setBean(model);
        editor.setDescriptor(property.getDescriptor());
        editor.setValue(property.getValue());
        String[] tags = editor.getTags();
        assertEquals(3, tags.length);
        assertEquals("Test 1", tags[0]);
        editor.setAsText( "Test 3" );
        property.setValue( editor.getValue() );
        assertEquals("Test 3", property.getValue().toString());
        
        property = model.getPropertyAt(2);
        assertEquals("input_select_multi", property.getName());
        assertEquals("Test 2", ((Object[])property.getValue())[0].toString());
        assertEquals("Test 3", ((Object[])property.getValue())[1].toString());
        assertTrue(GenericMultiSelectEditor.class.isAssignableFrom(property.getPropertyEditorClass()));
        
        property = model.getPropertyAt(3);
        assertEquals("input_boolean_checked", property.getName());
        assertEquals(Boolean.TRUE, property.getValue());
        
        property = model.getPropertyAt(4);
        assertEquals("input_boolean", property.getName());
        assertEquals(Boolean.FALSE, property.getValue());

        property = model.getPropertyAt(5);
        assertEquals("input_data_multi", property.getName());
        assertEquals(DataElementPathSet.class, property.getValueClass());
        assertEquals(DataElementPathEditor.class, property.getPropertyEditorClass());

        property = model.getPropertyAt(6);
        assertEquals("input_hidden", property.getName());
        assertTrue(property.getDescriptor().isHidden());
        assertEquals("hiddenvalue", property.getValue());

        property = model.getPropertyAt(7);
        assertEquals("input_text", property.getName());
        assertFalse(property.getDescriptor().isHidden());
        assertEquals("test text", property.getValue());

        property = model.getPropertyAt(8);
        assertEquals("input_integer", property.getName());
        assertEquals(5, property.getValue());

        property = model.getPropertyAt(9);
        assertEquals("input_float", property.getName());
        assertEquals(6.0f, property.getValue());
        
        property = model.getPropertyAt(10);
        assertTrue(property instanceof ArrayProperty);
        assertEquals(0, property.getPropertyCount());
        ( (ArrayProperty)property ).insertItem( property.getPropertyCount(), null);
        assertEquals(1, property.getPropertyCount());
        Property itemProperty = property.getPropertyAt(0);
        assertEquals(2, itemProperty.getPropertyCount());
        Property subProperty = itemProperty.getPropertyAt(0);
        assertEquals("input_repeat_int", subProperty.getName());
        assertEquals(5, subProperty.getValue());
        subProperty = itemProperty.getPropertyAt(1);
        assertEquals("input_repeat_float", subProperty.getName());
        assertEquals(6.0f, subProperty.getValue());
        
        property = model.getPropertyAt(11);
        Property textProperty = model.getPropertyAt(12);
        Property intProperty = model.getPropertyAt(13);
        Property floatProperty = model.getPropertyAt(14);

        assertEquals("input_condition|condition", property.getName());
        assertEquals("input_condition|text|cond", textProperty.getName());
        assertEquals("input_condition|int|cond", intProperty.getName());
        assertEquals("input_condition|float|cond", floatProperty.getName());

        assertFalse(textProperty.isVisible(Property.SHOW_EXPERT));
        assertTrue(intProperty.isVisible(Property.SHOW_EXPERT));
        assertFalse(floatProperty.isVisible(Property.SHOW_EXPERT));

        property.setValue(new SelectorOption("float", "float"));
        model = ComponentFactory.getModel(parameters, Policy.DEFAULT, true);
        textProperty = model.getPropertyAt(12);
        intProperty = model.getPropertyAt(13);
        floatProperty = model.getPropertyAt(14);
        assertFalse(textProperty.isVisible(Property.SHOW_EXPERT));
        assertFalse(intProperty.isVisible(Property.SHOW_EXPERT));
        assertTrue(floatProperty.isVisible(Property.SHOW_EXPERT));
        
        property.setValue(new SelectorOption("text", "text"));
        model = ComponentFactory.getModel(parameters, Policy.DEFAULT, true);
        textProperty = model.getPropertyAt(12);
        intProperty = model.getPropertyAt(13);
        floatProperty = model.getPropertyAt(14);
        assertTrue(textProperty.isVisible(Property.SHOW_EXPERT));
        assertFalse(intProperty.isVisible(Property.SHOW_EXPERT));
        assertFalse(floatProperty.isVisible(Property.SHOW_EXPERT));
    }
}
