# Drools expert and fusion usage examples
(this project has been forked from [this original one](https://github.com/dacr/scala-drools-dummy-project))


```
$ sbt test
$ sbt run
$ sbt universal:packageBin
```

Some advices :

- don't use fat jar with drools, switch to sbt-native-packager
- Scala case classes are perfect for your rules
- Use only java collections within the classes used by your rules.
  Avoid the scala collections in that precise case but rely on
  collection.JavaConversions._ implicits to hide that restriction.
- In knowledge bases only use declarative classes (declare) for
  internal usage, such as intermediary reasoning state.
- To change the java compiler used by drools, use the following 
  system property : `"drools.dialect.java.compiler=JANINO"`.
  It will replace ECJ by Apache Janino, but you will have to
  provide the dependency yourself : 
  "org.codehaus.janino" % "janino" % "2.5.16" (Use only this release,
  at least up to Drools 6.4.0.Final, to be check again later)

