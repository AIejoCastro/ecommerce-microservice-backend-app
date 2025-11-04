# CI/CD Runbook for `ecommerce-microservice-backend-app`

This guide walks through everything needed to build, package, test, and deploy the microservices using Docker, Kubernetes, and the Jenkins pipelines that ship with the repository.

---

## 1. Prerequisites

Install or verify the following tools on the workstation **and** on the Jenkins agent(s):

| Tool | Purpose | Verification |
|------|---------|--------------|
| Docker / Docker Compose | Build & run service containers | `docker --version`, `docker compose version`
| Java 11+ & Maven | Compile Spring Boot services | `java -version`, `mvn -v` (or use `./mvnw` wrapper)
| Python 3 & pip | Locust performance tests | `python3 --version`, `pip3 --version`
| Kubectl | Interact with Kubernetes clusters | `kubectl version --client`
| Git | Source control | `git --version`
| Jenkins (server) | CI/CD orchestrator | Accessible via browser
| Optional: Trivy, Sonar, Helm | Security & quality checks | `trivy --version`, `sonar-scanner --version`

Additional prerequisites:

- Docker Hub (or other registry) credentials with push permissions.
- Kubernetes clusters/contexts ready for `dev`, `stage`, and `prod` namespaces.
- Network connectivity from Jenkins to the registry and clusters.

---

## 2. Clone and Prepare the Repository

```bash
# Clone if needed
cd /path/where/you/keep/repos
git clone https://github.com/AIejoCastro/ecommerce-microservice-backend-app.git
cd ecommerce-microservice-backend-app

# Install dependencies and build artifacts once to warm caches
./mvnw clean package
```

The command compiles all 11 modules and creates `target/<service>-v0.1.0.jar` artifacts consumed by the Dockerfiles.

---

## 3. Configure Docker Registry Access

```bash
# Log in locally (do the same on the Jenkins agent)
docker login
```

Confirm that credentials are saved in Jenkins as `dockerhub-credentials` (username + access token).

---

## 4. Kubernetes Cluster Setup

### 4.1 Choose or Provision Clusters

- **Local testing**: Minikube or Kind clusters per environment.
- **Cloud**: EKS, AKS, GKE, etc.

Ensure kubeconfig contexts are available and named clearly, e.g. `dev-cluster`, `stage-cluster`, `prod-cluster`.

### 4.2 Create Namespaces & Shared Resources

For each environment:

```bash
# Example for dev
kubectl --context dev-cluster apply -f kubernetes/dev/namespace.yaml
kubectl --context dev-cluster apply -f kubernetes/dev/configmap.yaml

# Deploy supporting services (Zipkin, Eureka, Config Server)
kubectl --context dev-cluster apply -f kubernetes/dev/service-discovery-deployment.yaml
kubectl --context dev-cluster apply -f kubernetes/dev/cloud-config-deployment.yaml
kubectl --context dev-cluster apply -f kubernetes/dev/zipkin-deployment.yaml
```

Repeat the same for `kubernetes/stage` and `kubernetes/prod` directories. The Jenkins pipelines also apply these manifests automatically, but pre-creating namespaces avoids first-run issues.

---

## 5. Jenkins Server Configuration

### 5.1 Install Jenkins (if not already available)

Option A – Docker container:

```bash
docker run \
  --name jenkins \
  -d \
  -p 8080:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts
```

Option B – Native installation using pkg/brew/war (follow official docs).

### 5.2 Plugins

Install and keep updated the following plugins:

- Pipeline, Git, Credentials Binding
- Docker Pipeline
- Kubernetes CLI or Kubectl
- HTML Publisher, JUnit
- Slack Notification, Email Extension
- Warnings NG (optional)

### 5.3 Global Tools

Under **Manage Jenkins → Global Tool Configuration** configure:

- JDK 11 (or point to system Java)
- Maven (or rely on wrapper)
- Git
- Python (optional) – used for Locust via shell commands

### 5.4 Credentials

Add credentials matching the Jenkinsfiles:

| ID | Type | Usage |
|----|------|-------|
| `dockerhub-credentials` | Username/Password | `withDockerRegistry` block for pushes |
| `git-credentials` | Username/Password or PAT | Tag push in production pipeline |
| `slack-token` (if needed) | Secret text | Slack notifications |
| `kubectl-config` (optional) | Secret file | If you prefer injecting kubeconfig rather than using node-level config |

### 5.5 Job Setup

Recommended approach: **Multibranch Pipeline**.

1. `New Item → Multibranch Pipeline → Name: ecommerce-microservice`.
2. Under **Branch Sources**, add GitHub repo URL and credentials (if private).
3. Configure build retention, scan interval, etc.

