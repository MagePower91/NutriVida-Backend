//El backend responde con un accessToken, que dura 15 minutos y se envía en cada petición,
// y un refreshToken, que dura 7 días y sirve para pedir un token nuevo
// sin volver a escribir la contraseña.

package cl.nutrivida.identidad.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(length = 36)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_run", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    private boolean revocado = false;

    protected RefreshToken() { }

    public RefreshToken(String token, Usuario usuario, Instant expiraEn) {
        this.token = token;
        this.usuario = usuario;
        this.expiraEn = expiraEn;
    }

    public boolean estaVigente() {
        return !revocado && Instant.now().isBefore(expiraEn);
    }

    public String getToken() { return token; }
    public Usuario getUsuario() { return usuario; }
    public void setRevocado(boolean revocado) { this.revocado = revocado; }
}