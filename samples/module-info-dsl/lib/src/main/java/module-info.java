module org.example.lib {
    requires transitive com.fasterxml.jackson.databind;

    requires java.logging; // JDK module

    exports org.example.lib;
}