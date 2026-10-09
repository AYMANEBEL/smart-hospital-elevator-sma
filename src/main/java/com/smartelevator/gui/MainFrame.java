package com.smartelevator.gui;

import com.smartelevator.core.AppState;
import com.smartelevator.core.JadePlatform;
import com.smartelevator.core.MaintenanceCommand;
import com.smartelevator.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * V6: product-oriented desktop shell for the Smart Hospital SMA demo.
 * The live simulation, missions and agent architecture are separated into clear views.
 */
public class MainFrame extends JFrame {
    private static final Color BG = new Color(4, 10, 18);
    private static final Color SIDEBAR = new Color(6, 16, 28);
    private static final Color PANEL = new Color(9, 21, 35);
    private static final Color PANEL2 = new Color(12, 28, 45);
    private static final Color PANEL3 = new Color(15, 34, 53);
    private static final Color BORDER = new Color(35, 57, 75);
    private static final Color TEXT = new Color(239, 247, 250);
    private static final Color MUTED = new Color(136, 158, 177);
    private static final Color TEAL = new Color(20, 184, 166);
    private static final Color CYAN = new Color(34, 211, 238);
    private static final Color BLUE = new Color(59, 130, 246);
    private static final Color GREEN = new Color(34, 197, 94);
    private static final Color AMBER = new Color(245, 158, 11);
    private static final Color RED = new Color(244, 63, 94);
    private static final Color PURPLE = new Color(168, 85, 247);

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, NavButton> nav = new HashMap<>();

    private final HospitalCommandCenterPanel commandCenter = new HospitalCommandCenterPanel();
    private final AgentNetworkPanel agentNetwork = new AgentNetworkPanel();

    private final JLabel servedValue = value("0");
    private final JLabel pendingValue = value("0");
    private final JLabel avgValue = value("0.0 s");
    private final JLabel pmrValue = value("0.0 s");
    private final JLabel activeValue = value("0 / 3");

    private final DefaultTableModel liveMissionModel = missionModel();
    private final DefaultTableModel fullMissionModel = missionModel();
    private final JTable liveMissionTable = new JTable(liveMissionModel);
    private final JTable fullMissionTable = new JTable(fullMissionModel);
    private final JTextArea logsArea = new JTextArea();

    private final Map<String, ElevatorCard> elevatorCards = new HashMap<>();
    private final Map<String, JButton> maintenanceButtons = new HashMap<>();
    private final JLabel missionTitle = new JLabel("Aucune mission active");
    private final JLabel missionSub = new JLabel("Le système attend une demande patient.");
    private final JLabel missionStatus = new JLabel("PRÊT");

    public MainFrame() {
        super("Smart Hospital Mobility • Multi-Agent Command Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1420, 830));
        setSize(1720, 980);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        applyUiDefaults();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        setContentPane(root);
        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(buildTopbar(), BorderLayout.NORTH);
        content.setOpaque(false);
        content.add(buildLivePage(), "LIVE");
        content.add(buildMissionsPage(), "MISSIONS");
        content.add(buildAgentsPage(), "AGENTS");
        right.add(content, BorderLayout.CENTER);
        root.add(right, BorderLayout.CENTER);

        selectPage("LIVE");
        AppState.addListener(() -> SwingUtilities.invokeLater(this::refreshAll));
        javax.swing.Timer uiClock = new javax.swing.Timer(500, e -> refreshAll());
        uiClock.start();
        refreshAll();
    }

    private void applyUiDefaults() {
        UIManager.put("Label.font", font(Font.PLAIN, 12));
        UIManager.put("Button.font", font(Font.BOLD, 11));
        UIManager.put("Table.font", font(Font.PLAIN, 11));
        UIManager.put("TableHeader.font", font(Font.BOLD, 10));
        UIManager.put("TextArea.font", new Font("Consolas", Font.PLAIN, 11));
        UIManager.put("ToolTip.font", font(Font.PLAIN, 10));
    }

    // -------------------------------------------------------------------------
    // Shell
    // -------------------------------------------------------------------------

    private JComponent buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(SIDEBAR);
        side.setPreferredSize(new Dimension(94, 100));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(18, 10, 18, 10));

        JPanel logo = new JPanel(new BorderLayout());
        logo.setOpaque(false);
        logo.setMaximumSize(new Dimension(74, 66));
        JLabel cross = new JLabel("✚", SwingConstants.CENTER);
        cross.setOpaque(true);
        cross.setBackground(new Color(190, 24, 93));
        cross.setForeground(Color.WHITE);
        cross.setFont(font(Font.BOLD, 30));
        cross.setBorder(new EmptyBorder(8, 0, 8, 0));
        logo.add(cross);
        side.add(logo);
        side.add(Box.createVerticalStrut(28));

