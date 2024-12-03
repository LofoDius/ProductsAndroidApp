package lofod.products.api.model

enum class PriceLevel {
    LOW_PRICE {
        override fun text(): String {
            return "Дешево"
        }
    },
    MEDIUM_PRICE {
        override fun text(): String {
            return "Средненько"
        }
    },
    HIGH_PRICE {
        override fun text(): String {
            return "Дорого"
        }
    };

    abstract fun text(): String
}