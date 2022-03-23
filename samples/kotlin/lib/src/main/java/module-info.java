module org.my.lib {
    requires transitive com.fasterxml.jackson.databind;

    requires kotlin.stdlib;

    // JDK modules
    requires java.logging;
    requires jdk.charsets;
}