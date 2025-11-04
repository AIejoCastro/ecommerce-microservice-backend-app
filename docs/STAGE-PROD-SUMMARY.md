# Resumen de Archivos Creados para Stage y Prod

## ✅ Archivos Nuevos Creados

### 1. Pipelines Jenkins
```
jenkins/
├── Jenkinsfile            ← DEV (ya existía)
├── Jenkinsfile-stage      ← STAGE (nuevo) ✨
└── Jenkinsfile-prod       ← PROD (nuevo) ✨
```

### 2. Kubeconfigs
```
.
├── dev-kubeconfig.yaml    ← DEV (ya existía)
├── stage-kubeconfig.yaml  ← STAGE (nuevo) ✨
└── prod-kubeconfig.yaml   ← PROD (nuevo) ✨
```

### 3. Documentación
```
docs/
└── stage-prod-setup.md    ← Guía completa de configuración ✨
```

### 4. Scripts
```
scripts/
└── create-stage-prod-manifests.sh  ← Generador de manifiestos ✨
```

## 📋 Comparación de Pipelines

| Stage | DEV | STAGE | PROD |
|-------|-----|-------|------|
| **Namespace** | ecommerce-dev | ecommerce-stage | ecommerce-prod |
| **Branch** | dev | stage | main |
| **Docker Tags** | dev-latest | stage-latest | prod-latest + vX.Y.Z-BUILD |
| **Checkout** | ✅ | ✅ | ✅ |
| **Build Services** | ✅ | ✅ | ✅ |
| **Unit Tests** | ✅ | ✅ | ✅ |
| **Docker Build** | ✅ | ✅ | ✅ |
| **Push Registry** | ✅ | ✅ | ✅ |
| **Deploy K8s** | ✅ | ✅ | ✅ |
| **E2E Tests** | ✅ | ✅ | ✅ |
| **Performance Tests** | ❌ | ✅ | ✅ |
| **Stress Tests** | ❌ | ✅ | ❌ |
| **Smoke Tests** | ❌ | ❌ | ✅ |
| **Create Release** | ❌ | ❌ | ✅ |
| **Release Verification** | ❌ | ❌ | ✅ |

## 🎯 Características Principales

### JENKINSFILE-STAGE
```groovy
- Todo lo de DEV
- Performance Tests:
  * Pruebas de rendimiento
  * Métricas de tiempo de respuesta
  * Throughput validation
  
- Stress Tests:
  * Carga gradual (1-50 usuarios)
  * Spike tests (0-100-0 usuarios)
  * Carga sostenida (25 usuarios x 5 min)
```

### JENKINSFILE-PROD
```groovy
- Todo lo de STAGE (excepto Stress Tests)
- Smoke Tests:
  * Verificación de servicios UP
  * API Gateway routing
  * Database connectivity
  * Critical endpoints health
  
- Create Release:
  * Git tag: v0.1.0-BUILD_NUMBER
  * Release notes automáticas
  * Últimos 10 commits
  * Resumen de tests
  
- Release Verification:
  * Conteo de pods running
  * Verificación Eureka
  * Detección de restarts
  * Resource utilization
```

## 🚀 Pasos para Activar

### 1. Ejecutar Script (Opcional)
```bash
chmod +x scripts/create-stage-prod-manifests.sh
./scripts/create-stage-prod-manifests.sh
```
Esto generará automáticamente los manifiestos en:
- `kubernetes/stage/`
- `kubernetes/prod/`

### 2. Crear Namespaces en Kubernetes
```bash
kubectl create namespace ecommerce-stage
kubectl create namespace ecommerce-prod
```

### 3. Subir Kubeconfigs a Jenkins

#### Stage Kubeconfig:
1. Jenkins → Manage Jenkins → Credentials
2. Add Credentials → Secret file
3. File: `stage-kubeconfig.yaml`
4. ID: `kubeconfig-stage` ⚠️ (debe ser exacto)
5. Description: "Kubeconfig for Stage environment"

#### Prod Kubeconfig:
1. Repetir proceso
2. File: `prod-kubeconfig.yaml`
3. ID: `kubeconfig-prod` ⚠️ (debe ser exacto)
4. Description: "Kubeconfig for Prod environment"

### 4. Crear Pipeline Jobs en Jenkins

#### Job STAGE:
```
Name: ecommerce-microservices-stage
Type: Pipeline
Pipeline from SCM:
  - Repository: [tu-repo]
  - Branch: */stage
  - Script Path: jenkins/Jenkinsfile-stage
```

