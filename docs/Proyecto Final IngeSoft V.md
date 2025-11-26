# Proyecto Final IngeSoft V

---

## Repositorio y Organización

Link al repositorio de desarollo: https://github.com/AIejoCastro/ecommerce-microservice-backend-app.git

Link al repositorio de infraestructura: https://github.com/AIejoCastro/ecommerce-microservice-infra.git

### Política de commits y branching

Estrategia: **GitFlow**

Branches principales:

- `dev` → desarrollo
- `stage` → pruebas/preproducción
- `master/main` → producción

Commits descriptivos usando prefijos:

- `feat:` nueva funcionalidad
- `fix:` corrección de errores
- `docs:` documentación
- `chore:` tareas generales
- `refactor:` refactor de código
- `test:` pruebas

---

## Infraestructura con Terraform

Este proyecto utiliza **Terraform** para desplegar toda la infraestructura en AWS de manera reproducible y modular.

### Repositorio

El código Terraform se encuentra en el repositorio:

`https://github.com/AIejoCastro/ecommerce-microservice-infra.git`

### Infraestructura creada

![AWS Cloud Architecture.jpg](Proyecto%20Final%20IngeSoft%20V/AWS_Cloud_Architecture.jpg)

Terraform despliega la siguiente infraestructura:

- **Red (NETWORK)**: VPC, subnets públicas y privadas, NAT Gateway, Internet Gateway, tablas de ruteo.
- **Compute (COMPUTE)**: Cluster EKS, Node Groups, roles IAM, Security Groups, Add-ons EKS (CoreDNS, kube-proxy, VPC CNI).
- **Observabilidad (OBSERVABILITY)**: Helm releases para Metrics Server, Prometheus, Grafana, Elasticsearch, Kibana, Logstash, Filebeat y AWS Load Balancer Controller.
    
    ![image.png](Proyecto%20Final%20IngeSoft%20V/image.png)
    
    ```xml
    # Network Module
    module "network" {
      source = "./modules/network"
    
      project_name         = var.project_name
      environment          = var.environment
      region               = var.region
      vpc_cidr             = var.vpc_cidr
      availability_zones   = var.availability_zones
      public_subnet_cidrs  = var.public_subnet_cidrs
      private_subnet_cidrs = var.private_subnet_cidrs
      enable_nat_gateway   = var.enable_nat_gateway
    
      tags = local.common_tags
    }
    
    # Compute Module (EKS)
    module "compute" {
      source = "./modules/compute"
    
      project_name        = var.project_name
      environment         = var.environment
      region              = var.region
      vpc_id              = module.network.vpc_id
      private_subnet_ids  = module.network.private_subnet_ids
      public_subnet_ids   = module.network.public_subnet_ids
      cluster_version     = var.cluster_version
      node_group_config   = var.node_group_config
    
      tags = local.common_tags
    
      depends_on = [module.network]
    }
    
    # Observability Module - TEMPORARILY DISABLED DUE TO HELM SYNTAX ISSUES
    module "observability" {
      source = "./modules/observability"
      
      project_name                        = var.project_name
      environment                         = var.environment
      region                              = var.region
      cluster_name                        = module.compute.cluster_name
      cluster_endpoint                    = module.compute.cluster_endpoint
      cluster_certificate_authority_data  = module.compute.cluster_certificate_authority_data
      oidc_provider_arn                   = module.compute.oidc_provider_arn
      prometheus_retention_days           = var.prometheus_retention_days
      prometheus_storage_size             = var.prometheus_storage_size
      elasticsearch_node_count            = var.elasticsearch_node_count
      elasticsearch_storage_size          = var.elasticsearch_storage_size
      elasticsearch_retention_days        = var.elasticsearch_retention_days
      alb_controller_role_arn = module.compute.alb_controller_role_arn
      vpc_id              = module.network.vpc_id
    
      
      tags = local.common_tags
      
      depends_on = [module.compute]
    }
    
    ```
    
    ---
    
    Desde el archivo `main.tf` se hace el llamado a todos los modulos. Asi que algun cambio en un modulo unicamente se debe hacer dentro del modulo dentro del recurso que se quiere cambiar. Realizando una IaC escalable.
    

### Ambientes

- **dev**, **stage**, **prod**: Cada uno tiene su archivo `terraform.tfvars` con configuración de red, EKS y observabilidad.

