import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

public class AuthUI extends JFrame {
    private JTextField emailField = new JTextField(15), phoneField = new JTextField(15), colorField = new JTextField(15);
    private JPasswordField passField = new JPasswordField(15);
    private JComboBox<String> roleBox = new JComboBox<>(new String[]{"Student", "Teacher", "Staff"});
    public static ArrayList<User> userDatabase = new ArrayList<>();

    public AuthUI() {
        loadUsers();
        setTitle("TMS - Secure Login");
        setSize(450, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());

        JPanel p = new JPanel(new GridLayout(9, 2, 10, 10));
        p.add(new JLabel("Email:")); p.add(emailField);
        p.add(new JLabel("Password (>7):")); p.add(passField);
        p.add(new JLabel("Phone (Required):")); p.add(phoneField);
        p.add(new JLabel("Fav Color (Required):")); p.add(colorField);
        p.add(new JLabel("Role:")); p.add(roleBox);

        JButton regBtn = new JButton("Register"), loginBtn = new JButton("Login"), forgotBtn = new JButton("Forgot Password");
        p.add(regBtn); p.add(loginBtn);
        p.add(new JLabel("")); p.add(forgotBtn);

        regBtn.addActionListener(e -> {
            String email = emailField.getText().trim(), pass = new String(passField.getPassword()),
                    phone = phoneField.getText().trim(), color = colorField.getText().trim().toLowerCase(),
                    role = (String)roleBox.getSelectedItem();

            if (email.isEmpty() || phone.isEmpty() || color.isEmpty() || pass.length() <= 7) {
                JOptionPane.showMessageDialog(this, "Fill all boxes correctly (Password must be > 7 chars)!");
                return;
            }
            if (!email.contains("@")) {
                JOptionPane.showMessageDialog(this, "Please enter a valid email address containing '@'!");
                return;
            }

            long sCount = userDatabase.stream().filter(u -> u.getRole().equals("Staff")).count();
            long tCount = userDatabase.stream().filter(u -> u.getRole().equals("Teacher")).count();
            if (role.equals("Staff") && sCount >= 1) {
                JOptionPane.showMessageDialog(this, "Staff quota (1) reached!"); return;
            }
            if (role.equals("Teacher") && tCount >= 2) {
                JOptionPane.showMessageDialog(this, "Teacher quota (2) reached!"); return;
            }

            if (userDatabase.stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email))) {
                JOptionPane.showMessageDialog(this, "This email is already registered!");
                return;
            }

            userDatabase.add(new User(email, pass, role, phone, color));
            saveUsers();
            JOptionPane.showMessageDialog(this, "Account Registered Successfully!");
        });

        loginBtn.addActionListener(e -> {
            String inputEmail = emailField.getText().trim();
            String inputPass = new String(passField.getPassword());
            String selectedRole = (String)roleBox.getSelectedItem();

            for (User u : userDatabase) {
                if (u.getEmail().equalsIgnoreCase(inputEmail) && u.getPassword().equals(inputPass) && u.getRole().equals(selectedRole)) {
                    if (!u.isActive()) {
                        JOptionPane.showMessageDialog(this, "Your account is deactivated by Admin!", "Access Denied", 0);
                        return;
                    }
                    new TripUI(u);
                    this.dispose();
                    return;
                }
            }
            JOptionPane.showMessageDialog(this, "Invalid credentials or Role selection!");
        });

        forgotBtn.addActionListener(e -> {
            String email = JOptionPane.showInputDialog("Enter Registered Email:");
            if (email == null) return; // User clicked cancel

            User target = userDatabase.stream().filter(u -> u.getEmail().equalsIgnoreCase(email.trim())).findFirst().orElse(null);

            if (target != null) {
                String pV = JOptionPane.showInputDialog("Enter Registered Phone Number:");
                if (pV == null) return;

                String cV = JOptionPane.showInputDialog("Enter Favorite Color:");
                if (cV == null) return;

                pV = pV.trim();
                cV = cV.trim();

                if (pV.equals(target.getPhone()) && cV.equalsIgnoreCase(target.getFavoriteColor())) {
                    String newP = JOptionPane.showInputDialog("Identity Verified. Enter New Password (>7):");
                    if (newP == null) return; // User clicked cancel on password entry

                    if (newP.length() > 7) {
                        target.setPassword(newP);
                        saveUsers();
                        JOptionPane.showMessageDialog(this, "Password Reset Successful!");
                    } else {
                        JOptionPane.showMessageDialog(this, "Password must be greater than 7 characters.");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Identity verification failed.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "User not found.");
            }
        });

        add(p); setLocationRelativeTo(null); setVisible(true);
    }

    public static void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("users.txt"))) {
            oos.writeObject(userDatabase);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadUsers() {
        File f = new File("users.txt");
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                Object data = ois.readObject();
                if (data instanceof ArrayList) {
                    userDatabase = (ArrayList<User>) data;
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
