package server.model;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * Adapter personalizzato per serializzare e deserializzare oggetti LocalDateTime con Gson.
 */
public class LocalDateTimeTypeAdapter extends TypeAdapter<LocalDateTime> {
    // Formatter per convertire LocalDateTime in stringa e viceversa, usando lo standard ISO
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Scrive un oggetto LocalDateTime come stringa formattata in JSON.
     * @param out writer JSON
     * @param value valore LocalDateTime da serializzare
     * @throws IOException in caso di errori di scrittura
     */
    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        if (value == null) {
            // Scrive un valore null se l'oggetto è null
            out.nullValue();
        } else {
            // Scrive la data/ora come stringa formattata
            out.value(formatter.format(value));
        }
    }

    /**
     * Legge una stringa formattata da JSON e la converte in LocalDateTime.
     * @param in reader JSON
     * @return oggetto LocalDateTime deserializzato o null
     * @throws IOException in caso di errori di lettura
     */
    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        if (in.hasNext()) {
            // Legge la stringa e la converte in LocalDateTime usando il formatter
            String dateTimeString = in.nextString();
            return LocalDateTime.parse(dateTimeString, formatter);
        }
        // Restituisce null se non ci sono altri elementi
        return null;
    }
}
