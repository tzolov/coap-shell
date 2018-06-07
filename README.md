# CoAP Shell
[CoAP](https://en.wikipedia.org/wiki/Constrained_Application_Protocol) is a REST based web transfer protocol specialized for use with constrained nodes and constrained networks in the Internet of Things (IoT).

The `CoAP Shell` java, Command Line Interface for interacting with CoAP resources. It can be used with un-secured `coap:` and secured `coaps:` ([DTLS](https://en.wikipedia.org/wiki/Datagram_Transport_Layer_Security)) transports. 
The Shell can be used to explore the `IKEA TRÅDFRI Gateway` as well!. [ ![Download](https://api.bintray.com/packages/big-data/maven/coap-shell/images/download.svg) ](https://bintray.com/big-data/maven/coap-shell/_latestVersion)

The CoAP Shell leverages the [Spring Shell](https://projects.spring.io/spring-shell/), [Californium (Cf)](https://www.eclipse.org/californium/) and [Scandium (Sc)](https://www.eclipse.org/californium/), 
projects and the [SpringBoot](https://spring.io/projects/spring-boot) programming model. Build produces a single self-executable jar. Later can be used in any Java8 enabled environment.  


![CoAP Shell Demo](https://raw.githubusercontent.com/tzolov/coap-shell/master/src/test/resources/coap-shell-dem-overview.gif)

### Features
* Supports both the `coap://` as well as the secured `caps://` (over DTLS) protocols.
* Allows synchronous and asynchronous (with `--asynch` argument) `GET`, `PUT`, `POST` and `DELETE` methods execution.
* The CoAP `observer` is supported by the following commands: `observer <resource>`, `observer show responses` and `observer stop`. Note: only one observer resource is allowed at give time.
* Allows switching between Conformable (CON) and Non-Conformable (NON) message models. Looks for `config use CON/NON` command.
* The `discovery` command lists the well-known CoAP resource served by the server.  
* Autocompletion is provided the commands as well as for important parameters such as URI resource paths (requires at least one `discovery` call), content type parameters and so on.
* Smart response visualization and pretty payload formatting. 
* Support for the `IKEA Tradfri` smart lighting gateway.
* Can plug different KeyStores and certificates. 
* Self-executable jar. Can be run on every Java 8 enabled environment.

### Quick Start
* Download a pre-build [coap-shell.jar](https://bintray.com/big-data/maven/download_file?file_path=io%2Fdatalake%2Fcoap%2Fcoap-shell%2F1.0.1%2Fcoap-shell-1.0.1.jar) or build one yourself following the instructions in the next section
* Start the shell
```bash
java -jar ./coap-shell.jar
```

```bash
  _____     ___   ___     ______       ____
 / ___/__  / _ | / _ \   / __/ /  ___ / / /
/ /__/ _ \/ __ |/ ___/  _\ \/ _ \/ -_) / /
\___/\___/_/ |_/_/     /___/_//_/\__/_/_/
CoAP Shell (v0.0.1-SNAPSHOT) iot.datalake.io

unconnected:>
```
* Connect to CoAP server (such as `coaps://californium.eclipse.org:5684` or `coap://coap.me`)

```bash
unconnected:>connect coaps://californium.eclipse.org:5684
coaps://californium.eclipse.org:5684[CON]:>
```
* Discover the available CoAP resources:

```bash
coaps://californium.eclipse.org:5684[CON]:>discover
┌─────────────────┬───────────────────┬────────────────────────────────────┬───────────────┬──────────────────┬────────────────┐
│Path (href)      │Resource Types (rt)│Content Types (ct)                  │Interfaces (if)│Size estimate (sz)│Observable (obs)│
├─────────────────┼───────────────────┼────────────────────────────────────┼───────────────┼──────────────────┼────────────────┤
│/.well-known/core│                   │                                    │               │                  │                │
├─────────────────┼───────────────────┼────────────────────────────────────┼───────────────┼──────────────────┼────────────────┤
│/large           │block              │                                    │               │1280              │                │
├─────────────────┼───────────────────┼────────────────────────────────────┼───────────────┼──────────────────┼────────────────┤
│/multi-format    │                   │text/plain (0), application/xml (41)│               │                  │                │
├─────────────────┼───────────────────┼────────────────────────────────────┼───────────────┼──────────────────┼────────────────┤
```

* Get resource data

```bash
coaps://californium.eclipse.org:5684[CON]:>get /multi-format --accept application/xml
CoAP GET: coaps://californium.eclipse.org:5684/multi-format
-------------------------------- CoAP Response ---------------------------------
 MID    : 53611
 Token  : [c64e9a0af487b20d]
 Type   : ACK
 Status : 205-Reset Content
 Options: {"Content-Format":"application/xml"}
 RTT    : 124 ms
 Payload: 63 Bytes
............................... Body Payload ...................................
<msg type="CON" code="GET" mid=53611 accept="application/xml"/>
--------------------------------------------------------------------------------

```
* Use `help` to the available commands and how are they used.
* Use `TAB` for command and argument auto-completion.

### IKEA TRÅDFRI Gateway Support
TBD
* Generate Gateway pre-share key (psk)
* Use the psk to connect to gateway 

### How to Build
Clone the project from GitHub and build with Maven.
```bash
git clone https://github.com/tzolov/coap-shell.git
cd ./coap-shell
./mvnw clean install
```
Then run the self-executable jar in the `target` folder.

### Debugging
Start the shell with `--logging.level=DEBUG` to enable debug log level for the entire application
 or `--logging.level.org.eclipse.californium=DEBUG` to debug only californium and scandium. Later
 is useful to debug the CoAP request message and DTLS interactions.

For example: 
```
java -jar ./target/coap-shell-0.0.1-SNAPSHOT.jar --logging.level.org.eclipse.californium=DEBUG
```
