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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import io.datalake.coap.coapshell.util.Row;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;

/**
 * @author Christian Tzolov
 */
@ShellComponent
@ShellCommandGroup(CoapShellCommands.SHELL_COAP_REST_COMMANDS_GROUP)
public class CoapInfoCommands {

	@ShellMethod(key = "mime types", value = "List supported MIME types")
	public Table mimeTypes() {

		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("column[0]", "Type Id");
		headers.put("column[1]", "Type Name");

		List<Row> list = MediaTypeRegistry.getAllMediaTypes().stream()
				.map(i -> {
					Row row = new Row();
					row.getColumn().add("" + i);
					row.getColumn().add(MediaTypeRegistry.toString(i));
					return row;
				}).collect(Collectors.toList());

		TableModel model = new BeanListTableModel(list, headers);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}
}
