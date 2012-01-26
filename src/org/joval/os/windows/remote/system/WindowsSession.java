// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.system;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.slf4j.cal10n.LocLogger;

import org.jinterop.dcom.common.JISystem;

import com.h9labs.jwbem.SWbemLocator;
import com.h9labs.jwbem.SWbemServices;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.identity.IWindowsCredential;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.WindowsSystemInfo;
import org.joval.os.windows.identity.Directory;
import org.joval.os.windows.io.WOW3264FilesystemRedirector;
import org.joval.os.windows.registry.WOW3264RegistryRedirector;
import org.joval.os.windows.remote.io.SmbFilesystem;
import org.joval.os.windows.remote.registry.Registry;
import org.joval.os.windows.remote.wmi.WmiConnection;
import org.joval.util.AbstractSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsSession extends AbstractSession implements IWindowsSession, ILocked {
    private static int counter = 0;
    static {
	JISystem.getLogger().setLevel(Level.WARNING);
	JISystem.setAutoRegisteration(true);
	JISystem.setJavaCoClassAutoCollection(false);
    }

    private String host;
    private String tempDir, cwd;
    private IWindowsCredential cred;
    private WmiConnection conn;
    private IRegistry reg, reg32;
    private IFilesystem fs32;
    private Vector<IFile> tempFiles;
    private boolean is64bit = false;
    private WindowsSystemInfo info = null;
    private Directory directory = null;

    public WindowsSession(String host) {
	super();
	this.host = host;
	tempFiles = new Vector<IFile>();
	info = new WindowsSystemInfo(this);
    }

    // Implement IWindowsSession extensions

    public IDirectory getDirectory() {
	if (directory == null) {
	    directory = new Directory(this);
	}
	return directory;
    }

    public IRegistry getRegistry(View view) {
	switch(view) {
	  case _32BIT:
	    return reg32;
	}
	return reg;
    }

    public boolean supports(View view) {
	switch(view) {
	  case _32BIT:
	    return true;
	  case _64BIT:
	  default:
	    return is64bit;
	}
    }

    public IFilesystem getFilesystem(View view) {
	switch(view) {
	  case _32BIT:
	    return fs32;
	}
	return fs;
    }

    public IWmiProvider getWmiProvider() {
	if (conn == null) {
	    conn = new WmiConnection(host, cred, this);
	}
	return conn;
    }

    // Implement ILoggable

    /**
     * @override
     */
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	if (fs32 != null && !fs32.equals(fs)) {
	    fs32.setLogger(logger);
	}
    }

    // Implement ILocked

    public boolean unlock(ICredential credential) {
	if (credential instanceof IWindowsCredential) {
	    cred = (IWindowsCredential)credential;
	    return true;
	} else {
	    return false;
	}
    }

    // Implement IBaseSession

    /**
     * @override
     */
    public void setWorkingDir(String path) {
	cwd = env.expand(path);
    }

    /**
     * @override
     */
    public IProcess createProcess(String command) throws Exception {
	StringBuffer sb = new StringBuffer(tempDir).append(fs.getDelimiter()).append("rexec_");
	sb.append(Integer.toHexString(counter++));

	IFile out = fs.getFile(sb.toString() + ".out", true);
	out.getOutputStream(false).close(); // create/clear tmpOutFile
	tempFiles.add(out);

	IFile err = fs.getFile(sb.toString() + ".err", true);
	err.getOutputStream(false).close(); // create/clear tmpErrFile
	tempFiles.add(err);

	WindowsProcess p = new WindowsProcess(conn.getServices(host, IWmiProvider.CIMv2), command, cwd, out, err);
	return p;
    }

    public String getHostname() {
	return host;
    }

    public boolean connect() {
	if (cred == null) {
	    return false;
	} else {
	    reg = new Registry(host, cred, null, this);
	    if (reg.connect()) {
		env = reg.getEnvironment();
		fs = new SmbFilesystem(host, cred, env, null, logger);
		is64bit = env.getenv(ENV_ARCH).indexOf("64") != -1;
		if (is64bit) {
		    WOW3264RegistryRedirector.Flavor flavor = WOW3264RegistryRedirector.getFlavor(reg);
		    reg32 = new Registry(host, cred, new WOW3264RegistryRedirector(flavor), this);
		    fs32 = new SmbFilesystem(host, cred, env, new WOW3264FilesystemRedirector(env), logger);//DAS
		} else {
		    reg32 = reg;
		    fs32 = fs;
		}
		reg.disconnect();
		try {
		    tempDir = getTempDir();
		} catch (IOException e) {
		    return false;
		}
		cwd = env.expand("%SystemRoot%");
		conn = new WmiConnection(host, cred, this);
		if (conn.connect()) {
		    directory = new Directory(this);
		    directory.connect();
		    info.getSystemInfo();
		    return true;
		} else {
		    return false;
		}
	    } else {
		return false;
	    }
	}
    }

    public void disconnect() {
	Iterator<IFile> iter = tempFiles.iterator();
	while(iter.hasNext()) {
	    IFile f = iter.next();
	    try {
		synchronized(f) {
		    if (f.exists()) {
			f.delete();
		    }
		}
	    } catch (Exception e) {
		logger.warn(JOVALMsg.ERROR_FILE_DELETE, f.toString());
	    }
	}
	if (directory != null) {
	    directory.disconnect();
	    directory = null;
	}
	if (conn != null) {
	    conn.disconnect();
	    conn = null;
	}
    }

    public Type getType() {
	return Type.WINDOWS;
    }

    // Implement ISession

    public SystemInfoType getSystemInfo() {
	return info.getSystemInfo();
    }

    // Private

    private String getTempDir() throws IOException {
	Iterator<String> iter = getTempDirCandidates().iterator();
	while(iter.hasNext()) {
	    String path = iter.next();
	    if (testDir(path)) {
		return path;
	    }
	}
	throw new IOException("Unable to find a temp directory");
    }

    private List<String> getTempDirCandidates() {
	List<String> list = new Vector<String>();
	list.add("%TMP%");
	list.add("%TEMP%");
	list.add("%SystemDrive%\\Users\\" + cred.getUsername() + "\\AppData\\Local\\Temp");
	list.add("C:\\Users\\" + cred.getUsername() + "\\AppData\\Local\\Temp");
	list.add("%SystemRoot%\\Temp");
	return list;
    }

    private boolean testDir(String path) {
	if (path != null) {
	    try {
		IFile f = fs.getFile(path);
		return f.isDirectory();
	    } catch (Exception e) {
	    }
	}
	return false;
    }
}
