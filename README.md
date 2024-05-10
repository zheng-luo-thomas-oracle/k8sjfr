### Customers action required:

- Deploy the collector to the cluster with `pods` read access
- Restart **customers' java applications** with jmxremote flags:
```
  - Dcom.sun.management.jmxremote=true 
  - Dcom.sun.management.jmxremote.port=<> 
  - Dcom.sun.management.jmxremote.rmi.port=<> 
  - Dcom.sun.management.jmxremote.local.only=false 
  - Dcom.sun.management.jmxremote.authenticate=<> 
  - Dcom.sun.management.jmxremote.ssl=<>
```


---
### TODO:

1. Integrate with JMS
   - Trigger recording from JMS Console
   - Upload recording to Object Storage
2. Integrate with JMS java agent
   - Customers action required - With Java Agent?
     - Deploy the collector to the cluster with `pods` read access
     - Download **JMS java agent** and run it with jmxremote flags (feasible?)
3. Allow user to configure the recordings:
   - Length of recording, max size, output file name
4. Auto-detect open JMX connections
   - Detect open ports on 9091?
5. Authentication/SSL

---
- use Oracle Image for Java 22
- crypto road map demo
- try to see how we can get to deploy into production. ease of use.
- try with ingress controller? 
- Authentication 
- ingress connector ?
- Update JIRA