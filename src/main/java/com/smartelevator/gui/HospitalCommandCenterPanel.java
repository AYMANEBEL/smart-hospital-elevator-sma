package com.smartelevator.gui;

import com.smartelevator.core.AppState;
import com.smartelevator.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * V6 professional live operations surface. It is intentionally custom painted so the
 * demo looks like a digital-twin product rather than a collection of Swing widgets.
 * All operational data comes from AppState / JADE agents.
 */
public class HospitalCommandCenterPanel extends JPanel {
    private static final Color BG0 = new Color(4, 10, 20);
    private static final Color BG1 = new Color(7, 18, 32);
    private static final Color CARD = new Color(10, 24, 40);
    private static final Color CARD2 = new Color(14, 31, 49);
    private static final Color LINE = new Color(42, 66, 84);
    private static final Color TEXT = new Color(239, 247, 250);
    private static final Color MUTED = new Color(137, 160, 178);
    private static final Color CYAN = new Color(34, 211, 238);
    private static final Color TEAL = new Color(20, 184, 166);
    private static final Color GREEN = new Color(34, 197, 94);
    private static final Color BLUE = new Color(59, 130, 246);
    private static final Color RED = new Color(244, 63, 94);
    private static final Color AMBER = new Color(245, 158, 11);
    private static final Color PURPLE = new Color(168, 85, 247);

    private static final String[] SERVICES = {
            "Urgences & accueil", "Admissions", "Consultations", "Pédiatrie", "Radiologie",
            "Médecine interne", "Orthopédie", "Neurologie", "Cardiologie", "Réanimation", "Chirurgie"
    };

    private final Map<String, Double> visualFloors = new HashMap<>();
    private long frame = 0;

    public HospitalCommandCenterPanel() {
        setOpaque(true);
        setBackground(BG0);
        setMinimumSize(new Dimension(700, 580));
        javax.swing.Timer animation = new javax.swing.Timer(33, e -> {
            frame++;
            updateSmoothElevators();
            repaint();
        });
        animation.start();
    }

