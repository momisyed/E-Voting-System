import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

//================
// INTERFACES
//================
interface Storable {
    // Convert object to string suitable for saving in a file
    String toString();
}

interface Authenticatable {
    // Authenticate user with username and password
    boolean login(String username, String password);
}

//===================
// ABSTRACT CLASS
//===================
abstract class Person {
    protected String name;
    protected int age;

    public String getName() { return name; }
    public int getAge() { return age; }

    // Abstract method to get type of person (Voter/Admin)
    public abstract String getPersonType();
}

//===================
// MODEL CLASSES
//===================

// Candidate class representing a political candidate
class Candidate implements Storable {
    private String id, name, party;

    public Candidate(String id, String name, String party) {
        this.id = id;
        this.name = name;
        this.party = party;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getParty() { return party; }

    @Override
    public String toString() {
        // Format candidate as CSV (comma seperated values) string for file storage
        return id + "," + name + "," + party;
    }

    public static Candidate fromString(String line) {
        // Convert CSV line to Candidate object
        String[] p = line.split(",");
        return new Candidate(p[0], p[1], p[2]);
    }
}

// Voter class representing a voter
class Voter extends Person implements Storable, Authenticatable {
    private String cnic;      // Unique voter ID
    private String password;  // Password
    private boolean voted;    // Has voter cast vote

    public Voter(String cnic, String name, String password, int age, boolean voted) {
        this.cnic = cnic;
        this.name = name;
        this.age = age;
        this.password = password;
        this.voted = voted;
    }

    public String getCnic() { return cnic; }
    public String getPassword() { return password; }
    public boolean hasVoted() { return voted; }
    public void setVoted(boolean v) { this.voted = v; }

    @Override
    public String toString() {
        // Format voter as CSV string for file storage
        return cnic + "," + name + "," + password + "," + age + "," + voted;
    }

    public static Voter fromString(String line) {
        // Convert CSV line to Voter object
        String[] p = line.split(",", -1); // keep empty strings
        if(p.length < 5) return null;
        try {
            int age = Integer.parseInt(p[3]);
            boolean voted = Boolean.parseBoolean(p[4]);
            return new Voter(p[0], p[1], p[2], age, voted);
        } catch(Exception e) {
            return null; // return null if parsing fails
        }
    }

    @Override
    public String getPersonType() {
        return "Voter";
    }

    @Override
    public boolean login(String username, String password) {
        // Validate CNIC and password
        return this.cnic.equals(username) && this.password.equals(password);
    }
}

//===================
// Admin Class
//===================
class Admin extends Person implements Authenticatable {
    private String username;
    private String password;

    public Admin(String username, String password) {
        this.username = username;
        this.password = password;
        this.name = "Administrator";
        this.age = 30; // default age
    }

    @Override
    public boolean login(String username, String password) {
        // Validate admin credentials
        return this.username.equals(username) && this.password.equals(password);
    }

    @Override
    public String getPersonType() {
        return "Admin";
    }
}

//==========================
// FILE HANDLING SERVICE
//==========================
class FileService {

    // Load candidates from file
    public static List<Candidate> loadCandidates() throws IOException {
        List<Candidate> list = new ArrayList<>();
        File file = new File("candidates.txt");
        if (!file.exists()) file.createNewFile(); // create if missing
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            if(!line.trim().isEmpty()) list.add(Candidate.fromString(line));
        }
        br.close();
        return list;
    }