#### Job PROD:
```
Name: ecommerce-microservices-prod
Type: Pipeline
Pipeline from SCM:
  - Repository: [tu-repo]
  - Branch: */main
  - Script Path: jenkins/Jenkinsfile-prod
```

### 5. Crear Ramas en Git
```bash
# Crear rama stage
git checkout -b stage
git push origin stage

# Crear rama main (si no existe)
git checkout -b main
git push origin main
```

## 📊 Flujo de Trabajo

```
┌──────────────┐
│   DEV        │
│  (dev)       │  ← Desarrollo diario
└──────┬───────┘
       │ merge
       ↓
┌──────────────┐
│   STAGE      │
│  (stage)     │  ← Testing completo (E2E + Performance + Stress)
└──────┬───────┘
       │ merge
       ↓
┌──────────────┐
│   PROD       │
│  (main)      │  ← Release con verificación
└──────────────┘
```

## 🔍 Tests Implementados

### Performance Tests (Stage y Prod)
```groovy
stage('Performance Tests') {
    steps {
        script {
            // Test 1: Health endpoint - 1000 requests, 10 concurrent
            // Test 2: User service - 500 requests, 5 concurrent
            // Test 3: Product service - 500 requests, 5 concurrent
        }
    }
}
```

**Nota:** Actualmente son placeholders. Para implementación real:
- Usar Apache JMeter, k6, o Gatling
- Definir umbrales aceptables (e.g., 95% < 500ms)
- Fallar el build si no se cumplen

### Stress Tests (Solo Stage)
```groovy
stage('Stress Tests') {
    steps {
        script {
            // Test 1: Gradual load (1-50 concurrent users)
            // Test 2: Spike test (0-100-0 concurrent users)
            // Test 3: Sustained load (25 concurrent x 5 min)
        }
    }
}
```

### Smoke Tests (Solo Prod)
```groovy
stage('Smoke Tests') {
    steps {
        script {
            // Verificación rápida post-deployment
            // - Todos los servicios UP en Eureka
            // - API Gateway routing correctamente
            // - Bases de datos conectadas
            // - Endpoints críticos responden
        }
    }
}
```

## 📦 Release Notes (Prod)

Cada deployment a Prod genera automáticamente:
```markdown
# Release v0.1.0-42

**Date:** 2025-01-12 15:30:00
**Build:** #42
**Environment:** Production

## 🚀 Deployed Services
- cloud-config
- service-discovery
- api-gateway
- user-service
- product-service
- order-service
- payment-service
- shipping-service

## 📝 Recent Changes
- feat: Add new payment gateway (John Doe)
- fix: Resolve order processing bug (Jane Smith)
- refactor: Improve database queries (Bob Johnson)

## ✅ Testing Summary
- ✅ Unit Tests: PASSED
- ✅ E2E Tests: PASSED
- ✅ Performance Tests: PASSED
- ✅ Smoke Tests: PASSED

## 🐳 Docker Images
All images tagged with:
- Build number: 42
- Release tag: v0.1.0-42
- Latest: prod-latest
```

## ⚠️ Notas Importantes

1. **Todos los ambientes corren en el mismo minikube** local
   - Separados por namespaces (ecommerce-dev, ecommerce-stage, ecommerce-prod)

2. **Los tests de performance/stress son esqueletos**
   - Requieren implementación con herramientas reales
   - Actualmente solo imprimen mensajes de log

3. **Los kubeconfigs usan `insecure-skip-tls-verify: true`**
   - Necesario para acceso desde Jenkins en Docker a minikube
   - En producción real, usar certificados válidos

4. **Los credentials IDs deben ser exactos:**
   - `kubeconfig-stage` (no stage-kubeconfig ni otra variación)
   - `kubeconfig-prod` (no prod-kubeconfig ni otra variación)

5. **Los manifiestos de K8s deben estar en:**
   - `kubernetes/stage/*.yaml`
   - `kubernetes/prod/*.yaml`

## 🎉 Resultado Final

Con esta configuración tendrás:

✅ **3 ambientes completos** (Dev, Stage, Prod)
✅ **Pipeline progresivo** (cada nivel más riguroso)
✅ **Testing exhaustivo** (Unit → E2E → Performance → Stress → Smoke)
✅ **Release tracking** (Git tags, release notes, Docker tags)
✅ **Verificación automática** (health checks, pod status, restarts)

## 📚 Documentación Adicional

Ver `docs/stage-prod-setup.md` para:
- Instrucciones detalladas de configuración
- Troubleshooting común
- Referencias y recursos adicionales
