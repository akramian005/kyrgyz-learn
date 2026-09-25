package com.example.kyrgyzlearn.jwt;

//package com.example.demo2.shared.security.filter;

import com.example.kyrgyzlearn.security.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    /*
     * Метод говорит Spring, какие запросы не нужно прогонять через JWT-фильтр.
     * Публичные endpoint-ы должны открываться даже без токена или со старым токеном в клиенте.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        return HttpMethod.OPTIONS.matches(method)
                || path.startsWith("/api.http/auth/")
                || path.equals("/error")
                || isPublicGetEndpoint(method, path);
    }

    /*
     * Публичные GET endpoint-ы каталога.
     * Смотреть продукты и категории можно всем, а изменять их можно только ADMIN.
     */
    private boolean isPublicGetEndpoint(String method, String path) {
        return HttpMethod.GET.matches(method)
                && (path.equals("/api.http/products")
                || path.startsWith("/api.http/products/")
                || path.equals("/api.http/categories")
                || path.startsWith("/api.http/categories/"));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        /*
         * Authorization должен выглядеть так:
         * Authorization: Bearer <jwt-token>
         */
        String authHeader = request.getHeader("Authorization");

        /*
         * Если токена нет, фильтр пропускает запрос дальше.
         * Потом SecurityConfig решит: endpoint публичный или нужно вернуть 401.
         */
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * Убираем "Bearer " и оставляем только сам JWT.
         */
        String token = authHeader.substring(7);

        try {
            /*
             * Достаем email из токена.
             * Если токен битый или просроченный, JwtService выбросит исключение.
             */
            String email = jwtService.extractEmail(token);

            /*
             * Если пользователь еще не установлен в SecurityContext,
             * загружаем его из базы и проверяем токен относительно этого пользователя.
             */
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                /*
                 * Если токен валиден, создаем Authentication object.
                 * Authorities берутся из UserDetails, там лежит ROLE_USER или ROLE_ADMIN.
                 */
                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            /*
             * Любая проблема с токеном означает, что запрос нельзя авторизовать.
             * Чистим SecurityContext и возвращаем 401.
             */
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);

    }
}