Cada uno tiene su propio archivo `.tfvars`, donde cambian principalmente:

- **Tamaño del cluster EKS**
    - dev → 1 nodo pequeño
    - stage → 2 nodos medianos
    - prod → 3+ nodos y autoscaling
- **Cantidad y tamaño de subnets**
- **Retención de logs y métricas**
- **Configuración de observabilidad**
- **Versiones de imágenes**
- **Niveles de acceso y permisos**
- **Costos asignados**

`dev-us-east-1.tfvars`

```xml

# General Configuration
project_name = "ecommerce"
environment  = "dev"
region       = "us-east-1"
cost_center  = "engineering"
team         = "platform"

# EKS Configuration
cluster_version = "1.29"
node_group_config = {
  desired_size   = 2
  max_size       = 4
  min_size       = 2
  instance_types = ["t3.medium"]
  capacity_type  = "ON_DEMAND"
  disk_size      = 20
}

}

```

`prod-us-east-1.tfvars`

```xml
# Production Environment Configuration
# us-east-1 region

# General Configuration
project_name = "ecommerce"
environment  = "prod"
region       = "us-east-1"
cost_center  = "engineering"
team         = "platform"

# EKS Configuration
cluster_version = "1.29"
node_group_config = {
  desired_size   = 6
  max_size       = 10
  min_size       = 5
  instance_types = ["m7i-flex.large", "c7i-flex.large", "t3.small"]  
  capacity_type  = "ON_DEMAND"
  disk_size      = 100
}
```

---

En los anteriores fragmentos vemos como cambian el tipo de instancia y la cantidad de nodos según el ambiente en el que se despliegue

### Organización modular

- `modules/network`: Subnets, VPC, NAT, IGW, route tables.
- `modules/compute`: EKS cluster, Node Groups, IAM roles, security groups.
- `modules/observability`: Helm charts para monitoreo y logging.

### Backend remoto (estado remoto)

El estado de Terraform (**terraform.tfstate**) no se maneja localmente.

Se usa **AWS S3** como backend remoto **tanto para almacenar como para bloquear el estado**.

Beneficios:

- Evita ediciones simultáneas del estado
- Estado centralizado por ambiente
- Historial y versionamiento automático
- Trabajo colaborativo seguro

---

### Pipeline de infraestructura

La infraestructura se despliega mediante una **pipeline automatizada en GitHub Actions**

1. Instala Terraform y se conecta a AWS
2. Inicializa el backend remoto en S3
3. Revisa formato, sintaxis y módulos
4. Ejecuta `terraform plan` para ver cambios
5. Ejecuta `terraform apply` cuando se aprueba
- Permite `terraform destroy` solo con confirmación

### Seguridad

- La autenticación se hace asumiendo un **IAM Role vía OIDC**
- Permisos mínimos según ambiente

### Triggers

- La ejecución es **100% manual** usando `workflow_dispatch`
- Se selecciona el ambiente antes de correrla (`dev`, `stage`, `prod`)

---

![image.png](Proyecto%20Final%20IngeSoft%20V/image%201.png)

---

## Microservicios

El proyecto está compuesto por varios microservicios independientes, cada uno con su propia API, configuración, imagen Docker y perfil por ambiente. Todos registran en Eureka, cargan configuración desde Cloud Config y reportan trazas a Zipkin.

---

| Servicio | Propósito | Puerto local | Context path | Imagen |
| --- | --- | --- | --- | --- |
| `api-gateway` | Entrada única al sistema, routing y CORS | `8080` | `/` | `alejocastro/api-gateway:{tag}` |
| `service-discovery` | Eureka Server, registro de servicios | `8761` | `/` | `alejocastro/service-discovery:{tag}` |
| `cloud-config` | Configuración centralizada desde Git | `9296` | `/` | `alejocastro/cloud-config:{tag}` |
| `proxy-client` | Cliente web Angular servido por Spring | `8900` | `/app` | `alejocastro/proxy-client:{tag}` |
| `order-service` | Gestión de órdenes | `8300` | `/order-service` | `alejocastro/order-service:{tag}` |
| `payment-service` | Procesa pagos | `8400` | `/payment-service` | `alejocastro/payment-service:{tag}` |
| `product-service` | Productos del catálogo | `8500` | `/product-service` | `alejocastro/product-service:{tag}` |
| `shipping-service` | Envíos y logística | `8600` | `/shipping-service` | `alejocastro/shipping-service:{tag}` |
| `user-service` | Usuarios, login y perfiles | `8700` | `/user-service` | `alejocastro/user-service:{tag}` |
| `favourite-service` | Wishlist/favoritos del usuario | `8800` | `/favourite-service` | `alejocastro/favourite-service:{tag}` |

