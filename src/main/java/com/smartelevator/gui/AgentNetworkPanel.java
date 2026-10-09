package com.smartelevator.gui;

import com.smartelevator.core.AppState;
import com.smartelevator.model.ElevatorSnapshot;
import com.smartelevator.model.ElevatorStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Locale;

/** Visual explanation of the SMA architecture for the presentation view. */
public class AgentNetworkPanel extends JPanel {
    private static final Color BG = new Color(6, 14, 25);
    private static final Color TEXT = new Color(239, 247, 250);
    private static final Color MUTED = new Color(135, 158, 177);
    private static final Color CYAN = new Color(34, 211, 238);
    private static final Color TEAL = new Color(20, 184, 166);
    private static final Color PURPLE = new Color(168, 85, 247);
    private static final Color AMBER = new Color(245, 158, 11);
    private static final Color RED = new Color(244, 63, 94);
    private long frame;

    public AgentNetworkPanel() {
        setOpaque(true);
        setBackground(BG);
        Timer timer = new Timer(40, e -> { frame++; repaint(); });
        timer.start();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w=getWidth(), h=getHeight();
        g2.setPaint(new GradientPaint(0,0,new Color(8,22,37),w,h,new Color(4,10,18)));
        g2.fillRect(0,0,w,h);
        RadialGradientPaint glow = new RadialGradientPaint(new Point2D.Float(w*.5f,h*.45f), Math.max(w,h)*.55f,
                new float[]{0f,1f}, new Color[]{new Color(14,165,233,38), new Color(6,14,25,0)});
        g2.setPaint(glow); g2.fillRect(0,0,w,h);

        g2.setColor(TEXT); g2.setFont(font(Font.BOLD,22));
        g2.drawString("Architecture multi-agent en temps réel", 34, 44);
        g2.setColor(MUTED); g2.setFont(font(Font.PLAIN,11));
        g2.drawString("Les messages ACL circulent réellement entre les agents JADE — REQUEST, CFP, PROPOSE, ACCEPT",34,65);

        int cx=w/2, cy=h/2-8;
        int r=Math.min(250,Math.min(w,h)/3);
        int ux=cx-r-90, px=cx, mx=cx+r+90;
        int eY=cy+r/2+100;
        int aX=cx-r, bX=cx, cX=cx+r;

        drawLink(g2, ux,cy, cx,cy, TEAL, 0, "REQUEST");
        drawLink(g2, px,cy-r-80, cx,cy, PURPLE, 18, "PRIORITY");
        drawLink(g2, mx,cy, cx,cy, AMBER, 36, "STATUS");
        drawLink(g2, cx,cy, aX,eY, CYAN, 8, "CFP / PROPOSE");
        drawLink(g2, cx,cy, bX,eY, CYAN, 28, "CFP / PROPOSE");
        drawLink(g2, cx,cy, cX,eY, CYAN, 48, "CFP / PROPOSE");

        drawNode(g2,ux,cy,66,"U","UserAgent","Demande patient",new Color(59,130,246),true);
        drawNode(g2,px,cy-r-80,66,"P","PriorityAgent","Priorité + fairness",PURPLE,true);
        drawNode(g2,mx,cy,66,"M","MaintenanceAgent","Panne / reprise",AMBER,true);
        drawNode(g2,cx,cy,92,"C","CoordinatorAgent","Contract-Net • sélection",new Color(14,165,233),true);

        List<ElevatorSnapshot> elevators=AppState.getElevators();
        drawElevatorNode(g2,aX,eY,"A", elevators.stream().filter(e->"A".equals(e.getName())).findFirst().orElse(null));
        drawElevatorNode(g2,bX,eY,"B", elevators.stream().filter(e->"B".equals(e.getName())).findFirst().orElse(null));
        drawElevatorNode(g2,cX,eY,"C", elevators.stream().filter(e->"C".equals(e.getName())).findFirst().orElse(null));

        // live ACL event ribbon
        String last=latestAcl();
        int rw=Math.min(w-68,760); int rx=(w-rw)/2; int ry=h-78;
        g2.setColor(new Color(10,28,45,235));g2.fillRoundRect(rx,ry,rw,46,18,18);
        g2.setColor(new Color(42,65,83));g2.drawRoundRect(rx,ry,rw,46,18,18);
        g2.setColor(new Color(94,234,212));g2.setFont(font(Font.BOLD,9));g2.drawString("DERNIER ÉVÉNEMENT ACL",rx+16,ry+18);
        g2.setColor(TEXT);g2.setFont(font(Font.BOLD,11));g2.drawString(last,rx+16,ry+35);
        g2.dispose();
    }

