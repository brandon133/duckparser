# duckparser

BSD-3-Clause Licensed. See [LICENSE.txt](./LICENSE.txt)

Quacks like a duck, parses like a duck.

`DuckParser` does **2** things:

* Loads Java properties files.
* Parses command line arguments.

How? _You_ create a interface and `DuckParser` will create a proxy implementing your interface as
passed in a Java properties file _or_ passed with any argv-like object such as the command line.

Java 8+, _no_ dependencies.


## Quick Start

```java
import com.newbuck.duckparser.DuckParser;

public class Example {

    // Your Java interface for properties:
    public interface YourPropsInterface {
        int numThreads();
        String executorName();
    }

    @Test public void loadPropertiesFile() {
        // # The contents of test.properties file:
        // num.threads=8
        // executor.name=my_executor
        YourPropsInterface props = DuckParser.build()
            .with("test", $ -> {
                $.prefix = "test";
                // other options ..
            })
            .create(YourPropsInterface.class);

        assert props.numThreads() == 8;
        assert props.executorName().equals("my_executor");
    }

    // Your Java interface for args:
    public interface YourArgsInterface {
        // Optional Default and Alias annotations
        @Default("10")
        @Alias({"n"})
        int numLines();

        // Non-annotated
        boolean skipBlankLines();
        boolean really();

        // Non-option arguments (regular args)
        String[] _args(); // can also be: Collection<String> or List<String>
    }

    @Test public void loadArgv() {
        // the arguments:
        String[] args = new String[] { "-n", "8", "--skip-blank-lines", "/path/to/file.txt" };

        // parse the args to your interface:
        YourArgsInterface argv = DuckParser.build()
            .with($ -> {
                // options ..
            })
            .create(YourArgsInterface.class, args); // passing args

        assert argv.numLines() == 8;   // if not specified in args, then this would be `10`
        assert argv.skipBlankLines();  // kebab arguments gets converted
        assert argv.really() == false; // boolean methods that are not specified in args return false

        assert argv._args().length == 1 && argv._args()[0].equals("/path/to/file.txt");
    }
}
```

