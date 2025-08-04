import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

class SoundPlayer {
    private Clip backgroundClip;

    public void playMenuMusic() {
        playLoopingSound("main_tune.wav");
    }

    public void playStadiumSound() {
        playLoopingSound("stadium_crowd.wav");
    }

    public void playGoalSound(int score) {
        playSoundEffect("goal_crowd.wav");
        switch (score) {
            case 1:
                playSoundEffect("goal.wav");
                break;
            case 2:
                playSoundEffect("what_a_goal.wav");
                break;
            default:
                playSoundEffect("unstoppable.wav");
                break;
        }
    }

    public void playPassSound(int passCount) {
        playSoundEffect("pass.wav");
    }

    public void playShootSound() {
        playSoundEffect("shoot.wav");
    }

    public void playMissSound() {
    }

    public void playSaveSound() {
    }


    private void playLoopingSound(String filePath) {
        stopMusic();
        try {
            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(audioStream);
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundClip.start();
        } catch (Exception e) {
            System.err.println("Error playing looping sound: " + filePath);
            e.printStackTrace();
        }
    }

    private void playSoundEffect(String filePath) {
        try {
            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip effectClip = AudioSystem.getClip();
            effectClip.open(audioStream);
            effectClip.start();
        } catch (Exception e) {
            System.err.println("Error playing sound effect: " + filePath);
            e.printStackTrace();
        }
    }

    public void stopMusic() {
        if (backgroundClip != null) {
            backgroundClip.stop();
            backgroundClip.close();
            backgroundClip = null;
        }
    }
}

public class FootballGame {
    public static void main(String[] args) {
        final SoundPlayer soundPlayer = new SoundPlayer();
        soundPlayer.playMenuMusic();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2D Soccer Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            MatchHistory matchHistory = new MatchHistory();

            MainMenuPanel mainMenu = new MainMenuPanel(matchHistory, () -> cardLayout.show(mainPanel, "difficulty"), () -> System.exit(0));

            DifficultyPanel difficultyPanel = new DifficultyPanel(
                    () -> cardLayout.show(mainPanel, "team_selection"),
                    () -> cardLayout.show(mainPanel, "menu")
            );

            GamePanel gamePanel = new GamePanel(frame, soundPlayer, matchHistory, () -> {
                soundPlayer.playMenuMusic();
                cardLayout.show(mainPanel, "menu");
            });

            TeamSelectionPanel teamSelectionPanel = new TeamSelectionPanel((userTeam, aiTeam) -> {
                soundPlayer.playStadiumSound();
                cardLayout.show(mainPanel, "game");
                gamePanel.startGame(userTeam, aiTeam);
            }, () -> cardLayout.show(mainPanel, "difficulty"));

            mainPanel.add(mainMenu, "menu");
            mainPanel.add(difficultyPanel, "difficulty");
            mainPanel.add(teamSelectionPanel, "team_selection");
            mainPanel.add(gamePanel, "game");

            frame.add(mainPanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

class RoundedBorder implements Border {
    private int radius;
    RoundedBorder(int radius) {
        this.radius = radius;
    }
    public Insets getBorderInsets(Component c) {
        return new Insets(this.radius+1, this.radius+1, this.radius+2, this.radius);
    }
    public boolean isBorderOpaque() {
        return true;
    }
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        g.drawRoundRect(x, y, width-1, height-1, radius, radius);
    }
}

class RoundedGradientPanel extends JPanel {
    private Color color1;
    private Color color2;

    public RoundedGradientPanel(LayoutManager layout, Color c1, Color c2) {
        super(layout);
        this.color1 = c1;
        this.color2 = c2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
        g2d.setPaint(gp);
        g2d.fill(new RoundRectangle2D.Float(0, 0, w, h, 25, 25));
    }
}

class CustomMessageDialog extends JDialog {
    public CustomMessageDialog(JFrame parent, String title, String message) {
        super(parent, title, true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        RoundedGradientPanel mainPanel = new RoundedGradientPanel(new GridBagLayout(), new Color(25, 25, 112), new Color(0, 0, 50));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        messageLabel.setForeground(Color.WHITE);
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 20, 30, 20);
        mainPanel.add(messageLabel, gbc);

        JButton okButton = new JButton("Start");
        styleButton(okButton, new Color(60, 179, 113), new Color(46, 139, 87));
        okButton.addActionListener(e -> dispose());
        gbc.gridy = 1;
        gbc.insets = new Insets(10, 20, 20, 20);
        mainPanel.add(okButton, gbc);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(parent);
    }

    private void styleButton(JButton button, Color normal, Color hover) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(normal);
        button.setFocusPainted(false);
        button.setBorder(new RoundedBorder(15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(hover);
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(normal);
            }
        });
    }
}

class MatchHistoryDialog extends JDialog {
    public MatchHistoryDialog(JFrame parent, MatchHistory matchHistory) {
        super(parent, "Match History", true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        RoundedGradientPanel mainPanel = new RoundedGradientPanel(new GridBagLayout(), new Color(25, 25, 112), new Color(0, 0, 50));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel titleLabel = new JLabel("Match History");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(10, 20, 20, 20);
        mainPanel.add(titleLabel, gbc);

        JTextArea historyArea = new JTextArea(matchHistory.getHistoryAsString());
        historyArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        historyArea.setEditable(false);
        historyArea.setOpaque(false);
        historyArea.setForeground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(historyArea);
        scrollPane.setPreferredSize(new Dimension(400, 250));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 180), 1));
        
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        mainPanel.add(scrollPane, gbc);
        
        JButton closeButton = new JButton("Close");
        styleButton(closeButton, new Color(220, 20, 60), new Color(178, 34, 34));
        closeButton.addActionListener(e -> dispose());
        
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0;
        gbc.insets = new Insets(20, 20, 10, 20);
        mainPanel.add(closeButton, gbc);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void styleButton(JButton button, Color normal, Color hover) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(normal);
        button.setFocusPainted(false);
        button.setBorder(new RoundedBorder(15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(hover);
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(normal);
            }
        });
    }
}


class CustomGameOverDialog extends JDialog {
    public CustomGameOverDialog(JFrame parent, String title, String line1, String line2, Runnable onYes) {
        super(parent, title, true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0)); 
        RoundedGradientPanel mainPanel = new RoundedGradientPanel(new GridBagLayout(), new Color(25, 25, 112), new Color(0, 0, 50));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel line1Label = new JLabel(line1);
        line1Label.setFont(new Font("Segoe UI", Font.BOLD, 36));
        line1Label.setForeground(Color.WHITE);
        line1Label.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(10, 20, 10, 20);
        mainPanel.add(line1Label, gbc);

        JLabel line2Label = new JLabel(line2);
        line2Label.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        line2Label.setForeground(Color.LIGHT_GRAY);
        line2Label.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 1;
        gbc.insets = new Insets(10, 20, 30, 20);
        mainPanel.add(line2Label, gbc);

        JLabel promptLabel = new JLabel("Return to Main Menu?");
        promptLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        promptLabel.setForeground(Color.WHITE);
        promptLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 2;
        gbc.insets = new Insets(10, 20, 10, 20);
        mainPanel.add(promptLabel, gbc);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        buttonPanel.setOpaque(false);
        
        JButton yesButton = new JButton("Yes");
        JButton noButton = new JButton("No");

        styleButton(yesButton, new Color(60, 179, 113), new Color(46, 139, 87));
        styleButton(noButton, new Color(220, 20, 60), new Color(178, 34, 34));
        
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        
        gbc.gridy = 3;
        gbc.insets = new Insets(10, 20, 20, 20);
        mainPanel.add(buttonPanel, gbc);
        
        yesButton.addActionListener(e -> {
            dispose();
            onYes.run();
        });

        noButton.addActionListener(e -> System.exit(0));

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(parent);
    }

    private void styleButton(JButton button, Color normal, Color hover) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(normal);
        button.setFocusPainted(false);
        button.setBorder(new RoundedBorder(15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(hover);
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(normal);
            }
        });
    }
}

class MatchResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String team1Name;
    private final int team1Score;
    private final String team2Name;
    private final int team2Score;
    private final boolean isPenalty;
    private final int penalty1;
    private final int penalty2;

    public MatchResult(String team1Name, int team1Score, String team2Name, int team2Score) {
        this(team1Name, team1Score, team2Name, team2Score, false, 0, 0);
    }
    public MatchResult(String team1Name, int team1Score, String team2Name, int team2Score, boolean isPenalty, int p1, int p2) {
        this.team1Name = team1Name;
        this.team1Score = team1Score;
        this.team2Name = team2Name;
        this.team2Score = team2Score;
        this.isPenalty = isPenalty;
        this.penalty1 = p1;
        this.penalty2 = p2;
    }

    @Override
    public String toString() {
        String formattedTeam1 = team1Name.substring(0, 1).toUpperCase() + team1Name.substring(1).toLowerCase();
        String formattedTeam2 = team2Name.substring(0, 1).toUpperCase() + team2Name.substring(1).toLowerCase();
        if (isPenalty) {
             return String.format("%-10s %d (%d) - (%d) %d %s", formattedTeam1, team1Score, penalty1, penalty2, team2Score, formattedTeam2);
        } else {
             return String.format("%-10s %d - %d %s", formattedTeam1, team1Score, team2Score, formattedTeam2);
        }
    }
}

