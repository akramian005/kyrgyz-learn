package com.example.kyrgyzlearn.entity.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder //для удобного создания
public class User { //implements UserDetails

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String phone;

    // отображаемое имя; у аккаунтов, созданных до появления поля, может быть null
    private String name;

    // ключ картинки в MinIO; ссылку для клиента подписывает FileStorageService
    private String avatarObjectKey;
}