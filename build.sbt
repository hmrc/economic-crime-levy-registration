import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys

val appName = "economic-crime-levy-registration"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(SbtDistributablesPlugin.publishingSettings: _*)
  .settings(inConfig(Test)(testSettings): _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(itSettings): _*)
  .settings(majorVersion := 0)
  .settings(ThisBuild / useSuperShell := false)
  .settings(scoverageSettings: _*)
  .settings(
    scalaVersion := "2.12.15",
    name := appName,
    RoutesKeys.routesImport ++= Seq(
      "uk.gov.hmrc.economiccrimelevyregistration.models._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
    ),
    PlayKeys.playDefaultPort := 14001,
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    (update / evictionWarningOptions).withRank(KeyRanks.Invisible) :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    resolvers ++= Seq(Resolver.jcenterRepo)
  )

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  unmanagedSourceDirectories := Seq(
    baseDirectory.value / "test",
    baseDirectory.value / "test-common"
  ),
  fork := true
)

lazy val itSettings = Defaults.itSettings ++ Seq(
  unmanagedSourceDirectories := Seq(
    baseDirectory.value / "it",
    baseDirectory.value / "test-common"
  ),
  parallelExecution := false,
  fork := true
)

val excludedScoveragePackages: Seq[String] = Seq(
  "<empty>",
  "Reverse.*",
  ".*handlers.*",
  "uk.gov.hmrc.BuildInfo",
  "app.*",
  "prod.*",
  ".*Routes.*",
  "testOnly.*",
  "testOnlyDoNotUseInAppConf.*"
)

val scoverageSettings: Seq[Setting[_]] = Seq(
  ScoverageKeys.coverageExcludedFiles := excludedScoveragePackages.mkString(";"),
  ScoverageKeys.coverageMinimumStmtTotal := 80,
  ScoverageKeys.coverageFailOnMinimum := true,
  ScoverageKeys.coverageHighlighting := true,
  scalacOptions ++= Seq(
    "-feature",
    "-Ypartial-unification",
    "-rootdir",
    baseDirectory.value.getCanonicalPath,
    "-Wconf:cat=deprecation:ws,cat=feature:ws,cat=optimizer:ws,src=target/.*:s"
  )
)

addCommandAlias("runAllChecks", ";clean;compile;scalafmtCheckAll;coverage;test;it:test;scalastyle;coverageReport")
