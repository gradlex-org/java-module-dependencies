module org.my.app {
    requires org.my.lib;
    requires org.slf4j;

    requires static org.apache.xmlbeans;

    exports org.my.app;
}