module org.my.app {
    requires org.my.lib;
    requires org.slf4j;

    requires static org.apache.xmlbeans;

    requires /*runtime*/ org.slf4j.simple;

    exports org.my.app;
}