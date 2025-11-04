#!/bin/bash
# Script to generate a proper kubeconfig for stage environment

set -euo pipefail

TARGET_NAMESPACE=${1:-ecommerce-dev}
OUTPUT_FILE=${2:-stage-kubeconfig.yaml}
SOURCE_CONTEXT=${3:-minikube}
ACCESS_HOST=${MINIKUBE_ACCESS_HOST:-}

echo "🔧 Generating stage kubeconfig from context '${SOURCE_CONTEXT}' (namespace: ${TARGET_NAMESPACE})..."

if ! command -v kubectl >/dev/null 2>&1; then
    echo "❌ Error: kubectl is required but not found in PATH"
    exit 1
fi

if ! command -v minikube >/dev/null 2>&1; then
    echo "❌ Error: minikube CLI is required but not found in PATH"
    exit 1
fi

if ! minikube status >/dev/null 2>&1; then
    echo "❌ Error: Minikube is not running. Please start it first (minikube start)."
    exit 1
fi

# Resolve cluster server information from the selected context
SERVER_URL=$(kubectl config view --raw -o jsonpath="{.clusters[?(@.name=='${SOURCE_CONTEXT}')].cluster.server}")

if [[ -z "${SERVER_URL}" ]]; then
    echo "❌ Error: Could not find a cluster entry for context '${SOURCE_CONTEXT}' in the current kubeconfig."
    exit 1
fi

SERVER_HOST=$(echo "${SERVER_URL}" | sed -E 's#https://([^:/]+).*#\1#')
SERVER_PORT=$(echo "${SERVER_URL}" | sed -E 's#https://[^:/]+:([0-9]+).*#\1#')

if [[ -z "${SERVER_PORT}" ]]; then
    # Default LoadBalancer/secure port for kube-apiserver
    SERVER_PORT=8443
fi

if [[ -z "${ACCESS_HOST}" ]]; then
    if [[ "${SERVER_HOST}" == "127.0.0.1" || "${SERVER_HOST}" == "localhost" ]]; then
        ACCESS_HOST=$(minikube ip)
        echo "ℹ️  Server host is loopback; using Minikube IP ${ACCESS_HOST}"
    else
        ACCESS_HOST=${SERVER_HOST}
    fi
else
    echo "ℹ️  Using host override from MINIKUBE_ACCESS_HOST=${ACCESS_HOST}"
fi

CLUSTER_CA=$(kubectl config view --raw -o jsonpath="{.clusters[?(@.name=='${SOURCE_CONTEXT}')].cluster.certificate-authority-data}")
CLIENT_CERT=$(kubectl config view --raw -o jsonpath='{.users[0].user.client-certificate-data}')
CLIENT_KEY=$(kubectl config view --raw -o jsonpath='{.users[0].user.client-key-data}')

cat > "${OUTPUT_FILE}" <<EOF
apiVersion: v1
kind: Config
current-context: ${SOURCE_CONTEXT}-stage
clusters:
- name: ${SOURCE_CONTEXT}-stage
  cluster:
    $( [[ -n "${CLUSTER_CA}" ]] && printf "certificate-authority-data: %s\n" "${CLUSTER_CA}" )
    insecure-skip-tls-verify: true
    server: https://${ACCESS_HOST}:${SERVER_PORT}
contexts:
- name: ${SOURCE_CONTEXT}-stage
  context:
    cluster: ${SOURCE_CONTEXT}-stage
    namespace: ${TARGET_NAMESPACE}
    user: ${SOURCE_CONTEXT}-stage-user
users:
- name: ${SOURCE_CONTEXT}-stage-user
  user:
    client-certificate-data: ${CLIENT_CERT}
    client-key-data: ${CLIENT_KEY}
EOF

echo "✅ Stage kubeconfig written to ${OUTPUT_FILE}"
echo ""
echo "📝 Next steps:"
echo "1. Upload ${OUTPUT_FILE} to Jenkins as/into the credential 'kubeconfig-stage'."
echo "2. If Jenkins runs inside Docker and cannot reach Minikube IP, rerun with MINIKUBE_ACCESS_HOST=host.docker.internal."
echo "   Example: MINIKUBE_ACCESS_HOST=host.docker.internal ./scripts/generate-stage-kubeconfig.sh"
echo "3. Re-run the stage pipeline to verify connectivity."

