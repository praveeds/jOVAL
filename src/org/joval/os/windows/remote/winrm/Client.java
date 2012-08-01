// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.Proxy;
import java.net.URL;
import java.util.Properties;
import javax.security.auth.login.LoginException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.dmtf.wsman.AttributableEmpty;
import org.dmtf.wsman.AttributablePositiveInteger;
import org.dmtf.wsman.OptionSet;
import org.dmtf.wsman.OptionType;
import org.dmtf.wsman.SelectorSetType;
import org.dmtf.wsman.SelectorType;
import org.xmlsoap.ws.addressing.EndpointReferenceType;
import org.xmlsoap.ws.addressing.ReferenceParametersType;
import org.xmlsoap.ws.enumeration.Enumerate;
import org.xmlsoap.ws.enumeration.EnumerateResponse;
import org.xmlsoap.ws.enumeration.Pull;
import org.xmlsoap.ws.enumeration.PullResponse;
import org.xmlsoap.ws.eventing.Subscribe;
import org.xmlsoap.ws.eventing.SubscribeResponse;
import org.xmlsoap.ws.transfer.AnyXmlOptionalType;
import org.xmlsoap.ws.transfer.AnyXmlType;
import com.microsoft.wsman.config.ClientDefaultPortsType;
import com.microsoft.wsman.config.ClientAuthType;
import com.microsoft.wsman.config.ClientType;
import com.microsoft.wsman.config.ConfigType;
import com.microsoft.wsman.config.ServiceDefaultPortsType;
import com.microsoft.wsman.config.ServiceAuthType;
import com.microsoft.wsman.config.ServiceType;
import com.microsoft.wsman.config.WinrsType;
import com.microsoft.wsman.shell.ShellType;
import com.microsoft.wsman.shell.EnvironmentVariable;
import com.microsoft.wsman.shell.EnvironmentVariableList;

import org.joval.intf.ws.IPort;
import org.joval.os.windows.remote.winrm.operation.EnumerateOperation;
import org.joval.os.windows.remote.winrm.operation.PullOperation;
import org.joval.os.windows.remote.winrm.operation.CreateOperation;
import org.joval.os.windows.remote.winrm.operation.DeleteOperation;
import org.joval.os.windows.remote.winrm.operation.GetOperation;
import org.joval.os.windows.remote.winrm.operation.PutOperation;
import org.joval.os.windows.remote.winrm.operation.SubscribeOperation;

