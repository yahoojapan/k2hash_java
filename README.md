k2hash_java
------
[![Actions Status](https://github.com/yahoojapan/k2hash_java/workflows/CI/badge.svg)](https://github.com/yahoojapan/k2hash_java/actions)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/yahoojapan/k2hash_nodejs/master/LICENSE)

### Overview

k2hash_java is an official java driver for [k2hash](https://k2hash.antpick.ax/).
 - K2HASH - K2Hash by Yahoo! JAPAN

### Install

Add the following dependency to your pom.xml.
```
        <dependency>
          <groupId>ax.antpick</groupId>
          <artifactId>k2hash</artifactId>
        </dependency>
```

### Development

Clone this repository and go into the directory, then run the following command.
```
$ mvn clean exec:exec package
```

#### JDK

You can try a new JDK by changing the JAVA_HOME environment and run `mvn clean exec:exec package`.
```
$ export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.12.0.7-0.el7_9.x86_64
$ mvn clean exec:exec package
```

#### Update outdated dependency libraries

Run the following command to find outdated dependency libraries.
```
$ mvn versions:display-plugin-updates
$ mvn versions:display-dependency-updates
```
Then, you should replace the outdated versions with the newer ones in the pom.xml.

### Documents
  - [Document top page](https://java.k2hash.antpick.ax/)
  - [API Document page](https://java.k2hash.antpick.ax/apidocs/index.html)
  - [About K2HASH](https://k2hash.antpick.ax/)
  - [About AntPickax](https://antpick.ax/)

### Packages

  - [Maven Central Repository](https://mvnrepository.com/artifact/ax.antpick/k2hash)

### License

MIT License. See the LICENSE file.

## AntPickax

[AntPickax](https://antpick.ax/) is
  - an open source team in [Yahoo Japan Corporation](https://about.yahoo.co.jp/info/en/company/).
  - a product family of open source software developed by [AntPickax](https://antpick.ax/).
