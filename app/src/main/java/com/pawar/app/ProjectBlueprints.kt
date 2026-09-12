package com.pawar.app

/**
 * Deterministic starter templates. These are scaffolds, not full application
 * implementations; a reasoning/code-generation capability can extend them.
 */
object ProjectBlueprintPlanner {
    fun blueprint(profile: TaskProfile): ProjectBlueprint? {
        if (profile.intent != TaskIntent.CREATE) return null

        val templates = when (profile.projectType) {
            ProjectType.ANDROID -> mapOf(
                "settings.gradle.kts" to """pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\ndependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }\nrootProject.name = \"StarterApp\"\ninclude(\":app\")\n""",
                "build.gradle.kts" to """plugins {\n    id(\"com.android.application\") version \"9.4.0\" apply false\n    id(\"org.jetbrains.kotlin.plugin.compose\") version \"2.3.21\" apply false\n}\n""",
                "gradle.properties" to """org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8\nandroid.useAndroidX=true\n""",
                "app/build.gradle.kts" to """plugins {\n    id(\"com.android.application\")\n    id(\"org.jetbrains.kotlin.plugin.compose\")\n}\n\nandroid { namespace = \"com.example.starter\"; compileSdk = 37\n    defaultConfig { applicationId = \"com.example.starter\"; minSdk = 24; targetSdk = 37; versionCode = 1; versionName = \"1.0\" }\n    buildFeatures { compose = true }\n}\n\ndependencies {\n    val composeBom = platform(\"androidx.compose:compose-bom:2026.08.00\")\n    implementation(composeBom)\n    implementation(\"androidx.core:core-ktx:1.19.0\")\n    implementation(\"androidx.activity:activity-compose:1.13.0\")\n    implementation(\"androidx.compose.material3:material3\")\n    implementation(\"androidx.compose.ui:ui\")\n    implementation(\"androidx.compose.foundation:foundation\")\n}\n""",
                "app/src/main/AndroidManifest.xml" to """<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n    <application android:theme=\"@style/Theme.Starter\" android:label=\"StarterApp\">\n        <activity android:name=\".MainActivity\" android:exported=\"true\">\n            <intent-filter>\n                <action android:name=\"android.intent.action.MAIN\" />\n                <category android:name=\"android.intent.category.LAUNCHER\" />\n            </intent-filter>\n        </activity>\n    </application>\n</manifest>\n""",
                "app/src/main/res/values/styles.xml" to """<resources><style name=\"Theme.Starter\" parent=\"android:style/Theme.Material.Light.NoActionBar\" /></resources>\n""",
                "app/src/main/java/com/example/starter/MainActivity.kt" to """package com.example.starter\n\nimport android.os.Bundle\nimport androidx.activity.ComponentActivity\nimport androidx.activity.compose.setContent\nimport androidx.compose.material3.Text\n\nclass MainActivity : ComponentActivity() {\n    override fun onCreate(savedInstanceState: Bundle?) {\n        super.onCreate(savedInstanceState)\n        setContent { Text(\"Starter project\") }\n    }\n}\n"""
            )
            ProjectType.WEB -> mapOf(
                "index.html" to """<!doctype html>
<html lang=\"en\">
<head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Starter project</title><link rel=\"stylesheet\" href=\"src/style.css\"></head>
<body><main><h1>Starter project</h1><p>Ready for implementation.</p></main><script type=\"module\" src=\"src/main.js\"></script></body>
</html>
""",
                "src/main.js" to "document.querySelector('main')?.setAttribute('data-ready', 'true');\n",
                "src/style.css" to "body { margin: 0; font-family: system-ui, sans-serif; }\nmain { padding: 32px; }\n"
            )
            ProjectType.PYTHON -> mapOf(
                "pyproject.toml" to """[project]\nname = \"starter-python\"\nversion = \"0.1.0\"\nrequires-python = \">=3.10\"\n""",
                "src/main.py" to "def main() -> None:\n    print('Starter project')\n\nif __name__ == '__main__':\n    main()\n",
                "tests/test_main.py" to "def test_smoke():\n    assert True\n",
                "README.md" to "# Starter Python project\n\nReady for implementation.\n"
            )
            ProjectType.NATIVE -> mapOf(
                "CMakeLists.txt" to "cmake_minimum_required(VERSION 3.20)\nproject(starter_native)\nset(CMAKE_CXX_STANDARD 17)\nadd_executable(starter src/main.cpp)\n",
                "src/main.cpp" to "#include <iostream>\nint main() { std::cout << \"Starter project\\n\"; return 0; }\n",
                "include/app.h" to "#pragma once\n",
                "README.md" to "# Starter C++ project\n"
            )
            ProjectType.JVM -> mapOf(
                "settings.gradle.kts" to "rootProject.name = \"starter-jvm\"\n",
                "build.gradle.kts" to "plugins { kotlin(\"jvm\") version \"2.3.21\" }\nrepositories { mavenCentral() }\n",
                "src/main/kotlin/Main.kt" to "fun main() = println(\"Starter project\")\n",
                "src/test/kotlin/MainTest.kt" to "import kotlin.test.Test\nimport kotlin.test.assertTrue\nclass MainTest { @Test fun smoke() { assertTrue(true) } }\n"
            )
            ProjectType.RUST -> mapOf(
                "Cargo.toml" to "[package]\nname = \"starter-rust\"\nversion = \"0.1.0\"\nedition = \"2021\"\n",
                "src/main.rs" to "fn main() { println!(\"Starter project\"); }\n",
                "tests/smoke.rs" to "#[test]\nfn smoke() { assert!(true); }\n"
            )
            ProjectType.GO -> mapOf(
                "go.mod" to "module example.com/starter\n\ngo 1.23\n",
                "main.go" to "package main\nimport \"fmt\"\nfunc main() { fmt.Println(\"Starter project\") }\n",
                "main_test.go" to "package main\nimport \"testing\"\nfunc TestSmoke(t *testing.T) {}\n"
            )
            ProjectType.GENERIC -> mapOf(
                "README.md" to "# Starter project\n\nThis is a generic scaffold ready for implementation.\n",
                "src/main" to "",
                "tests/README.md" to "# Tests\n"
            )
        }
        return ProjectBlueprint(profile.projectType, templates.keys.toList(), templates)
    }
}

data class ProjectBlueprint(
    val projectType: ProjectType,
    val initialFiles: List<String>,
    val templates: Map<String, String> = initialFiles.associateWith { "" }
)