Jenkins will automatically discover the `dev`, `stage`, and `prod` branches and use the corresponding Jenkinsfiles. Each file enforces that it runs only on its designated branch.

If you prefer separate jobs, create three Pipeline jobs pointing to each branch and ensure the `Checkout` stage is configured with the correct branch name.

---

## 6. Running the Pipelines

### 6.1 Development Environment (`dev` Branch)

Trigger the pipeline by committing to `dev` or manually from Jenkins. The pipeline stages:

1. **Checkout** – validates current branch and shows latest commit.
2. **Build Services** – `mvn clean package -DskipTests` per microservice.
3. **Build Docker Images** – builds/tag images with `${BUILD_NUMBER}` and `dev-latest`.
4. **Push to Registry** – pushes tags using `dockerhub-credentials`.
5. **Deploy to Kubernetes Dev** – ensures namespace, applies ConfigMap, updates deployments.
6. **Smoke Tests** – curls health endpoints of API Gateway and User Service.

Monitor Jenkins logs. On success, verify pods:

```bash
kubectl --context dev-cluster get pods -n ecommerce-dev
```

### 6.2 Stage Environment (`stage` Branch)

Merge `dev` into `stage` (see promotion workflow below) and trigger the stage pipeline. Additional steps performed here:

- Runs unit tests (`mvn clean test package`) for each service.
- Publishes Surefire and JaCoCo reports.
- Security scan (Trivy) if available.
- Deploys Zipkin, Service Discovery, Config Server first, then microservices.
- Executes integration tests (`tests/integration`) and end-to-end tests (`tests/e2e`).
- Runs Locust performance tests from `tests/performance` and publishes HTML/CSV results.
- Applies a quality gate (error rate < 5%, avg response < 2s).

After completion:

```bash
kubectl --context stage-cluster get pods -n ecommerce-stage
```

### 6.3 Production Environment (`prod` Branch)

Merge `stage` into `prod`, then trigger the prod pipeline. It includes:

- Pre-flight checks (ensures branch = prod, clean workspace).
- Semantic version calculation based on `RELEASE_TYPE` parameter.
- Build, tests (optional skip), Sonar/trivy scans.
- Push images tagged with semantic version and `latest`.
- Optional system tests against stage before approval gate.
- Manual approval step with release summary.
- Rolling deployments to `ecommerce-prod` namespace.
- Post-deployment health verification.
- Release notes generation, Git tag creation, Slack/email notifications.
- Automatic rollback on failure via `kubectl rollout undo`.

Verify production services:

```bash
kubectl --context prod-cluster get pods -n ecommerce-prod
kubectl --context prod-cluster get svc -n ecommerce-prod
```

---

## 7. Promotion Workflow

To promote code safely between environments:

```bash
# After validating DEV
git checkout stage
git merge dev
git push origin stage

# After validating STAGE
git checkout prod
git merge stage
git push origin prod
```

Each push triggers the matching Jenkins pipeline thanks to the multibranch job.

---

## 8. Local Smoke Testing (Optional)

Before hitting Jenkins, you can run the stack locally:

```bash
# Build images with local tag
docker compose build

# Start services (requires dependent containers like databases if defined)
docker compose up

# Check health endpoints
curl http://localhost:8080/actuator/health   # API Gateway
curl http://localhost:8700/actuator/health   # User Service
```

Stop the stack with `docker compose down` when finished.

---

## 9. Troubleshooting Tips

- **Docker build fails**: check `target` directory for missing JARs; rerun `./mvnw clean package`.
- **Kubernetes rollout stuck**: `kubectl describe pod <name> -n <namespace>` for events; ensure Config Server/Eureka reachable.
- **Pipeline aborts due to branch mismatch**: ensure Jenkins job is checking out the intended branch, or adjust the guard helper if you use different naming.
- **Locust errors**: verify Python dependencies via `pip3 install -r tests/performance/requirements.txt` on the Jenkins agent.
- **Registry push denied**: confirm `dockerhub-credentials` are valid and Jenkins agent has Docker socket access.

---

## 10. Next Steps & Enhancements

- Configure webhook from GitHub to Jenkins for automatic scans.
- Add branch protection rules in GitHub (reviews, CI status) for `dev`, `stage`, `prod`.
- Integrate monitoring dashboards (Prometheus/Grafana) and log aggregation.
- Automate release version bumping via a script if desired.

---

Following this runbook ensures the entire CI/CD pipeline, from source to production deployment, executes consistently across development, staging, and production environments.
