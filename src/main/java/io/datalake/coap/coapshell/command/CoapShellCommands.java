/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datalake.coap.coapshell.command;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.datalake.coap.coapshell.provider.ContentTypeValueProvider;
import io.datalake.coap.coapshell.provider.DiscoveryQueryValueProvider;
import io.datalake.coap.coapshell.provider.UriPathValueProvider;
import io.datalake.coap.coapshell.util.CoapDtlsSupport;
import io.datalake.coap.coapshell.util.PrintUtils;
import io.datalake.coap.coapshell.util.Row;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.elements.util.StringUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.jline.terminal.Terminal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import static io.datalake.coap.coapshell.util.PrintUtils.cyan;
import static io.datalake.coap.coapshell.util.PrintUtils.green;
import static io.datalake.coap.coapshell.util.PrintUtils.normal;
import static io.datalake.coap.coapshell.util.PrintUtils.red;

/**
 * @author Christian Tzolov
 */
@ShellComponent
@ShellCommandGroup(CoapShellCommands.SHELL_COAP_REST_COMMANDS_GROUP)
public class CoapShellCommands implements ApplicationEventPublisherAware {

	public static final String COAP_TEXT_PLAIN = "" + MediaTypeRegistry.TEXT_PLAIN;
	public static final String SHELL_CONNECTIVITY_GROUP = "Server Connectivity";
	public static final String SHELL_COAP_REST_COMMANDS_GROUP = "CoAP Commands";
	public static final String COAP_SHELL_CONFIGURATION_GROUP = "CoAP Configuration";
	public static final int DEFAULT_TCP_CONNECTION_IDLE_TIMEOUT = 60 * 10; // 10 min [sec]

	private CoapClient coapClient;
	private ApplicationEventPublisher eventPublisher;
	private CoapObserveRelation observeRelation;
	private String observeURI;

	private StringBuilder observerResponses = new StringBuilder();

	@Autowired
	private UriPathValueProvider coapUriPathValueProvider;

	public enum RequestMode {con, non}

	private RequestMode requestMessageType = RequestMode.con;

	@Autowired
	private CoapDtlsSupport dtsl;

	@Autowired
	@Lazy
	private Terminal terminal;

	@ShellMethod(value = "Connect to CoAP server", group = SHELL_CONNECTIVITY_GROUP)
	public String connect(
			@ShellOption(help = "URI of the server to connect to") URI uri,
			@ShellOption(defaultValue = ShellOption.NULL, help = "pre-shared key identity") String identity,
			@ShellOption(defaultValue = ShellOption.NULL, help = "pre-shared key secret") String secret) {

		Assert.notNull(uri, "Null or invalid URI");
		Assert.hasText(uri.getScheme(), "Missing URI schema. Valid CoAP URI requires either coap:// or coaps:// schema");
		Assert.isTrue(uri.getScheme().equalsIgnoreCase("coap")
				|| uri.getScheme().equalsIgnoreCase("coaps"), String.format("Invalid CoAP URI schema [%s]. " +
				"Valid CoAP URI requires either coap:// or coaps:// schema", uri.getScheme()));

		if (this.availabilityCheck().isAvailable()) {
			this.shutdown();
		}
		this.observeStop();

		this.coapClient = new CoapClient(uri);

		if (uri.getScheme().equalsIgnoreCase("coaps")
				|| StringUtils.hasText(secret) || StringUtils.hasText(identity)) {
			DTLSConnector dtlsConnector = dtsl.createConnector(identity, secret);

			CoapEndpoint coapEndpoint = new CoapEndpoint.CoapEndpointBuilder()
					.setNetworkConfig(NetworkConfig.getStandard().set(
							NetworkConfig.Keys.TCP_CONNECTION_IDLE_TIMEOUT, DEFAULT_TCP_CONNECTION_IDLE_TIMEOUT))
					.setConnector(dtlsConnector).build();

			this.coapClient.setEndpoint(coapEndpoint);
		}
		this.requestMessageType = RequestMode.con;
		String resourceUri = this.coapClient.getURI();
		if (resourceUri != null) {
			this.eventPublisher.publishEvent(resourceUri + "[" + this.requestMessageType.name().toUpperCase() + "]");
		}
		return "Connected to [" + resourceUri + "]";
	}

	@ShellMethod(value = "Check CoAP resources availability", group = SHELL_CONNECTIVITY_GROUP)
	@ShellMethodAvailability({ "availabilityCheck" })
	public String ping(
			@ShellOption(defaultValue = "/", help = "URI path", valueProvider = UriPathValueProvider.class) String path) {

		String before = this.coapClient.getURI();
		try {
			this.coapClient.setURI(this.coapClient.getURI() + path);
			boolean available = this.coapClient.ping(5000);
			return (available) ? green("available") : red("not available");
		}
		finally {
			this.coapClient.setURI(before);
		}
	}

