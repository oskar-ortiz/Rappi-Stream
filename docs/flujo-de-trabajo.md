# Flujo de trabajo del equipo

## Ramas

- `dev` es la rama por defecto e integración diaria. Toda contribución se integra mediante pull request hacia `dev`.
- `main` es solo producción. La persona responsable (provisionalmente `oskar-ortiz`) abre manualmente un PR de `dev` a `main` cuando se aprueba una entrega.
- Ramas asignadas: `feature/dominio` para Persona A, `feature/mensajeria` para Persona B y `feature/infra` para Persona C.

La rama de trabajo del equipo se crea desde `dev`. Para actualizar una rama local de trabajo:

```powershell
git switch feature/dominio
git pull origin dev
```

Resuelve conflictos en tu rama, ejecuta las pruebas y publica el resultado; no hagas push directo a `dev` o `main`.

## Commits y pull requests

Usa Conventional Commits en inglés: `feat`, `fix`, `docs`, `chore`, `test`, `refactor`, `build` o `ci` (por ejemplo, `feat(order-api): add request validation`).

1. Trabaja únicamente en tu rama asignada.
2. Ejecuta `mvn -B clean verify`; si cambias infraestructura, valida además `docker compose config`.
   Para cambios del dashboard, valida el runtime con `docker compose up -d --build` y los scripts de humo descritos en el README.
3. Publica tu rama y abre un PR hacia `dev`.
4. Completa la plantilla, vincula el issue correspondiente y solicita revisión.
5. Integra mediante squash cuando las verificaciones requeridas pasen y las conversaciones estén resueltas.

## Archivos compartidos

Solo la persona encargada del repositorio (`@oskar-ortiz`) debe modificar directamente estos archivos compartidos: `pom.xml` raíz, `docker-compose.yml`, `.env.example` y `docs/contratos.md`. Coordina cambios transversales mediante PR y acuerdo del equipo. Los cambios de contratos siempre requieren acuerdo de las tres personas.

## Decisiones iniciales

- Se conservó el repositorio existente `oskar-ortiz/Rappi-Stream`; no se creó otro ni se modificó su visibilidad.
- La línea Spring Boot está fijada en `3.2.12`; las dependencias se administran mediante su BOM.
- El topic se crea mediante el servicio efímero `kafka-init`; los datos se guardan en volúmenes con nombre y `docker compose down` no los elimina.
- Compose levanta por defecto infraestructura y los dos servicios implementados del dashboard. Usa el perfil `full` para iniciar los seis servicios aún sin lógica funcional.
- `.env.example` contiene únicamente credenciales marcadoras `change-me`; `.env` se excluye de Git.
- CODEOWNERS asigna provisionalmente la propiedad a `@oskar-ortiz`; los usuarios reales de Persona A/B no se inventan.

## Protección de ramas

La protección activa de `dev` exige PR, una aprobación, check `verify`, rama actualizada y conversaciones resueltas; también bloquea force-push y borrado. `main` exige PR, una aprobación y check `verify`, y bloquea force-push y borrado. `enforce_admins` está desactivado mientras el propietario sea el único colaborador.

La restricción de push de `main` **no pudo aplicarse**: GitHub respondió HTTP 422 (`Only organization repositories can have users and team restrictions`) porque el repositorio pertenece a una cuenta personal. La regla de `main` quedó activa sin restricciones de usuario; agregarla requiere migrar a una organización o una capacidad futura de GitHub para repositorios personales.

El estado efectivo debe consultarse con:

```powershell
gh api repos/oskar-ortiz/Rappi-Stream/branches/dev/protection
gh api repos/oskar-ortiz/Rappi-Stream/branches/main/protection
```

Si la API rechaza la restricción de push de un repositorio personal, añadirla después de migrar el repositorio a una organización o cuando GitHub permita administrar esa regla. No considerar una regla parcialmente aplicada como protección completa.

Para recrear reglas con `gh api`, preparar un JSON conforme a la API de protección de ramas. Ejemplo de cuerpo para `dev` (la regla `verify` debe existir primero como check de CI):

```powershell
$body = @{
  required_status_checks = @{ strict = $true; contexts = @("verify") }
  enforce_admins = $false
  required_pull_request_reviews = @{
    dismiss_stale_reviews = $false
    require_code_owner_reviews = $false
    required_approving_review_count = 1
  }
  restrictions = $null
  required_conversation_resolution = $true
  allow_force_pushes = $false
  allow_deletions = $false
} | ConvertTo-Json -Depth 8
$body | gh api --method PUT repos/oskar-ortiz/Rappi-Stream/branches/dev/protection --input -
```

Para recrear la protección efectiva actual de `main` (sin restricción de usuario disponible):

```powershell
$body = @{
  required_status_checks = @{ strict = $false; contexts = @("verify") }
  enforce_admins = $false
  required_pull_request_reviews = @{
    dismiss_stale_reviews = $false
    require_code_owner_reviews = $false
    required_approving_review_count = 1
  }
  restrictions = $null
  allow_force_pushes = $false
  allow_deletions = $false
} | ConvertTo-Json -Depth 8
$body | gh api --method PUT repos/oskar-ortiz/Rappi-Stream/branches/main/protection --input -
```

## Producción

La persona encargada de producción revisa el estado de `dev`, abre un PR manual de `dev` a `main` y lo integra cuando se aprueba el despliegue. Nadie usa `main` como rama de trabajo diario.
