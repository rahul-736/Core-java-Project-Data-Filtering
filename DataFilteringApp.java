import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.Timer;
import java.util.TimerTask;

public class DataFilteringApp {
    private JFrame frame, loginFrame;
    private JTextField searchField, fromDateField, toDateField, usernameField;
    private JPasswordField passwordField;
    private JTable table;
    private DefaultTableModel model;
    private JButton searchButton, exportButton, addButton, editButton, deleteButton, loginButton;
    private JComboBox<String> filterType, sortType;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/demo";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    public DataFilteringApp() {
        loginFrame = new JFrame("Login");
        loginFrame.setSize(300, 200);
        loginFrame.setLayout(new FlowLayout());
        loginFrame.add(new JLabel("Username: "));
        usernameField = new JTextField(15);
        loginFrame.add(usernameField);
        loginFrame.add(new JLabel("Password: "));
        passwordField = new JPasswordField(15);
        loginFrame.add(passwordField);
        loginButton = new JButton("Login");
        loginFrame.add(loginButton);
        loginButton.addActionListener(e -> authenticateUser());
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setVisible(true);
    }
    
    private void filterData() {
    model.setRowCount(0); // Clear the table before adding new data
    String query = "SELECT * FROM records";
    String filter = searchField.getText().trim();
    String filterOption = (String) filterType.getSelectedItem();
    String sortOption = (String) sortType.getSelectedItem();

    if (!filter.isEmpty()) {
        if ("Name".equals(filterOption)) {
            query += " WHERE name LIKE ?";
        } else if ("Date".equals(filterOption)) {
            query += " WHERE date = ?";
        } else if ("Date Range".equals(filterOption)) {
            query += " WHERE date BETWEEN ? AND ?";
        }
    }

    if ("Sort by Name".equals(sortOption)) {
        query += " ORDER BY name";
    } else if ("Sort by Date".equals(sortOption)) {
        query += " ORDER BY date";
    } else {
        query += " ORDER BY id";
    }

    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
         PreparedStatement stmt = conn.prepareStatement(query)) {
        
        if (!filter.isEmpty()) {
            if ("Name".equals(filterOption)) {
                stmt.setString(1, "%" + filter + "%");
            } else if ("Date".equals(filterOption)) {
                stmt.setString(1, filter);
            } else if ("Date Range".equals(filterOption)) {
                stmt.setString(1, fromDateField.getText().trim());
                stmt.setString(2, toDateField.getText().trim());
            }
        }

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDate("date")
            });
        }
    } catch (SQLException ex) {
        ex.printStackTrace();
    }
}
    
    private void exportToCSV() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Save CSV File");
    int userSelection = fileChooser.showSaveDialog(frame);
    
    if (userSelection == JFileChooser.APPROVE_OPTION) {
        try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile() + ".csv")) {
            // Write column headers
            for (int i = 0; i < model.getColumnCount(); i++) {
                writer.write(model.getColumnName(i) + ",");
            }
            writer.write("\n");

            // Write row data
            for (int i = 0; i < model.getRowCount(); i++) {
                for (int j = 0; j < model.getColumnCount(); j++) {
                    writer.write(model.getValueAt(i, j).toString() + ",");
                }
                writer.write("\n");
            }

            writer.flush();
            JOptionPane.showMessageDialog(frame, "Data exported successfully!");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error exporting data!", "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}


    
    private void authenticateUser() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                loginFrame.dispose();
                showMainApp();
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Invalid credentials!");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void showMainApp() {
        frame = new JFrame("Data Filtering Application");
        frame.setSize(800, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        
        filterType = new JComboBox<>(new String[]{"Name", "Date", "Date Range"});
        searchField = new JTextField(10);
        fromDateField = new JTextField(8);
        toDateField = new JTextField(8);
        searchButton = new JButton("Search");
        sortType = new JComboBox<>(new String[]{"Sort by ID", "Sort by Name", "Sort by Date"});
        exportButton = new JButton("Export to CSV");
        addButton = new JButton("Add");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");
        
        panel.add(filterType);
        panel.add(searchField);
        panel.add(new JLabel("From:"));
        panel.add(fromDateField);
        panel.add(new JLabel("To:"));
        panel.add(toDateField);
        panel.add(searchButton);
        panel.add(sortType);
        panel.add(exportButton);
        panel.add(addButton);
        panel.add(editButton);
        panel.add(deleteButton);
        
        frame.add(panel, BorderLayout.NORTH);
        
        model = new DefaultTableModel(new String[]{"ID", "Name", "Date"}, 0);
        table = new JTable(model);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        
        searchButton.addActionListener(e -> filterData());
        exportButton.addActionListener(e -> exportToCSV());
        addButton.addActionListener(e -> addRecord());
        editButton.addActionListener(e -> editRecord());
        deleteButton.addActionListener(e -> deleteRecord());
        
        frame.setVisible(true);
        autoRefresh();
    }
    
    private void autoRefresh() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                filterData();
            }
        }, 0, 5000);
    }
    
    private void addRecord() {
        String name = JOptionPane.showInputDialog("Enter Name:");
        String date = JOptionPane.showInputDialog("Enter Date (YYYY-MM-DD):");
        if (name != null && date != null) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO records (name, date) VALUES (?, ?)");) {
                stmt.setString(1, name);
                stmt.setString(2, date);
                stmt.executeUpdate();
                filterData();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void editRecord() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Select a record to edit");
            return;
        }
        int id = (int) model.getValueAt(selectedRow, 0);
        String newName = JOptionPane.showInputDialog("Enter New Name:", model.getValueAt(selectedRow, 1));
        String newDate = JOptionPane.showInputDialog("Enter New Date (YYYY-MM-DD):", model.getValueAt(selectedRow, 2));
        if (newName != null && newDate != null) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement("UPDATE records SET name=?, date=? WHERE id=?")) {
                stmt.setString(1, newName);
                stmt.setString(2, newDate);
                stmt.setInt(3, id);
                stmt.executeUpdate();
                filterData();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void deleteRecord() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Select a record to delete");
            return;
        }
        int id = (int) model.getValueAt(selectedRow, 0);
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM records WHERE id=?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            filterData();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DataFilteringApp::new);
    }
}