class MatchHistory {
    private List<MatchResult> results;
    private static final String HISTORY_FILE = "match_history.txt";

    public MatchHistory() {
        loadHistory();
    }

    public void addResult(MatchResult result) {
        if (results == null) {
            results = new ArrayList<>();
        }
        results.add(result);
        saveHistory();
    }

    @SuppressWarnings("unchecked")
    private void loadHistory() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(HISTORY_FILE))) {
            results = (List<MatchResult>) ois.readObject();
        } catch (FileNotFoundException e) {
            results = new ArrayList<>();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading match history: " + e.getMessage());
            results = new ArrayList<>();
        }
    }
    private void saveHistory() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HISTORY_FILE))) {
            oos.writeObject(results);
        } catch (IOException e) {
            System.err.println("Error saving match history: " + e.getMessage());
        }
    }

    public String getHistoryAsString() {
        if (results == null || results.isEmpty()) {
            return "No matches have been played yet.";
        }
        List<MatchResult> reversedResults = new ArrayList<>(results);
        Collections.reverse(reversedResults);
        return reversedResults.stream()
                .map(MatchResult::toString)
                .collect(Collectors.joining("\n"));
    }
}

enum Team {
    ARGENTINA("ARG"),
    BRAZIL("BRA"),
    PORTUGAL("POR"),
    GERMANY("GER");

    private final String abbreviation;

    Team(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return this.abbreviation;
    }
}

class MainMenuPanel extends JPanel {
    private BufferedImage backgroundImage;
    private final MatchHistory matchHistory;

    public MainMenuPanel(MatchHistory matchHistory, Runnable onStart, Runnable onExit) {
        this.matchHistory = matchHistory;
        setPreferredSize(new Dimension(GamePanel.WIDTH, GamePanel.HEIGHT));
        setLayout(new GridBagLayout());
        try {
            backgroundImage = ImageIO.read(new File("background.jpg"));
        } catch (IOException e) {
            System.err.println("Background image not found: background.jpg");
            setBackground(new Color(0, 50, 0));
        }
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        JButton startButton = createStyledButton("Start Game");
        JButton optionsButton = createStyledButton("Options");
        JButton historyButton = createStyledButton("Match History");
        JButton exitButton = createStyledButton("Exit");
        startButton.addActionListener(e -> onStart.run());
        optionsButton.addActionListener(e -> showOptions());
        historyButton.addActionListener(e -> showHistory());
        exitButton.addActionListener(e -> onExit.run());
        add(startButton, gbc);
        add(optionsButton, gbc);
        add(historyButton, gbc);
        add(exitButton, gbc);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 24));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(0, 100, 0));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return button;
    }

    private void showOptions() {
        String optionsText = "<html><h2>Controls</h2>" +
                "<p><b>Player 1:</b></p>" +
                "<ul>" +
                "<li><b>Arrow Keys:</b> Move</li>" +
                "<li><b>Shift:</b> Shoot</li>" +
                "<li><b>Control:</b> Pass</li>" +
                "</ul>" +
                "<h3>Penalty Shootout</h3>" +
                "<ul>" +
                "<li><b>Aiming:</b> Arrow Keys (Left/Right/Up)</li>" +
                "<li><b>Shooting:</b> Shift</li>" +
                "<li><b>Goalkeeping:</b> Arrow Keys (Left/Right/Up) to choose dive direction</li>" +
                "</ul>" +
                "<p><b>Player 2 (AI):</b></p>" +
                "<ul>" +
                "<li>Controlled by the computer.</li>" +
                "</ul></html>";
        JOptionPane.showMessageDialog(this, optionsText, "Options", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showHistory() {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        MatchHistoryDialog dialog = new MatchHistoryDialog(parentFrame, matchHistory);
        dialog.setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}

class DifficultyPanel extends JPanel {
    private BufferedImage backgroundImage;

    public DifficultyPanel(Runnable onDifficultySelected, Runnable onBack) {
        setPreferredSize(new Dimension(GamePanel.WIDTH, GamePanel.HEIGHT));
        setLayout(new GridBagLayout());
        try {
            backgroundImage = ImageIO.read(new File("background.jpg"));
        } catch (IOException e) {
            System.err.println("Background image not found: background.jpg");
            setBackground(new Color(0, 50, 0));
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);

        JButton easyButton = createStyledButton("Easy");
        JButton mediumButton = createStyledButton("Medium");
        JButton hardButton = createStyledButton("Hard");
        JButton backButton = createStyledButton("Back");

        easyButton.addActionListener(e -> {
            GamePanel.USER_SPEED = 1.0;
            GamePanel.AI_BASE_SPEED = 0.5;
            onDifficultySelected.run();
        });
        mediumButton.addActionListener(e -> {
            GamePanel.USER_SPEED = 1.0;
            GamePanel.AI_BASE_SPEED = 0.8;
            onDifficultySelected.run();
        });
        hardButton.addActionListener(e -> {
            GamePanel.USER_SPEED = 1.2;
            GamePanel.AI_BASE_SPEED = 1.5;
            onDifficultySelected.run();
        });
        backButton.addActionListener(e -> onBack.run());

        add(easyButton, gbc);
        add(mediumButton, gbc);
        add(hardButton, gbc);
        gbc.insets = new Insets(40, 0, 10, 0);
        add(backButton, gbc);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 24));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(0, 100, 0));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return button;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}


@FunctionalInterface
interface GameStarter {
    void startGame(Team userTeam, Team aiTeam);
}

class TeamSelectionPanel extends JPanel {
    private Team userTeam;
    private Team aiTeam;
    private final Map<Team, JButton> userButtons = new HashMap<>();
    private final Map<Team, JButton> aiButtons = new HashMap<>();
    private final JButton startMatchButton;

    public TeamSelectionPanel(GameStarter gameStarter, Runnable onBack) {
        setLayout(new BorderLayout());
        setBackground(new Color(10, 20, 10));

        JLabel titleLabel = new JLabel("Team Selection", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(titleLabel, BorderLayout.NORTH);
        JPanel selectionGrid = new JPanel(new GridLayout(2, 1, 0, 20));
        selectionGrid.setOpaque(false);
        selectionGrid.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        add(selectionGrid, BorderLayout.CENTER);
        selectionGrid.add(createSelectionPanel("Select Your Team", userButtons, true));
        selectionGrid.add(createSelectionPanel("Select AI Team", aiButtons, false));
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        bottomPanel.setOpaque(false);
        add(bottomPanel, BorderLayout.SOUTH);
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> onBack.run());
        bottomPanel.add(backButton);
        startMatchButton = new JButton("Start Match");
        startMatchButton.setEnabled(false);
        startMatchButton.addActionListener(e -> gameStarter.startGame(userTeam, aiTeam));
        bottomPanel.add(startMatchButton);
        addUserTeamButtonListeners();
        addAiTeamButtonListeners();
    }

    private JPanel createSelectionPanel(String title, Map<Team, JButton> buttonMap, boolean isUser) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE),
                title,
                0, 0,
                new Font("Arial", Font.BOLD, 24),
                Color.WHITE
        ));

        JPanel flagsPanel = new JPanel(new GridLayout(1, 4, 15, 15));
        flagsPanel.setOpaque(false);
        flagsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(flagsPanel, BorderLayout.CENTER);

        for (Team team : Team.values()) {
            try {
                String path = team.name().toLowerCase() + "_flag.png";
                ImageIcon icon = new ImageIcon(new ImageIcon(path).getImage().getScaledInstance(150, 100, Image.SCALE_SMOOTH));
                JButton button = new JButton(icon);
                button.setOpaque(false);
                button.setContentAreaFilled(false);
                button.setBorder(new LineBorder(Color.GRAY, 2));
                flagsPanel.add(button);
                buttonMap.put(team, button);
            } catch (Exception e) {
                System.err.println("Could not load flag: " + team.name().toLowerCase() + "_flag.png");
                JButton button = new JButton(team.name());
                flagsPanel.add(button);
                buttonMap.put(team, button);
            }
        }
        return panel;
    }

    private void addUserTeamButtonListeners() {
        for (Map.Entry<Team, JButton> entry : userButtons.entrySet()) {
            entry.getValue().addActionListener(e -> {
                userTeam = entry.getKey();
                userButtons.forEach((team, button) -> button.setBorder(new LineBorder(Color.GRAY, 2)));
                entry.getValue().setBorder(new LineBorder(Color.CYAN, 4));
                aiButtons.forEach((team, button) -> button.setEnabled(true));
                aiButtons.get(userTeam).setEnabled(false);
                if (aiTeam == userTeam) {
                    aiTeam = null;
                    aiButtons.forEach((team, button) -> button.setBorder(new LineBorder(Color.GRAY, 2)));
                }
                checkIfReadyToStart();
            });
        }
    }

    private void addAiTeamButtonListeners() {
        for (Map.Entry<Team, JButton> entry : aiButtons.entrySet()) {
            entry.getValue().addActionListener(e -> {
                aiTeam = entry.getKey();
                aiButtons.forEach((team, button) -> button.setBorder(new LineBorder(Color.GRAY, 2)));
                entry.getValue().setBorder(new LineBorder(Color.ORANGE, 4));
                checkIfReadyToStart();
            });
        }
    }

    private void checkIfReadyToStart() {
        startMatchButton.setEnabled(userTeam != null && aiTeam != null);
    }
}

