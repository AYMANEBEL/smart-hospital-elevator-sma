# Interface V6 — Multi-Agent Command Center

La version actuelle adopte une interface **Multi-Agent Command Center** pensée comme un vrai produit de supervision : navigation Live / Missions / Agents, jumeau numérique hospitalier, ascenseurs animés avec interpolation fluide, patients visibles, mission focus, réseau d’agents ACL animé et mode présentation plein écran. La logique JADE reste inchangée et pilote réellement la visualisation.

# Smart Hospital Mobility SMA

**Système Multi-Agent hospitalier pour la gestion intelligente et prioritaire des ascenseurs**

Projet académique de niveau Master réalisé en **Java + JADE + Swing**. Le système simule un bâtiment de **RDC + 10 étages** avec **3 ascenseurs autonomes**. Les agents communiquent par messages ACL afin d'affecter les demandes, prioriser les personnes à mobilité réduite, gérer les demandes simultanées et réagir à une panne.

## 1. Problème traité

Dans un système classique, un ascenseur peut être affecté uniquement selon la proximité. Cette stratégie ne tient pas forcément compte :

- des personnes à mobilité réduite (PMR) ;
- des personnes âgées ;
- des situations d'urgence ;
- de la charge de travail de chaque ascenseur ;
- de la direction courante ;
- du temps d'attente ;
- des pannes.

Le projet propose une coordination multi-agent avec une **priorité dynamique et équitable**.

## 2. Agents

| Agent | Rôle |
|---|---|
| `UserAgent` | Représente une demande utilisateur et l'envoie au système |
| `PriorityAgent` | Attribue une priorité de base selon le type d'utilisateur |
| `CoordinatorAgent` | Ordonne les demandes, lance la négociation et sélectionne le meilleur ascenseur |
| `ElevatorAgent` A/B/C | Calcule une proposition locale, exécute les trajets et maintient sa file |
| `MaintenanceAgent` | Transmet les commandes panne / réactivation |

## 3. Communication ACL

Flux principal :

```text
UserAgent
   │ REQUEST
   ▼
PriorityAgent
   │ REQUEST + priorité
   ▼
CoordinatorAgent
   │ CFP
   ├──────────────┬──────────────┐
   ▼              ▼              ▼
Elevator A     Elevator B     Elevator C
   │ PROPOSE       │ PROPOSE       │ PROPOSE
   └──────────────┴──────────────┘
                  │
                  ▼
           CoordinatorAgent
                  │
       ACCEPT / REJECT_PROPOSAL
                  ▼
           Elevator sélectionné
```

## 4. Politique de décision

### Priorité des demandes

Valeurs de base :

```text
Normal       = 1
Personne âgée= 2
PMR          = 4
Urgence      = 7
```

Pour éviter qu'un utilisateur normal attende indéfiniment :

```text
PrioritéEffective = PrioritéBase + min(5, 0.15 × TempsAttenteSecondes)
```

Cette règle introduit une notion de **fairness / anti-starvation**.

### Score d'un ascenseur

Pour une demande sélectionnée, chaque `ElevatorAgent` calcule localement :

```text
Score = 1.6 × Distance + 6 × Workload + DirectionPenalty
```

avec :

- `Distance` : distance entre la position projetée de l'ascenseur et l'étage d'appel ;
- `Workload` : nombre de demandes déjà affectées ;
- `DirectionPenalty` : pénalité lorsque le trajet demandé est opposé au mouvement courant.

**Le score le plus faible gagne.**

## 5. Interface graphique — V6 Command Center

La V6 remplace l’ancien dashboard par une application structurée en trois espaces :

- **LIVE** : jumeau numérique du bâtiment, 11 niveaux, trois gaines d’ascenseurs, cabines à mouvement visuel lissé, patients PMR/âgés/urgence, mission active, progression et décision SMA ;
- **MISSIONS** : historique des demandes, priorité, temps d’attente, ascenseur retenu et indicateurs de qualité de service ;
- **AGENTS** : architecture JADE animée montrant `UserAgent`, `PriorityAgent`, `CoordinatorAgent`, `MaintenanceAgent` et les trois `ElevatorAgent`, avec flux `REQUEST / CFP / PROPOSE / ACCEPT`.

La page LIVE contient également un **Agent Mesh** animé et un **mode présentation plein écran** pour la soutenance. Les éléments visuels sont alimentés par `AppState` : ils reflètent l’état réel des agents et ne sont pas une simple vidéo préenregistrée.

## 6. Scénarios prêts pour la démonstration

L'interface contient trois scénarios :

1. **PMR + utilisateur normal** : montre la priorisation.
2. **Plusieurs demandes** : montre la coordination et la répartition de charge.
3. **Urgence prioritaire** : montre le traitement d'une demande critique.

Pour tester une panne : lancer plusieurs demandes, puis cliquer sur **Panne** pour E1, E2 ou E3. Le `MaintenanceAgent` prévient l'ascenseur et son mouvement est suspendu jusqu'à réactivation.

## 7. Prérequis

- Java **17 ou supérieur**
- Maven **3.9+**
- Connexion Internet lors de la première compilation pour télécharger JADE

JADE utilisé pour ce build : **4.3** depuis Maven Central (fallback fiable pour éviter les erreurs de certificat du dépôt TILAB). Les API JADE utilisées par le projet sont compatibles.

## 8. Lancement sous Windows

### Méthode simple

Double-cliquer sur :

```text
run.bat
```

### Avec le terminal

```bash
mvn clean compile exec:java -Dexec.mainClass=com.smartelevator.Main
```

### Générer un JAR exécutable

```text
build.bat
```

ou :

```bash
mvn clean package
java -jar target/smart-elevator-sma-1.0.0.jar
```

## 9. Structure

```text
SmartElevatorSMA/
├── pom.xml
├── run.bat
├── build.bat
├── README.md
├── docs/
│   ├── GUIDE_PRESENTATION.md
│   └── SCENARIOS_TEST.md
└── src/main/java/com/smartelevator/
    ├── Main.java
    ├── agents/
    │   ├── UserAgent.java
    │   ├── PriorityAgent.java
    │   ├── CoordinatorAgent.java
    │   ├── ElevatorAgent.java
    │   └── MaintenanceAgent.java
    ├── core/
    │   ├── AppState.java
    │   ├── JadePlatform.java
    │   └── MaintenanceCommand.java
    ├── model/
    │   ├── Request.java
    │   ├── Proposal.java
    │   ├── UserType.java
    │   ├── RequestStatus.java
    │   ├── ElevatorStatus.java
    │   ├── Direction.java
    │   ├── ElevatorSnapshot.java
    │   └── RequestSnapshot.java
    └── gui/
        ├── MainFrame.java
        ├── HospitalCommandCenterPanel.java
        └── AgentNetworkPanel.java
```

## 10. Points à expliquer devant l'enseignante

Le projet n'utilise pas un « gros algorithme IA ». L'intelligence vient de l'**organisation distribuée** : plusieurs agents autonomes disposent d'informations locales, communiquent, proposent des solutions et coordonnent leurs décisions.

Les notions SMA visibles dans le projet sont :

- autonomie ;
- communication ACL ;
- coopération ;
- coordination ;
- négociation de type Contract-Net simplifié ;
- allocation dynamique de ressources ;
- priorité et équité ;
- tolérance à une panne simulée.

## 11. Extensions possibles

- capteurs IoT réels ;
- estimation prédictive du trafic ;
- optimisation énergétique ;
- plusieurs bâtiments ;
- détection de surcharge ;
- authentification par badge PMR ;
- comparaison expérimentale avec FCFS / nearest-elevator.