    private void updateSmoothElevators() {
        for (ElevatorSnapshot e : AppState.getElevators()) {
            double current = visualFloors.getOrDefault(e.getName(), (double) e.getFloor());
            double target = e.getFloor();
            double next = current + (target - current) * 0.14;
            if (Math.abs(target - next) < 0.01) next = target;
            visualFloors.put(e.getName(), next);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();
        paintBackground(g2, w, h);

        int pad = 20;
        int topH = 72;
        int bottomH = 110;
        drawSceneHeader(g2, pad, pad, w - pad * 2, topH);
        drawDigitalTwin(g2, pad, pad + topH + 12, w - pad * 2, h - topH - bottomH - pad * 2 - 24);
        drawAgentMesh(g2, pad, h - bottomH - pad, w - pad * 2, bottomH);

        g2.dispose();
    }

    private void paintBackground(Graphics2D g2, int w, int h) {
        g2.setPaint(new GradientPaint(0, 0, BG1, w, h, BG0));
        g2.fillRect(0, 0, w, h);

        // soft technical grid
        g2.setColor(new Color(255,255,255,7));
        for (int x = 0; x < w; x += 48) g2.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 48) g2.drawLine(0, y, w, y);

        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(w * .55f, h * .40f), Math.max(w, h) * .52f,
                new float[]{0f, 1f}, new Color[]{new Color(20,184,166,36), new Color(4,10,20,0)});
        g2.setPaint(glow);
        g2.fillRect(0, 0, w, h);
    }

    private void drawSceneHeader(Graphics2D g2, int x, int y, int w, int h) {
        roundCard(g2, x, y, w, h, 20, new Color(8, 22, 37, 245), new Color(31, 55, 74));
        RequestSnapshot focus = chooseFocusRequest();
        int focusFloor = chooseFocusFloor(focus);

        g2.setColor(new Color(13, 148, 136));
        g2.fillRoundRect(x + 14, y + 14, 42, 42, 14, 14);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 35, y + 23, x + 35, y + 47);
        g2.drawLine(x + 23, y + 35, x + 47, y + 35);

        g2.setColor(TEXT);
        g2.setFont(uiFont(Font.BOLD, 16f));
        g2.drawString("LIVE OPERATIONS • DIGITAL TWIN", x + 70, y + 29);
        g2.setColor(MUTED);
        g2.setFont(uiFont(Font.PLAIN, 10.5f));
        g2.drawString("Hôpital universitaire • transport patient prioritaire piloté par agents JADE", x + 70, y + 48);

        String service = SERVICES[Math.max(0, Math.min(10, focusFloor))];
        int chipW = 185;
        int cx = x + w - chipW - 16;
        g2.setColor(new Color(13, 78, 72));
        g2.fillRoundRect(cx, y + 14, chipW, 42, 14, 14);
        g2.setColor(new Color(153, 246, 228));
        g2.setFont(uiFont(Font.BOLD, 9f));
        g2.drawString("CAMÉRA ACTIVE", cx + 13, y + 30);
        g2.setFont(uiFont(Font.BOLD, 11f));
        g2.drawString(floorLabel(focusFloor) + "  •  " + service, cx + 13, y + 47);
    }

    private void drawDigitalTwin(Graphics2D g2, int x, int y, int w, int h) {
        roundCard(g2, x, y, w, h, 24, new Color(7, 18, 31, 250), new Color(35, 58, 75));

        RequestSnapshot focus = chooseFocusRequest();
        int focusFloor = chooseFocusFloor(focus);

        // split scene: building 72%, mission console 28%
        int missionW = Math.max(220, Math.min(300, (int)(w * .28)));
        int buildingW = w - missionW - 18;
        int bx = x + 16;
        int by = y + 16;
        int bw = buildingW - 18;
        int bh = h - 32;
        int mx = x + buildingW;
        int my = y + 16;
        int mh = h - 32;

        drawHospitalCutaway(g2, bx, by, bw, bh, focus, focusFloor);
        drawMissionConsole(g2, mx, my, missionW - 16, mh, focus, focusFloor);
    }

    private void drawHospitalCutaway(Graphics2D g2, int x, int y, int w, int h,
                                     RequestSnapshot focus, int focusFloor) {
        // glass frame
        roundCard(g2, x, y, w, h, 22, new Color(222, 237, 241), new Color(89, 120, 132));
        Shape oldClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Double(x, y, w, h, 22, 22));

        // clinical background with subtle vertical gradient
        g2.setPaint(new GradientPaint(x, y, new Color(239, 248, 250), x, y+h, new Color(189, 214, 221)));
        g2.fillRect(x, y, w, h);

        int top = y + 42;
        int bottom = y + h - 30;
        int floorAreaH = bottom - top;
        int floorH = Math.max(24, floorAreaH / 11);
        int labelW = Math.min(150, Math.max(105, w / 5));
        int shaftAreaW = Math.min(310, Math.max(210, w / 3));
        int shaftsX = x + w/2 - shaftAreaW/2;
        int shaftGap = 10;
        int shaftW = (shaftAreaW - shaftGap*2)/3;

        // roof brand and building perspective side wall
        g2.setColor(new Color(15, 68, 81));
        g2.fillRect(x, y, w, 34);
        g2.setColor(Color.WHITE);
        g2.setFont(uiFont(Font.BOLD, 11f));
        g2.drawString("HÔPITAL UNIVERSITAIRE • BLOC CENTRAL", x + 18, y + 22);

        Polygon rightSide = new Polygon();
        rightSide.addPoint(x+w-18, y+34);
        rightSide.addPoint(x+w, y+48);
        rightSide.addPoint(x+w, y+h);
        rightSide.addPoint(x+w-18, y+h-8);
        g2.setColor(new Color(128, 161, 170, 90));
        g2.fillPolygon(rightSide);

        // floors
        for (int floor=10; floor>=0; floor--) {
            int row = 10-floor;
            int fy = top + row*floorH;
            boolean selected = floor == focusFloor;
            Color base = row%2==0 ? new Color(242, 248, 249) : new Color(232, 242, 244);
            if (selected) base = new Color(211, 242, 240);
            g2.setColor(base);
            g2.fillRect(x+1, fy, w-20, floorH);
            g2.setColor(new Color(189, 207, 213));
            g2.drawLine(x+1, fy+floorH, x+w-20, fy+floorH);

            // floor number badge
            Color badge = floor==0 ? new Color(190, 24, 93) : selected ? new Color(13, 148, 136) : new Color(31, 124, 138);
            g2.setColor(badge);
            g2.fillRoundRect(x+12, fy+5, 42, Math.max(20, floorH-10), 9, 9);
            g2.setColor(Color.WHITE);
            g2.setFont(uiFont(Font.BOLD, 9.5f));
            String fl = floor==0 ? "RDC" : String.valueOf(floor);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(fl, x+33-fm.stringWidth(fl)/2, fy+floorH/2+4);

            g2.setColor(new Color(29, 55, 66));
            g2.setFont(uiFont(selected?Font.BOLD:Font.PLAIN, selected?9.5f:9f));
            String svc = SERVICES[floor];
            if (g2.getFontMetrics().stringWidth(svc) > labelW-68) svc = abbreviate(svc, 16);
            g2.drawString(svc, x+62, fy+floorH/2+4);

            // room windows on left/right to create a real building feel
            drawWardStrip(g2, x+labelW, fy+4, Math.max(45, shaftsX-(x+labelW)-12), floorH-8, floor, false, selected);
            int rx = shaftsX + shaftAreaW + 10;
            drawWardStrip(g2, rx, fy+4, Math.max(45, x+w-24-rx), floorH-8, floor, true, selected);
        }

        // shafts and smooth cabins
        for (int i=0; i<3; i++) {
            String name = String.valueOf((char)('A'+i));
            int sx = shaftsX + i*(shaftW+shaftGap);
            g2.setColor(new Color(24, 42, 54));
            g2.fillRoundRect(sx, top, shaftW, floorH*11, 9, 9);
            g2.setColor(new Color(70, 94, 107));
            g2.drawRoundRect(sx, top, shaftW, floorH*11, 9, 9);

            // rail lines
            g2.setColor(new Color(116, 139, 149, 90));
            g2.drawLine(sx+shaftW/3, top+6, sx+shaftW/3, top+floorH*11-6);
            g2.drawLine(sx+shaftW*2/3, top+6, sx+shaftW*2/3, top+floorH*11-6);

            ElevatorSnapshot e = elevatorByName(name);
            drawSmoothCabin(g2, sx+5, top, shaftW-10, floorH, e, name);
        }

        drawHospitalPeople(g2, x, top, w-20, floorH, shaftsX, shaftAreaW, focus);
        drawGroundDetails(g2, x, top + 10*floorH, w-20, floorH);

        g2.setClip(oldClip);
        g2.setColor(new Color(74, 104, 116));
        g2.drawRoundRect(x, y, w, h, 22, 22);
    }

    private void drawWardStrip(Graphics2D g2, int x, int y, int w, int h, int floor, boolean right, boolean selected) {
        if (w < 35 || h < 14) return;
        g2.setColor(selected ? new Color(191, 231, 227) : new Color(215, 230, 233));
        g2.fillRoundRect(x, y, w, h, 7, 7);
        g2.setColor(new Color(143, 194, 205));
        int winW = Math.min(34, Math.max(18, w/4));
        int wx = right ? x+7 : x+w-winW-7;
        g2.fillRoundRect(wx, y+3, winW, Math.max(8,h-6), 5,5);
        // bed / workstation
        g2.setColor(new Color(87, 115, 126, 140));
        int ux = right ? wx+winW+9 : x+8;
        int uw = Math.max(12, w-winW-24);
        if (uw > 10) g2.fillRoundRect(ux, y+h/2, uw, 4, 2,2);
        if (floor == 0) {
            g2.setColor(new Color(244,63,94));
            g2.fillOval(x+w/2-3, y+4, 7,7);
        }
    }

    private void drawSmoothCabin(Graphics2D g2, int x, int top, int w, int floorH, ElevatorSnapshot e, String name) {
        double vf = visualFloors.getOrDefault(name, e == null ? 0.0 : (double)e.getFloor());
        vf = Math.max(0, Math.min(10, vf));
        double row = 10.0 - vf;
        int y = top + (int)Math.round(row*floorH) + 3;
        int ch = Math.max(22, floorH-7);
        Color status = e == null ? new Color(100,116,139)
                : e.getStatus()==ElevatorStatus.OUT_OF_SERVICE ? RED
                : e.getStatus()==ElevatorStatus.BUSY ? BLUE : TEAL;

        g2.setColor(new Color(0,0,0,95));
        g2.fillRoundRect(x+3,y+4,w,ch,9,9);
        g2.setColor(status);
        g2.fillRoundRect(x,y,w,ch,9,9);
        g2.setPaint(new GradientPaint(x,y,new Color(239,250,250),x,y+ch,new Color(177,205,212)));
        g2.fillRoundRect(x+4,y+4,w-8,ch-8,6,6);
        int mid=x+w/2;
        g2.setColor(new Color(100,125,135));
        g2.drawLine(mid,y+5,mid,y+ch-5);
        g2.setColor(new Color(13,52,64));
        g2.setFont(uiFont(Font.BOLD,8.8f));
        g2.drawString("E"+name,x+7,y+14);
        if (e!=null) {
            g2.setFont(uiFont(Font.BOLD,8f));
            String dir=e.getDirection().getSymbol();
            g2.drawString(dir,x+w-15,y+14);
        }
        if (e!=null && e.getStatus()==ElevatorStatus.OUT_OF_SERVICE) {
            g2.setColor(new Color(127,29,29,215));
            g2.fillRoundRect(x+4,y+4,w-8,ch-8,6,6);
            g2.setColor(Color.WHITE);
            g2.setFont(uiFont(Font.BOLD,7.5f));
            g2.drawString("PANNE",x+7,y+14);
        }
    }

    private void drawHospitalPeople(Graphics2D g2, int x, int top, int w, int floorH,
                                    int shaftsX, int shaftsW, RequestSnapshot focus) {
        List<RequestSnapshot> recent = AppState.getRequests().stream()
                .filter(r -> r.getStatus()!=RequestStatus.SERVED || (r.getCompletedAt()>0 && System.currentTimeMillis()-r.getCompletedAt()<5000))
                .sorted(Comparator.comparingLong(r -> r.getRequest().getCreatedAt()))
                .toList();
        int[] count = new int[11];
        for (RequestSnapshot rs: recent) {
            Request r=rs.getRequest();
            int floor;
            if (rs.getStatus()==RequestStatus.RIDING) {
                ElevatorSnapshot e=elevatorByName(rs.getElevatorName());
                floor=e==null?r.getSourceFloor():e.getFloor();
            } else if (rs.getStatus()==RequestStatus.SERVED) floor=r.getDestinationFloor();
            else floor=r.getSourceFloor();
            floor=Math.max(0,Math.min(10,floor));
            int fy=top+(10-floor)*floorH;
            int idx=count[floor]++;
            int px;
            if (rs.getStatus()==RequestStatus.RIDING && rs.getElevatorName()!=null) {
                int ei=Math.max(0,Math.min(2,rs.getElevatorName().charAt(0)-'A'));
                int gap=10; int sw=(shaftsW-gap*2)/3;
                px=shaftsX+ei*(sw+gap)+sw/2;
            } else {
                px=shaftsX-28-(idx%3)*25;
                if (idx>=3) px=shaftsX+shaftsW+28+(idx%3)*25;
            }
            int py=fy+floorH-5;
            drawPatientAvatar(g2,px,py,r.getUserType(),r.getId(),rs.getStatus(),rs==focus);
        }
    }

    private void drawPatientAvatar(Graphics2D g2, int cx, int baseY, UserType type,
                                   String id, RequestStatus status, boolean focused) {
        Color c=typeColor(type);
        int bob=(int)(Math.sin((frame+cx)*.10)*1.5);
        baseY+=bob;
        if (focused) {
            int pulse=4+(int)((Math.sin(frame*.14)+1)*3);
            g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),45));
            g2.fillOval(cx-16-pulse,baseY-35-pulse,32+pulse*2,38+pulse*2);
        }
        switch(type) {
            case PMR -> drawWheelchair(g2,cx,baseY,c);
            case EMERGENCY -> drawStretcher(g2,cx,baseY,c);
            case ELDERLY -> drawPerson(g2,cx,baseY,c,true);
            default -> drawPerson(g2,cx,baseY,c,false);
        }
        g2.setColor(new Color(5,14,24,215));
        g2.fillRoundRect(cx-18,baseY-42,36,13,7,7);
        g2.setColor(Color.WHITE);
        g2.setFont(uiFont(Font.BOLD,6.8f));
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(id,cx-fm.stringWidth(id)/2,baseY-33);
        if(status==RequestStatus.SERVED){g2.setColor(GREEN);g2.fillOval(cx+10,baseY-38,7,7);}
    }

    private void drawPerson(Graphics2D g2,int cx,int baseY,Color c,boolean cane){
        g2.setColor(new Color(231,185,143)); g2.fillOval(cx-4,baseY-25,8,8);
        g2.setColor(c); g2.fillRoundRect(cx-5,baseY-17,10,12,5,5);
        g2.setStroke(new BasicStroke(1.7f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.drawLine(cx-2,baseY-5,cx-5,baseY); g2.drawLine(cx+2,baseY-5,cx+5,baseY);
        if(cane){g2.setColor(new Color(92,67,44));g2.drawLine(cx+7,baseY-12,cx+10,baseY);}
    }
    private void drawWheelchair(Graphics2D g2,int cx,int baseY,Color c){
        g2.setColor(c);g2.setStroke(new BasicStroke(2f));g2.drawOval(cx-9,baseY-15,17,17);
        g2.setColor(new Color(231,185,143));g2.fillOval(cx-1,baseY-27,7,7);
        g2.setColor(c);g2.drawLine(cx+2,baseY-19,cx+8,baseY-9);g2.drawLine(cx+8,baseY-9,cx+13,baseY-9);g2.fillOval(cx+10,baseY-4,5,5);
    }
    private void drawStretcher(Graphics2D g2,int cx,int baseY,Color c){
        g2.setColor(new Color(218,228,232));g2.fillRoundRect(cx-18,baseY-14,36,7,4,4);
        g2.setColor(c);g2.fillRoundRect(cx-14,baseY-18,25,5,3,3);
        g2.setColor(new Color(75,100,111));g2.drawLine(cx-13,baseY-7,cx-10,baseY);g2.drawLine(cx+13,baseY-7,cx+10,baseY);
        g2.fillOval(cx-12,baseY-2,4,4);g2.fillOval(cx+8,baseY-2,4,4);
    }

    private void drawGroundDetails(Graphics2D g2,int x,int y,int w,int floorH){
        int base=y+floorH-5;
        // ambulance
        g2.setColor(Color.WHITE);g2.fillRoundRect(x+70,base-20,48,17,5,5);
        g2.setColor(RED);g2.fillRect(x+89,base-18,5,12);g2.fillRect(x+85,base-14,13,4);
        g2.setColor(new Color(37,55,65));g2.fillOval(x+78,base-6,7,7);g2.fillOval(x+106,base-6,7,7);
        // reception desk
        g2.setColor(new Color(14,116,144));g2.fillRoundRect(x+w-152,base-23,125,19,8,8);
        g2.setColor(Color.WHITE);g2.setFont(uiFont(Font.BOLD,7.8f));g2.drawString("ACCUEIL • TRIAGE",x+w-137,base-10);
    }

    private void drawMissionConsole(Graphics2D g2,int x,int y,int w,int h,RequestSnapshot focus,int focusFloor){
        // title
        g2.setColor(TEXT);g2.setFont(uiFont(Font.BOLD,13.5f));g2.drawString("MISSION ACTIVE",x+4,y+16);
        g2.setColor(MUTED);g2.setFont(uiFont(Font.PLAIN,9.5f));g2.drawString("Décision et progression en temps réel",x+4,y+34);

        int cardY=y+50;
        roundCard(g2,x,cardY,w,h-50,18,new Color(9,23,38,240),new Color(36,61,80));
        if(focus==null){
            drawIdleConsole(g2,x,cardY,w,h-50);
            return;
        }
        Request r=focus.getRequest(); Color c=typeColor(r.getUserType());
        g2.setColor(c);g2.fillRoundRect(x+12,cardY+14,5,58,3,3);
        g2.setColor(TEXT);g2.setFont(uiFont(Font.BOLD,13f));g2.drawString(r.getId(),x+27,cardY+31);
        g2.setColor(c.brighter());g2.setFont(uiFont(Font.BOLD,9f));
        g2.drawString(r.getUserType().getLabel().toUpperCase(Locale.ROOT),x+27,cardY+48);
        g2.setColor(MUTED);g2.setFont(uiFont(Font.PLAIN,9f));g2.drawString("Priorité " + r.getPriority(),x+27,cardY+64);

        // route
        int routeY=cardY+94;
        g2.setColor(new Color(16,34,52));g2.fillRoundRect(x+12,routeY,w-24,66,14,14);
        drawRoute(g2,x+24,routeY+15,w-48,36,r.getSourceFloor(),r.getDestinationFloor(),c);

        // assigned elevator panel
        int infoY=routeY+78;
        String elev=focus.getElevatorName()==null?"—":"E"+focus.getElevatorName();
        drawMiniMetric(g2,x+12,infoY,(w-32)/2,62,"ASCENSEUR",elev,BLUE);
        drawMiniMetric(g2,x+20+(w-32)/2,infoY,(w-32)/2,62,"ATTENTE",String.format(Locale.US,"%.1f s",focus.getWaitingSeconds()),AMBER);

        // progress
        int progressY=infoY+78;
        g2.setColor(MUTED);g2.setFont(uiFont(Font.BOLD,8.5f));g2.drawString("PROGRESSION",x+12,progressY);
        String[] steps={"Demande","Priorité","ACL","Affectation","Transport","Arrivée"};
        int completed=progressIndex(focus.getStatus(),focus.getElevatorName()!=null);
        int lineX=x+18;int lineW=w-36;int py=progressY+30;
        g2.setStroke(new BasicStroke(3f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(49,68,85));g2.drawLine(lineX,py,lineX+lineW,py);
        for(int i=0;i<steps.length;i++){
            int sx=lineX+i*lineW/(steps.length-1);boolean done=i<=completed;
            g2.setColor(done?c:new Color(65,84,101));g2.fillOval(sx-6,py-6,12,12);
            if(done){g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),45));g2.fillOval(sx-11,py-11,22,22);}
            g2.setColor(done?TEXT:MUTED);g2.setFont(uiFont(Font.PLAIN,7.2f));
            FontMetrics fm=g2.getFontMetrics();g2.drawString(steps[i],sx-fm.stringWidth(steps[i])/2,py+23);
        }

        // decision text
        int decisionY=progressY+78;
        g2.setColor(new Color(16,34,52));g2.fillRoundRect(x+12,decisionY,w-24,82,14,14);
        g2.setColor(new Color(94,234,212));g2.setFont(uiFont(Font.BOLD,8.5f));g2.drawString("DÉCISION SMA",x+24,decisionY+20);
        g2.setColor(TEXT);g2.setFont(uiFont(Font.BOLD,10f));
        String d=focus.getElevatorName()==null?"Négociation Contract-Net en cours":"E"+focus.getElevatorName()+" sélectionné par le coordinateur";
        drawWrapped(g2,d,x+24,decisionY+40,w-48,14);
        g2.setColor(MUTED);g2.setFont(uiFont(Font.PLAIN,8.5f));
        String s="État : "+focus.getStatus().getLabel()+" • "+floorLabel(focusFloor)+" • "+SERVICES[focusFloor];
        drawWrapped(g2,s,x+24,decisionY+68,w-48,12);
    }

    private void drawIdleConsole(Graphics2D g2,int x,int y,int w,int h){
        int cx=x+w/2;int cy=y+Math.min(h/2,180);
        g2.setColor(new Color(13,78,72));g2.fillOval(cx-34,cy-34,68,68);
        g2.setColor(new Color(153,246,228));g2.setStroke(new BasicStroke(3f));g2.drawOval(cx-19,cy-19,38,38);
        g2.drawLine(cx-9,cy,cx-2,cy+8);g2.drawLine(cx-2,cy+8,cx+12,cy-10);
        g2.setColor(TEXT);g2.setFont(uiFont(Font.BOLD,12f));
        String t="SYSTÈME PRÊT";FontMetrics fm=g2.getFontMetrics();g2.drawString(t,cx-fm.stringWidth(t)/2,cy+58);
        g2.setColor(MUTED);g2.setFont(uiFont(Font.PLAIN,9f));
        String s="Lancez un scénario ou créez un appel patient";fm=g2.getFontMetrics();g2.drawString(s,cx-fm.stringWidth(s)/2,cy+78);
    }

    private void drawRoute(Graphics2D g2,int x,int y,int w,int h,int src,int dst,Color c){
        int cy=y+h/2;
        g2.setStroke(new BasicStroke(3f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(58,78,94));g2.drawLine(x+18,cy,x+w-18,cy);
        g2.setColor(c);g2.drawLine(x+18,cy,x+w-18,cy);
        g2.fillOval(x+9,cy-9,18,18);g2.fillOval(x+w-27,cy-9,18,18);
        g2.setColor(Color.WHITE);g2.setFont(uiFont(Font.BOLD,7.5f));
        String a=src==0?"R":String.valueOf(src);String b=dst==0?"R":String.valueOf(dst);
        FontMetrics fm=g2.getFontMetrics();g2.drawString(a,x+18-fm.stringWidth(a)/2,cy+3);g2.drawString(b,x+w-18-fm.stringWidth(b)/2,cy+3);
        g2.setColor(TEXT);g2.setFont(uiFont(Font.BOLD,9f));
        String label=floorLabel(src)+"  →  "+floorLabel(dst);fm=g2.getFontMetrics();g2.drawString(label,x+w/2-fm.stringWidth(label)/2,y+3);
    }

    private void drawMiniMetric(Graphics2D g2,int x,int y,int w,int h,String title,String value,Color accent){
        roundCard(g2,x,y,w,h,13,new Color(13,29,46),new Color(36,58,75));
        g2.setColor(accent);g2.fillRoundRect(x+10,y+12,4,h-24,2,2);
        g2.setColor(MUTED);g2.setFont(uiFont(Font.BOLD,7.5f));g2.drawString(title,x+22,y+23);
        g2.setColor(TEXT);g2.setFont(uiFont(Font.BOLD,14f));g2.drawString(value,x+22,y+45);
    }

    private void drawAgentMesh(Graphics2D g2,int x,int y,int w,int h){
        roundCard(g2,x,y,w,h,20,new Color(8,21,35,245),new Color(31,54,72));
        g2.setColor(TEXT);g2.setFont(uiFont(Font.BOLD,11.5f));g2.drawString("AGENT MESH",x+16,y+21);
        g2.setColor(MUTED);g2.setFont(uiFont(Font.PLAIN,8.5f));g2.drawString("Contract-Net ACL • autonomie • coordination distribuée",x+16,y+38);

        int cy=y+h/2+8;
        int centerX=x+w/2;
        int coordinatorR=24;
        // nodes positions
        int leftX=x+150;int rightX=x+w-150;
        int e1x=centerX-170,e2x=centerX,e3x=centerX+170;

        // lines and moving packets
        g2.setStroke(new BasicStroke(1.6f));
        drawLink(g2,centerX,cy,leftX,cy,TEAL,0);
        drawLink(g2,centerX,cy,rightX,cy,PURPLE,18);
        drawLink(g2,centerX,cy,e1x,cy-32,BLUE,35);
        drawLink(g2,centerX,cy,e2x,cy-42,BLUE,52);
        drawLink(g2,centerX,cy,e3x,cy-32,BLUE,69);

        drawAgentNode(g2,leftX,cy,42,"P","PRIORITÉ",TEAL);
        drawAgentNode(g2,rightX,cy,42,"M","MAINT.",AMBER);
        drawAgentNode(g2,centerX,cy,52,"C","COORDINATEUR",new Color(14,165,233));
        drawAgentNode(g2,e1x,cy-32,36,"A","ELEVATOR A",BLUE);
        drawAgentNode(g2,e2x,cy-42,36,"B","ELEVATOR B",BLUE);
        drawAgentNode(g2,e3x,cy-32,36,"C","ELEVATOR C",BLUE);

        // recent ACL message badge
        String msg=latestAclMessage();
        if(msg!=null){
            int mw=Math.min(310,g2.getFontMetrics(uiFont(Font.BOLD,8.5f)).stringWidth(msg)+26);
            g2.setColor(new Color(15,35,52));g2.fillRoundRect(x+w-mw-16,y+13,mw,24,12,12);
            g2.setColor(new Color(165,243,252));g2.setFont(uiFont(Font.BOLD,8.5f));
            g2.drawString(msg,x+w-mw-4,y+29);
        }
    }

    private void drawLink(Graphics2D g2,int x1,int y1,int x2,int y2,Color c,int phase){
        g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),70));g2.drawLine(x1,y1,x2,y2);
        double t=((frame+phase)%80)/80.0;
        int px=(int)Math.round(x1+(x2-x1)*t);int py=(int)Math.round(y1+(y2-y1)*t);
        g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),45));g2.fillOval(px-7,py-7,14,14);
        g2.setColor(c);g2.fillOval(px-3,py-3,6,6);
    }

    private void drawAgentNode(Graphics2D g2,int cx,int cy,int size,String initial,String label,Color c){
        g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),35));g2.fillOval(cx-size/2-6,cy-size/2-6,size+12,size+12);
        g2.setColor(new Color(9,25,41));g2.fillOval(cx-size/2,cy-size/2,size,size);
        g2.setColor(c);g2.setStroke(new BasicStroke(2f));g2.drawOval(cx-size/2,cy-size/2,size,size);
        g2.setColor(Color.WHITE);g2.setFont(uiFont(Font.BOLD,size>45?13f:10f));FontMetrics fm=g2.getFontMetrics();g2.drawString(initial,cx-fm.stringWidth(initial)/2,cy+4);
        g2.setColor(MUTED);g2.setFont(uiFont(Font.BOLD,6.8f));fm=g2.getFontMetrics();g2.drawString(label,cx-fm.stringWidth(label)/2,cy+size/2+14);
    }

    private String latestAclMessage(){
        List<String> logs=AppState.getLogs();
        for(int i=logs.size()-1;i>=0;i--){
            String l=logs.get(i);
            String u=l.toUpperCase(Locale.ROOT);
            if(u.contains("CFP")) return "CFP • appel à propositions";
            if(u.contains("PROPOSE")) return "PROPOSE • score reçu";
            if(u.contains("ACCEPT_PROPOSAL")) return "ACCEPT • mission attribuée";
            if(u.contains("REQUEST")) return "REQUEST • nouvelle demande";
        }
        return "ACL • réseau opérationnel";
    }

    private RequestSnapshot chooseFocusRequest(){
        return AppState.getRequests().stream()
                .filter(r->r.getStatus()!=RequestStatus.SERVED)
                .min(Comparator.comparingInt((RequestSnapshot r)->-r.getRequest().getPriority())
                        .thenComparingLong(r->r.getRequest().getCreatedAt()))
                .orElse(null);
    }
    private int chooseFocusFloor(RequestSnapshot focus){
        if(focus==null) return 0;
        Request r=focus.getRequest();
        if(focus.getStatus()==RequestStatus.RIDING){ElevatorSnapshot e=elevatorByName(focus.getElevatorName());return e==null?r.getSourceFloor():e.getFloor();}
        if(focus.getStatus()==RequestStatus.SERVED) return r.getDestinationFloor();
        return r.getSourceFloor();
    }
    private ElevatorSnapshot elevatorByName(String name){
        if(name==null)return null;
        return AppState.getElevators().stream().filter(e->name.equals(e.getName())).findFirst().orElse(null);
    }

    private static int progressIndex(RequestStatus st,boolean assigned){
        return switch(st){case WAITING -> assigned?3:2;case ASSIGNED -> 3;case RIDING -> 4;case SERVED -> 5;};
    }
    private static Color typeColor(UserType t){return switch(t){case PMR->PURPLE;case ELDERLY->AMBER;case EMERGENCY->RED;default->new Color(71,112,214);};}
    private static String floorLabel(int f){return f==0?"RDC":"ÉTAGE "+f;}
    private static String abbreviate(String s,int n){return s.length()<=n?s:s.substring(0,Math.max(1,n-1))+"…";}
    private static Font uiFont(int style,float size){return new Font("Segoe UI",style,Math.round(size));}

    private static void roundCard(Graphics2D g2,int x,int y,int w,int h,int r,Color fill,Color border){
        g2.setColor(new Color(0,0,0,45));g2.fillRoundRect(x+3,y+4,w,h,r,r);
        g2.setColor(fill);g2.fillRoundRect(x,y,w,h,r,r);
        g2.setColor(border);g2.drawRoundRect(x,y,w,h,r,r);
    }
    private static void drawWrapped(Graphics2D g2,String text,int x,int y,int maxW,int lineH){
        String[] words=text.split(" ");StringBuilder line=new StringBuilder();int yy=y;
        for(String word:words){String test=line.length()==0?word:line+" "+word;if(g2.getFontMetrics().stringWidth(test)>maxW && line.length()>0){g2.drawString(line.toString(),x,yy);yy+=lineH;line=new StringBuilder(word);}else line=new StringBuilder(test);}if(line.length()>0)g2.drawString(line.toString(),x,yy);
    }
}
