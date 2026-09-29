package com.vending.machine.infrastructure.gui;

import com.vending.machine.domain.model.AutomatonResult;
import com.vending.machine.domain.model.NfaState;
import com.vending.machine.domain.ports.in.ProcessSequenceUseCase;
import com.vending.machine.domain.service.NfaTransitionEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class VendingMachineApp extends JFrame {

    private final transient ProcessSequenceUseCase automatonService;
    private final JTextField inputField;
    private final JLabel statusLabel;
    private final MachinePanel machinePanel;
    private final GraphPanel graphPanel;

    // Productos ampliados a 15 para cubrir los nuevos slots
    private static final List<String> PRODUCT_POOL = Arrays.asList(
            "Agua", "Jugo", "Té", "Energizante",
            "Galletas", "Maní", "Papas", "Nachos",
            "Chicle", "Chocolatina", "Gomitas", "Menta",
            "Chitos", "Barra Cereal", "Tostacos"
    );

    private static final String FONT = "SansSerif";

    // Mapeo exacto de los 15 costos según las secuencias de la tabla
    private static final Map<NfaState, Integer> SLOT_PRICES = Map.ofEntries(
            Map.entry(NfaState.P1, 2000),   // 11
            Map.entry(NfaState.P2, 3000),   // 12
            Map.entry(NfaState.P13, 1500),  // 15 (λ)
            Map.entry(NfaState.P3, 2000),   // 155
            Map.entry(NfaState.P4, 2500),   // 151
            Map.entry(NfaState.P5, 3000),   // 21 (λ)
            Map.entry(NfaState.P6, 3500),   // 215
            Map.entry(NfaState.P7, 4000),   // 22
            Map.entry(NfaState.P14, 2500),  // 25 (λ)
            Map.entry(NfaState.P8, 3000),   // 255
            Map.entry(NfaState.P15, 1000),  // 55 (λ)
            Map.entry(NfaState.P9, 1500),   // 555
            Map.entry(NfaState.P10, 2000),  // 551
            Map.entry(NfaState.P11, 2000),  // 515
            Map.entry(NfaState.P12, 2500)   // 52
    );

    private final Map<NfaState, String> slotAssignments = new EnumMap<>(NfaState.class);
    private Set<NfaState> currentActiveStates = new HashSet<>();
    private Timer animationTimer;

    public VendingMachineApp() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Flujo normal sin LookAndFeel
        }

        this.automatonService = new NfaTransitionEngine();

        setTitle("AFND - Máquina Expendedora Aleatoria (15 Slots)");
        setSize(1350, 880); // Ajustado para albergar el grafo extendido
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(new Color(240, 242, 245));

        shuffleProducts();

        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));

        JPanel legendPanel = leyendaDeValores();
        headerPanel.add(legendPanel, BorderLayout.NORTH);

        JPanel inputContainer = new JPanel(new BorderLayout(10, 10));
        inputContainer.setOpaque(false);
        inputContainer.setBorder(BorderFactory.createTitledBorder(
                null, "Ingrese la cadena de dinero y presione ENTER",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font(FONT, Font.BOLD, 14), Color.DARK_GRAY
        ));

        inputField = new JTextField();
        inputField.setFont(new Font("Monospaced", Font.BOLD, 28));
        inputField.setHorizontalAlignment(SwingConstants.CENTER);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        inputContainer.add(inputField, BorderLayout.CENTER);

        statusLabel = new JLabel("Estado: Esperando entrada de cadena...", SwingConstants.CENTER);
        statusLabel.setFont(new Font(FONT, Font.BOLD, 16));
        statusLabel.setForeground(new Color(100, 100, 100));
        inputContainer.add(statusLabel, BorderLayout.SOUTH);

        headerPanel.add(inputContainer, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 15, 15));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 15, 15));

        machinePanel = new MachinePanel();
        graphPanel = new GraphPanel();

        centerPanel.add(machinePanel);
        centerPanel.add(graphPanel);
        add(centerPanel, BorderLayout.CENTER);

        inputField.addActionListener(e -> processInputSequence());

        updateVisualsToInitialState();
    }

    private static JPanel leyendaDeValores() {
        JPanel legendPanel = new JPanel();
        legendPanel.setBackground(new Color(255, 255, 255));
        legendPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        JLabel legendLabel = new JLabel("<html><span style='font-size:14px;'>"
                + "<b>Guía de Símbolos del Alfabeto (Σ):</b> &nbsp;&nbsp;&nbsp;&nbsp;"
                + "<font color='#e74c3c'><b>0:</b> Reiniciar Orden</font> &nbsp;&nbsp;|&nbsp;&nbsp; "
                + "<font color='#27ae60'><b>1:</b> $1000</font> &nbsp;&nbsp;|&nbsp;&nbsp; "
                + "<font color='#2980b9'><b>2:</b> $2000</font> &nbsp;&nbsp;|&nbsp;&nbsp; "
                + "<font color='#8e44ad'><b>5:</b> $500</font>"
                + "</span></html>");
        legendPanel.add(legendLabel);
        return legendPanel;
    }

    private void shuffleProducts() {
        Collections.shuffle(PRODUCT_POOL);
        int index = 0;
        for (NfaState state : NfaState.values()) {
            if (state.isAcceptance()) {
                slotAssignments.put(state, PRODUCT_POOL.get(index++));
            }
        }
    }

    private void updateVisualsToInitialState() {
        AutomatonResult initResult = automatonService.process("");
        currentActiveStates = initResult.finalActiveStates();
        machinePanel.repaint();
        graphPanel.repaint();
    }

    private void processInputSequence() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        String sequence = inputField.getText().trim();

        if (sequence.contains("0")) {
            shuffleProducts();
            statusLabel.setText("Botón '0' detectado. Orden de productos REINICIADO aleatoriamente.");
            statusLabel.setForeground(new Color(231, 76, 60));
        } else {
            statusLabel.setText("Evaluando cadena paso a paso (No Determinismo simultáneo)...");
            statusLabel.setForeground(Color.BLUE);
        }

        List<Set<NfaState>> trace = new ArrayList<>();
        AutomatonResult finalResult = automatonService.process(sequence);

        if (!finalResult.belongsToAlphabet()) {
            statusLabel.setText(finalResult.message());
            statusLabel.setForeground(Color.RED);
            return;
        }

        for (int i = 0; i <= sequence.length(); i++) {
            trace.add(automatonService.process(sequence.substring(0, i)).finalActiveStates());
        }

        Timer timer = new Timer(1500, new java.awt.event.ActionListener() {
            int step = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (step < trace.size()) {
                    currentActiveStates = trace.get(step);
                    machinePanel.repaint();
                    graphPanel.repaint();
                    step++;
                } else {
                    ((Timer)e.getSource()).stop();
                    finishAnimation(finalResult);
                }
            }
        });
        animationTimer = timer;
        timer.start();
    }

    private void finishAnimation(AutomatonResult result) {
        if (result.isAccepted()) {
            String productName = slotAssignments.get(result.acceptedSlot());
            int price = SLOT_PRICES.get(result.acceptedSlot());
            statusLabel.setText(String.format("¡PERTENECE! %s alcanzado. Entrega: %s (Valor: $%d)",
                    result.acceptedSlot().name(), productName, price));
            statusLabel.setForeground(new Color(39, 174, 96));
        } else {
            statusLabel.setText("NO PERTENECE. La cadena no culminó en ningún Slot válido.");
            statusLabel.setForeground(Color.RED);
        }
    }

    // --- PANEL DE LA MÁQUINA EXPENDEDORA ---
    private class MachinePanel extends JPanel {
        public MachinePanel() {
            setBorder(BorderFactory.createTitledBorder(null, "15 Slots de la Máquina (Reorganización Aleatoria)",
                    0, 0, new Font(FONT, Font.BOLD, 14)));
            setBackground(new Color(250, 250, 250));
            // Matriz 5x3 para albergar los 15 slots cómodamente
            setLayout(new GridLayout(5, 3, 10, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            removeAll();

            for (NfaState state : NfaState.values()) {
                if (state.isAcceptance()) {
                    JPanel slot = new JPanel(new BorderLayout(5, 5));
                    boolean isActive = currentActiveStates.contains(state);

                    slot.setBackground(isActive ? new Color(200, 255, 200) : Color.WHITE);
                    slot.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(isActive ? new Color(46, 204, 113) : new Color(200, 200, 200), isActive ? 3 : 1),
                            BorderFactory.createEmptyBorder(8, 8, 8, 8)
                    ));

                    JLabel idLabel = new JLabel(state.name() + " - $" + SLOT_PRICES.get(state), SwingConstants.CENTER);
                    idLabel.setFont(new Font(FONT, Font.BOLD, 13));
                    idLabel.setForeground(new Color(80, 80, 80));

                    JLabel prodLabel = new JLabel(slotAssignments.get(state), SwingConstants.CENTER);
                    prodLabel.setFont(new Font(FONT, Font.BOLD, 16));
                    prodLabel.setForeground(isActive ? new Color(39, 174, 96) : Color.BLACK);

                    slot.add(idLabel, BorderLayout.NORTH);
                    slot.add(prodLabel, BorderLayout.CENTER);
                    add(slot);
                }
            }
            revalidate();
        }
    }

    // --- PANEL DEL GRAFO DINÁMICO ---
    private class GraphPanel extends JPanel {
        private final Map<NfaState, Point> nodes = new EnumMap<>(NfaState.class);

        public GraphPanel() {
            setBorder(BorderFactory.createTitledBorder(null, "AFND - Grafo Dinámico de Transiciones (15 Cadenas)",
                    0, 0, new Font(FONT, Font.BOLD, 14)));
            setBackground(Color.WHITE);
        }

        private void calculateNodePositions() {
            nodes.clear();

            // Dividimos el ancho del panel en 16 franjas invisibles para distribuir los 15 productos finales sin colisiones
            double stepX = getWidth() / 16.0;
            int startY = Math.max(30, (getHeight() - 400) / 2); // Centrado vertical dinámico

            // Posiciones Y fijas por niveles del árbol
            int y0 = startY;
            int y1 = startY + 60;
            int y2 = startY + 130;
            int y3 = startY + 210;
            int y4 = startY + 300;

            // Nivel 0 y 1 (Centrados geométricamente)
            nodes.put(NfaState.Q0, new Point((int)(8 * stepX), y0));
            nodes.put(NfaState.Q_START, new Point((int)(8 * stepX), y1));

            // Nivel 2: Ramificaciones base (Separadas por su peso de hojas)
            nodes.put(NfaState.Q1, new Point((int)(1.5 * stepX), y2));
            nodes.put(NfaState.Q2, new Point((int)(4.0 * stepX), y2));
            nodes.put(NfaState.Q3, new Point((int)(6.5 * stepX), y2));
            nodes.put(NfaState.Q4, new Point((int)(8.75 * stepX), y2));
            nodes.put(NfaState.Q5, new Point((int)(12.0 * stepX), y2));
            nodes.put(NfaState.Q6, new Point((int)(14.5 * stepX), y2));

            // Nivel 3: Subnodos intermedios y Productos tempranos
            nodes.put(NfaState.P1, new Point((int)(1 * stepX), y3));
            nodes.put(NfaState.P2, new Point((int)(2 * stepX), y3));
            nodes.put(NfaState.Q2A, new Point((int)(4 * stepX), y3));
            nodes.put(NfaState.Q3A, new Point((int)(6.5 * stepX), y3));
            nodes.put(NfaState.P7, new Point((int)(8 * stepX), y3));
            nodes.put(NfaState.Q4A, new Point((int)(9.5 * stepX), y3));
            nodes.put(NfaState.Q5A, new Point((int)(12 * stepX), y3));
            nodes.put(NfaState.Q6A, new Point((int)(14 * stepX), y3));
            nodes.put(NfaState.P12, new Point((int)(15 * stepX), y3));

            // Nivel 4: Productos finales
            nodes.put(NfaState.P13, new Point((int)(3 * stepX), y4));
            nodes.put(NfaState.P3, new Point((int)(4 * stepX), y4));
            nodes.put(NfaState.P4, new Point((int)(5 * stepX), y4));
            nodes.put(NfaState.P5, new Point((int)(6 * stepX), y4));
            nodes.put(NfaState.P6, new Point((int)(7 * stepX), y4));
            nodes.put(NfaState.P14, new Point((int)(9 * stepX), y4));
            nodes.put(NfaState.P8, new Point((int)(10 * stepX), y4));
            nodes.put(NfaState.P15, new Point((int)(11 * stepX), y4));
            nodes.put(NfaState.P9, new Point((int)(12 * stepX), y4));
            nodes.put(NfaState.P10, new Point((int)(13 * stepX), y4));
            nodes.put(NfaState.P11, new Point((int)(14 * stepX), y4));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            calculateNodePositions();

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(200, 200, 200));
            g2.setStroke(new BasicStroke(2.0f));

            // Trazado de Aristas según Idea Maquina Final.md
            drawEdge(g2, NfaState.Q0, NfaState.Q_START, "λ");

            drawEdge(g2, NfaState.Q_START, NfaState.Q1, "1");
            drawEdge(g2, NfaState.Q_START, NfaState.Q2, "1");
            drawEdge(g2, NfaState.Q_START, NfaState.Q3, "2");
            drawEdge(g2, NfaState.Q_START, NfaState.Q4, "2");
            drawEdge(g2, NfaState.Q_START, NfaState.Q5, "5");
            drawEdge(g2, NfaState.Q_START, NfaState.Q6, "5");

            // Ramas '1'
            drawEdge(g2, NfaState.Q1, NfaState.P1, "1");
            drawEdge(g2, NfaState.Q1, NfaState.P2, "2");
            drawEdge(g2, NfaState.Q2, NfaState.Q2A, "5");
            drawEdge(g2, NfaState.Q2A, NfaState.P13, "λ");
            drawEdge(g2, NfaState.Q2A, NfaState.P3, "5");
            drawEdge(g2, NfaState.Q2A, NfaState.P4, "1");

            // Ramas '2'
            drawEdge(g2, NfaState.Q3, NfaState.Q3A, "1");
            drawEdge(g2, NfaState.Q3A, NfaState.P5, "λ");
            drawEdge(g2, NfaState.Q3A, NfaState.P6, "5");
            drawEdge(g2, NfaState.Q4, NfaState.P7, "2");
            drawEdge(g2, NfaState.Q4, NfaState.Q4A, "5");
            drawEdge(g2, NfaState.Q4A, NfaState.P14, "λ");
            drawEdge(g2, NfaState.Q4A, NfaState.P8, "5");

            // Ramas '5'
            drawEdge(g2, NfaState.Q5, NfaState.Q5A, "5");
            drawEdge(g2, NfaState.Q5A, NfaState.P15, "λ");
            drawEdge(g2, NfaState.Q5A, NfaState.P9, "5");
            drawEdge(g2, NfaState.Q5A, NfaState.P10, "1");
            drawEdge(g2, NfaState.Q6, NfaState.Q6A, "1");
            drawEdge(g2, NfaState.Q6A, NfaState.P11, "5");
            drawEdge(g2, NfaState.Q6, NfaState.P12, "2");

            // Bucle en Q0
            Point p0 = nodes.get(NfaState.Q0);
            g2.setColor(new Color(200, 200, 200));
            g2.drawArc(p0.x - 15, p0.y - 35, 30, 30, 0, 180);
            g2.setColor(Color.BLUE);
            g2.setFont(new Font(FONT, Font.BOLD, 12));
            g2.drawString("0", p0.x - 4, p0.y - 38);

            // Dibujar los Nodos
            for (Map.Entry<NfaState, Point> entry : nodes.entrySet()) {
                NfaState state = entry.getKey();
                Point p = entry.getValue();
                boolean isActive = currentActiveStates.contains(state);

                // Nodos ajustados en tamaño para caber los 28
                int size = isActive ? 34 : 30;
                int offset = size / 2;

                Color targetColor = Color.WHITE;
                if (isActive) {
                    targetColor = new Color(46, 204, 113);
                } else if (state.isAcceptance()) {
                    targetColor = new Color(214, 234, 248);
                }

                g2.setColor(targetColor);
                g2.fillOval(p.x - offset, p.y - offset, size, size);

                g2.setColor(isActive ? new Color(39, 174, 96) : Color.GRAY);
                g2.drawOval(p.x - offset, p.y - offset, size, size);

                g2.setColor(isActive ? Color.WHITE : Color.BLACK);
                g2.setFont(new Font(FONT, isActive ? Font.BOLD : Font.PLAIN, 11));
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(state.name());
                g2.drawString(state.name(), p.x - (textWidth / 2), p.y + 4);
            }
        }

        private void drawEdge(Graphics2D g2, NfaState s1, NfaState s2, String label) {
            Point p1 = nodes.get(s1);
            Point p2 = nodes.get(s2);
            if (p1 != null && p2 != null) {
                g2.setColor(new Color(200, 200, 200));
                g2.drawLine(p1.x, p1.y + 15, p2.x, p2.y - 15);

                int textX = (p1.x + p2.x) / 2;
                int textY = (p1.y + p2.y) / 2;

                g2.setColor(Color.WHITE);
                g2.fillOval(textX - 8, textY - 10, 16, 16);

                g2.setColor(Color.BLUE);
                g2.setFont(new Font(FONT, Font.BOLD, 10));
                g2.drawString(label, textX - 3, textY + 3);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VendingMachineApp().setVisible(true));
    }
}