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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.core.MethodParameter;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProviderSupport;
import org.springframework.stereotype.Component;

/**
 * @author Christian Tzolov
 */
@Component
public class UriPathValueProvider extends ValueProviderSupport {

	private ConcurrentHashMap<String, String> hintList = new ConcurrentHashMap<>();

	@Override
	public List<CompletionProposal> complete(MethodParameter parameter, CompletionContext completionContext, String[] hints) {
		List<CompletionProposal> result = new ArrayList<>();
		for (String hint : this.hintList.keySet()) {
			String prefix = completionContext.currentWordUpToCursor();
			if (prefix == null) {
				prefix = "";
			}
			if (hint.startsWith(prefix)) {
				result.add(new CompletionProposal(hint));
			}
		}
		return result;
	}

	public void updateHints(List<String> hintsUpdate) {
		this.hintList.clear();
		this.hintList.putAll(
				hintsUpdate.stream().collect(Collectors.toMap(h -> h, h -> "")));
	}
}
