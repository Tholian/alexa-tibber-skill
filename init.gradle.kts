initscript {
    repositories {
        maven { url = uri("https://plugins.gradle.org/m2") }
    }
    dependencies { classpath("org.openrewrite:plugin:7.30.0") }
}
rootProject {
    plugins.apply(org.openrewrite.gradle.RewritePlugin::class.java)
    dependencies {
        add("rewrite", "org.openrewrite.recipe:rewrite-migrate-java:3.33.0")
        add("rewrite", "org.openrewrite:rewrite-java")
    }
    extensions.configure<org.openrewrite.gradle.RewriteExtension> {
        activeRecipe("org.openrewrite.java.migrate.UpgradeToJava25")
        activeRecipe("org.openrewrite.java.migrate.lombok.LombokBestPractices")
        activeRecipe("org.openrewrite.gradle.GradleBestPractices")
        setExportDatatables(true)
    }
    afterEvaluate {
        if (repositories.isEmpty()) {
            repositories {
                mavenCentral()
            }
        }
    }
}
