name := "TwitchKarma"

version := "1.0"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Typesafe repository releases"  at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype repo"                 at "https://oss.sonatype.org/content/groups/scala-tools/",
  "Sonatype releases"             at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots"            at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype staging"              at "http://oss.sonatype.org/content/repositories/staging",
  "Java.net Maven2 Repository"    at "http://download.java.net/maven/2/",
  "Twitter Repository"            at "http://maven.twttr.com",
  Resolver.bintrayRepo("websudos", "oss-releases")
)

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  val playV = "2.4.4"
  val sparkV = "1.4.1"
  val phantomV = "1.11.0"
  Seq(
    "io.spray"           %% "spray-can"                 % sprayV,
    "io.spray"           %% "spray-client"              % sprayV,
    "io.spray"           %% "spray-routing-shapeless2"  % sprayV,
    "io.spray"           %% "spray-caching"             % sprayV,
    "io.spray"           %% "spray-testkit"             % sprayV   % "test",
    "com.typesafe.akka"  %% "akka-actor"                % akkaV,
    "com.typesafe.akka"  %% "akka-testkit"              % akkaV    % "test",
    "com.websudos"       %% "phantom-dsl"               % phantomV exclude ("com.chuusai", "shapeless"),
    "com.websudos"       %% "phantom-connectors"        % phantomV,
    "com.websudos"       %% "phantom-testkit"           % phantomV % "test, provided",
    "org.slf4j" % "slf4j-simple" % "1.7.14"
  )
}
