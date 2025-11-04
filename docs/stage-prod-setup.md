# Configuración de Pipelines Stage y Prod

Este documento describe cómo configurar y usar los pipelines de Stage y Producción para el proyecto ecommerce-microservice-backend-app.

## 📁 Archivos Creados

### Pipelines Jenkins
- `jenkins/Jenkinsfile` - Pipeline para DEV (ya configurado)
- `jenkins/Jenkinsfile-stage` - Pipeline para STAGE (nuevo)
- `jenkins/Jenkinsfile-prod` - Pipeline para PROD (nuevo)

### Configuraciones Kubernetes
- `dev-kubeconfig.yaml` - Configuración para DEV (ya configurado)
- `stage-kubeconfig.yaml` - Configuración para STAGE (nuevo)
- `prod-kubeconfig.yaml` - Configuración para PROD (nuevo)

## 🏗️ Arquitectura de los Pipelines

### Pipeline DEV (actual)
1. Checkout
2. Build Services (parallel)
3. Run Unit Tests (parallel)
4. Build Docker Images
5. Push to Registry (tags: `${BUILD_NUMBER}`, `dev-latest`)
6. Deploy to Kubernetes (namespace: `ecommerce-dev`)
7. Run E2E Tests

### Pipeline STAGE (nuevo)
**Incluye todo lo de DEV más:**
8. **Performance Tests** - Pruebas de rendimiento
9. **Stress Tests** - Pruebas de estrés

**Cambios en Stage:**
- Namespace: `ecommerce-stage`
- Tags Docker: `${BUILD_NUMBER}`, `stage-latest`
- Spring Profile: `stage`
- Timeouts más largos para estabilización

### Pipeline PROD (nuevo)
**Incluye todo lo de STAGE más:**
10. **Smoke Tests** - Pruebas de humo en producción
11. **Create Release** - Creación de release con notas
12. **Release Verification** - Verificación final del despliegue

**Cambios en Prod:**
- Namespace: `ecommerce-prod`
- Tags Docker: `${BUILD_NUMBER}`, `${RELEASE_TAG}` (v0.1.0-XX), `prod-latest`
- Spring Profile: `prod`
- Timeouts aún más largos (10 minutos)
- Validación exhaustiva pre-despliegue

## 🔧 Configuración en Jenkins

### 1. Crear Namespaces en Kubernetes

```bash
kubectl create namespace ecommerce-stage
kubectl create namespace ecommerce-prod
```

### 2. Subir Kubeconfigs a Jenkins

#### Para Stage:
1. Ir a Jenkins → Manage Jenkins → Credentials
2. Clic en "(global)" domain
3. Clic en "Add Credentials"
4. Tipo: "Secret file"
5. File: Subir `stage-kubeconfig.yaml`
6. ID: `kubeconfig-stage`
7. Description: "Kubeconfig for Stage environment"
8. Save

#### Para Prod:
1. Repetir los pasos anteriores
2. File: Subir `prod-kubeconfig.yaml`
3. ID: `kubeconfig-prod`
4. Description: "Kubeconfig for Prod environment"

### 3. Crear Jobs de Pipeline en Jenkins

#### Pipeline STAGE:
1. Jenkins → New Item
2. Nombre: `ecommerce-microservices-stage`
3. Tipo: "Pipeline"
4. En "Pipeline" section:
   - Definition: "Pipeline script from SCM"
   - SCM: Git
   - Repository URL: [tu-repositorio]
   - Branch: `*/stage`
   - Script Path: `jenkins/Jenkinsfile-stage`
5. Save

#### Pipeline PROD:
1. Jenkins → New Item
2. Nombre: `ecommerce-microservices-prod`
3. Tipo: "Pipeline"
4. En "Pipeline" section:
   - Definition: "Pipeline script from SCM"
   - SCM: Git
   - Repository URL: [tu-repositorio]
   - Branch: `*/main`
   - Script Path: `jenkins/Jenkinsfile-prod`
5. Save

### 4. Crear Manifiestos de Kubernetes

Necesitas duplicar los manifiestos de `kubernetes/dev/` para Stage y Prod:

```bash
# Crear directorios
mkdir -p kubernetes/stage
mkdir -p kubernetes/prod

# Copiar manifiestos de dev
cp kubernetes/dev/*.yaml kubernetes/stage/
cp kubernetes/dev/*.yaml kubernetes/prod/

# Actualizar referencias de namespace en los archivos
# Para Stage: cambiar ecommerce-dev a ecommerce-stage
# Para Prod: cambiar ecommerce-dev a ecommerce-prod
```

**Modificar cada archivo en `kubernetes/stage/`:**
```yaml
metadata:
  namespace: ecommerce-stage  # Cambiar aquí
```

**Modificar cada archivo en `kubernetes/prod/`:**
```yaml
metadata:
  namespace: ecommerce-prod  # Cambiar aquí
```

También actualizar las imagePullPolicy y tags:
- Stage: `image: alejocastro/[service]-ecommerce-boot:stage-latest`
- Prod: `image: alejocastro/[service]-ecommerce-boot:prod-latest`

## 🚀 Flujo de Trabajo de Despliegue

### Desarrollo → Stage → Producción

