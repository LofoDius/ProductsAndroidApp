package lofod.products.data.remote.model

enum class CustomFieldType {
    TEXT {
        override fun text(): String = "Текст"
    },
    NUMBER {
        override fun text(): String = "Число"
    },
    BOOLEAN {
        override fun text(): String = "Переключатель"
    },
    DATE {
        override fun text(): String = "Дата"
    },
    COUNTER {
        override fun text(): String = "Счётчик"
    };

    abstract fun text(): String
}