---

Naming estándar:

`{tag}` depende del ambiente: `dev`, `stage`, `prod`

- `dev` → entorno de desarrollo
- `stage` → staging
- `prod` → producción

### Dependencias comunes

- **Eureka** para service discovery
- **Cloud Config** para configuración centralizada
- **Zipkin** para tracing distribuido
- **Resilience4j** para resiliencia (circuit breaker, rate limit, bulkhead)
- **Actuator** para health checks

---

## **Patrones de Diseño**

### Patrones encontrados

### Microservices Architecture

**Función:** dividir el sistema en servicios independientes, desplegables y escalables.

**Dónde aparece:** en la separación del backend en servicios como order, user, product, payment, favourite, etc.

**Beneficio principal:** permite ciclos de desarrollo y despliegue autónomos.

### API Gateway Pattern

**Función:** actuar como punto único de entrada al ecosistema backend.

**Dónde aparece:** en el servicio api-gateway.

**Beneficio principal:** centraliza routing, CORS, autorización, trazabilidad y monitoreo.

### Service Discovery Pattern

**Función:** resolver dinámicamente endpoints de microservicios registrados.

**Dónde aparece:** Spring Cloud Eureka y RestTemplate load balanced.

**Beneficio principal:** elimina configuraciones estáticas y facilita escalamiento horizontal.

### Circuit Breaker Pattern

**Función:** evitar cascadas de fallos cuando un servicio dependiente presenta errores.

**Dónde aparece:** Resilience4j configurado en application.yml de cada microservicio.

**Beneficio principal:** protección ante fallas y disponibilidad controlada.

### DTO (Data Transfer Object)

**Función:** transportar datos entre servicios sin exponer entidades internas.

**Dónde aparece:** clases DTO utilizadas en controladores, servicios y llamados remotos.

**Beneficio principal:** desacoplamiento, validación y seguridad del dominio.

### Repository Pattern

**Función:** abstraer la capa de acceso a datos.

**Dónde aparece:** interfaces que extienden JpaRepository.

**Beneficio principal:** evita SQL manual y desacopla la lógica de persistencia.

### Builder Pattern

**Función:** construir objetos complejos de forma segura y legible.

**Dónde aparece:** entidades y DTOs con Lombok builder.

**Beneficio principal:** claridad, inmutabilidad y reducción de errores.

### Mapper / Adapter Pattern

**Función:** transformar datos entre entidades, DTOs y objetos de dominio.

**Dónde aparece:** clases helper encargadas de conversiones.

**Beneficio principal:** evita duplicación de lógica de mapeo y mantiene separadas las capas.

---

### Patrones agregados

### Bulkhead Pattern

**Implementación**

Se configuraron bulkheads en Resilience4j asignando límites de concurrencia a las llamadas hacia servicios externos. Cada dependencia opera dentro de su propio “compartimiento” aislado para evitar consumo ilimitado de recursos.

**Propósito**

Evitar que un servicio lento o fallando consuma todos los hilos, conexiones o capacidad del microservicio que lo llama.

**Beneficios para el sistema**

- Previene cascadas de fallos entre microservicios
- Mantiene tiempos de respuesta estables bajo carga
- Garantiza disponibilidad parcial incluso con dependencias degradadas
- Aumenta resiliencia y control del consumo de recursos

**En acción / Prueba**

Configuración

![image.png](Proyecto%20Final%20IngeSoft%20V/image%203.png)

```java
	@Bulkhead(name = "userServiceBulkhead", fallbackMethod = "fallbackUser")
	public UserDto fetchUserById(Integer userId) {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return restTemplate.getForObject(
				AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + userId,
				UserDto.class);
	}
	
	public UserDto fallbackUser(Integer userId, Throwable t) {
		log.warn("Fallback triggered for user {}: {}", userId, t.toString());
		return new UserDto();
	}
```

---

Se configura en el `favourite-service` el cual tiene maximo 1 llamada concurrente para el microservicio `user-service` En caso de que se ejecute mas de una 1 llamada concurrente se activa el fallback. 

