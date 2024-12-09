package project;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

class Process2 {
    String name;
    int arrivalTime, burstTime, priority, remainingTime, quantum;
    double FCAIFactor;

    public Process2(String name, int arrivalTime, int burstTime, int priority, int quantum) {
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.remainingTime = burstTime;
        this.quantum = quantum;
    }
}

public class FCAIScheduler {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    static void createAndShowGUI() {
        JFrame frame = new JFrame("FCAI Scheduler Input");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        JPanel initialPanel = new JPanel();
        initialPanel.setLayout(new BorderLayout());

        JLabel promptLabel = new JLabel("Enter the number of processes:", SwingConstants.CENTER);
        JTextField numProcessesField = new JTextField();
        JButton proceedButton = new JButton("Proceed");

        initialPanel.add(promptLabel, BorderLayout.NORTH);
        initialPanel.add(numProcessesField, BorderLayout.CENTER);
        initialPanel.add(proceedButton, BorderLayout.SOUTH);

        frame.add(initialPanel);
        frame.setVisible(true);

        proceedButton.addActionListener(e -> {
            String input = numProcessesField.getText();
            try {
                int numProcesses = Integer.parseInt(input);
                if (numProcesses <= 0) {
                    JOptionPane.showMessageDialog(frame, "Please enter a valid positive number!");
                    return;
                }

                frame.remove(initialPanel); // Remove initial panel
                showProcessInputPanel(frame, numProcesses); // Proceed to process input panel
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter a valid number!");
            }
        });
    }

