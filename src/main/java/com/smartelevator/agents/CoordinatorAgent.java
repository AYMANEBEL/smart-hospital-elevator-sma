package com.smartelevator.agents;

import com.smartelevator.core.AppState;
import com.smartelevator.model.Proposal;
import com.smartelevator.model.Request;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CoordinatorAgent extends Agent {
    private static final List<String> ELEVATORS = List.of("A", "B", "C");
    private final Map<String, PendingDecision> pending = new ConcurrentHashMap<>();
    private final Map<String, Request> incomingQueue = new ConcurrentHashMap<>();
    private final Map<String, Request> waitingRetry = new ConcurrentHashMap<>();

    @Override
    protected void setup() {
        AppState.log("Coordinator", "CoordinatorAgent prêt");

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) {
                    block();
                    return;
                }

                try {
                    if (msg.getPerformative() == ACLMessage.REQUEST) {
                        Request request = (Request) msg.getContentObject();
                        incomingQueue.put(request.getId(), request);
                        AppState.log("Coordinator", request.getId() + " placé dans la file de décision (priorité=" + request.getPriority() + ")");
                    } else if (msg.getPerformative() == ACLMessage.PROPOSE) {
                        Proposal proposal = (Proposal) msg.getContentObject();
                        PendingDecision pd = pending.get(msg.getConversationId());
                        if (pd != null) {
                            pd.proposals.put(proposal.getElevatorName(), proposal);
                            pd.responses++;
                            AppState.log("Coordinator", proposal.getRequestId() + " ← E" + proposal.getElevatorName()
                                    + " PROPOSE score=" + String.format(Locale.US, "%.2f", proposal.getScore()));
                        }
                    } else if (msg.getPerformative() == ACLMessage.REFUSE) {
                        PendingDecision pd = pending.get(msg.getConversationId());
                        if (pd != null) pd.responses++;
                    } else if (msg.getPerformative() == ACLMessage.INFORM) {
                        AppState.log("Coordinator", msg.getSender().getLocalName() + " : " + msg.getContent());
                    }
                } catch (Exception e) {
                    AppState.log("Coordinator", "Erreur message : " + e.getMessage());
                }
            }
        });

        // Une seule négociation à la fois : le coordinateur choisit d'abord la demande
        // ayant la priorité effective la plus élevée. L'attente augmente progressivement
        // cette priorité afin d'éviter la famine des utilisateurs normaux.
        addBehaviour(new TickerBehaviour(this, 300) {
            @Override
            protected void onTick() {
                if (!pending.isEmpty() || incomingQueue.isEmpty()) return;
                Request next = incomingQueue.values().stream()
                        .max(Comparator.comparingDouble(CoordinatorAgent::effectivePriority))
                        .orElse(null);
                if (next != null && incomingQueue.remove(next.getId()) != null) {
                    AppState.log("Coordinator", next.getId() + " choisie pour négociation (priorité effective="
                            + String.format(Locale.US, "%.2f", effectivePriority(next)) + ")");
                    launchCfp(next);
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, 200) {
            @Override
            protected void onTick() {
                long now = System.currentTimeMillis();
                for (PendingDecision pd : new ArrayList<>(pending.values())) {
                    if (pd.responses >= ELEVATORS.size() || now >= pd.deadline) {
                        decide(pd);
                    }
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, 2200) {
            @Override
            protected void onTick() {
                for (Request request : new ArrayList<>(waitingRetry.values())) {
                    if (!pending.containsKey(request.getId()) && waitingRetry.remove(request.getId()) != null) {
                        AppState.log("Coordinator", request.getId() + " remise dans la file après indisponibilité");
                        incomingQueue.put(request.getId(), request);
                    }
                }
            }
        });
    }

    private void launchCfp(Request request) {
        if (pending.containsKey(request.getId())) return;

        PendingDecision pd = new PendingDecision(request, System.currentTimeMillis() + 850);
        pending.put(request.getId(), pd);
        waitingRetry.remove(request.getId());

        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        cfp.setConversationId(request.getId());
        for (String elevator : ELEVATORS) {
            cfp.addReceiver(new AID("elevator-" + elevator, AID.ISLOCALNAME));
        }
        try {
            cfp.setContentObject(request);
            send(cfp);
            AppState.log("Coordinator", request.getId() + " → [E1,E2,E3] CFP");
        } catch (IOException e) {
            AppState.log("Coordinator", "Erreur CFP " + request.getId());
            pending.remove(request.getId());
        }
    }

    private void decide(PendingDecision pd) {
        if (pending.remove(pd.request.getId()) == null) return;

        Proposal best = pd.proposals.values().stream()
                .min(Comparator.comparingDouble(Proposal::getScore))
                .orElse(null);

        if (best == null) {
            waitingRetry.put(pd.request.getId(), pd.request);
            AppState.log("Coordinator", pd.request.getId() + " aucun ascenseur disponible → mise en attente");
            return;
        }

        for (Proposal proposal : pd.proposals.values()) {
            ACLMessage reply = new ACLMessage(
                    proposal.getElevatorName().equals(best.getElevatorName())
                            ? ACLMessage.ACCEPT_PROPOSAL : ACLMessage.REJECT_PROPOSAL);
            reply.addReceiver(new AID("elevator-" + proposal.getElevatorName(), AID.ISLOCALNAME));
            reply.setConversationId(pd.request.getId());
            if (proposal.getElevatorName().equals(best.getElevatorName())) {
                try { reply.setContentObject(pd.request); } catch (IOException ignored) {}
            }
            send(reply);
        }

        AppState.markAssigned(pd.request.getId(), best.getElevatorName());
        AppState.log("Coordinator", pd.request.getId() + " ✅ E" + best.getElevatorName()
                + " sélectionné (score=" + String.format(Locale.US, "%.2f", best.getScore()) + ")");
    }

    private static double effectivePriority(Request request) {
        // Fairness: +0.15 par seconde d'attente, plafonné à +5.
        return request.getPriority() + Math.min(5.0, request.waitingSeconds() * 0.15);
    }

    private static class PendingDecision {
        private final Request request;
        private final long deadline;
        private final Map<String, Proposal> proposals = new ConcurrentHashMap<>();
        private volatile int responses = 0;

        private PendingDecision(Request request, long deadline) {
            this.request = request;
            this.deadline = deadline;
        }
    }
}