	@ShellMethod(value = "Disconnect CoAP client", group = SHELL_CONNECTIVITY_GROUP)
	@ShellMethodAvailability("availabilityCheck")
	public String shutdown() {
		if (observerAvailabilityCheck().isAvailable() && this.observeRelation != null) {
			this.observeRelation.proactiveCancel();
		}
		if (this.coapClient.getEndpoint() != null) {
			this.coapClient.getEndpoint().destroy();
		}
		this.coapClient.shutdown();
		this.coapUriPathValueProvider.updateHints(new ArrayList<>());
		this.coapClient = null;
		this.eventPublisher.publishEvent("");
		return "Client disconnected!";
	}

	@ShellMethod("List available resources")
	@ShellMethodAvailability("availabilityCheck")
	public Table discover(
			@ShellOption(defaultValue = ShellOption.NULL, help = "discover query (e.g 'href=*', 'ct=40', 'obs' and ect. )",
					valueProvider = DiscoveryQueryValueProvider.class) String query) {

		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("column[0]", "Path (href)");
		headers.put("column[1]", "Resource Types (rt)");
		headers.put("column[2]", "Content Types (ct)");
		headers.put("column[3]", "Interfaces (if)");
		headers.put("column[4]", "Size estimate (sz)");
		headers.put("column[5]", "Observable (obs)");

		Set<WebLink> resources = this.coapClient.discover(query);

		if (resources == null) {// empty response
			resources = new HashSet<>();
		}

		this.coapUriPathValueProvider.updateHints(
				resources.stream().map(WebLink::getURI).collect(Collectors.toList()));

		TableModel model = new BeanListTableModel(formatDiscoveryResult(resources), headers);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}

	private List<Row> formatDiscoveryResult(Set<WebLink> links) {
		List<Row> rows = links.stream().map(link -> {
			Row row = new Row();
			row.getColumn().add(link.getURI());
			row.getColumn().add(toString(link.getAttributes().getResourceTypes()));
			row.getColumn().add(typeNames(link.getAttributes().getContentTypes()));
			row.getColumn().add(toString(link.getAttributes().getInterfaceDescriptions()));
			row.getColumn().add(link.getAttributes().getMaximumSizeEstimate());
			row.getColumn().add(link.getAttributes().hasObservable() ? "observable" : "");
			return row;
		}).collect(Collectors.toList());
		return rows;
	}

	private String typeNames(List<String> contentTypes) {
		return contentTypes.stream()
				.map(Integer::valueOf)
				.map(ct -> MediaTypeRegistry.toString(ct) + " (" + ct + ")")
				.collect(Collectors.joining(", "));
	}

	private String toString(List<String> list) {
		return list.stream().collect(Collectors.joining(", "));
	}

	@ShellMethod(key = "config use", value = "Let the client use Confirmable or Non-Confirmable requests.", group = COAP_SHELL_CONFIGURATION_GROUP)
	@ShellMethodAvailability("availabilityCheck")
	public void configUse(@ShellOption(defaultValue = "con") RequestMode mode) {
		this.requestMessageType = mode;
		switch (mode) {
		case con:
			this.coapClient.useCONs(); break;
		case non:
			this.coapClient.useNONs(); break;
		}
		this.eventPublisher.publishEvent(this.coapClient.getURI() + "[" + this.requestMessageType.name().toUpperCase() + "]");
	}

	@ShellMethod("Request data from CoAP Resource")
	@ShellMethodAvailability("availabilityCheck")
	public String get(
			@ShellOption(help = "Resource URI path", valueProvider = UriPathValueProvider.class) String path,
			@ShellOption(defaultValue = "false", help = "If set an asynchronous Get will be performed") boolean async,
			@ShellOption(defaultValue = COAP_TEXT_PLAIN, help = "accepted response content-type", valueProvider = ContentTypeValueProvider.class) String accept) {

		StringBuffer result = new StringBuffer();
		final String baseUri = this.coapClient.getURI();
		try {
			result.append(requestInfo("GET", baseUri + path, async));
			this.coapClient.setURI(this.coapClient.getURI() + path);
			if (async) {
				this.coapClient.get(asyncHandler(), coapContentType(accept));
			}
			else {
				CoapResponse response = this.coapClient.get(coapContentType(accept));
				result.append(PrintUtils.prettyPrint(response));
			}
		}
		finally {
			this.coapClient.setURI(baseUri);
		}

		return normal(result.toString());
	}

