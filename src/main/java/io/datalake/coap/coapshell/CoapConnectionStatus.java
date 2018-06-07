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

package io.datalake.coap.coapshell;

/**
 * @author Christian Tzolov
 */
public class CoapConnectionStatus {

	public enum RequestMode {con, non}

	private String baseUri;
	private RequestMode mode;
	private String observedUri;
	private String identity;
	private String secret;

	public String getBaseUri() {
		return baseUri;
	}

	public CoapConnectionStatus setBaseUri(String baseUri) {
		this.baseUri = baseUri;
		return this;
	}

	public RequestMode getMode() {
		return mode;
	}

	public CoapConnectionStatus setMode(RequestMode mode) {
		this.mode = mode;
		return this;
	}

	public boolean isObserveActivated() {
		return this.observedUri != null;
	}

	public CoapConnectionStatus reset() {
		this.baseUri = null;
		this.mode = RequestMode.con;
		this.observedUri = null;
		this.identity = null;
		this.secret = null;
		return this;
	}

	public String getObservedUri() {
		return observedUri;
	}

	public CoapConnectionStatus setObservedUri(String observedUri) {
		this.observedUri = observedUri;
		return this;
	}

	public String getIdentity() {
		return identity;
	}

	public CoapConnectionStatus setIdentity(String identity) {
		this.identity = identity;
		return this;
	}

	public String getSecret() {
		return secret;
	}

	public CoapConnectionStatus setSecret(String secret) {
		this.secret = secret;
		return this;
	}

	@Override
	public String toString() {
		return String.format("[%s], [%s], [%s]", baseUri, mode, (isObserveActivated()) ? "observable" : "non-observable");
	}
}