class GamePanel extends JPanel implements Runnable {
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    private static final int PLAYER_SIZE = 25;
    private static final int BALL_SIZE = 12;
    private static final int MATCH_DURATION_SECONDS = 300;
    public static final int PLAYABLE_X = 170;
    public static final int PLAYABLE_Y = 100;
    public static final int PLAYABLE_WIDTH = WIDTH - 345;
    public static final int PLAYABLE_HEIGHT = HEIGHT - 155;

    public static double USER_SPEED = 1.0;
    public static double AI_BASE_SPEED = 0.8;

    public enum GameState { KICK_OFF, RUNNING, GAME_OVER, PENALTY_SHOOTOUT }
    private enum PenaltyState { AIMING, KICKING, RESULT }
    private enum ShotDirection { LEFT, CENTER, RIGHT }
    private GameState gameState;
    private PenaltyState penaltyState;
    private ShotDirection shotDirection;
    private ShotDirection userDiveDirection;
    private String kickOffTakerTeamName;
    private long lastTickTime;

    private Thread gameThread;
    private boolean running = false;
    private volatile boolean paused = false;
    private JPanel pauseMenuPanel;
    private JButton pauseButton;
    private final JFrame parentFrame;
    private final SoundPlayer soundPlayer;
    private final Runnable onGameEnd;
    private final MatchHistory matchHistory;
    private final Random random = new Random();

    private Team userTeam;
    private Team aiTeam;
    private BufferedImage fieldImage;
    private BufferedImage ballImage;
    private BufferedImage userGkPenaltySprite;
    private Ball ball;
    private List<Player> team1;
    private List<Player> team2;
    private List<Player> allPlayers;
    private int scoreTeam1 = 0;
    private int scoreTeam2 = 0;
    private int penaltyScoreTeam1 = 0;
    private int penaltyScoreTeam2 = 0;
    private int penaltyKicksTeam1 = 0;
    private int penaltyKicksTeam2 = 0;
    private boolean isUserTurnToShoot = true;
    private String penaltyMessage = "";
    private long penaltyMessageStartTime = 0;
    private int passCountTeam1 = 0;
    private int passCountTeam2 = 0;
    private String lastPassingTeam = "";
    private int remainingSeconds;
    private long goalMessageStartTime = 0;
    private Player penaltyKicker;
    private Player penaltyGoalkeeper;


    public GamePanel(JFrame parentFrame, SoundPlayer soundPlayer, MatchHistory matchHistory, Runnable onGameEnd) {
        this.parentFrame = parentFrame;
        this.soundPlayer = soundPlayer;
        this.matchHistory = matchHistory;
        this.onGameEnd = onGameEnd;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(new KeyInputHandler());
        setLayout(null);

        pauseButton = new JButton("Pause");
        pauseButton.setBounds(WIDTH - 130, 10, 100, 40);
        pauseButton.setFont(new Font("Arial", Font.BOLD, 16));
        pauseButton.setFocusable(false);
        pauseButton.addActionListener(e -> pauseGame());
        add(pauseButton);

        createPauseMenu(onGameEnd);

        try {
            fieldImage = ImageIO.read(new File("field.png"));
            ballImage = ImageIO.read(new File("ball.png"));
        } catch (IOException e) {
            System.err.println("Could not load image: " + e.getMessage());
            setBackground(new Color(0, 128, 0));
        }
    }

    private void createPauseMenu(Runnable onGameEnd) {
        pauseMenuPanel = new JPanel(new GridBagLayout());
        pauseMenuPanel.setBounds(WIDTH / 2 - 200, HEIGHT / 2 - 150, 400, 300);
        pauseMenuPanel.setBackground(new Color(0, 0, 0, 180));
        pauseMenuPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        pauseMenuPanel.setVisible(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 0, 10, 0);

        JButton resumeButton = new JButton("Resume");
        resumeButton.setFont(new Font("Arial", Font.BOLD, 24));
        resumeButton.addActionListener(e -> resumeGame());
        pauseMenuPanel.add(resumeButton, gbc);

        JButton backButton = new JButton("Back to Menu");
        backButton.setFont(new Font("Arial", Font.BOLD, 24));
        backButton.addActionListener(e -> {
            running = false;
            onGameEnd.run();
        });
        pauseMenuPanel.add(backButton, gbc);
        add(pauseMenuPanel);
    }

    public void pauseGame() {
        if (gameState != GameState.PENALTY_SHOOTOUT) {
             paused = true;
             repaint();
        }
    }

    public void resumeGame() {
        paused = false;
        requestFocusInWindow();
    }


    public void startGame(Team userTeam, Team aiTeam) {
        this.userTeam = userTeam;
        this.aiTeam = aiTeam;
        if (running) return;
       
        paused = false;
        initializeGame(userTeam, aiTeam);
        scoreTeam1 = 0;
        scoreTeam2 = 0;
        penaltyScoreTeam1 = 0;
        penaltyScoreTeam2 = 0;
        penaltyKicksTeam1 = 0;
        penaltyKicksTeam2 = 0;
        passCountTeam1 = 0;
        passCountTeam2 = 0;
        lastPassingTeam = "";
        this.remainingSeconds = MATCH_DURATION_SECONDS;
        
        this.penaltyMessage = "";
        this.penaltyState = null;

        this.kickOffTakerTeamName = "Team 1"; 
        resetPositions();

        running = true;
        gameThread = new Thread(this);
        gameThread.start();
        requestFocusInWindow();
    }