        NavButton live = navButton("LIVE", "▣", "Opérations live");
        NavButton missions = navButton("MISSIONS", "≡", "Missions patients");
        NavButton agents = navButton("AGENTS", "◎", "Réseau d'agents");
        side.add(live); side.add(Box.createVerticalStrut(10));
        side.add(missions); side.add(Box.createVerticalStrut(10));
        side.add(agents);
        side.add(Box.createVerticalGlue());

        JButton present = new JButton("⛶");
        present.setToolTipText("Mode présentation plein écran");
        present.setAlignmentX(Component.CENTER_ALIGNMENT);
        present.setMaximumSize(new Dimension(54, 48));
        present.setBackground(new Color(13, 148, 136));
        present.setForeground(Color.WHITE);
        present.setBorderPainted(false);
        present.setFocusPainted(false);
        present.setFont(font(Font.BOLD, 20));
        present.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        present.addActionListener(e -> openPresentationMode());
        side.add(present);
        return side;
    }

    private JComponent buildTopbar() {
        JPanel top = new JPanel(new BorderLayout(18, 0));
        top.setBackground(BG);
        top.setBorder(new EmptyBorder(14, 18, 10, 18));
        top.setPreferredSize(new Dimension(100, 94));

        JPanel brand = transparent();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        JPanel line = transparent(); line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));
        JLabel title = new JLabel("SMART HOSPITAL MOBILITY");
        title.setForeground(TEXT); title.setFont(font(Font.BOLD, 24));
        line.add(title); line.add(Box.createHorizontalStrut(10));
        line.add(chip("● JADE ONLINE", new Color(6,78,59), new Color(167,243,208)));
        brand.add(line); brand.add(Box.createVerticalStrut(5));
        JLabel sub = new JLabel("Multi-Agent Command Center  /  patient transport & elevator orchestration");
        sub.setForeground(MUTED); sub.setFont(font(Font.PLAIN, 10.5f)); brand.add(sub);
        top.add(brand, BorderLayout.WEST);

        JPanel metrics = new JPanel(new GridLayout(1, 5, 8, 0));
        metrics.setOpaque(false);
        metrics.setPreferredSize(new Dimension(630, 64));
        metrics.add(kpi("SERVIS", servedValue, TEAL));
        metrics.add(kpi("EN ATTENTE", pendingValue, AMBER));
        metrics.add(kpi("ATTENTE MOY.", avgValue, BLUE));
        metrics.add(kpi("PMR / URGENCE", pmrValue, PURPLE));
        metrics.add(kpi("ASCENSEURS", activeValue, GREEN));
        top.add(metrics, BorderLayout.EAST);
        return top;
    }

    // -------------------------------------------------------------------------
    // Live page
    // -------------------------------------------------------------------------

    private JComponent buildLivePage() {
        JPanel page = new JPanel(new BorderLayout(12, 0));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(0, 18, 18, 18));
        page.add(buildActionRail(), BorderLayout.WEST);
        page.add(commandCenter, BorderLayout.CENTER);
        page.add(buildOperationsRail(), BorderLayout.EAST);
        return page;
    }

    private JComponent buildActionRail() {
        RoundedPanel rail = new RoundedPanel(22, PANEL);
        rail.setPreferredSize(new Dimension(292, 100));
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.setBorder(new EmptyBorder(18, 16, 18, 16));

        rail.add(eyebrow("NOUVEL APPEL"));
        rail.add(Box.createVerticalStrut(5));
        rail.add(h2("Créer une mission patient"));
        rail.add(Box.createVerticalStrut(6));
        rail.add(body("Renseignez le trajet et le profil. Le coordinateur lance ensuite la négociation entre les ascenseurs."));
        rail.add(Box.createVerticalStrut(16));

        JSpinner source = spinner(0);
        JSpinner destination = spinner(7);
        JComboBox<UserType> type = combo();
        rail.add(field("ÉTAGE DE DÉPART", source));
        rail.add(Box.createVerticalStrut(10));
        rail.add(field("DESTINATION", destination));
        rail.add(Box.createVerticalStrut(10));
        rail.add(field("PROFIL PATIENT", type));
        rail.add(Box.createVerticalStrut(14));

        JButton call = actionButton("APPELER UN ASCENSEUR", TEAL);
        call.addActionListener(e -> JadePlatform.submitRequest((Integer)source.getValue(), (Integer)destination.getValue(), (UserType)type.getSelectedItem()));
        rail.add(call);

        rail.add(Box.createVerticalStrut(20));
        rail.add(divider());
        rail.add(Box.createVerticalStrut(16));
        rail.add(eyebrow("SCÉNARIOS DÉMO"));
        rail.add(Box.createVerticalStrut(10));

        JButton pmr = scenario("♿", "PMR + visiteur", "Priorité et comparaison");
        pmr.addActionListener(e -> { JadePlatform.submitRequest(1,8,UserType.PMR); JadePlatform.submitRequest(6,2,UserType.NORMAL); });
        JButton crowd = scenario("▦", "Affluence multi-étages", "Allocation distribuée");
        crowd.addActionListener(e -> {
            JadePlatform.submitRequest(2,9,UserType.NORMAL); JadePlatform.submitRequest(8,1,UserType.ELDERLY);
            JadePlatform.submitRequest(4,10,UserType.NORMAL); JadePlatform.submitRequest(0,7,UserType.PMR);
        });
        JButton urgent = scenario("✚", "Urgence médicale", "Priorité critique");
        urgent.addActionListener(e -> { JadePlatform.submitRequest(0,10,UserType.EMERGENCY); JadePlatform.submitRequest(5,1,UserType.NORMAL); });
        rail.add(pmr); rail.add(Box.createVerticalStrut(8)); rail.add(crowd); rail.add(Box.createVerticalStrut(8)); rail.add(urgent);

        rail.add(Box.createVerticalGlue());
        rail.add(buildPriorityLegend());
        return rail;
    }

    private JComponent buildOperationsRail() {
        RoundedPanel rail = new RoundedPanel(22, PANEL);
        rail.setPreferredSize(new Dimension(350, 100));
        rail.setLayout(new BorderLayout(0, 12));
        rail.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel top = transparent(); top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(eyebrow("MISSION FOCUS")); top.add(Box.createVerticalStrut(8));
        top.add(buildMissionFocusCard()); top.add(Box.createVerticalStrut(14));
        top.add(eyebrow("ASCENSEURS")); top.add(Box.createVerticalStrut(8));
        for (String name : List.of("A","B","C")) {
            ElevatorCard card = new ElevatorCard(name);
            elevatorCards.put(name, card);
            top.add(card); top.add(Box.createVerticalStrut(8));
        }
        rail.add(top, BorderLayout.NORTH);

        JPanel missions = transparent(); missions.setLayout(new BorderLayout(0, 8));
        JPanel mh = transparent(); mh.setLayout(new BorderLayout());
        mh.add(eyebrow("DERNIÈRES MISSIONS"), BorderLayout.WEST);
        JLabel live = chip("LIVE", new Color(13,78,72), new Color(153,246,228));
        mh.add(live, BorderLayout.EAST);
        missions.add(mh, BorderLayout.NORTH);
        styleMissionTable(liveMissionTable, true);
        JScrollPane sp = tableScroll(liveMissionTable);
        sp.setPreferredSize(new Dimension(320, 190));
        missions.add(sp, BorderLayout.CENTER);
        rail.add(missions, BorderLayout.CENTER);
        return rail;
    }

    private JComponent buildMissionFocusCard() {
        RoundedPanel card = new RoundedPanel(18, PANEL2);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        JPanel icon = new JPanel(new GridBagLayout());
        icon.setBackground(new Color(13, 78, 72));
        icon.setPreferredSize(new Dimension(54, 54));
        JLabel cross = new JLabel("✚"); cross.setForeground(new Color(153,246,228)); cross.setFont(font(Font.BOLD,22)); icon.add(cross);
        card.add(icon, BorderLayout.WEST);
        JPanel txt = transparent(); txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        missionTitle.setForeground(TEXT); missionTitle.setFont(font(Font.BOLD, 12));
        missionSub.setForeground(MUTED); missionSub.setFont(font(Font.PLAIN, 9));
        missionStatus.setForeground(new Color(167,243,208)); missionStatus.setFont(font(Font.BOLD, 9));
        txt.add(missionTitle); txt.add(Box.createVerticalStrut(4)); txt.add(missionSub); txt.add(Box.createVerticalStrut(6)); txt.add(missionStatus);
        card.add(txt, BorderLayout.CENTER);
        return card;
    }

    // -------------------------------------------------------------------------
    // Missions page
    // -------------------------------------------------------------------------

    private JComponent buildMissionsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setOpaque(false); page.setBorder(new EmptyBorder(0,18,18,18));

        RoundedPanel hero = new RoundedPanel(22, PANEL);
        hero.setLayout(new BorderLayout()); hero.setBorder(new EmptyBorder(18,20,18,20));
        JPanel left=transparent();left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS));
        left.add(eyebrow("MISSION CONTROL")); left.add(Box.createVerticalStrut(5)); left.add(h2("Historique et performance des transports"));
        left.add(Box.createVerticalStrut(5)); left.add(body("Suivez les priorités, l'ascenseur sélectionné, le temps d'attente et l'état de chaque mission."));
        hero.add(left,BorderLayout.WEST);
        JButton back = actionButton("← RETOUR AU LIVE", new Color(14,116,144));
        back.setPreferredSize(new Dimension(150,40)); back.addActionListener(e->selectPage("LIVE")); hero.add(back,BorderLayout.EAST);
        page.add(hero,BorderLayout.NORTH);

        JPanel body=new JPanel(new BorderLayout(12,0));body.setOpaque(false);
        RoundedPanel tableCard=new RoundedPanel(22,PANEL);tableCard.setLayout(new BorderLayout(0,12));tableCard.setBorder(new EmptyBorder(16,16,16,16));
        JPanel th=transparent();th.setLayout(new BorderLayout());th.add(h2("Toutes les missions"),BorderLayout.WEST);th.add(chip("DONNÉES JADE",new Color(30,58,95),new Color(191,219,254)),BorderLayout.EAST);tableCard.add(th,BorderLayout.NORTH);
        styleMissionTable(fullMissionTable,false);tableCard.add(tableScroll(fullMissionTable),BorderLayout.CENTER);
        body.add(tableCard,BorderLayout.CENTER);
        body.add(buildAnalyticsRail(),BorderLayout.EAST);
        page.add(body,BorderLayout.CENTER);
        return page;
    }

    private JComponent buildAnalyticsRail() {
        RoundedPanel p=new RoundedPanel(22,PANEL);p.setPreferredSize(new Dimension(330,100));p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(new EmptyBorder(18,16,18,16));
        p.add(eyebrow("INDICATEURS"));p.add(Box.createVerticalStrut(6));p.add(h2("Qualité de service"));p.add(Box.createVerticalStrut(14));
        p.add(infoCard("Priorisation PMR", "Les profils PMR et urgence disposent d'une priorité supérieure.", PURPLE));p.add(Box.createVerticalStrut(10));
        p.add(infoCard("Fairness", "Le temps d'attente augmente progressivement le score pour éviter la famine.", AMBER));p.add(Box.createVerticalStrut(10));
        p.add(infoCard("Allocation", "Distance, direction, charge et disponibilité influencent la sélection.", BLUE));p.add(Box.createVerticalStrut(10));
        p.add(infoCard("Résilience", "Un ascenseur en panne est retiré des négociations jusqu'à sa réactivation.", RED));
        p.add(Box.createVerticalGlue());
        JLabel note=body("Conseil démo : comparez une mission normale et une mission PMR, puis simulez une panne.");note.setForeground(new Color(165,243,252));p.add(note);
        return p;
    }

    // -------------------------------------------------------------------------
    // Agents page
    // -------------------------------------------------------------------------

    private JComponent buildAgentsPage() {
        JPanel page = new JPanel(new BorderLayout(12,0));
        page.setOpaque(false);page.setBorder(new EmptyBorder(0,18,18,18));
        RoundedPanel network = new RoundedPanel(22, PANEL);network.setLayout(new BorderLayout());network.setBorder(new EmptyBorder(8,8,8,8));network.add(agentNetwork,BorderLayout.CENTER);
        page.add(network,BorderLayout.CENTER);

        RoundedPanel logs = new RoundedPanel(22,PANEL);logs.setPreferredSize(new Dimension(390,100));logs.setLayout(new BorderLayout(0,10));logs.setBorder(new EmptyBorder(16,16,16,16));
        JPanel lh=transparent();lh.setLayout(new BorderLayout());
        JPanel lt=transparent();lt.setLayout(new BoxLayout(lt,BoxLayout.Y_AXIS));lt.add(eyebrow("ACL TRACE"));lt.add(Box.createVerticalStrut(4));lt.add(h2("Messages entre agents"));lh.add(lt,BorderLayout.WEST);
        JButton clear=smallButton("EFFACER");clear.addActionListener(e->AppState.clearLogs());lh.add(clear,BorderLayout.EAST);logs.add(lh,BorderLayout.NORTH);
        logsArea.setEditable(false);logsArea.setBackground(new Color(3,10,20));logsArea.setForeground(new Color(197,221,233));logsArea.setCaretColor(Color.WHITE);logsArea.setBorder(new EmptyBorder(10,10,10,10));
        JScrollPane sp=new JScrollPane(logsArea);sp.setBorder(BorderFactory.createLineBorder(BORDER));sp.getViewport().setBackground(new Color(3,10,20));logs.add(sp,BorderLayout.CENTER);
        JPanel pills=transparent();pills.setLayout(new FlowLayout(FlowLayout.LEFT,6,0));pills.add(chip("REQUEST",new Color(30,58,138),new Color(219,234,254)));pills.add(chip("CFP",new Color(88,28,135),new Color(233,213,255)));pills.add(chip("PROPOSE",new Color(146,64,14),new Color(254,215,170)));pills.add(chip("ACCEPT",new Color(6,95,70),new Color(167,243,208)));logs.add(pills,BorderLayout.SOUTH);
        page.add(logs,BorderLayout.EAST);
        return page;
    }

    // -------------------------------------------------------------------------
    // Data refresh
    // -------------------------------------------------------------------------

    private void refreshAll() {
        servedValue.setText(String.valueOf(AppState.getServedCount()));
        pendingValue.setText(String.valueOf(AppState.getPendingCount()));
        avgValue.setText(String.format(Locale.US,"%.1f s",AppState.getAverageWait()));
        pmrValue.setText(String.format(Locale.US,"%.1f s",AppState.getPmrAverageWait()));
        activeValue.setText(AppState.getActiveElevators()+" / 3");

        List<RequestSnapshot> requests=AppState.getRequests();
        fillModel(liveMissionModel, requests.size()>6?requests.subList(Math.max(0,requests.size()-6),requests.size()):requests);
        fillModel(fullMissionModel,requests);

        for(ElevatorSnapshot e:AppState.getElevators()){
            ElevatorCard c=elevatorCards.get(e.getName());if(c!=null)c.update(e);
        }

        RequestSnapshot focus=requests.stream().filter(r->r.getStatus()!=RequestStatus.SERVED)
                .min(Comparator.comparingInt((RequestSnapshot r)->-r.getRequest().getPriority()).thenComparingLong(r->r.getRequest().getCreatedAt())).orElse(null);
        if(focus==null){missionTitle.setText("Aucune mission active");missionSub.setText("Le système attend une demande patient.");missionStatus.setText("● PRÊT");missionStatus.setForeground(new Color(167,243,208));}
        else{
            Request r=focus.getRequest();String asc=focus.getElevatorName()==null?"négociation":"E"+focus.getElevatorName();
            missionTitle.setText(r.getId()+" • "+r.getUserType().getLabel());
            missionSub.setText(floorShort(r.getSourceFloor())+" → "+floorShort(r.getDestinationFloor())+"  •  "+asc+"  •  P"+r.getPriority());
            missionStatus.setText("● "+focus.getStatus().getLabel().toUpperCase(Locale.ROOT));missionStatus.setForeground(typeColor(r.getUserType()).brighter());
        }

        String logs=String.join("\n",AppState.getLogs());
        if(!logs.equals(logsArea.getText())){logsArea.setText(logs);logsArea.setCaretPosition(logsArea.getDocument().getLength());}
        commandCenter.repaint(); agentNetwork.repaint();
    }

    private void fillModel(DefaultTableModel model,List<RequestSnapshot> list){
        model.setRowCount(0);
        for(RequestSnapshot rs:list){Request r=rs.getRequest();model.addRow(new Object[]{r.getId(),r.getUserType().getLabel(),floorShort(r.getSourceFloor())+" → "+floorShort(r.getDestinationFloor()),r.getPriority(),String.format(Locale.US,"%.1f s",rs.getWaitingSeconds()),rs.getStatus().getLabel(),rs.getElevatorName()==null?"—":"E"+rs.getElevatorName()});}
    }

    // -------------------------------------------------------------------------
    // Navigation / presentation
    // -------------------------------------------------------------------------

    private NavButton navButton(String key,String icon,String tooltip){NavButton b=new NavButton(icon,tooltip);b.addActionListener(e->selectPage(key));nav.put(key,b);return b;}
    private void selectPage(String key){cards.show(content,key);nav.forEach((k,b)->b.setActive(k.equals(key)));}

    private void openPresentationMode(){
        JFrame f=new JFrame("Smart Hospital Mobility • Presentation Mode");
        f.setUndecorated(true);f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);f.setExtendedState(JFrame.MAXIMIZED_BOTH);
        JPanel root=new JPanel(new BorderLayout());root.setBackground(BG);root.setBorder(new EmptyBorder(12,12,12,12));
        HospitalCommandCenterPanel stage=new HospitalCommandCenterPanel();root.add(stage,BorderLayout.CENTER);
        JButton close=new JButton("ESC  FERMER");close.setBackground(new Color(15,34,53));close.setForeground(TEXT);close.setBorderPainted(false);close.setFocusPainted(false);close.setFont(font(Font.BOLD,10));close.addActionListener(e->f.dispose());
        JPanel bar=new JPanel(new FlowLayout(FlowLayout.RIGHT));bar.setOpaque(false);bar.add(close);root.add(bar,BorderLayout.NORTH);
        f.setContentPane(root);f.getRootPane().registerKeyboardAction(e->f.dispose(),KeyStroke.getKeyStroke("ESCAPE"),JComponent.WHEN_IN_FOCUSED_WINDOW);f.setVisible(true);
    }

    // -------------------------------------------------------------------------
    // Components / styling
    // -------------------------------------------------------------------------

    private JComponent buildPriorityLegend(){
        RoundedPanel p=new RoundedPanel(16,new Color(10,25,41));p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(new EmptyBorder(11,12,11,12));p.setMaximumSize(new Dimension(Integer.MAX_VALUE,108));
        JLabel t=eyebrow("PRIORITÉS");p.add(t);p.add(Box.createVerticalStrut(8));p.add(legend("Patient / visiteur",BLUE));p.add(legend("Personne âgée",AMBER));p.add(legend("PMR / mobilité réduite",PURPLE));p.add(legend("Urgence médicale",RED));return p;
    }

    private JComponent legend(String text,Color c){JPanel p=transparent();p.setLayout(new FlowLayout(FlowLayout.LEFT,6,1));JLabel d=new JLabel("●");d.setForeground(c);JLabel l=new JLabel(text);l.setForeground(new Color(199,214,225));l.setFont(font(Font.PLAIN,8.5f));p.add(d);p.add(l);p.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));return p;}
    private JComponent infoCard(String title,String text,Color accent){RoundedPanel p=new RoundedPanel(15,new Color(12,29,46));p.setLayout(new BorderLayout(10,0));p.setBorder(new EmptyBorder(11,11,11,11));JPanel stripe=new JPanel();stripe.setBackground(accent);stripe.setPreferredSize(new Dimension(4,44));p.add(stripe,BorderLayout.WEST);JPanel c=transparent();c.setLayout(new BoxLayout(c,BoxLayout.Y_AXIS));JLabel t=new JLabel(title);t.setForeground(TEXT);t.setFont(font(Font.BOLD,10));JLabel b=body(text);b.setFont(font(Font.PLAIN,8.5f));c.add(t);c.add(Box.createVerticalStrut(3));c.add(b);p.add(c,BorderLayout.CENTER);p.setMaximumSize(new Dimension(Integer.MAX_VALUE,76));return p;}

    private JComponent field(String title,JComponent control){JPanel p=transparent();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setAlignmentX(Component.LEFT_ALIGNMENT);p.setMaximumSize(new Dimension(Integer.MAX_VALUE,65));JLabel l=new JLabel(title);l.setForeground(MUTED);l.setFont(font(Font.BOLD,8.5f));l.setAlignmentX(Component.LEFT_ALIGNMENT);control.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(l);p.add(Box.createVerticalStrut(5));p.add(control);return p;}

    private JSpinner spinner(int v){JSpinner s=new JSpinner(new SpinnerNumberModel(v,0,10,1));s.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));s.setPreferredSize(new Dimension(250,40));s.setBorder(BorderFactory.createLineBorder(BORDER));if(s.getEditor() instanceof JSpinner.DefaultEditor de){de.getTextField().setBackground(new Color(15,34,53));de.getTextField().setForeground(TEXT);de.getTextField().setCaretColor(TEXT);de.getTextField().setBorder(new EmptyBorder(7,10,7,10));de.getTextField().setFont(font(Font.BOLD,11));}return s;}
    private JComboBox<UserType> combo(){JComboBox<UserType> c=new JComboBox<>(UserType.values());c.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));c.setPreferredSize(new Dimension(250,40));c.setBackground(new Color(15,34,53));c.setForeground(TEXT);c.setBorder(BorderFactory.createLineBorder(BORDER));c.setRenderer(new DefaultListCellRenderer(){@Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean isSelected,boolean cellHasFocus){JLabel l=(JLabel)super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);l.setBorder(new EmptyBorder(7,9,7,9));l.setBackground(isSelected?new Color(18,93,99):new Color(15,34,53));l.setForeground(Color.WHITE);l.setFont(font(Font.PLAIN,10));return l;}});return c;}

    private JButton actionButton(String text,Color bg){JButton b=new JButton(text);b.setAlignmentX(Component.LEFT_ALIGNMENT);b.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));b.setPreferredSize(new Dimension(250,42));b.setBackground(bg);b.setForeground(Color.WHITE);b.setBorderPainted(false);b.setFocusPainted(false);b.setFont(font(Font.BOLD,10));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;}
    private JButton scenario(String icon,String title,String sub){JButton b=new JButton("<html><b>"+icon+"  "+title+"</b><br><span style='color:#8ba0b3;font-size:8px'>"+sub+"</span></html>");b.setHorizontalAlignment(SwingConstants.LEFT);b.setAlignmentX(Component.LEFT_ALIGNMENT);b.setMaximumSize(new Dimension(Integer.MAX_VALUE,48));b.setPreferredSize(new Dimension(250,48));b.setBackground(new Color(13,30,48));b.setForeground(TEXT);b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),new EmptyBorder(4,10,4,10)));b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;}
    private JButton smallButton(String text){JButton b=new JButton(text);b.setBackground(new Color(19,41,61));b.setForeground(new Color(205,220,230));b.setBorderPainted(false);b.setFocusPainted(false);b.setFont(font(Font.BOLD,8));b.setPreferredSize(new Dimension(72,27));return b;}

    private static DefaultTableModel missionModel(){return new DefaultTableModel(new Object[]{"ID","Patient","Trajet","P","Attente","État","Asc."},0){@Override public boolean isCellEditable(int r,int c){return false;}};}

    private void styleMissionTable(JTable t,boolean compact){
        t.setBackground(new Color(5,15,27));t.setForeground(new Color(218,230,238));t.setGridColor(new Color(28,49,66));t.setRowHeight(compact?28:34);t.setSelectionBackground(new Color(15,83,91));t.setSelectionForeground(Color.WHITE);t.setShowVerticalLines(false);t.setShowHorizontalLines(true);t.setIntercellSpacing(new Dimension(0,1));t.setFillsViewportHeight(true);t.getTableHeader().setBackground(new Color(14,31,49));t.getTableHeader().setForeground(new Color(174,195,210));t.getTableHeader().setReorderingAllowed(false);t.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){@Override public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column){JLabel l=(JLabel)super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);l.setBorder(new EmptyBorder(0,8,0,8));if(!isSelected){l.setBackground(row%2==0?new Color(6,17,30):new Color(8,20,34));l.setForeground(new Color(218,230,238));Object type=table.getValueAt(row,1);if(column==1&&type!=null){String s=type.toString();if(s.contains("Urgence"))l.setForeground(new Color(253,164,175));else if(s.contains("PMR"))l.setForeground(new Color(216,180,254));else if(s.contains("âgée"))l.setForeground(new Color(253,211,128));}if(column==5)l.setForeground(new Color(110,231,183));}return l;}});
        if(t.getColumnModel().getColumnCount()==7){t.getColumnModel().getColumn(0).setPreferredWidth(48);t.getColumnModel().getColumn(1).setPreferredWidth(compact?105:160);t.getColumnModel().getColumn(2).setPreferredWidth(80);t.getColumnModel().getColumn(3).setPreferredWidth(32);t.getColumnModel().getColumn(4).setPreferredWidth(60);t.getColumnModel().getColumn(5).setPreferredWidth(70);t.getColumnModel().getColumn(6).setPreferredWidth(45);}
    }

    private JScrollPane tableScroll(JTable t){JScrollPane sp=new JScrollPane(t);sp.setBorder(BorderFactory.createLineBorder(BORDER));sp.getViewport().setBackground(new Color(5,15,27));sp.getVerticalScrollBar().setUnitIncrement(14);return sp;}

    private JComponent kpi(String title,JLabel value,Color c){RoundedPanel p=new RoundedPanel(15,new Color(9,22,36));p.setLayout(new BorderLayout(8,0));p.setBorder(new EmptyBorder(8,10,8,10));JPanel accent=new JPanel();accent.setBackground(c);accent.setPreferredSize(new Dimension(3,38));p.add(accent,BorderLayout.WEST);JPanel txt=transparent();txt.setLayout(new BoxLayout(txt,BoxLayout.Y_AXIS));JLabel t=new JLabel(title);t.setForeground(MUTED);t.setFont(font(Font.BOLD,7.8f));txt.add(t);txt.add(Box.createVerticalStrut(2));txt.add(value);p.add(txt,BorderLayout.CENTER);return p;}
    private static JLabel value(String s){JLabel l=new JLabel(s);l.setForeground(TEXT);l.setFont(font(Font.BOLD,17));return l;}
    private static JLabel eyebrow(String s){JLabel l=new JLabel(s);l.setForeground(new Color(94,234,212));l.setFont(font(Font.BOLD,8.5f));l.setAlignmentX(Component.LEFT_ALIGNMENT);return l;}
    private static JLabel h2(String s){JLabel l=new JLabel(s);l.setForeground(TEXT);l.setFont(font(Font.BOLD,15));l.setAlignmentX(Component.LEFT_ALIGNMENT);return l;}
    private static JLabel body(String s){JLabel l=new JLabel("<html><div style='width:235px'>"+s+"</div></html>");l.setForeground(MUTED);l.setFont(font(Font.PLAIN,9));l.setAlignmentX(Component.LEFT_ALIGNMENT);return l;}
    private static JLabel chip(String s,Color bg,Color fg){JLabel l=new JLabel("  "+s+"  ");l.setOpaque(true);l.setBackground(bg);l.setForeground(fg);l.setFont(font(Font.BOLD,8));l.setBorder(new EmptyBorder(5,7,5,7));return l;}
    private static JPanel transparent(){JPanel p=new JPanel();p.setOpaque(false);return p;}
    private static JComponent divider(){JSeparator s=new JSeparator();s.setForeground(new Color(33,53,70));s.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));return s;}
    private static String floorShort(int f){return f==0?"RDC":"E"+f;}
    private static Color typeColor(UserType t){return switch(t){case PMR->PURPLE;case ELDERLY->AMBER;case EMERGENCY->RED;default->BLUE;};}
    private static Font font(int style,float size){return new Font("Segoe UI",style,Math.round(size));}

    private class ElevatorCard extends RoundedPanel {
        private final String name;
        private final JLabel floor = new JLabel("—");
        private final JLabel status = new JLabel("Initialisation");
        private final JLabel queue = new JLabel("file 0");
        private final JButton maintenance;
        ElevatorCard(String name){super(16,PANEL2);this.name=name;setLayout(new BorderLayout(10,0));setBorder(new EmptyBorder(10,11,10,11));setMaximumSize(new Dimension(Integer.MAX_VALUE,64));
            JLabel badge=new JLabel("E"+name,SwingConstants.CENTER);badge.setOpaque(true);badge.setBackground(new Color(14,116,144));badge.setForeground(Color.WHITE);badge.setFont(font(Font.BOLD,11));badge.setPreferredSize(new Dimension(40,40));add(badge,BorderLayout.WEST);
            JPanel c=transparent();c.setLayout(new BoxLayout(c,BoxLayout.Y_AXIS));floor.setForeground(TEXT);floor.setFont(font(Font.BOLD,11));status.setForeground(MUTED);status.setFont(font(Font.PLAIN,8.5f));queue.setForeground(new Color(148,163,184));queue.setFont(font(Font.PLAIN,8));c.add(floor);c.add(Box.createVerticalStrut(2));c.add(status);c.add(Box.createVerticalStrut(2));c.add(queue);add(c,BorderLayout.CENTER);
            maintenance=smallButton("PANNE");maintenance.setBackground(new Color(108,31,31));maintenance.addActionListener(e->{ElevatorSnapshot es=AppState.getElevators().stream().filter(v->name.equals(v.getName())).findFirst().orElse(null);boolean out=es!=null&&es.getStatus()==ElevatorStatus.OUT_OF_SERVICE;AppState.enqueueMaintenance(new MaintenanceCommand(name,out));});maintenanceButtons.put(name,maintenance);add(maintenance,BorderLayout.EAST);
        }
        void update(ElevatorSnapshot e){floor.setText((e.getFloor()==0?"RDC":"Étage "+e.getFloor())+"  "+e.getDirection().getSymbol());status.setText(e.getStatus().getLabel()+" • occ. "+e.getOccupancy());queue.setText("file "+e.getQueueSize()+(e.getCurrentRequestId()==null?" • libre":" • mission "+e.getCurrentRequestId()));boolean out=e.getStatus()==ElevatorStatus.OUT_OF_SERVICE;maintenance.setText(out?"RÉACTIVER":"PANNE");maintenance.setBackground(out?new Color(6,95,70):new Color(108,31,31));}
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;private final Color fill;
        RoundedPanel(int radius,Color fill){this.radius=radius;this.fill=fill;setOpaque(false);}
        @Override protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g2.setColor(new Color(0,0,0,30));g2.fillRoundRect(2,3,getWidth()-3,getHeight()-3,radius,radius);g2.setColor(fill);g2.fillRoundRect(0,0,getWidth()-3,getHeight()-3,radius,radius);g2.setColor(BORDER);g2.drawRoundRect(0,0,getWidth()-3,getHeight()-3,radius,radius);g2.dispose();super.paintComponent(g);}
    }

    private static class NavButton extends JButton {
        private boolean active;
        NavButton(String icon,String tooltip){super("<html><div style='text-align:center'><span style='font-size:18px'>"+icon+"</span></div></html>");setToolTipText(tooltip);setAlignmentX(Component.CENTER_ALIGNMENT);setMaximumSize(new Dimension(62,58));setPreferredSize(new Dimension(62,58));setFocusPainted(false);setBorderPainted(false);setContentAreaFilled(false);setForeground(new Color(139,160,177));setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));addMouseListener(new MouseAdapter(){@Override public void mouseEntered(MouseEvent e){repaint();}@Override public void mouseExited(MouseEvent e){repaint();}});}
        void setActive(boolean a){active=a;setForeground(a?new Color(153,246,228):new Color(139,160,177));repaint();}
        @Override protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);boolean hover=getModel().isRollover();if(active||hover){g2.setColor(active?new Color(13,78,72):new Color(15,32,50));g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);if(active){g2.setColor(TEAL);g2.fillRoundRect(0,10,4,getHeight()-20,4,4);}}g2.dispose();super.paintComponent(g);}
    }
}
