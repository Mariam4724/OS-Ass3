import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

class Process {
    String name;
    String color;
    int arrivalTime;
    int burstTime;
    int priority;
    int remainingTime;
    int startTime;
    int endTime;
    int waitingTime;
    int turnaroundTime;

    public Process(String name, String color, int arrivalTime, int burstTime, int priority) {
        this.name = name;
        this.color = color;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.remainingTime = burstTime;
    }
}

public class PriorityScheduler {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JTextField processCountField, contextSwitchField;
    private List<Process> processes;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PriorityScheduler::new);
    }

    public PriorityScheduler() {
        processes = new ArrayList<>();

        frame = new JFrame("Non-preemptive Priority Scheduler");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.LIGHT_GRAY);

        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.setBackground(Color.DARK_GRAY);

        JLabel countLabel = new JLabel("Enter Number of Processes: ");
        countLabel.setForeground(Color.WHITE);
        processCountField = new JTextField(5);

        JLabel contextSwitchLabel = new JLabel("Enter Context Switching Time: ");
        contextSwitchLabel.setForeground(Color.WHITE);
        contextSwitchField = new JTextField(5);

        JButton generateButton = new JButton("Generate Table");
        generateButton.setBackground(Color.WHITE);
        generateButton.addActionListener(e -> generateProcessTable());

        inputPanel.add(countLabel);
        inputPanel.add(processCountField);
        inputPanel.add(contextSwitchLabel);
        inputPanel.add(contextSwitchField);
        inputPanel.add(generateButton);

        model = new DefaultTableModel(new String[]{"Process Name", "Color", "Arrival Time", "Burst Time", "Priority"}, 0);
        table = new JTable(model);

        // Set custom editor for "Color" column
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JTextField()) {
            private JButton colorButton = new JButton("Choose Color");
            private Color currentColor;

            {
                colorButton.addActionListener(e -> {
                    Color selectedColor = JColorChooser.showDialog(frame, "Select a Process Color", currentColor);
                    if (selectedColor != null) {
                        currentColor = selectedColor;
                        String hexColor = String.format("#%06x", selectedColor.getRGB() & 0xFFFFFF);
                        table.setValueAt(hexColor, table.getSelectedRow(), 1);
                    }
                });
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                currentColor = value != null ? Color.decode(value.toString()) : Color.WHITE;
                return colorButton;
            }

            @Override
            public Object getCellEditorValue() {
                return String.format("#%06x", currentColor.getRGB() & 0xFFFFFF);
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);

        JButton runButton = new JButton("Run Scheduler");
        runButton.setBackground(Color.WHITE);
        runButton.addActionListener(e -> runScheduler());

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(tableScroll, BorderLayout.CENTER);
        mainPanel.add(runButton, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private void generateProcessTable() {
        try {
            int processCount = Integer.parseInt(processCountField.getText());
            model.setRowCount(0); // Clear existing rows
            for (int i = 0; i < processCount; i++) {
                model.addRow(new Object[]{"", "#FFFFFF", "", "", ""});
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid number of processes.");
        }
    }

    private void runScheduler() {
        processes.clear();
        try {
            int contextSwitch = Integer.parseInt(contextSwitchField.getText());

            for (int i = 0; i < model.getRowCount(); i++) {
                String name = model.getValueAt(i, 0).toString().trim();
                String color = model.getValueAt(i, 1).toString().trim();
                String arrivalTimeStr = model.getValueAt(i, 2).toString().trim();
                String burstTimeStr = model.getValueAt(i, 3).toString().trim();
                String priorityStr = model.getValueAt(i, 4).toString().trim();

                if (name.isEmpty() || color.isEmpty() || arrivalTimeStr.isEmpty() || burstTimeStr.isEmpty() || priorityStr.isEmpty()) {
                    throw new Exception("All fields must be filled for Process " + (i + 1));
                }

                int arrivalTime = Integer.parseInt(arrivalTimeStr);
                int burstTime = Integer.parseInt(burstTimeStr);
                int priority = Integer.parseInt(priorityStr);

                processes.add(new Process(name, color, arrivalTime, burstTime, priority));
            }

            nonPreemptivePriorityScheduling(contextSwitch);
            drawGanttChart(contextSwitch);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please ensure that Arrival Time, Burst Time, and Priority are valid integers.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        }
    }

    private void nonPreemptivePriorityScheduling(int contextSwitch) {
        processes.sort(Comparator.comparingInt((Process p) -> p.arrivalTime).thenComparingInt(p -> p.priority));
        int time = 0;

        for (Process current : processes) {
            if (current.arrivalTime > time) {
                time = current.arrivalTime;
            }
            current.startTime = time;
            time += current.burstTime;
            current.endTime = time;
            current.turnaroundTime = current.endTime - current.arrivalTime;
            current.waitingTime = current.startTime - current.arrivalTime;
            time += contextSwitch;
        }
    }

    private void drawGanttChart(int contextSwitch) {
        JFrame ganttFrame = new JFrame("Gantt Chart");
        ganttFrame.setSize(1000, 400);
        ganttFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                int x = 50;
                int y = 100;
                int height = 50;
                int widthUnit = 20;

                for (Process p : processes) {
                    int width = (p.burstTime * widthUnit);
                    g.setColor(Color.decode(p.color));
                    g.fillRect(x, y, width, height);
                    g.setColor(Color.BLACK);
                    g.drawRect(x, y, width, height);
                    g.drawString(p.name + " (" + p.startTime + "-" + p.endTime + ")", x + 5, y - 5);
                    x += width + (contextSwitch * widthUnit);
                }

                double averageWaitingTime = processes.stream().mapToInt(p -> p.waitingTime).average().orElse(0);
                double averageTurnaroundTime = processes.stream().mapToInt(p -> p.turnaroundTime).average().orElse(0);

                g.setColor(Color.BLACK);
                g.drawString("Average Waiting Time: " + averageWaitingTime, 50, y + 100);
                g.drawString("Average Turnaround Time: " + averageTurnaroundTime, 50, y + 120);
            }
        };

        chartPanel.setBackground(Color.WHITE);
        ganttFrame.add(chartPanel);
        ganttFrame.setVisible(true);
    }
}
