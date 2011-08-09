// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.unix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.definitions.core.EntityStateStringType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.unix.UnameObject;
import oval.schemas.definitions.unix.UnameState;
import oval.schemas.definitions.unix.UnameTest;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.unix.UnameItem;
import oval.schemas.systemcharacteristics.unix.ObjectFactory;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.util.JOVALSystem;

/**
 * Evaluates UnameTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnameAdapter implements IAdapter {
    private IAdapterContext ctx;
    private ISession session;
    private oval.schemas.systemcharacteristics.core.ObjectFactory coreFactory;
    private ObjectFactory unixFactory;

    public UnameAdapter(ISession session) {
	this.session = session;
	coreFactory = new oval.schemas.systemcharacteristics.core.ObjectFactory();
	unixFactory = new ObjectFactory();
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
    }

    public Class getObjectClass() {
	return UnameObject.class;
    }

    public Class getStateClass() {
	return UnameState.class;
    }

    public Class getItemClass() {
	return UnameItem.class;
    }

    public boolean connect() {
	return session != null;
    }

    public void disconnect() {
    }

    public List<JAXBElement<? extends ItemType>> getItems(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	try {
	    items.add(getItem());
	} catch (Exception e) {
	    MessageType msg = new MessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    ctx.addObjectMessage(obj.getId(), msg);
	    ctx.log(Level.WARNING, e.getMessage(), e);
	}
	return items;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws TestException, OvalException {
	UnameState state = (UnameState)st;
	UnameItem item = (UnameItem)it;

	if (state.isSetMachineClass()) {
	    return ctx.test(state.getMachineClass(), item.getMachineClass());
	} else if (state.isSetNodeName()) {
	    return ctx.test(state.getNodeName(), item.getNodeName());
	} else if (state.isSetOsName()) {
	    return ctx.test(state.getOsName(), item.getOsName());
	} else if (state.isSetOsRelease()) {
	    return ctx.test(state.getOsRelease(), item.getOsRelease());
	} else if (state.isSetOsVersion()) {
	    return ctx.test(state.getOsVersion(), item.getOsVersion());
	} else if (state.isSetProcessorType()) {
	    return ctx.test(state.getProcessorType(), item.getProcessorType());
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_STATE_EMPTY", state.getId()));
	}
    }

    // Internal

    private JAXBElement<UnameItem> getItem() throws Exception {
	UnameItem item = unixFactory.createUnameItem();
	EntityItemStringType machineClass = coreFactory.createEntityItemStringType();
	IProcess p = session.createProcess("uname -m");
	p.start();
	BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	String result = br.readLine();
	br.close();
	machineClass.setValue(result);
	item.setMachineClass(machineClass);

	EntityItemStringType nodeName = coreFactory.createEntityItemStringType();
	p = session.createProcess("uname -n");
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	nodeName.setValue(result);
	item.setNodeName(nodeName);

	EntityItemStringType osName = coreFactory.createEntityItemStringType();
	p = session.createProcess("uname -s");
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	osName.setValue(result);
	item.setOsName(osName);

	EntityItemStringType osRelease = coreFactory.createEntityItemStringType();
	p = session.createProcess("uname -r");
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	osRelease.setValue(result);
	item.setOsRelease(osRelease);

	EntityItemStringType osVersion = coreFactory.createEntityItemStringType();
	p = session.createProcess("uname -v");
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	osVersion.setValue(result);
	item.setOsVersion(osVersion);

	EntityItemStringType processorType = coreFactory.createEntityItemStringType();
	p = session.createProcess("uname -p");
	p.start();
	br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	result = br.readLine();
	br.close();
	processorType.setValue(result);
	item.setProcessorType(processorType);

	return unixFactory.createUnameItem(item);
    }
}