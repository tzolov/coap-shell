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
package io.datalake.coap.coapshell.util;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.util.StringUtil;
import org.w3c.dom.Document;

import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.http.HttpStatus;
import org.springframework.util.MimeTypeUtils;

/**
 * @author Christian Tzolov
 */
public class PrintUtils {
	/**
	 * Formats a {@link Response} into a readable String representation.
	 *
	 * @param
	 * @return the pretty print
	 */
	public static String prettyPrint(CoapResponse coapResponse) {
		Response r = coapResponse.advanced();

		int httpStatusCode = Integer.valueOf(r.getCode().codeClass) * 100 + Integer.valueOf(r.getCode().codeDetail);
		HttpStatus httpStatus = HttpStatus.resolve(httpStatusCode);
		String status = colorText(String.format("%s-%s", httpStatusCode, httpStatus.getReasonPhrase()), httpStatus.isError() ? AnsiColor.RED : AnsiColor.DEFAULT);

		StringBuilder sb = new StringBuilder();
		sb.append(green("-------------------------------- CoAP Response ---------------------------------")).append(StringUtil.lineSeparator());
		sb.append(String.format(" MID    : %d", r.getMID())).append(StringUtil.lineSeparator());
		sb.append(String.format(" Token  : %s", r.getTokenString())).append(StringUtil.lineSeparator());
		sb.append(String.format(" Type   : %s", r.getType().toString())).append(StringUtil.lineSeparator());
		sb.append(String.format(" Status : %s", status)).append(StringUtil.lineSeparator());
		sb.append(String.format(" Options: %s", r.getOptions().toString())).append(StringUtil.lineSeparator());
		if (r.getRTT() != null) {
			sb.append(String.format(" RTT    : %d ms", r.getRTT())).append(StringUtil.lineSeparator());
		}
		sb.append(String.format(" Payload: %d Bytes", r.getPayloadSize())).append(StringUtil.lineSeparator());
		if (r.getPayloadSize() > 0 && MediaTypeRegistry.isPrintable(r.getOptions().getContentFormat())) {
			sb.append(green("............................... Body Payload ...................................")).append(StringUtil.lineSeparator());
			if (r.getOptions().toString().contains(MimeTypeUtils.APPLICATION_JSON_VALUE)) {
				sb.append(cyan(prettyJson(r.getPayloadString())));
			}
			else if (r.getOptions().toString().contains(MimeTypeUtils.APPLICATION_XML_VALUE)) {
				sb.append(cyan(prettyXml(r.getPayloadString())));
			}
			else {
				sb.append(r.getPayloadString());
			}
			sb.append(StringUtil.lineSeparator());
		}
		sb.append(green("--------------------------------------------------------------------------------"));

		return sb.toString();
	}

	private static String prettyJson(String text) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Object jsonObject = mapper.readValue(text, Object.class);
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
		}
		catch (IOException io) {
			return text;
		}
	}

	private static String prettyXml(String text) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(text);

			Transformer tform = TransformerFactory.newInstance().newTransformer();
			tform.setOutputProperty(OutputKeys.INDENT, "yes");
			tform.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			StringWriter sw = new StringWriter();
			tform.transform(new DOMSource(document), new StreamResult(sw));
			return sw.toString();
		}
		catch (Exception e) {
			return text;
		}
	}

	public static String cyan(String text) {
		return colorText(text, AnsiColor.CYAN);
	}

	public static String red(String text) {
		return colorText(text, AnsiColor.RED);
	}

	public static String green(String text) {
		return colorText(text, AnsiColor.GREEN);
	}

	public static String normal(String text) {
		return colorText(text, AnsiColor.DEFAULT);
	}

	public static String colorText(String text, AnsiColor color) {
		return AnsiOutput.toString(color, text, AnsiColor.DEFAULT);
	}
}
