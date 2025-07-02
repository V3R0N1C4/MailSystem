package common.model;

import java.util.regex.Pattern;

/**
 * Classe di utilità per la validazione del formato delle email.
 */
public class EmailValidator {
    /**
     * Espressione regolare per validare il formato di un indirizzo email.
     * Accetta lettere, numeri e alcuni caratteri speciali prima della @,
     * e un dominio con estensione da 2 a 7 lettere.
     */
    private static final String EMAIL_PATTERN =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                    "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";  // Parte dominio (con punto obbligatorio)

    /**
     * Pattern compilato per la validazione delle email.
     */
    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    /**
     * Verifica se la stringa fornita rispetta il formato di un indirizzo email valido.
     *
     * @param email la stringa da validare
     * @return true se la stringa è un'email valida, false altrimenti
     */
    public static boolean isValidEmailFormat(String email) {
        if (email == null) return false; // Controlla se la stringa è null
        return pattern.matcher(email).matches(); // Verifica la corrispondenza con il pattern
    }
}
