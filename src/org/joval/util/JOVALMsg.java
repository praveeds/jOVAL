// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import ch.qos.cal10n.BaseName;
import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.LocaleData;
import ch.qos.cal10n.MessageConveyor;
import ch.qos.cal10n.MessageConveyorException;
import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;

/**
 * Uses cal10n to define localized messages for jOVAL.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
@BaseName("jovalmsg")
@LocaleData(
  defaultCharset="ASCII",
  value = { @Locale("en_US")
          }
)
public enum JOVALMsg {
    STATUS_OBJECT,
    STATUS_DEFINITION,
    STATUS_TEST,
    STATUS_CONFIG_OVERLAY,
    STATUS_CONFIG_SESSION,
    STATUS_AUTHMESSAGE,
    STATUS_EMPTY_FILE,
    STATUS_EMPTY_ENTITY,
    STATUS_EMPTY_RECORD,
    STATUS_EMPTY_SET,
    STATUS_NOT_FILE,
    STATUS_NOT_FOUND,
    STATUS_PE_EMPTY,
    STATUS_PE_READ,
    STATUS_VARIABLE_CREATE,
    STATUS_VARIABLE_RECYCLE,
    STATUS_WINCRED_CREATE,
    STATUS_WINREG_CONNECT,
    STATUS_WINREG_DISCONNECT,
    STATUS_WINREG_KEYCLEAN,
    STATUS_WINREG_KEYCLOSED,
    STATUS_WINREG_REDIRECT,
    STATUS_WINREG_KEYDEREG,
    STATUS_WINREG_KEYREG,
    STATUS_WINREG_VALINSTANCE,
    STATUS_WSMV_CONNECT,
    STATUS_WSMV_REQUEST,
    STATUS_WSMV_RESPONSE,
    STATUS_WINSMB_MAP,
    STATUS_FILTER,
    STATUS_SESSION_TYPE,
    STATUS_SESSION_DISPOSE,
    STATUS_WINDOWS_BITNESS,
    STATUS_AIX_FILESET,
    STATUS_AIX_FIX,
    STATUS_RPMINFO_LIST,
    STATUS_RPMINFO_RPM,
    STATUS_SOLPKG_LIST,
    STATUS_SOLPKG_PKGINFO,
    STATUS_CACHE_SEARCH,
    STATUS_CACHE_ACCESS,
    STATUS_TREESEARCH,
    STATUS_FS_PRELOAD_DONE,
    STATUS_FS_PRELOAD_AUTOSTART,
    STATUS_FS_PRELOAD_CACHE_TEMP,
    STATUS_FS_PRELOAD_CACHE_PROGRESS,
    STATUS_FS_PRELOAD_CACHE_CREATE,
    STATUS_FS_PRELOAD_CACHE_EXPIRED,
    STATUS_FS_PRELOAD_CACHE_REUSE,
    STATUS_FS_PRELOAD_CACHE_MISMATCH,
    STATUS_FS_PRELOAD_FILE_PROGRESS,
    STATUS_FS_MOUNT_ADD,
    STATUS_FS_MOUNT_SKIP,
    STATUS_FS_RECURSE,
    STATUS_FS_LOOP,
    STATUS_FS_SEARCH,
    STATUS_FS_SKIP,
    STATUS_SMF,
    STATUS_SMF_SERVICE,
    STATUS_AD_DOMAIN_SKIP,
    STATUS_AD_DOMAIN_ADD,
    STATUS_AD_GROUP_SKIP,
    STATUS_UPN_CONVERT,
    STATUS_NAME_DOMAIN_ERR,
    STATUS_NAME_DOMAIN_OK,
    STATUS_UNIX_FILE,
    STATUS_OFFLINE,
    STATUS_WINREG_REDIRECT_UNSUPPORTED,
    STATUS_SSH_CONNECT,
    STATUS_SSH_DISCONNECT,
    STATUS_SSH_PROCESS_START,
    STATUS_SSH_PROCESS_END,
    STATUS_SSH_SHELL_ATTACH,
    STATUS_SSH_SHELL_PRINTLN,
    STATUS_SSH_SHELL_DETACH,
    STATUS_PROCESS_RETRY,
    STATUS_CREDENTIAL_SET,
    STATUS_WMI_CONNECT,
    STATUS_WMI_QUERY,
    STATUS_WMI_DISCONNECT,
    STATUS_DATECONVERSION,
    STATUS_CHECK_NONE_EXIST,
    STATUS_TREE_MKNODE,
    STATUS_TREE_MKLINK,
    STATUS_NETCONF_SESSIONID,
    STATUS_XCCDF_BENCHMARK,
    STATUS_XCCDF_DICTIONARY,
    STATUS_XCCDF_PLATFORM,
    STATUS_XCCDF_OVAL,
    WARNING_PERISHABLEIO_INTERRUPT,
    WARNING_XCCDF_DICTIONARY,
    WARNING_XCCDF_PLATFORM,
    WARNING_FIELD_STATUS,
    WARNING_MISSING_OUTPUT,
    ERROR_MISSING_RESOURCE,
    ERROR_DIRECTORY,
    ERROR_SESSION_INTEGRITY,
    ERROR_SESSION_NONE,
    ERROR_SESSION_TYPE,
    ERROR_SESSION_CONNECT,
    ERROR_SESSION_NOT_CONNECTED,
    ERROR_SESSION_LOCK,
    ERROR_SESSION_TARGET,
    ERROR_SESSION_CREDENTIAL,
    ERROR_SESSION_CREDENTIAL_PASSWORD,
    ERROR_SESSION_CREDENTIAL_STORE,
    ERROR_ENGINE_STATE,
    ERROR_ENGINE_ABORT,
    ERROR_BAD_COMPONENT,
    ERROR_BAD_TIMEDIFFERENCE,
    ERROR_DEFINITION_NOID,
    ERROR_DEFINITIONS_NONE,
    ERROR_DEFINITIONS_BAD_SOURCE,
    ERROR_DEFINITION_FILTER_BAD_SOURCE,
    ERROR_SCHEMATRON_VALIDATION,
    ERROR_DIRECTIVES_BAD_SOURCE,
    ERROR_CPE_BAD_SOURCE,
    ERROR_XCCDF_BAD_SOURCE,
    ERROR_XCCDF_MISSING_PART,
    ERROR_SCE_PLATFORM,
    ERROR_SCE_PLATFORMLANG,
    ERROR_SCE_RAN,
    ERROR_SCE_NOTRUN,
    ERROR_EOF,
    ERROR_EOS,
    ERROR_EXTERNAL_VARIABLE_SOURCE,
    ERROR_EXTERNAL_VARIABLE,
    ERROR_FILE_GENERATE,
    ERROR_FILE_CLOSE,
    ERROR_FILE_DELETE,
    ERROR_FILEOBJECT_ITEMS,
    ERROR_PE,
    ERROR_PE_STRINGSTR_OVERFLOW,
    ERROR_FS_LOCALPATH,
    ERROR_CACHE_NOT_LINK,
    ERROR_CACHE_IRRETRIEVABLE,
    ERROR_INSTANCE,
    ERROR_IO,
    ERROR_IO_NOT_FILE,
    ERROR_IO_DIR_LISTING,
    ERROR_ADAPTER_MISSING,
    ERROR_ADAPTER_COLLECTION,
    ERROR_MISSING_COMPONENT,
    ERROR_PLUGIN_CLASSPATH,
    ERROR_PLUGIN_CLASSPATH_ELT,
    ERROR_PLUGIN_MAIN,
    ERROR_PLUGIN_INTERFACE,
    ERROR_OBJECT_ITEM_FIELD,
    ERROR_REF_DEFINITION,
    ERROR_REF_ITEM,
    ERROR_REF_OBJECT,
    ERROR_REF_STATE,
    ERROR_REF_TEST,
    ERROR_REF_VARIABLE,
    ERROR_SC_BAD_SOURCE,
    ERROR_RESULTS_BAD_SOURCE,
    ERROR_STATE_BAD,
    ERROR_OBJECT_MISSING,
    ERROR_STATE_MISSING,
    ERROR_VARIABLE_MISSING,
    ERROR_OBJECT_PERMUTATION,
    ERROR_VARIABLE_NO_VALUES,
    ERROR_TEST_NOOBJREF,
    ERROR_TEST_INCOMPARABLE,
    ERROR_FILE_SPEC,
    ERROR_UNIXFILEINFO,
    ERROR_TESTEXCEPTION,
    ERROR_TIMESTAMP,
    ERROR_VARIABLES_BAD_SOURCE,
    ERROR_UNKNOWN_HOST,
    ERROR_UNIX_FLAVOR,
    ERROR_UNSUPPORTED_OS_VERSION,
    ERROR_UNSUPPORTED_SESSION_TYPE,
    ERROR_UNSUPPORTED_UNIX_FLAVOR,
    ERROR_UNSUPPORTED_OBJECT,
    ERROR_UNSUPPORTED_OPERATOR,
    ERROR_UNSUPPORTED_OPERATION,
    ERROR_UNSUPPORTED_CHECK,
    ERROR_UNSUPPORTED_COMPONENT,
    ERROR_UNSUPPORTED_EXISTENCE,
    ERROR_UNSUPPORTED_STATE,
    ERROR_UNSUPPORTED_ENTITY,
    ERROR_UNSUPPORTED_DATATYPE,
    ERROR_UNSUPPORTED_BEHAVIOR,
    ERROR_UNSUPPORTED_ITEM,
    ERROR_FLAG,
    ERROR_OPERATION_DATATYPE,
    ERROR_VERSION_CLASS,
    ERROR_VERSION_STR,
    ERROR_SYSINFO_TYPE,
    ERROR_SYSINFO_ARCH,
    ERROR_SYSINFO_HOSTNAME,
    ERROR_SYSINFO_OSVERSION,
    ERROR_SYSINFO_OSNAME,
    ERROR_SYSINFO_INTERFACE,
    ERROR_WINENV_NONSTR,
    ERROR_WINENV_SYSENV,
    ERROR_WINENV_SYSROOT,
    ERROR_WINENV_PROGRAMFILES,
    ERROR_WINENV_PROGRAMFILESX86,
    ERROR_WINENV_USRENV,
    ERROR_WINENV_VOLENV,
    ERROR_WINFILE_TYPE,
    ERROR_WINFILE_DEVCLASS,
    ERROR_WINFILE_LANGUAGE,
    ERROR_WINFILE_OWNER,
    ERROR_WINDIR_NOPRINCIPAL,
    ERROR_WINPE_BUFFERLEN,
    ERROR_WINPE_ILLEGALSECTION,
    ERROR_WINPE_LOCALERESOURCE,
    ERROR_WINPE_MAGIC,
    ERROR_WINPE_STRSTR0LEN,
    ERROR_WINPE_VSVKEY,
    ERROR_WINREG_CONNECT,
    ERROR_WINREG_CONVERSION,
    ERROR_WINREG_ENUMKEY,
    ERROR_WINREG_ENUMVAL,
    ERROR_WINREG_HIVE,
    ERROR_WINREG_HIVE_NAME,
    ERROR_WINREG_HIVE_OPEN,
    ERROR_WINREG_KEY,
    ERROR_WINREG_KEY_MISSING,
    ERROR_WINREG_SUBKEY_MISSING,
    ERROR_WINREG_KEYCLOSE,
    ERROR_WINREG_ACCESS,
    ERROR_WINREG_MATCH,
    ERROR_WSMV_RESPONSE,
    ERROR_WIN_ACCESSTOKEN_OUTPUT,
    ERROR_WIN_ACCESSTOKEN_TOKEN,
    ERROR_WIN_ACCESSTOKEN_CODE,
    ERROR_WIN_AUDITPOL_SETTING,
    ERROR_WIN_AUDITPOL_SUBCATEGORY,
    ERROR_WIN_AUDITPOL_CODE,
    ERROR_WIN_SECEDIT_VALUE,
    ERROR_WIN_SECEDIT_CODE,
    ERROR_WIN_LOCKOUTPOLICY_OUTPUT,
    ERROR_WIN_LOCKOUTPOLICY_VALUE,
    ERROR_PATTERN,
    ERROR_TREE_ADD,
    ERROR_TREE_NODE,
    ERROR_TREE_MKLINK,
    ERROR_NODE_CHILDREN,
    ERROR_NODE_LINK,
    ERROR_LINK_NOWHERE,
    ERROR_LINK_SELF,
    ERROR_LINK_CYCLE,
    ERROR_NODE_DEPTH,
    ERROR_TREESEARCH,
    ERROR_TREESEARCH_TOKEN,
    ERROR_TREESEARCH_PATH,
    ERROR_WINREG_QUERYVAL,
    ERROR_WINREG_REKEY,
    ERROR_WINREG_STATE,
    ERROR_WINREG_TYPE,
    ERROR_WINREG_VALUE,
    ERROR_WINREG_VALUETOSTR,
    ERROR_WINREG_FLAVOR,
    ERROR_WINREG_DISCONNECT,
    ERROR_WINREG_HEARTBEAT,
    ERROR_WINWMI_GENERAL,
    ERROR_WINWMI_CONNECT,
    ERROR_POWERSHELL,
    ERROR_UNAME_OVERFLOW,
    ERROR_FAMILY_OVERFLOW,
    ERROR_STATE_EMPTY,
    ERROR_NO_ITEMS,
    ERROR_FILE_STREAM_CLOSE,
    ERROR_REFLECTION,
    ERROR_MISSING_PASSWORD,
    ERROR_AUTHENTICATION_FAILED,
    ERROR_DATATYPE_MISMATCH,
    ERROR_RPMINFO,
    ERROR_RPMINFO_SIGKEY,
    ERROR_SELINUX_BOOL,
    ERROR_SELINUX_SC,
    ERROR_SOLPKG,
    ERROR_UNIX_FILE,
    ERROR_ILLEGAL_TIME,
    ERROR_TIME_PARSE,
    ERROR_BAD_FILE_OBJECT,
    ERROR_BAD_PLIST_OBJECT,
    ERROR_BAD_PROCESS58_OBJECT,
    ERROR_SUBSTRING,
    ERROR_SET_COMPLEMENT,
    ERROR_SMF,
    ERROR_XML_XPATH,
    ERROR_XML_PARSE,
    ERROR_XML_TRANSFORM,
    ERROR_PLIST_PARSE,
    ERROR_PLIST_UNSUPPORTED_TYPE,
    ERROR_MOUNT,
    ERROR_PRELOAD,
    ERROR_PRELOAD_LINE,
    ERROR_PRELOAD_OVERFLOW,
    ERROR_RESOLVE_VAR,
    ERROR_RESOLVE_ITEM_FIELD,
    ERROR_FMRI,
    ERROR_AD_DOMAIN_REQUIRED,
    ERROR_AD_DOMAIN_UNKNOWN,
    ERROR_AD_BAD_OU,
    ERROR_AD_INIT,
    ERROR_WMI_STR_CONVERSION,
    ERROR_WMI_PROCESS,
    ERROR_PROCESS_KILL,
    ERROR_PROCESS_RETRY,
    ERROR_PROCESS_CREATE,
    ERROR_SSH_CONNECT,
    ERROR_SSH_DISCONNECTED,
    ERROR_SSH_DROPPED_CONN,
    ERROR_SSH_UNEXPECTED_RESPONSE,
    ERROR_SSH_CHANNEL,
    ERROR_SSH_SHELL_BUSY,
    ERROR_OFFLINE_INPUT,
    ERROR_APPLE_OFFLINE,
    ERROR_IOS_OFFLINE,
    ERROR_IOS_NO_SHOW,
    ERROR_IOS_SHOW,
    ERROR_IOS_SNMP,
    ERROR_IOS_TECH_SHOW,
    ERROR_IOS_TECH_ORPHAN,
    ERROR_JUNOS_OFFLINE,
    ERROR_JUNOS_SUPPORT_ORPHAN,
    ERROR_JUNOS_SHOW,
    ERROR_NETCONF_GETCONFIG,
    ERROR_WINDOWS_BITNESS_INCOMPATIBLE,
    ERROR_COMPONENT_FILTER,
    ERROR_SSH_PROCESS_DESTROY,
    ERROR_READ_TIMEOUT,
    ERROR_TFTP,
    ERROR_PROTOCOL,
    ERROR_ASCII_CONVERSION,
    ERROR_BINARY_LENGTH,
    ERROR_PASSWD_LINE,
    ERROR_SHADOW_LINE,
    ERROR_TYPED_STATUS,
    ERROR_TYPE_CONVERSION,
    ERROR_TYPE_INCOMPATIBLE,
    ERROR_CHECKSUM_ALGORITHM,
    ERROR_EXCEPTION;

    private static IMessageConveyor mc;
    private static LocLoggerFactory loggerFactory;
    private static LocLogger sysLogger;

    static {
	mc = new MessageConveyor(java.util.Locale.getDefault());
	try {
	    //
	    // Get a message to test whether localized messages are available for the default Locale
	    //
	    getMessage(JOVALMsg.ERROR_EXCEPTION);
	} catch (MessageConveyorException e) {
	    //
	    // The test failed, so set the message Locale to English
	    //
	    mc = new MessageConveyor(java.util.Locale.ENGLISH);
	}
	loggerFactory = new LocLoggerFactory(mc);
	sysLogger = loggerFactory.getLocLogger(JOVALMsg.class);
    }

    /**
     * Retrieve the default localized system logger used by the jOVAL library.
     */
    public static LocLogger getLogger() {
	return sysLogger;
    }

    /**
     * Retrieve/create a localized jOVAL logger with a particular name.  This is useful for passing to an IPlugin, if you
     * want all of the plugin's log messages routed to a specific logger.
     */
    public static LocLogger getLogger(String name) {
	return loggerFactory.getLocLogger(name);
    }

    /**
     * Retrieve a localized String, given the key and substitution arguments.
     */
    public static String getMessage(JOVALMsg key, Object... args) {
	return mc.getMessage(key, args);
    }
}