    private BufferedImage loadSprite(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            System.err.println("Could not load sprite: " + path);
            return null;
        }
    }

    private void initializeGame(Team userTeam, Team aiTeam) {
        String userPlayerPath = userTeam.name().toLowerCase() + "_player.png";
        String userGkPath = "gk.png";
        String aiPlayerPath = "ai_" + aiTeam.name().toLowerCase() + ".png";
        String aiGkPath = "ai_gk.png";
        userGkPenaltySprite = loadSprite("gk_penalty.png");
        BufferedImage userPlayerSprite = loadSprite(userPlayerPath);
        BufferedImage userGkSprite = loadSprite(userGkPath);
        BufferedImage aiPlayerSprite = loadSprite(aiPlayerPath);
        BufferedImage aiGkSprite = loadSprite(aiGkPath);
        ball = new Ball(0, 0, BALL_SIZE, ballImage);
        team1 = new ArrayList<>();
        team2 = new ArrayList<>();
        allPlayers = new ArrayList<>();
        team1.add(new Player(0, 0, PLAYER_SIZE, userPlayerSprite, Color.CYAN, "Team 1", Player.PlayerRole.STRIKER, this));
        team1.add(new AIPlayer(0, 0, PLAYER_SIZE, userGkSprite, Color.CYAN, "Team 1", Player.PlayerRole.GOALKEEPER, this));
        team1.add(new AIPlayer(0, 0, PLAYER_SIZE, userPlayerSprite, Color.CYAN, "Team 1", Player.PlayerRole.DEFENDER, this));
        team1.add(new AIPlayer(0, 0, PLAYER_SIZE, userPlayerSprite, Color.CYAN, "Team 1", Player.PlayerRole.DEFENDER, this));
        team1.add(new AIPlayer(0, 0, PLAYER_SIZE, userPlayerSprite, Color.CYAN, "Team 1", Player.PlayerRole.MIDFIELDER, this));
        team1.add(new AIPlayer(0, 0, PLAYER_SIZE, userPlayerSprite, Color.CYAN, "Team 1", Player.PlayerRole.MIDFIELDER, this));
        team2.add(new AIPlayer(0, 0, PLAYER_SIZE, aiPlayerSprite, Color.ORANGE, "Team 2", Player.PlayerRole.STRIKER, this));
        team2.add(new AIPlayer(0, 0, PLAYER_SIZE, aiGkSprite, Color.ORANGE, "Team 2", Player.PlayerRole.GOALKEEPER, this));
        team2.add(new AIPlayer(0, 0, PLAYER_SIZE, aiPlayerSprite, Color.ORANGE, "Team 2", Player.PlayerRole.DEFENDER, this));
        team2.add(new AIPlayer(0, 0, PLAYER_SIZE, aiPlayerSprite, Color.ORANGE, "Team 2", Player.PlayerRole.DEFENDER, this));
        team2.add(new AIPlayer(0, 0, PLAYER_SIZE, aiPlayerSprite, Color.ORANGE, "Team 2", Player.PlayerRole.MIDFIELDER, this));
        team2.add(new AIPlayer(0, 0, PLAYER_SIZE, aiPlayerSprite, Color.ORANGE, "Team 2", Player.PlayerRole.MIDFIELDER, this));
        allPlayers.addAll(team1);
        allPlayers.addAll(team2);
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;
        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            if (delta >= 1) {
                update();
                delta--;
            }
            repaint();
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void update() {
        if (paused) return;

        if (gameState == GameState.PENALTY_SHOOTOUT) {
            updatePenaltyShootout();
            return;
        }

        if (isGameOver()) {
            gameState = GameState.GAME_OVER;
            running = false;
            showGameOverDialog();
            return;
        }

        allPlayers.forEach(Player::move);
        ball.move();
        checkAllCollisions();

        if (gameState == GameState.RUNNING) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTickTime >= 1000) {
                if (remainingSeconds > 0) {
                    remainingSeconds--;
                }
                lastTickTime = currentTime;
            }
            checkGoal();
        }
    }

    private void checkAllCollisions() {
        ball.checkWallCollision(PLAYABLE_X, PLAYABLE_X + PLAYABLE_WIDTH, PLAYABLE_Y, PLAYABLE_Y + PLAYABLE_HEIGHT);
        allPlayers.forEach(p -> p.checkCollisions(ball, allPlayers));
    }
    
    private void checkGoal() {
        int goalHeight = 100;
        Line2D.Double leftGoalLine = new Line2D.Double(PLAYABLE_X, PLAYABLE_Y + (PLAYABLE_HEIGHT - goalHeight) / 2.0, PLAYABLE_X, PLAYABLE_Y + (PLAYABLE_HEIGHT + goalHeight) / 2.0);
        Line2D.Double rightGoalLine = new Line2D.Double(PLAYABLE_X + PLAYABLE_WIDTH, PLAYABLE_Y + (PLAYABLE_HEIGHT - goalHeight) / 2.0, PLAYABLE_X + PLAYABLE_WIDTH, PLAYABLE_Y + (PLAYABLE_HEIGHT + goalHeight) / 2.0);
        Rectangle2D.Double ballBounds = ball.getBounds();
        if (leftGoalLine.intersects(ballBounds)) {
            scoreTeam2++;
            soundPlayer.playGoalSound(scoreTeam2);
            goalMessageStartTime = System.currentTimeMillis();
            this.kickOffTakerTeamName = "Team 1";
            resetPositions();
        } else if (rightGoalLine.intersects(ballBounds)) {
            scoreTeam1++;
            soundPlayer.playGoalSound(scoreTeam1);
            goalMessageStartTime = System.currentTimeMillis();
            this.kickOffTakerTeamName = "Team 2";
            resetPositions();
        }
    }

    private void resetPositions() {
        this.gameState = GameState.KICK_OFF;
        ball.setVelocity(0, 0);
        ball.setDribbler(null);
        passCountTeam1 = 0;
        passCountTeam2 = 0;
        lastPassingTeam = "";

        if (allPlayers != null) {
            allPlayers.forEach(p -> p.isDribbling = false);
        }

        setFormation(team1, 1);
        setFormation(team2, 2);

        if (kickOffTakerTeamName == null) return;

        List<Player> kickOffTeamList = kickOffTakerTeamName.equals("Team 1") ? team1 : team2;
        Player kickOffPlayer = kickOffTeamList.stream()
                .filter(p -> p.getRole() == Player.PlayerRole.STRIKER)
                .findFirst()
                .orElse(kickOffTeamList.isEmpty() ? null : kickOffTeamList.get(0));

        if (kickOffPlayer == null) return;

        if (kickOffTakerTeamName.equals("Team 1")) {
            Player currentHuman = team1.stream().filter(p -> !(p instanceof AIPlayer)).findFirst().orElse(null);
            if (currentHuman != kickOffPlayer && kickOffPlayer instanceof AIPlayer) {
                switchControlToPlayer(kickOffPlayer);
                kickOffPlayer = team1.stream().filter(p -> !(p instanceof AIPlayer)).findFirst().orElse(null);
            }
        }
        
        if (kickOffPlayer == null) return;

        double centerX = PLAYABLE_X + PLAYABLE_WIDTH / 2.0;
        double centerY = PLAYABLE_Y + PLAYABLE_HEIGHT / 2.0;
        kickOffPlayer.setPosition(centerX - kickOffPlayer.size / 2.0, centerY - kickOffPlayer.size / 2.0);
        
        if (kickOffPlayer.getTeam().equals("Team 1")) {
            kickOffPlayer.setDirection(-1, 0);
        } else {
            kickOffPlayer.setDirection(1, 0);
        }

        ball.setDribbler(kickOffPlayer);
        kickOffPlayer.isDribbling = true;
        kickOffPlayer.possessionStartTime = System.currentTimeMillis();
        kickOffPlayer.move();
    }

    private void setFormation(List<Player> team, int side) {
        if (team == null || team.isEmpty()) return;
        double xBase = (side == 1) ? PLAYABLE_X + PLAYABLE_WIDTH / 4.0 : PLAYABLE_X + 3 * PLAYABLE_WIDTH / 4.0;
        List<Player> defenders = team.stream().filter(p -> p.getRole() == Player.PlayerRole.DEFENDER).collect(Collectors.toList());
        List<Player> midfielders = team.stream().filter(p -> p.getRole() == Player.PlayerRole.MIDFIELDER).collect(Collectors.toList());
        for (Player p : team) {
            double homeX = 0, homeY = 0;
            switch (p.getRole()) {
                case GOALKEEPER:
                    homeX = (side == 1) ? PLAYABLE_X + 50 : PLAYABLE_X + PLAYABLE_WIDTH - 50 - PLAYER_SIZE;
                    homeY = PLAYABLE_Y + PLAYABLE_HEIGHT / 2.0 - PLAYER_SIZE / 2.0;
                    break;
                case DEFENDER:
                    homeX = xBase - (side == 1 ? 100 : -100);
                    homeY = PLAYABLE_Y + ((defenders.indexOf(p) == 0) ? PLAYABLE_HEIGHT / 4.0 : 3 * PLAYABLE_HEIGHT / 4.0);
                    break;
                case MIDFIELDER:
                    homeX = xBase;
                    homeY = PLAYABLE_Y + ((midfielders.indexOf(p) == 0) ? PLAYABLE_HEIGHT / 4.0 + 40 : 3 * PLAYABLE_HEIGHT / 4.0 - 40);
                    break;
                case STRIKER:
                    homeX = xBase + (side == 1 ? 120 : -120);
                    homeY = PLAYABLE_Y + PLAYABLE_HEIGHT / 2.0 - PLAYER_SIZE / 2.0;
                    break;
            }
            p.setPosition(homeX, homeY);
            if (p instanceof AIPlayer) {
                ((AIPlayer) p).setHomePosition(homeX, homeY);
            }
        }
    }

    public void switchControlToPlayer(Player newHumanController) {
        if (!(newHumanController instanceof AIPlayer)) { return; }
        Player currentHuman = team1.stream().filter(p -> !(p instanceof AIPlayer)).findFirst().orElse(null);
        if (currentHuman == null || newHumanController == currentHuman) { return; }

        AIPlayer targetAI = (AIPlayer) newHumanController;

        int humanIndexInTeam1 = team1.indexOf(currentHuman);
        int aiIndexInTeam1 = team1.indexOf(targetAI);
        int humanIndexInAll = allPlayers.indexOf(currentHuman);
        int aiIndexInAll = allPlayers.indexOf(targetAI);

        Player newHuman = new Player(targetAI);
        newHuman.setVelX(0);
        newHuman.setVelY(0);

        AIPlayer newAI = new AIPlayer(currentHuman, targetAI.getHomeX(), targetAI.getHomeY());
        newAI.setVelX(0);
        newAI.setVelY(0);

        if (humanIndexInTeam1 != -1 && aiIndexInTeam1 != -1) {
            team1.set(humanIndexInTeam1, newAI);
            team1.set(aiIndexInTeam1, newHuman);
        }
        if (humanIndexInAll != -1 && aiIndexInAll != -1) {
            allPlayers.set(humanIndexInAll, newAI);
            allPlayers.set(aiIndexInAll, newHuman);
        }
    }
    
    public void transitionToRunningState() {
        if (this.gameState == GameState.KICK_OFF) {
            this.gameState = GameState.RUNNING;
            this.lastTickTime = System.currentTimeMillis();
        }
    }

    public void handlePass(String teamName) {
        if (!teamName.equals(lastPassingTeam)) {
            passCountTeam1 = 0;
            passCountTeam2 = 0;
            this.lastPassingTeam = teamName;
        }

        if ("Team 1".equals(teamName)) {
            passCountTeam1++;
            soundPlayer.playPassSound(passCountTeam1);
        } else if ("Team 2".equals(teamName)) {
            passCountTeam2++;
            soundPlayer.playPassSound(passCountTeam2);
        }
    }

    public SoundPlayer getSoundPlayer() {
        return this.soundPlayer;
    }

    public Ball getBall() { return ball; }
    public List<Player> getTeam(String teamName) { return teamName.equals("Team 1") ? team1 : team2; }
    private boolean isGameOver() { return remainingSeconds <= 0; }
    public GameState getGameState() { return gameState; }

    private void showGameOverDialog() {
        SwingUtilities.invokeLater(() -> {
            if (scoreTeam1 == scoreTeam2) {
                javax.swing.Timer timer = new javax.swing.Timer(2000, e -> {
                    CustomMessageDialog dialog = new CustomMessageDialog(parentFrame, "Shootout", "Penalty Shootout Starts Now");
                    dialog.setVisible(true);

                    startPenaltyShootout();
                });
                timer.setRepeats(false);
                timer.start();
                return;
            }

            MatchResult result = new MatchResult(userTeam.name(), scoreTeam1, aiTeam.name(), scoreTeam2);
            matchHistory.addResult(result);

            String line1;
            java.util.function.Function<Team, String> formatName = t -> {
                if (t == null) return "Unknown";
                String name = t.name().toLowerCase();
                return Character.toUpperCase(name.charAt(0)) + name.substring(1);
            };

            if (scoreTeam1 > scoreTeam2) {
                line1 = formatName.apply(userTeam) + " Wins! 🏆";
            } else {
                line1 = formatName.apply(aiTeam) + " Wins!";
            }
            
            String userAbbr = (userTeam != null) ? userTeam.getAbbreviation() : "USER";
            String aiAbbr = (aiTeam != null) ? aiTeam.getAbbreviation() : "AI";
            String line2 = "Final Score: " + userAbbr + " " + scoreTeam1 + " - " + scoreTeam2 + " " + aiAbbr;

            CustomGameOverDialog dialog = new CustomGameOverDialog(parentFrame, "Game Over", line1, line2, onGameEnd);
            dialog.setVisible(true);
        });
    }

    private void startPenaltyShootout() {
        gameState = GameState.PENALTY_SHOOTOUT;
        penaltyState = PenaltyState.AIMING;
        isUserTurnToShoot = true; 
        running = true; 

        team1.stream()
            .filter(p -> p.getRole() == Player.PlayerRole.GOALKEEPER)
            .findFirst()
            .ifPresent(gk -> gk.setSprite(userGkPenaltySprite));

        resetForPenaltyKick();
        if (gameThread == null || !gameThread.isAlive()) {
             gameThread = new Thread(this);
             gameThread.start();
        }
        requestFocusInWindow();
    }
    
    private void updatePenaltyShootout() {
        ball.move();
        penaltyKicker.move();
        penaltyGoalkeeper.move();
        if (penaltyGoalkeeper != null) {
            int goalHeight = 100;
            double goalTop = PLAYABLE_Y + (PLAYABLE_HEIGHT - goalHeight) / 2.0;
            double goalBottom = PLAYABLE_Y + (PLAYABLE_HEIGHT + goalHeight) / 2.0;
            double goalLineX = PLAYABLE_X + PLAYABLE_WIDTH - PLAYER_SIZE - 5;
    
            penaltyGoalkeeper.x = goalLineX;
    
            if (penaltyGoalkeeper.y < goalTop) {
                penaltyGoalkeeper.y = goalTop;
                penaltyGoalkeeper.setVelY(0);
            } else if (penaltyGoalkeeper.y + penaltyGoalkeeper.size > goalBottom) {
                penaltyGoalkeeper.y = goalBottom - penaltyGoalkeeper.size;
                penaltyGoalkeeper.setVelY(0);
            }
        }
    
        if (ball.y <= PLAYABLE_Y || ball.y >= (PLAYABLE_Y + PLAYABLE_HEIGHT) - ball.size) {
            ball.velY *= -1;
            ball.y = Math.max(PLAYABLE_Y, Math.min(ball.y, (PLAYABLE_Y + PLAYABLE_HEIGHT) - ball.size));
        }
    
        List<Player> penaltyPlayers = new ArrayList<>();
        penaltyPlayers.add(penaltyKicker);
        penaltyPlayers.add(penaltyGoalkeeper);
        penaltyGoalkeeper.checkCollisions(ball, penaltyPlayers);
    
        if (penaltyState == PenaltyState.KICKING) {
            int goalHeight = 100;
            double goalTop = PLAYABLE_Y + (PLAYABLE_HEIGHT - goalHeight) / 2.0;
            double goalBottom = PLAYABLE_Y + (PLAYABLE_HEIGHT + goalHeight) / 2.0;
    
            boolean goalScored = ball.x + ball.size >= (PLAYABLE_X + PLAYABLE_WIDTH) &&
                                 ball.getCenterY() > goalTop && ball.getCenterY() < goalBottom;
    
            if (goalScored && ball.getDribbler() == null) {
                handlePenaltyGoal();
                return;
            }
        }
    
        if (penaltyState == PenaltyState.AIMING) {
            if (!isUserTurnToShoot) {
                if (System.currentTimeMillis() - penaltyMessageStartTime > 1500) {
                    penaltyState = PenaltyState.KICKING;
                    shotDirection = ShotDirection.values()[random.nextInt(3)];
                    shootTheBall();
                    goalkeeperDive(userDiveDirection);
                }
            }
        } else if (penaltyState == PenaltyState.KICKING) {
            if ((Math.abs(ball.velX) < 0.5 && Math.abs(ball.velY) < 0.5) || ball.getDribbler() == penaltyGoalkeeper) {
                handlePenaltyResult();
            }
        } else if (penaltyState == PenaltyState.RESULT) {
            if (System.currentTimeMillis() - penaltyMessageStartTime > 2000) {
                if (checkPenaltyWinCondition()) {
                    showPenaltyEndDialog();
                } else {
                    nextPenaltyTaker();
                }
            }
        }
    }
    private void resetForPenaltyKick() {
        Player currentDribbler = ball.getDribbler();
        if (currentDribbler != null) {
            currentDribbler.losePossession(ball);
        }
        
        ball.setVelocity(0, 0);
        ball.setDribbler(null);

        if (isUserTurnToShoot) {
            penaltyKicker = team1.stream().filter(p -> p.getRole() == Player.PlayerRole.STRIKER).findFirst().get();
            penaltyGoalkeeper = team2.stream().filter(p -> p.getRole() == Player.PlayerRole.GOALKEEPER).findFirst().get();
        } else {
            penaltyKicker = team2.stream().filter(p -> p.getRole() == Player.PlayerRole.STRIKER).findFirst().get();
            penaltyGoalkeeper = team1.stream().filter(p -> p.getRole() == Player.PlayerRole.GOALKEEPER).findFirst().get();
        }
        
        double penaltySpotX = PLAYABLE_X + PLAYABLE_WIDTH - 150;
        double penaltySpotY = PLAYABLE_Y + PLAYABLE_HEIGHT / 2.0;

        ball.setPosition(penaltySpotX - ball.size / 2.0, penaltySpotY - ball.size / 2.0);
        
        penaltyKicker.setDirection(1, 0);
        penaltyKicker.setPosition(penaltySpotX - PLAYER_SIZE - 10, penaltySpotY - PLAYER_SIZE / 2.0);
        
        double gkX = PLAYABLE_X + PLAYABLE_WIDTH - PLAYER_SIZE - 5;
        double gkY = PLAYABLE_Y + PLAYABLE_HEIGHT / 2.0 - PLAYER_SIZE / 2.0;
        penaltyGoalkeeper.setPosition(gkX, gkY);
        
        penaltyKicker.setVelX(0); penaltyKicker.setVelY(0);
        penaltyGoalkeeper.setVelX(0); penaltyGoalkeeper.setVelY(0);

        penaltyState = PenaltyState.AIMING;
        shotDirection = ShotDirection.CENTER;
        userDiveDirection = ShotDirection.CENTER;
        penaltyMessageStartTime = System.currentTimeMillis(); 
    }

    private void shootTheBall() {
        soundPlayer.playShootSound();
        double targetY;
        int goalHeight = 100;
        int goalTop = PLAYABLE_Y + (PLAYABLE_HEIGHT - goalHeight) / 2;
        int goalBottom = goalTop + goalHeight;
        
        switch(shotDirection) {
            case LEFT: targetY = goalTop + goalHeight * 0.25; break;
            case RIGHT: targetY = goalBottom - goalHeight * 0.25; break;
            case CENTER: default: targetY = (goalTop + goalBottom) / 2.0; break;
        }

        double targetX = PLAYABLE_X + PLAYABLE_WIDTH;
        double dirX = targetX - ball.getCenterX();
        double dirY = targetY - ball.getCenterY();
        double mag = Math.sqrt(dirX * dirX + dirY * dirY);
        ball.setVelocity((dirX/mag) * 15.0, (dirY/mag) * 15.0);
    }

    private void goalkeeperDive(ShotDirection diveDirection) {
        double diveDistance = 10.0;
        penaltyGoalkeeper.setVelY(0);
    
        switch (diveDirection) {
            case LEFT:
                penaltyGoalkeeper.y -= diveDistance;
                break;
            case RIGHT:
                penaltyGoalkeeper.y += diveDistance;
                break;
            case CENTER:
                break;
        }
    }
    
    private void handlePenaltyResult() {
        penaltyState = PenaltyState.RESULT;
        penaltyMessageStartTime = System.currentTimeMillis();
    
        boolean saved = ball.getDribbler() == penaltyGoalkeeper;
    
        if (saved) {
            penaltyMessage = "SAVED!";
            soundPlayer.playSaveSound();
            ball.setVelocity(0, 0);
        } else {
            penaltyMessage = "MISS!";
            soundPlayer.playMissSound();
        }
    }

    private void handlePenaltyGoal() {
        penaltyState = PenaltyState.RESULT;
        penaltyMessageStartTime = System.currentTimeMillis();
        penaltyMessage = "GOAL!";
        soundPlayer.playGoalSound(1);
        if (isUserTurnToShoot) {
            penaltyScoreTeam1++;
        } else {
            penaltyScoreTeam2++;
        }
        ball.setVelocity(0, 0);
    }
    
    private void nextPenaltyTaker() {
        if(isUserTurnToShoot) penaltyKicksTeam1++; else penaltyKicksTeam2++;
        isUserTurnToShoot = !isUserTurnToShoot;
        penaltyMessage = "";
        resetForPenaltyKick();
    }
    
    private boolean checkPenaltyWinCondition() {
        if (penaltyKicksTeam1 < 5 || penaltyKicksTeam2 < 5) {
            int remaining1 = 5 - penaltyKicksTeam1;
            int remaining2 = 5 - penaltyKicksTeam2;
            if (penaltyScoreTeam1 > penaltyScoreTeam2 + remaining2) return true;
            if (penaltyScoreTeam2 > penaltyScoreTeam1 + remaining1) return true;
        } else {
            if (penaltyKicksTeam1 == penaltyKicksTeam2 && penaltyScoreTeam1 != penaltyScoreTeam2) {
                return true;
            }
        }
        
        return false;
    }

     private void showPenaltyEndDialog() {
        running = false;
        SwingUtilities.invokeLater(() -> {
            MatchResult result = new MatchResult(userTeam.name(), scoreTeam1, aiTeam.name(), scoreTeam2, true, penaltyScoreTeam1, penaltyScoreTeam2);
            matchHistory.addResult(result);

            String line1;
            java.util.function.Function<Team, String> formatName = t -> {
                if (t == null) return "Unknown";
                String name = t.name().toLowerCase();
                return Character.toUpperCase(name.charAt(0)) + name.substring(1);
            };

            if (penaltyScoreTeam1 > penaltyScoreTeam2) {
                line1 = formatName.apply(userTeam) + " Wins the Shootout! 🏆";
            } else {
                line1 = formatName.apply(aiTeam) + " Wins the Shootout!";
            }
            String line2 = "Penalty Score: " + penaltyScoreTeam1 + " - " + penaltyScoreTeam2;

            CustomGameOverDialog dialog = new CustomGameOverDialog(parentFrame, "Shootout Over", line1, line2, onGameEnd);
            dialog.setVisible(true);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        if (fieldImage != null) {
            g2d.drawImage(fieldImage, 0, 0, getWidth(), getHeight(), this);
        }
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        drawGoals(g2d);

        if (gameState == GameState.PENALTY_SHOOTOUT) {
             penaltyKicker.draw(g2d);
             penaltyGoalkeeper.draw(g2d);
             ball.draw(g2d);
        } else {
            if (allPlayers != null) allPlayers.forEach(p -> p.draw(g2d));
            if (ball != null) ball.draw(g2d);
        }
        
        drawUI(g2d);

        if (paused) {
            g2d.setColor(new Color(0, 0, 0, 128));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
        pauseMenuPanel.setVisible(paused);
    }

    private void drawGoals(Graphics2D g2d) {
        int goalHeight = 100;
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(PLAYABLE_X, PLAYABLE_Y + (PLAYABLE_HEIGHT - goalHeight) / 2-10, PLAYABLE_X, PLAYABLE_Y + (PLAYABLE_HEIGHT + goalHeight) / 2-10);
        g2d.drawLine(PLAYABLE_X + PLAYABLE_WIDTH, PLAYABLE_Y + (PLAYABLE_HEIGHT - goalHeight) / 2-10, PLAYABLE_X + PLAYABLE_WIDTH, PLAYABLE_Y + (PLAYABLE_HEIGHT + goalHeight) / 2-10);
    }

    private void drawUI(Graphics2D g2d) {
        if (pauseButton != null) {
            pauseButton.setVisible(gameState != GameState.PENALTY_SHOOTOUT && !paused);
        }

        int boxX = 10, boxY = 10, boxWidth = 150, boxHeight = 50, cornerRadius = 10;
        
        int totalBoxHeight = boxHeight;
        if (gameState == GameState.PENALTY_SHOOTOUT) {
            totalBoxHeight += 25;
        }

        g2d.setColor(new Color(0, 0, 128, 220));
        g2d.fillRoundRect(boxX, boxY, boxWidth, totalBoxHeight, cornerRadius, cornerRadius);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(boxX, boxY, boxWidth, totalBoxHeight, cornerRadius, cornerRadius);

        g2d.setColor(Color.WHITE);
        String userAbbr = (userTeam != null) ? userTeam.getAbbreviation() : "USER";
        String aiAbbr = (aiTeam != null) ? aiTeam.getAbbreviation() : "AI";
        String scoreText = String.format("%s %d - %d %s", userAbbr, scoreTeam1, scoreTeam2, aiAbbr);
        g2d.setFont(new Font("Consolas", Font.BOLD, 20));
        FontMetrics fmScore = g2d.getFontMetrics();
        int scoreTextWidth = fmScore.stringWidth(scoreText);
        g2d.drawString(scoreText, boxX + (boxWidth - scoreTextWidth) / 2, boxY + fmScore.getAscent() + 5);
        
        if (gameState == GameState.PENALTY_SHOOTOUT) {
             String penaltyScoreText = String.format("(%d - %d)", penaltyScoreTeam1, penaltyScoreTeam2);
             g2d.setFont(new Font("Consolas", Font.PLAIN, 22));
             FontMetrics fmPenalty = g2d.getFontMetrics();
             int penaltyTextWidth = fmPenalty.stringWidth(penaltyScoreText);
             g2d.drawString(penaltyScoreText, boxX + (boxWidth - penaltyTextWidth) / 2, boxY + boxHeight + 10);
        } else {
             int minutes = Math.max(0, remainingSeconds) / 60;
             int seconds = Math.max(0, remainingSeconds) % 60;
             String timeText = String.format("%02d:%02d", minutes, seconds);
             g2d.setFont(new Font("Consolas", Font.PLAIN, 22));
             FontMetrics fmTime = g2d.getFontMetrics();
             int timeTextWidth = fmTime.stringWidth(timeText);
             g2d.drawString(timeText, boxX + (boxWidth - timeTextWidth) / 2, boxY + boxHeight - 5);
        }
        
        if (System.currentTimeMillis() - goalMessageStartTime < 3000) {
            String goalText = "GOAL!!!";
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            FontMetrics fmGoal = g2d.getFontMetrics();
            int goalTextWidth = fmGoal.stringWidth(goalText);
            int goalX = (GamePanel.WIDTH - goalTextWidth) / 2;
            int goalY = GamePanel.HEIGHT / 2;
            g2d.setColor(Color.YELLOW);
            g2d.drawString(goalText, goalX, goalY);
        }
        
        if (gameState == GameState.PENALTY_SHOOTOUT && penaltyState == PenaltyState.AIMING) {
            String instructionText;
            ShotDirection indicatorDirection;

            if (isUserTurnToShoot) {
                instructionText = "AIM: Use ↑ ← → keys. Shoot: SHIFT";
                indicatorDirection = shotDirection;
            } else {
                instructionText = "CHOOSE DIVE: Use ↑ ← → keys.";
                indicatorDirection = userDiveDirection;
            }

            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(instructionText);
            g2d.setColor(Color.WHITE);
            g2d.drawString(instructionText, (WIDTH - textWidth) / 2, HEIGHT - 50);

            g2d.setColor(Color.YELLOW);
            int indicatorX = 0, indicatorY = 0;
            int goalCenterY = PLAYABLE_Y + PLAYABLE_HEIGHT / 2;
            int goalPostHeight = 50;

            switch(indicatorDirection){
                case LEFT: indicatorX = (int) (PLAYABLE_X + PLAYABLE_WIDTH); indicatorY = goalCenterY - goalPostHeight; break;
                case CENTER: indicatorX = (int) (PLAYABLE_X + PLAYABLE_WIDTH); indicatorY = goalCenterY; break;
                case RIGHT: indicatorX = (int) (PLAYABLE_X + PLAYABLE_WIDTH); indicatorY = goalCenterY + goalPostHeight; break;
            }
            g2d.fillOval(indicatorX-10, indicatorY-10, 20, 20);

        } else if (penaltyState == PenaltyState.RESULT && !penaltyMessage.isEmpty()) {
             g2d.setFont(new Font("Arial", Font.BOLD, 60));
             FontMetrics fm = g2d.getFontMetrics();
             int textWidth = fm.stringWidth(penaltyMessage);
             g2d.setColor(penaltyMessage.equals("GOAL!") ? Color.GREEN : Color.RED);
             g2d.drawString(penaltyMessage, (WIDTH - textWidth) / 2, HEIGHT / 2);
        }
    }

    private class KeyInputHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (gameState == GameState.PENALTY_SHOOTOUT && penaltyState == PenaltyState.AIMING) {
                if (isUserTurnToShoot) {
                    if (key == KeyEvent.VK_LEFT) shotDirection = ShotDirection.LEFT;
                    if (key == KeyEvent.VK_RIGHT) shotDirection = ShotDirection.RIGHT;
                    if (key == KeyEvent.VK_UP) shotDirection = ShotDirection.CENTER;
                    if (key == KeyEvent.VK_SHIFT) {
                        penaltyState = PenaltyState.KICKING;
                        shootTheBall();
                        goalkeeperDive(ShotDirection.values()[random.nextInt(3)]);
                    }
                } else {
                    if (key == KeyEvent.VK_LEFT) userDiveDirection = ShotDirection.LEFT;
                    if (key == KeyEvent.VK_RIGHT) userDiveDirection = ShotDirection.RIGHT;
                    if (key == KeyEvent.VK_UP) userDiveDirection = ShotDirection.CENTER;
                }
                return;
            }

            if (team1 == null || paused) return;
            Player humanPlayer1 = team1.stream().filter(p -> !(p instanceof AIPlayer)).findFirst().orElse(null);
            if (humanPlayer1 != null) {
                if (key == KeyEvent.VK_UP) humanPlayer1.setVelY(-USER_SPEED);
                if (key == KeyEvent.VK_DOWN) humanPlayer1.setVelY(USER_SPEED);
                if (key == KeyEvent.VK_LEFT) humanPlayer1.setVelX(-USER_SPEED);
                if (key == KeyEvent.VK_RIGHT) humanPlayer1.setVelX(USER_SPEED);
                if (key == KeyEvent.VK_SHIFT && humanPlayer1.isDribbling()) humanPlayer1.shoot();
                if (key == KeyEvent.VK_CONTROL && humanPlayer1.isDribbling()) humanPlayer1.pass();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();
            if (gameState == GameState.PENALTY_SHOOTOUT || paused) return;
            if (team1 == null) return;
            Player humanPlayer1 = team1.stream().filter(p -> !(p instanceof AIPlayer)).findFirst().orElse(null);
            if (humanPlayer1 != null) {
                if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) humanPlayer1.setVelY(0);
                if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) humanPlayer1.setVelX(0);
            }
        }
    }
}

