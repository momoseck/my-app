# Déploiement sur un VPS Contabo — méthode manuelle (Docker Compose)

Guide complet et prêt à l'emploi pour déployer l'API Todolist sur un serveur
Contabo, avec Docker Compose (application + PostgreSQL + Nginx), sécurisation du
serveur et HTTPS optionnel.

> Pour un déploiement plus « clic-bouton » avec redéploiement automatique depuis
> Git, voir plutôt [DEPLOY-COOLIFY.md](./DEPLOY-COOLIFY.md).

Durée estimée : 20–30 minutes.

---

## 1. Commander le VPS

Sur [contabo.com](https://contabo.com), commande un **VPS** (le plus petit,
« VPS 10 », suffit largement pour cette application).

Lors de la configuration :

- **Système d'exploitation** : Ubuntu 24.04 LTS
- **Region** : la plus proche de tes utilisateurs
- **Type d'authentification** : idéalement une **clé SSH** (voir §3). Sinon,
  Contabo t'envoie un mot de passe root par e-mail.

Une fois le serveur provisionné, tu reçois son **adresse IP publique**.
Dans la suite, remplace `IP_DU_SERVEUR` et `ton-domaine.com` par tes valeurs.

---

## 2. Première connexion

Depuis ton ordinateur (terminal Linux/macOS, ou PowerShell / Windows Terminal) :

```bash
ssh root@IP_DU_SERVEUR
```

Accepte l'empreinte, puis saisis le mot de passe root reçu (ou utilise ta clé).

Mets le système à jour immédiatement :

```bash
apt update && apt upgrade -y
```

---

## 3. Sécuriser le serveur

Ne pas exposer un serveur en root avec mot de passe. On crée un utilisateur
dédié, on active le pare-feu et on durcit SSH.

### 3.1 Créer un utilisateur sudo

```bash
adduser deploy
usermod -aG sudo deploy
```

### 3.2 Installer ta clé SSH (recommandé)

Depuis **ton ordinateur** (pas le serveur), si tu n'as pas encore de clé :

```bash
ssh-keygen -t ed25519 -C "todolist-deploy"
ssh-copy-id deploy@IP_DU_SERVEUR
```

Teste la connexion dans un nouveau terminal : `ssh deploy@IP_DU_SERVEUR`.
Tu dois pouvoir te connecter **sans mot de passe**.

### 3.3 Durcir la configuration SSH

Sur le serveur (connecté en `deploy`) :

```bash
sudo nano /etc/ssh/sshd_config
```

Règle (ou ajoute) ces lignes :

```
PermitRootLogin no
PasswordAuthentication no
```

Puis recharge SSH :

```bash
sudo systemctl restart ssh
```

> ⚠️ Garde ta session actuelle ouverte et vérifie dans un **autre terminal** que
> tu peux toujours te connecter avant de fermer la session en cours.

### 3.4 Pare-feu (UFW)

```bash
sudo apt install -y ufw
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow OpenSSH        # port 22
sudo ufw allow 80/tcp         # HTTP
sudo ufw allow 443/tcp        # HTTPS
sudo ufw enable
sudo ufw status
```

### 3.5 Protection anti-brute-force (fail2ban)

```bash
sudo apt install -y fail2ban
sudo systemctl enable --now fail2ban
```

---

## 4. Installer Docker et Docker Compose

Le script officiel installe Docker Engine + le plugin Compose v2 :

```bash
curl -fsSL https://get.docker.com | sudo sh
```

Autorise `deploy` à utiliser Docker sans `sudo` :

```bash
sudo usermod -aG docker deploy
newgrp docker          # applique le groupe sans se déconnecter
docker --version
docker compose version
```

---

## 5. Récupérer le projet sur le serveur

### Option A — depuis Git (recommandé)

```bash
cd ~
git clone <URL_DE_TON_DEPOT> my-app
cd my-app
```

### Option B — copie depuis ton ordinateur

Depuis **ton ordinateur**, à la racine du projet :

```bash
rsync -av --exclude target --exclude .git ./ deploy@IP_DU_SERVEUR:~/my-app/
```

Puis sur le serveur : `cd ~/my-app`.

---

## 6. Configurer les variables d'environnement

```bash
cp .env.example .env
nano .env
```

À définir impérativement :

- `POSTGRES_PASSWORD` : un mot de passe fort. Pour en générer un :
  `openssl rand -hex 24`
- `HTTP_PORT` : laisse `80` (Nginx écoutera dessus).
- `APP_CORS_ALLOWED_ORIGINS` : en production, mets l'URL de ton frontend
  (ex. `https://app.ton-domaine.com`) au lieu de `*`.

