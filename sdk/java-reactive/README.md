# Sawtooth Java SDK - a Reactive view.

This project intends to be a Java SDK to projects that will interact with a Sawtooth Network.

It is based on the initial work of the Sawtooth team, from Intel.

__This is an ongoing effort, any help is welcome.__


## Modules

### Commons

Handle the protobuf classes generation, crypto handling, basic configurations and Sawotth Messages creation.

### REST Client SDK

Using Jersey as the core REST client, and CDI to manage dependency injections, it provides basic calling on the Sawtooth REST endpoints. 

### Transaction Processor SDK

Base classes to implement a new Transaction Processor

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

## Prerequisites

### Java 10

The java version may be 10.

### Maven

Version 3.5.0 or higher.

### Bitcoin Native Secp256k1 Library

The signing methods are based on Bitcoin code.

To install it, 

Clone the secp256k1 from the [repository](https://github.com/bitcoin-core/secp256k1).

Build it, from the [Documentation :](https://bitcoinj.github.io/javadoc/0.14.7/org/bitcoin/NativeSecp256k1.html)

> To build secp256k1 for use with bitcoinj, run `./configure --enable-jni --enable-experimental --enable-module-schnorr --enable-module-ecdh` and `make` then copy `.libs/libsecp256k1.so` to your system library path or point the JVM to the folder containing it with -Djava.library.path 



## Running the tests

The REST tests would need a running Sawtooth network.

If it isn't at localhost, you can change the address on the maven CLI:

- `mvn -Dsw_rest_url="http(s):<REST API ADDRESS>:<REST API PORT>" -Dsw_validator_url="http(s)://<VALIDATOR ADDRESS>:<VALIDATOR PORT>" clean test` 

Keep in mind that the Integer TP test only validates the Message structure validity (signatures, hashes, etc), and the submitted transaction will not work.


## Built With


* [Maven](https://maven.apache.org/) 	- Dependency Management
* [Eclipse](http://www.eclipse.org/)	- IDE
* [TestNG](https://testng.org/)			- Testing suite.


## Authors

* **Leonardo T. de Carvalho** - *Initial work* - [CarvalhoLeonardo](https://github.com/CarvalhoLeonardo)


## Acknowledgments

* The Sawtooth community
* The guys at the [#Sawtooth Channel](https://chat.hyperledger.org/channel/sawtooth)
* Intel, that provided the initial code.
* Hail Eris!

