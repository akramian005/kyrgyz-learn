package com.example.kyrgyzlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Address {

    @Column(name = "country")
    private String country;

    @Column(name = "city")
    private String city;

    @Column(name = "street")
    private String street;

    @Column(name = "house_number")
    private String houseNumber;

    @Column(name = "apartment")
    private String apartment;

    @Column(name = "postal_code")
    private String postalCode;
}

//public class Address {
//
//    @NotBlank(message = "Страна обязательна")
//    private String country;
//
//    @NotBlank(message = "Город обязателен")
//    private String city;
//
//    @NotBlank(message = "Улица обязательна")
//    private String street;
//
//    @NotBlank(message = "Индекс обязателен")
//    private String postalCode;
//
//}