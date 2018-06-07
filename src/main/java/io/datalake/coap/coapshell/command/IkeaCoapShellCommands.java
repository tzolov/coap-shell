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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.datalake.coap.coapshell.CoapConnectionStatus;
import io.datalake.coap.coapshell.provider.IkeaDeviceInstanceValueProvider;
import io.datalake.coap.coapshell.util.CoapDtlsSupport;
import io.datalake.coap.coapshell.util.PrintUtils;
import io.datalake.coap.coapshell.util.Row;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
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
import org.springframework.util.StringUtils;

import static io.datalake.coap.coapshell.util.PrintUtils.cyan;
import static io.datalake.coap.coapshell.util.PrintUtils.normal;
import static io.datalake.coap.coapshell.util.PrintUtils.red;

/**
 * @author Christian Tzolov
 */
@ShellComponent
@ShellCommandGroup(IkeaCoapShellCommands.SHELL_IKEA_COMMANDS_GROUP)
public class IkeaCoapShellCommands {

	public static final String IKEA_GATEWAY_CLIENT_IDENTITY = "Client_identity";
	public static final String IKEA_GATEWAY_KEY_URL_TEMPLATE = "coaps://%s:5684/15011/9063";

	public static final String SHELL_IKEA_COMMANDS_GROUP = "Ikea Gateway Commands";

	@Autowired
	private CoapDtlsSupport dtsl;

	private CoapConnectionStatus connectionStatus;

	@Autowired
	private IkeaDeviceInstanceValueProvider instanceValueProvider;

	@EventListener
	public void handle(CoapConnectionStatus connectionStatus) {
		this.connectionStatus = connectionStatus;
	}


	@ShellMethod(key = "ikea gateway key", value = "Generate IDENTITY and PRE_SHARED_KEY for IKEA TRÅDFRI Gateway")
	public String generateIkeaGatewayKey(
			@ShellOption(value = "--ip", help = "Gateway IP address") String gatewayIp,
			@ShellOption(value = "--identity", help = "Identity name to associate with the generated key") String newIdentity,
			@ShellOption(value = "--security-code", help = "Gateway security code (on the back of the box)") String gatewayCode) {

		DTLSConnector dtlsConnector = dtsl.createConnector(IKEA_GATEWAY_CLIENT_IDENTITY, gatewayCode);

		CoapEndpoint coapEndpoint = new CoapEndpoint.CoapEndpointBuilder()
				.setNetworkConfig(NetworkConfig.getStandard())
				.setConnector(dtlsConnector).build();
		CoapClient pskCoapClient = new CoapClient(String.format(IKEA_GATEWAY_KEY_URL_TEMPLATE, gatewayIp.trim()))
				.setTimeout(TimeUnit.SECONDS.toMillis(10))
				.setEndpoint(coapEndpoint);

		CoapResponse gatewayResponse = pskCoapClient.post(String.format("{\"9090\":\"%s\"}", newIdentity), 0);

		dtlsConnector.destroy();
		pskCoapClient.shutdown();

		StringBuilder commandResponse = new StringBuilder();

		commandResponse.append(normal(PrintUtils.prettyPrint(gatewayResponse))).append("\n");

		if (gatewayResponse != null && gatewayResponse.isSuccess()) {
			try {
				Map<String, String> jsonMap = new ObjectMapper().readValue(gatewayResponse.getResponseText(), Map.class);
				String preSharedKey = jsonMap.get("9091");
				commandResponse.append(
						cyan(String.format("IDENTITY: %s , PRE_SHARED_KEY: %s", newIdentity, preSharedKey)));
				return commandResponse.toString();
			}
			catch (IOException e) {
				commandResponse.append(red("Invalid or incomplete JSON response!")).append("\n");
			}
		}

		if (gatewayResponse != null && gatewayResponse.getCode() == CoAP.ResponseCode.BAD_REQUEST) {
			commandResponse.append(red("This error could indicate that you need to " +
					"choose a new identity name, different from: " + newIdentity)).append("\n");
		}

		commandResponse.append(red("Failed to obtain Gateway pre shared key!")).append("\n");

		return commandResponse.toString();
	}

	@ShellMethod(key = "ikea turn on", value = "switch light ON")
	@ShellMethodAvailability("ikeaAvailabilityCheck")
	public String turnLightOn(
			@ShellOption(help = "device id", valueProvider = IkeaDeviceInstanceValueProvider.class) int instance) throws IOException {
		return this.turnLight(instance, true);
	}

	@ShellMethod(key = "ikea turn off", value = "switch light OFF")
	@ShellMethodAvailability("ikeaAvailabilityCheck")
	public String turnLightOff(
			@ShellOption(help = "device id", valueProvider = IkeaDeviceInstanceValueProvider.class) int instance) throws IOException {
		return this.turnLight(instance, false);
	}

