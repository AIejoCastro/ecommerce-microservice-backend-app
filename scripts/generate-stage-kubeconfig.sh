#!/bin/bash
# Script to generate a proper kubeconfig for stage environment

set -e

echo "🔧 Generating stage kubeconfig from minikube..."

# Get the current kubeconfig
KUBECONFIG_PATH="${HOME}/.kube/config"

# Check if minikube is running
if ! minikube status > /dev/null 2>&1; then
    echo "❌ Error: Minikube is not running. Please start minikube first."
    echo "   Run: minikube start"
    exit 1
fi

# Get minikube cluster info
MINIKUBE_IP=$(minikube ip)
MINIKUBE_PORT=$(kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}' | sed 's|https://[^:]*:||')

echo "📋 Minikube IP: ${MINIKUBE_IP}"
echo "📋 API Server Port: ${MINIKUBE_PORT}"

# Create a temporary kubeconfig
TEMP_CONFIG=$(mktemp)
kubectl config view --flatten > "${TEMP_CONFIG}"

# Create stage-specific kubeconfig
STAGE_CONFIG="stage-kubeconfig.yaml"

cat > "${STAGE_CONFIG}" <<EOF
apiVersion: v1
kind: Config
current-context: minikube-stage
clusters:
- name: minikube-stage
  cluster:
    insecure-skip-tls-verify: true
    server: https://host.docker.internal:${MINIKUBE_PORT}
contexts:
- name: minikube-stage
  context:
    cluster: minikube-stage
    user: minikube
    namespace: ecommerce-stage
users:
- name: minikube
  user:
    client-certificate-data: $(kubectl config view --raw -o jsonpath='{.users[0].user.client-certificate-data}')
    client-key-data: $(kubectl config view --raw -o jsonpath='{.users[0].user.client-key-data}')
EOF

# Clean up
rm -f "${TEMP_CONFIG}"

echo "✅ Stage kubeconfig generated: ${STAGE_CONFIG}"
echo ""
echo "📝 Next steps:"
echo "1. Update the Jenkins credential 'kubeconfig-stage' with the contents of ${STAGE_CONFIG}"
echo "2. Go to Jenkins → Manage Jenkins → Credentials → kubeconfig-stage"
echo "3. Update the credential with the new kubeconfig content"

