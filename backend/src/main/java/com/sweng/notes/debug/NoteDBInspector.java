package com.sweng.notes.debug;

import com.google.gson.*;
import com.sweng.notes.model.Note;

import org.mapdb.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NoteDBInspector {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (value, type, context) ->
                            new JsonPrimitive(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .create();


    private static final Serializer<Note> NOTE_SERIALIZER = new Serializer<Note>() {

        @Override
        public void serialize(DataOutput2 out, Note value) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
            oos.close();

            byte[] data = baos.toByteArray();
            out.writeInt(data.length);
            out.write(data);
        }

        @Override
        public Note deserialize(DataInput2 in, int available) throws IOException {
            int length = in.readInt();
            byte[] data = new byte[length];
            in.readFully(data);

            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);

            try {
                return (Note) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException("Errore nella deserializzazione della Note", e);
            }
        }
    };

    public static void main(String[] args) {

        File dbFile = new File("data/notes.db");

        if (!dbFile.exists()) {
            System.out.println("❌ ERRORE: Il file notes.db non esiste!");
            return;
        }

        System.out.println("\n======================================");
        System.out.println("   📝 ISPEZIONE DATABASE NOTE (notes.db)");
        System.out.println("======================================\n");

        DB db = DBMaker.fileDB(dbFile)
                .readOnly()
                .closeOnJvmShutdown()
                .make();

        HTreeMap<Integer, Note> noteMap = db
                .hashMap("notes", Serializer.INTEGER, NOTE_SERIALIZER)
                .open();

        if (noteMap.isEmpty()) {
            System.out.println("📭 Nessuna nota presente.");
        }

        noteMap.forEach((id, nota) -> {
            System.out.println("────────────────────────────────────────");
            System.out.println("📝 Nota ID: " + id);
            System.out.println("────────────────────────────────────────");

            System.out.println(gson.toJson(nota));
            System.out.println();
        });

        db.close();
        System.out.println("✔ Ispezione completata.");
    }
}
