# Despliegue de NutriVida en AWS

Esta guía levanta el sistema completo en una instancia EC2 con Docker Compose:

```
Internet ──► EC2 :80 ──► nginx (frontend React)
                            ├── /api/...         ──► api-gateway :8080 ──► ms-identidad :8081
                            └── /swagger-ui.html ──►                    ├─► ms-catalogo  :8082
                                                                        ├─► ms-ordenes   :8083
                                                                        ├─► ms-citas     :8084
                                                                        └─► ms-clinico   :8085
                                                     MySQL 8.4 :3306 (red interna, volumen persistente)
```

Solo el puerto 80 queda abierto al público. No hace falta instalar Docker, Java ni MySQL en tu computador:
todo se construye y se ejecuta en el servidor.

Tiempo estimado: 1 a 2 horas la primera vez (la primera construcción tarda de 10 a 20 minutos).

---

## 1. Crear la instancia EC2

En la consola de AWS → **EC2** → **Launch instance**:

| Campo | Valor |
|---|---|
| Name | `nutrivida` |
| AMI | **Ubuntu Server 24.04 LTS** |
| Instance type | **t3.medium** (2 vCPU, 4 GB). Con menos memoria los 6 servicios Java no caben. |
| Key pair | Crear una nueva (`nutrivida-key`, tipo RSA, formato `.pem`) y guardar el archivo. |
| Network settings → Security group | Crear uno nuevo con estas reglas de entrada: |
| | **SSH** (22) · Source: *My IP* |
| | **HTTP** (80) · Source: *Anywhere (0.0.0.0/0)* |
| Storage | **20 GB** gp3 |

Presiona **Launch instance** y espera a que el estado sea **Running**. Copia la **Public IPv4 address**.

> **AWS Academy (Learner Lab):** inicia el laboratorio con *Start Lab* y entra a la consola con el botón *AWS*.
> La sesión dura unas 4 horas; al terminar la instancia se detiene. La próxima vez basta con
> *Start instance* (los contenedores se levantan solos), pero la IP pública cambia. Si quieres una IP
> fija, en EC2 → *Elastic IPs* → *Allocate* y luego *Associate* con la instancia.
> Si el laboratorio no permite `t3.medium`, avisa: se puede ajustar la memoria de los servicios.

**Pantallazo para el Anexo 2 (imagen 6):** la instancia en estado *Running* con su IP pública.

## 2. Conectarse al servidor

Opción fácil: en la consola, selecciona la instancia → **Connect** → **EC2 Instance Connect** → **Connect**.

Opción con terminal (Mac):

```bash
chmod 400 ~/Downloads/nutrivida-key.pem
ssh -i ~/Downloads/nutrivida-key.pem ubuntu@<IP-PUBLICA>
```

## 3. Preparar el servidor (una sola vez)

Copia y pega estos comandos en la terminal del servidor:

```bash
# Docker, Docker Compose y Git desde los repositorios de Ubuntu
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2 git
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu

# Memoria de intercambio (swap) de 4 GB, para que la compilación no se quede sin memoria
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

Cierra la sesión (`exit`) y vuelve a conectarte para que el usuario pueda usar Docker sin `sudo`.
Comprueba con:

```bash
docker --version
docker compose version
```

## 4. Descargar el código

Los dos repositorios deben quedar en la misma carpeta, con estos nombres:

```bash
cd ~
git clone https://github.com/MagePower91/NutriVida-Backend.git
git clone https://github.com/Anys-VS/NutriVida-React.git
```

> Si quieres desplegar una rama distinta de la principal, agrega `-b nombre-rama` a cada `git clone`.

## 5. Configurar los secretos

```bash
cd ~/NutriVida-Backend
cp .env.example .env
nano .env
```

Cambia `DB_PASSWORD` y `JWT_SECRET` (mínimo 32 caracteres). Puedes generar una clave con:

```bash
openssl rand -base64 48
```

Guarda con `Ctrl+O`, `Enter` y sal con `Ctrl+X`. El archivo `.env` no se sube a GitHub.

## 6. Construir y levantar todo

```bash
cd ~/NutriVida-Backend
docker compose up -d --build
```

La primera vez descarga imágenes y dependencias y compila los 7 proyectos (10 a 20 minutos).
Las siguientes veces es mucho más rápido.

## 7. Verificar

```bash
docker compose ps
```

Todos los contenedores deben aparecer como **running** (mysql como **healthy**). Los servicios Java
tardan 1 o 2 minutos en terminar de arrancar.

Luego abre en el navegador:

| Qué | URL |
|---|---|
| Sitio | `http://<IP-PUBLICA>` |
| Swagger | `http://<IP-PUBLICA>/swagger-ui.html` |

Cuentas de prueba: las del README (por ejemplo `paciente@gmail.com`).

**Pantallazos:** `docker compose ps` (manual, imagen 1), Swagger (manual imagen 2 y Anexo 2 imagen 1)
y el sitio funcionando (Anexo 2, imagen 7).

---

## Comandos útiles

| Para | Comando |
|---|---|
| Ver el estado | `docker compose ps` |
| Ver los registros de un servicio | `docker compose logs -f ms-identidad` |
| Ver los últimos errores de todos | `docker compose logs --tail=50` |
| Reiniciar un servicio | `docker compose restart ms-citas` |
| Actualizar con cambios nuevos de GitHub | `git -C ~/NutriVida-Backend pull && git -C ~/NutriVida-React pull && docker compose up -d --build` |
| Detener todo (los datos se conservan) | `docker compose down` |
| Borrar también la base de datos | `docker compose down -v` (¡borra todos los datos!) |
| Ver la memoria usada | `docker stats --no-stream` |

## Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Falta DB_PASSWORD en el archivo .env` | No existe el `.env` | Paso 5. |
| El sitio no abre | Puerto 80 cerrado o contenedores detenidos | Revisar el grupo de seguridad y `docker compose ps`. |
| «No pudimos conectar con el servidor» | El gateway o un servicio todavía está arrancando o falló | Esperar 2 minutos; luego `docker compose logs api-gateway` y del servicio. |
| Un servicio se reinicia una y otra vez | No conecta a MySQL o falta memoria | `docker compose logs <servicio>`; revisar `DB_PASSWORD` y `docker stats`. |
| `Invalid CORS request` | El navegador usa un origen distinto | En `.env` agregar `CORS_ORIGENES=http://<IP-PUBLICA>` y `docker compose up -d`. |
| La compilación se detiene sin memoria | Falta swap | Repetir la parte de swap del paso 3. |
| Cambió la IP (Learner Lab) | La instancia se reinició | Usar la IP nueva o asociar una Elastic IP. |

## Desarrollo local (sin Docker)

Sigue igual que antes: cada servicio usa `localhost` por defecto. Las variables `IDENTIDAD_URL`,
`CATALOGO_URL`, `ORDENES_URL`, `CITAS_URL`, `CLINICO_URL` y `CORS_ORIGENES` solo se usan en Docker.
