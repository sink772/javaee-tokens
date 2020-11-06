[ ![Download](https://api.bintray.com/packages/sink772/maven/javaee-tokens/images/download.svg) ](https://bintray.com/sink772/maven/javaee-tokens/_latestVersion)

# A Java SCORE Library for Standard Tokens

This repository contains a Java SCORE library for ICON standard tokens like
[IRC2](https://github.com/icon-project/IIPs/blob/master/IIPS/iip-2.md) and
[IRC3](https://github.com/icon-project/IIPs/blob/master/IIPS/iip-3.md).
SCORE developers are no longer required to write the whole things from scratch.
This project provides reusable Java classes to build custom user contracts conveniently.

## Usage

You can include this package from JCenter by adding the following dependency in your `build.gradle`.

```groovy
implementation 'com.github.sink772:javaee-tokens:0.5.3'
```

You need to create a entry Java class to inherit the attributes and methods from the basic token classes.
The example below would be the simplest IRC2 token SCORE with a fixed supply.

```java
public class IRC2FixedSupply extends IRC2Basic {
    public IRC2FixedSupply(String _name, String _symbol) {
        super(_name, _symbol, 3);
        _mint(Context.getCaller(), BigInteger.valueOf(1000000));
    }
}
```

For a more complete example, please visit [Java SCORE Examples](https://github.com/icon-project/java-score-examples).

## License

This project is available under the [Apache License, Version 2.0](LICENSE).
