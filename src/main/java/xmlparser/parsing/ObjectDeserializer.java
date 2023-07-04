package xmlparser.parsing;

import xmlparser.model.XmlElement;

import java.util.HashMap;
import java.util.Map;

import static xmlparser.utils.Reflection.toObjectClass;

public interface ObjectDeserializer {
    static Map<Class<?>, ObjectDeserializer> defaultDeserializers() {
        final Map<Class<?>, ObjectDeserializer> deserializers = new HashMap<>();
        deserializers.put(Integer.class, Integer::valueOf);
        deserializers.put(int.class, Integer::valueOf);
        deserializers.put(Double.class, Double::valueOf);
        deserializers.put(double.class, Double::valueOf);
        deserializers.put(Float.class, Float::valueOf);
        deserializers.put(float.class, Float::valueOf);
        deserializers.put(Byte.class, Byte::valueOf);
        deserializers.put(byte.class, Byte::valueOf);
        deserializers.put(Character.class, value -> value.charAt(0));
        deserializers.put(char.class, value -> value.charAt(0));
        deserializers.put(Long.class, Long::valueOf);
        deserializers.put(long.class, Long::valueOf);
        deserializers.put(Short.class, Short::valueOf);
        deserializers.put(short.class, Short::valueOf);
        deserializers.put(String.class, value -> value);
        deserializers.put(boolean.class, Boolean::valueOf);
        deserializers.put(Boolean.class, Boolean::valueOf);
        deserializers.put(Object.class, value -> value);
        return deserializers;
    }

    /**
     * Used for turning attributes and tag names into objects. XML that looks like this:
     * &lt;tag attribute=&quot;value&quot;&gt;&lt;/tag&gt;
     * and also:
     * &lt;map&gt;
     * &lt;value&gt;something&lt;/value&gt;
     * &lt;/map&gt;
     *
     * @param value the value to convert
     * @return the object, null is allowed
     */
    Object convert(String value);

    /**
     * Used in most cases when we have an Element and we need to convert it into an Object.
     * The default implementation takes the text node and calls the convert(String) method. But if you want
     * to convert something special you will probably want to override this method.
     *
     * @param element the XML element to convert
     * @return the object, null is allowed
     */
    default Object convert(XmlElement element) {
        if (element == null) return null;
        final String text = element.getText();
        if (text == null) return null;
        return convert(text);
    }

    default <T> T convert(XmlElement element, Class<T> clazz) {
        return toObjectClass(clazz).cast(convert(element));
    }

}
