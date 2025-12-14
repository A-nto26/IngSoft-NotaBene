package com.sweng.notes.debug;

import com.google.gson.*;
import com.sweng.notes.model.Utente;

import org.mapdb.*;
import java.io.*;

public class UserDBInspector {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    
    private static final Serializer<Utente> USER_SERIALIZER = new Serializer<Utente>() {

        @Override
        public void serialize(DataOutput2 out, Utente value) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
            oos.close();

            byte[] data = baos.toByteArray();
            out.writeInt(data.length);
            out.write(data);
        }

        @Override
        public Utente deserialize(DataInput2 in, int available) throws IOException {
            int length = in.readInt();
            byte[] data = new byte[length];
            in.readFully(data);

            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);

            try {
                return (Utente) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException("Errore nella deserializzazione dell'Utente", e);
            }
        }
    };

    public static void main(String[] args) {

        // =====================================================
        //  TROVA users.db in modo robusto (root / backend / ecc.)
        // =====================================================
        File dbFile = new File("data/users.db");

        // Se lanci da root progetto, spesso il db è in backend/data/users.db
        if (!dbFile.exists()) {
            dbFile = new File("backend/data/users.db");
        }

        // Fallback extra: se lanci da backend/, allora "data/users.db" va già bene,
        // ma teniamo anche questo per sicurezza
        if (!dbFile.exists()) {
            dbFile = new File("backend\\data\\users.db"); // Windows-safe
        }

        if (!dbFile.exists()) {
            System.out.println("❌ ERRORE: Il file users.db non esiste!");
            System.out.println("   Cercato in: data/users.db e backend/data/users.db");
            return;
        }

        System.out.println("✅ DB trovato: " + dbFile.getPath());

        System.out.println("\n======================================");
        System.out.println("   👤 ISPEZIONE DATABASE UTENTI");
        System.out.println("======================================\n");

        DB db = DBMaker.fileDB(dbFile)
                .readOnly()
                .closeOnJvmShutdown()
                .make();

        HTreeMap<String, Utente> userMap = db
                .hashMap("users", Serializer.STRING, USER_SERIALIZER)
                .open();

        if (userMap.isEmpty()) {
            System.out.println("📭 Nessun utente presente.");
        }

        userMap.forEach((username, utente) -> {
            System.out.println("────────────────────────────────────────");
            System.out.println("👤 Utente: " + username);
            System.out.println("────────────────────────────────────────");

            System.out.println(gson.toJson(utente));
            System.out.println();
        });

        db.close();
        System.out.println("✔ Ispezione completata.");
    }
}