	@ShellMethod("Create/Update data in CoAP Resource")
	@ShellMethodAvailability("availabilityCheck")
	public String post(
			@ShellOption(help = "Resource URI path", valueProvider = UriPathValueProvider.class) String path,
			@ShellOption(defaultValue = ShellOption.NULL, help = "message payload") String payload,
			@ShellOption(defaultValue = ShellOption.NULL, help = "message payload file") File payloadFile,
			@ShellOption(defaultValue = COAP_TEXT_PLAIN, help = "payload content-type", valueProvider = ContentTypeValueProvider.class) String format,
			@ShellOption(defaultValue = COAP_TEXT_PLAIN, help = "accepted response content-type", valueProvider = ContentTypeValueProvider.class) String accept,
			@ShellOption(defaultValue = "false", help = "If set an asynchronous Post will be performed") boolean async) throws IOException {

		Assert.isTrue(payloadFile == null || payloadFile.exists(),
				"Payload file [" + payloadFile + "] doesn't exists!");
		Assert.isTrue(!(payloadFile != null && StringUtils.hasText(payload)),
				"The `payload` and `payload-file` arguments are mutually exclusive!");
		Assert.isTrue(payloadFile != null || StringUtils.hasText(payload), "Either the `payload` or `payload-file` parameter must be set!");

		String payloadContent = (payloadFile != null) ? FileCopyUtils.copyToString(new FileReader(payloadFile)) : payload;

		StringBuffer result = new StringBuffer();
		String baseUri = coapClient.getURI();
		try {
			this.coapClient.setURI(baseUri + path);
			result.append(requestInfo("POST", baseUri + path, async));
			if (async) {
				coapClient.post(asyncHandler(), payloadContent,
						coapContentType(format), coapContentType(accept));
			}
			else {
				CoapResponse response = coapClient.post(payloadContent,
						coapContentType(format), coapContentType(accept));
				result.append(PrintUtils.prettyPrint(response));
			}
		}
		finally {
			coapClient.setURI(baseUri);
		}
		return normal(result.toString());
	}

	@ShellMethod("Update data in CoAP Resource")
	@ShellMethodAvailability("availabilityCheck")
	public String put(
			@ShellOption(help = "PUT resource URI path", valueProvider = UriPathValueProvider.class) String path,
			@ShellOption(defaultValue = ShellOption.NULL, help = "PUT message payload") String payload,
			@ShellOption(defaultValue = ShellOption.NULL, help = "message payload file") File payloadFile,
			@ShellOption(defaultValue = COAP_TEXT_PLAIN, help = "payload content-type", valueProvider = ContentTypeValueProvider.class) String format,
			@ShellOption(defaultValue = "false", help = "If set an asynchronous PUT will be performed") boolean async) throws IOException {

		Assert.isTrue(payloadFile == null || payloadFile.exists(),
				"Payload file [" + payloadFile + "] doesn't exists!");
		Assert.isTrue(!(payloadFile != null && StringUtils.hasText(payload)),
				"The `payload` and `payload-file` arguments are mutually exclusive!");
		Assert.isTrue(payloadFile != null || StringUtils.hasText(payload), "Either the `payload` or `payload-file` parameter must be set!");

		String payloadContent = (payloadFile != null) ? FileCopyUtils.copyToString(new FileReader(payloadFile)) : payload;

		StringBuffer result = new StringBuffer();
		final String baseUri = this.coapClient.getURI();
		try {
			this.coapClient.setURI(baseUri + path);
			result.append(requestInfo("PUT", baseUri + path, async));
			if (async) {
				this.coapClient.put(asyncHandler(), payloadContent, coapContentType(format));
			}
			else {
				CoapResponse response = this.coapClient.put(payloadContent, coapContentType(format));
				result.append(PrintUtils.prettyPrint(response));
			}
		}
		finally {
			this.coapClient.setURI(baseUri);
		}

		return normal(result.toString());
	}

	@ShellMethod("Delete CoAP Resource")
	@ShellMethodAvailability("availabilityCheck")
	public String delete(@ShellOption(help = "Resource URI path to delete",
			valueProvider = UriPathValueProvider.class) String path,
			@ShellOption(defaultValue = "false", help = "Perform the delete asynchronously") boolean async) {

		StringBuffer result = new StringBuffer();
		final String baseUri = this.coapClient.getURI();
		try {
			this.coapClient.setURI(baseUri + path);
			result.append(requestInfo("DELETE", baseUri + path, async));
			if (async) {
				this.coapClient.delete(asyncHandler());
			}
			else {
				CoapResponse response = this.coapClient.delete();
				result.append(PrintUtils.prettyPrint(response));
			}
		}
		finally {
			this.coapClient.setURI(baseUri);
		}
		return normal(result.toString());
	}

