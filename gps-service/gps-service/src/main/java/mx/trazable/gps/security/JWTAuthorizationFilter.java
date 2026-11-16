package mx.trazable.gps.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import mx.trazable.gps.core.config.WebServiceResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;

@AllArgsConstructor
@Component
public class JWTAuthorizationFilter extends OncePerRequestFilter {

    private final TokenUtils tokenUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String bearerToken = request.getHeader("Authorization");

        // Si no hay token, continúa sin romper el flujo (permitir endpoints públicos)
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = bearerToken.substring(7).trim(); // quitar "Bearer " y espacios

        // Si el token está vacío, retornar error claro
        if (token.isEmpty()) {
            sendErrorResponse(response, "Empty JWT token");
            return;
        }

        Object authorization = tokenUtils.getAuthorization(token);

        if (authorization instanceof UsernamePasswordAuthenticationToken usernamePAT) {
            SecurityContextHolder.getContext().setAuthentication(usernamePAT);
            filterChain.doFilter(request, response);
        } else if (authorization instanceof String errorMessage) {
            sendErrorResponse(response, errorMessage);
        } else {
            sendErrorResponse(response, "Invalid token type");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        WebServiceResponse errorResponse = new WebServiceResponse(false, message);
        ObjectMapper mapper = new ObjectMapper();
        try (PrintWriter out = response.getWriter()) {
            mapper.writeValue(out, errorResponse);
        }
    }

}

