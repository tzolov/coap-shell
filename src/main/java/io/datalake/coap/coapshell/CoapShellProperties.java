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

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.PositiveOrZero;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("coap")
public class CoapShellProperties {

	private String trustStoreLocation;
	private String trustStorePassword;
	private String trustStoreAlias;
	private String keyStoreLocation;
	private String keyStorePassword;
	private String keyStoreAlias;

	private int staleConnectionThreshold = 24 * 60 * 60; // 24 hours (sec)

	@NotEmpty
	public String getTrustStoreLocation() {
		return trustStoreLocation;
	}

	public void setTrustStoreLocation(String trustStoreLocation) {
		this.trustStoreLocation = trustStoreLocation;
	}

	@NotEmpty
	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	@NotEmpty
	public String getTrustStoreAlias() {
		return trustStoreAlias;
	}

	public void setTrustStoreAlias(String trustStoreAlias) {
		this.trustStoreAlias = trustStoreAlias;
	}

	@NotEmpty
	public String getKeyStoreLocation() {
		return keyStoreLocation;
	}

	public void setKeyStoreLocation(String keyStoreLocation) {
		this.keyStoreLocation = keyStoreLocation;
	}

	@NotEmpty
	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	@NotEmpty
	public String getKeyStoreAlias() {
		return keyStoreAlias;
	}

	public void setKeyStoreAlias(String keyStoreAlias) {
		this.keyStoreAlias = keyStoreAlias;
	}

	@PositiveOrZero
	public int getStaleConnectionThreshold() {
		return staleConnectionThreshold;
	}

	public void setStaleConnectionThreshold(int staleConnectionThreshold) {
		this.staleConnectionThreshold = staleConnectionThreshold;
	}

	@Override
	public String toString() {
		return "CoapShellProperties{" +
				"trustStoreLocation='" + trustStoreLocation + '\'' +
				", trustStorePassword='" + trustStorePassword + '\'' +
				", keyStoreLocation='" + keyStoreLocation + '\'' +
				", keyStorePassword='" + keyStorePassword + '\'' +
				", keyStoreAlias='" + keyStoreAlias + '\'' +
				'}';
	}
}