```
1. Desarrollar en rama 'dev'
   └─> Push → Jenkins ejecuta Jenkinsfile (DEV)
       └─> Tests unitarios + E2E
       └─> Deploy a ecommerce-dev

2. Promover a rama 'stage'
   └─> Merge dev → stage
   └─> Push → Jenkins ejecuta Jenkinsfile-stage
       └─> Tests unitarios + E2E + Performance + Stress
       └─> Deploy a ecommerce-stage

3. Promover a rama 'main' (producción)
   └─> Merge stage → main
   └─> Push → Jenkins ejecuta Jenkinsfile-prod
       └─> Todos los tests + Smoke tests
       └─> Deploy a ecommerce-prod
       └─> Crear Release con tag vX.Y.Z-BUILD
       └─> Verificación final
```

## 🧪 Pruebas Implementadas

### Tests Unitarios (Todos los pipelines)
- Excluyen E2E, IntegrationTest, IT
- Ejecutan en paralelo por servicio

### Tests E2E (Todos los pipelines)
- Ejecutan después del despliegue
- Esperan 45-60s para estabilización
- Actualmente: order-service

### Tests de Performance (Stage y Prod)
- Validación de tiempos de respuesta
- Throughput (requests/segundo)
- Disponibilidad bajo carga normal
- **Nota:** Actualmente esqueleto, se puede integrar con:
  - Apache JMeter
  - Gatling
  - k6
  - Apache Bench

### Tests de Estrés (Solo Stage)
- Pruebas de carga gradual
- Spike tests (picos súbitos)
- Carga sostenida
- Validación de límites de recursos
- **Nota:** Actualmente esqueleto

### Smoke Tests (Solo Prod)
- Verificación de servicios UP en Eureka
- Verificación de enrutamiento en API Gateway
- Conectividad con bases de datos
- Health checks de endpoints críticos

### Release Verification (Solo Prod)
- Conteo de pods corriendo
- Verificación de registros en Eureka
- Detección de restarts
- Utilización de recursos (si metrics-server disponible)

## 📊 Outputs y Artefactos

### Todos los pipelines:
- JUnit test reports
- JAR artifacts

### Pipeline Prod adicional:
- **Release notes** (Markdown)
  - Versión y timestamp
  - Lista de servicios desplegados
  - Últimos 10 commits
  - Resumen de tests
  - Estado del despliegue

## 🔐 Variables de Entorno

### DEV
```groovy
NAMESPACE = 'ecommerce-dev'
EXPECTED_BRANCH = 'dev'
KUBE_CONFIG_CREDENTIALS_ID = 'kubeconfig-dev'
```

### STAGE
```groovy
NAMESPACE = 'ecommerce-stage'
EXPECTED_BRANCH = 'stage'
KUBE_CONFIG_CREDENTIALS_ID = 'kubeconfig-stage'
```

### PROD
```groovy
NAMESPACE = 'ecommerce-prod'
EXPECTED_BRANCH = 'main'
KUBE_CONFIG_CREDENTIALS_ID = 'kubeconfig-prod'
RELEASE_TAG = "v${APP_VERSION}-${BUILD_NUMBER}"
```

## 🎯 Próximos Pasos

1. **Crear manifiestos para Stage y Prod:**
   ```bash
   ./scripts/create-stage-prod-manifests.sh
   ```

2. **Subir kubeconfigs a Jenkins:**
   - stage-kubeconfig.yaml → credential ID: `kubeconfig-stage`
   - prod-kubeconfig.yaml → credential ID: `kubeconfig-prod`

3. **Crear jobs en Jenkins:**
   - ecommerce-microservices-stage
   - ecommerce-microservices-prod

4. **Implementar tests de performance reales:**
   - Instalar herramienta (JMeter/k6/Gatling)
   - Escribir scripts de carga
   - Actualizar stages en Jenkinsfiles

5. **Configurar notificaciones:**
   - Slack/Email para Stage failures
   - PagerDuty/urgent alerts para Prod failures

## 📝 Notas Importantes

- **Todos los ambientes corren en el mismo minikube** con namespaces diferentes
- **Las imagePullPolicy** deben ser `Always` para ambientes locales con latest tags
- **Los certificados de minikube** usan `insecure-skip-tls-verify: true` para host.docker.internal
- **Los tests de performance/estrés** son esqueletos que necesitan implementación real
- **El versionado** usa formato `v0.1.0-BUILD_NUMBER` para releases

## 🐛 Troubleshooting

### Error: "No such credential: kubeconfig-stage"
- Asegúrate de haber subido el kubeconfig a Jenkins con el ID exacto

### Error: "namespace not found"
- Crear el namespace: `kubectl create namespace ecommerce-stage`

### Pods no inician en Stage/Prod
- Verificar que las imágenes Docker tengan los tags correctos
- Verificar que los deployments apunten al namespace correcto

### Tests de performance no ejecutan
- Son placeholders, necesitan implementación de herramienta real

## 📚 Referencias

- [Documentación Jenkins Pipeline](https://www.jenkins.io/doc/book/pipeline/)
- [Kubernetes Multi-namespace Deployments](https://kubernetes.io/docs/concepts/overview/working-with-objects/namespaces/)
- [Apache JMeter Performance Testing](https://jmeter.apache.org/)
- [k6 Load Testing](https://k6.io/)
