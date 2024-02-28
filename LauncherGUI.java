import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDateTime;

public class LauncherGUI extends JFrame
{
    private StringWriter sw;
    private PrintWriter pw; // We use these to redirect the stack trace
    private JPanel panel = new JPanel();
    JTextArea log;
    private JTextArea txtUsername;
    private JComboBox cmbVersions;
    private JButton btnLaunch;
    private JButton btnOptions;
    private OptionsWindow optionsWindow;
    private JButton btnRefreshVersionList;
    static String settingsFilePath = "launcher.config.json";
    int memoryAmount = 1024;
    String customJavaExePath = "";
    public static void main(String[] args) { SwingUtilities.invokeLater(LauncherGUI::new); }
    public LauncherGUI()
    {
        sw = new StringWriter();
        pw = new PrintWriter(sw);

        setTitle("Minecraft Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(854, 480);
        setIconImage(new ImageIcon(getClass().getResource("/resources/SeaLantern.png")).getImage());

        // Initialize the layout
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // +----------------------------------------+
        // |                                        |
        // |                                        |
        // |                 log                    |
        // |                                        |
        // |                                        |
        // |[Vers] [Options]    (Play)    [Username]|
        // +----------------------------------------+

        constraints.insets = new Insets(3, 3, 3, 3);

        // Log
        constraints.gridx = constraints.gridy = 0;
        constraints.gridwidth = 5;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 0.97;
        log = new JTextArea();
        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane scroller = new JScrollPane(log);
        panel.add(scroller, constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.weighty = 0.03;
        // Version selection
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.weightx = 0.01;
        cmbVersions = new JComboBox(GetVersionList());
        panel.add(cmbVersions, constraints);

        // Refresh button
        constraints.gridx = 1;
        ImageIcon refreshIcon = new ImageIcon(getClass().getResource("/resources/refresh.png"));
        btnRefreshVersionList = new JButton(refreshIcon);
        btnRefreshVersionList.addActionListener(e -> UpdateVersionList());
        panel.add(btnRefreshVersionList, constraints);


        // Options button
        constraints.gridx = 2;
        btnOptions = new JButton("Options");
        btnOptions.addActionListener(e -> optionsWindow = new OptionsWindow(this, getX() + getWidth() / 2, getY() + getHeight() / 2));
        panel.add(btnOptions, constraints);

        // Play button
        constraints.gridx = 3;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.CENTER;
        btnLaunch = new JButton("Launch Minecraft");
        btnLaunch.addActionListener(e -> { SaveSettings(); LaunchMinecraft((String)cmbVersions.getSelectedItem(), txtUsername.getText()); });
        panel.add(btnLaunch, constraints);

        // Username input
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = 4;
        constraints.weightx = 0.01;
        txtUsername = new JTextArea("pino");
        panel.add(txtUsername, constraints);

        add(panel);
        LoadSettings();
        setVisible(true);
    }

    public void DownloadVersion(String version, String path)
    {
        optionsWindow.btnDownload.setEnabled(false);
        btnLaunch.setEnabled(false);
        Thread downloaderThread = new Thread(() -> {
            PrintStream stream = new PrintStream(new ConsoleOutputToJTextArea(log));
            System.setOut(stream);
            System.setErr(stream);

            Downloader.Download((String)optionsWindow.cmbVersions.getSelectedItem(), ".", true, true, true);
            optionsWindow.btnDownload.setEnabled(true);
            btnLaunch.setEnabled(true);
            UpdateVersionList();
        });

        downloaderThread.start();
    }

    private void LaunchMinecraft(String version, String username)
    {
        btnLaunch.setEnabled(false);
        Thread mcThread = new Thread(() -> {
            try
            {
                PrintStream stream = new PrintStream(new ConsoleOutputToJTextArea(log));
                System.setOut(stream);
                System.setErr(stream);

                Launcher.Launch(version, username, memoryAmount, false, customJavaExePath);
            } catch (Exception e)
            {
                e.printStackTrace(pw);
                ShowError(sw.toString());
            } finally { SwingUtilities.invokeLater(() -> btnLaunch.setEnabled(true)); }
        });

        mcThread.start();
    }

    public void SetMemory(String amount)
    {
        try
        {
            memoryAmount = Integer.parseInt(amount);
        } catch (NumberFormatException ignored) { }
    }

    public void SetJavaExePath(String path)
    {
        customJavaExePath = path;
    }

    public void SaveLog()
    {
        String fileName = LocalDateTime.now().toString().split("\\.")[0].replaceAll(":", ".") + ".log";
        try
        {
            new File(fileName).createNewFile();
            FileWriter fw = new FileWriter(fileName);
            fw.write(log.getText());
            fw.close();
            log.append("Saved log to: " + fileName);
        } catch (IOException e) { e.printStackTrace(pw); ShowError(sw.toString()); }
    }

    public void UpdateVersionList()
    {
        cmbVersions.setModel(new DefaultComboBoxModel(GetVersionList()));
    }

    private String[] GetVersionList()
    {
        String[] versions = new String[] { "No versions found" };
        File versionsDir = new File(Launcher.VERSIONS_DIR);
        if(versionsDir.exists())
        {
            versions = versionsDir.list();
        }
        return versions;
    }

    public static void ShowError(String errorMessage)
    {
        JOptionPane.showMessageDialog(null, errorMessage, "An error occurred!", JOptionPane.ERROR_MESSAGE);
    }

    public void SaveSettings()
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Config config = new Config(memoryAmount, customJavaExePath, txtUsername.getText(), new int[] { getX(), getY() });
        try
        {
            new File(settingsFilePath).createNewFile();
            FileWriter fw = new FileWriter(settingsFilePath);
            gson.toJson(config, fw);
            fw.close();
            log.append("Saved config!\n");
        } catch (IOException e) { e.printStackTrace(pw); ShowError(sw.toString()); }
    }

    public void LoadSettings()
    {
        Gson gson = new Gson();
        try
        {
            FileReader fr = new FileReader(settingsFilePath);
            Config settings = gson.fromJson(fr, Config.class);
            fr.close();
            memoryAmount = settings.memoryAmount;
            customJavaExePath = settings.customJavaExePath;
            txtUsername.setText(settings.username);
            setLocation(settings.pos[0], settings.pos[1]);
            log.append("Loaded config!\n");
        } catch (IOException e) { log.append("Config not found!\n"); }
    }
}

class ConsoleOutputToJTextArea extends OutputStream
{
    private JTextArea txt;
    public ConsoleOutputToJTextArea(JTextArea textArea) { txt = textArea; }
    @Override
    public void write(int b)
    {
        // Append to the text area
        txt.append(String.valueOf((char) b));
        // Scroll to the bottom
        txt.setCaretPosition(txt.getDocument().getLength());
    }
}