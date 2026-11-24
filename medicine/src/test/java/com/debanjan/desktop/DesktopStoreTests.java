package com.debanjan.desktop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class DesktopStoreTests {
    @TempDir Path root;

    @Test
    void copiesClosedStoreAndPreservesBothRecordsAndSource() throws Exception {
        Path old = Files.createDirectories(root.resolve("old pharmacy"));
        Path target = Files.createDirectories(root.resolve("desktop data"));
        String url = "jdbc:h2:file:" + old.resolve("pharmacy");
        Path source = old.resolve(DesktopStore.DATABASE);
        try (var db = DriverManager.getConnection(url, "sa", ""); var sql = db.createStatement()) {
            sql.execute("create table medicine(id bigint)");
            sql.execute("create table sale(id bigint)");
            sql.execute("insert into medicine values (42)");
            sql.execute("insert into sale values (7)");
            assertThrows(IOException.class, () -> DesktopStore.importClosedDatabase(source, target));
        }
        byte[] original = Files.readAllBytes(source);
        DesktopStore.importClosedDatabase(source, target);
        assertArrayEquals(original, Files.readAllBytes(source));
        assertArrayEquals(original, Files.readAllBytes(target.resolve(DesktopStore.DATABASE)));
        assertThrows(IOException.class, () -> DesktopStore.importClosedDatabase(source, target));
        try (var db = DriverManager.getConnection("jdbc:h2:file:" + target.resolve("pharmacy"), "sa", "");
             var sql = db.createStatement(); var rows = sql.executeQuery("select medicine.id, sale.id from medicine, sale")) {
            assertTrue(rows.next());
            assertEquals(42, rows.getLong(1));
            assertEquals(7, rows.getLong(2));
        }
    }

    @Test
    void rejectsInvalidDatabaseBeforeInstallingIt() throws IOException {
        Path file = Files.writeString(root.resolve(DesktopStore.DATABASE), "invalid");
        Path destination = Files.createDirectory(root.resolve("desktop"));
        assertThrows(IOException.class, () -> DesktopStore.importClosedDatabase(file, destination));
        assertFalse(Files.exists(destination.resolve(DesktopStore.DATABASE)));
    }
}
