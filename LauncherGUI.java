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
    int memoryAmount = 1024;
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(LauncherGUI::new);
    }
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
        constraints.gridwidth = 4;
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

        // Options button
        constraints.gridx = 1;
        btnOptions = new JButton("Options");
        btnOptions.addActionListener(e -> optionsWindow = new OptionsWindow(this));
        panel.add(btnOptions, constraints);

        // Play button
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.CENTER;
        btnLaunch = new JButton("Launch Minecraft");
        btnLaunch.addActionListener(e -> LaunchMinecraft((String)cmbVersions.getSelectedItem(), txtUsername.getText()));
        panel.add(btnLaunch, constraints);

        // Username input
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = 3;
        constraints.weightx = 0.01;
        txtUsername = new JTextArea("pino");
        panel.add(txtUsername, constraints);

        add(panel);
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

                Launcher.Launch(version, username, memoryAmount, false);
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

    private void UpdateVersionList()
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