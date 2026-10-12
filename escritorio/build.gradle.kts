// App de escritorio (Swing). También se puede compilar y empaquetar sin Gradle
// con empaquetar.ps1, o ejecutar desde Eclipse.
plugins {
    application
}

dependencies {
    implementation(project(":nucleo"))
}

application {
    mainClass.set("com.impresion3d.Main")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}