---

Obtenemos la siguiente respuesta si se ejecuta mas de una llamada concurrente

```java
2025-11-25 11:46:36.772  WARN [FAVOURITE-SERVICE,,] 89658 --- [onPool-worker-5] c.s.a.service.impl.FavouriteServiceImpl  : Fallback triggered for user 1: io.github.resilience4j.bulkhead.BulkheadFullException: Bulkhead 'userServiceBulkhead' is full and does not permit further calls
```

---

### Rate Limiter Pattern

**Implementación**

Se configuró un rate limiter por microservicio usando Resilience4j, estableciendo cuántas solicitudes pueden procesarse dentro de un intervalo de tiempo. Las llamadas excedentes activan un fallback.

**Propósito**

Controlar y regular el tráfico entrante para proteger servicios internos y externos.

**Beneficios para el sistema**

- Evita sobrecarga y saturación del backend
- Reduce riesgos ante picos inesperados de tráfico
- Protege recursos críticos y terceros
- Mejora estabilidad y consistencia en respuestas

**En acción / Prueba**

Configuración

![image.png](Proyecto%20Final%20IngeSoft%20V/image%204.png)

```java
	@Override
	@RateLimiter(name = "favouriteServiceRateLimiter", fallbackMethod = "fallbackRateLimiter")
	public List<FavouriteDto> findAll() {
		log.info("*** FavouriteDto List, service; fetch all favourites *");
		return this.favouriteRepository.findAll()
				.stream()
				.map(FavouriteMappingHelper::map)
				.map(f -> {
					f.setUserDto(fetchUserById(f.getUserId()));
					f.setProductDto(fetchProductById(f.getProductId()));
					return f;
				})
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}
	
		public List<FavouriteDto> fallbackRateLimiter(Throwable t) {
		log.warn("Favourite service does not permit further calls: {}", t.toString());
		return List.of();
	}
```

---

Este rate limiter permite **máximo 2 solicitudes cada 2 minutos** hacia el servicio de favoritos. Si hay más solicitudes, esperan hasta 30 segundos antes de fallar y nos regresa el fallback que es una lista vacía 

![image.png](Proyecto%20Final%20IngeSoft%20V/image%205.png)

---

### Artifact Repository (Nexus Sonatype)

**Implementación**

El pipeline publica automáticamente artefactos versionados en Nexus. Cada release queda almacenada para despliegues, auditoría o rollback.

**Propósito**

Centralizar, versionar y asegurar la distribución de binarios generados durante CI/CD.

**Beneficios para el sistema**

- Trazabilidad completa de releases
- Rollback seguro sin recompilar
- Menos dependencias externas durante despliegues
- Builds reproducibles y confiables en cualquier ambiente

**En acción / Prueba**

Se despliega en el cluster de EKS, se configuran un repositorio maven2. Ademas, en la pipeline de desarollo de la rama `master/main/prod` se configura la siguiente stage

```groovy
stage('Upload Artifacts') {
    when { 
        branch 'master' 
    }
    steps {
        script {
            def services = [
                'api-gateway',
                'cloud-config',
                'favourite-service',
                'order-service',
                'payment-service',
                'product-service',
                'proxy-client',
                'service-discovery',
                'shipping-service',
                'user-service'
            ]

            def version = "${env.BUILD_ID}-${new Date().format('yyyyMMdd-HHmmss')}"

            def artifacts = services.collect { service ->
                [
                    artifactId: service,
                    classifier: '',
                    file: "${service}/target/${service}-v0.1.0.jar",
                    type: 'jar'
                ]
            }

            nexusArtifactUploader(
                nexusVersion: 'nexus3',
                protocol: 'http',
                nexusUrl: 'k8s-artifact-sonatype-d27d7dd556-1059737199.us-east-1.elb.amazonaws.com',
                groupId: 'com.ecommerce',
                version: version,
                repository: 'ecommerce-app',
                credentialsId: 'nexusLogin',
                artifacts: artifacts
            )
        }
    }
}

```

---

Asi, obtenemos un repositorio de artefactos (`.jar`)

![image.png](Proyecto%20Final%20IngeSoft%20V/image%206.png)

---

## CI/CD

### Resumen general de la pipeline

