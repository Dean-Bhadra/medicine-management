package com.debanjan.desktop;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.Channels;
import java.nio.channels.OverlappingFileLockException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;


final class DesktopStore {
    static final String DATABASE = "pharmacy.mv.db";

    static void importClosedDatabase(Path source, Path directory) throws IOException {
        if (Files.exists(directory.resolve(DATABASE))) {
            throw new IOException("A desktop database already exists.");
        }
        if (!source.getFileName().toString().equalsIgnoreCase(DATABASE)) {
            throw new IOException("Choose the pharmacy.mv.db file from the old app's data folder.");
        }
        Path temporary = Files.createTempFile(directory, "import-", ".mv.db");
        try (FileChannel channel = FileChannel.open(source, StandardOpenOption.READ, StandardOpenOption.WRITE);
             FileLock lock = channel.tryLock()) {
            if (lock == null) throw new IOException("Close the old pharmacy app before importing its database.");
            // Read through the owning channel. Windows enforces file locks across handles.
            try (var output = Files.newOutputStream(temporary)) {
                Channels.newInputStream(channel).transferTo(output);
            }
            if (Files.size(temporary) == 0) throw new IOException("The selected database is empty.");
            verifyPharmacyDatabase(temporary);
            Files.move(temporary, directory.resolve(DATABASE), ATOMIC_MOVE);
        } catch (OverlappingFileLockException e) {
            throw new IOException("Close the old pharmacy app before importing its database.", e);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void verifyPharmacyDatabase(Path file) throws IOException {
        String path = file.toAbsolutePath().toString();
        String base = path.substring(0, path.length() - ".mv.db".length());
        try (var db = DriverManager.getConnection("jdbc:h2:file:" + base + ";IFEXISTS=TRUE;ACCESS_MODE_DATA=r", "sa", "");
             var statement = db.createStatement()) {
            statement.executeQuery("SELECT id FROM medicine WHERE 1=0").close();
            statement.executeQuery("SELECT id FROM sale WHERE 1=0").close();
        } catch (SQLException e) {
            throw new IOException("Choose a pharmacy database created by the local H2 version of the app.", e);
        }
    }

    private DesktopStore() {}
}
