hotrod-endpoint: Use JDG remotely through Hotrod and cache-store DB

SETUP:

- create table JDG_residentials on mysql;
- update property file with correct binding address of JDG 5.3.0.Final

THEN:

- mvn clean package
- mvn exec:java
