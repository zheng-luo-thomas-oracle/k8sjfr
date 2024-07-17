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
