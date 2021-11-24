module org.my.lib {
    requires transitive com.fasterxml.jackson.databind;

    // JDK modules
    requires java.logging;
    requires jdk.charsets;
}