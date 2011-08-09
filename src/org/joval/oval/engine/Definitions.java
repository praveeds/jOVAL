// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Level;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import oval.schemas.definitions.core.DefinitionType;
import oval.schemas.definitions.core.DefinitionsType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.ObjectsType;
import oval.schemas.definitions.core.OvalDefinitions;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.core.StatesType;
import oval.schemas.definitions.core.TestType;
import oval.schemas.definitions.core.TestsType;
import oval.schemas.definitions.core.VariableType;
import oval.schemas.definitions.core.VariablesType;

import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;

/**
 * Index to an OvalDefinitions object, for fast look-up of definitions, tests, variables, objects and states.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class Definitions {
    private static final int LEAF	= 1;
    private static final int SET	= 2;

    private static List<String> schematronValidationErrors = null;

    /**
     * Unmarshalls an XML file and returns the root OvalDefinitions object.
     */
    static final OvalDefinitions getOvalDefinitions(File f) throws OvalException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_DEFINITIONS));
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(f);
	    if (rootObj instanceof OvalDefinitions) {
		return (OvalDefinitions)rootObj;
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_DEFINITIONS_BAD_FILE", f.toString()));
	    }
	} catch (JAXBException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_DEFINITIONS_PARSE"), e);
	    throw new OvalException(e);
	}
    }

    /**
     * Applies the specified Schematron XSL template transformation to the OvalDefinitions, and verifies that the result
     * is an empty document.
     */
    static boolean schematronValidate(OvalDefinitions defs, File schematronTemplate) {
	schematronValidationErrors = null;
	try {
	    TransformerFactory xf = TransformerFactory.newInstance();
	    Transformer transformer = xf.newTransformer(new StreamSource(new FileInputStream(schematronTemplate)));
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_DEFINITIONS));
	    DOMResult result = new DOMResult();
	    transformer.transform(new JAXBSource(ctx, defs), result);
	    Node root = result.getNode();
	    if (root.getNodeType() == Node.DOCUMENT_NODE) {
		NodeList children = root.getChildNodes();
		int len = children.getLength();
		if (len == 0) {
		    return true;
		} else {
		    schematronValidationErrors = new Vector<String>();
		    for (int i=0; i < len; i++) {
			schematronValidationErrors.add(children.item(i).getTextContent());
		    }
		}
	    }
	} catch (FileNotFoundException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_DEFINITIONS_SCHEMATRON_VALIDATION"), e);
	} catch (JAXBException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_DEFINITIONS_SCHEMATRON_VALIDATION"), e);
	} catch (TransformerConfigurationException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_DEFINITIONS_SCHEMATRON_VALIDATION"), e);
	} catch (TransformerException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_DEFINITIONS_SCHEMATRON_VALIDATION"), e);
	}
	return false;
    }

    static List<String> getSchematronValidationErrors() {
	return schematronValidationErrors;
    }

    private OvalDefinitions defs;
    private Hashtable <String, DefinitionType>definitions;
    private Hashtable <String, TestType>tests;
    private Hashtable <String, StateType>states;
    private Hashtable <String, VariableType>variables;
    private Hashtable <String, ObjectType>objects;

    Definitions(OvalDefinitions defs) {
	this.defs = defs;

	objects = new Hashtable <String, ObjectType>();
	if (defs.getObjects() != null) {
	    List <JAXBElement <? extends ObjectType>> objectList = defs.getObjects().getObject();
	    int len = objectList.size();
	    for (int i=0; i < len; i++) {
		ObjectType ot = objectList.get(i).getValue();
		objects.put(ot.getId(), ot);
	    }
	}

	tests = new Hashtable <String, TestType>();
	List <JAXBElement <? extends TestType>> testList = defs.getTests().getTest();
	int len = testList.size();
	for (int i=0; i < len; i++) {
	    TestType tt = testList.get(i).getValue();
	    tests.put(tt.getId(), tt);
	}

	variables = new Hashtable <String, VariableType>();
	if (defs.getVariables() != null) {
	    List <JAXBElement <? extends VariableType>> varList = defs.getVariables().getVariable();
	    len = varList.size();
	    for (int i=0; i < len; i++) {
		VariableType vt = varList.get(i).getValue();
		variables.put(vt.getId(), vt);
	    }
	}

	states = new Hashtable <String, StateType>();
	if (defs.getStates() != null) {
	    List <JAXBElement <? extends StateType>> stateList = defs.getStates().getState();
	    len = stateList.size();
	    for (int i=0; i < len; i++) {
		StateType st = stateList.get(i).getValue();
		states.put(st.getId(), st);
	    }
	}

	definitions = new Hashtable <String, DefinitionType>();
	List <DefinitionType> defList = defs.getDefinitions().getDefinition();
	len = defList.size();
	for (int i=0; i < len; i++) {
	    DefinitionType dt = defList.get(i);
	    definitions.put(dt.getId(), dt);
	}
    }

    OvalDefinitions getOvalDefinitions() {
	return defs;
    }

    // Internal

    <T extends ObjectType> T getObject(String id, Class<T> type) throws OvalException {
	ObjectType object = objects.get(id);
	if (object == null) {
	    throw new OvalException("Unresolved object reference ID=" + id);
	} else if (type.isInstance(object)) {
	    return type.cast(object);
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE", type.getName(), object.getClass().getName()));
	}
    }

    Iterator<ObjectType> iterateLeafObjects(Class type) {
	return new SpecifiedObjectIterator(type, LEAF);
    }

    Iterator<ObjectType> iterateSetObjects() {
	return new SpecifiedObjectIterator(ObjectType.class, SET);
    }

    /**
     * Type-checked retrieval of a StateType.
     */
    StateType getState(String id, Class type) throws OvalException {
	StateType state = states.get(id);
	if (state == null) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_REF_STATE", id));
	} else if (type.isInstance(state)) {
	    return state;
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE", type.getName(), state.getClass().getName()));
	}
    }

    TestType getTest(String id) throws OvalException {
	TestType test = tests.get(id);
	if (test == null) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_REF_TEST", id));
	}
	return test;
    }

    ObjectType getObject(String id) throws OvalException {
	ObjectType object = objects.get(id);
	if (object == null) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_REF_OBJECT", id));
	}
	return object;
    }

    VariableType getVariable(String id) throws OvalException {
	VariableType variable = variables.get(id);
	if (variable == null) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_REF_VARIABLE", id));
	} else {
	    return variable;
	}
    }

    DefinitionType getDefinition(String id) throws OvalException {
	DefinitionType definition = definitions.get(id);
	if (definition == null) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_REF_DEFINITION", id));
	}
	return definition;
    }

    /**
     * Sort all DefinitionTypes into two lists according to whether the filter allows/disallows them.
     */
    void filterDefinitions(DefinitionFilter filter, List<DefinitionType> allowed, List<DefinitionType> disallowed) {
	for (DefinitionType dt : definitions.values()) {
	    if (filter.accept(dt.getId())) {
		allowed.add(dt);
	    } else {
		disallowed.add(dt);
	    }
	}
    }

    Iterator <ObjectType>iterateObjects() {
	return objects.values().iterator();
    }

    Iterator <VariableType>iterateVariables() {
	return variables.values().iterator();
    }

    // Private


    private boolean isSet(ObjectType obj) {
        try {
            Method isSetSet = obj.getClass().getMethod("isSetSet");
            return ((Boolean)isSetSet.invoke(obj)).booleanValue();
        } catch (NoSuchMethodException e) {
        } catch (IllegalAccessException e) {
            JOVALSystem.getLogger().log(Level.SEVERE, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
        } catch (InvocationTargetException e) {
            JOVALSystem.getLogger().log(Level.SEVERE, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
        }
        return false;
    }

    class SpecifiedObjectIterator implements Iterator<ObjectType> {
	Iterator <ObjectType>iter;
	Class type;
	ObjectType next;
	int flags;

	SpecifiedObjectIterator(Class type, int flags) {
	    this.type = type;
	    this.flags = flags;
	    iter = objects.values().iterator();
	}

	public boolean hasNext() {
	    if (next != null) {
		return true;
	    }
	    try {
		next = next();
		return true;
	    } catch (NoSuchElementException e) {
	    }
	    return false;
	}

	public ObjectType next() throws NoSuchElementException {
	    if (next != null) {
		ObjectType temp = next;
		next = null;
		return temp;
	    }
	    while (true) {
		ObjectType temp = iter.next();
		if (type.isInstance(temp)) {
		    boolean isSet = isSet(temp);
		    if ((flags & LEAF) == LEAF && !isSet) {
			return temp;
		    } else if ((flags & SET) == SET && isSet) {
			return temp;
		    }
		}
	    }
	}

	public void remove() throws UnsupportedOperationException {
	    throw new UnsupportedOperationException("remove");
	}
    }
}