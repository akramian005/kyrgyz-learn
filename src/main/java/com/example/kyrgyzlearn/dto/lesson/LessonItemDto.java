package com.example.kyrgyzlearn.dto.lesson;

// Общий интерфейс для элементов урока (вопрос / сообщение-объяснение).
// Jackson сериализует конкретный record как есть, плюс поле "kind" даёт
// фронтенду понять, с каким типом элемента он имеет дело.
// Реализации: QuestionDto (dto.question) и LessonMessageDto (dto.message).
// Не sealed: permits между разными пакетами запрещён вне именованного модуля.
public interface LessonItemDto {
    Long id();
    Integer position();
    String kind();
}
