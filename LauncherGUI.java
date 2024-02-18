import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.List;

public class LauncherGUI extends JFrame
{
    JPanel panel = new JPanel();
    JTextArea log = new JTextArea();
    JTextArea txtUsername = new JTextArea("pino");
    JComboBox cmbVersions;
    JButton btnLaunch = new JButton("Launch Minecraft");
    public LauncherGUI()
    {
        setTitle("Minecraft Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(854, 480);

        // Initialize the layout
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // +------------------------+
        // |                        |
        // |          log           |
        // |                        |
        // |Vers     Play   Username|
        // +------------------------+

        // log
        constraints.gridx = constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 0.97;
        log.setEditable(false);
        JScrollPane scroller = new JScrollPane(log);
        panel.add(scroller, constraints);

        constraints.fill = 0;
        // version selection
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.weighty = 0.03;
        cmbVersions = new JComboBox(GetVersionList());
        panel.add(cmbVersions, constraints);

        // play button
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 1;
        btnLaunch.addActionListener(e -> LaunchMinecraft((String) cmbVersions.getSelectedItem(), txtUsername.getText()));
        panel.add(btnLaunch, constraints);

        // username input
        //constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = 2;
        //constraints.weightx = 0.0;
        panel.add(txtUsername, constraints);

        add(panel);
        setVisible(true);
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

                Launcher.Launch(version, username);
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally { SwingUtilities.invokeLater(() -> btnLaunch.setEnabled(true)); }
        });

        mcThread.start();
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { new LauncherGUI(); }
        });
    }

    private void UpdateComboBox(String[] strings, JComboBox menu)
    {
        DefaultComboBoxModel<String> versions = (DefaultComboBoxModel<String>) menu.getModel();
        for(String s : strings) { versions.addElement(s); }
    }

    private String[] GetVersionList()
    {
        List<String> versions = new ArrayList<>();
        File versionsDir = new File("versions" + File.separator);
        try
        {
            for(String fileName : versionsDir.list())
            {
                File ver = new File( versionsDir.getPath() + File.separator + fileName + File.separator);
                if(ver.exists())
                {
                    versions.add(fileName);
                }
            }
        } catch (NullPointerException e) { }
        String[] ret = versions.toArray(new String[0]);
//        Arrays.sort(ret);
        return ret;
    }
}

class ConsoleOutputToJTextArea extends OutputStream
{
    private JTextArea txt;
    public  ConsoleOutputToJTextArea(JTextArea textArea)
    {
        txt = textArea;
    }

    @Override
    public void write(int b)
    {
        // Append to the text area
        txt.append(String.valueOf((char) b));
        // Scroll to the bottom
        txt.setCaretPosition(txt.getDocument().getLength());
    }
}