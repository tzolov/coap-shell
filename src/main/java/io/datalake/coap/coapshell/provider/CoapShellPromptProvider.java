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

package io.datalake.coap.coapshell.provider;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import org.springframework.context.event.EventListener;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Christian Tzolov
 */
@Component
public class CoapShellPromptProvider implements PromptProvider {

	private String connection;

	@Override
	public AttributedString getPrompt() {
		if (StringUtils.hasText(connection)) {
			return new AttributedString(connection + ":>",
					AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
		}
		else {
			return new AttributedString("unconnected:>",
					AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
		}
	}

	@EventListener
	public void handle(String connectionUri) {
		this.connection = connectionUri;
	}
}