La pipeline automatiza todo el ciclo de vida de los microservicios desde la construcción hasta el despliegue en Kubernetes, asegurando calidad, seguridad y trazabilidad. Sus etapas principales son:

1. **Detección de rama y configuración**: Define perfil (dev, stage, prod) y variables de entorno según la rama.
2. **Checkout del código**: Obtiene la versión correcta desde el repositorio.
3. **Verificación de herramientas**: Confirma disponibilidad de Java, Maven, Docker y kubectl.
4. **Build de servicios**: Genera archivos `.jar` de cada microservicio y construye imágenes Docker.
5. **Publicación de artefactos**: Subida a Nexus Sonatype para control de versiones y repositorio centralizado.
6. **Análisis de calidad y seguridad**: SonarQube para código, Trivy y OWASP ZAP para vulnerabilidades.
7. **Pruebas automatizadas**: Unitarias, integración, E2E y pruebas de carga/estrés con Locust.
8. **Despliegue a Kubernetes**: Aplicación de configuraciones comunes, core services, microservicios y ingress, con validación de rollout.
9. **Control de versiones y releases**: Generación de SemVer, release notes, tagging en Git y creación de release en GitHub.
10. **Notificación y aprobación**:
    - Envía correo automático si ocurre algún fallo en la pipeline.
    - Espera aprobación manual antes de desplegar a producción para mayor control.
11. **Post-deployment**: Limpieza de contenedores temporales y publicación de reportes de calidad y seguridad.

---

### Herramientas usadas

- **Jenkins**: Orquestación de la pipeline y multibranch.
- **Maven**: Build y gestión de dependencias Java.
- **Java (JDK 11 y JDK 17)**: Compilación y ejecución de servicios y análisis de calidad.
- **Docker & Docker Hub**: Build, push y distribución de imágenes de microservicios.
- **Kubernetes (EKS)**: Orquestación y despliegue de microservicios.
- **AWS CLI**: Configuración de kubeconfig y acceso a EKS.
- **Nexus Sonatype**: Repositorio de artefactos Java y control de versiones.
- **SonarQube**: Análisis de calidad de código.
- **Trivy**: Escaneo de vulnerabilidades en imágenes Docker.
- **OWASP ZAP**: Escaneo de seguridad de endpoints HTTP.
- **Locust**: Pruebas de carga y estrés.
- **GitHub API**: Generación de releases y tagging.
- **Jenkins email extension**: Notificación automática en caso de fallo de pipeline.

---

### Pipeline multibranch

La pipeline está diseñada para soportar múltiples ramas y ajustar su comportamiento según el entorno:

- **Dev branch**:
    - Build de servicios
    - Ejecución de tests unitarios y cobertura
    - Build y push de Docker images
        
        ![AWS Cloud Architecture - Frame 1.jpg](Proyecto%20Final%20IngeSoft%20V/AWS_Cloud_Architecture_-_Frame_1.jpg)
        
- **Stage branch**:
    - Todo lo de dev
    - Análisis de SonarQube, Trivy y OWASP ZAP
    - Ejecución de integración y E2E tests
    - Pruebas de carga y estrés con Locust
    - Publicación de reportes
        
        ![AWS Cloud Architecture - Frame 2.jpg](Proyecto%20Final%20IngeSoft%20V/AWS_Cloud_Architecture_-_Frame_2.jpg)
        
- **Master branch**:
    - Todo lo de stage
    - Publicación de artefactos en Nexus
    - Despliegue completo en Kubernetes (core services, microservicios, ingress)
    - Approval manual antes de producción
    - Control de versiones y creación de release
        
        ![AWS Cloud Architecture - Frame 3.jpg](Proyecto%20Final%20IngeSoft%20V/AWS_Cloud_Architecture_-_Frame_3.jpg)
        

---

## Pruebas

### Tipos de pruebas implementadas

- **Unitarias**: Validan métodos individuales de cada microservicio.
- **Integración**: Comprueban la interacción entre microservicios dentro del mismo ambiente.
- **End-to-End (E2E)**: Simulan flujos completos de negocio para garantizar que todos los componentes trabajan juntos.
- **Carga (Load Test)**: Simulan tráfico normal para evaluar rendimiento y tiempos de respuesta.
- **Estrés (Stress Test)**: Simulan picos de carga extrema para identificar límites del sistema.
- **Seguridad**: Escaneos con OWASP ZAP para identificar vulnerabilidades en endpoints.

