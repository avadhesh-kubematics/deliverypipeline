import sbtdocker.ImageName
import sbtdocker.Plugin.DockerKeys._
import sbtdocker.mutable.Dockerfile

dockerSettings

//val gitHeadCommitSha = settingKey[String]("current git commit SHA")
//
//gitHeadCommitSha in ThisBuild := Process("git rev-parse HEAD").lines.head
//
//version in ThisBuild := gitHeadCommitSha.value

// Make docker depend on the package task, which generates a jar file of the application code
docker <<= docker.dependsOn(Keys.`package`.in(Compile, packageBin))

// Define a Dockerfile
dockerfile in docker <<= (artifactPath.in(Compile, packageBin), managedClasspath in Compile, mainClass.in(Compile, packageBin)) map {
  case (jarFile, managedClasspath, Some(mainClass)) =>
    val libs = "/app/libs"
    val jarTarget = "/app/" + jarFile.name
    new Dockerfile {
      // Use a base image that contain Java
      from("dockerfile/java")
      // Expose port 8080
      expose(8080)
      // Copy all dependencies to 'libs' in stage dir
      managedClasspath.files.foreach { depFile =>
        val target = file(libs) / depFile.name
        copyToStageDir(depFile, target)
      }
      // Add the libs dir
      add(libs, libs)
      // Add the generated jar file
      add(jarFile, jarTarget)
      // The classpath is the 'libs' dir and the produced jar file
      val classpath = s"$libs/*:$jarTarget"
      // Set the entry point to start the application using the main class
      cmd("java", "-cp", classpath, mainClass)
    }
}

imageName in docker := {
  ImageName(
    namespace = Some("lachatak"),
    repository = name.value,
    tag = Some(version.value))
}