import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.PrintStream;

public class OptionsWindow extends JFrame
{
    LauncherGUI mainWindow;
    JPanel panel = new JPanel();
    JTextArea txtMemory;
    JComboBox cmbVersions;
    JButton btnDownload;
    JButton btnSaveLog;
    public OptionsWindow(LauncherGUI mainWindow)
    {
        this.mainWindow = mainWindow;
        setTitle("Options");
        setSize(375, 100);
        setResizable(false);

        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // +---------------------+
        // | Memory: []          +
        // |                     |
        // | Downloader: [] (DL) |
        // |     (Save logs)     |
        // +---------------------+

        constraints.insets = new Insets(3, 3, 3, 3);
        // Memory
        constraints.gridx = constraints.gridy = 0;
        constraints.fill = GridBagConstraints.WEST;
        panel.add(new Label("Memory (mb):"), constraints);
        // Memory input
        constraints.gridx = 1;
        txtMemory = new JTextArea(Integer.toString(mainWindow.memoryAmount));
        txtMemory.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { mainWindow.SetMemory(txtMemory.getText()); }
            @Override
            public void removeUpdate(DocumentEvent e) { mainWindow.SetMemory(txtMemory.getText()); }
            @Override
            public void changedUpdate(DocumentEvent e) { }
        });
        panel.add(txtMemory, constraints);

        // Downloader
        constraints.gridy = 1;
        constraints.gridx = 0;
        panel.add(new Label("Downloader:"), constraints);
        // Versions list
        constraints.gridx = 1;
        //cmbVersions = new JComboBox(ManifestParser.GetDownloadableVersions());
        cmbVersions = new JComboBox(new String[] { "Could not get manifest" });
        UpdateDownloadableVersionsList();
        panel.add(cmbVersions, constraints);
        // Download button
        btnDownload = new JButton("Download");
        btnDownload.addActionListener(e -> mainWindow.DownloadVersion((String)cmbVersions.getSelectedItem(), "."));
        constraints.gridx = 2;
        panel.add(btnDownload, constraints);
        // Save log button
        constraints.gridy = 2;
        constraints.gridx = 0;
        btnSaveLog = new JButton("Save log");
        btnSaveLog.addActionListener(e -> mainWindow.SaveLog());
        panel.add(btnSaveLog);

        add(panel);
        setVisible(true);
    }
    public void UpdateDownloadableVersionsList()
    {
        Thread versionListThread = new Thread(() -> {
            PrintStream stream = new PrintStream(new ConsoleOutputToJTextArea(mainWindow.log));
            System.setOut(stream);
            System.setErr(stream);

            cmbVersions.setModel(new DefaultComboBoxModel(ManifestParser.GetDownloadableVersions()));
        });

        versionListThread.start();
    }
}