	@ShellMethod(key = "observe", value = "Start observing data from a CoAP Resource")
	@ShellMethodAvailability("observerAvailabilityCheck")
	public String observeStart(
			@ShellOption(help = "Resource URI path", valueProvider = UriPathValueProvider.class) String path,
			@ShellOption(defaultValue = COAP_TEXT_PLAIN, help = "accepted response content-type", valueProvider = ContentTypeValueProvider.class) String accept) {

		StringBuffer result = new StringBuffer();
		final String baseUri = this.coapClient.getURI();
		try {
			this.coapClient.setURI(this.coapClient.getURI() + path);
			result.append(requestInfo("OBSERVE Start", baseUri + path, false));
			this.observeURI = baseUri + path;

			CoapHandler handler = new CoapHandler() {
				@Override
				public void onLoad(CoapResponse response) {
					observerResponses
							.append(cyan("OBSERVE Response (" + observeURI + "):")).append(StringUtil.lineSeparator())
							.append(cyan(PrintUtils.prettyPrint(response))).append(StringUtil.lineSeparator());
				}

				@Override
				public void onError() {
					observerResponses
							.append(red("OBSERVE Error (" + observeURI + "):")).append(StringUtil.lineSeparator());
				}
			};

			this.observeRelation = this.coapClient.observe(handler, coapContentType(accept));
		}
		finally {
			this.coapClient.setURI(baseUri);
		}

		return normal(result.toString());
	}

	@ShellMethod(key = "observe show messages", value = "List observed responses")
	@ShellMethodAvailability("stopObserverAvailabilityCheck")
	public String observeShowMessages() {
		return cyan(this.observerResponses.toString());
	}

	@ShellMethod(key = "observe stop", value = "Stop the observe task")
	@ShellMethodAvailability("stopObserverAvailabilityCheck")
	public String observeStop() {
		if (this.observeRelation != null && !this.observeRelation.isCanceled()) {
			this.observeRelation.proactiveCancel();
			this.observerResponses = new StringBuilder();
			return cyan("OBSERVE stopped (" + observeURI + ")");
		}
		return red("NO observer to stop");
	}

	//
	// Shell Command Availability Polices
	//
	public Availability availabilityCheck() {
		return (coapClient != null)
				? Availability.available()
				: Availability.unavailable("you are not connected");
	}

	public Availability observerAvailabilityCheck() {
		if (!availabilityCheck().isAvailable()) {
			return availabilityCheck();
		}
		if (observeRelation == null || observeRelation.isCanceled() == true) {
			return Availability.available();
		}

		return Availability.unavailable("Only one observer is allowed to run in any given time");
	}

	public Availability stopObserverAvailabilityCheck() {
		if (observeRelation != null && observeRelation.isCanceled() == false) {
			return Availability.available();
		}

		return Availability.unavailable("There is no active observer");
	}

	private String requestInfo(String method, String path, boolean async) {
		return cyan("CoAP " + method.toUpperCase() + (async ? "[ASYNC]" : "") + ": " + path + StringUtil.lineSeparator);
	}

	/**
	 * Converts content type text into CoAP's media type code.
	 * If the input is a number assumes it is already a CoAP media type code.
	 * @param contentType - Content type number or text
	 * @return CoAP media type code
	 */
	private int coapContentType(String contentType) {
		int coapContentTypeCode = -1;
		try {
			coapContentTypeCode = Integer.parseInt(contentType);
		}
		catch (NumberFormatException nfe) {
		}
		return (coapContentTypeCode < 0) ? MediaTypeRegistry.parse(contentType) : coapContentTypeCode;
	}

	/**
	 * @return New asynchronous handler for the purpose of the async method calls (GET, POST, PUT).
	 */
	private CoapHandler asyncHandler() {
		return new CoapHandler() {
			@Override
			public void onLoad(CoapResponse response) {
				terminal.writer()
						.append(cyan(String.format("\nAsync (%s)\n %s \n", coapClient.getURI(), PrintUtils.prettyPrint(response))))
						.flush();
			}

			@Override
			public void onError() {
				terminal.writer()
						.append(red(String.format("\nAsync (%s) Failure!!\n", coapClient.getURI())))
						.flush();
			}
		};
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		eventPublisher = applicationEventPublisher;
	}
}
