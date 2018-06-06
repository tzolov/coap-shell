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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.datalake.coap.coapshell.provider.UriPathValueProvider;
import io.datalake.coap.coapshell.util.CoapDtlsSupport;
import io.datalake.coap.coapshell.util.PrintUtils;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.jline.terminal.Terminal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import static io.datalake.coap.coapshell.util.PrintUtils.cyan;
import static io.datalake.coap.coapshell.util.PrintUtils.normal;
import static io.datalake.coap.coapshell.util.PrintUtils.red;

/**
 * @author Christian Tzolov
 */
@ShellComponent
@ShellCommandGroup(IkeaCoapShellCommands.SHELL_COAP_REST_COMMANDS_GROUP)
public class IkeaCoapShellCommands {

	public static final String IKEA_GATEWAY_CLIENT_IDENTITY = "Client_identity";
	public static final String IKEA_GATEWAY_KEY_URL_TEMPLATE = "coaps://%s:5684/15011/9063";
	public static final String SHELL_COAP_REST_COMMANDS_GROUP = "CoAP Commands";

	@Autowired
	private CoapDtlsSupport dtsl;

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
}