---

### Cómo ejecutarlas

- **Unitarias e Integración**: Maven (`mvn clean test` y `mvn verify`) dentro de cada microservicio.
- **E2E**: Maven (`mvn verify -pl e2e-tests`) sobre escenarios de negocio completos.
- **Carga y Estrés**: Locust ejecutando scripts `.py` sobre contenedores de servicios desplegados temporalmente.
- **Seguridad**: OWASP ZAP ejecutando escaneo automatizado de endpoints expuestos.

---

### Reportes y cobertura

- **Unitarias/Integración**: JUnit y JaCoCo, reportes HTML para cobertura de código.
- **Carga/Estrés**: Locust genera reportes HTML con métricas de latencia, RPS (requests per second) y errores.
- **Seguridad**: OWASP ZAP genera reportes HTML con vulnerabilidades encontradas.

---

### Pruebas de carga y estrés

### **Order Service**

- **Carga:** 295 requests en 59 segundos, 0 fallos.
    - Promedio: 13.4 ms
    - Min: 4 ms / Max: 200 ms
    - RPS: 4.96
- **Estrés:** 1,395 requests en 1 minuto, 0 fallos.
    - Promedio: 5.62 ms
    - Min: 1 ms / Max: 39 ms
    - RPS: 23.31
- **Análisis:** Latencias bajas y consistentes bajo carga alta. El servicio es eficiente y escalable, tolerando picos de tráfico sin errores.

### **Favourite Service**

- **Carga:** 12 requests en 49 segundos, 0 fallos.
    - Promedio: 27,652 ms (~27 s)
    - Min: 15,809 ms / Max: 30,024 ms
    - RPS: 0.25
- **Estrés:** 50 requests en 39 segundos, 0 fallos.
    - Promedio: 29,451 ms (~29 s)
    - Min: 15,278 ms / Max: 30,134 ms
    - RPS: 1.28
- **Análisis:** Latencias extremadamente altas, aunque sin errores. Indica que el servicio funciona correctamente, pero puede ser un **cuello de botella**, especialmente con cargas concurrentes. Probable necesidad de optimización en consultas, base de datos o caching.

### **Payment Service**

- **Carga:** 272 requests en 1 minuto, 0 fallos.
    - GET `/api/payments`: 78 requests, promedio 89.37 ms, RPS 1.31
    - POST `/api/payments`: 96 requests, promedio 11.75 ms, RPS 1.62
    - GET `/api/payments/{id}`: 98 requests, promedio 15.05 ms, RPS 1.65
    - Promedio agregado: 35.2 ms
- **Estrés:** 1,380 requests en 1 minuto, 0 fallos.
    - GET `/api/payments`: 437 requests, promedio 207.1 ms, RPS 7.32
    - POST `/api/payments`: 482 requests, promedio 7.47 ms, RPS 8.07
    - GET `/api/payments/{id}`: 461 requests, promedio 6.52 ms, RPS 7.72
    - Promedio agregado: 70.37 ms
- **Análisis:** Latencias bajas a moderadas, con buen rendimiento general incluso bajo estrés. Los endpoints POST y GET por ID son especialmente rápidos, mientras que GET `/api/payments` puede requerir optimización bajo cargas muy altas.

---

### Resultados

![image.png](Proyecto%20Final%20IngeSoft%20V/image%207.png)

![image.png](Proyecto%20Final%20IngeSoft%20V/image%208.png)

![image.png](Proyecto%20Final%20IngeSoft%20V/image%209.png)

![image.png](Proyecto%20Final%20IngeSoft%20V/image%2010.png)

![image.png](Proyecto%20Final%20IngeSoft%20V/image%2011.png)

![total_requests_per_second_1764092524.695.png](Proyecto%20Final%20IngeSoft%20V/total_requests_per_second_1764092524.695.png)

![image.png](Proyecto%20Final%20IngeSoft%20V/sonarqube.png)

---

## Observabilidad

La plataforma implementa observabilidad integral sobre los microservicios, combinando **logs, métricas y trazas**, con dashboards y alertas configuradas para monitoreo en tiempo real y detección temprana de problemas.

### 1. Monitoreo y Logging

