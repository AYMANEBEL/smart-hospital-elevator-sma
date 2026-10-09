package com.smartelevator.agents;

import com.smartelevator.core.AppState;
import com.smartelevator.model.Request;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.IOException;

public class PriorityAgent extends Agent {
    @Override
    protected void setup() {
        AppState.log("Priority", "PriorityAgent prêt");
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) {
                    block();
                    return;
                }
                if (msg.getPerformative() != ACLMessage.REQUEST) return;

                try {
                    Request request = (Request) msg.getContentObject();
                    int priority = request.getUserType().getBasePriority();
                    Request enriched = request.withPriority(priority);
                    AppState.updatePriority(enriched);
                    AppState.log("Priority", request.getId() + " priorité=" + priority + " (" + request.getUserType().getLabel() + ")");

                    ACLMessage forward = new ACLMessage(ACLMessage.REQUEST);
                    forward.addReceiver(new AID("coordinator", AID.ISLOCALNAME));
                    forward.setConversationId(request.getId());
                    forward.setContentObject(enriched);
                    send(forward);
                } catch (Exception e) {
                    AppState.log("Priority", "Message invalide : " + e.getMessage());
                }
            }
        });
    }
}
