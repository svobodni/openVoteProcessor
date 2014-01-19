name := "openVoteProcessor"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaJpa,
  cache
)

libraryDependencies += "org.hibernate" % "hibernate-entitymanager" % "4.3.0.Final"

play.Project.playJavaSettings