    static void showProcessInputPanel(JFrame frame, int numProcesses) {

        JTextArea resultsArea = new JTextArea(10, 50);
        resultsArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultsArea);


        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(6, 2, 3, 3));

        // Input fields
        JTextField nameField = new JTextField();
        JTextField arrivalField = new JTextField();
        JTextField burstField = new JTextField();
        JTextField priorityField = new JTextField();
        JTextField quantumField = new JTextField();

        // Labels
        inputPanel.add(new JLabel("Process Name:"));
        inputPanel.add(nameField);

        inputPanel.add(new JLabel("Arrival Time:"));
        inputPanel.add(arrivalField);

        inputPanel.add(new JLabel("Burst Time:"));
        inputPanel.add(burstField);

        inputPanel.add(new JLabel("Priority:"));
        inputPanel.add(priorityField);

        inputPanel.add(new JLabel("Quantum:"));
        inputPanel.add(quantumField);

        JButton addButton = new JButton("Add Process");
        JButton submitButton = new JButton("Submit");

        inputPanel.add(addButton);
        inputPanel.add(submitButton);

        frame.add(inputPanel, BorderLayout.CENTER);

        List<Process2> processes = new ArrayList<>();

        // Add Process Button Action
        addButton.addActionListener(e -> {
            if (processes.size() >= numProcesses) {
                JOptionPane.showMessageDialog(frame, "You have already added all processes!");
                return;
            }

            try {
                String name = nameField.getText();
                int arrivalTime = Integer.parseInt(arrivalField.getText());
                int burstTime = Integer.parseInt(burstField.getText());
                int priority = Integer.parseInt(priorityField.getText());
                int quantum = Integer.parseInt(quantumField.getText());

                processes.add(new Process2(name, arrivalTime, burstTime, priority, quantum));
//                JOptionPane.showMessageDialog(frame, "Process Added!");

                nameField.setText("");
                arrivalField.setText("");
                burstField.setText("");
                priorityField.setText("");
                quantumField.setText("");

                if (processes.size() == numProcesses) {
                    JOptionPane.showMessageDialog(frame, "All processes added. You can now submit.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter valid numeric values!");
            }
        });

        submitButton.addActionListener(e -> {
            if (processes.size() < numProcesses) {
                JOptionPane.showMessageDialog(frame, "Please add all processes before submitting!");
                return;
            }

            List<String> executionOrder = fcaiScheduling(processes);

            Map<String, List<int[]>> executionPeriods = analyzeExecutionOrder(executionOrder);

            Map<String, Color> processColors = new HashMap<>();
            for (String process : executionPeriods.keySet()) {
                Color processColor = JColorChooser.showDialog(null, "Choose color for " + process, Color.GRAY);
                processColors.put(process, processColor);
            }

            double totalWaitingTime = 0, totalTurnaroundTime = 0;
            for (String process : executionPeriods.keySet()) {
                totalWaitingTime += processes.stream().filter(p -> p.name.equals(process)).mapToDouble(p -> p.arrivalTime).sum();
                totalTurnaroundTime += processes.stream().filter(p -> p.name.equals(process)).mapToDouble(p -> p.burstTime).sum();
            }

            double avgWaitingTime = totalWaitingTime / processes.size();
            double avgTurnaroundTime = totalTurnaroundTime / processes.size();

            showTimeline(executionPeriods, processColors);
        });


        frame.revalidate();
        frame.repaint();
    }

    static int calculateFCAI(Process2 p, double V1, double V2) {
        return (int) ((10 - p.priority) + (Math.ceil(p.arrivalTime / V1)) + (Math.ceil(p.remainingTime / V2)));
    }

    static double calculateV1(List<Process2> processes) {
        int maxArrivalTime = processes.stream().mapToInt(p -> p.arrivalTime).max().orElse(0);
        return maxArrivalTime / 10.0;
    }

    static double calculateV2(List<Process2> processes) {
        int maxBurstTime = processes.stream().mapToInt(p -> p.burstTime).max().orElse(0);
        return maxBurstTime / 10.0;
    }

    static void sortFactorQueue(List<Process2> factorQueue) {
        factorQueue.sort(Comparator.comparingDouble(p -> p.FCAIFactor));
    }

    static void updateFCAIFactors(List<Process2> factorQueue, double V1, double V2) {
        for (Process2 p : factorQueue) {
            p.FCAIFactor = calculateFCAI(p, V1, V2);
        }
        sortFactorQueue(factorQueue);
    }


    static double avg1 = 0 , avg2 = 0;

    static List<String> fcaiScheduling(List<Process2> processes) {
        double V1 = calculateV1(processes);
        double V2 = calculateV2(processes);

        Queue<Process2> readyQueue = new LinkedList<>();
        List<Process2> factorQueue = new ArrayList<>();
        List<String> executionOrder = new ArrayList<>();
        Map<String, List<Integer>> quantumHistory = new HashMap<>();
        Map<String, Integer> waitingTimes = new HashMap<>();
        Map<String, Integer> turnaroundTimes = new HashMap<>();

        int currentTime = 0;
        boolean preempted = false;

        while (!processes.isEmpty() || !readyQueue.isEmpty()) {
            for (Iterator<Process2> it = processes.iterator(); it.hasNext(); ) {
                Process2 p = it.next();
                if (p.arrivalTime <= currentTime) {
                    p.FCAIFactor = calculateFCAI(p, V1, V2);
                    readyQueue.add(p);
                    factorQueue.add(p);
                    quantumHistory.putIfAbsent(p.name, new ArrayList<>());
                    waitingTimes.putIfAbsent(p.name, 0);
                    turnaroundTimes.putIfAbsent(p.name, 0);
                    it.remove();
                }
            }

            updateFCAIFactors(factorQueue, V1, V2);

            if (!readyQueue.isEmpty() || !factorQueue.isEmpty()) {
                Process2 currentProcess;

                if (!factorQueue.isEmpty() && preempted) {
                    currentProcess = factorQueue.remove(0);
                    readyQueue.remove(currentProcess);
                } else {
                    currentProcess = readyQueue.poll();
                    factorQueue.remove(currentProcess);
                }

                int executeTime = 0;
                preempted = false;

                for (int i = 1; i <= currentProcess.quantum; i++) {
                    currentTime++;
                    currentProcess.remainingTime--;
                    executeTime++;
                    executionOrder.add(currentProcess.name);

                    for (Iterator<Process2> it = processes.iterator(); it.hasNext(); ) {
                        Process2 p = it.next();
                        if (p.arrivalTime <= currentTime) {
                            p.FCAIFactor = calculateFCAI(p, V1, V2);
                            readyQueue.add(p);
                            factorQueue.add(p);
                            quantumHistory.putIfAbsent(p.name, new ArrayList<>());
                            waitingTimes.putIfAbsent(p.name, 0);
                            turnaroundTimes.putIfAbsent(p.name, 0);
                            it.remove();
                        }
                    }

                    if (i >= Math.ceil(0.4 * currentProcess.quantum)) {
                        updateFCAIFactors(factorQueue, V1, V2);

                        if (!factorQueue.isEmpty() && factorQueue.get(0).FCAIFactor < currentProcess.FCAIFactor) {
                            readyQueue.add(currentProcess);
                            factorQueue.add(currentProcess);

                            preempted = true;
                            break;
                        }
                    }

                    if (currentProcess.remainingTime == 0) {
                        int tat = currentTime - currentProcess.arrivalTime;
                        turnaroundTimes.put(currentProcess.name, tat);
                        int wt = tat - currentProcess.burstTime;
                        waitingTimes.put(currentProcess.name, wt);
                        System.out.println("Process " + currentProcess.name + " completed at time " + currentTime);
                        readyQueue.remove(currentProcess);
                        factorQueue.remove(currentProcess);
                        updateFCAIFactors(factorQueue, V1, V2);
                        break;
                    }
                }

                if (currentProcess.remainingTime > 0 && !preempted) {
                    currentProcess.quantum += 2;
                    readyQueue.add(currentProcess);
                    factorQueue.add(currentProcess);
                } else if (preempted) {
                    currentProcess.quantum += (currentProcess.quantum - executeTime); // Update quantum on preemption
                }
                quantumHistory.get(currentProcess.name).add(currentProcess.quantum);
                updateFCAIFactors(factorQueue, V1, V2);
            }
        }
        double totalWaitingTime = 0, totalTurnaroundTime = 0;
        for (String process : waitingTimes.keySet()) {
            totalWaitingTime += waitingTimes.get(process);
            totalTurnaroundTime += turnaroundTimes.get(process);
        }


        avg1 = totalWaitingTime / waitingTimes.size();
        avg2 =  totalTurnaroundTime / turnaroundTimes.size();

        System.out.println("\nExecution Order: " + String.join(" -> ", executionOrder));
        System.out.println("Waiting Times: " + waitingTimes);
        System.out.println("Turnaround Times: " + turnaroundTimes);
        System.out.printf("Average Waiting Time: %.2f%n", totalWaitingTime / waitingTimes.size());
        System.out.printf("Average Turnaround Time: %.2f%n", totalTurnaroundTime / turnaroundTimes.size());
        System.out.println("\nQuantum History:");
        for (String process : quantumHistory.keySet()) {
            System.out.println(process + ": " + quantumHistory.get(process));
        }

        return executionOrder;
    }

    static Map<String, List<int[]>> analyzeExecutionOrder(List<String> executionOrder) {
        Map<String, List<int[]>> executionPeriods = new LinkedHashMap<>();
        String currentProcess = executionOrder.get(0);
        int start = 0;

        for (int i = 1; i < executionOrder.size(); i++) {
            if (!executionOrder.get(i).equals(currentProcess)) {
                executionPeriods.putIfAbsent(currentProcess, new ArrayList<>());
                executionPeriods.get(currentProcess).add(new int[]{start, i});
                currentProcess = executionOrder.get(i);
                start = i;
            }
        }

        executionPeriods.putIfAbsent(currentProcess, new ArrayList<>());
        executionPeriods.get(currentProcess).add(new int[]{start, executionOrder.size()});

        return executionPeriods;
    }

    static void showTimeline(Map<String, List<int[]>> executionPeriods, Map<String, Color> processColors) {


        double avgWaitingTime = avg1;
        double avgTurnaroundTime = avg2;

        JFrame frame = new JFrame("Gantt Chart");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                int x = 50, y = 50, height = 50; // Starting position and dimensions of the timeline bars

                for (String process : executionPeriods.keySet()) {
                    g.setColor(processColors.getOrDefault(process, Color.GRAY));
                    for (int[] period : executionPeriods.get(process)) {
                        int start = period[0] * 10;
                        int width = (period[1] - period[0]) * 10;
                        g.fillRect(x + start, y, width, height);

                        g.setColor(processColors.getOrDefault(process, Color.GRAY));
                        g.fillRect(x + start, y, width, height);

                        g.setColor(Color.BLACK);
                        g.drawRect(x + start, y, width, height);

                        g.drawString(process + " (" + period[0] + "-" + period[1] + ")", x + start + 5, y + height + 20);
                    }
                    y += height + 20;
                }

                g.setColor(Color.BLACK);
                y += 30; // Move further down below the chart
                g.drawString("Average Waiting Time: " + String.format("%.2f", avgWaitingTime), x, y);

                y += 20; // Move down for the next line
                g.drawString("Average Turnaround Time: " + String.format("%.2f", avgTurnaroundTime), x, y);
            }
        };

        frame.add(panel);
        frame.setVisible(true);
    }

}
