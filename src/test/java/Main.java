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

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.datalake.coap.coapshell.util.Row;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.elements.exception.ConnectorException;

import org.springframework.beans.BeanWrapperImpl;

/**
 * @author Christian Tzolov
 */
public class Main {

	public static void main(String[] args) throws ConnectorException, IOException {

		CoapClient client = new CoapClient("coap://californium.eclipseprojects.io:5683/");
		//client.setURI(client.getURI() + "/test");
		//CoapResponse response = client.get();
		//
		//
		//System.out.println("CODE : " + response.getCode());
		//System.out.println("OPTIONS : " + response.getOptions());
		//System.out.println("BODY : " + response.getResponseText());
		//
		//System.out.println(System.lineSeparator() + "ADVANCED" + System.lineSeparator());
		//System.out.println(Utils.prettyPrint(response));


		Set<WebLink> links = client.discover();

		List<Row> al = links.stream().map(l -> {
			Row row = new Row();
			row.getColumn().add(l.getURI());
			row.getColumn().add(l.getAttributes().getResourceTypes().toString());
			row.getColumn().add(l.getAttributes().getContentTypes().stream()
					.map(Integer::valueOf)
					.map(ct -> MediaTypeRegistry.toString(ct) + " (" + ct + ")")
					.collect(Collectors.toList()).toString());
			row.getColumn().add(l.getAttributes().getInterfaceDescriptions().toString());
			row.getColumn().add(l.getAttributes().getMaximumSizeEstimate());
			row.getColumn().add("" + l.getAttributes().hasObservable());
			return row;
		}).collect(Collectors.toList());

		System.out.println(al);

		for (Row l : al) {

			BeanWrapperImpl bw = new BeanWrapperImpl(l);

			System.out.println(bw.getPropertyValue("column[0]"));
		}


		//for (WebLink link : links) {
		//	System.out.println(link);
		//System.out.println(link.getAttributes().getContentTypes());
		//System.out.println(
		//		link.getAttributes().getContentTypes().stream()
		//				.map(ctStr -> Integer.valueOf(ctStr))
		//				.map(ct -> MediaTypeRegistry.toString(ct))
		//				.collect(Collectors.toList()));
		//System.out.println(link.getAttributes().hasObservable());
		//}
	}
}
