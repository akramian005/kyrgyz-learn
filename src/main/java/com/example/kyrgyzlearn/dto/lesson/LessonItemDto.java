package com.example.kyrgyzlearn.dto.lesson;

// Общий интерфейс для элементов урока (вопрос / сообщение-объяснение).
// Jackson сериализует конкретный record как есть, плюс поле "kind" даёт
// фронтенду понять, с каким типом элемента он имеет дело.
public sealed interface LessonItemDto permits QuestionDto, LessonMessageDto {
    Long id();
    Integer position();
    String kind();
}
