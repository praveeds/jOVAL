// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.independent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.IEnvironment;
import jsaf.intf.system.ISession;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.independent.EnvironmentvariableObject;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.independent.EnvironmentvariableItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates Environmentvariable OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class EnvironmentvariableAdapter implements IAdapter {
    private ISession session;
    private IEnvironment environment;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	try {
	    this.session = session;
	    environment = session.getEnvironment();
	    classes.add(EnvironmentvariableObject.class);
	} catch (UnsupportedOperationException e) {
	    // ISession.getEnvironment not supported
	    notapplicable.add(EnvironmentvariableObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	return getItems(obj, rc, environment, null);
    }

    // Internal

    Collection<EnvironmentvariableItem> getItems(ObjectType obj, IRequestContext rc, IEnvironment env, String reserved)
		throws CollectException {

	List<EnvironmentvariableItem> items = new ArrayList<EnvironmentvariableItem>();
	EnvironmentvariableObject eObj = (EnvironmentvariableObject)obj;
	String name = (String)eObj.getName().getValue();

	OperationEnumeration op = eObj.getName().getOperation();
	switch(op) {
	  case EQUALS:
	    if (env.getenv(name) != null) {
		items.add((EnvironmentvariableItem)makeItem(name, env.getenv(name), reserved));
	    }
	    break;

	  case CASE_INSENSITIVE_EQUALS:
	    for (String varName : env) {
		if (varName.equalsIgnoreCase(name)) {
		    items.add((EnvironmentvariableItem)makeItem(varName, env.getenv(varName), reserved));
		    break;
		}
	    }
	    break;

	  case NOT_EQUAL:
	    for (String varName : env) {
		if (!name.equals(varName)) {
		    items.add((EnvironmentvariableItem)makeItem(name, env.getenv(varName), reserved));
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = Pattern.compile(name);
		for (String varName : env) {
		    if (p.matcher(varName).find()) {
			items.add((EnvironmentvariableItem)makeItem(varName, env.getenv(varName), reserved));
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
	return items;
    }

    ItemType makeItem(String name, String value, String reserved) {
	EnvironmentvariableItem item = Factories.sc.independent.createEnvironmentvariableItem();
	EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
	nameType.setValue(name);
	item.setName(nameType);
	EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
	valueType.setValue(value);
	item.setValue(valueType);
	return item;
    }
}