- **Prometheus y Grafana:** Desplegados como pods en el cluster mediante Helm charts desde la infraestructura. Se recolectan métricas de todos los nodos y contenedores.
- **ELK (Elasticsearch, Logstash, Kibana):** Recolecta logs de todos los microservicios, permitiendo búsqueda y análisis centralizado de eventos y errores.
- **Trazas:** Integración con Zipkin/Jaeger para seguimiento de requests distribuidos entre servicios.

### 2. Alertas configuradas

Se implementaron alertas usando **Prometheus Rules**. Las principales son:

- **Node Down:** Detecta si un nodo del cluster deja de estar activo por más de 5 minutos. **Gravedad:** crítica.
- **Alto uso de CPU:** Monitorea nodos o contenedores con CPU superior al 80% durante más de 10 minutos. **Gravedad:** warning.
- **Alto uso de memoria:** Detecta nodos o contenedores usando más del 85% de memoria por más de 10 minutos. **Gravedad:** warning.
- **Pods en CrashLoopBackOff:** Identifica pods que se reinician repetidamente en 15 minutos. **Gravedad:** crítica.
- **Presión de disco:** Monitorea nodos con menos del 15% de espacio libre. **Gravedad:** warning.
- **Pods no listos:** Detecta pods en estado distinto a Running o Succeeded por más de 15 minutos. **Gravedad:**warning.
- **Desajuste de réplicas en deployments:** Identifica deployments que no han alcanzado el número esperado de réplicas. **Gravedad:** warning.
- **Salud de Elasticsearch:** Detecta clusters en estado rojo (crítico) o amarillo (warning) y alto uso de memoria JVM.
- **Uso elevado de recursos por contenedor:** Memoria por encima del 90% o CPU siendo throttled. **Gravedad:**warning.

Estas alertas permiten una **respuesta rápida ante problemas de infraestructura, servicios y componentes críticos**.

### 3. Dashboards

Se configuraron dashboards en **Grafana** para visualizar

![image.png](Proyecto%20Final%20IngeSoft%20V/image%2012.png)

![image.png](Proyecto%20Final%20IngeSoft%20V/image%2013.png)

![image.png](Proyecto%20Final%20IngeSoft%20V/image%2014.png)

![image.png](Proyecto%20Final%20IngeSoft%20V/image%2015.png)

### 4. Logs en ELK

Todos los microservicios generan logs que se recolectan y centralizan en **Kibana**.

![image.png](Proyecto%20Final%20IngeSoft%20V/image%2016.png)

### 5. Métricas de negocio

Se implementan métricas que permiten monitorear la **salud funcional del sistema**, no solo la infraestructura. Estas métricas permiten medir el rendimiento y éxito de los servicios desde la perspectiva del negocio:

- **Order Service**
    - **Pedidos procesados:** total de pedidos recibidos en un periodo.
    - **Pedidos exitosos (%):** porcentaje de pedidos completados sin errores.
    - **Pedidos con error (%):** porcentaje de pedidos que fallaron o fueron cancelados.
    - **Pedidos pendientes (%):** porcentaje de pedidos que aún no se han completado.
- **Payment Service**
    - **Transacciones completadas (%):** porcentaje de pagos realizados con éxito.
    - **Transacciones fallidas (%):** porcentaje de pagos que no se completaron o generaron error.
    - **Tiempo promedio de pago:** tiempo promedio entre la solicitud de pago y su confirmación.
- **Favourite Service**
    - **Favoritos activos por usuario:** número promedio de items marcados como favoritos por usuario activo.
    - **Solicitudes totales:** total de consultas al servicio de favoritos en un periodo.
    - **Tasa de error en consultas (%):** porcentaje de solicitudes que fallaron o devolvieron error.
- **Disponibilidad de endpoints críticos**
    - **% de solicitudes exitosas por servicio:** porcentaje de requests HTTP que retornaron código 2xx en los endpoints principales de cada microservicio.
    - **% de latencia aceptable:** porcentaje de requests que cumplen con el tiempo de respuesta objetivo definido para cada servicio.

---

![image.png](Proyecto%20Final%20IngeSoft%20V/image%2018.png)

---

## Gestión de Cambios y Releases

### Estrategia de Branching

