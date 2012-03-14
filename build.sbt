name := "bootsmart"

scalacOptions ++= Seq("-unchecked", "-deprecation")

retrieveManaged := true

EclipseKeys.executionEnvironment := Some(EclipseExecutionEnvironment.JavaSE17)

EclipseKeys.withSource := true

EclipseKeys.configurations := Set(Compile)
