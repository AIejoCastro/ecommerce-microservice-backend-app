# Taller 2 : Pruebas y lanzamiento

```
Para este ejercicio, debe configurar los pipelines necesarios para al menos seis
de los microservicios del código disponible en:
https://github.com/SelimHorri/ecommerce-microservice-backend-app/
Al escoger los microservicios, considere que los escogidos se comuniquen
entre sí para permitir la posterior implementación de pruebas que los
involucren.
Actividades a considerar:
 10% configurar Jenkins, Docker y Kubernetes para su utilización.
 15% Para los microservicios escogidos, debe definir los pipelines que
permitan la construcción de la aplicación (dev environment).
 30% En algunos de los microservicios, debe definir pruebas unitarias,
integración, E2E y rendimiento que involucren los microservicios:
Al menos cinco nuevas pruebas unitarias que validen componentes
individuales
Al menos cinco nuevas pruebas de integración que validen la
comunicación entre servicios
Al menos cinco nuevas pruebas E2E que validen flujos completos de
usuario
Pruebas de rendimiento y estrés utilizando Locust que simulen casos
de uso reales del sistema.
```
```
Todas las pruebas deben ser relevantes sobre funcionalidades existentes.
 15% Para los microservicios escogidos, debe definir los pipelines que
permitan la construcción incluyendo las pruebas de la aplicación
desplegada en Kubernetes (stage environment).
 15% Para los microservicios escogidos, debe ejecutar un pipeline de
despliegue, que realice la construcción incluyendo las pruebas unitarias,
valide las pruebas de sistema y posteriormente despliegue la aplicación en
```
Taller 2 Pruebas y lanzamiento 1


```
Kubernetes. Defina todas las fases que considere adecuadas (master
environment).Debe incluir la generación automática de Release Notes
siguiendo las buenas prácticas de Change Management.
 15% Adecuada documentación del proceso realizado.
```
```
Reporte de de los resultados: debe entregar un documento que contenga la
siguiente
información para cada uno de los pipelines:
Configuración: Texto de la configuración de los pipelines, con pantallazos
de configuración relevante en los mismos.
Resultado: pantallazos de la ejecución exitosa de los pipelines con los
detalles y resultados relevantes.
Análisis: interpretación de los resultados de las pruebas, especialmente las
de rendimiento, con métricas clave como tiempo de respuesta, throughput
y tasa de errores.
Release Notes: documentación de las versiones desplegadas en cada
ambiente.
Adicionalmente, un zip con las pruebas implementadas.
```
Taller 2 Pruebas y lanzamiento 2


