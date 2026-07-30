# Déploiement sur Contabo avec Coolify

Guide complet et prêt à l'emploi pour déployer l'API Todolist sur un VPS Contabo
via **Coolify** — une plateforme auto-hébergée (type Heroku/Vercel) qui gère à ta
place le reverse proxy, le **HTTPS automatique (Let's Encrypt)**, le déploiement
depuis Git et le **redéploiement automatique** à chaque `git push`.

Ce guide utilise le fichier [`docker-compose.coolify.yml`](../docker-compose.coolify.yml)
présent à la racine du projet : il ne contient **pas** de service Nginx (Coolify
fournit déjà le proxy + TLS) et ne publie **aucun port** sur l'hôte.

> Pour un déploiement 100 % manuel sans Coolify, voir
> [DEPLOY-CONTABO.md](./DEPLOY-CONTABO.md).

Durée estimée : 15–20 minutes.

---

## 1. Prérequis

- Un **VPS Contabo** sous **Ubuntu 24.04 LTS**.
  Coolify demande au minimum **2 Go de RAM** (4 Go recommandés en production),
  2 vCPU et ~40 Go de disque. Sur Contabo, un « VPS 10 » convient ; prends un
  cran au-dessus si tu vises la production.
- Un **nom de domaine** (fortement recommandé pour le HTTPS automatique).
- Le projet poussé sur un dépôt **Git** (GitHub, GitLab…).

Sécurise d'abord le serveur en suivant les sections 2 et 3 de
[DEPLOY-CONTABO.md](./DEPLOY-CONTABO.md) (utilisateur sudo, clé SSH, UFW,
fail2ban). **Important pour le pare-feu** : Coolify a besoin des ports
**80, 443 et 8000** :

```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8000/tcp      # tableau de bord Coolify
```

---

## 2. Installer Coolify

Connecté au serveur (en root ou via `sudo`), lance le script officiel. Il
installe Docker + Compose si besoin, puis Coolify :

```bash
curl -fsSL https://cdn.coollabs.io/coolify/install.sh | bash
```

L'installation prend 2 à 5 minutes. À la fin, le tableau de bord est disponible
sur :

```
http://IP_DU_SERVEUR:8000
```

Ouvre cette URL dans ton navigateur et **crée le compte administrateur**
(premier utilisateur = admin). Termine l'assistant de démarrage (il détecte le
serveur local « localhost » comme cible de déploiement).

---

## 3. (Recommandé) Donner un domaine à Coolify

Chez ton registrar DNS, crée deux enregistrements **A** vers l'IP du serveur :

```
coolify.ton-domaine.com   A   IP_DU_SERVEUR     # accès au dashboard
todo.ton-domaine.com      A   IP_DU_SERVEUR     # ton API
```

Dans Coolify : **Settings → General → Instance Domain**, renseigne
`https://coolify.ton-domaine.com`. Coolify génère alors un certificat pour son
propre tableau de bord.

---

## 4. Connecter ton dépôt Git

Dans Coolify : **Sources** (ou lors de l'ajout de la ressource).

- **Dépôt public** : rien à configurer, tu colleras simplement l'URL du dépôt.
- **Dépôt privé GitHub** : crée une **GitHub App** depuis
  **Sources → + Add → GitHub App** et suis l'assistant (autorise l'accès au
  dépôt). C'est cette méthode qui active le redéploiement automatique par
  webhook.

---

## 5. Créer le projet et la ressource

1. **Projects → + Add** : crée un projet (ex. « todolist »), un environnement
   `production` est créé par défaut.
2. **+ New Resource → Docker Compose** (dépôt public), ou
   **Private Repository (with GitHub App)** pour un dépôt privé.
3. Sélectionne le **dépôt** et la **branche** (ex. `main`).
4. Coolify détecte le type. Si demandé, choisis explicitement le build pack
   **Docker Compose**.

---

## 6. Indiquer le bon fichier Compose

Par défaut Coolify lit `docker-compose.yml` (celui avec Nginx, destiné au
déploiement manuel). Il faut le pointer vers le fichier adapté à Coolify.

Dans l'onglet **General** de la ressource, règle **« Docker Compose Location »**
sur :

```
docker-compose.coolify.yml
```