abstract class GameObject {
    protected double x, y, velX, velY;
    protected int size;
    public GameObject(double x, double y, int size) { this.x = x; this.y = y; this.size = size; }
    public abstract void draw(Graphics2D g2d);
    public Rectangle2D.Double getBounds() { return new Rectangle2D.Double(x, y, size, size); }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public double getCenterX() { return x + size / 2.0; }
    public double getCenterY() { return y + size / 2.0; }
    public double distanceTo(GameObject other) {
        return Math.sqrt(Math.pow(this.getCenterX() - other.getCenterX(), 2) + Math.pow(this.getCenterY() - other.getCenterY(), 2));
    }
    public void setVelocity(double vx, double vy) { this.velX = vx; this.velY = vy; }
    public void setVelX(double velX) { this.velX = velX; }
    public void setVelY(double velY) { this.velY = velY; }
}

class Ball extends GameObject {
    private static final double FRICTION = 0.985;
    private Player dribbler;
    private final BufferedImage sprite;
    private double rotationAngle = 0;
    public Ball(double x, double y, int size, BufferedImage sprite) { super(x, y, size); this.sprite = sprite; }
    public void move() {
        if (dribbler == null) {
            x += velX;
            y += velY;
            velX *= FRICTION;
            velY *= FRICTION;
            rotationAngle += Math.sqrt(velX * velX + velY * velY) * 0.1;
        }
    }
    public void checkWallCollision(int minX, int maxX, int minY, int maxY) {
        if (x <= minX || x >= maxX - size) { velX *= -1; x = Math.max(minX, Math.min(x, maxX - size)); }
        if (y <= minY || y >= maxY - size) { velY *= -1; y = Math.max(minY, Math.min(y, maxY - size)); }
    }
    
