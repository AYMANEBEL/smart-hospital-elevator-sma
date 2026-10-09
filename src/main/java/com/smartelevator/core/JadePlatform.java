package com.smartelevator.core;

import com.smartelevator.agents.*;
import com.smartelevator.model.Request;
import com.smartelevator.model.UserType;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.concurrent.atomic.AtomicInteger;

public final class JadePlatform {
    private static AgentContainer container;
    private static final AtomicInteger REQUEST_SEQ = new AtomicInteger(1);

    private JadePlatform() {}

    public static synchronized void start() throws StaleProxyException {
        if (container != null) return;

        Runtime runtime = Runtime.instance();
        runtime.setCloseVM(true);
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "127.0.0.1");
        profile.setParameter(Profile.GUI, "false");
        container = runtime.createMainContainer(profile);

        startAgent("priority", PriorityAgent.class.getName(), null);
        startAgent("coordinator", CoordinatorAgent.class.getName(), null);
        startAgent("maintenance", MaintenanceAgent.class.getName(), null);
        startAgent("elevator-A", ElevatorAgent.class.getName(), new Object[]{"A", 0});
        startAgent("elevator-B", ElevatorAgent.class.getName(), new Object[]{"B", 5});
        startAgent("elevator-C", ElevatorAgent.class.getName(), new Object[]{"C", 10});

        AppState.log("SYSTEM", "Plateforme JADE démarrée : 3 ascenseurs + coordinateur + priorité + maintenance");
    }

    private static void startAgent(String name, String className, Object[] args) throws StaleProxyException {
        AgentController controller = container.createNewAgent(name, className, args);
        controller.start();
    }

    public static void submitRequest(int source, int destination, UserType type) {
        if (source == destination) {
            AppState.log("Interface", "Demande ignorée : étage de départ = destination");
            return;
        }
        String id = String.format("R%03d", REQUEST_SEQ.getAndIncrement());
        Request request = new Request(id, source, destination, type);
        try {
            startAgent("user-" + id, UserAgent.class.getName(), new Object[]{request});
        } catch (StaleProxyException e) {
            AppState.log("ERROR", "Impossible de créer UserAgent : " + e.getMessage());
        }
    }
}
