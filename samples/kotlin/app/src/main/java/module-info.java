module org.my.app {
    requires org.slf4j;
    requires org.my.lib;
    requires static org.apache.xmlbeans;

    requires kotlin.stdlib;

    exports org.my.app;
}