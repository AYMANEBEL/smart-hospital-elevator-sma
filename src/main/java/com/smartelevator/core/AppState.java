package com.smartelevator.core;

import com.smartelevator.model.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AppState {
    private static final Map<String, ElevatorSnapshot> ELEVATORS = new ConcurrentHashMap<>();
    private static final Map<String, RequestSnapshot> REQUESTS = new ConcurrentHashMap<>();
    private static final Deque<String> LOGS = new ArrayDeque<>();
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static final ConcurrentLinkedQueue<MaintenanceCommand> MAINTENANCE = new ConcurrentLinkedQueue<>();
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss");

    private AppState() {}

    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    private static void fireChanged() {
        for (Runnable listener : LISTENERS) {
            try { listener.run(); } catch (Exception ignored) {}
        }
    }

    public static void log(String source, String message) {
        synchronized (LOGS) {
            LOGS.addLast(String.format("[%s] %-12s %s", TIME.format(new Date()), source, message));
            while (LOGS.size() > 350) LOGS.removeFirst();
        }
        fireChanged();
    }

    public static List<String> getLogs() {
        synchronized (LOGS) { return new ArrayList<>(LOGS); }
    }

    public static void updateElevator(ElevatorSnapshot snapshot) {
        ELEVATORS.put(snapshot.getName(), snapshot);
        fireChanged();
    }

    public static List<ElevatorSnapshot> getElevators() {
        List<ElevatorSnapshot> list = new ArrayList<>(ELEVATORS.values());
        list.sort(Comparator.comparing(ElevatorSnapshot::getName));
        return list;
    }

    public static void addRequest(Request request) {
        REQUESTS.put(request.getId(), new RequestSnapshot(request, RequestStatus.WAITING, null, 0, 0));
        log("UserAgent", request.getId() + " demande étage " + request.getSourceFloor() + " → " + request.getDestinationFloor()
                + " (" + request.getUserType().getLabel() + ")");
    }

    public static void updatePriority(Request request) {
        RequestSnapshot old = REQUESTS.get(request.getId());
        if (old != null) {
            REQUESTS.put(request.getId(), new RequestSnapshot(request, old.getStatus(), old.getElevatorName(), old.getPickupAt(), old.getCompletedAt()));
        }
        fireChanged();
    }

    public static void markAssigned(String requestId, String elevator) {
        RequestSnapshot old = REQUESTS.get(requestId);
        if (old != null) {
            REQUESTS.put(requestId, new RequestSnapshot(old.getRequest(), RequestStatus.ASSIGNED, elevator, 0, 0));
            fireChanged();
        }
    }

    public static void markRiding(String requestId, String elevator) {
        RequestSnapshot old = REQUESTS.get(requestId);
        if (old != null) {
            REQUESTS.put(requestId, new RequestSnapshot(old.getRequest(), RequestStatus.RIDING, elevator,
                    System.currentTimeMillis(), 0));
            log("Elevator-" + elevator, requestId + " embarqué, destination " + old.getRequest().getDestinationFloor());
        }
    }

    public static void markServed(String requestId, String elevator) {
        RequestSnapshot old = REQUESTS.get(requestId);
        if (old != null) {
            long pickup = old.getPickupAt() > 0 ? old.getPickupAt() : System.currentTimeMillis();
            REQUESTS.put(requestId, new RequestSnapshot(old.getRequest(), RequestStatus.SERVED, elevator,
                    pickup, System.currentTimeMillis()));
            log("Elevator-" + elevator, requestId + " servi ✅");
        }
    }

    public static List<RequestSnapshot> getRequests() {
        List<RequestSnapshot> list = new ArrayList<>(REQUESTS.values());
        list.sort(Comparator.comparingLong(a -> a.getRequest().getCreatedAt()));
        return list;
    }

    public static int getServedCount() {
        return (int) REQUESTS.values().stream().filter(r -> r.getStatus() == RequestStatus.SERVED).count();
    }

    public static double getAverageWait() {
        return REQUESTS.values().stream()
                .filter(r -> r.getPickupAt() > 0)
                .mapToDouble(RequestSnapshot::getWaitingSeconds)
                .average().orElse(0.0);
    }

    public static double getPmrAverageWait() {
        return REQUESTS.values().stream()
                .filter(r -> r.getPickupAt() > 0)
                .filter(r -> r.getRequest().getUserType() == UserType.PMR || r.getRequest().getUserType() == UserType.EMERGENCY)
                .mapToDouble(RequestSnapshot::getWaitingSeconds)
                .average().orElse(0.0);
    }

    public static int getActiveElevators() {
        return (int) ELEVATORS.values().stream().filter(e -> e.getStatus() != ElevatorStatus.OUT_OF_SERVICE).count();
    }

    public static int getPendingCount() {
        return (int) REQUESTS.values().stream().filter(r -> r.getStatus() != RequestStatus.SERVED).count();
    }

    public static void clearLogs() {
        synchronized (LOGS) { LOGS.clear(); }
        fireChanged();
    }

    public static void enqueueMaintenance(MaintenanceCommand command) {
        MAINTENANCE.add(command);
        log("Interface", (command.isActive() ? "Réactivation " : "Panne simulée ") + "ascenseur " + command.getElevatorName());
    }

    public static MaintenanceCommand pollMaintenance() {
        return MAINTENANCE.poll();
    }
}