**Note**: `$` is a valid variable name in Java. The above builder pattern comes from [Sujit
Kamthe](https://medium.com/beingprofessional/think-functional-advanced-builder-pattern-using-lambda-284714b85ed5)


## Properties

### Properties Files

Yes, most Java applications still use properties files. You might be feeling left out of the newer
file format parties. But don't feel bad: property files are simple, compact, and easy to read and
understand. For most application configuration, they are the best choice.  If you really need
hierarchy, then use YAML or XML (but stay away from JSON). To see what more complicated
configuration looks like, view the [log4j2 configuration
docs](https://logging.apache.org/log4j/2.x/manual/configuration.html#ConfigurationSyntax) which
support all of the above formats. Notice that `log4j2` also includes properties file configuration
so you can probably do more than you think.


### Properties Resources and Locations

Properties are loaded from the classpath and/or files. The client can also pass explicit strings or
properties as options. The rules:

* The `DuckParser` class has a `baseName` property either passed as an option or in the ctr:

    ```java
    // in ctr:
    DuckParser.build().with("name", $ -> { .. }).create(..);

    // or in options:
    DuckParser.build().with($ -> {
        $.baseName = "name";
        ..
    }).create(..);
    ```

* Properties are loaded in order:
    1. `classpath:<baseName>-default.properties`
    2. `classpath:<baseName>.properties`
    3. `file:<searchDirs>/<baseName>-default.properties`
    4. `file:<searchDirs>/<baseName>.properties`

    `<searchDirs>` is set in options and are an array of directory `Path`s to search for the
    properties files. There are no default paths so they must be set explicitly


### DuckParser Options

The `DuckParser` class takes these options in the `with` clause:

| Option | Type | Default | Description |
| --- | --- | --- | --- |
| baseName | String | | The base name for all properties |
| prefix | String | null (no prefix) | The prefix for all properties |
| initPropsMap | Map<String, String> | | Initialize props with these (added first) |
| addProps | String | | In properties file format (file to string), these _override_ all other props, i.e. parsed last |
| searchClasspath | boolean | true | Search the classpath for properties |
| searchDefault | boolean | true | Search the default props name. This is `{baseName}-default.properties` for both classpath and files |
| searchDirs | List\<Path\> | | The dir paths to search for property locations |
| ignoreCase | boolean | true | Ignore case in comparison of props to object fields |
| resolveVarsWithEnv | boolean | true | Resolve properties variables with system and environment |


### Property Substitution

`DuckParser` supports property substitution/variable expansion.

* Variables must be inside `${` and `}`. Literal markers have a backslash before the dollar sign:

  - `prop = ${foo} bar` is substituted but
  - `prop = \${foo} bar` is _not_

* Substituted variables _must_ have already been defined (no forward references):

    ```
    foo = fred
    bar = ${foo} and barney
    ```

  will result in `bar` being "fred and barney". But:

    ```
    bar = ${foo} and barney
    foo = fred
    ```

  will result in `bar` being "${foo} and barney" because if a variable is not found, it is not
  replaced (literal).


* Properties that have matching system and environmental variables are substituted unless the option
  `resolveVarsWithEnv` is set to false. E.g. this will give you what you expect:

    ```
    hello = Hi, my name is ${USER}
    ```


## Argument Parsing

You've already probably noticed, many times, that argument parsing is similar to properties files.
The both relate to configuration that are in string format and converted to your program
configuration in typed format. Quack-quack.


## Properties Resources and Locations

Properties are loaded from either or both the classpath and files. The client can also pass
explicit strings or properties as options. The rules:

* The `DuckParser` class has a `baseName` property either passed as an option or in the ctr:
    ```java
    // in ctr:
    DuckParser.build().with("name", $-> { .. }).create(..);

    // or in options:
    DuckParser.build().with($-> {
        $.baseName = "name";
        ..
    }).create(..);
    ```

* Properties are loaded in order:

    1. `classpath:<baseName>-default.properties`
    2. `classpath:<baseName>.properties`
    3. `file:<searchDirs>/<baseName>-default.properties`
    4. `file:<searchDirs>/<baseName>.properties`

    `<searchDirs>` is an option and is specified as an array of directory paths.  There are no
    default paths so this option must be set to look for properties files in the file system.


## Argument parsing

As shown in the Quick Start section, pass the argv object as the second argument:
`DuckParser.build().with(...).create(Interface.class, argv)` to parse arguments rather than search
for properties files.

### Types of arguments

There are 3 types of arguments:

1. **FLAG** &mdash; A boolean argument that either takes no arguments or only takes the values
   `true` or `false`. If this argument is _not_ specified, then the value is FALSE. If it is
   specified without an argument, then the value is TRUE. A FLAG argument can only be specified
   _once_, if it is specified multiple times, then the _last_ argument is the value.
   E.g.: (for argument `flag`)

    ```
    `-blah`       => flag() == false
    `-flag`       => flag() == true
    `-falg=false` => flag() == false
    `-flag=true`  => flag() == true

    `-flag=true -flag=false` => flag() == false
    `-flag=false -flag`      => flag() == true
    ```

2. **OPTIONAL** &mdash; An argument that takes an OPTIONAL argument. Can only be specified with
   a `@Default` annotation on the method. If the value is not specified, takes the default value.

3. **REQUIRED** &mdash; An argument that requires an argument. If an argument is not given, then an
   `IllegalStateException` is thrown.


### Annotations

There are 2 annotations that affect argument parsing: `@Default` and `@Alias`

* `@Default` &mdash; This is also interpreted by the properties loader. Provides a default value for
    the method if no argument was provided. But, for argument parsing, this implies an OPTIONAL
    argument.

* `@Alias` &mdash; Takes an array of aliases for this method. The method name is always the
    reference but the argument can also take any of the aliased forms. E.g.:

    ```
    @Alias({"f", "file"}) List<String> files();
     ..
    % cmd -f file1 --file file2 -files "file3,file4"
     ..
    argv.files() => { file1, file2, file3, file4 }
    ```


### Multiple specified arguments

Multiple specified arguments are converted to a CSV and then parsed as the type

    ```java
    interface Args {
        String[] arr();
        String str();
    }
    // parse `-arr a -arr b -str x -str y` to argv:
    assert argv.arr().length == 2;
    assert argv.arr()[0].equals("a") && argv.arr()[1].equals("b");
    assert argv.str().equals("x,y"); // CSV string
    ```

  See FLAG arguments below for multiple defined FLAG arguments which always return either true or
  false.


### Arguments

All regular arguments, that is, not part of an option is returned in `_args()` method. To access
these, your interface must include this method:

```java
    interface Args {
        String someArg();
        List<String> _args(); // can also be: String[], Set<String> (or concrete Set type)
        ..
    }
```

There are 2 special arguments:

* `--` (Double dash) &mdash; This is eaten by the parser and all arguments afterwards become regular
  arguments returned in `_args()`:

  - `-flag -- -foo bar` => `flag() == true`, `_args() == {"-foo", "bar"}`

* `-` (Single dash) &mdash; Always a flag option returned in `boolean _dash()`

  - `-` => `_dash() == true`


### Argument Syntax and Interpretation

Arguments can contain alphanumeric letters and dashes. An argument cannot start or end with a dash.

Dashes in arguments are converted to camel case for methods. For example, `-num-lines` is converted
to `numLines()`.


## Type Conversion

`DuckParser` has various conversion strategies to convert strings to types:

* Supports primitive and boxed types.
* Supports static creators `valueOf(String)` and `parse<TYPE>(String)` where `<TYPE>` is the name of
  the type to convert to (e.g. `parseFoo` parses to type `Foo`.)
    - Support enums using `valueOf()`
* Support arrays of supported types.
* Support collections of supported types. The collection types supported are:
    - `List` (default `ArrayList`)
    - `Set` (default `LinkedHashSet`)
    - `SortedSet` (default `TreeSet`)
* Custom Types
    - Any class with a string constructor is supported
    - `com.newbuck.duckparser.TypeParser.Parser<T>` store via `TypeParser.addParsers(Parser...)` are supported.
      This is a static store of custom parsers that convert a string to types.


### Arrays and Collections

If the return type of the interface is an array or Java Collection, then the value is interpreted as
a CSV and parsed to that array or collection type.

```java
interface Props {
    int[] intArray();
    List<String> strList();
    Set[] strSet();
}

Props props = DuckParser.build().with("name", $-> { .. }).create(Props.class);
```

For properties file:

```
int.array = 1,2,3
str.list = a, "b,c", d
str.set = x,y
```

The values returned would be: (pseudo assert functions)

```java
assertArrayEquals(new int[]{1,2,3}, props.intArray());
assertListEquals(Arrays.asList("a", "b,c", "d"), props.strList());
assertSetContains(Arrays.asList("x", "y"), props.strSet());
```

See further examples in tests.

The CSV format is from the standard: https://tools.ietf.org/html/rfc4180
<br/>There's more info here: https://en.wikipedia.org/wiki/Comma-separated_values

The standard is a little goofy, so there are some extensions:

* Default separator `,` (comma) and quote `"` (double quote) are from the standard but can be
    overridden.

* Spaces around the separator is supported and _always_ trimmed:
    `a, b`    => [`a`, `b`]
    `a, " b"` => [`a`, ` b`]

* Quotes can be anywhere in the field and all characters inside quotes, including commas and
  newlines, are escaped:

    `a","b`  => [`a,b`]
    `a"\n"b` => [`a\nb`]

* BUT, to escape a quote, the quote escape (2 quotes in a row) **must** be quoted, i.e. _inside_
  quotes: (this is from the standard)

    `a""b`   => [`ab`]
    `"a""b"` => [`a"b`]
    `a""""b` => [`a"b`]

    `"a"b",c$` => [`ab,c`]

    The last example has bad syntax: there are 3 quotes, so the value is unclosed and interpreted as
    a single field, usually this results in entire or partial file being mismatched.


## DuckParser Options and Merging of Properties

The same `DuckParser` class and logic for reading properties is used when parsing arguments.
Arguments are parsed after any properties files so arguments can override properties files.

```java
public class Example {
    interface Args {
        Integer fred();
    }

    @Test void test() {
        Args argv = DuckParser.build()
            .with($ -> {
                $.addProps = String.join("\n",
                        "fred = 13");
            })
            .create(Args.class, new String[] { "-fred", "42" });

        // args override properties
        assert argv.fred() == 42;
    }
```

The `DuckParser` class takes the same options for parsing arguments (argv) as parsing properties
(props loading.) When parsing only arguments, these rules apply:

| Option | Notes |
| --- | --- |
| baseName | Only affects props loading |
| prefix | Only affects props loading |
| initPropsMap | Only affects props loading |
| addProps | Only affects props loading |
| searchClasspath | Only affects props loading |
| searchDefault | Only affects props loading |
| searchDirs | Only affects props loading |
| ignoreCase | Only affects props loading |
| **resolveVarsWithEnv** | Affects both props and arg parsing |


## Properties and Argument Store (ParserStore)

All properties and arguments are accessible via the interface `ParserStore`. You can either use
the interface directly or extend from it and combine method access with store string access. Use
the method `as(Stringrop, Class<T>)` to access the properties and convert to type.

```java
ParserStore props = DuckParser.build()
    .with($ -> {
        $.addProps = String.join("\n",
                "str.val = "ikr");
    })
    .create(ParserStore.class);

    assertEquals("ikr", props.as("theKey", int.class));
    assertEquals("ikr", props.as("the_key", int.class));
    assertEquals("ikr", props.as("the.key", int.class));
```


## Comparisons to Other Libraries

There are a lot of choices for argument parsing.  See Dustin Marx's 30 part(!)
[series](https://github.com/dustinmarx/java-cli-demos) on Java CLI parsing.

There are various ways to load configuration files. Java doesn't seem to have the variety of
choices and file formats you see in other languages such as Python, Node, etc. Two popular
libraries for Java (as far as I can tell) are Luigi Viggiano's Owner library and Apache Commons
Configuration. (Spring Framework also has various choices.) [Owner](http://owner.aeonbits.org) uses
interfaces derived from a common class with optional annotations. `DuckParser` is similar in it's
use of interfaces to Owner. It creates a concrete class from the interface with the correct types
converted:

```java
import org.aeonbits.owner.Props;
import org.aeonbits.owner.ConfigFactory;

public interface ServerConfig extends Props {
    @Key("server.http.port")
    int port();

    String hostname();

    @DefaultValue("42")
    int maxThreads();
}
// create concrete class and load properties file
ServerConfig cfg = ConfigFactory.create(ServerConfig.class);
```

[Commons Configuration](https://commons.apache.org/proper/commons-configuration/) uses a central
configuration class that loads properties and has typed methods such as `getInt()` or `getString()`
asking for the properties by a string name:

```java
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.Configurations;

Configurations configs = new Configurations();
try {
    Configuration config = configs.properties(new File("config.properties"));
    // access configuration properties
    String dbHost = config.getString("database.host");
    int dbPort = config.getInt("database.port");
} catch (ConfigurationException cex) {
    // Something went wrong
}
```

There is also Spring Framework's [`@Value`
annotation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/factory/annotation/Value.html)
which uses annotations combined with the [Spring Expression
Language](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#expressions)
to set class fields.

Also, Spring Boot provides the [ConfigurationProperties
class](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/context/properties/ConfigurationProperties.html)
in which properties values are externalized and binds values.


## FAQ

_Ask Some Questions!_


## Updates

Date | Version | Note
--- | --- | ---
2018-10-30 | 0.9.2 | Uploaded to Maven Central
