package xmlparser;

import xmlparser.annotations.XmlAbstractClass;
import xmlparser.annotations.XmlEnumValue;
import xmlparser.annotations.XmlMapTagIsKey;
import xmlparser.annotations.XmlMapWithAttributes;
import xmlparser.annotations.XmlMapWithChildNodes;
import xmlparser.annotations.XmlNoImport;
import xmlparser.annotations.XmlObjectValidator;
import xmlparser.annotations.XmlPath;
import xmlparser.error.InvalidAnnotation;
import xmlparser.error.InvalidObject;
import xmlparser.error.InvalidXPath;
import xmlparser.model.XmlElement;
import xmlparser.parsing.DomBuilder;
import xmlparser.parsing.ObjectDeserializer;
import xmlparser.utils.Escaping.UnEscape;
import xmlparser.utils.Interfaces.AccessDeserializers;
import xmlparser.utils.Trimming.Trim;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.objenesis.ObjenesisHelper.newInstance;
import static xmlparser.model.XmlElement.findChildForName;
import static xmlparser.utils.Reflection.findAbstractType;
import static xmlparser.utils.Reflection.invokeFieldDeserializer;
import static xmlparser.utils.Reflection.isWrapped;
import static xmlparser.utils.Reflection.listFields;
import static xmlparser.utils.Reflection.setField;
import static xmlparser.utils.Reflection.toClassOfCollection;
import static xmlparser.utils.Reflection.toClassOfMapKey;
import static xmlparser.utils.Reflection.toClassOfMapValue;
import static xmlparser.utils.Reflection.toEnumType;
import static xmlparser.utils.Reflection.toFieldType;
import static xmlparser.utils.Reflection.toName;
import static xmlparser.utils.Reflection.toWrappedName;
import static xmlparser.utils.Validator.multipleAreNotNull;
import static xmlparser.xpath.XPathExpression.newXPath;

public interface XmlReader extends AccessDeserializers {

    static XmlElement toXmlDom(final InputStreamReader in, final Trim trimmer, final UnEscape escaper) throws IOException {
        final DomBuilder p = new DomBuilder();
        XmlStreamReader.toXmlStream(in, p, trimmer, escaper);
        return p.getRoot();
    }

    default <T> T domToObject(final XmlElement node, final Class<T> clazz) throws InvalidXPath {
        if (node == null) return null;
        final ObjectDeserializer c = getDeserializer(clazz);
        if (c != null) return c.convert(node, clazz);

        final T object = domToClass(node, clazz);
        for (final Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(XmlObjectValidator.class))
                continue;
            try {
                method.invoke(object);
            } catch (final Exception e) {
                throw new InvalidObject(clazz, e);
            }
        }

