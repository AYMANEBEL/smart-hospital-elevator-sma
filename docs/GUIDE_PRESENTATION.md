# Guide de présentation orale — Smart Elevator SMA

## 1. Introduction (30 s)

« Notre projet traite la gestion intelligente des ascenseurs, en particulier pour les personnes qui dépendent fortement de l'ascenseur, comme les personnes âgées ou à mobilité réduite. L'objectif n'est pas seulement de choisir l'ascenseur le plus proche, mais de coordonner plusieurs ascenseurs autonomes en prenant en compte la priorité, la distance, la charge de travail, la direction et les pannes. »

## 2. Pourquoi un SMA ? (40 s)

Un ascenseur peut naturellement être vu comme un agent autonome : il possède son état, sa position, sa file de tâches et prend une décision locale. Un coordinateur ne connaît pas à l'avance la meilleure solution ; il demande donc aux ascenseurs de proposer leur coût.

Le problème devient une allocation distribuée de ressources.

## 3. Les agents (50 s)

- `UserAgent` crée la demande.
- `PriorityAgent` détermine sa priorité.
- `CoordinatorAgent` choisit quelle demande traiter et lance une négociation.
- chaque `ElevatorAgent` calcule son propre score.
- `MaintenanceAgent` gère la panne ou la réactivation.

## 4. Démonstration recommandée (2 min)

1. Lancer **PMR + utilisateur normal**.
2. Montrer dans les logs : `REQUEST`, priorité, `CFP`, `PROPOSE`, puis `ACCEPT_PROPOSAL`.
3. Montrer l'ascenseur sélectionné qui se déplace.
4. Ajouter plusieurs demandes.
5. Mettre un ascenseur en panne.
6. Montrer que les autres continuent à fonctionner.
7. Réactiver l'ascenseur.

## 5. Algorithmes (1 min)

### Ordonnancement des utilisateurs

`PrioritéEffective = PrioritéBase + bonusTempsAttente`

Cela permet de favoriser PMR/Urgence sans bloquer éternellement les utilisateurs normaux.

### Affectation de l'ascenseur

`Score = distance + workload + pénalité de direction`

Chaque ascenseur calcule sa proposition localement. Le coordinateur sélectionne le score minimum.

## 6. Conclusion (30 s)

« Le projet montre qu'un SMA est adapté lorsqu'il existe plusieurs ressources autonomes qui doivent coopérer. Le système est extensible : capteurs IoT, optimisation énergétique, prédiction du trafic ou intégration dans un hôpital ou un bâtiment public. »
