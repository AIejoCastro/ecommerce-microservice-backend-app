# Release v1.1.0
## Changes since v1.1.0

### Chores / Misc
- 9e61439 Update Nexus URL in Jenkinsfile and modify security group in ingress configuration for Nexus Sonatype. (AIejoCastro)
- 064fa82 Update application.yml to set context-path to root for improved service discovery configuration. (AIejoCastro)
- 9156172 Update application.yml to remove context-path value for Eureka client configuration. (AIejoCastro)
- cffac88 Refactor Jenkinsfile to update artifact upload stage with dynamic versioning and change Nexus URL for artifact storage. Update security group in ingress configuration for Nexus Sonatype. (AIejoCastro)
- 2401a1b Update Jenkinsfile (AIejoCastro)
- 5d49150 Implement deployment stage for Ingress in Jenkinsfile, update application.yml for Eureka client configuration, and modify Kubernetes deployment and service files to set namespaces and change service types to ClusterIP. Enhance service-discovery configuration with context path and Eureka settings. (AIejoCastro)
- b94d49b Enhance Jenkinsfile by reintroducing stages for kubeconfig configuration, namespace creation, and deployment of common configurations and core services. Update AWS role ARN and Docker image tags to 'dev' for improved deployment management. (AIejoCastro)

