module org.my.app {
    requires org.slf4j;
    requires org.my.lib;
    requires static org.apache.xmlbeans;

    exports org.my.app;
}