    // Save a single candidate to file
    public static void saveCandidate(Candidate c) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("candidates.txt", true));
        bw.write(c.toString());
        bw.newLine();
        bw.close();
    }

    // Load voters from file
    public static List<Voter> loadVoters() throws IOException {
        List<Voter> list = new ArrayList<>();
        File file = new File("voters.txt");
        if (!file.exists()) file.createNewFile();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            if(!line.trim().isEmpty()) {
                Voter v = Voter.fromString(line);
                if(v != null) list.add(v);
            }
        }
        br.close();
        return list;
    }

    // Save entire voter list to file
    public static void saveVoters(List<Voter> voters) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("voters.txt"));
        for(Voter v : voters) {
            bw.write(v.toString());
            bw.newLine();
        }
        bw.close();
    }

    // Save a single vote
    public static void saveVote(String voterId, String candidateId) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("votes.txt", true));
        bw.write(voterId + "," + candidateId);
        bw.newLine();
        bw.close();
    }

    // Count votes for each candidate without using HashMap
    public static List<String> countVotes() throws IOException {
        List<String> candidateIds = new ArrayList<>();

        File file = new File("votes.txt");
        if(!file.exists()) file.createNewFile();

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        // Step 1: Load candidate IDs from votes
        while((line = br.readLine()) != null) {
            String[] p = line.split(",");
            if (p.length >= 2) {
                candidateIds.add(p[1]);  
            }
        }
        br.close();

        // Step 2: Sort candidate IDs for counting
        Collections.sort(candidateIds);

        List<String> result = new ArrayList<>();

        if (candidateIds.isEmpty()) {
            result.add("No votes found");
            return result;
        }

        // Step 3: Count votes by iterating sorted list
        String current = candidateIds.get(0);
        int count = 1;

        for (int i = 1; i < candidateIds.size(); i++) {
            if (candidateIds.get(i).equals(current)) {
                count++;   
            } else {
                result.add(current + " = " + count);
                current = candidateIds.get(i);
                count = 1;
            }
        }
        result.add(current + " = " + count);

        return result;
    }
}

//=====================
// VOTER MANAGEMENT
//=====================
class VoterManager {
    private List<Voter> voters;

    public VoterManager() {
        try { voters = FileService.loadVoters(); }
        catch (IOException e) { voters = new ArrayList<>(); }
    }

    // Register a new voter
    public boolean register(Voter v) {
        for(Voter existing : voters) {
            if(existing.getCnic().equals(v.getCnic())) return false; // duplicate CNIC
        }
        voters.add(v);
        try { FileService.saveVoters(voters); }
        catch (IOException e) { e.printStackTrace(); }
        return true;
    }

    // Login existing voter
    public Voter login(String cnic, String name) {
        for(Voter v : voters) {
            if(v.getCnic().equals(cnic) && v.getName().equalsIgnoreCase(name)) return v;
        }
        return null;
    }

    // Update voter after voting
    public void updateVoter(Voter v) {
        for(int i=0;i<voters.size();i++) {
            if(voters.get(i).getCnic().equals(v.getCnic())) {
                voters.set(i, v);
                break;
            }
        }
        try { FileService.saveVoters(voters); }
        catch(IOException e) { e.printStackTrace(); }
    }
}

//=====================
// GUI COMPONENTS
//=====================

// Main menu GUI
class MainMenu extends JFrame {
    public MainMenu() {
        setTitle("E-Voting System");
        setSize(400,300);
        setLayout(new GridLayout(3,1));

        JButton adminBtn = new JButton("Admin Login");
        JButton voterBtn = new JButton("Voter Login/Register");
        JButton exitBtn = new JButton("Exit");

        // Admin login button using anonymous class
        adminBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                new AdminLogin(); dispose();
            }
        }); 

        // Voter login/register using lambda
        voterBtn.addActionListener(e -> { new VoterLoginScreen(); dispose(); });

        // Exit application
        exitBtn.addActionListener(e -> System.exit(0));

        add(adminBtn); add(voterBtn); add(exitBtn);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }
}

// Admin login GUI
class AdminLogin extends JFrame {
    public AdminLogin() {
        setTitle("Admin Login");
        setSize(300,180);
        setLayout(new GridLayout(3,2));

        JLabel l1 = new JLabel("Username:");
        JLabel l2 = new JLabel("Password:");
        JTextField user = new JTextField();
        JPasswordField pass = new JPasswordField();
        JButton login = new JButton("Login");

        login.addActionListener(e -> {
            Admin admin = new Admin("admin","123"); // default admin credentials
            if(admin.login(user.getText(), String.valueOf(pass.getPassword()))) {
                JOptionPane.showMessageDialog(this, "Login Successful!");
                new AdminPanel();
                dispose();
            } else JOptionPane.showMessageDialog(this, "Invalid Credentials!");
        });

        add(l1); add(user); add(l2); add(pass); add(new JLabel()); add(login);
        setVisible(true);
    }
}

