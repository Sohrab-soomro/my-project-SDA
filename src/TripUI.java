import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

public class TripUI extends JFrame {
    private DefaultTableModel model;
    private JTable table;
    private static ArrayList<Trip> tripList = new ArrayList<>();
    private static ArrayList<String> registrations = new ArrayList<>();
    private static ArrayList<String> pendingRequests = new ArrayList<>();
    private static ArrayList<String> approvalResults = new ArrayList<>();

    public TripUI(User currentUser) {
        loadData();
        checkApprovalNotifications(currentUser);

        setTitle("TMS Dashboard - " + currentUser.getRole());
        setSize(1100, 600);
        setLayout(new BorderLayout());

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> { new AuthUI(); this.dispose(); });
        add(logout, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"Destination", "Cost", "Date", "Seats Left", "Status"}, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel cp = new JPanel();

        if (currentUser.getRole().equals("Staff") || currentUser.getRole().equals("Teacher")) {
            JButton addT = new JButton("Add Trip"), delT = new JButton("Delete Trip"),
                    canR = new JButton("Cancel Std Reg"), rep = new JButton("Trip Report"),
                    manageU = new JButton("Manage Users"), appR = new JButton("Approve Requests"),
                    editS = new JButton("Update Status"); // Integrated Feature

            cp.add(addT); cp.add(delT); cp.add(editS); cp.add(appR); cp.add(canR); cp.add(rep); cp.add(manageU);

            // FUNCTIONAL STATUS: Manual Toggle
            editS.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(this, "Please select a trip from the table first.");
                    return;
                }
                Trip t = tripList.get(row);
                String[] options = {"Available", "Closed"};
                String newStatus = (String) JOptionPane.showInputDialog(this, "Set Status for " + t.getDestination() + ":",
                        "Update Status", 3, null, options, t.getStatus());
                if (newStatus != null) {
                    t.setStatus(newStatus);
                    saveData(); refresh();
                }
            });

            appR.addActionListener(e -> {
                if (pendingRequests.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No pending trip requests found.");
                    return;
                }
                String selection = (String) JOptionPane.showInputDialog(this, "Select a Request to Review:", "Approve Requests", 3, null, pendingRequests.toArray(), pendingRequests.get(0));
                if (selection != null) {
                    int choice = JOptionPane.showConfirmDialog(this, "Approve this request for " + selection + "?", "Decision", JOptionPane.YES_NO_OPTION);
                    String[] parts = selection.split("\\|");
                    String studentEmail = parts[0];
                    String dest = parts[1];

                    if (choice == JOptionPane.YES_OPTION) {
                        registrations.add(selection);
                        approvalResults.add(studentEmail + "|" + dest + "|Approved");
                    } else {
                        for (Trip t : tripList) {
                            if (t.getDestination().equals(dest)) {
                                t.incrementSeats();
                                // Logic: If a trip was closed because it was full, re-open it now
                                if (t.getSeats() > 0 && t.getStatus().equals("Closed")) t.setStatus("Available");
                                break;
                            }
                        }
                        approvalResults.add(studentEmail + "|" + dest + "|Declined");
                    }
                    pendingRequests.remove(selection);
                    saveData(); refresh();
                }
            });

            addT.addActionListener(e -> {
                JTextField d = new JTextField(), c = new JTextField(), dt = new JTextField(), s = new JTextField();
                JComboBox<String> st = new JComboBox<>(new String[]{"Available", "Closed"});
                Object[] msg = {"Dest:", d, "Cost:", c, "Date:", dt, "Seats:", s, "Status:", st};
                if (JOptionPane.showConfirmDialog(null, msg, "Add Trip", 2) == 0) {
                    try {
                        double tripCost = Double.parseDouble(c.getText());
                        int tripSeats = Integer.parseInt(s.getText());
                        String initStatus = (String)st.getSelectedItem();

                        if (tripCost <= 0 || tripSeats < 0) {
                            JOptionPane.showMessageDialog(this, "Cost and Seats must be non-negative!");
                            return;
                        }
                        if (tripSeats == 0) initStatus = "Closed";

                        tripList.add(new Trip(d.getText(), tripCost, tripSeats, dt.getText(), initStatus));
                        saveData(); refresh();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Error: Cost and Seats must be numbers!");
                    }
                }
            });

            delT.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(this, "Please select a trip from the table first.");
                    return;
                }
                if (JOptionPane.showConfirmDialog(this, "Delete whole trip?") == 0) {
                    String dest = (String) table.getValueAt(row, 0);
                    tripList.remove(row);
                    registrations.removeIf(r -> r.endsWith("|" + dest));
                    pendingRequests.removeIf(r -> r.endsWith("|" + dest));
                    saveData(); refresh();
                }
            });

            manageU.addActionListener(e -> {
                ArrayList<User> targets = new ArrayList<>();
                for (User u : AuthUI.userDatabase) {
                    if (u.getEmail().equalsIgnoreCase(currentUser.getEmail())) continue;
                    if (currentUser.getRole().equals("Staff")) targets.add(u);
                    else if (currentUser.getRole().equals("Teacher") && u.getRole().equals("Student")) targets.add(u);
                }
                if (targets.isEmpty()) { JOptionPane.showMessageDialog(this, "No users to manage."); return; }
                User selected = (User) JOptionPane.showInputDialog(this, "Select User:", "Manage", 3, null, targets.toArray(), targets.get(0));
                if (selected != null) {
                    String[] opts = {"Activate", "Deactivate", "Delete Account"};
                    int choice = JOptionPane.showOptionDialog(null, "Action for " + selected.getEmail(), "Admin", 0, 2, null, opts, opts[0]);
                    if (choice == 0) selected.setActive(true);
                    else if (choice == 1) selected.setActive(false);
                    else if (choice == 2) {
                        String studentEmail = selected.getEmail().toLowerCase();
                        registrations.removeIf(r -> r.startsWith(studentEmail + "|"));
                        pendingRequests.removeIf(r -> r.startsWith(studentEmail + "|"));
                        AuthUI.userDatabase.remove(selected);
                    }
                    AuthUI.saveUsers(); saveData(); refresh();
                }
            });

            canR.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(this, "Please select a trip from the table first.");
                    return;
                }
                String dest = (String) table.getValueAt(row, 0);
                ArrayList<String> applicants = new ArrayList<>();
                for (String r : registrations) if (r.endsWith("|" + dest)) applicants.add(r.split("\\|")[0]);
                if (applicants.isEmpty()) { JOptionPane.showMessageDialog(this, "No approved registrations found."); return; }
                String selEmail = (String) JOptionPane.showInputDialog(this, "Select student to remove:", "Cancel Reg", 3, null, applicants.toArray(), applicants.get(0));
                if (selEmail != null && registrations.remove(selEmail.toLowerCase() + "|" + dest)) {
                    Trip t = tripList.get(row);
                    t.incrementSeats();
                    if (t.getSeats() > 0 && !t.getStatus().equals("Closed")) t.setStatus("Available");
                    saveData(); refresh();
                }
            });

            rep.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(this, "Please select a trip from the table first.");
                    return;
                }
                String dest = (String)table.getValueAt(row, 0);
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%-10s %-30s %-15s\n", "Seat", "Email", "Phone"));
                sb.append("------------------------------------------------------------\n");

                int seatCounter = 1;
                boolean found = false;
                for (String r : registrations) {
                    if (r.endsWith("|" + dest)) {
                        found = true;
                        String email = r.split("\\|")[0];
                        String phone = "N/A";
                        for (User u : AuthUI.userDatabase) {
                            if (u.getEmail().equalsIgnoreCase(email)) { phone = u.getPhone(); break; }
                        }
                        sb.append(String.format("%-10d %-30s %-15s\n", seatCounter++, email, phone));
                    }
                }
                if (!found) JOptionPane.showMessageDialog(this, "No registrations found for " + dest);
                else {
                    JTextArea area = new JTextArea(sb.toString(), 15, 50);
                    area.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    area.setEditable(false);
                    JOptionPane.showMessageDialog(this, new JScrollPane(area), "Report: " + dest, 1);
                }
            });

        } else {
            JButton app = new JButton("Apply Trip"), canT = new JButton("Cancel My Trip");
            cp.add(app); cp.add(canT);

            app.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(this, "Please select a trip from the table first.");
                    return;
                }
                Trip t = tripList.get(row);

                // FUNCTIONAL STATUS: Check status before applying
                if (t.getStatus().equalsIgnoreCase("Closed")) {
                    JOptionPane.showMessageDialog(this, "Registration for this trip is currently closed.");
                    return;
                }

                String key = currentUser.getEmail().toLowerCase() + "|" + t.getDestination();
                if (registrations.contains(key) || pendingRequests.contains(key)) {
                    JOptionPane.showMessageDialog(this, "Your application for this trip is already submitted or awaiting approval.");
                } else if (t.bookSeat()) {
                    pendingRequests.add(key);
                    // Logic: Auto-close if seats reach zero
                    if (t.getSeats() <= 0) t.setStatus("Closed");

                    JOptionPane.showMessageDialog(this,
                            "Application Submitted Successfully.\n\n" +
                                    "Please transfer the trip fee to Account #1234 to finalize your request.\n" +
                                    "Your status will be updated upon administrative verification.");
                    saveData(); refresh();
                } else {
                    JOptionPane.showMessageDialog(this, "This trip is currently full.");
                }
            });

            canT.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(this, "Please select a trip from the table first.");
                    return;
                }
                String dest = (String) table.getValueAt(row, 0);
                String key = currentUser.getEmail().toLowerCase() + "|" + dest;
                if (registrations.remove(key) || pendingRequests.remove(key)) {
                    for (Trip t : tripList) {
                        if (t.getDestination().equals(dest)) {
                            t.incrementSeats();
                            // Logic: Auto-reopen if seats become available
                            if (t.getSeats() > 0 && !t.getStatus().equals("Closed")) t.setStatus("Available");
                        }
                    }
                    saveData(); refresh();
                }
            });
        }
        add(cp, BorderLayout.SOUTH); refresh(); setLocationRelativeTo(null); setVisible(true);
    }

    private void checkApprovalNotifications(User u) {
        ArrayList<String> toRemove = new ArrayList<>();
        for (String res : approvalResults) {
            String[] parts = res.split("\\|");
            if (parts[0].equalsIgnoreCase(u.getEmail())) {
                String dest = parts[1];
                if (parts[2].equals("Approved")) {
                    int seatNo = 0;
                    for (String reg : registrations) {
                        if (reg.endsWith("|" + dest)) {
                            seatNo++;
                            if (reg.startsWith(u.getEmail().toLowerCase() + "|")) break;
                        }
                    }
                    JOptionPane.showMessageDialog(this, "Great news! Your request for " + dest + " has been approved. Your assigned seat number is " + seatNo + ".");
                } else {
                    JOptionPane.showMessageDialog(this, "We regret to inform you that your request for " + dest + " was declined.");
                }
                toRemove.add(res);
            }
        }
        approvalResults.removeAll(toRemove);
        saveData();
    }

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("trips.txt"))) {
            oos.writeObject(tripList); oos.writeObject(registrations);
            oos.writeObject(pendingRequests); oos.writeObject(approvalResults);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadData() {
        File f = new File("trips.txt");
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                tripList = (ArrayList<Trip>) ois.readObject();
                registrations = (ArrayList<String>) ois.readObject();
                pendingRequests = (ArrayList<String>) ois.readObject();
                approvalResults = (ArrayList<String>) ois.readObject();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void refresh() {
        model.setRowCount(0);
        for (Trip t : tripList) model.addRow(new Object[]{t.getDestination(), t.getCost(), t.getDeadline(), t.getSeats(), t.getStatus()});
    }
}
