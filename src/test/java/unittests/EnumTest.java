package unittests;

import model.EnumPojo;
import org.junit.Test;
import xmlparser.XmlParser;

import static model.SimpleEnum.one;
import static model.SimpleEnum.value;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class EnumTest {

    private final XmlParser parser = new XmlParser();

    @Test
    public void deserializeEnumHasValue() {
        final String pojoXml = "<enumpojo><test>one</test></enumpojo>\n";

        final EnumPojo pojo = parser.fromXml(pojoXml, EnumPojo.class);

        assertNotNull("Pojo is null", pojo);
        assertEquals("Pojo has the wrong name", one, pojo.test);
    }

    @Test
    public void deserializeEnumEmpty() {
        final String pojoXml = "<enumpojo><test></test></enumpojo>\n";

        final EnumPojo pojo = parser.fromXml(pojoXml, EnumPojo.class);

        assertNotNull("Pojo is null", pojo);
        assertNull("Pojo has the wrong name", pojo.test);
    }

    @Test
    public void deserializeEnumMissing() {
        final String pojoXml = "<enumpojo></enumpojo>\n";

        final EnumPojo pojo = parser.fromXml(pojoXml, EnumPojo.class);

        assertNotNull("Pojo is null", pojo);
        assertNull("Pojo has the wrong name", pojo.test);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deserializeNoEnumOption() {
        final String pojoXml = "<enumpojo><test>missing</test></enumpojo>\n";

        parser.fromXml(pojoXml, EnumPojo.class);

        fail("Missing enum value didn't throw exception");
    }

    @Test
    public void serializeWithValueAnnotation() {
        final EnumPojo pojo = new EnumPojo(value);
        final String pojoXml = "<enumpojo>\n  <test>123</test>\n</enumpojo>\n";

        final String pojoOut = parser.toXml(pojo);

        assertNotNull("Pojo is null", pojoOut);
        assertEquals("Invalid POJO XML", pojoXml, pojoOut);
    }

    @Test
    public void deserializeWithValueAnnotation() {
        final String pojoXml = "<enumpojo><test>123</test></enumpojo>\n";

        final EnumPojo pojo = parser.fromXml(pojoXml, EnumPojo.class);

        assertNotNull("Pojo is null", pojo);
        assertEquals("Pojo has the wrong name", value, pojo.test);
    }

}
