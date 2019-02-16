name := "bootsmart"

ThisBuild / scalaVersion := "2.12.8"

scalacOptions ++= Seq("-unchecked", "-deprecation")

retrieveManaged := true

libraryDependencies += "org.scala-lang.modules" % "scala-xml_2.12" % "1.1.1"
