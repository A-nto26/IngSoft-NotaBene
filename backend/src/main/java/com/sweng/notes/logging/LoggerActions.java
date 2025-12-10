package com.sweng.notes.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class LoggerActions {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static String getLogFilePath() {
        return "logs/actions-" + LocalDateTime.now().format(DATE) + ".jsonl";
    }

    public static synchronized void log(String event, String user, Map<String, Object> details) {
        try {
            String file = getLogFilePath();
            try (FileWriter fw = new FileWriter(file, true)) {

                var json = gson.toJson(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "event", event,
                        "user", user,
                        "details", details
                ));

                fw.write(json + "\n");
            }
        } catch (IOException e) {
            System.err.println("❌ Errore nel logging azione: " + e.getMessage());
        }
    }
}
