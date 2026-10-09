package com.smartelevator.agents;

import com.smartelevator.core.AppState;
import com.smartelevator.core.MaintenanceCommand;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class MaintenanceAgent extends Agent {
    @Override
    protected void setup() {
        AppState.log("Maintenance", "MaintenanceAgent prêt");
        addBehaviour(new TickerBehaviour(this, 250) {
            @Override
            protected void onTick() {
                MaintenanceCommand command;
                while ((command = AppState.pollMaintenance()) != null) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(new AID("elevator-" + command.getElevatorName(), AID.ISLOCALNAME));
                    msg.setConversationId("maintenance-" + command.getElevatorName());
                    msg.setContent(command.isActive() ? "ENABLE" : "DISABLE");
                    send(msg);
                    AppState.log("Maintenance", "Commande " + msg.getContent() + " → E" + command.getElevatorName());
                }
            }
        });
    }
}
