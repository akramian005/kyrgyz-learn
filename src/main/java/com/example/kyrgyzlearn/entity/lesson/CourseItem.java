package com.example.kyrgyzlearn.entity.lesson;

// Элемент курса: урок или видео. Входит в юнит (unitId); у уроков и видео общая нумерация position
// внутри курса — по порядку юнитов, поэтому видео можно поставить между любыми уроками (см. CourseItemService).
public interface CourseItem {
    Long getId();

    Integer getPosition();

    void setPosition(Integer position);

    Long getUnitId();

    void setUnitId(Long unitId);
}
