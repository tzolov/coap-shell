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

import java.security.cert.Certificate;

import io.datalake.coap.coapshell.CoapShellProperties;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedSinglePskStore;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Christian Tzolov
 */
@Component
public class CoapDtlsSupport {

	private CoapShellProperties properties;

	@Autowired
	public CoapDtlsSupport(CoapShellProperties properties) {
		this.properties = properties;
	}

	public DTLSConnector createConnector(String identity, String preSharedKey) {
		DTLSConnector dtlsConnector = null;

		try {
			// load key store
			SslContextUtil.Credentials clientCredentials = SslContextUtil.loadCredentials(
					this.properties.getKeyStoreLocation(), this.properties.getKeyStoreAlias(),
					this.properties.getKeyStorePassword().toCharArray(),
					this.properties.getKeyStorePassword().toCharArray());

			Certificate[] trustedCertificates = SslContextUtil.loadTrustedCertificates(
					this.properties.getTrustStoreLocation(), this.properties.getTrustStoreAlias(),
					this.properties.getTrustStorePassword().toCharArray());

			DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder();
			if (StringUtils.hasText(identity) && StringUtils.hasText(preSharedKey)) {
				builder.setAdvancedPskStore(new AdvancedSinglePskStore(identity, preSharedKey.getBytes()));
			}

			builder.setIdentity(clientCredentials.getPrivateKey(), clientCredentials.getCertificateChain(),
					CertificateType.RAW_PUBLIC_KEY, CertificateType.X_509);

			builder.setAdvancedCertificateVerifier(StaticNewAdvancedCertificateVerifier.builder()
					.setTrustedCertificates(trustedCertificates).setTrustAllRPKs().build());

			builder.setMaxConnections(100);
			builder.setConnectionThreadCount(1);

			builder.setStaleConnectionThreshold(properties.getStaleConnectionThreshold());

			// Create DTLS endpoint
			dtlsConnector = new DTLSConnector(builder.build());
			dtlsConnector.setRawDataReceiver(raw -> System.out.println("Received response: " + new String(raw.getBytes())));
		}
		catch (Exception e) {
			System.err.println("Error creating DTLS endpoint");
			e.printStackTrace();
		}

		return dtlsConnector;
	}

}
