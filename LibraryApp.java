import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class LibraryApp extends JFrame {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/library_db";
    private static final String USER = "root";
    private static final String PASS = "password";

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    private String loggedInUser = null;

    private JTable booksTable, studentsTable, categoriesTable, issuesTable;
    private DefaultTableModel booksModel, studentsModel, categoriesModel, issuesModel;
    
    private JComboBox<String> cbBookCategory, cbIssueBook, cbIssueStudent;

    private int selectedBookId = -1;
    private int selectedStudentId = -1;
    private int selectedCategoryId = -1;
    private int selectedIssueId = -1;

    public LibraryApp() {
        setTitle("Complete Library Management System");
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        mainPanel.add(createLoginPanel(), "Login");
        mainPanel.add(createRegisterPanel(), "Register");
        mainPanel.add(createDashboardPanel(), "Dashboard");

        add(mainPanel, BorderLayout.CENTER);
        
        // Add low opacity credit badge at the bottom
        JLabel lblCredit = new JLabel("Abhinav Das");
        lblCredit.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 11));
        // Using RGBA color (Black with 80/255 opacity) for a faded look
        lblCredit.setForeground(new Color(0, 0, 0, 80)); 
        lblCredit.setHorizontalAlignment(SwingConstants.RIGHT);
        lblCredit.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 10));
        add(lblCredit, BorderLayout.SOUTH);

        cardLayout.show(mainPanel, "Login");
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JPanel form = new JPanel(new GridLayout(4, 1, 5, 10));
        form.setBorder(BorderFactory.createTitledBorder("Librarian Login"));
        
        JTextField txtUser = new JTextField(15);
        JPasswordField txtPass = new JPasswordField(15);
        JButton btnLogin = new JButton("Login");
        JButton btnGoRegister = new JButton("Create New Account");

        form.add(createLabeledField("Username:", txtUser));
        form.add(createLabeledField("Password:", txtPass));
        form.add(btnLogin);
        form.add(btnGoRegister);
        panel.add(form);

        btnLogin.addActionListener(e -> {
            if(authenticate(txtUser.getText().trim(), new String(txtPass.getPassword()))) {
                loggedInUser = txtUser.getText().trim();
                txtUser.setText(""); txtPass.setText("");
                refreshAllData();
                cardLayout.show(mainPanel, "Dashboard");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or Password!", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnGoRegister.addActionListener(e -> cardLayout.show(mainPanel, "Register"));
        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JPanel form = new JPanel(new GridLayout(4, 1, 5, 10));
        form.setBorder(BorderFactory.createTitledBorder("Register New Librarian"));
        
        JTextField txtUser = new JTextField(15);
        JPasswordField txtPass = new JPasswordField(15);
        JButton btnRegister = new JButton("Register Account");
        JButton btnGoLogin = new JButton("Back to Login");

        form.add(createLabeledField("New Username:", txtUser));
        form.add(createLabeledField("New Password:", txtPass));
        form.add(btnRegister);
        form.add(btnGoLogin);
        panel.add(form);

        btnRegister.addActionListener(e -> {
            if(txtUser.getText().trim().isEmpty() || new String(txtPass.getPassword()).isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fields cannot be empty.");
                return;
            }
            if(registerUser(txtUser.getText().trim(), new String(txtPass.getPassword()))) {
                JOptionPane.showMessageDialog(this, "Registration Successful! Please login.");
                txtUser.setText(""); txtPass.setText("");
                cardLayout.show(mainPanel, "Login");
            }
        });

        btnGoLogin.addActionListener(e -> cardLayout.show(mainPanel, "Login"));
        return panel;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel lblTitle = new JLabel("Library Management Dashboard", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        JButton btnLogout = new JButton("Logout");
        
        btnLogout.addActionListener(e -> {
            loggedInUser = null;
            cardLayout.show(mainPanel, "Login");
        });
        
        header.add(lblTitle, BorderLayout.CENTER);
        header.add(btnLogout, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Books", createBooksTab());
        tabbedPane.addTab("Students", createStudentsTab());
        tabbedPane.addTab("Categories", createCategoriesTab());
        tabbedPane.addTab("Issue Records", createIssuesTab());
        
        panel.add(tabbedPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBooksTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridLayout(6, 1, 5, 10));
        form.setBorder(BorderFactory.createTitledBorder("Manage Books"));
        form.setPreferredSize(new Dimension(280, 0));
        
        JTextField txtTitle = new JTextField();
        JTextField txtAuthor = new JTextField();
        cbBookCategory = new JComboBox<>();
        JTextField txtQty = new JTextField();
        
        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        btnPanel.add(btnAdd); btnPanel.add(btnUpdate); btnPanel.add(btnDelete);

        form.add(createLabeledField("Title:", txtTitle));
        form.add(createLabeledField("Author:", txtAuthor));
        form.add(createLabeledField("Category:", cbBookCategory));
        form.add(createLabeledField("Quantity:", txtQty));
        form.add(new JLabel());
        form.add(btnPanel);

        booksModel = new DefaultTableModel(new String[]{"ID", "Title", "Author", "Category", "Qty"}, 0);
        booksTable = new JTable(booksModel);

        booksTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && booksTable.getSelectedRow() != -1) {
                int row = booksTable.getSelectedRow();
                selectedBookId = (int) booksModel.getValueAt(row, 0);
                txtTitle.setText(booksModel.getValueAt(row, 1).toString());
                txtAuthor.setText(booksModel.getValueAt(row, 2).toString());
                txtQty.setText(booksModel.getValueAt(row, 4).toString());
                String catName = booksModel.getValueAt(row, 3).toString();
                for (int i = 0; i < cbBookCategory.getItemCount(); i++) {
                    if (cbBookCategory.getItemAt(i).contains(catName)) {
                        cbBookCategory.setSelectedIndex(i);
                        break;
                    }
                }
            }
        });

        btnAdd.addActionListener(e -> {
            if(cbBookCategory.getSelectedItem() == null) return;
            int catId = Integer.parseInt(((String)cbBookCategory.getSelectedItem()).split(" - ")[0]);
            executeQuery("INSERT INTO Books (Title, Author, CategoryID, Quantity) VALUES (?, ?, ?, ?)", 
                txtTitle.getText(), txtAuthor.getText(), catId, Integer.parseInt(txtQty.getText()));
            txtTitle.setText(""); txtAuthor.setText(""); txtQty.setText("");
            refreshAllData();
        });

        btnUpdate.addActionListener(e -> {
            if(selectedBookId == -1 || cbBookCategory.getSelectedItem() == null) return;
            int catId = Integer.parseInt(((String)cbBookCategory.getSelectedItem()).split(" - ")[0]);
            executeQuery("UPDATE Books SET Title=?, Author=?, CategoryID=?, Quantity=? WHERE BookID=?", 
                txtTitle.getText(), txtAuthor.getText(), catId, Integer.parseInt(txtQty.getText()), selectedBookId);
            selectedBookId = -1;
            txtTitle.setText(""); txtAuthor.setText(""); txtQty.setText("");
            refreshAllData();
        });

        btnDelete.addActionListener(e -> {
            if(selectedBookId == -1) return;
            executeQuery("DELETE FROM Books WHERE BookID=?", selectedBookId);
            selectedBookId = -1;
            txtTitle.setText(""); txtAuthor.setText(""); txtQty.setText("");
            refreshAllData();
        });
        
        panel.add(form, BorderLayout.WEST);
        panel.add(new JScrollPane(booksTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStudentsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridLayout(5, 1, 5, 10));
        form.setBorder(BorderFactory.createTitledBorder("Manage Students"));
        form.setPreferredSize(new Dimension(280, 0));
        
        JTextField txtName = new JTextField();
        JTextField txtEmail = new JTextField();
        JTextField txtPhone = new JTextField();
        
        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        btnPanel.add(btnAdd); btnPanel.add(btnUpdate); btnPanel.add(btnDelete);

        form.add(createLabeledField("Full Name:", txtName));
        form.add(createLabeledField("Email:", txtEmail));
        form.add(createLabeledField("Phone:", txtPhone));
        form.add(new JLabel());
        form.add(btnPanel);

        studentsModel = new DefaultTableModel(new String[]{"ID", "Name", "Email", "Phone"}, 0);
        studentsTable = new JTable(studentsModel);

        studentsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && studentsTable.getSelectedRow() != -1) {
                int row = studentsTable.getSelectedRow();
                selectedStudentId = (int) studentsModel.getValueAt(row, 0);
                txtName.setText(studentsModel.getValueAt(row, 1).toString());
                txtEmail.setText(studentsModel.getValueAt(row, 2).toString());
                txtPhone.setText(studentsModel.getValueAt(row, 3).toString());
            }
        });

        btnAdd.addActionListener(e -> {
            executeQuery("INSERT INTO Students (FullName, Email, Phone) VALUES (?, ?, ?)", 
                txtName.getText(), txtEmail.getText(), txtPhone.getText());
            txtName.setText(""); txtEmail.setText(""); txtPhone.setText("");
            refreshAllData();
        });

        btnUpdate.addActionListener(e -> {
            if(selectedStudentId == -1) return;
            executeQuery("UPDATE Students SET FullName=?, Email=?, Phone=? WHERE StudentID=?", 
                txtName.getText(), txtEmail.getText(), txtPhone.getText(), selectedStudentId);
            selectedStudentId = -1;
            txtName.setText(""); txtEmail.setText(""); txtPhone.setText("");
            refreshAllData();
        });

        btnDelete.addActionListener(e -> {
            if(selectedStudentId == -1) return;
            executeQuery("DELETE FROM Students WHERE StudentID=?", selectedStudentId);
            selectedStudentId = -1;
            txtName.setText(""); txtEmail.setText(""); txtPhone.setText("");
            refreshAllData();
        });
        
        panel.add(form, BorderLayout.WEST);
        panel.add(new JScrollPane(studentsTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCategoriesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridLayout(3, 1, 5, 10));
        form.setBorder(BorderFactory.createTitledBorder("Manage Categories"));
        form.setPreferredSize(new Dimension(280, 0));
        
        JTextField txtName = new JTextField();
        
        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        btnPanel.add(btnAdd); btnPanel.add(btnUpdate); btnPanel.add(btnDelete);

        form.add(createLabeledField("Category Name:", txtName));
        form.add(new JLabel());
        form.add(btnPanel);

        categoriesModel = new DefaultTableModel(new String[]{"ID", "Category Name"}, 0);
        categoriesTable = new JTable(categoriesModel);

        categoriesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && categoriesTable.getSelectedRow() != -1) {
                int row = categoriesTable.getSelectedRow();
                selectedCategoryId = (int) categoriesModel.getValueAt(row, 0);
                txtName.setText(categoriesModel.getValueAt(row, 1).toString());
            }
        });

        btnAdd.addActionListener(e -> {
            executeQuery("INSERT INTO Categories (CategoryName) VALUES (?)", txtName.getText());
            txtName.setText("");
            refreshAllData();
        });

        btnUpdate.addActionListener(e -> {
            if(selectedCategoryId == -1) return;
            executeQuery("UPDATE Categories SET CategoryName=? WHERE CategoryID=?", txtName.getText(), selectedCategoryId);
            selectedCategoryId = -1;
            txtName.setText("");
            refreshAllData();
        });

        btnDelete.addActionListener(e -> {
            if(selectedCategoryId == -1) return;
            executeQuery("DELETE FROM Categories WHERE CategoryID=?", selectedCategoryId);
            selectedCategoryId = -1;
            txtName.setText("");
            refreshAllData();
        });
        
        panel.add(form, BorderLayout.WEST);
        panel.add(new JScrollPane(categoriesTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createIssuesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridLayout(6, 1, 5, 10));
        form.setBorder(BorderFactory.createTitledBorder("Manage Issues"));
        form.setPreferredSize(new Dimension(280, 0));
        
        cbIssueBook = new JComboBox<>();
        cbIssueStudent = new JComboBox<>();
        JTextField txtIssueDate = new JTextField(LocalDate.now().toString());
        JTextField txtDueDate = new JTextField(LocalDate.now().plusDays(14).toString());
        
        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        JButton btnAdd = new JButton("Issue");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Return/Del");
        btnPanel.add(btnAdd); btnPanel.add(btnUpdate); btnPanel.add(btnDelete);

        form.add(createLabeledField("Select Book:", cbIssueBook));
        form.add(createLabeledField("Select Student:", cbIssueStudent));
        form.add(createLabeledField("Issue Date (YYYY-MM-DD):", txtIssueDate));
        form.add(createLabeledField("Due Date (YYYY-MM-DD):", txtDueDate));
        form.add(new JLabel());
        form.add(btnPanel);

        issuesModel = new DefaultTableModel(new String[]{"ID", "Book", "Student", "Issue Date", "Due Date"}, 0);
        issuesTable = new JTable(issuesModel);

        issuesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && issuesTable.getSelectedRow() != -1) {
                int row = issuesTable.getSelectedRow();
                selectedIssueId = (int) issuesModel.getValueAt(row, 0);
                txtIssueDate.setText(issuesModel.getValueAt(row, 3).toString());
                txtDueDate.setText(issuesModel.getValueAt(row, 4).toString());
                
                String bookName = issuesModel.getValueAt(row, 1).toString();
                for (int i = 0; i < cbIssueBook.getItemCount(); i++) {
                    if (cbIssueBook.getItemAt(i).contains(bookName)) {
                        cbIssueBook.setSelectedIndex(i);
                        break;
                    }
                }
                
                String studentName = issuesModel.getValueAt(row, 2).toString();
                for (int i = 0; i < cbIssueStudent.getItemCount(); i++) {
                    if (cbIssueStudent.getItemAt(i).contains(studentName)) {
                        cbIssueStudent.setSelectedIndex(i);
                        break;
                    }
                }
            }
        });

        btnAdd.addActionListener(e -> {
            if(cbIssueBook.getSelectedItem() == null || cbIssueStudent.getSelectedItem() == null) return;
            int bookId = Integer.parseInt(((String)cbIssueBook.getSelectedItem()).split(" - ")[0]);
            int studentId = Integer.parseInt(((String)cbIssueStudent.getSelectedItem()).split(" - ")[0]);
            
            executeQuery("INSERT INTO Issue_Records (BookID, StudentID, IssueDate, DueDate) VALUES (?, ?, ?, ?)", 
                bookId, studentId, txtIssueDate.getText(), txtDueDate.getText());
            executeQuery("UPDATE Books SET Quantity = Quantity - 1 WHERE BookID = ? AND Quantity > 0", bookId);
            refreshAllData();
        });

        btnUpdate.addActionListener(e -> {
            if(selectedIssueId == -1 || cbIssueBook.getSelectedItem() == null || cbIssueStudent.getSelectedItem() == null) return;
            int bookId = Integer.parseInt(((String)cbIssueBook.getSelectedItem()).split(" - ")[0]);
            int studentId = Integer.parseInt(((String)cbIssueStudent.getSelectedItem()).split(" - ")[0]);
            
            executeQuery("UPDATE Issue_Records SET BookID=?, StudentID=?, IssueDate=?, DueDate=? WHERE IssueID=?", 
                bookId, studentId, txtIssueDate.getText(), txtDueDate.getText(), selectedIssueId);
            selectedIssueId = -1;
            refreshAllData();
        });

        btnDelete.addActionListener(e -> {
            if(selectedIssueId == -1) return;
            executeQuery("DELETE FROM Issue_Records WHERE IssueID=?", selectedIssueId);
            selectedIssueId = -1;
            refreshAllData();
        });
        
        panel.add(form, BorderLayout.WEST);
        panel.add(new JScrollPane(issuesTable), BorderLayout.CENTER);
        return panel;
    }

    private boolean authenticate(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Librarians WHERE Username=? AND PasswordHash=?");
            stmt.setString(1, username);
            stmt.setString(2, password);
            return stmt.executeQuery().next();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
            return false;
        }
    }

    private boolean registerUser(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO Librarians (Username, PasswordHash) VALUES (?, ?)");
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            return true;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Registration Error: Username may already exist.");
            return false;
        }
    }

    private void executeQuery(String sql, Object... params) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Operation Successful!");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshAllData() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            Statement stmt = conn.createStatement();
            
            categoriesModel.setRowCount(0);
            cbBookCategory.removeAllItems();
            ResultSet rsCat = stmt.executeQuery("SELECT * FROM Categories");
            while(rsCat.next()) {
                categoriesModel.addRow(new Object[]{rsCat.getInt("CategoryID"), rsCat.getString("CategoryName")});
                cbBookCategory.addItem(rsCat.getInt("CategoryID") + " - " + rsCat.getString("CategoryName"));
            }

            studentsModel.setRowCount(0);
            cbIssueStudent.removeAllItems();
            ResultSet rsStu = stmt.executeQuery("SELECT * FROM Students");
            while(rsStu.next()) {
                studentsModel.addRow(new Object[]{rsStu.getInt("StudentID"), rsStu.getString("FullName"), rsStu.getString("Email"), rsStu.getString("Phone")});
                cbIssueStudent.addItem(rsStu.getInt("StudentID") + " - " + rsStu.getString("FullName"));
            }

            booksModel.setRowCount(0);
            cbIssueBook.removeAllItems();
            ResultSet rsBook = stmt.executeQuery("SELECT Books.BookID, Books.Title, Books.Author, Categories.CategoryName, Books.Quantity FROM Books LEFT JOIN Categories ON Books.CategoryID = Categories.CategoryID ORDER BY Books.BookID DESC");
            while(rsBook.next()) {
                int qty = rsBook.getInt("Quantity");
                booksModel.addRow(new Object[]{rsBook.getInt("BookID"), rsBook.getString("Title"), rsBook.getString("Author"), rsBook.getString("CategoryName"), qty});
                cbIssueBook.addItem(rsBook.getInt("BookID") + " - " + rsBook.getString("Title"));
            }

            issuesModel.setRowCount(0);
            ResultSet rsIss = stmt.executeQuery("SELECT Issue_Records.IssueID, Books.Title, Students.FullName, Issue_Records.IssueDate, Issue_Records.DueDate FROM Issue_Records JOIN Books ON Issue_Records.BookID = Books.BookID JOIN Students ON Issue_Records.StudentID = Students.StudentID");
            while(rsIss.next()) {
                issuesModel.addRow(new Object[]{rsIss.getInt("IssueID"), rsIss.getString("Title"), rsIss.getString("FullName"), rsIss.getDate("IssueDate"), rsIss.getDate("DueDate")});
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private JPanel createLabeledField(String labelText, JComponent field) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(labelText), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new LibraryApp().setVisible(true));
    }
}