Le fichier `.env` est ignoré par Git : il ne sera jamais committé.

---

## 7. Démarrer l'application

```bash
docker compose up -d --build
```

Au premier lancement, Docker :

1. compile le `.jar` de l'application dans un conteneur Maven (JDK 21) ;
2. démarre PostgreSQL (volume persistant `db-data`) ;
3. lance les migrations Flyway (création de la table `todos`) ;
4. démarre l'API et le reverse proxy Nginx.

Vérifie que tout tourne :

```bash
docker compose ps
curl http://localhost/actuator/health      # -> {"status":"UP"}
curl http://localhost/api/todos            # -> liste paginée (vide)
```

L'API est joignable depuis l'extérieur sur `http://IP_DU_SERVEUR/`.

Petit test d'écriture :

```bash
curl -X POST http://localhost/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"Première tâche","priority":"HIGH"}'
```

---

## 8. Nom de domaine + HTTPS (optionnel mais recommandé)

### 8.1 Pointer le domaine

Chez ton registrar / gestionnaire DNS, crée un enregistrement **A** :

```
ton-domaine.com   A   IP_DU_SERVEUR
```

Attends la propagation (`ping ton-domaine.com` doit renvoyer l'IP du serveur).

### 8.2 Activer le HTTPS automatique avec Caddy

La façon la plus simple d'obtenir un certificat Let's Encrypt automatique est de
remplacer Nginx par **Caddy** (renouvellement TLS géré tout seul).

Crée un fichier `Caddyfile` à la racine du projet :

```
ton-domaine.com {
    reverse_proxy app:8080
}
```

Crée un fichier `docker-compose.override.yml` à la racine (Compose le fusionne
automatiquement) :

```yaml
services:
  # désactive Nginx en le remplaçant par Caddy
  nginx:
    profiles: ["disabled"]

  caddy:
    image: caddy:2-alpine
    container_name: todolist-caddy
    restart: unless-stopped
    depends_on:
      - app
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy-data:/data
      - caddy-config:/config
    networks:
      - backend

volumes:
  caddy-data:
  caddy-config:
```

Puis relance :

```bash
docker compose up -d
```

Caddy obtient et renouvelle le certificat automatiquement. L'API est alors sur
`https://ton-domaine.com/`.

---

## 9. Exploitation au quotidien

```bash
docker compose logs -f app        # suivre les logs de l'API
docker compose restart app        # redémarrer seulement l'API
docker compose ps                 # état des conteneurs
docker compose down               # tout arrêter (garde la base)
docker compose down -v            # tout arrêter ET supprimer la base
```

### Mettre à jour après une modification du code

```bash
cd ~/my-app
git pull
docker compose up -d --build
```

Flyway applique automatiquement les nouvelles migrations au démarrage.

### Sauvegarder / restaurer la base

```bash
# Sauvegarde
docker compose exec db pg_dump -U todolist todolist > backup_$(date +%F).sql

# Restauration
cat backup_2026-07-31.sql | docker compose exec -T db psql -U todolist -d todolist
```

Pense à automatiser la sauvegarde (ex. une tâche `cron` quotidienne qui lance la
commande `pg_dump` ci-dessus et envoie le fichier vers un stockage distant).

---

## 10. Dépannage

- **Un conteneur redémarre en boucle** : `docker compose logs <service>` pour
  voir l'erreur.
- **`missing table [todos]`** : la base est dans un état incohérent. Réinitialise
  le volume : `docker compose down -v && docker compose up -d --build`.
- **Le port 80 est déjà utilisé** : un autre service (Apache/Nginx hôte) occupe
  le port. `sudo ss -tlnp | grep :80` puis arrête-le, ou change `HTTP_PORT`.
- **HTTPS ne s'active pas** : vérifie que le DNS pointe bien vers l'IP et que les
  ports 80 et 443 sont ouverts dans UFW (Let's Encrypt a besoin du port 80).
- **Impossible de se connecter en SSH après durcissement** : reconnecte-toi
  depuis le terminal resté ouvert et corrige `/etc/ssh/sshd_config`.

---

## Récapitulatif express

```bash
# Sur le VPS Contabo (Ubuntu 24.04), en utilisateur "deploy" :
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker deploy && newgrp docker
git clone <URL_DE_TON_DEPOT> my-app && cd my-app
cp .env.example .env && nano .env      # définir POSTGRES_PASSWORD
docker compose up -d --build
curl http://localhost/actuator/health
```