(ou `/docker-compose.coolify.yml` selon l'affichage). Enregistre.

---

## 7. Configurer les variables d'environnement

Onglet **Environment Variables** de la ressource. Ajoute :

| Clé                    | Valeur                                             |
|------------------------|----------------------------------------------------|
| `POSTGRES_DB`          | `todolist`                                         |
| `POSTGRES_USER`        | `todolist`                                          |
| `POSTGRES_PASSWORD`    | `$SERVICE_PASSWORD_POSTGRES` (Coolify le génère)   |
| `APP_CORS_ALLOWED_ORIGINS` | `https://todo.ton-domaine.com` (ou `*`)        |

À propos du **domaine + HTTPS** : le fichier `docker-compose.coolify.yml`
déclare la variable magique `SERVICE_FQDN_APP_8080`. Coolify génère
automatiquement un domaine et un certificat TLS, et route le trafic vers le port
`8080` du service `app`. Pour imposer **ton** domaine plutôt qu'un domaine
généré, ajoute aussi :

| Clé                     | Valeur                          |
|-------------------------|---------------------------------|
| `SERVICE_FQDN_APP_8080` | `https://todo.ton-domaine.com`  |

> Les valeurs `$SERVICE_PASSWORD_*` et `$SERVICE_FQDN_*` sont des « magic
> variables » Coolify : elles sont générées une fois puis réutilisées de façon
> cohérente entre les services (l'app et la base partagent le même mot de passe).

---

## 8. Déployer

Clique sur **Deploy**.

Coolify va :

1. cloner le dépôt et lire `docker-compose.coolify.yml` ;
2. construire l'image de l'app via le `Dockerfile` (compilation Maven / JDK 21) ;
3. démarrer PostgreSQL puis l'app ; Flyway crée la table `todos` au démarrage ;
4. configurer son proxy Traefik + le certificat Let's Encrypt pour ton domaine.

Suis la progression dans l'onglet **Deployments / Logs**.

---

## 9. Vérifier

Une fois « Running » et le domaine actif :

```bash
curl https://todo.ton-domaine.com/actuator/health     # {"status":"UP"}
curl https://todo.ton-domaine.com/api/todos           # liste paginée (vide)

curl -X POST https://todo.ton-domaine.com/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"Première tâche via Coolify","priority":"HIGH"}'
```

Sans domaine, utilise l'URL générée par Coolify affichée dans l'onglet
**Configuration → Domains** du service `app`.

---

## 10. Redéploiement automatique

Avec la **GitHub App** (dépôt privé) ou un **webhook** (dépôt public), chaque
`git push` sur la branche suivie déclenche un redéploiement automatique. Tu peux
aussi lancer un déploiement manuel avec le bouton **Redeploy**, ou activer/gérer
le webhook dans l'onglet **Webhooks** de la ressource.

---

## 11. Sauvegardes

Coolify sait planifier des sauvegardes automatiques pour les bases de données :
ouvre le service **db** → onglet **Backups**, configure la fréquence et,
idéalement, une destination S3.

Sauvegarde manuelle à tout moment :

```bash
docker ps --filter "name=db" --format "{{.Names}}"     # trouver le conteneur
docker exec -t <conteneur_db> pg_dump -U todolist todolist > backup_$(date +%F).sql
```

---

## 12. Dépannage

- **Le build échoue** : ouvre les logs de déploiement. Vérifie que « Docker
  Compose Location » pointe bien sur `docker-compose.coolify.yml`.
- **502 / Bad Gateway** : l'app met quelques dizaines de secondes à démarrer
  (JVM + migrations). Attends, puis recharge. Vérifie les logs du service `app`.
- **Pas de HTTPS** : vérifie que le DNS de `todo.ton-domaine.com` pointe vers
  l'IP du serveur et que les ports 80/443 sont ouverts (Let's Encrypt utilise le
  port 80 pour la validation).
- **`missing table [todos]`** : base incohérente. Supprime le volume de la base
  dans Coolify (**db → Storages**) puis redéploie pour repartir propre.
- **Conflit de port 80/443** : n'exécute pas en parallèle un déploiement manuel
  (Nginx) et Coolify sur la même machine — ils se disputeraient les ports.
  Choisis l'une **ou** l'autre méthode.

---

## Manuel vs Coolify — que choisir ?

La méthode **manuelle** ([DEPLOY-CONTABO.md](./DEPLOY-CONTABO.md)) est plus
légère (moins de RAM), sans dépendance supplémentaire, idéale si tu es à l'aise
en ligne de commande. **Coolify** ajoute une interface web, le HTTPS et le
redéploiement Git automatiques, au prix d'un peu plus de ressources — pratique si
tu déploies souvent ou gères plusieurs applications.
