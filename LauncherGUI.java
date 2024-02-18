import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class LauncherGUI extends JFrame
{
    JTextArea log = new JTextArea();
    JPanel panel = new JPanel();
    JLabel lblLaunch = new JLabel("Click the button to launch Minecraft");
    JButton launchBtn = new JButton("Launch Minecraft");
    public LauncherGUI()
    {
        setTitle("Minecraft Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(854, 480);

        panel.setLayout(new GridLayout(2, 2));
        launchBtn.addActionListener(e -> LaunchMinecraft());
        panel.add(lblLaunch, 1, 0);
        panel.add(launchBtn, 0, 1);

        //JTextArea log = new JTextArea();
        log.setEditable(false);
        JScrollPane scroller = new JScrollPane(log);
        panel.add(scroller, 0, 0);

        add(panel);
        setVisible(true);
    }

    private void LaunchMinecraft()
    {
        launchBtn.setEnabled(false);

        Thread mcThread = new Thread(() -> {
            try
            {
                PrintStream stream = new PrintStream(new ConsoleOutputToJTextArea(log));
                System.setOut(stream);
                System.setErr(stream);

                Launcher.Launch("1.7.10", "pino");
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally { SwingUtilities.invokeLater(() -> launchBtn.setEnabled(true)); }
        });

        mcThread.start();
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LauncherGUI();
            }
        });
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
        // Redirect output to txt
        txt.append(String.valueOf((char) b));
        // Scroll to the bottom
        txt.setCaretPosition(txt.getDocument().getLength());
    }
}