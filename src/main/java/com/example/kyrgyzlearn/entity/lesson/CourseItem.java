package com.example.kyrgyzlearn.entity.lesson;

// Элемент курса: урок или видео. У них общая нумерация position внутри курса,
// поэтому видео можно поставить между любыми уроками (см. CourseItemService).
public interface CourseItem {
    Long getId();

    Integer getPosition();

    void setPosition(Integer position);
}
