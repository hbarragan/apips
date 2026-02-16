# Montar sso

**Lanza el `docker-compose`** y automáticamente Nexus se ejecutará con los datos del volumen descomprimido.
> **Nota**: Para ejecutar docker-compose, accede a la carpeta `./docker` y ejecuta el siguiente comando:
```bash
docker-compose -p "adasoft-api-ps" up -d
```

# Configuración dev hacia remoto


Existan en la maquina local certificados rovi para arrancar con la run 3-dev

C:\Adasoft\certs\SERVER144.p12

 - ca.crt
 - fta_truststore.jks
 - fta_truststore.p12
 - SERVER144.crt
 - SERVER144.p12

Modificar el host
C:\Windows\System32\drivers\etc\hosts

192.168.5.19 server144
192.168.5.19 SERVER144

Abrir swagger por SERVER144

https://server144:9101/swagger-ui/index.html
http://server144:9101/swagger-ui/index.html


