// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.di;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.Vector;
import java.util.logging.Level;

import oval.schemas.common.FamilyEnumeration;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.di.IJovaldiPlugin;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.ISession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.plugin.adapter.independent.FamilyAdapter;
import org.joval.plugin.adapter.independent.Textfilecontent54Adapter;
import org.joval.plugin.adapter.linux.RpminfoAdapter;
import org.joval.plugin.adapter.unix.UnameAdapter;
import org.joval.plugin.adapter.windows.FileAdapter;
import org.joval.plugin.adapter.windows.RegistryAdapter;
import org.joval.plugin.adapter.windows.WmiAdapter;
import org.joval.unix.UnixSystemInfo;
import org.joval.util.JOVALSystem;
import org.joval.windows.WindowsSystemInfo;

/**
 * Abstract base class for jovaldi plug-ins.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BasePlugin implements IJovaldiPlugin {
    private static PropertyResourceBundle resources;
    static {
	try {
	    ClassLoader cl = BasePlugin.class.getClassLoader();
	    Locale locale = Locale.getDefault();
	    URL url = cl.getResource("plugin.resources_" + locale.toString() + ".properties");
	    if (url == null) {
		url = cl.getResource("plugin.resources_" + locale.getLanguage() + ".properties");
	    }
	    if (url == null) {
		url = cl.getResource("plugin.resources.properties");
	    }
	    resources = new PropertyResourceBundle(url.openStream());
	} catch (IOException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	}
    }

    /**
     * Retrieve a message using its key.
     */
    protected static final String getMessage(String key, Object... arguments) {
        return MessageFormat.format(resources.getString(key), arguments);
    }

    protected ISession session;
    protected SystemInfoType info;
    protected String err;
    protected List<IAdapter> adapters;

    /**
     * Create a plugin for scanning or test evaluation.
     */
    protected BasePlugin() {
	adapters = new Vector<IAdapter>();
    }

    // Implement IJovaldiPlugin

    public void connect() {
	adapters.add(new FamilyAdapter(this));

	if (session == null) {
	    //
	    // If there is no session, then the IPlugin is being invoked to perform analysis on a pre-existing
	    // System-Characteristics file.  Therefore, all adapters are added, but without resources for data retrieval.
	    //
	    adapters.add(new Textfilecontent54Adapter(null));
	    adapters.add(new WmiAdapter(null));
	    adapters.add(new FileAdapter(null, null));
	    adapters.add(new RegistryAdapter(null));
	} else if (session.connect()) {
	    adapters.add(new Textfilecontent54Adapter(session.getFilesystem()));

	    switch(session.getType()) {
	      case ISession.WINDOWS: {
		IWindowsSession win = (IWindowsSession)session;
		//
		// Gather SystemInfo data
		//
		WindowsSystemInfo wsi = new WindowsSystemInfo(win.getRegistry(), win.getWmiProvider());
		try {
		    info = wsi.getSystemInfo();
		} catch (Exception e) {
		    throw new RuntimeException(getMessage("ERROR_INFO"), e);
		}
		adapters.add(new WmiAdapter(win.getWmiProvider()));
		adapters.add(new FileAdapter(win.getFilesystem(), win.getWmiProvider()));
		adapters.add(new RegistryAdapter(win.getRegistry()));
		break;
	      }

	      case ISession.UNIX: {
		info = new UnixSystemInfo(session).getSystemInfo();
		adapters.add(new RpminfoAdapter(session));
		adapters.add(new UnameAdapter(session));
		break;
	      }
	    }
	} else {
	    throw new RuntimeException(getMessage("ERROR_SESSION_CONNECTION"));
	}
    }

    public void disconnect() {
	if (session != null) {
	    session.disconnect();
	}
    }

    public void setDataDirectory(File dir) {
    }

    public String getProperty(String key) {
	return resources.getString(key);
    }

    public String getLastError() {
	return err;
    }

    public List<IAdapter> getAdapters() {
	return adapters;
    }

    public SystemInfoType getSystemInfo() {
	return info;
    }

    public FamilyEnumeration getFamily() {
	switch(session.getType()) {
	  case ISession.WINDOWS:
	    return FamilyEnumeration.WINDOWS;

	  case ISession.UNIX:
	    return FamilyEnumeration.UNIX;

	  default:
	    return FamilyEnumeration.UNDEFINED;
	}
    }
}
