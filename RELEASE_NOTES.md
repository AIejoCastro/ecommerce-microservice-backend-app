# Release v1.1.1
## Changes since v1.1.0

### Chores / Misc
- 592b45a fix: Correct Nexus URL formatting in Jenkinsfile for artifact upload stage. (AIejoCastro)
- 258acf9 fix: Remove Docker image push stage from Jenkinsfile, update Nexus URL in artifact upload stage, delete obsolete AWS Cloud Architecture image, and modify security group in ingress.yaml for Nexus Sonatype. (AIejoCastro)
- 4ebe8ce chore(release): v1.1.0 (AIejoCastro)
- cb2a59a Refactor Jenkinsfile to remove deprecated ingress deployments for service discovery, monitoring, and logging. Update ingress.yaml to define paths for Eureka and Zipkin services under a single ALB, streamlining the ingress configuration. (AIejoCastro)
- 6bc4231 chore(release): v1.1.0 (AIejoCastro)
- fcba518 Enhance Jenkinsfile to deploy additional ingresses for service discovery, monitoring, and logging, while updating ingress.yaml to group microservices under a shared ALB. Include checks for namespace existence before deploying monitoring and logging ingresses. (AIejoCastro)
- 9067bea Update binary files: .DS_Store and image 18.png with new versions. (AIejoCastro)
- 7207510 chore(release): v1.1.0 (AIejoCastro)
- f6f13a0 Remove deprecated ingress configuration for api-gateway from ingress.yaml, streamlining the deployment setup. (AIejoCastro)
- f227376 Enhance Jenkinsfile to include JSON report generation for ZAP scans alongside HTML reports, ensuring both formats are available for service security assessments. (AIejoCastro)
- 4d4eebf Update Jenkinsfile to deploy api-gateway and add new services to appServices list. Change namespace for common-config, api-gateway, and shipping-service configurations to 'microservices'. Modify ingress configuration to include api-gateway service. Update security group for Nexus Sonatype ingress. (AIejoCastro)
- 97554fc Documentation (AIejoCastro)
- d27e9d5 chore(release): v1.1.0 (AIejoCastro)
- 3dca5eb Update Nexus URL in Jenkinsfile and change security group in ingress.yaml for Nexus Sonatype. (AIejoCastro)
- b1de42a chore(release): v1.1.0 (AIejoCastro)
- 9e61439 Update Nexus URL in Jenkinsfile and modify security group in ingress configuration for Nexus Sonatype. (AIejoCastro)
- 064fa82 Update application.yml to set context-path to root for improved service discovery configuration. (AIejoCastro)
- 9156172 Update application.yml to remove context-path value for Eureka client configuration. (AIejoCastro)
- cffac88 Refactor Jenkinsfile to update artifact upload stage with dynamic versioning and change Nexus URL for artifact storage. Update security group in ingress configuration for Nexus Sonatype. (AIejoCastro)
- 2401a1b Update Jenkinsfile (AIejoCastro)
- 5d49150 Implement deployment stage for Ingress in Jenkinsfile, update application.yml for Eureka client configuration, and modify Kubernetes deployment and service files to set namespaces and change service types to ClusterIP. Enhance service-discovery configuration with context path and Eureka settings. (AIejoCastro)
- b94d49b Enhance Jenkinsfile by reintroducing stages for kubeconfig configuration, namespace creation, and deployment of common configurations and core services. Update AWS role ARN and Docker image tags to 'dev' for improved deployment management. (AIejoCastro)

