package org.os;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

class Process {
    int id, arrivalTime, burstTime, remainingTime, completionTime, waitingTime, turnaroundTime, priority, startTime;

    Process(int id, int arrivalTime, int burstTime) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.priority = 0;
    }
}

public class SRTFScheduler {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JTextField processCountField;
    private List<String> ganttChart;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SRTFScheduler::new);
    }

    public SRTFScheduler() {
        ganttChart = new ArrayList<>();
        frame = new JFrame("SRTF Scheduler");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.LIGHT_GRAY);

        
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

        
        model = new DefaultTableModel(new String[]{"Process ID", "Arrival Time", "Burst Time"}, 0);
        table = new JTable(model);
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
            model.setRowCount(0); 
            for (int i = 0; i < processCount; i++) {
                model.addRow(new Object[]{i + 1, "", ""});
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid number of processes.");
        }
    }

    private void runScheduler() {
        List<Process> processes = new ArrayList<>();

        
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

        
        srtfScheduling(processes);

        
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

    private void srtfScheduling(List<Process> processes) {
        int time = 0;
        int completed = 0;
        int n = processes.size();

        Process currentProcess = null;
        Queue<Process> readyQueue = new LinkedList<>();

        while (completed < n) {
            
            for (Process p : processes) {
                if (p.arrivalTime == time) {
                    readyQueue.add(p);
                }
            }

            
            for (Process p : readyQueue) {
                if (p != currentProcess) {
                    p.priority++;
                }
            }

            
            readyQueue = new LinkedList<>(readyQueue.stream()
                    .sorted(Comparator.comparingInt((Process p) -> p.remainingTime)
                            .thenComparing(p -> -p.priority)) 
                    .collect(Collectors.toList()));

            
            if (currentProcess == null || currentProcess.remainingTime == 0) {
                if (!readyQueue.isEmpty()) {
                    if (currentProcess != null) {
                        ganttChart.add("P" + currentProcess.id);
                    }
                    currentProcess = readyQueue.poll();
                }
            } else if (!readyQueue.isEmpty() && readyQueue.peek().remainingTime < currentProcess.remainingTime) {
                readyQueue.add(currentProcess);
                ganttChart.add("P" + currentProcess.id);
                currentProcess = readyQueue.poll();
            }

            
            if (currentProcess != null) {
                currentProcess.remainingTime--;
                if (currentProcess.remainingTime == 0) {
                    completed++;
                    currentProcess.completionTime = time + 1;
                    currentProcess.turnaroundTime = currentProcess.completionTime - currentProcess.arrivalTime;
                    currentProcess.waitingTime = currentProcess.turnaroundTime - currentProcess.burstTime;
                }
            }

            time++;
        }

        
        if (currentProcess != null) {
            ganttChart.add("P" + currentProcess.id);
        }
    }
}







