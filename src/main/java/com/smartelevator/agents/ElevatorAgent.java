package com.smartelevator.agents;

import com.smartelevator.core.AppState;
import com.smartelevator.model.*;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public class ElevatorAgent extends Agent {
    private String elevatorName;
    private int currentFloor;
    private boolean active = true;
    private final Deque<Request> queue = new ArrayDeque<>();
    private Request currentRequest;
    private Phase phase = Phase.IDLE;
    private int targetFloor;
    private int occupancy = 0;

    private enum Phase { IDLE, TO_PICKUP, TO_DESTINATION }

    @Override
    protected void setup() {
        Object[] args = getArguments();
        elevatorName = String.valueOf(args[0]);
        currentFloor = (Integer) args[1];
        publish();
        AppState.log("Elevator-" + elevatorName, "Agent prêt à l'étage " + currentFloor);

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) {
                    block();
                    return;
                }

                try {
                    switch (msg.getPerformative()) {
                        case ACLMessage.CFP -> handleCfp(msg);
                        case ACLMessage.ACCEPT_PROPOSAL -> handleAccept(msg);
                        case ACLMessage.REJECT_PROPOSAL -> { /* rien */ }
                        case ACLMessage.REQUEST -> handleMaintenance(msg);
                        default -> { }
                    }
                } catch (Exception e) {
                    AppState.log("Elevator-" + elevatorName, "Erreur : " + e.getMessage());
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, 650) {
            @Override
            protected void onTick() {
                moveOneStep();
            }
        });
    }

    private void handleCfp(ACLMessage msg) throws Exception {
        Request request = (Request) msg.getContentObject();
        ACLMessage response = msg.createReply();

        if (!active) {
            response.setPerformative(ACLMessage.REFUSE);
            response.setContent("OUT_OF_SERVICE");
            send(response);
            return;
        }

        Proposal proposal = calculateProposal(request);
        response.setPerformative(ACLMessage.PROPOSE);
        response.setContentObject(proposal);
        send(response);
    }

    private Proposal calculateProposal(Request request) {
        int projectedFloor = currentFloor;
        int workload = queue.size() + (currentRequest != null ? 1 : 0);

        if (!queue.isEmpty()) {
            Request last = queue.peekLast();
            projectedFloor = last.getDestinationFloor();
        } else if (currentRequest != null) {
            projectedFloor = currentRequest.getDestinationFloor();
        }

        int distance = Math.abs(projectedFloor - request.getSourceFloor());
        int requestedDirection = Integer.compare(request.getDestinationFloor(), request.getSourceFloor());
        int currentDirection = Integer.compare(targetFloor, currentFloor);
        int directionPenalty = (phase != Phase.IDLE && currentDirection != 0 && requestedDirection != currentDirection) ? 4 : 0;

        // Le score compare les ascenseurs pour UNE même demande.
        // La priorité de l'utilisateur est gérée en amont par CoordinatorAgent.
        double score = distance * 1.6 + workload * 6.0 + directionPenalty;

        return new Proposal(elevatorName, request.getId(), score, distance, workload, directionPenalty);
    }

    private void handleAccept(ACLMessage msg) throws Exception {
        Request request = (Request) msg.getContentObject();
        queue.addLast(request);
        AppState.log("Elevator-" + elevatorName, request.getId() + " ACCEPT_PROPOSAL (file=" + queue.size() + ")");
        if (currentRequest == null) startNextRequest();
        publish();

        ACLMessage inform = msg.createReply();
        inform.setPerformative(ACLMessage.INFORM);
        inform.setContent(request.getId() + " ajouté à la file de E" + elevatorName);
        send(inform);
    }

    private void handleMaintenance(ACLMessage msg) {
        String content = msg.getContent();
        if ("DISABLE".equals(content)) {
            active = false;
            AppState.log("Elevator-" + elevatorName, "🔴 En panne - mouvement suspendu");
        } else if ("ENABLE".equals(content)) {
            active = true;
            AppState.log("Elevator-" + elevatorName, "🟢 Réactivé");
            if (currentRequest == null) startNextRequest();
        }
        publish();
    }

    private void startNextRequest() {
        if (!active || currentRequest != null) return;
        currentRequest = queue.pollFirst();
        if (currentRequest == null) {
            phase = Phase.IDLE;
            targetFloor = currentFloor;
            publish();
            return;
        }
        phase = Phase.TO_PICKUP;
        targetFloor = currentRequest.getSourceFloor();
        AppState.log("Elevator-" + elevatorName, "Départ vers étage " + targetFloor + " pour " + currentRequest.getId());
        publish();
    }

    private void moveOneStep() {
        if (!active) {
            publish();
            return;
        }
        if (currentRequest == null) {
            startNextRequest();
            return;
        }

        if (currentFloor < targetFloor) currentFloor++;
        else if (currentFloor > targetFloor) currentFloor--;

        publish();

        if (currentFloor == targetFloor) {
            if (phase == Phase.TO_PICKUP) {
                occupancy = 1;
                AppState.markRiding(currentRequest.getId(), elevatorName);
                phase = Phase.TO_DESTINATION;
                targetFloor = currentRequest.getDestinationFloor();
                publish();
            } else if (phase == Phase.TO_DESTINATION) {
                occupancy = 0;
                AppState.markServed(currentRequest.getId(), elevatorName);
                currentRequest = null;
                phase = Phase.IDLE;
                targetFloor = currentFloor;
                publish();
                startNextRequest();
            }
        }
    }

    private void publish() {
        Direction direction = Direction.IDLE;
        if (active && currentRequest != null) {
            if (targetFloor > currentFloor) direction = Direction.UP;
            else if (targetFloor < currentFloor) direction = Direction.DOWN;
        }

        ElevatorStatus status = !active ? ElevatorStatus.OUT_OF_SERVICE
                : (currentRequest == null && queue.isEmpty() ? ElevatorStatus.AVAILABLE : ElevatorStatus.BUSY);

        AppState.updateElevator(new ElevatorSnapshot(
                elevatorName, currentFloor, direction, status,
                queue.size(), currentRequest == null ? null : currentRequest.getId(), occupancy));
    }
}
