package lofod.products.data.remote.model

enum class QualityLevel {
    LOW_QUALITY {
        override fun text(): String = "Бич"
    },
    MEDIUM_QUALITY {
        override fun text(): String = "Ну норм"
    },
    HIGH_QUALITY {
        override fun text(): String = "Лухари"
    };

    abstract fun text(): String
}
