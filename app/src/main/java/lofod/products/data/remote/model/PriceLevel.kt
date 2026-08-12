package lofod.products.data.remote.model

enum class PriceLevel {
    LOW_PRICE {
        override fun text(): String = "Дешево"
    },
    MEDIUM_PRICE {
        override fun text(): String = "Средненько"
    },
    HIGH_PRICE {
        override fun text(): String = "Дорого"
    };

    abstract fun text(): String
}
