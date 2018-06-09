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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.core.MethodParameter;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProviderSupport;
import org.springframework.util.Assert;

/**
 * @author Christian Tzolov
 */
public abstract class StringListValueProvider extends ValueProviderSupport {

	private CopyOnWriteArrayList<String> prefixHints = new CopyOnWriteArrayList();

	public StringListValueProvider() {
		this(new ArrayList<>());
	}

	public StringListValueProvider(List<String> prefixHints) {
		Assert.notNull(prefixHints, "Prefix Hints can't be null");
		this.prefixHints = new CopyOnWriteArrayList();
		this.prefixHints.addAll(prefixHints);
	}

	@Override
	public List<CompletionProposal> complete(MethodParameter parameter,
			CompletionContext completionContext, String[] hints) {

		return prefixHints.stream()
				.filter(o -> o.startsWith(prefix(completionContext)))
				.map(contentType -> new CompletionProposal(contentType))
				.collect(Collectors.toList());
	}

	private String prefix(CompletionContext completionContext) {
		final String prefix = completionContext.currentWordUpToCursor();
		return (prefix != null) ? prefix : "";
	}

	public void updatePrefixHints(List<String> hintsUpdate) {
		this.prefixHints.clear();
		this.prefixHints.addAll(hintsUpdate);
	}

	public void addPrefixHint(String hint) {
		this.prefixHints.add(hint);
	}

	public void clearHints() {
		this.prefixHints.clear();
	}
}
