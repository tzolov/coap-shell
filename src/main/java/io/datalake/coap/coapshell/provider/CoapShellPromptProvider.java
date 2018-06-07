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

import io.datalake.coap.coapshell.CoapConnectionStatus;
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

	private CoapConnectionStatus connectionStatus;

	@Override
	public AttributedString getPrompt() {
		if (this.connectionStatus != null && StringUtils.hasText(this.connectionStatus.getBaseUri())) {
			return new AttributedString(this.promptPrefix() + ":>",
					AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
		}
		else {
			return new AttributedString("server-unknown:>",
					AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
		}
	}

	@EventListener
	public void handle(CoapConnectionStatus connectionStatus) {
		this.connectionStatus = connectionStatus;
	}

	private String promptPrefix() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.connectionStatus.getBaseUri());
		if (this.connectionStatus.getMode() != CoapConnectionStatus.RequestMode.con) {
			sb.append("[").append(this.connectionStatus.getMode().name().toUpperCase()).append("]");
		}
		if (this.connectionStatus.isObserveActivated()) {
			sb.append("[OBS]");
		}
		return sb.toString();
	}
}
