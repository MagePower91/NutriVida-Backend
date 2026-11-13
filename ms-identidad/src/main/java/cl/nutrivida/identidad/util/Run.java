//Valida el RUN chileno con el mismo cálculo que usa el frontend.

package cl.nutrivida.identidad.util;

public final class Run {

    private Run() { }

    public static String normalizar(String run) {
        return run == null ? "" : run.trim().toUpperCase();
    }

    public static boolean esValido(String run) {
        String r = normalizar(run);
        if (!r.matches("^[0-9]{6,8}[0-9K]$")) return false;
        String cuerpo = r.substring(0, r.length() - 1);
        char dv = r.charAt(r.length() - 1);
        int suma = 0, mult = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * mult;
            mult = (mult == 7) ? 2 : mult + 1;
        }
        int resto = 11 - (suma % 11);
        char esperado = resto == 11 ? '0' : resto == 10 ? 'K' : (char) ('0' + resto);
        return dv == esperado;
    }
}