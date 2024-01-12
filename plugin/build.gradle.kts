import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    `kotlin-dsl`
}

group = "com.storyteller_f.song"
version = "2.0"

repositories {
    mavenCentral()
    google()
}

val javaVersion = JavaVersion.VERSION_11
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }
}

gradlePlugin {
    plugins {
        // 声明插件信息，这里的 hello 名字随意
        register("song") {
            // 插件ID
            id = "com.storyteller_f.song"
            // 插件的实现类
            implementationClass = "com.storyteller_f.song.Song"
        }
    }
}

publishing {
    repositories {
        maven {
            // $rootDir 表示你项目的根目录
            setUrl("$rootDir/repo")
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    // https://mvnrepository.com/artifact/org.slf4j/slf4j-nop
    testImplementation("org.slf4j:slf4j-nop:2.0.9")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}