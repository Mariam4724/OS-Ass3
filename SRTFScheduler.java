package org.os;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;

class Process {
    String name;
    Color color;
    int id, arrivalTime, burstTime, remainingTime, completionTime, waitingTime, turnaroundTime;

    Process(String name, Color color, int id, int arrivalTime, int burstTime) {
        this.name = name;
        this.color = color;
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
    }
}

public class SRTFScheduler {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JTextField processCountField;
    private JTextField contextSwitchField; // Field for context switching time
    private List<String> ganttChart;
    private List<Process> processes;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SRTFScheduler::new);
    }

    public SRTFScheduler() {
        ganttChart = new ArrayList<>();
        processes = new ArrayList<>();
        frame = new JFrame("SRTF Scheduler");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.LIGHT_GRAY);

        // Input panel
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.setBackground(Color.DARK_GRAY);

        JLabel label = new JLabel("Enter Number of Processes: ");
        label.setForeground(Color.WHITE);
        processCountField = new JTextField(5);

        JLabel contextSwitchLabel = new JLabel("Context Switching Time: ");
        contextSwitchLabel.setForeground(Color.WHITE);
        contextSwitchField = new JTextField(5);

        JButton generateButton = new JButton("Generate Table");
        generateButton.setBackground(Color.WHITE);
        generateButton.addActionListener(e -> generateProcessTable());

        inputPanel.add(label);
        inputPanel.add(processCountField);
        inputPanel.add(contextSwitchLabel);
        inputPanel.add(contextSwitchField);
        inputPanel.add(generateButton);

        // Table panel
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
                model.addRow(new Object[]{"Process " + (i + 1), Color.BLACK, "", ""});
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid number of processes.");
        }
    }

    private void runScheduler() {
        processes.clear();

        // Validate and collect input
        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                String name = model.getValueAt(i, 0).toString();
                Color color = JColorChooser.showDialog(frame, "Choose Color for " + name, Color.BLACK);
                int arrivalTime = Integer.parseInt(model.getValueAt(i, 2).toString());
                int burstTime = Integer.parseInt(model.getValueAt(i, 3).toString());
                processes.add(new Process(name, color, i + 1, arrivalTime, burstTime));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Please fill all fields correctly for Process " + (i + 1));
                return;
            }
        }

        // Get context switching time
        int contextSwitchTime;
        try {
            contextSwitchTime = Integer.parseInt(contextSwitchField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid context switching time.");
            return;
        }

        // Run SRTF scheduling
        srtfScheduling(processes, contextSwitchTime, 20);

        // Display results
        DefaultTableModel resultModel = new DefaultTableModel(
                new String[]{"Process Name", "Arrival Time", "Burst Time", "Waiting Time", "Turnaround Time"}, 0);
        double totalWaitingTime = 0;
        double totalTurnaroundTime = 0;
        for (Process p : processes) {
            resultModel.addRow(new Object[]{
                    p.name, p.arrivalTime, p.burstTime, p.waitingTime, p.turnaroundTime
            });
            totalWaitingTime += p.waitingTime;
            totalTurnaroundTime += p.turnaroundTime;
        }

        double averageWaitingTime = totalWaitingTime / processes.size();
        double averageTurnaroundTime = totalTurnaroundTime / processes.size();

        JTable resultTable = new JTable(resultModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                int modelRow = convertRowIndexToModel(row);
                Process process = processes.get(modelRow);
                component.setBackground(process.color);
                return component;
            }
        };
        resultTable.setRowHeight(30);
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        resultTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        resultTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        JScrollPane resultScroll = new JScrollPane(resultTable);

        JPanel resultPanel = new JPanel(new BorderLayout(10, 10));
        resultPanel.add(resultScroll, BorderLayout.CENTER);

        JLabel ganttLabel = new JLabel("Gantt Chart:");
        ganttLabel.setFont(new Font("Arial", Font.BOLD, 14));
        ganttLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel ganttPanel = new JPanel();
        ganttPanel.setLayout(new FlowLayout());

        // Define a scaling factor for burst time to pixel width
        int scaleFactor = 10; // Adjust this value as needed for better visualization

        for (String processName : ganttChart) {
            Process process = processes.stream()
                    .filter(p -> p.name.equals(processName))
                    .findFirst()
                    .orElse(null);
            if (process != null) {
                JPanel processPanel = new JPanel();
                processPanel.setBackground(process.color);
                processPanel.setPreferredSize(new Dimension(process.burstTime * scaleFactor, 50)); // Width based on burst time
                ganttPanel.add(processPanel);
                ganttPanel.add(new JLabel(process.name));
            }
        }

        JLabel avgWaitingLabel = new JLabel("Average Waiting Time: " + averageWaitingTime);
        JLabel avgTurnaroundLabel = new JLabel("Average Turnaround Time: " + averageTurnaroundTime);

        resultPanel.add(ganttLabel, BorderLayout.NORTH);
        resultPanel.add(ganttPanel, BorderLayout.SOUTH);
        resultPanel.add(avgWaitingLabel, BorderLayout.WEST);
        resultPanel.add(avgTurnaroundLabel, BorderLayout.EAST);

        JOptionPane.showMessageDialog(frame, resultPanel, "Scheduling Results", JOptionPane.INFORMATION_MESSAGE);
    }

    private void srtfScheduling(List<Process> processes, int contextSwitchTime, int agingThreshold) {
        int currentTime = 0;
        int completedProcesses = 0;
        int n = processes.size();
        boolean[] isCompleted = new boolean[n];
        boolean firstProcessExecuted = false; // Flag to skip context switch for the first process
        ganttChart.clear();
        String lastExecutedProcess = "";

        while (completedProcesses < n) {
            // Aging: Adjust remaining times for processes waiting longer than the threshold
            for (int i = 0; i < n; i++) {
                if (!isCompleted[i] && processes.get(i).arrivalTime <= currentTime &&
                        (currentTime - processes.get(i).arrivalTime > agingThreshold)) {
                    processes.get(i).remainingTime--; // Boost priority by artificially reducing remaining time
                }
            }

            // Find the process with the shortest remaining time
            int idx = -1;
            int minRemainingTime = Integer.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                if (processes.get(i).arrivalTime <= currentTime && !isCompleted[i] && processes.get(i).remainingTime < minRemainingTime) {
                    minRemainingTime = processes.get(i).remainingTime;
                    idx = i;
                }
            }

            if (idx != -1) {
                if (!lastExecutedProcess.equals(processes.get(idx).name)) {
                    if (!firstProcessExecuted) {
                        // Skip context switch for the first process
                        firstProcessExecuted = true;
                    } else if (!lastExecutedProcess.isEmpty()) {
                        currentTime += contextSwitchTime; // Apply context switch time for subsequent processes
                        ganttChart.add("Context Switch");
                    }
                    ganttChart.add(processes.get(idx).name);
                    lastExecutedProcess = processes.get(idx).name;
                }
                processes.get(idx).remainingTime--;
                currentTime++;

                if (processes.get(idx).remainingTime == 0) {
                    processes.get(idx).completionTime = currentTime;
                    processes.get(idx).turnaroundTime = processes.get(idx).completionTime - processes.get(idx).arrivalTime;
                    processes.get(idx).waitingTime = processes.get(idx).turnaroundTime - processes.get(idx).burstTime;
                    isCompleted[idx] = true;
                    completedProcesses++;
                }
            } else {
                currentTime++;
                lastExecutedProcess = "";
            }
        }
    }


}
