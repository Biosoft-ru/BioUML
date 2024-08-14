package ru.biosoft.util._test.j2html.attributes;

import ru.biosoft.util.j2html.attributes.Attribute;
import ru.biosoft.util.j2html.tags.ContainerTag;
import junit.framework.TestCase;

public class AttributeTest extends TestCase {

    public void testRender() throws Exception {
        Attribute attributeWithValue = new Attribute("href", "http://example.com");
        assertEquals(attributeWithValue.render(), " href=\"http://example.com\"");

        Attribute attribute = new Attribute("required", null);
        assertEquals(attribute.render(), " required");

        Attribute nullAttribute = new Attribute(null, null);
        assertEquals(nullAttribute.render(), "");
    }

    public void testSetAttribute() throws Exception {
        ContainerTag testTag = new ContainerTag("a");
        testTag.attr("href", "http://example.com");
        testTag.attr("href", "http://example.org");
        assertEquals(testTag.render(), "<a href=\"http://example.org\"></a>");
    }

}
