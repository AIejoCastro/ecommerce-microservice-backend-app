# Resumen de Manifiestos de Kubernetes

## ✅ Estado Actual

Todos los manifiestos de Kubernetes para los tres ambientes (DEV, STAGE, PROD) están correctamente configurados y listos para usar.

## 📁 Estructura de Directorios

```
kubernetes/
├── dev/       ← Ambiente de Desarrollo
├── stage/     ← Ambiente de Staging  
└── prod/      ← Ambiente de Producción
```

## 🐳 Imágenes Docker Configuradas

### DEV (desarrollo)
```yaml
image: alejocastro/[servicio]-ecommerce-boot:dev-latest
```

### STAGE (staging)
```yaml
image: alejocastro/[servicio]-ecommerce-boot:stage-latest
```

### PROD (producción)
```yaml
image: alejocastro/[servicio]-ecommerce-boot:prod-latest
```

## 📊 Servicios por Ambiente

Todos los ambientes incluyen los siguientes 8 servicios:

### Servicios de Infraestructura
1. **cloud-config** - Servidor de configuración centralizada
2. **service-discovery** - Eureka para service discovery
3. **zipkin** - Distributed tracing

### Servicios de Aplicación
4. **api-gateway** - Gateway de entrada
5. **user-service** - Gestión de usuarios
6. **product-service** - Gestión de productos
7. **order-service** - Gestión de pedidos
8. **payment-service** - Gestión de pagos
9. **shipping-service** - Gestión de envíos

## 🔧 Configuración por Ambiente

### 📊 Réplicas

| Servicio | DEV | STAGE | PROD |
|----------|-----|-------|------|
| api-gateway | 1 | 2 | 3 |
| cloud-config | 1 | 1 | 2 |
| service-discovery | 1 | 1 | 2 |
| user-service | 1 | 2 | 2 |
| product-service | 1 | 2 | 2 |
| order-service | 1 | 2 | 2 |
| payment-service | 1 | 2 | 2 |
| shipping-service | 1 | 2 | 2 |

### 💾 Recursos (API Gateway como ejemplo)

#### DEV
```yaml
resources:
  requests:
    cpu: 150m
    memory: 256Mi
  limits:
    cpu: 300m
    memory: 512Mi
```

#### STAGE
```yaml
resources:
  requests:
    cpu: 200m
    memory: 256Mi
  limits:
    cpu: 400m
    memory: 512Mi
```

#### PROD
```yaml
resources:
  requests:
    cpu: 300m
    memory: 512Mi
  limits:
    cpu: 600m
    memory: 1Gi
```

### 🔍 Health Checks

Todos los servicios tienen configurados:
- **readinessProbe** - Verifica cuándo el pod está listo para recibir tráfico
- **livenessProbe** - Verifica si el pod está funcionando correctamente

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health
    port: [service-port]
  initialDelaySeconds: 20-90
  periodSeconds: 10-15
  
livenessProbe:
  httpGet:
    path: /actuator/health
    port: [service-port]
  initialDelaySeconds: 40-120
  periodSeconds: 30
```

## 🌍 Variables de Entorno (ConfigMaps)

### DEV (`kubernetes/dev/configmap.yaml`)
```yaml
SPRING_PROFILES_ACTIVE: "dev"
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: "http://service-discovery:8761/eureka/"
SPRING_CONFIG_IMPORT: "optional:configserver:http://cloud-config:9296/"
```

### STAGE (`kubernetes/stage/configmap.yaml`)
```yaml
SPRING_PROFILES_ACTIVE: "stage"
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: "http://service-discovery:8761/eureka/"
SPRING_CONFIG_IMPORT: "optional:configserver:http://cloud-config:9296/"
```

### PROD (`kubernetes/prod/configmap.yaml`)
```yaml
SPRING_PROFILES_ACTIVE: "prod"
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: "http://service-discovery:8761/eureka/"
SPRING_CONFIG_IMPORT: "optional:configserver:http://cloud-config:9296/"
```

## 🚀 Cómo Desplegar

### 1. Crear Namespaces
```bash
kubectl create namespace ecommerce-dev
kubectl create namespace ecommerce-stage
kubectl create namespace ecommerce-prod
```

### 2. Desplegar en DEV
```bash
kubectl apply -f kubernetes/dev/
```

### 3. Desplegar en STAGE
```bash
kubectl apply -f kubernetes/stage/
```

### 4. Desplegar en PROD
```bash
kubectl apply -f kubernetes/prod/
```

## 🔍 Verificación del Despliegue

### Ver pods en cada ambiente
```bash
# DEV
kubectl get pods -n ecommerce-dev

