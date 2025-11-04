#!/bin/bash
set -e

echo "🚀 Configurando kubeconfig para Jenkins en Docker"
echo "=================================================="
echo ""

# Verificar Minikube
if ! minikube status | grep -q "Running"; then
    echo "❌ Minikube no está corriendo. Ejecuta: minikube start"
    exit 1
fi

# Obtener IP de Minikube
MINIKUBE_IP=$(minikube ip)
echo "✅ Minikube IP: $MINIKUBE_IP"

# Verificar conectividad desde Jenkins container
echo "🔍 Verificando conectividad desde Jenkins..."
if docker exec jenkins ping -c 2 $MINIKUBE_IP &>/dev/null; then
    echo "✅ Jenkins puede alcanzar Minikube"
else
    echo "⚠️  Warning: Jenkins no puede hacer ping a Minikube"
    echo "   Esto puede funcionar de todas formas..."
fi

# Generar kubeconfig con certificados embebidos
echo "📝 Generando kubeconfig con certificados..."
kubectl config view --raw --minify --flatten | \
  sed "s|server:.*|server: https://${MINIKUBE_IP}:8443|g" > jenkins-stage-kubeconfig.yaml

# Asegurar que tenga el namespace
if ! grep -q "namespace: ecommerce-stage" jenkins-stage-kubeconfig.yaml; then
    # Para macOS
    sed -i '' 's/context:/context:\
    namespace: ecommerce-stage/' jenkins-stage-kubeconfig.yaml 2>/dev/null || \
    # Para Linux
    sed -i 's/context:/context:\n    namespace: ecommerce-stage/' jenkins-stage-kubeconfig.yaml
fi

echo "✅ Kubeconfig generado: jenkins-stage-kubeconfig.yaml"
echo ""

# Probar desde tu máquina
echo "🧪 Probando desde tu máquina local..."
if kubectl --kubeconfig=jenkins-stage-kubeconfig.yaml cluster-info &>/dev/null; then
    echo "✅ Conexión exitosa desde host"
else
    echo "❌ Error de conexión desde host"
    exit 1
fi

# Probar desde Jenkins container
echo "🧪 Probando desde Jenkins container..."
docker cp jenkins-stage-kubeconfig.yaml jenkins:/tmp/test-kubeconfig.yaml
if docker exec jenkins kubectl --kubeconfig=/tmp/test-kubeconfig.yaml cluster-info &>/dev/null; then
    echo "✅ Conexión exitosa desde Jenkins container"
    docker exec jenkins rm /tmp/test-kubeconfig.yaml
else
    echo "❌ Error de conexión desde Jenkins container"
    echo "   Verifica la red entre Jenkins y Minikube"
    docker exec jenkins rm /tmp/test-kubeconfig.yaml
    exit 1
fi

echo ""
echo "=================================================="
echo "✅ ¡TODO LISTO!"
echo "=================================================="
echo ""
echo "📋 Próximos pasos:"
echo ""
echo "1. Copia el contenido del kubeconfig:"
echo "   cat jenkins-stage-kubeconfig.yaml"
echo ""
echo "2. Ve a Jenkins:"
echo "   http://localhost:8080"
echo ""
echo "3. Navega a:"
echo "   Manage Jenkins → Credentials → System → Global credentials"
echo ""
echo "4. Busca y edita el credential: kubeconfig-stage"
echo ""
echo "5. Pega el contenido completo del archivo"
echo ""
echo "6. Guarda y ejecuta tu pipeline"
echo ""
echo "=================================================="