	private String turnLight(int instance, boolean on) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		String deviceJson = getJson("/15001/" + instance);
		Map<String, Object> deviceMap = mapper.readValue(deviceJson, Map.class);
		String type = deviceTypeName((Integer) deviceMap.get("5750"));
		if (type.equalsIgnoreCase("LIGHT")) {
			String payload = String.format("{\"3311\":[{\"5850\":%s}]}", (on) ? 1 : 0);
			return putJson("/15001/" + instance, payload);
		}
		return "";
	}

	@ShellMethod(key = "ikea devices", value = "List all devices registered to the IKEA TRÅDFRI Gateway")
	@ShellMethodAvailability("ikeaAvailabilityCheck")
	public Table listIkeaDevices() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		String json = getJson("/15001");
		Integer[] deviceIds = mapper.readValue(json, Integer[].class);
		this.instanceValueProvider.updateHints(Arrays.stream(deviceIds)
				.map(String::valueOf).collect(Collectors.toList()));
		List<Row> list = new ArrayList<>();
		for (int d : deviceIds) {
			String deviceJson = getJson("/15001/" + d);
			Map<String, Object> deviceMap = mapper.readValue(deviceJson, Map.class);
			Row row = new Row();
			row.getColumn().add("" + d); //id
			row.getColumn().add((String) deviceMap.get("9001")); //name
			row.getColumn().add(deviceTypeName((Integer) deviceMap.get("5750"))); //type
			row.getColumn().add(((Map<String, String>) deviceMap.get("3")).get("1")); //model
			row.getColumn().add(((Map<String, String>) deviceMap.get("3")).get("3")); // firmware
			row.getColumn().add(normalize(((Map<String, Object>) deviceMap.get("3")).get("9"))); // battery
			row.getColumn().add(onOffStatus(deviceMap.get("3311"))); // ON/OFF
//			row.getColumn().add(String.valueOf(new Date((Integer) deviceMap.get("9002")))); //created at
			list.add(row);
		}

		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("column[0]", "Instance");
		headers.put("column[1]", "Name");
		headers.put("column[2]", "Type");
		headers.put("column[3]", "Model");
		headers.put("column[4]", "Firmware");
		headers.put("column[5]", "Battery [%]");
		headers.put("column[6]", "ON/OFF");
//		headers.put("column[7]", "Created at");
		TableModel model = new BeanListTableModel(list, headers);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}

	private String onOffStatus(Object value) {
		if (value != null) {
			try {
				Map<String, Object> map = ((Map<String, Object>) ((ArrayList) value).get(0));
				Integer onOfIndicator = (Integer) map.get("5850");
				return (onOfIndicator == 1) ? "ON" : "OFF";
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return "-";
	}

	private String deviceTypeName(Integer typeCode) {
		switch (typeCode) {
		case 0:
			return "SWITCH";
		case 2:
			return "LIGHT";
		case 4:
			return "SENSOR";
		}
		return "" + typeCode;
	}

	private String normalize(Object o) {
		return (o == null) ? "-" : "" + o;
	}

	private String getJson(String path) {
		CoapClient coapClient = new CoapClient(this.connectionStatus.getBaseUri() + path);
		DTLSConnector dtlsConnector = dtsl.createConnector(this.connectionStatus.getIdentity(), this.connectionStatus.getSecret());

		CoapEndpoint coapEndpoint = new CoapEndpoint.CoapEndpointBuilder()
				.setNetworkConfig(NetworkConfig.getStandard())
				.setConnector(dtlsConnector).build();
		coapClient.setEndpoint(coapEndpoint);

		String json = coapClient.get().getResponseText();
		dtlsConnector.destroy();
		coapClient.shutdown();
		return json;
	}

	private String putJson(String path, String payload) {
		CoapClient coapClient = new CoapClient(this.connectionStatus.getBaseUri() + path);
		DTLSConnector dtlsConnector = dtsl.createConnector(this.connectionStatus.getIdentity(), this.connectionStatus.getSecret());

		CoapEndpoint coapEndpoint = new CoapEndpoint.CoapEndpointBuilder()
				.setNetworkConfig(NetworkConfig.getStandard())
				.setConnector(dtlsConnector).build();
		coapClient.setEndpoint(coapEndpoint);

		CoapResponse response = coapClient.put(payload, MediaTypeRegistry.APPLICATION_JSON);
		dtlsConnector.destroy();
		coapClient.shutdown();

		return response.isSuccess() ? "OK" : "FAILED";
	}

	public Availability ikeaAvailabilityCheck() {
		return (this.connectionStatus != null
				&& StringUtils.hasText(this.connectionStatus.getBaseUri())
				&& StringUtils.hasText(this.connectionStatus.getIdentity())
				&& StringUtils.hasText(this.connectionStatus.getSecret()))
				? Availability.available()
				: Availability.unavailable("you are not connected to IKEA gateway");
	}

}
