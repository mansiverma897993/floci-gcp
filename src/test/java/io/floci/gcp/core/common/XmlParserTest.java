package io.floci.gcp.core.common;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XmlParserTest {

    @Test
    void parseFlatSimple() {
        String xml = "<Root><Name>bucket-1</Name><Location>US</Location></Root>";
        Map<String, String> result = XmlParser.parseFlat(xml);
        assertEquals("bucket-1", result.get("Name"));
        assertEquals("US", result.get("Location"));
    }

    @Test
    void parseFlatPreservesOrder() {
        String xml = "<Root><A>1</A><B>2</B><C>3</C></Root>";
        Map<String, String> result = XmlParser.parseFlat(xml);
        List<String> keys = List.copyOf(result.keySet());
        assertEquals(List.of("A", "B", "C"), keys);
    }

    @Test
    void parseFlatSkipsBlankText() {
        String xml = "<Root><Name>   </Name><Value>ok</Value></Root>";
        Map<String, String> result = XmlParser.parseFlat(xml);
        assertFalse(result.containsKey("Name"));
        assertEquals("ok", result.get("Value"));
    }

    @Test
    void parseFlatSkipsNestedElements() {
        String xml = "<Root><Outer><Inner>x</Inner></Outer><Name>top</Name></Root>";
        Map<String, String> result = XmlParser.parseFlat(xml);
        assertFalse(result.containsKey("Outer"));
        assertFalse(result.containsKey("Inner"));
        assertEquals("top", result.get("Name"));
    }

    @Test
    void parseFlatNullReturnsEmpty() {
        assertTrue(XmlParser.parseFlat(null).isEmpty());
    }

    @Test
    void parseFlatBlankReturnsEmpty() {
        assertTrue(XmlParser.parseFlat("   ").isEmpty());
    }

    @Test
    void parseFlatStripsWhitespace() {
        String xml = "<Root><Name>  hello  </Name></Root>";
        Map<String, String> result = XmlParser.parseFlat(xml);
        assertEquals("hello", result.get("Name"));
    }

    @Test
    void parseListFindsRepeatedElements() {
        String xml = "<Delete><Object><Key>a</Key></Object><Object><Key>b</Key></Object></Delete>";
        List<String> keys = XmlParser.parseList(xml, "Key");
        assertEquals(List.of("a", "b"), keys);
    }

    @Test
    void parseListReturnsEmptyWhenElementAbsent() {
        String xml = "<Root><Name>x</Name></Root>";
        assertTrue(XmlParser.parseList(xml, "Missing").isEmpty());
    }

    @Test
    void parseListNullReturnsEmpty() {
        assertTrue(XmlParser.parseList(null, "Key").isEmpty());
    }

    @Test
    void parseListBlankReturnsEmpty() {
        assertTrue(XmlParser.parseList("  ", "Key").isEmpty());
    }

    @Test
    void parseListStripsWhitespace() {
        String xml = "<Root><Key>  my-key  </Key></Root>";
        List<String> keys = XmlParser.parseList(xml, "Key");
        assertEquals("my-key", keys.get(0));
    }
}
