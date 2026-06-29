package bioroute.util;

import bioroute.exception.DatosException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilidad para generar y verificar hashes de contrasena con SHA-256 y sal.
 * Evita almacenar contrasenas en texto plano en la base de datos.
 */
public final class HashUtil {

    private static final int SAL_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private HashUtil() { }

    public static String generarSal() {
        byte[] sal = new byte[SAL_BYTES];
        RANDOM.nextBytes(sal);
        return Base64.getEncoder().encodeToString(sal);
    }

    public static String hashear(String contrasena, String sal) throws DatosException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(sal));
            byte[] bytes = digest.digest(contrasena.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new DatosException("No se pudo aplicar el algoritmo de hash.", e);
        }
    }

    public static boolean verificar(String contrasena, String sal, String hashEsperado)
            throws DatosException {
        return hashear(contrasena, sal).equals(hashEsperado);
    }

    /**
     * Metodo main de utilidad para generar hashes nuevos desde consola.
     * Ejecutar con: java bioroute.util.HashUtil [contrasena]
     */
    public static void main(String[] args) throws DatosException {
        String contrasena = args.length > 0 ? args[0] : "admin123";
        String sal = generarSal();
        String hash = hashear(contrasena, sal);
        System.out.println("Contrasena: " + contrasena);
        System.out.println("Sal:        " + sal);
        System.out.println("Hash:       " + hash);
        System.out.println();
        System.out.println("SQL para insertar:");
        System.out.printf("UPDATE usuarios SET hash_contrasena='%s', sal='%s' WHERE nombre_usuario='???';%n",
            hash, sal);
    }
}
