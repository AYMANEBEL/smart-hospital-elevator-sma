package com.smartelevator.agents;

import com.smartelevator.core.AppState;
import com.smartelevator.model.Request;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.io.IOException;

public class UserAgent extends Agent {
    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args == null || args.length == 0 || !(args[0] instanceof Request request)) {
            doDelete();
            return;
        }

        AppState.addRequest(request);

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("priority", AID.ISLOCALNAME));
        msg.setConversationId(request.getId());
        try {
            msg.setContentObject(request);
            send(msg);
            AppState.log("UserAgent", request.getId() + " → PriorityAgent : REQUEST");
        } catch (IOException e) {
            AppState.log("UserAgent", "Erreur sérialisation " + request.getId());
        }

        doDelete();
    }
}