    public Player getDribbler() { return dribbler; }
    public void setDribbler(Player dribbler) { this.dribbler = dribbler; }
    @Override
    public void draw(Graphics2D g2d) {
        if (sprite != null) {
            AffineTransform old = g2d.getTransform();
            g2d.translate(getCenterX(), getCenterY());
            g2d.rotate(rotationAngle);
            g2d.drawImage(sprite, -size/2, -size/2, size, size, null);
            g2d.setTransform(old);
        } else {
            g2d.setColor(Color.WHITE);
            g2d.fill(new Ellipse2D.Double(x, y, size, size));
        }
    }
}

class Player extends GameObject {
    public enum PlayerRole { GOALKEEPER, DEFENDER, MIDFIELDER, STRIKER }
    protected String team;
    protected PlayerRole role;
    protected GamePanel gamePanel;
    protected boolean isDribbling = false;
    protected double lastDirX = 1;
    protected double lastDirY = 0;
    protected long possessionStartTime = 0;
    protected BufferedImage sprite;
    protected final Color fallbackColor;
    public Player(double x, double y, int size, BufferedImage sprite, Color fallbackColor, String team, PlayerRole role, GamePanel gamePanel) {
        super(x, y, size);
        this.sprite = sprite;
        this.fallbackColor = fallbackColor;
        this.team = team; this.role = role; this.gamePanel = gamePanel;
        this.lastDirX = team.equals("Team 1") ? 1 : -1;
    }
    public Player(AIPlayer other) {
        super(other.x, other.y, other.size);
        this.sprite = other.sprite;
        this.fallbackColor = other.fallbackColor;
        this.team = other.team;
        this.role = other.role;
        this.gamePanel = other.gamePanel;
        this.velX = other.velX;
        this.velY = other.velY;
        this.isDribbling = other.isDribbling;
        this.lastDirX = other.lastDirX;
        this.lastDirY = other.lastDirY;
        this.possessionStartTime = other.possessionStartTime;
    }
    public void move() {
        x += velX;
        y += velY;

        if (gamePanel.getGameState() != GamePanel.GameState.PENALTY_SHOOTOUT) {
             if (Math.abs(velX) > 0.01 || Math.abs(velY) > 0.01) {
                 double magnitude = Math.sqrt(velX * velX + velY * velY);
                 if (magnitude > 0) {
                     lastDirX = velX / magnitude;
                     lastDirY = velY / magnitude;
                 }
             }
        }
        
        if (isDribbling) {
            Ball ball = gamePanel.getBall();
            double ballOffset = this.size * 0.6;
            double ballX = this.getCenterX() + lastDirX * ballOffset - ball.size / 2.0;
            double ballY = this.getCenterY() + lastDirY * ballOffset - ball.size / 2.0;
            ball.setPosition(ballX, ballY);
            ball.setVelocity(0, 0);
        }
    }
    