        return object;
    }

    default <T> T domToClass(final XmlElement node, final Class<T> clazz) {
        final T o = newInstance(clazz);

        final String parentName = toName(clazz);
        XmlElement selectedNode;
        for (final Field f : listFields(clazz)) {
            f.setAccessible(true);
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (f.isAnnotationPresent(XmlNoImport.class)) continue;

            selectedNode = node;
            if (f.isAnnotationPresent(XmlPath.class)) {
                selectedNode = newXPath(parentName + "/" + f.getAnnotation(XmlPath.class).value()).evaluateAny(node);
            }

            final Object fieldValue;
            switch (toFieldType(f)) {
                case FIELD_DESERIALIZER: {
                    fieldValue = invokeFieldDeserializer(f, selectedNode);
                    break;
                }
                case TEXTNODE: {
                    fieldValue = textNodeToValue(f.getType(), selectedNode);
                    break;
                }
                case ANNOTATED_ATTRIBUTE: {
                    fieldValue = attributeToValue(f.getType(), toName(f), deWrap(selectedNode, f));
                    break;
                }
                case SET: {
                    fieldValue = domToSet(f, toClassOfCollection(f), toName(f), deWrap(selectedNode, f));
                    break;
                }
                case LIST: {
                    fieldValue = domToList(f, toClassOfCollection(f), toName(f), deWrap(selectedNode, f));
                    break;
                }
                case ARRAY: {
                    fieldValue = domToArray(f.getType().getComponentType(), toName(f), deWrap(selectedNode, f));
                    break;
                }
                case MAP: {
                    fieldValue = domToMap(f, (ParameterizedType) f.getGenericType(), toName(f), deWrap(selectedNode, f));
                    break;
                }
                case ENUM: {
                    fieldValue = enumNodeToValue(toEnumType(f), toName(f), deWrap(selectedNode, f));
                    break;
                }
                default: {
                    final String name = toName(f);
                    if (selectedNode == null) {
                        fieldValue = null;
                        break;
                    }

                    final String value = selectedNode.attributes.get(name);
                    if (value != null) {
                        fieldValue = stringToValue(f.getType(), value);
                        break;
                    }
                    final XmlAbstractClass isAbstract = f.getAnnotation(XmlAbstractClass.class);
                    if (isAbstract != null) {
                        final XmlElement child = selectedNode.findChildForName(name, null);
                        fieldValue = domToObject(child, findAbstractType(isAbstract, child));
                        break;
                    }
                    fieldValue = domToObject(findChildForName(deWrap(selectedNode, f), name, null), f.getType());
                }
            }
            setField(f, o, fieldValue);
        }
        return o;
    }

    default XmlElement deWrap(final XmlElement element, final Field field) {
        if (!isWrapped(field)) return element;
        return element.findChildForName(toWrappedName(field), null);
    }

    default Object textNodeToValue(final Class<?> type, final XmlElement node) {
        final ObjectDeserializer conv = getDeserializer(type);
        return (conv != null) ? conv.convert(node) : null;
    }

    default Object enumNodeToValue(final Class<? extends Enum<?>> type, final String name, final XmlElement node) {
        final XmlElement text = findChildForName(node, name, null);
        if (text == null) return null;
        final String value = text.getText();
        if (value == null) return null;
        final ObjectDeserializer conv = getDeserializer(type);
        return (conv == null) ? valueOfEnum(type, value) : conv.convert(node);
    }

    boolean isEnumCachingEnabled();

    <T extends Enum<?>> Map<Class<T>, Map<String, T>> getEnumCache();

    default <T extends Enum<?>> Map<String, T> getEnumValueDirectory(final Class<T> type) {
        final boolean enumCaching = isEnumCachingEnabled();
        if (enumCaching) {
            final Map<Class<T>, Map<String, T>> cache = getEnumCache();
            return cache.computeIfAbsent(type, this::newEnumValueMap);
        }
        return newEnumValueMap(type);
    }

    default <T extends Enum<?>> Map<String, T> newEnumValueMap(final Class<T> type) {
        final Map<String, T> valueMap = new HashMap<>();
        for (final T t : type.getEnumConstants()) {
            try {
                final XmlEnumValue annotation = type.getField(t.name()).getAnnotation(XmlEnumValue.class);
                valueMap.put(annotation != null ? annotation.value() : t.name(), t);
            } catch (final NoSuchFieldException e) {
                // impossible
                throw new RuntimeException(e);
            }
        }
        return valueMap;
    }

    default <T extends Enum<?>> T valueOfEnum(final Class<T> type, final String value) {
        final T t = getEnumValueDirectory(type).get(value);
        if (t == null) throw new IllegalArgumentException("No enum constant for " + type.getName() + "." + value);
        return t;
    }

    default Object attributeToValue(final Class<?> type, final String name, final XmlElement node) {
        if (node == null) return null;
        final ObjectDeserializer conv = getDeserializer(type);
        if (conv == null) return null;
        final String value = node.attributes.get(name);
        if (value == null) return null;
        return conv.convert(value);
    }

    default Object stringToValue(final Class<?> type, final String value) {
        final ObjectDeserializer conv = getDeserializer(type);
        return (conv != null) ? conv.convert(value) : null;
    }

    default Set<Object> domToSet(final Field field, final Class<?> type, final String name, final XmlElement node) throws InvalidXPath {
        return node == null ? null : domToSet(field.getAnnotation(XmlAbstractClass.class), type, name, node);
    }

    default Set<Object> domToSet(final XmlAbstractClass isAbstract, final Class<?> type, final String name, final XmlElement node) throws InvalidXPath {
        final ObjectDeserializer elementConv = getDeserializer(type);

        final Set<Object> set = new HashSet<>();
        for (final XmlElement n : node.children) {
            if (!n.name.equals(name)) continue;
            if (isAbstract != null) {
                set.add(domToObject(n, findAbstractType(isAbstract, n)));
                continue;
            }

            set.add((elementConv == null) ? domToObject(n, type) : elementConv.convert(n));
        }
        return set;
    }

    default List<Object> domToList(final Field field, final Class<?> type, final String name, final XmlElement node) throws InvalidXPath {
        return node == null ? null : domToList(field.getAnnotation(XmlAbstractClass.class), type, name, node);
    }

    default List<Object> domToList(final XmlAbstractClass isAbstract, final Class<?> type, final String name, final XmlElement node) throws InvalidXPath {
        if (node == null) return null;
        final ObjectDeserializer elementConv = getDeserializer(type);

        final List<Object> list = new LinkedList<>();
        for (final XmlElement n : node.children) {
            if (!n.name.equals(name)) continue;
            if (isAbstract != null) {
                list.add(domToObject(n, findAbstractType(isAbstract, n)));
                continue;
            }

            list.add((elementConv == null) ? domToObject(n, type) : elementConv.convert(n));
        }
        return list;
    }

    default Object[] domToArray(final Class<?> type, final String name, final XmlElement node) throws InvalidXPath {
        if (node == null) return null;
        final ObjectDeserializer elementConv = getDeserializer(type);

        final Object[] array = (Object[]) Array.newInstance(type, node.numChildrenWithName(name));
        int i = 0;
        for (final XmlElement n : node.children) {
            if (n.name.equals(name)) {
                array[i] = (elementConv == null) ? domToObject(n, type) : elementConv.convert(n, type);
                i++;
            }
        }
        return array;
    }

    default Map<Object, Object> domToMap(final Field field, final ParameterizedType type, final String name, final XmlElement node) {
        if (node == null) return null;

        final XmlMapTagIsKey isXmlMapTagIsKey = field.getAnnotation(XmlMapTagIsKey.class);
        final XmlMapWithAttributes isXmlMapWithAttributes = field.getAnnotation(XmlMapWithAttributes.class);
        final XmlMapWithChildNodes isXmlMapWithChildNodes = field.getAnnotation(XmlMapWithChildNodes.class);

        return domToMap(isXmlMapTagIsKey, isXmlMapWithAttributes, isXmlMapWithChildNodes, type, name, node);
    }

    default Map<Object, Object> domToMap(final XmlMapTagIsKey isXmlMapTagIsKey, final XmlMapWithAttributes isXmlMapWithAttributes
            , final XmlMapWithChildNodes isXmlMapWithChildNodes, final ParameterizedType type, final String name
            , final XmlElement node) {
        if (multipleAreNotNull(isXmlMapTagIsKey, isXmlMapWithAttributes, isXmlMapWithChildNodes))
            throw new InvalidAnnotation("Only one of XmlMapTagIsKey, XmlMapWithAttributes and XmlMapWithChildNodes is allowed per field");

        final ObjectDeserializer convKey = getDeserializer(toClassOfMapKey(type));
        final ObjectDeserializer convVal = getDeserializer(toClassOfMapValue(type));

        if (isXmlMapWithAttributes != null) {
            final String keyName = isXmlMapWithAttributes.keyName();
            final String valueName = isXmlMapWithAttributes.valueName();

            final Map<Object, Object> map = new HashMap<>();
            for (final XmlElement child : node.children) {
                if (!name.equals(child.name)) continue;

                final String key = child.attributes.get(keyName);
                if (key == null) continue;

                final String value = valueName.isEmpty() ? child.getText() : child.attributes.get(valueName);
                map.put(convKey.convert(key), convVal.convert(value));
            }
            return map;
        }
        if (isXmlMapWithChildNodes != null) {
            final String keyName = isXmlMapWithChildNodes.keyName();
            final String valueName = isXmlMapWithChildNodes.valueName();

            final Map<Object, Object> map = new HashMap<>();
            for (final XmlElement child : node.children) {
                if (!name.equals(child.name)) continue;

                final XmlElement key = child.findChildForName(keyName, null);
                if (key == null) continue;

                final XmlElement value = valueName.isEmpty() ? child : child.findChildForName(valueName, null);
                if (value == null) continue;

                map.put(convKey.convert(key.getText()), convVal.convert(value.getText()));
            }
            return map;
        }

        final XmlElement element = node.findChildForName(name, null);
        if (element == null) return null;

        final Map<Object, Object> map = new HashMap<>();
        for (final XmlElement child : element.children) {
            map.put(convKey.convert(child.name), convVal.convert(child));
        }
        return map;
    }

}
