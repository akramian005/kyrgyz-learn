package com.example.kyrgyzlearn.config;

import com.example.kyrgyzlearn.jwt.JwtFilter;
import com.example.kyrgyzlearn.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // это добавленно
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                /*
                 * CSRF обычно нужен для браузерных session/cookie приложений.
                 * Здесь API stateless и авторизация идет через JWT, поэтому CSRF отключен.
                 */
                .csrf(csrf -> csrf.disable())
                /*
                 * Отключает стандартную HTML-форму логина Spring Security.
                 * Логин в этом проекте идет через /api.http/auth/login.
                 */
                .formLogin(form -> form.disable())
                /*
                 * Отключает HTTP Basic auth, чтобы приложение не принимало логин/пароль
                 * через стандартный browser/basic механизм.
                 */
                .httpBasic(basic -> basic.disable())
                /*
                 * Включает CORS-настройки из corsConfigurationSource().
                 * Это нужно, чтобы frontend с другого origin мог обращаться к API.
                 */
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                /*
                 * Делает приложение stateless: сервер не хранит session пользователя.
                 * Каждый закрытый запрос должен сам принести JWT-токен.
                 */
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                /*
                 * Настраивает ответы при ошибках доступа:
                 * 401 - пользователь не авторизован или токен отсутствует/невалиден,
                 * 403 - пользователь авторизован, но у него недостаточно прав.
                 */
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(401, "Unauthorized"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(403, "Forbidden"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/login", "/register", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/error").permitAll()
                        // файлы официальных курсов (см. OfficialMediaConfig) — как и сами курсы, открыты всем
                        .requestMatchers(HttpMethod.GET, "/media/official/**").permitAll()

                        // Курсы: чтение открыто всем (официальные — на главной, личные — через поиск).
                        // Создание/изменение/удаление — авторизованным, а какой именно курс им можно
                        // трогать (OFFICIAL — никому, USER — только владельцу) решает CourseService.
                        .requestMatchers(HttpMethod.GET, "/api/courses/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/courses/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/courses/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/**").authenticated()

                        // Уроки (+ вопросы и сообщения) — та же логика, права по владельцу курса проверяют LessonService, QuestionService, LessonMessageService
                        .requestMatchers(HttpMethod.GET, "/api/lessons/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/lessons/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/lessons/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/lessons/**").authenticated()

                        .anyRequest().authenticated()
                )
                /*
                 * Подключает provider, который проверяет email/пароль пользователя через базу
                 * и сравнивает пароль через BCrypt PasswordEncoder.
                 */
                .authenticationProvider(authenticationProvider())
                /*
                 * Ставит JwtFilter перед стандартным UsernamePasswordAuthenticationFilter.
                 * Так JWT проверяется до того, как Spring примет решение об авторизации запроса.
                 */
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /*
     * CORS-настройки для запросов с frontend-а.
     * Разрешена веб-версия Expo с этого компьютера (localhost) и из домашней/локальной сети —
     * чтобы открыть приложение в браузере телефона по IP компьютера (http://192.168.x.x:8081).
     * Любые headers и основные HTTP-методы для REST API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // шаблоны, а не точные адреса: IP компьютера в сети может меняться
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:[*]",
                "http://127.0.0.1:[*]",
                "http://192.168.*.*:[*]",
                "http://10.*.*.*:[*]",
                "http://172.*.*.*:[*]"
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /*
     * PasswordEncoder хеширует пароли при регистрации и проверяет их при логине.
     * BCrypt - стандартный безопасный вариант для хранения паролей.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
     * AuthenticationProvider говорит Spring Security, как проверять пользователя.
     * Он берет пользователя через CustomUserDetailsService и сравнивает пароль через BCrypt.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /*
     * AuthenticationManager используется в AuthService при логине.
     * Через него Spring проверяет email и пароль, которые пришли в /api.http/auth/login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}


