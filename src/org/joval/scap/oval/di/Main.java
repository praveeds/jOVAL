// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.di;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import org.xml.sax.SAXException;

import jsaf.util.Checksum;

import scap.oval.definitions.core.OvalDefinitions;
import scap.oval.systemcharacteristics.core.OvalSystemCharacteristics;
import scap.oval.results.DefinitionType;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.oval.IDefinitionFilter;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.oval.IOvalEngine;
import org.joval.intf.scap.oval.IResults;
import org.joval.intf.scap.oval.ISystemCharacteristics;
import org.joval.intf.scap.oval.IVariables;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.OvalFactory;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.LogFormatter;
import org.joval.util.Version;
import org.joval.xml.SchemaValidator;
import org.joval.xml.XSLTools;
import org.joval.xml.schematron.ValidationException;
import org.joval.xml.schematron.Validator;

/**
 * Command-Line Interface main class, whose purpose is to replicate the CLI of Ovaldi (the MITRE OVAL Definition
 * Interpreter).
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Main implements IObserver<IOvalEngine.Message> {
    private static final String LF			= System.getProperty("line.separator");
    private static final String JAVA_VERSION		= System.getProperty("java.specification.version");
    private static final String MIN_JAVA_VERSION	= "1.5";

    private static ExecutionState state = null;
    private static String lastStatus = null;

    private static Logger logger;
    private static PropertyResourceBundle resources;
    static {
	try {
	    LogManager.getLogManager().readConfiguration(new ByteArrayInputStream("java.util.logging.handlers=".getBytes()));
	    logger = Logger.getLogger("jovaldi");

	    Locale locale = Locale.getDefault();
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    URL url = cl.getResource("jovaldi.resources_" + locale.toString() + ".properties");
	    if (url == null) {
		url = cl.getResource("jovaldi.resources_" + locale.getLanguage() + ".properties");
	    }
	    if (url == null) {
		url = cl.getResource("jovaldi.resources.properties");
	    }
	    resources = new PropertyResourceBundle(url.openStream());
	    JOVALSystem.setSystemProperty(JOVALSystem.SYSTEM_PROP_PRODUCT, getMessage("product.name"));
	} catch (IOException e) {
	    e.printStackTrace();
	    System.exit(-1);
	}
    }

    /**
     * Definition Interpreter application entry-point.
     */
    public static void main (String[] argv) {
	if (new Version(JAVA_VERSION).compareTo(new Version(MIN_JAVA_VERSION)) < 0) {
	    print(getMessage("ERROR_JAVAVERSION", JAVA_VERSION, MIN_JAVA_VERSION));
	    System.exit(ERR);
	} else {
	    state = new ExecutionState();
	    if (state.processArguments(argv)) {
		Main main = new Main();
		printHeader();
		if (state.printHelp) {
		    printHelp();
		    System.exit(OK);
		} else if (state.processPluginArguments()) {
		    if (OK == main.exec()) {
			System.exit(OK);
		    } else {
			System.exit(ERR);
		    }
		} else {
		    printPluginHelp();
		    System.exit(ERR);
		}
	    } else {
		printHeader();
		printHelp();
		System.exit(ERR);
	    }
	}
    }

    static void configureLogging(File logfile, Level logLevel) {
	try {
	    Logger jSysLogger = Logger.getLogger(JOVALMsg.getLogger().getName());
	    Handler logHandler = new FileHandler(logfile.getPath(), false);
	    logHandler.setFormatter(new LogfileFormatter());
	    logHandler.setLevel(logLevel);
	    logger.setLevel(logLevel);
	    logger.addHandler(logHandler);
	    jSysLogger.setLevel(state.logLevel);
	    jSysLogger.addHandler(logHandler);
	    if (state.printLogs) {
		Handler consoleHandler = new ConsoleHandler();
		consoleHandler.setFormatter(new ConsoleFormatter());
		consoleHandler.setLevel(logLevel);
		logger.addHandler(consoleHandler);
		jSysLogger.addHandler(consoleHandler);
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    // Internal

    /**
     * Print to both the log and the console.
     */
    static void print(String format, Object... args) {
	String s = String.format(format, args).toString();
	logger.log(Level.INFO, s);
	if (!state.printLogs) {
	    System.out.println(s);
	}
    }

    static void logException(Throwable thrown) {
	logger.log(Level.WARNING, getMessage("ERROR_FATAL"), thrown);
	if (thrown.getCause() != null) {
	    logException(thrown.getCause());
	}
    }

    /**
     * Retrieve a message using its key.
     */
    static String getMessage(String key, Object... arguments) {
	return MessageFormat.format(resources.getString(key), arguments);
    }

    // Private

    /**
     * Proceed to the next line on the console.
     */
    private static void clearStatus() {
	System.out.println("");
	lastStatus = null;
    }

    /**
     * Print to the console only (not the log).  Repeated calls to printStatus will all appear on a single line in the console,
     * over-writing the status message that was previously shown.  (Actually, it only over-writes the characters that differ
     * from the last status message, for a more fluid appearance when changes are very rapid).
     */
    private static void printStatus(String format, Object... args) {
	String s = String.format(format, args).toString();
	int offset=0;
	if (lastStatus != null) {
	    int len = lastStatus.length();
	    int n = Math.min(len, s.length());
	    for (int i=0; i < n; i++) {
		if (s.charAt(i) == lastStatus.charAt(i)) {
		    offset++;
		}
	    }
	    StringBuffer back = new StringBuffer();
	    StringBuffer clean = new StringBuffer();
	    int toClear = len - offset;
	    for (int i=0; i < toClear; i++) {
		back.append('\b');
		clean.append(' ');
	    }
	    System.out.print(back.toString());
	    System.out.print(clean.toString());
	    System.out.print(back.toString());
	}
	System.out.print(s.substring(offset));
	lastStatus = s;
    }

    /**
     * Print help text to the console.
     */
    private static void printHelp() {
	print("");
	print(getMessage("MESSAGE_HELPTEXT"));
	printPluginHelp();
    }

    /**
     * Print the plugin's help text to the console.
     */
    private static void printPluginHelp() {
	if (state.plugin != null) {
	    System.out.println(state.plugin.getProperty(IPlugin.PROP_HELPTEXT));
	}
    }

    /**
     * Print program information.
     */
    private static void printHeader() {
	print("");
	print(getMessage("MESSAGE_DIVIDER"));
	print(getMessage("MESSAGE_PRODUCT", JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_PRODUCT)));
	print(getMessage("MESSAGE_VERSION", JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_VERSION)));
	print(getMessage("MESSAGE_BUILD_DATE", JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_BUILD_DATE)));
	print(getMessage("MESSAGE_COPYRIGHT"));
	if (state.plugin != null) {
	    print("");
	    print(getMessage("MESSAGE_PLUGIN_NAME", state.plugin.getProperty(IPlugin.PROP_DESCRIPTION)));
	    print(getMessage("MESSAGE_PLUGIN_VERSION", state.plugin.getProperty(IPlugin.PROP_VERSION)));
	    print(getMessage("MESSAGE_PLUGIN_COPYRIGHT", state.plugin.getProperty(IPlugin.PROP_COPYRIGHT)));
	}
	print(getMessage("MESSAGE_DIVIDER"));
	print("");
	print(getMessage("MESSAGE_START_TIME", new Date()));
	print("");
    }

    // Implement IObserver<IOvalEngine.Message>

    public void notify(IProducer<IOvalEngine.Message> source, IOvalEngine.Message msg, Object arg) {
	switch(msg) {
	  case OBJECT_PHASE_START:
	    print(getMessage("MESSAGE_OBJECT_PHASE"));
	    break;

	  case OBJECT:
	    printStatus(getMessage("MESSAGE_OBJECT", (String)arg));
	    logger.log(Level.INFO, getMessage("MESSAGE_OBJECT_LOG", (String)arg).trim());
	    break;

	  case OBJECTS:
	    String[] args = (String[])arg;
	    printStatus(getMessage("MESSAGE_OBJECTS", args.length));
	    for (String s : args) {
		logger.log(Level.INFO, getMessage("MESSAGE_OBJECT_LOG", s.trim()));
	    }
	    break;

	  case OBJECT_PHASE_END:
	    printStatus(getMessage("MESSAGE_OBJECTS_DONE"));
	    clearStatus();
	    break;

	  case DEFINITION_PHASE_START:
	    print(getMessage("MESSAGE_DEFINITION_PHASE"));
	    break;

	  case DEFINITION:
	    printStatus(getMessage("MESSAGE_DEFINITION", (String)arg));
	    break;

	  case DEFINITION_PHASE_END:
	    printStatus(getMessage("MESSAGE_DEFINITIONS_DONE"));
	    clearStatus();
	    break;

	  case SYSTEMCHARACTERISTICS: {
	    ISystemCharacteristics sc = (ISystemCharacteristics)arg;
	    print(getMessage("MESSAGE_SAVING_SYSTEMCHARACTERISTICS", state.getPath(state.dataFile)));
	    sc.writeXML(state.dataFile);
	    if (state.schematronSC) {
		try {
		    print(getMessage("MESSAGE_RUNNING_XMLVALIDATION", state.getPath(state.dataFile)));
		    if (!validateSchema(state.dataFile, SystemCharacteristicsSchemaFilter.list())) {
			state.plugin.disconnect();
			state.plugin.dispose();
			System.exit(ERR);
		    }
		    print(getMessage("MESSAGE_RUNNING_SCHEMATRON", state.getPath(state.dataFile)));
		    OvalSystemCharacteristics osc = sc.getOvalSystemCharacteristics(false);
		    new Validator(state.getSCSchematron()).validate(sc.getSource());
		    print(getMessage("MESSAGE_SCHEMATRON_SUCCESS"));
		} catch (ValidationException e) {
		    List<String> errors = e.getErrors();
		    if (errors == null) {
			print(e.getMessage());
			logger.log(Level.SEVERE, e.getMessage(), e);
		    } else {
			for (int i=0; i < errors.size(); i++) {
			    if (i == 0) {
				print(getMessage("ERROR_SCHEMATRON", new Integer(errors.size()), errors.get(i)));
			    } else {
				logger.log(Level.SEVERE, getMessage("ERROR_SCHEMATRON_ERROR", i, errors.get(i)));
			    }
			}
		    }
		    state.plugin.disconnect();
		    state.plugin.dispose();
		    System.exit(ERR);
		} catch (Exception e) {
		    logger.log(Level.WARNING, e.getMessage(), e);
		}
	    } else {
		print(getMessage("MESSAGE_SKIPPING_SCHEMATRON"));
	    }
	    break;
	  }
	}
    }

    // Private

    private static int OK	= 0;
    private static int ERR	= 1;

    private Main() {
    }

    /**
     * Execute.
     */
    private int exec() {
	try {
	    if (state.computeChecksum) {
		print("");
		print(Checksum.getChecksum(state.defsFile, Checksum.Algorithm.MD5));
		return OK;
	    } else if (state.validateChecksum) {
		print(" ** verifying the MD5 hash of '" + state.getPath(state.defsFile) + "' file");
		String checksum = Checksum.getChecksum(state.defsFile, Checksum.Algorithm.MD5);
		if (!state.specifiedChecksum.equals(checksum)) {
		    print(getMessage("ERROR_CHECKSUM_MISMATCH", state.getPath(state.defsFile)));
		    return ERR;
		}
	    }

	    print(getMessage("MESSAGE_PARSING_FILE", state.getPath(state.defsFile)));
	    IDefinitions defs = OvalFactory.createDefinitions(state.defsFile);

	    print(getMessage("MESSAGE_VALIDATING_XML"));
	    if (!validateSchema(state.defsFile, DefinitionsSchemaFilter.list())) {
		return ERR;
	    }

	    print(getMessage("MESSAGE_SCHEMA_VERSION_CHECK"));
	    Version schemaVersion = new Version(defs.getOvalDefinitions().getGenerator().getSchemaVersion());
	    print(getMessage("MESSAGE_SCHEMA_VERSION", schemaVersion.toString()));
	    if (IOvalEngine.SCHEMA_VERSION.compareTo(schemaVersion) < 0) {
		print(getMessage("ERROR_SCHEMA_VERSION", schemaVersion.toString()));
		return ERR;
	    }

	    if (state.schematronDefs) {
		print(getMessage("MESSAGE_RUNNING_SCHEMATRON", state.getPath(state.defsFile)));
		try {
		    new Validator(state.getDefsSchematron()).validate(defs.getSource());
		    print(getMessage("MESSAGE_SCHEMATRON_SUCCESS"));
		} catch (ValidationException e) {
		    List<String> errors = e.getErrors();
		    if (errors == null) {
			print(e.getMessage());
			logger.log(Level.SEVERE, e.getMessage(), e);
		    } else {
			for (int i=0; i < errors.size(); i++) {
			    if (i == 0) {
				print(getMessage("ERROR_SCHEMATRON", new Integer(errors.size()), errors.get(i)));
			    } else {
				logger.log(Level.SEVERE, getMessage("ERROR_SCHEMATRON", errors.get(i)));
			    }
			}
		    }
		    return ERR;
		} catch (Exception e) {
		    logger.log(Level.WARNING, e.getMessage(), e);
		}
	    } else {
		print(getMessage("MESSAGE_SKIPPING_SCHEMATRON"));
	    }

	    IDefinitionFilter filter = null;
	    ISystemCharacteristics sc = null;
	    IVariables variables = null;

	    if (state.inputFile == null) {
		print(getMessage("MESSAGE_CREATING_SYSTEMCHARACTERISTICS"));
	    } else {
		print(" ** parsing " + state.getPath(state.inputFile) + " for analysis.");
		print(getMessage("MESSAGE_VALIDATING_XML"));
		if (validateSchema(state.inputFile, SystemCharacteristicsSchemaFilter.list())) {
		    sc = OvalFactory.createSystemCharacteristics(state.inputFile);
		} else {
		    return ERR;
		}
	    }
	    if (state.variablesFile.exists() && state.variablesFile.isFile()) {
		variables = OvalFactory.createVariables(state.variablesFile);
	    }
	    if (state.inputDefsFile != null) {
		print(getMessage("MESSAGE_READING_INPUTDEFINITIONS", state.inputDefsFile));
		filter = OvalFactory.createDefinitionFilter(state.inputDefsFile);
	    } else if (state.definitionIDs != null) {
		print(getMessage("MESSAGE_PARSING_INPUTDEFINITIONS"));
		filter = OvalFactory.createDefinitionFilter(state.definitionIDs);
	    }

	    IOvalEngine engine = OvalFactory.createEngine(IOvalEngine.Mode.EXHAUSTIVE, state.plugin);
	    engine.setDefinitions(defs);

	    if (filter != null) {
		engine.setDefinitionFilter(filter);
	    }
	    if (sc != null) {
		engine.setSystemCharacteristics(sc);
	    }
	    if (variables != null) {
		engine.setExternalVariables(variables);
	    }
	    engine.getNotificationProducer().addObserver(this);
	    engine.run();
	    switch(engine.getResult()) {
	      case ERR:
		throw engine.getError();
	    }
	    if (state.plugin != null) {
		state.plugin.dispose();
	    }

	    IResults results = engine.getResults();
	    if (state.directivesFile.exists() && state.directivesFile.isFile()) {
		print(getMessage("MESSAGE_APPLYING_DIRECTIVES"));
		results.setDirectives(state.directivesFile);
	    }
	    print(getMessage("MESSAGE_RESULTS"));
	    print("");
	    print(getMessage("MESSAGE_DEFINITION_TABLE_HEAD"));
	    print(getMessage("MESSAGE_DEFINITION_TABLE_DIV"));

	    TreeSet<DefinitionType> ordered = new TreeSet<DefinitionType>(
		new Comparator<DefinitionType>() {
		    public int compare(DefinitionType o1, DefinitionType o2) {
			String o1s = new StringBuffer(o1.getResult().toString()).append(o1.getDefinitionId()).toString();
			String o2s = new StringBuffer(o2.getResult().toString()).append(o2.getDefinitionId()).toString();
			return o1s.compareTo(o2s);
		    }
		}
	    );
	    ordered.addAll(results.getOvalResults().getResults().getSystem().get(0).getDefinitions().getDefinition());
	    for (DefinitionType d : ordered) {
		String id = d.getDefinitionId();
		String result = d.getResult().toString().toLowerCase();
		print(getMessage("MESSAGE_DEFINITION_TABLE_ROW", String.format("%-40s", id), result));
	    }
	    print(getMessage("MESSAGE_DEFINITION_TABLE_DIV"));
	    print("");
	    print("");
	    print(getMessage("MESSAGE_DEFINITIONS_EVALUATED"));
	    print("");
	    print(getMessage("MESSAGE_SAVING_RESULTS", state.getPath(state.resultsXML)));
	    results.writeXML(state.resultsXML);
	    if (state.schematronResults) {
		try {
		    print(getMessage("MESSAGE_RUNNING_XMLVALIDATION", state.getPath(state.resultsXML)));
		    if (!validateSchema(state.dataFile, SystemCharacteristicsSchemaFilter.list())) {
			return ERR;
		    }
		    print(getMessage("MESSAGE_RUNNING_SCHEMATRON", state.getPath(state.resultsXML)));
		    new Validator(state.getResultsSchematron()).validate(results.getSource());
		    print(getMessage("MESSAGE_SCHEMATRON_SUCCESS"));
		} catch (ValidationException e) {
		    List<String> errors = e.getErrors();
		    if (errors == null) {
			print(e.getMessage());
			logger.log(Level.SEVERE, e.getMessage(), e);
		    } else {
			for (int i=0; i < errors.size(); i++) {
			    if (i == 0) {
				print(getMessage("ERROR_SCHEMATRON", new Integer(errors.size()), errors.get(i)));
			    } else {
				logger.log(Level.SEVERE, getMessage("ERROR_SCHEMATRON", errors.get(i)));
			    }
			}
		    }
		    return ERR;
		} catch (Exception e) {
		    logger.log(Level.WARNING, e.getMessage(), e);
		}
	    } else {
		print(getMessage("MESSAGE_SKIPPING_SCHEMATRON"));
	    }
	    if (state.applyTransform) {
		print(getMessage("MESSAGE_RUNNING_TRANSFORM", state.getPath(state.getXMLTransform())));
		try {
		    FileInputStream fin = new FileInputStream(state.getXMLTransform());
		    results.writeTransform(XSLTools.getTransformer(fin), state.resultsHTML);
		} catch (Exception e) {
		    logger.log(Level.WARNING, e.getMessage(), e);
		}
	    } else {
		print(getMessage("MESSAGE_SKIPPING_TRANSFORM"));
	    }
	    print("");
	    print(getMessage("MESSAGE_DIVIDER"));
	    return OK;
	} catch (UnknownHostException e) {
	    print("");
	    print("");
	    print(getMessage("ERROR_UNKNOWN_HOST", e.getMessage()));
	    print("");
	    return ERR;
	} catch (ConnectException e) {
	    print("");
	    print("");
	    print(getMessage("ERROR_CONNECT", e.getMessage()));
	    print("");
	    return ERR;
	} catch (OvalException e) {
	    print("");
	    print("");
	    print(getMessage("ERROR_OVAL"));
	    print("");
	    print(LogFormatter.toString(e));
	    return ERR;
	} catch (Exception e) {
	    print("");
	    print("");
	    print(getMessage("ERROR_FATAL"));
	    print(LogFormatter.toString(e));
	    return ERR;
	}
    }

    private boolean validateSchema(File f, File[] schemas) throws SAXException, IOException {
	SchemaValidator validator = new SchemaValidator(schemas);
	try {
	    validator.validate(f);
	    return true;
	} catch (Exception e) {
	    print(getMessage("ERROR_VALIDATION", e.getMessage()));
	    return false;
	}
    }

    private static class ConsoleFormatter extends Formatter {
	public String format(LogRecord record) {
	    StringBuffer line = new StringBuffer(record.getMessage());
	    line.append(LF);
	    return line.toString();
	}
    }

    private static class LogfileFormatter extends Formatter {
	public String format(LogRecord record) {
	    StringBuffer line = new StringBuffer(record.getMessage());
	    line.append(LF);
	    Throwable thrown = record.getThrown();
	    if (thrown != null) {
		line.append(thrown.toString());
		line.append(LF);
		StackTraceElement[] ste = thrown.getStackTrace();
		for (int i=0; i < ste.length; i++) {
		    line.append("    at ");
		    line.append(ste[i].getClassName());
		    line.append(".");
		    line.append(ste[i].getMethodName());
		    line.append(", ");
		    line.append(ste[i].getFileName());
		    line.append(" line: ");
		    line.append(Integer.toString(ste[i].getLineNumber()));
		    line.append(LF);
		}
	    }
	    return line.toString();
	}
    }


    static final String SIGNATURE_SCHEMA = "xmldsig-core-schema.xsd";
    static final String DEFINITION_SCHEMA = "-definitions-schema.xsd";
    static final String SYSTEMCHARACTERISTICS_SCHEMA = "-system-characteristics-schema.xsd";

    private static class DefinitionsSchemaFilter implements FilenameFilter {
	static File[] list() {
	    File ovalDir = new File(state.xmlDir, "oval-" + IOvalEngine.SCHEMA_VERSION.toString());
	    return ovalDir.listFiles(new DefinitionsSchemaFilter());
	}

	DefinitionsSchemaFilter() {}

	public boolean accept(File dir, String fname) {
	    return fname.equals(SIGNATURE_SCHEMA) || fname.endsWith(DEFINITION_SCHEMA);
	}
    }

    private static class SystemCharacteristicsSchemaFilter implements FilenameFilter {
	static File[] list() {
	    File ovalDir = new File(state.xmlDir, "oval-" + IOvalEngine.SCHEMA_VERSION.toString());
	    return ovalDir.listFiles(new SystemCharacteristicsSchemaFilter());
	}

	SystemCharacteristicsSchemaFilter() {}

	public boolean accept(File dir, String fname) {
	    return fname.equals(SIGNATURE_SCHEMA) || fname.endsWith(SYSTEMCHARACTERISTICS_SCHEMA);
	}
    }
}
