package lofod.products.api.model

enum class QualityLevel {
    LOW_QUALITY {
        override fun text(): String {
            return "Бич"
        }
    },
    MEDIUM_QUALITY {
        override fun text(): String {
            return "Ну норм"
        }
    },
    HIGH_QUALITY {
        override fun text(): String {
            return "Лухари"
        }
    };

    abstract fun text(): String
}