# STAGE
kubectl get pods -n ecommerce-stage

# PROD
kubectl get pods -n ecommerce-prod
```

### Ver servicios
```bash
# DEV
kubectl get svc -n ecommerce-dev

# STAGE
kubectl get svc -n ecommerce-stage

# PROD
kubectl get svc -n ecommerce-prod
```

### Port-forward para acceso local

#### Eureka (Service Discovery)
```bash
# DEV
kubectl port-forward -n ecommerce-dev svc/service-discovery 8761:8761

# STAGE
kubectl port-forward -n ecommerce-stage svc/service-discovery 8762:8761

# PROD
kubectl port-forward -n ecommerce-prod svc/service-discovery 8763:8761
```

#### API Gateway
```bash
# DEV
kubectl port-forward -n ecommerce-dev svc/api-gateway 8081:8080

# STAGE
kubectl port-forward -n ecommerce-stage svc/api-gateway 8082:8080

# PROD
kubectl port-forward -n ecommerce-prod svc/api-gateway 8083:8080
```

## 📝 Archivos Principales

Cada directorio contiene:
- `namespace.yaml` - Define el namespace
- `configmap.yaml` - Variables de entorno compartidas
- `[servicio]-deployment.yaml` - Deployment + Service para cada microservicio

## ⚙️ Configuración de Jenkins

Los Jenkinsfiles están configurados para usar estos manifiestos:

### Jenkinsfile (DEV)
```groovy
kubectl apply -f kubernetes/dev/[servicio]-deployment.yaml
```

### Jenkinsfile-stage (STAGE)
```groovy
kubectl apply -f kubernetes/stage/[servicio]-deployment.yaml
```

### Jenkinsfile-prod (PROD)
```groovy
kubectl apply -f kubernetes/prod/[servicio]-deployment.yaml
```

## 🎯 Diferencias Clave entre Ambientes

### DEV
- ✅ Mínimas réplicas (1 por servicio)
- ✅ Recursos limitados (para desarrollo rápido)
- ✅ imagePullPolicy: IfNotPresent
- ✅ Profile: `dev`

### STAGE
- ✅ Réplicas moderadas (2 por servicio de app)
- ✅ Recursos medios (simula producción)
- ✅ imagePullPolicy: IfNotPresent
- ✅ Profile: `stage`
- ✅ Labels: `environment: staging`

### PROD
- ✅ Máximas réplicas (3 para gateway, 2 para servicios)
- ✅ Recursos altos (alta disponibilidad)
- ✅ imagePullPolicy: IfNotPresent
- ✅ Profile: `prod`
- ✅ Labels: `environment: production`

## 🔒 Notas de Seguridad

Para producción real (no local), considera:
- Usar **Secrets** para credenciales sensibles (no ConfigMaps)
- Configurar **Network Policies** para aislar los servicios
- Implementar **RBAC** (Role-Based Access Control)
- Usar **TLS/SSL** para comunicaciones internas
- Configurar **Resource Quotas** a nivel de namespace
- Implementar **Pod Security Policies**

## 📚 Referencias

- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
- [Eureka on Kubernetes](https://github.com/spring-cloud/spring-cloud-netflix)

---

✅ **Status**: Todos los manifiestos están actualizados y listos para usar
🗓️ **Última actualización**: 3 de noviembre de 2025
👤 **Configurado por**: Alejandro Castro
