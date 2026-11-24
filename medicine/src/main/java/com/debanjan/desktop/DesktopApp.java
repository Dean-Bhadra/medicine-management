package com.debanjan.desktop;

import com.debanjan.MedicineManagementApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public final class DesktopApp {
    private static final String NAME = "Dispensary Desktop";

    public static void launch() {
        System.setProperty("java.awt.headless", "false");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            Path home = Path.of(System.getenv("LOCALAPPDATA"), "DispensaryDesktop");
            Path data = Files.createDirectories(home.resolve("data"));
            try (FileChannel channel = FileChannel.open(home.resolve("desktop.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock lock = channel.tryLock()) {
                if (lock == null) {
                    JOptionPane.showMessageDialog(null, "Dispensary Desktop is already running. Open its control window from the taskbar.");
                    return;
                }
                if (!Files.exists(data.resolve(DesktopStore.DATABASE)) && !chooseDatabase(data)) return;
                String hash = staffPassword(home);
                if (hash == null) return;
                try (var server = (ServletWebServerApplicationContext) new SpringApplicationBuilder(MedicineManagementApplication.class)
                        .headless(false).run("--server.address=127.0.0.1", "--server.port=0",
                                "--spring.datasource.url=jdbc:h2:file:" + data.resolve("pharmacy").toString().replace('\\', '/'),
                                "--spring.datasource.username=sa", "--spring.datasource.password=",
                                "--spring.security.user.name=pharmacist", "--spring.security.user.password=" + hash,
                                "--pharmacy.alerts.enabled=false", "--logging.file.name=" + home.resolve("desktop.log"))) {
                    URI uri = URI.create("http://127.0.0.1:" + server.getWebServer().getPort() + "/");
                    CountDownLatch closed = new CountDownLatch(1);
                    SwingUtilities.invokeAndWait(() -> controlWindow(home, uri, closed));
                    closed.await();
                }
            }
        } catch (Exception failure) {
            JOptionPane.showMessageDialog(null, "Could not open Dispensary.\n" + failure.getMessage()
                    + "\nFor startup details, check %LOCALAPPDATA%\\DispensaryDesktop\\desktop.log.",
                    NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    private static boolean chooseDatabase(Path data) throws IOException {
        String[] actions = {"Import existing records", "Start empty", "Cancel"};
        int choice = JOptionPane.showOptionDialog(null,
                "Do you already have medicines or sales? Close the old app before importing.",
                NAME, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, actions, actions[0]);
        if (choice != 0) return choice == 1;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose the old app's pharmacy.mv.db");
        String relative = "medicine-management-pharmacy-retail-workbench/medicine/data/pharmacy.mv.db";
        for (Path folder : new Path[]{Path.of(System.getProperty("java.io.tmpdir"), "pharmacy-launch"),
                Path.of(System.getenv("LOCALAPPDATA"), "Dispensary")}) {
            Path candidate = folder.resolve(relative);
            if (Files.isRegularFile(candidate)) {
                chooser.setSelectedFile(candidate.toFile());
                break;
            }
        }
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return false;
        DesktopStore.importClosedDatabase(chooser.getSelectedFile().toPath(), data);
        return true;
    }

    private static String staffPassword(Path home) throws IOException {
        Path file = home.resolve("staff.hash");
        if (Files.exists(file)) return Files.readString(file).trim();
        while (true) {
            JPasswordField password = new JPasswordField(), confirm = new JPasswordField();
            Object[] fields = {"Choose a local password for pharmacist (8 or more characters).", password,
                    "Confirm password", confirm};
            if (JOptionPane.showConfirmDialog(null, fields, NAME, JOptionPane.OK_CANCEL_OPTION)
                    != JOptionPane.OK_OPTION) return null;
            char[] first = password.getPassword(), second = confirm.getPassword();
            try {
                if (first.length >= 8 && Arrays.equals(first, second)) {
                    String hash = "{bcrypt}" + new BCryptPasswordEncoder().encode(new String(first));
                    Path pending = Files.createTempFile(home, "staff-", ".tmp");
                    try {
                        Files.writeString(pending, hash);
                        Files.move(pending, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                    } finally {
                        Files.deleteIfExists(pending);
                    }
                    return hash;
                }
                JOptionPane.showMessageDialog(null, "Enter matching passwords of at least eight characters.");
            } finally {
                Arrays.fill(first, '\0');
                Arrays.fill(second, '\0');
            }
        }
    }

    private static void controlWindow(Path home, URI uri, CountDownLatch closed) {
        JFrame frame = new JFrame(NAME);
        JButton open = new JButton("Open pharmacy"), quit = new JButton("Quit Dispensary");
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.add(open);
        buttons.add(quit);
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(new JLabel("Pharmacy is running on this PC. Sign in as pharmacist."), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { closed.countDown(); }
        });
        open.addActionListener(e -> {
            try { openWorkspace(home, uri); }
            catch (IOException err) { JOptionPane.showMessageDialog(frame, "Open this local address in your browser:\n" + uri); }
        });
        quit.addActionListener(e -> frame.dispose());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        open.doClick();
    }

    private static void openWorkspace(Path home, URI uri) throws IOException {
        for (String variable : new String[]{"ProgramFiles(x86)", "ProgramFiles", "LOCALAPPDATA"}) {
            String base = System.getenv(variable);
            if (base == null) continue;
            Path edge = Path.of(base, "Microsoft", "Edge", "Application", "msedge.exe");
            if (Files.isRegularFile(edge)) {
                new ProcessBuilder(edge.toString(), "--app=" + uri, "--no-first-run",
                        "--user-data-dir=" + home.resolve("window")).start();
                return;
            }
        }
        Desktop.getDesktop().browse(uri);
    }

    private DesktopApp() {}
}
