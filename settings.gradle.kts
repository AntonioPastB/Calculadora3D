// Proyecto con dos módulos:
//   nucleo     -> cálculos, pedido, historial y CSV (Java puro)
//   escritorio -> app de Windows (Swing)
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "Calculadora3D"
include(":nucleo", ":escritorio")