    public void checkCollisions(Ball ball, List<Player> allPlayers) {
        checkWallCollision(GamePanel.PLAYABLE_X, GamePanel.PLAYABLE_X + GamePanel.PLAYABLE_WIDTH, GamePanel.PLAYABLE_Y, GamePanel.PLAYABLE_Y + GamePanel.PLAYABLE_HEIGHT);
        for (Player other : allPlayers) {
            if (this != other && this.getBounds().intersects(other.getBounds())) {
                resolvePlayerCollision(other);
            }
        }
        if (isDribbling) {
            for (Player other : allPlayers) {
                if (other != this && !other.getTeam().equals(this.team) && this.getBounds().intersects(other.getBounds())) {
                    double dx = other.getCenterX() - this.getCenterX();
                    double dirX = (lastDirX >= 0) ? 1 : -1;
                    if ((dx > 0 && dirX > 0) || (dx < 0 && dirX < 0)) {
                        this.losePossession(ball);
                        other.kick(ball, 2.0);
                        break;
                    }
                }
            }
        } else if (ball.getDribbler() == null && this.getBounds().intersects(ball.getBounds())) {
            ball.setDribbler(this);
            this.isDribbling = true;
            this.possessionStartTime = System.currentTimeMillis();
            
            if (gamePanel.getGameState() == GamePanel.GameState.RUNNING) {
                if (this instanceof AIPlayer && this.team.equals("Team 1")) {
                    gamePanel.switchControlToPlayer(this);
                }
            }
        }
    }
    
    private void resolvePlayerCollision(Player other) {
        double dx = this.getCenterX() - other.getCenterX();
        double dy = this.getCenterY() - other.getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        double overlap = (this.size / 2.0 + other.size / 2.0) - distance;
        if (overlap > 0) {
            double resolveX = (distance == 0) ? overlap : (dx / distance) * overlap;
            double resolveY = (distance == 0) ? 0 : (dy / distance) * overlap;
            this.x += resolveX / 2.0; this.y += resolveY / 2.0;
            other.x -= resolveX / 2.0; other.y -= resolveY / 2.0;
        }
    }
    public void kick(Ball ball, double strength) {
        kickInDirection(ball, ball.getCenterX() - this.getCenterX(), ball.getCenterY() - this.getCenterY(), strength);
    }
    private void kickInDirection(Ball ball, double dirX, double dirY, double strength) {
        double mag = Math.sqrt(dirX*dirX + dirY*dirY);
        if (mag == 0) return;
        ball.setVelocity((dirX / mag) * strength, (dirY / mag) * strength);
    }
    public void losePossession(Ball ball) { this.isDribbling = false; this.possessionStartTime = 0; ball.setDribbler(null); }
    