    private void drawElevatorNode(Graphics2D g2,int x,int y,String name,ElevatorSnapshot e){
        Color c=e!=null&&e.getStatus()== ElevatorStatus.OUT_OF_SERVICE?RED:new Color(34,197,94);
        String sub=e==null?"Initialisation":"Étage "+e.getFloor()+" • "+e.getStatus().getLabel();
        drawNode(g2,x,y,72,name,"ElevatorAgent "+name,sub,c,true);
    }

    private void drawNode(Graphics2D g2,int cx,int cy,int size,String icon,String title,String sub,Color c,boolean pulse){
        if(pulse){int p=8+(int)((Math.sin((frame+cx)*.08)+1)*4);g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),28));g2.fillOval(cx-size/2-p,cy-size/2-p,size+2*p,size+2*p);}
        g2.setColor(new Color(8,23,38));g2.fillOval(cx-size/2,cy-size/2,size,size);
        g2.setColor(c);g2.setStroke(new BasicStroke(2.5f));g2.drawOval(cx-size/2,cy-size/2,size,size);
        g2.setColor(Color.WHITE);g2.setFont(font(Font.BOLD,size>80?22:17));FontMetrics fm=g2.getFontMetrics();g2.drawString(icon,cx-fm.stringWidth(icon)/2,cy+6);
        g2.setColor(TEXT);g2.setFont(font(Font.BOLD,10));fm=g2.getFontMetrics();g2.drawString(title,cx-fm.stringWidth(title)/2,cy+size/2+20);
        g2.setColor(MUTED);g2.setFont(font(Font.PLAIN,8.5f));fm=g2.getFontMetrics();g2.drawString(sub,cx-fm.stringWidth(sub)/2,cy+size/2+36);
    }

    private void drawLink(Graphics2D g2,int x1,int y1,int x2,int y2,Color c,int phase,String label){
        g2.setStroke(new BasicStroke(1.4f));g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),70));g2.drawLine(x1,y1,x2,y2);
        double t=((frame+phase)%100)/100.0;int px=(int)(x1+(x2-x1)*t), py=(int)(y1+(y2-y1)*t);
        g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),38));g2.fillOval(px-9,py-9,18,18);g2.setColor(c);g2.fillOval(px-3,py-3,6,6);
        int lx=(x1+x2)/2, ly=(y1+y2)/2-8;
        g2.setColor(new Color(5,14,25,220));g2.setFont(font(Font.BOLD,7.2f));int tw=g2.getFontMetrics().stringWidth(label)+12;g2.fillRoundRect(lx-tw/2,ly-10,tw,17,8,8);
        g2.setColor(new Color(199,242,255));g2.drawString(label,lx-g2.getFontMetrics().stringWidth(label)/2,ly+2);
    }

    private String latestAcl(){
        List<String> logs=AppState.getLogs();
        for(int i=logs.size()-1;i>=0;i--){
            String l=logs.get(i),u=l.toUpperCase(Locale.ROOT);
            if(u.contains("CFP")||u.contains("PROPOSE")||u.contains("ACCEPT")||u.contains("REQUEST")) return trim(l,96);
        }
        return "Réseau JADE opérationnel — en attente d'une nouvelle mission";
    }
    private static String trim(String s,int n){return s.length()<=n?s:s.substring(0,n-1)+"…";}
    private static Font font(int style,float size){return new Font("Segoe UI",style,Math.round(size));}
}