/**
 * A WinRM client.  To use it, you must first do this on the target machine:
 *
 *   winrm set winrm/config/service @{AllowUnencrypted="true"}
 *   winrm set winrm/config/service/auth @{Basic="true"}
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Client implements IWSMConstants {
    public static void main(String[] argv) {
	String host = argv[0];
	String user = argv[1];
	String pass = argv[2];

	StringBuffer targetSpec = new StringBuffer("http://");
	targetSpec.append(host);
	if (host.indexOf(":") == -1) {
	    targetSpec.append(":");
	    targetSpec.append(Integer.toString(HTTP_PORT));
	}
	targetSpec.append("/");
	targetSpec.append(URL_PREFIX);
	String url = targetSpec.toString();

	try {
	    Client client = new Client(new WSMPort(url, user, pass));

/*
	    client.testGet();
	    client.testEnumerate();
	    client.testPut();
	    client.testDelete();
*/
	    client.testShell();

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private IPort port;

    public Client(IPort port) {
	this.port = port;
    }

    public void testGet() throws Exception {
	AnyXmlOptionalType arg = Factories.TRANSFER.createAnyXmlOptionalType();
	GetOperation operation = new GetOperation(arg);

	operation.addResourceURI("http://schemas.microsoft.com/wbem/wsman/1/config");
	if (operation.dispatch(port) instanceof AnyXmlType) {
	    System.out.println("Get test succeeded");
	} else {
	    System.out.println("Get test failed");
	}

/*
	operation.addResourceURI("http://schemas.microsoft.com/wbem/wsman/1/wmi/root/cimv2/Win32_Processor");
	SelectorSetType set = WSMPort.Factories.WSMAN.createSelectorSetType();
	SelectorType sel = WSMPort.Factories.WSMAN.createSelectorType();
	sel.setName("DeviceID");
	sel.getContent().add("CPU0");
	set.getSelector().add(sel);
	operation.addSelectorSet(set);
	DOMSource ds = new DOMSource((Element)operation.dispatch(port).getAny());
	StreamResult sr = new StreamResult(new OutputStreamWriter(System.out, "utf-8"));
	TransformerFactory tf = TransformerFactory.newInstance();
	tf.setAttribute("indent-number", 4);
	Transformer trans = tf.newTransformer();
	trans.setOutputProperty(OutputKeys.INDENT,"yes");
	trans.transform(ds, sr);
*/
    }

    public void testPut() throws Exception {
	ConfigType config = Factories.WSMC.createConfigType();
	config.setMaxEnvelopeSizekb(50);
	config.setMaxTimeoutms(6000);
	config.setMaxBatchItems(10);
	config.setMaxProviderRequests(4294967295L); // DAS - this is a deprecated setting

	ClientType client = Factories.WSMC.createClientType();
	client.setNetworkDelayms(5000);
	client.setURLPrefix("wsman");
	client.setAllowUnencrypted(true);
	ClientAuthType clientAuth = Factories.WSMC.createClientAuthType();
	clientAuth.setBasic(true);
	clientAuth.setDigest(true);
	clientAuth.setKerberos(true);
	clientAuth.setNegotiate(true);
	clientAuth.setCertificate(true);
	clientAuth.setCredSSP(Boolean.FALSE);
	client.setAuth(clientAuth);
	ClientDefaultPortsType clientDefaultPorts = Factories.WSMC.createClientDefaultPortsType();
	clientDefaultPorts.setHTTP(5985);
	clientDefaultPorts.setHTTPS(5986);
	client.setDefaultPorts(clientDefaultPorts);
	config.setClient(client);

	ServiceType service = Factories.WSMC.createServiceType();
	service.setRootSDDL("O:NSG:BAD:P(A;;GA;;;BA)S:P(AU;FA;GA;;;WD)(AU;SA;GWGX;;;WD)");
	service.setMaxConcurrentOperations(4294967295L);
	service.setMaxConcurrentOperationsPerUser(new Long(15));
	service.setEnumerationTimeoutms(60000);
	service.setMaxConnections(25);
	service.setMaxPacketRetrievalTimeSeconds(new Long(120));
	service.setAllowUnencrypted(true);
	ServiceAuthType serviceAuth = Factories.WSMC.createServiceAuthType();
	serviceAuth.setBasic(true);
	serviceAuth.setKerberos(true);
	serviceAuth.setNegotiate(true);
	serviceAuth.setCertificate(true);
	serviceAuth.setCredSSP(Boolean.FALSE);
	serviceAuth.setCbtHardeningLevel("Relaxed");
	service.setAuth(serviceAuth);
	ServiceDefaultPortsType serviceDefaultPorts = Factories.WSMC.createServiceDefaultPortsType();
	serviceDefaultPorts.setHTTP(5985);
	serviceDefaultPorts.setHTTPS(5986);
	service.setDefaultPorts(serviceDefaultPorts);
	service.setIPv4Filter("*");
	service.setIPv6Filter("*");
	service.setEnableCompatibilityHttpListener(false);
	service.setEnableCompatibilityHttpsListener(false);
	config.setService(service);

	WinrsType winrs = Factories.WSMC.createWinrsType();
	winrs.setAllowRemoteShellAccess(true);
	winrs.setIdleTimeout(new BigInteger("180000"));
	winrs.setMaxConcurrentUsers(5);
	winrs.setMaxProcessesPerShell(new BigInteger("15"));
	winrs.setMaxMemoryPerShellMB(new BigInteger("150"));
	winrs.setMaxShellsPerUser(new BigInteger("5"));
	config.setWinrs(winrs);

	AnyXmlType arg = Factories.TRANSFER.createAnyXmlType();
	arg.setAny(Factories.WSMC.createConfig(config));
	PutOperation operation = new PutOperation(arg);
	operation.addResourceURI("http://schemas.microsoft.com/wbem/wsman/1/config");
	if (operation.dispatch(port) instanceof AnyXmlOptionalType) {
	    System.out.println("Put test succeeded");
	} else {
	    System.out.println("Put test failed");
	}
    }

    public void testDelete() throws Exception {
	DeleteOperation operation = new DeleteOperation();
	operation.addResourceURI("http://schemas.microsoft.com/wbem/wsman/1/config/service/certmapping");
	//DAS
	SelectorSetType set = WSMPort.Factories.WSMAN.createSelectorSetType();
	SelectorType sel1 = WSMPort.Factories.WSMAN.createSelectorType();
	sel1.setName("Issuer");
	sel1.getContent().add("1212131238d84023982e381f2");
	set.getSelector().add(sel1);
	SelectorType sel2 = WSMPort.Factories.WSMAN.createSelectorType();
	sel2.setName("Subject");
	sel2.getContent().add("*.sampl.com");
	set.getSelector().add(sel2);
	SelectorType sel3 = WSMPort.Factories.WSMAN.createSelectorType();
	sel3.setName("URI");
	sel3.getContent().add("http://schemas.microsoft.com/wbem/wsman/1/wmi/root/cimv2/*");
	set.getSelector().add(sel3);
	operation.addSelectorSet(set);
	operation.setTimeout(60000);
	if (operation.dispatch(port) == null) {
	    System.out.println("Delete test succeeded");
	} else {
	    System.out.println("Delete test failed");
	}
    }

    public void testEnumerate() throws Exception {
	Enumerate enumerate = WSMPort.Factories.ENUMERATION.createEnumerate();
	AttributableEmpty optimize = WSMPort.Factories.WSMAN.createAttributableEmpty();
	enumerate.getAny().add(WSMPort.Factories.WSMAN.createOptimizeEnumeration(optimize));
	AttributablePositiveInteger maxElements = WSMPort.Factories.WSMAN.createAttributablePositiveInteger();
	maxElements.setValue(new BigInteger("1"));
	enumerate.getAny().add(WSMPort.Factories.WSMAN.createMaxElements(maxElements));
	EnumerateOperation operation = new EnumerateOperation(new Enumerate());
	operation.addResourceURI("http://schemas.microsoft.com/wbem/wsman/1/wmi/root/cimv2/Win32_Processor");
//	operation.addResourceURI("http://schemas.microsoft.com/wbem/wsman/1/config/service/certmapping");
	EnumerateResponse response = operation.dispatch(port);



	Pull pull = WSMPort.Factories.ENUMERATION.createPull();
	pull.setEnumerationContext(response.getEnumerationContext());
	pull.setMaxElements(new BigInteger("1"));
	PullOperation pullOperation = new PullOperation(pull);
	pullOperation.addResourceURI("http://schemas.microsoft.com/wbem/wsman/1/wmi/root/cimv2/Win32_Processor");
//	pullOperation.addResourceURI("http://schemas.microsoft.com/wbem/wsman/1/config/service/certmapping");

	boolean endOfSequence = false;
	while(!endOfSequence) {
	    PullResponse pullResponse = pullOperation.dispatch(port);
	    endOfSequence = pullResponse.isSetEndOfSequence();
	}
    }

    public void testSubscribe() throws Exception {
	Subscribe subscribe = WSMPort.Factories.EVENTING.createSubscribe();

	SubscribeOperation operation = new SubscribeOperation(subscribe);
	operation.addResourceURI("http://schemas.microsoft.com/wbem/wsman/1/windows/EventLog");

	OptionSet options = Factories.WSMAN.createOptionSet();
	OptionType option1 = Factories.WSMAN.createOptionType();
	option1.setName("Compression");
	option1.setValue("SLDC");
	options.getOption().add(option1);
	OptionType option2 = Factories.WSMAN.createOptionType();
	option2.setName("CDATA");
	option2.getOtherAttributes().put(NIL, "true");
	options.getOption().add(option2);
	OptionType option3 = Factories.WSMAN.createOptionType();
	option3.setName("ContentFormat");
	option3.setValue("RenderedText");
	options.getOption().add(option3);
	OptionType option4 = Factories.WSMAN.createOptionType();
	option4.setName("IgnoreChannelError");
	option4.getOtherAttributes().put(NIL, "true");
	options.getOption().add(option4);
	operation.addOptionSet(options);

	SubscribeResponse response = operation.dispatch(port);
    }

    public void testShell() throws Exception {
	String uri = "http://schemas.microsoft.com/wbem/wsman/1/windows/shell/cmd";
	AnyXmlType arg = Factories.TRANSFER.createAnyXmlType();
	ShellType shell = Factories.SHELL.createShellType();
	EnvironmentVariableList env = Factories.SHELL.createEnvironmentVariableList();
	EnvironmentVariable var = Factories.SHELL.createEnvironmentVariable();
	var.setName("test");
	var.setValue("1");
	env.getVariable().add(var);
	shell.setEnvironment(env);
	shell.setWorkingDirectory("%windir%");
//	shell.setLifetime(Factories.XMLDT.newDuration(1000000));
	shell.getOutputStreams().add("stdout");
	shell.getOutputStreams().add("stderr");
	shell.getInputStreams().add("stdin");
	arg.setAny(Factories.SHELL.createShell(shell));
	CreateOperation createOperation = new CreateOperation(arg);
	createOperation.addResourceURI(uri);
	createOperation.setTimeout(60000);

	OptionSet options = Factories.WSMAN.createOptionSet();
	OptionType option1 = Factories.WSMAN.createOptionType();
	option1.setName("WINRS_NOPROFILE");
	option1.setValue("TRUE");
	options.getOption().add(option1);
	OptionType option2 = Factories.WSMAN.createOptionType();
	option2.setName("WINRS_CODEPAGE");
	option2.setValue("437");
	options.getOption().add(option2);
	createOperation.addOptionSet(options);
	Object response = createOperation.dispatch(port);
	String shellId = null;
	if (response instanceof EndpointReferenceType) {
	    for (Object param : ((EndpointReferenceType)response).getReferenceParameters().getAny()) {
		if (param instanceof JAXBElement) {
		    param = ((JAXBElement)param).getValue();
		}
		if (param instanceof SelectorSetType) {
		    for (SelectorType sel : ((SelectorSetType)param).getSelector()) {
			if ("ShellId".equals(sel.getName())) {
			    shellId = (String)sel.getContent().get(0);
			    break;
			} else {
			    System.out.println("selector name is " + sel.getName());
			}
		   }
		} else {
		    System.out.println("param is " + param.getClass().getName());
		}
		if (shellId != null) break;
	    }
	} else {
	    System.out.println("response is " + response.getClass().getName());
	}
	if (shellId != null) {
	    System.out.println("Created shell " + shellId);

	    // do stuff in the shell

	    // close/delete the shell
	    DeleteOperation deleteOperation = new DeleteOperation();
	    deleteOperation.addResourceURI(uri);
	    SelectorSetType set = WSMPort.Factories.WSMAN.createSelectorSetType();
	    SelectorType sel = WSMPort.Factories.WSMAN.createSelectorType();
	    sel.setName("ShellId");
	    sel.getContent().add(shellId);
	    set.getSelector().add(sel);
	    deleteOperation.addSelectorSet(set);
	    if (deleteOperation.dispatch(port) == null) {
		System.out.println("Deleted shell " + shellId);
	    }
	}
    }
}