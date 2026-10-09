# Scénarios de test

## S1 — Cas normal

- Départ : étage 3
- Destination : étage 8
- Type : Normal
- Résultat attendu : CFP envoyé aux trois ascenseurs, score minimal sélectionné, déplacement visible.

## S2 — PMR vs Normal

Utiliser le bouton **PMR + utilisateur normal**.

Résultat attendu : lorsque les demandes arrivent presque simultanément, le coordinateur choisit en premier celle ayant la priorité effective la plus forte.

## S3 — Plusieurs demandes

Utiliser le bouton **Plusieurs demandes**.

Observer :

- la file de décisions ;
- les différentes propositions ;
- la charge de travail qui pénalise un ascenseur déjà occupé ;
- la distribution progressive des demandes.

## S4 — Urgence

Utiliser **Urgence prioritaire**.

Résultat attendu : la demande Urgence possède la priorité de base 7 et passe avant une demande normale concurrente.

## S5 — Panne

1. Lancer plusieurs demandes.
2. Cliquer sur `Panne` pour l'ascenseur B.
3. Observer le statut rouge et l'arrêt du mouvement.
4. Les nouveaux CFP reçoivent un `REFUSE` de B.
5. Cliquer sur `Réactiver`.
6. B reprend son travail.

## S6 — Anti-starvation

Créer plusieurs demandes prioritaires puis une demande normale. Tant que la demande normale attend, sa priorité effective augmente progressivement. Cette stratégie limite le risque de famine.
