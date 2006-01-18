package quickfix;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

public class DataDictionaryTest extends TestCase {

    public DataDictionaryTest(String arg0) {
        super(arg0);
    }

    public void testDictionary() throws Exception {
        DataDictionary dd = getDictionary();

        assertEquals("wrong field name", "Currency", dd.getFieldName(15));
        assertEquals("wrong value description", "BUY", dd.getValueName(4, "B"));
        assertEquals("wrong value type", FieldType.String, dd.getFieldTypeEnum(1));
        assertEquals("wrong version", FixVersions.BEGINSTRING_FIX44, dd.getVersion());
        assertFalse("unexpected field values existence", dd.hasFieldValue(1));
        assertTrue("unexpected field values nonexistence", dd.hasFieldValue(4));
        assertFalse("unexpected field existence", dd.isField(9999));
        assertTrue("unexpected field nonexistence", dd.isField(4));
        assertTrue("unexpected field value existence", !dd.isFieldValue(4, "C"));
        assertTrue("unexpected field value nonexistence", dd.isFieldValue(4, "B"));
        assertTrue("wrong group info", dd.isGroup("A", 384));
        assertFalse("wrong group info", dd.isGroup("A", 1));
        assertNotNull("wrong group info", dd.getGroup("6", 232));
        assertTrue("incorrect header field", dd.isHeaderField(8));
        assertFalse("incorrect header field", dd.isHeaderField(1));
        assertTrue("incorrect trailer field", dd.isTrailerField(89));
        assertFalse("incorrect trailer field", dd.isTrailerField(1));
        assertTrue("incorrect message field", dd.isMsgField("A", 98));
        assertFalse("incorrect message field", dd.isMsgField("A", 1));
        // component field
        assertTrue("incorrect message field", dd.isMsgField("6", 235)); 
        // group->component field
        //assertTrue("incorrect message field", dd.isMsgField("6", 311));
        assertTrue("incorrect message type", dd.isMsgType("A"));
        assertFalse("incorrect message type", dd.isMsgType("%"));
        assertTrue("incorrect field requirement", dd.isRequiredField("A", 98));
        assertFalse("incorrect field requirement", dd.isRequiredField("A", 95));
        assertEquals("incorrect field name", "Account", dd.getFieldName(1));
    }

    public void testMissingFieldAttributeForRequired() throws Exception {
    	String data = "";
    	data += "<fix major=\"4\" minor=\"0\">";
    	data += "  <header>";  
    	data += "    <field name=\"BeginString\" required=\"Y\"/>";
    	data += "  </header>";
    	data += "  <trailer>";  
    	data += "    <field name=\"CheckSum\" required=\"Y\"/>";
    	data += "  </trailer>";
    	data += "  <fields>";
    	data += "    <field number=\"8\" name=\"BeginString\" type=\"STRING\"/>";
    	data += "    <field number=\"10\" name=\"CheckSum\" type=\"STRING\"/>";
    	data += "    <field number=\"112\" name=\"TestReqID\" type=\"STRING\"/>";
    	data += "  </fields>";
    	data += "  <messages>";
        data += "    <message name=\"Heartbeat\" msgtype=\"0\" msgcat=\"admin\">";
        data += "      <field name=\"TestReqID\"/>";
        data += "    </message>";
        data += "  </messages>";
        data += "</fix>";

    	assertConfigErrorForMissingAttributeRequired(data);
    }

	private void assertConfigErrorForMissingAttributeRequired(String data) {
		try {
			new DataDictionary(new ByteArrayInputStream(data.getBytes()));
		} catch (ConfigError e) {
			// Expected
			assertTrue(e.getMessage().indexOf("does not have a 'required'") != -1);
		}
	}
    
    public void testMissingComponentAttributeForRequired() throws Exception {
    	String data = "";
    	data += "<fix major=\"4\" minor=\"0\">";
    	data += "  <header>";  
    	data += "    <field name=\"BeginString\" required=\"Y\"/>";
    	data += "  </header>";
    	data += "  <trailer>";  
    	data += "    <field name=\"CheckSum\" required=\"Y\"/>";
    	data += "  </trailer>";
    	data += "  <fields>";
    	data += "    <field number=\"8\" name=\"BeginString\" type=\"STRING\"/>";
    	data += "    <field number=\"10\" name=\"CheckSum\" type=\"STRING\"/>";
    	data += "    <field number=\"112\" name=\"TestReqID\" type=\"STRING\"/>";
    	data += "  </fields>";
    	data += "  <messages>";
        data += "    <message name=\"Heartbeat\" msgtype=\"0\" msgcat=\"admin\">";
        data += "      <component name=\"Instrument\"/>";
        data += "    </message>";
        data += "  </messages>";
        data += "</fix>";

    	assertConfigErrorForMissingAttributeRequired(data);
    }

    public void testMissingGroupAttributeForRequired() throws Exception {
    	String data = "";
    	data += "<fix major=\"4\" minor=\"0\">";
    	data += "  <header>";  
    	data += "    <field name=\"BeginString\" required=\"Y\"/>";
    	data += "  </header>";
    	data += "  <trailer>";  
    	data += "    <field name=\"CheckSum\" required=\"Y\"/>";
    	data += "  </trailer>";
    	data += "  <fields>";
    	data += "    <field number=\"8\" name=\"BeginString\" type=\"STRING\"/>";
    	data += "    <field number=\"10\" name=\"CheckSum\" type=\"STRING\"/>";
    	data += "    <field number=\"112\" name=\"TestReqID\" type=\"STRING\"/>";
    	data += "  </fields>";
    	data += "  <messages>";
        data += "    <message name=\"Heartbeat\" msgtype=\"0\" msgcat=\"admin\">";
        data += "      <group name=\"TestReqID\"/>";
        data += "    </message>";
        data += "  </messages>";
        data += "</fix>";

    	assertConfigErrorForMissingAttributeRequired(data);
    }

    // QF C++ treats the string argument as a filename although it's
    // named 'url'. QFJ string argument can be either but this test
    // ensures the DD works correctly with a regular file path.
    public void testDictionaryWithFilename() throws Exception {
        DataDictionary dd = new DataDictionary("etc/FIX40.xml");
        assertEquals("wrong field name", "Currency", dd.getFieldName(15));
        // It worked!
    }
    
    private static DataDictionary testDataDictionary;

    public static DataDictionary getDictionary() throws Exception {
        if (testDataDictionary == null) {
            testDataDictionary = new DataDictionary(DataDictionaryTest.class.getClassLoader()
                    .getResourceAsStream("FIX44.xml"));
        }
        return testDataDictionary;
    }
}