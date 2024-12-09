package project;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SJFScheduler {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JTextField processCountField;
    private JTextField contextSwitchField; // New field for context switching time
    private List<String> ganttChart;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SJFScheduler::new);
    }

    public SJFScheduler() {
        ganttChart = new ArrayList<>();
        frame = new JFrame("Non-Preemptive SJF Scheduler");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.LIGHT_GRAY);

        // Input Panel
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.setBackground(Color.DARK_GRAY);

        JLabel label = new JLabel("Enter Number of Processes: ");
        label.setForeground(Color.WHITE);
        processCountField = new JTextField(5);

        JLabel contextSwitchLabel = new JLabel("Context Switching Time: ");
        contextSwitchLabel.setForeground(Color.WHITE);
        contextSwitchField = new JTextField(5); // Initialize context switch field

        JButton generateButton = new JButton("Generate Table");
        generateButton.setBackground(Color.WHITE);
        generateButton.addActionListener(e -> generateProcessTable());

        inputPanel.add(label);
        inputPanel.add(processCountField);
        inputPanel.add(contextSwitchLabel);
        inputPanel.add(contextSwitchField); // Add context switch field to input panel
        inputPanel.add(generateButton);

        // Table Panel
        model = new DefaultTableModel(new String[]{"Process Name", "Color", "Arrival Time", "Burst Time"}, 0);
        table = new JTable(model);
        JScrollPane tableScroll = new JScrollPane(table);

        // Run Scheduler Button
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
            model.setRowCount(0); // Clear the table
            for (int i = 0; i < processCount; i++) {
                model.addRow(new Object[]{"P" + (i + 1), Color.WHITE, "", ""});
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid number of processes.");
        }
    }

    private void runScheduler() {
        List<Process> processes = new ArrayList<>();
        int contextSwitchTime;

        // Validate and collect input
        try {
            contextSwitchTime = Integer.parseInt(contextSwitchField.getText()); // Get context switch time
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid context switching time.");
            return;
        }

        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                String name = model.getValueAt(i, 0).toString();
                Color color = JColorChooser.showDialog(frame, "Choose Color for " + name, Color.BLACK);
                int arrivalTime = Integer.parseInt(model.getValueAt(i, 2).toString());
                int burstTime = Integer.parseInt(model.getValueAt(i, 3).toString());
                processes.add(new Process(name, color, arrivalTime, burstTime));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Please fill all fields correctly for Process " + (i + 1));
                return;
            }
        }

        // Run SJF scheduling
        sjfScheduling(processes, contextSwitchTime);

        // Display results DefaultTableModel resultModel = new DefaultTableModel(
        DefaultTableModel resultModel = new DefaultTableModel(
                new String[]{"Process Name", "Arrival Time", "Burst Time", "Waiting Time", "Turnaround Time"}, 0);
        int totalWaitingTime = 0;
        int totalTurnaroundTime = 0;

        for (Process p : processes) {
            resultModel.addRow(new Object[]{
                    p.name, p.arrivalTime, p.burstTime, p.waitingTime, p.turnaroundTime
            });
            totalWaitingTime += p.waitingTime;
            totalTurnaroundTime += p.turnaroundTime;
        }

        double averageWaitingTime = (double) totalWaitingTime / processes.size();
        double averageTurnaroundTime = (double) totalTurnaroundTime / processes.size();

        JTable resultTable = new JTable(resultModel);
        resultTable.setDefaultRenderer(Object.class, new ProcessColorRenderer(processes));

        JScrollPane resultScroll = new JScrollPane(resultTable);

        JPanel resultPanel = new JPanel(new BorderLayout(10, 10));
        resultPanel.add(resultScroll, BorderLayout.CENTER);

        // Create a panel for averages
        JPanel averagesPanel = new JPanel(new GridLayout(2, 1));
        averagesPanel.add(new JLabel("Average Waiting Time: " + String.format("%.2f", averageWaitingTime)));
        averagesPanel.add(new JLabel("Average Turnaround Time: " + String.format("%.2f", averageTurnaroundTime)));

        // Add averages panel to the left and right of the result table
        JPanel sidePanel = new JPanel(new GridLayout(1, 2));
        sidePanel.add(averagesPanel);
        resultPanel.add(sidePanel, BorderLayout.WEST);
        resultPanel.add(sidePanel, BorderLayout.EAST);

        // Gantt Chart Panel
        JPanel ganttPanel = new JPanel();
        ganttPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        for (String process : ganttChart) {
            JLabel ganttBlock = new JLabel(process);
            ganttBlock.setOpaque(true);
            ganttBlock.setBackground(getProcessColor(processes, process));
            ganttBlock.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            ganttBlock.setPreferredSize(new Dimension(50, 30));
            ganttPanel.add(ganttBlock);
        }

        resultPanel.add(ganttPanel, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(frame, resultPanel, "Scheduling Results", JOptionPane.INFORMATION_MESSAGE);
    }
    private void sjfScheduling(List<Process> processes, int contextSwitchTime) {
        int time = 0;
        int completed = 0;
        int n = processes.size();
        boolean[] isCompleted = new boolean[n];
        boolean isFirstProcess = true; // Flag to ignore context-switching time for the first process

        while (completed < n) {
            List<Process> readyQueue = new ArrayList<>();
            Process priorityProcess = null;

            for (int i = 0; i < n; i++) {
                if (!isCompleted[i] && processes.get(i).arrivalTime <= time) {
                    Process p = processes.get(i);
                    readyQueue.add(p);

                    int waitingTime = time - p.arrivalTime;
                    if (waitingTime > 10 && (priorityProcess == null || waitingTime > (time - priorityProcess.arrivalTime))) {
                        priorityProcess = p;
                    }
                }
            }

            // If a priority process with waiting time > 20 is found, execute it directly
            if (priorityProcess != null) {
                readyQueue.clear();
                readyQueue.add(priorityProcess);
            }

            // Sort readyQueue by burst time
            readyQueue.sort(Comparator.comparingInt(p -> p.burstTime));

            if (!readyQueue.isEmpty()) {
                Process currentProcess = readyQueue.get(0);
                ganttChart.add(currentProcess.name);

                // Simulate execution of the process
                if (isFirstProcess) {
                    time += currentProcess.burstTime; // Ignore context-switching time for the first process
                    isFirstProcess = false;
                } else {
                    time += currentProcess.burstTime + contextSwitchTime; // Include context-switching time
                }

                currentProcess.completionTime = time;
                currentProcess.turnaroundTime = currentProcess.completionTime - currentProcess.arrivalTime;
                currentProcess.waitingTime = currentProcess.turnaroundTime - currentProcess.burstTime;

                isCompleted[currentProcess.id - 1] = true;
                completed++;
            } else {
                time++; // No process is ready; increment time
            }
        }
    }




    private class ProcessColorRenderer extends DefaultTableCellRenderer {
        private final List<Process> processes;

        public ProcessColorRenderer(List<Process> processes) {
            this.processes = processes;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String processName = (String) table.getValueAt(row, 0);
            Color processColor = getProcessColor(processes, processName);
            cell.setBackground(processColor);
            return cell;
        }
    }

    private Color getProcessColor(List<Process> processes, String processName) {
        for (Process p : processes) {
            if (p.name.equals(processName)) {
                return p.color;
            }
        }
        return Color.WHITE; // Default color if not found
    }

    static class Process {
        String name;
        Color color;
        int id;
        int arrivalTime;
        int burstTime;
        int completionTime;
        int turnaroundTime;
        int waitingTime;

        Process(String name, Color color, int arrivalTime, int burstTime) {
            this.name = name;
            this.color = color;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.id = Integer.parseInt(name.substring(1)); // Extract ID from name
        }
    }
}
