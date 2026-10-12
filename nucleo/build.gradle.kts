// Código común a la app de escritorio y a la de Android. Solo usa funciones de
// Java disponibles en Android 8 (API 26) y se compila para Java 17.
plugins {
    `java-library`
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}
