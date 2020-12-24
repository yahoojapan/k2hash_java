k2hash_java
------
[![Actions Status](https://github.com/yahoojapan/k2hash_java/workflows/CI/badge.svg)](https://github.com/yahoojapan/k2hash_java/actions)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/yahoojapan/k2hash_nodejs/master/LICENSE)

### Overview

k2hash_java is an official java driver for [k2hash](https://k2hash.antpick.ax/).
 - K2HASH - K2Hash by Yahoo! JAPAN

### Install

Add the following repository and the dependency to your pom.xml or settings.xml.
```
  <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>

  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
        <repository>
          <id>github</id>
          <name>GitHub yahoojapan Apache Maven Packages</name>
          <url>https://maven.pkg.github.com/yahoojapan/k2hash_java</url>
        </repository>
      </repositories>
    </profile>
  </profiles>

```
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

### Documents
  - [Document top page](https://java.k2hash.antpick.ax/)
  - [API Document page](https://java.k2hash.antpick.ax/apidocs/index.html)
  - [About K2HASH](https://k2hash.antpick.ax/)
  - [About AntPickax](https://antpick.ax/)

### Packages

  - [GitHub Packages](https://github.com/yahoojapan/k2hash_java/packages)

### License

MIT License. See the LICENSE file.

## AntPickax

[AntPickax](https://antpick.ax/) is
  - an open source team in [Yahoo Japan Corporation](https://about.yahoo.co.jp/info/en/company/).
  - a product family of open source software developed by [AntPickax](https://antpick.ax/).
