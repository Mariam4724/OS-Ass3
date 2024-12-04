package org.os;

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

        JButton generateButton = new JButton("Generate Table");
        generateButton.setBackground(Color.WHITE);
        generateButton.addActionListener(e -> generateProcessTable());

        inputPanel.add(label);
        inputPanel.add(processCountField);
        inputPanel.add(generateButton);

        // Table Panel
        model = new DefaultTableModel(new String[]{"Process ID", "Arrival Time", "Burst Time"}, 0);
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
                model.addRow(new Object[]{i + 1, "", ""});
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid number of processes.");
        }
    }

    private void runScheduler() {
        List<Process> processes = new ArrayList<>();

        // Validate and collect input
        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                int id = Integer.parseInt(model.getValueAt(i, 0).toString());
                int arrivalTime = Integer.parseInt(model.getValueAt(i, 1).toString());
                int burstTime = Integer.parseInt(model.getValueAt(i, 2).toString());
                processes.add(new Process(id, arrivalTime, burstTime));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Please fill all fields correctly for Process " + (i + 1));
                return;
            }
        }

        // Run SJF scheduling
        sjfScheduling(processes);

        // Display results
        DefaultTableModel resultModel = new DefaultTableModel(
                new String[]{"Process ID", "Arrival Time", "Burst Time", "Completion Time", "Turnaround Time", "Waiting Time"}, 0);
        for (Process p : processes) {
            resultModel.addRow(new Object[]{
                    p.id, p.arrivalTime, p.burstTime, p.completionTime, p.turnaroundTime, p.waitingTime
            });
        }

        JTable resultTable = new JTable(resultModel);
        JScrollPane resultScroll = new JScrollPane(resultTable);

        JPanel resultPanel = new JPanel(new BorderLayout(10, 10));
        resultPanel.add(resultScroll, BorderLayout.CENTER);

        JLabel ganttLabel = new JLabel("Gantt Chart:");
        ganttLabel.setFont(new Font("Arial", Font.BOLD, 14));
        ganttLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel ganttOutput = new JLabel(String.join(" | ", ganttChart));
        ganttOutput.setFont(new Font("Arial", Font.PLAIN, 14));
        ganttOutput.setHorizontalAlignment(SwingConstants.CENTER);

        resultPanel.add(ganttLabel, BorderLayout.NORTH);
        resultPanel.add(ganttOutput, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(frame, resultPanel, "Scheduling Results", JOptionPane.INFORMATION_MESSAGE);
    }

    private void sjfScheduling(List<Process> processes) {
        int time = 0;
        int completed = 0;
        int n = processes.size();
        boolean[] isCompleted = new boolean[n];

        while (completed < n) {
            // Get processes that have arrived but not completed
            List<Process> readyQueue = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (!isCompleted[i] && processes.get(i).arrivalTime <= time) {
                    readyQueue.add(processes.get(i));
                }
            }

            // Apply aging to increase priorities of waiting processes
            for (Process p : readyQueue) {
                p.priority++;
            }

            // Select the process with the shortest burst time (and highest priority if tied)
            readyQueue.sort(Comparator.comparingInt((Process p) -> p.burstTime)
                    .thenComparing(p -> -p.priority));

            if (!readyQueue.isEmpty()) {
                Process currentProcess = readyQueue.get(0);

                // Execute the process
                ganttChart.add("P" + currentProcess.id); // Add to Gantt chart
                time += currentProcess.burstTime;
                currentProcess.completionTime = time;
                currentProcess.turnaroundTime = currentProcess.completionTime - currentProcess.arrivalTime;
                currentProcess.waitingTime = currentProcess.turnaroundTime - currentProcess.burstTime;

                // Mark as completed
                isCompleted[currentProcess.id - 1] = true;
                completed++;
            } else {
                // If no process is ready, move the time forward
                time++;
            }
        }
    }
}