    public void shoot() {
        if (!isDribbling) return;
        gamePanel.getSoundPlayer().playShootSound();
        if (gamePanel.getGameState() == GamePanel.GameState.KICK_OFF) {
            gamePanel.transitionToRunningState();
        }
        Ball ball = gamePanel.getBall();
        losePossession(ball);
        double targetX = team.equals("Team 1") ? GamePanel.PLAYABLE_X + GamePanel.PLAYABLE_WIDTH : GamePanel.PLAYABLE_X;
        double targetY = GamePanel.PLAYABLE_Y + GamePanel.PLAYABLE_HEIGHT / 2.0;
        kickInDirection(ball, targetX - ball.getCenterX(), targetY - ball.getCenterY(), 13.0);
    }
    
    public void pass() {
        if (!isDribbling) return;
        gamePanel.handlePass(this.team);
        if (gamePanel.getGameState() == GamePanel.GameState.KICK_OFF) {
            gamePanel.transitionToRunningState();
        }
        Ball ball = gamePanel.getBall();
        losePossession(ball);

        final double forwardVecX = this.lastDirX;
        final double forwardVecY = this.lastDirY;

        Player bestTeammate = gamePanel.getTeam(this.team).stream()
            .filter(p -> p != this && p.getRole() != PlayerRole.GOALKEEPER)
            .filter(teammate -> {
                double toTeammateVecX = teammate.getCenterX() - this.getCenterX();
                double toTeammateVecY = teammate.getCenterY() - this.getCenterY();
                double dotProduct = (forwardVecX * toTeammateVecX) + (forwardVecY * toTeammateVecY);
                return dotProduct > 0;
            })
            .min(Comparator.comparingDouble(p -> p.distanceTo(this)))
            .orElse(null);

        if (bestTeammate != null) {
            kickInDirection(ball, bestTeammate.getCenterX() - ball.getCenterX(), bestTeammate.getCenterY() - ball.getCenterY(), 9.0);
        } else {
            kickInDirection(ball, forwardVecX, forwardVecY, 4.5);
        }
    }
    public void checkWallCollision(int minX, int maxX, int minY, int maxY) {
        x = Math.max(minX, Math.min(x, maxX - size));
        y = Math.max(minY, Math.min(y, maxY - size));
    }

    public void setDirection(double dirX, double dirY) {
        this.lastDirX = dirX;
        this.lastDirY = dirY;
    }
    
    public void setSprite(BufferedImage newSprite) {
        if (newSprite != null) {
            this.sprite = newSprite;
        }
    }

    public PlayerRole getRole() { return role; }
    public String getTeam() { return team; }
    public boolean isDribbling() { return isDribbling; }
    
    @Override
    public void draw(Graphics2D g2d) {
        if (!(this instanceof AIPlayer) && this.team.equals("Team 1") && gamePanel.getGameState() != GamePanel.GameState.PENALTY_SHOOTOUT) {
            g2d.setColor(Color.YELLOW);
            g2d.fillOval((int)getCenterX() - 5, (int)y + size + 2, 10, 10);
        }

        if (sprite != null) {
            AffineTransform oldTransform = g2d.getTransform();
            g2d.translate(getCenterX(), getCenterY());

            boolean shouldRotate = (gamePanel.getGameState() != GamePanel.GameState.PENALTY_SHOOTOUT) || (this.getRole() == PlayerRole.GOALKEEPER);

            if (shouldRotate) {
                if (this.getRole() != PlayerRole.GOALKEEPER) {
                    double angle = Math.atan2(lastDirY, lastDirX);
                    g2d.rotate(angle);
                    if (Math.abs(angle) > Math.PI / 2) {
                        g2d.scale(1, -1);
                    }
                } else {
                    if (this.team.equals("Team 2")) {
                        g2d.scale(-1, 1);
                    }
                }
            }

            g2d.drawImage(sprite, -size / 2, -size / 2, size, size, null);
            g2d.setTransform(oldTransform);

        } else {
            g2d.setColor(fallbackColor);
            g2d.fill(new Ellipse2D.Double(x, y, size, size));
            g2d.setColor(Color.BLACK);
            g2d.draw(new Ellipse2D.Double(x, y, size, size));
        }
    }
}

class AIPlayer extends Player {
    private double homeX, homeY;
    public AIPlayer(double x, double y, int size, BufferedImage sprite, Color fallbackColor, String team, PlayerRole role, GamePanel gamePanel) {
        super(x, y, size, sprite, fallbackColor, team, role, gamePanel);
    }
    public AIPlayer(Player other, double homeX, double homeY) {
        super(other.x, other.y, other.size, other.sprite, other.fallbackColor, other.team, other.role, other.gamePanel);
        this.velX = other.velX;
        this.velY = other.velY;
        this.isDribbling = other.isDribbling;
        this.lastDirX = other.lastDirX;
        this.lastDirY = other.lastDirY;
        this.possessionStartTime = other.possessionStartTime;
        this.homeX = homeX;
        this.homeY = homeY;
    }
    public double getHomeX() { return homeX; }
    public double getHomeY() { return homeY; }
    public void setHomePosition(double hx, double hy) { this.homeX = hx; this.homeY = hy; }
    
    @Override 
    public void move() {
        if (gamePanel.getGameState() == GamePanel.GameState.PENALTY_SHOOTOUT) {
        } else if (gamePanel.getGameState() == GamePanel.GameState.RUNNING) {
            decideAction();
        } else if (gamePanel.getGameState() == GamePanel.GameState.KICK_OFF) {
            if (this.isDribbling) {
                this.velX = 0;
                this.velY = 0;
                if (System.currentTimeMillis() - this.possessionStartTime > 3000) {
                    this.pass();
                }
            } else {
                velX = 0;
                velY = 0;
            }
        }
        else {
            velX = 0;
            velY = 0;
        }
        super.move();
    }
    
    private void decideAction() {
        if (this.isDribbling()) {
            decideWithBall();
        } else {
            Ball ball = gamePanel.getBall();
            Player dribbler = ball.getDribbler();
            boolean myTeamHasBall = dribbler != null && dribbler.getTeam().equals(this.team);
            if (myTeamHasBall) {
                behaveOffensively(dribbler);
            } else {
                behaveDefensively();
            }
        }
    }

    private void decideWithBall() {
        if (this.getRole() == PlayerRole.GOALKEEPER) {
            if (System.currentTimeMillis() - this.possessionStartTime > 250) {
                this.pass();
            } else {
                this.velX = 0;
                this.velY = 0;
            }
            return;
        }

        boolean timeUp = System.currentTimeMillis() - this.possessionStartTime > 3000;
        double goalX = team.equals("Team 1") ? GamePanel.PLAYABLE_X + GamePanel.PLAYABLE_WIDTH : GamePanel.PLAYABLE_X;
        double distanceToGoal = Math.abs(this.getCenterX() - goalX);

        if (timeUp) {
            if (distanceToGoal < 500 && role != PlayerRole.DEFENDER) {
                this.shoot();
            } else {
                this.pass();
            }
            return;
        }

        if (distanceToGoal < 400 && role != PlayerRole.DEFENDER) {
            this.shoot();
        } else {
            moveTo(goalX, this.getCenterY(), GamePanel.AI_BASE_SPEED);
        }
    }

    private void behaveOffensively(Player ballCarrier) {
        double targetX, targetY;
        double halfWayLine = GamePanel.PLAYABLE_X + GamePanel.PLAYABLE_WIDTH / 2.0;
        switch (role) {
            case STRIKER:
                targetX = (team.equals("Team 1")) ? GamePanel.PLAYABLE_X + GamePanel.PLAYABLE_WIDTH - 150 : GamePanel.PLAYABLE_X + 150;
                targetY = this.homeY;
                break;
            case MIDFIELDER:
                targetX = ballCarrier.getCenterX() + (team.equals("Team 1") ? 150 : -150);
                targetY = this.homeY;
                break;
            case DEFENDER:
                targetX = halfWayLine + (team.equals("Team 1") ? 50 : -50);
                targetY = this.y;
                break;
            default:
                targetX = homeX;
                targetY = homeY;
                break;
        }
        moveTo(targetX, targetY, GamePanel.AI_BASE_SPEED * 0.8);
    }
    
    private void behaveDefensively() {
        Ball ball = gamePanel.getBall();
        if (role == PlayerRole.GOALKEEPER) {
            moveTo(homeX, ball.getCenterY(), GamePanel.AI_BASE_SPEED * 1.5);
            return;
        }
        
        Player closestToBall = gamePanel.getTeam(this.team).stream()
                .filter(p -> p.getRole() != PlayerRole.GOALKEEPER)
                .min(Comparator.comparingDouble(p -> p.distanceTo(ball))).orElse(this);
        if (this == closestToBall) {
            moveTo(ball.getCenterX(), ball.getCenterY(), GamePanel.AI_BASE_SPEED);
        } else {
            moveTo(homeX, homeY, GamePanel.AI_BASE_SPEED * 0.9);
        }
    }

    private void moveTo(double targetX, double targetY, double speed) {
        double dx = targetX - this.getCenterX(); double dy = targetY - this.getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > 1) { velX = (dx / distance) * speed; velY = (dy / distance) * speed; }
        else { velX = 0; velY = 0; }
    }
}