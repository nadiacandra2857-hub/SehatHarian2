#!/usr/bin/env python3
"""Compile all Java sources and run isolated tests using already cached dependencies.

Does not download dependencies or run Gradle, the UI, database bootstrap or seed.
--jdbc explicitly runs the separate database probe instead of the isolated suite.
"""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parent.parent
SAFE_TESTS = """
AccountRepositoryTest AuthServiceTest PasswordUtilTest PenggunaServiceTest
KebiasaanServiceTest TidurServiceTest TantanganServiceTest EvaluasiServiceTest
ProgresHarianServiceTest RekomendasiServiceTest OwnershipServiceTest
WriteConfirmationTest CatalogReadTest UserSessionTest FrontendFlowValidationTest
FrontendFeatureFlowIntegrationTest InstanceContextTest ReminderLifecycleTest
MainLayoutContractTest FrontendOwnershipTest WriteActionTest WriteViewContractTest
CatalogPreviewTest ExerciseCatalogTest
""".split()

RUNNER = r"""
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.engine.discovery.DiscoverySelectors;
public class SourceTestRunner {
    public static void main(String[] names) {
        var request = LauncherDiscoveryRequestBuilder.request()
            .configurationParameter("junit.jupiter.execution.parallel.enabled", "false");
        for (String name : names) request.selectors(DiscoverySelectors.selectClass("com.sehatharian." + name));
        var listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request.build(), listener);
        var summary = listener.getSummary();
        var writer = new java.io.PrintWriter(System.out, true);
        summary.printTo(writer);
        summary.printFailuresTo(writer);
        if (summary.getTestsFailedCount() > 0 || summary.getContainersFailedCount() > 0
                || summary.getTestsSucceededCount() == 0) System.exit(1);
    }
}
"""


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dependency-cache", type=Path, default=Path.home() / ".gradle/caches/modules-2/files-2.1")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--jdbc", action="store_true")
    args = parser.parse_args()
    if args.jdbc and not os.getenv("SEHATHARIAN_JDBC_TEST_URL"):
        parser.error("--jdbc requires SEHATHARIAN_JDBC_TEST_URL and a dedicated sehatharian_test_* schema")
    output = (args.output or Path(tempfile.mkdtemp(prefix="sehatharian-source-tests-"))).resolve()
    output.mkdir(parents=True, exist_ok=True)
    groups = ["org.openjfx", "org.mindrot", "com.mysql", "one.jpro",
              "org.junit.jupiter", "org.junit.platform", "org.opentest4j", "org.apiguardian"]
    jars = sorted(p for group in groups for p in (args.dependency_cache / group).rglob("*.jar")
                  if not p.name.endswith(("-sources.jar", "-javadoc.jar")))
    if not jars:
        parser.error("Dependency cache is empty; install project dependencies with Gradle first")
    classpath = os.pathsep.join(map(str, jars))
    print(f"Evidence: {output}", flush=True)

    def run(name, command):
        with (output / f"{name}.log").open("w") as log:
            result = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
        print((output / f"{name}.log").read_text(), end="", flush=True)
        if result.returncode:
            raise SystemExit(result.returncode)

    for phase in ["main", "test"]:
        target = output / phase
        target.mkdir(exist_ok=True)
        files = sorted(p for module in ["Backend2", "Frontend2"]
                       for p in (ROOT / module / "src" / phase / "java").rglob("*.java"))
        run(f"compile-{phase}", ["javac", "--release", "27", "-encoding", "UTF-8", "-cp",
            classpath + os.pathsep + str(output / "main"), "-d", str(target), *map(str, files)])
        print(f"Compiled {len(files)} {phase} files", flush=True)
    for module in ["Backend2", "Frontend2"]:
        shutil.copytree(ROOT / module / "src/main/resources", output / "main", dirs_exist_ok=True)
    runner = output / "SourceTestRunner.java"
    runner.write_text(RUNNER)
    runtime = os.pathsep.join([classpath, str(output / "main"), str(output / "test")])
    run("compile-runner", ["javac", "-cp", runtime, "-d", str(output / "test"), str(runner)])
    tests = ["JdbcTransactionIntegrationTest"] if args.jdbc else SAFE_TESTS
    run("junit", ["java", "--enable-native-access=ALL-UNNAMED", "-cp", runtime, "SourceTestRunner", *tests])


if __name__ == "__main__":
    main()
