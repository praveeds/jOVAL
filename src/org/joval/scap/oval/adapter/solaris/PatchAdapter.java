// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.solaris;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.solaris.PatchObject;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.solaris.PatchItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates the legacy Solaris Patch OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PatchAdapter implements IAdapter {
    IUnixSession session;
    String error = null;
    boolean initialized = false;
    Hashtable<String, Collection<RevisionEntry>> revisions;
    Hashtable<String, Collection<SupercedenceEntry>> supercedence;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.SOLARIS) {
	    this.session = (IUnixSession)session;
	    revisions = new Hashtable<String, Collection<RevisionEntry>>();
	    supercedence = new Hashtable<String, Collection<SupercedenceEntry>>();
	    classes.add(PatchObject.class);
	} else {
	    notapplicable.add(PatchObject.class);
	}
	return classes;
    }


    public Collection<PatchItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (!initialized) {
	    scanRevisions();
	}

	PatchObject pObj = (PatchObject)obj;
	Collection<PatchItem> items = new ArrayList<PatchItem>();
	if (error != null) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(error);
	    rc.addMessage(msg);
	}

	try {
	    int iBase = Integer.parseInt((String)pObj.getBase().getValue());

	    switch(pObj.getBase().getOperation()) {
	      case EQUALS:
		items.addAll(getItems((String)pObj.getBase().getValue()));
		break;

	      case NOT_EQUAL:
		for (String base : revisions.keySet()) {
		    if (!base.equals((String)pObj.getBase().getValue())) {
			items.addAll(getItems(base));
		    }
		}
		break;

	      case LESS_THAN:
		for (String base : revisions.keySet()) {
		    if (Integer.parseInt(base) < iBase) {
			items.addAll(getItems(base));
		    }
		}
		break;

	      case LESS_THAN_OR_EQUAL:
		for (String base : revisions.keySet()) {
		    if (Integer.parseInt(base) <= iBase) {
			items.addAll(getItems(base));
		    }
		}
		break;

	      case GREATER_THAN:
		for (String base : revisions.keySet()) {
		    if (Integer.parseInt(base) > iBase) {
			items.addAll(getItems(base));
		    }
		}
		break;

	      case GREATER_THAN_OR_EQUAL:
		for (String base : revisions.keySet()) {
		    if (Integer.parseInt(base) >= iBase) {
			items.addAll(getItems(base));
		    }
		}
		break;

	      case PATTERN_MATCH:
		Pattern p = StringTools.pattern((String)pObj.getBase().getValue());
		for (String base : revisions.keySet()) {
		    if (p.matcher(base).find()) {
			items.addAll(getItems(base));
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, pObj.getBase().getOperation());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	} catch (NumberFormatException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (PatternSyntaxException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	return items;
    }

    // Internal

    private Collection<PatchItem> getItems(String base) {
	Collection<PatchItem> items = new ArrayList<PatchItem>();
	Collection<RevisionEntry> entries = revisions.get(base);
	if (entries != null) {
	    for (RevisionEntry entry : entries) {
		PatchItem item = Factories.sc.solaris.createPatchItem();
		EntityItemIntType baseType = Factories.sc.core.createEntityItemIntType();
		baseType.setValue(entry.patch.getBaseString());
		baseType.setDatatype(SimpleDatatypeEnumeration.INT.value());
		item.setBase(baseType);
		EntityItemIntType versionType = Factories.sc.core.createEntityItemIntType();
		versionType.setValue(entry.patch.getVersionString());
		versionType.setDatatype(SimpleDatatypeEnumeration.INT.value());
		item.setPatchVersion(versionType);
		items.add(item);
	    }
	}
	return items;
    }

    private static final String PATCH		= "Patch:";
    private static final String OBSOLETES	= "Obsoletes:";
    private static final String REQUIRES	= "Requires:";
    private static final String INCOMPATIBLES	= "Incompatibles:";
    private static final String PACKAGES	= "Packages:";

    /**
     * REMIND: Stops if it encounters any exceptions at all; make this more robust?
     */
    protected void scanRevisions() {
	try {
	    for (String line : SafeCLI.multiLine("/usr/bin/showrev -p", session, IUnixSession.Timeout.M)) {
		if (!line.startsWith(PATCH)) {
		    continue;
		}
		StringTokenizer tok = null;
		String buff = null;
		int begin, end;

		begin = line.indexOf(PATCH) + PATCH.length();
		end = line.indexOf(OBSOLETES);
		buff = line.substring(begin, end).trim();
		PatchEntry patch = new PatchEntry(buff);

		begin = line.indexOf(OBSOLETES) + OBSOLETES.length();
		end = line.indexOf(REQUIRES);
		buff = line.substring(begin, end).trim();
		ArrayList<PatchEntry> obsoletes = new ArrayList<PatchEntry>();
		tok = new StringTokenizer(buff, ",");
		while(tok.hasMoreTokens()) {
		    PatchEntry superceded = new PatchEntry(tok.nextToken().trim());
		    obsoletes.add(superceded);
		    String obsoleteBase = superceded.getBaseString();
		    Collection<SupercedenceEntry> list = supercedence.get(obsoleteBase);
		    if (list == null) {
			list = new ArrayList<SupercedenceEntry>();
			supercedence.put(obsoleteBase, list);
		    }
		    SupercedenceEntry entry = new SupercedenceEntry(superceded, patch);
		    if (!list.contains(entry)) {
			list.add(entry);
		    }
		}

		begin = line.indexOf(REQUIRES) + REQUIRES.length();
		end = line.indexOf(INCOMPATIBLES);
		buff = line.substring(begin, end).trim();
		ArrayList<PatchEntry> requires = new ArrayList<PatchEntry>();
		tok = new StringTokenizer(buff, ",");
		while(tok.hasMoreTokens()) {
		    requires.add(new PatchEntry(tok.nextToken().trim()));
		}

		begin = line.indexOf(INCOMPATIBLES) + INCOMPATIBLES.length();
		end = line.indexOf(PACKAGES);
		buff = line.substring(begin, end).trim();
		ArrayList<PatchEntry> incompatibles = new ArrayList<PatchEntry>();
		tok = new StringTokenizer(buff, ",");
		while(tok.hasMoreTokens()) {
		    incompatibles.add(new PatchEntry(tok.nextToken().trim()));
		}

		begin = line.indexOf(PACKAGES) + PACKAGES.length();
		buff = line.substring(begin).trim();
		ArrayList<String> packages = new ArrayList<String>();
		tok = new StringTokenizer(buff, ",");
		while(tok.hasMoreTokens()) {
		    packages.add(tok.nextToken().trim());
		}

		RevisionEntry entry = new RevisionEntry(patch, obsoletes, requires, incompatibles, packages);
		if (revisions.containsKey(patch.getBaseString())) {
		    revisions.get(patch.getBaseString()).add(entry);
		} else {
		    Collection<RevisionEntry> list = new ArrayList<RevisionEntry>();
		    list.add(entry);
		    revisions.put(patch.getBaseString(), list);
		}
	    }
	} catch (Exception e) {
	    error = e.getMessage();
	    session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	initialized = true;
    }

    class RevisionEntry {
	PatchEntry patch;
	Collection<PatchEntry> obsoletes, requires, incompatibles;
	Collection<String> packages;

	RevisionEntry(PatchEntry patch,
		      Collection<PatchEntry> obsoletes,
		      Collection<PatchEntry> requires,
		      Collection<PatchEntry> incompatibles,
		      Collection<String> packages) {
	    this.patch = patch;
	    this.obsoletes = obsoletes;
	    this.requires = requires;
	    this.incompatibles = incompatibles;
	    this.packages = packages;
	}
    }

    class PatchEntry {
	int base, version;

	PatchEntry(String id) throws Exception {
	    int ptr = id.indexOf("-");
	    base = Integer.parseInt(id.substring(0, ptr));
	    version = Integer.parseInt(id.substring(ptr+1));
	}

	String getBaseString() {
	    return Integer.toString(base);
	}

	String getVersionString() {
	    return Integer.toString(version);
	}
    }

    class SupercedenceEntry {
	PatchEntry superceded;
	PatchEntry by;

	SupercedenceEntry (PatchEntry superceded, PatchEntry by) {
	    this.superceded = superceded;
	    this.by = by;
	}

	public boolean equals(Object other) {
	    if (other instanceof SupercedenceEntry) {
		return superceded == ((SupercedenceEntry)other).superceded && by == ((SupercedenceEntry)other).by;
	    }
	    return false;
	}
    }
}