// Admin panel GUI
class AdminPanel extends JFrame {
    public AdminPanel() {
        setTitle("Admin Panel");
        setSize(300,200);
        setLayout(new GridLayout(3,1));

        JButton add = new JButton("Add Candidate");
        JButton view = new JButton("View Results");
        JButton back = new JButton("Back");

        add.addActionListener(e -> new AddCandidate());
        view.addActionListener(e -> new ViewResults());
        back.addActionListener(e -> { new MainMenu(); dispose(); });

        add(add); add(view); add(back);
        setVisible(true);
    }
}

// Add candidate GUI
class AddCandidate extends JFrame {
    public AddCandidate() {
        setTitle("Add Candidate");
        setSize(300,200);
        setLayout(new GridLayout(4,2));

        JTextField id = new JTextField();
        JTextField name = new JTextField();
        JTextField party = new JTextField();
        JButton save = new JButton("Save");

        save.addActionListener(e -> {
            try {
                String cid = id.getText().trim();
                String cname = name.getText().trim();
                String cparty = party.getText().trim();
                if(cid.isEmpty() || cname.isEmpty() || cparty.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Fill all fields!");
                    return;
                }
                FileService.saveCandidate(new Candidate(cid,cname,cparty));
                JOptionPane.showMessageDialog(this, "Candidate Added!");
                dispose();
            } catch(Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        add(new JLabel("ID:")); add(id);
        add(new JLabel("Name:")); add(name);
        add(new JLabel("Party:")); add(party);
        add(new JLabel()); add(save);
        setVisible(true);
    }
}

// View results GUI
class ViewResults extends JFrame {
    public ViewResults() {
        setTitle("Results");
        setSize(300, 300);
        setLayout(new GridLayout(0, 1));

        try {
            List<Candidate> candidates = FileService.loadCandidates();
            if (candidates.isEmpty()) {
                add(new JLabel("No candidates found."));
                setVisible(true);
                return;
            }

            List<String> voteIds = new ArrayList<>();
            File file = new File("votes.txt");
            if(file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while((line = br.readLine()) != null) {
                    String[] p = line.split(",");
                    if(p.length >= 2) voteIds.add(p[1]); // add candidate ID
                }
                br.close();
            }
            Collections.sort(voteIds); // sort for counting

            // Count votes per candidate
            for(Candidate c : candidates) {
                String cid = c.getId();
                int votes = 0;
                for(String vid : voteIds) {
                    if(vid.equals(cid)) votes++;
                    else if(vid.compareTo(cid) > 0) break; // stop early for sorted list
                }
                add(new JLabel(c.getName() + " (" + c.getParty() + ") = " + votes + " votes"));
            }

        } catch(Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading results!");
        }

        setVisible(true);
    }
}

// Voter login/register GUI
class VoterLoginScreen extends JFrame {

    private VoterManager manager = new VoterManager();

    public VoterLoginScreen() {
        setTitle("Voter Login/Register");
        setSize(400,300);
        setLayout(new GridLayout(5,1));

        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        JButton back = new JButton("Back");

        add(new JLabel("Login or Register as a Voter:", SwingConstants.CENTER));
        add(loginBtn);
        add(registerBtn);
        add(back);

        loginBtn.addActionListener(e -> showLogin());
        registerBtn.addActionListener(e -> showRegister());
        back.addActionListener(e -> { new MainMenu(); dispose(); });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // Show login dialog
    private void showLogin() {
        JTextField cnicField = new JTextField();
        JTextField nameField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Enter CNIC:"));
        panel.add(cnicField);
        panel.add(new JLabel("Enter Name:"));
        panel.add(nameField);

        int option = JOptionPane.showConfirmDialog(this, panel, "Voter Login", JOptionPane.OK_CANCEL_OPTION);
        if(option == JOptionPane.OK_OPTION) {
            String cnic = cnicField.getText().trim();
            String name = nameField.getText().trim();
            if(cnic.isEmpty() || name.isEmpty()) { 
                JOptionPane.showMessageDialog(this,"Fields cannot be empty!"); 
                return; 
            }
            if(cnic.length() != 13) { 
                JOptionPane.showMessageDialog(this,"CNIC must be 13 digits!"); 
                return; 
            }

            Voter v = manager.login(cnic, name);
            if(v == null) JOptionPane.showMessageDialog(this,"Invalid CNIC or Name!");
            else new VotingPage(v, manager);
        }
    }

    // Show registration dialog
    private void showRegister() {
        JTextField cnicField = new JTextField();
        JTextField nameField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JTextField ageField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(4, 2)); 
        panel.add(new JLabel("Enter CNIC:"));
        panel.add(cnicField);
        panel.add(new JLabel("Enter Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Enter Password:"));
        panel.add(passField);
        panel.add(new JLabel("Enter Age:"));
        panel.add(ageField);

        while(true) {
            int option = JOptionPane.showConfirmDialog(this, panel, "Voter Registration", JOptionPane.OK_CANCEL_OPTION);
            if(option != JOptionPane.OK_OPTION) break;

            String cnic = cnicField.getText().trim();
            String name = nameField.getText().trim();
            String pass = String.valueOf(passField.getPassword());
            String ageText = ageField.getText().trim();

            // Validation checks
            if(cnic.isEmpty() || name.isEmpty() || pass.isEmpty() || ageText.isEmpty()) {
                JOptionPane.showMessageDialog(this,"All fields must be filled!");
                continue;
            }
            if(!cnic.matches("\\d{13}")) {
                JOptionPane.showMessageDialog(this,"CNIC must be exactly 13 digits!");
                continue;
            }
            if(!name.matches("[a-zA-Z ]+")) {
                JOptionPane.showMessageDialog(this,"Name must contain only alphabets!");
                continue;
            }
            if(pass.length() < 6) {
                JOptionPane.showMessageDialog(this,"Password must be at least 6 characters!");
                continue;
            }
            int age;
            try { age = Integer.parseInt(ageText); }
            catch(NumberFormatException e) { 
                JOptionPane.showMessageDialog(this,"Age must be a valid number!");
                continue; 
            }
            if(age < 18) {
                JOptionPane.showMessageDialog(this,"You must be 18 or older to vote!");
                continue;
            }

            Voter v = new Voter(cnic, name, pass, age, false);
            if(manager.register(v)) {
                JOptionPane.showMessageDialog(this,"Registration successful!");
                break;
            } else {
                JOptionPane.showMessageDialog(this,"CNIC already registered!");
            }
        }
    }
}

// Voting page GUI
class VotingPage extends JFrame {
    public VotingPage(Voter voter, VoterManager manager) {
        setTitle("Vote — " + voter.getName());
        setSize(400,300);
        setLayout(new GridLayout(0,1));

        try {
            if(voter.hasVoted()) { JOptionPane.showMessageDialog(this,"You already voted!"); dispose(); return; }

            List<Candidate> candidates = FileService.loadCandidates();
            if(candidates.isEmpty()) { JOptionPane.showMessageDialog(this,"No candidates available."); dispose(); return; }

            // Create a button for each candidate
            for(Candidate c : candidates) {
                JButton b = new JButton(c.getName() + " (" + c.getParty() + ")");
                b.addActionListener(e -> {
                    try {
                        // Save vote to file
                        FileService.saveVote(voter.getCnic(), c.getId());
                        voter.setVoted(true); 
                        manager.updateVoter(voter);
                        JOptionPane.showMessageDialog(this,"Vote cast for: "+c.getName());
                        dispose();
                    } catch(Exception ex) { JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
                });
                add(b);
            }
        } catch(Exception e) { JOptionPane.showMessageDialog(this,"Error loading candidates."); }

        setVisible(true);
    }
}

//=====================
// MAIN CLASS
//=====================
public class EVotingSystemFinal {
    public static void main(String[] args) {
        // Launch main menu
        new MainMenu();
    }
}