- Se sigue **Git Flow**:
    - **feature/**: nuevas funcionalidades.
    - **fix/**: correcciones de bugs.
    - **chore/**: tareas de mantenimiento.
    - **develop**: rama de integración para pruebas.
    - **stage**: rama de preproducción.
    - **master**: rama de producción.

### Flujo de Cambios y Pipeline

1. **Desarrollo en Feature/Branches**
    - Los cambios se realizan en ramas `feat/*`, `fix/*` o `chore/*`.
    - Cada commit debe seguir **Standard Commit Messages**:
        - **BREAKING CHANGE** → indica un cambio mayor que rompe compatibilidad.
        - **feat** → indica una nueva funcionalidad (minor).
        - **fix** → indica corrección de errores (patch).
    - Al hacer push, se ejecuta la **pipeline de dev**, que realiza build, pruebas unitarias, análisis de calidad y despliegue en entorno de desarrollo.
2. **Pull Request a Stage**
    - Tras pasar la pipeline de dev, se abre un **PR hacia la rama `stage`**.
    - Requiere aprobación de al menos un revisor.
    - La pipeline de stage incluye build completo, pruebas, análisis de calidad y despliegue en preproducción para validación funcional.
3. **Pull Request a Master y Versionado**
    - Tras validación en stage, se abre **PR hacia `master`**.
    - La pipeline de master realiza build final, pruebas completas y determina la **versión SemVer** según los commits:
        - **Major (X.0.0)** → commit con **BREAKING CHANGE**.
        - **Minor (0.Y.0)** → commit con **feat**.
        - **Patch (0.0.Z)** → commit con **fix**.
- Se genera automáticamente:
    - **Tag en GitHub** (ej. `v2.2.0`)
    - **Release notes** que incluyen todos los commits desde la versión anterior, organizados por tipo de cambio.
    - Despliegue a producción tras aprobación.

### Release Notes

- Documentan todos los cambios realizados desde la versión anterior, permitiendo rastrear qué se incluyó en cada release.
- Ejemplo de contenido en la release notes:
    - **Features** → nuevas funcionalidades agregadas.
    - **Fixes** → correcciones aplicadas.
    - **Chores/Misc** → tareas de mantenimiento o mejoras menores.

---

![image.png](Proyecto%20Final%20IngeSoft%20V/image%2019.png)

### Plan de Rollback

- Basado en artefactos versionados en **Nexus Sonatype**:
    1. **Identificación de versión estable:** seleccionar la última versión aprobada y publicada en producción.
    2. **Despliegue del artefacto anterior:** utilizar el artifact repository para extraer la versión estable y redeploy en el cluster.
    3. **Validación funcional:** ejecutar smoke tests y pruebas críticas para asegurar que todo funciona como en la versión estable.
    4. **Registro y trazabilidad:** documentar la acción de rollback, la versión restaurada y los motivos del rollback.
    5. **Análisis post-rollback:** identificar la causa del fallo para evitar que se repita en futuros releases.

---

## Costos de Infraestructura

- **Amazon VPC**
    - 1 VPC principal
    - 3 public subnets + 3 private subnets
    - NAT Gateway (1, en la primera AZ)
    - Internet Gateway
    - Route Tables (públicas y privadas)
- **Amazon EC2 (nodos EKS)**
    - Node Group de EKS con 6 nodos (deseados)
    - Instancias: `m7i-flex.large`, `c7i-flex.large`, `t3.small` (mezcla)
    - Tipo de capacidad: On-Demand
- **Amazon EKS (Kubernetes)**
    - 1 cluster EKS
    - Add-ons: `vpc-cni`, `coredns`, `kube-proxy`
    - Gestión de logs habilitada: API y Audit
    - Helm releases de observabilidad: Prometheus + Grafana, Metrics Server
- **Elastic Load Balancer (ALB)**
    - Tráfico a aplicaciones y Nexus
    - Listener HTTP/HTTPS
    - Reglas simples de path/host
- **Amazon EBS (volúmenes de nodos y storage para Prometheus/Grafana/ELK)**
    - Tamaño disco nodo: 100 GiB por nodo
    - Prometheus storage: 100 GiB
    - Grafana persistence: 10 GiB
    - Elasticsearch nodes: 3 × 50 GiB
- **Amazon S3**
    - Bucket para estado remoto de Terraform (solo almacenamiento)
- **Data Transfer**
    - Tráfico entre ALB → nodos
    - Tráfico internet → ALB
    - Tráfico entre subnets públicas y privadas

---

![image.png](Proyecto%20Final%20IngeSoft%20V/image%2021.png)

---