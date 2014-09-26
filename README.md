**hotrod-endpoint: Use JDG remotely through Hotrod and cache-store DB**

**DOCS:**  
- http://infinispan.org/docs/5.3.x/user_guide/user_guide.html#_cache_loaders_and_stores  
- $INFINISPAN_HOME/docs/examples/configs

**SETUP:**

- create table JDG_residentials on mysql:  
```
CREATE TABLE `JDG_residentials` ( 
    `id` VARCHAR( 255 ) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL, 
    `datum` BINARY( 255 ) NOT NULL, 
    `version` BIGINT( 255 ) NOT NULL,
    PRIMARY KEY ( `id` )
)
CHARACTER SET = utf8
COLLATE = utf8_bin
ENGINE = INNODB;
```

- update property file 'jdg.properties' with correct binding address of JDG 5.3.0.Final
- run 1st JDG instance with  
```./clustered.sh -b your_IP -Djboss.node.name=nodeA```  
- optionally run 2nd JDG instance with  
```./clustered.sh -b your_IP -Djboss.socket.binding.port-offset=10000 -Djboss.node.name=nodeB```  


**THEN:**

- mvn clean package
- mvn exec:java


**EXTRA: If you want to test a 2-node cluster, increase os settings:**

- edit **/etc/sysctl.conf**  
```  
fs.aio-max-nr = 1048576  
fs.file-max = 6815744  
kernel.shmall = 2097152  
kernel.shmmax = 536870912  
kernel.shmmni = 4096  
# semaphores: semmsl, semmns, semopm, semmni  
kernel.sem = 250 32000 100 128  
net.ipv4.ip_local_port_range = 9000 65500  
net.core.rmem_default=262144  
net.core.rmem_max=4194304  
net.core.wmem_default=262144  
net.core.wmem_max=1048586  
```  
- execute /sbin/sysctl -p  

- edit **/etc/security/limits.conf**  
```
bwbuser              soft    nproc   2047  
bwbuser              hard    nproc   16384  
bwbuser              soft    nofile  2047  
bwbuser              hard    nofile  65536  
```
  
- **Add the following line to the "/etc/pam.d/login" file, if it does not already exist.**  
```
session    required     pam_limits.so
```
