package com.example.kyrgyzlearn.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

// Страница из списка в памяти — для выдачи, где официальные курсы (из файлов) идут вместе с данными из БД
final class Pages {

    private Pages() {
    }

    static <T> Page<T> of(List<T> all, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new PageImpl<>(all);
        }
        int from = (int) Math.min(pageable.getOffset(), all.size());
        int to = Math.min(from + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(from, to), pageable, all.size());
    }
}
