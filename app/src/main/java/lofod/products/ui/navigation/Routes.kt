package lofod.products.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CATALOG = "catalog"
    const val CARD_CREATE = "card/create/{categoryId}"
    const val CARD_EDIT = "card/edit/{categoryId}/{cardId}"
    const val CATEGORY_CREATE = "category/create/{parentId}"
    const val CATEGORY_EDIT = "category/edit/{categoryId}"
    const val CATEGORY_MEMBERS = "category/{categoryId}/members"

    const val ARG_CATEGORY_ID = "categoryId"
    const val ARG_CARD_ID = "cardId"
    const val ARG_PARENT_ID = "parentId"

    /** SavedStateHandle key on catalog entry: set true after card form save. */
    const val KEY_CARD_FORM_SAVED = "card_form_saved"

    /** SavedStateHandle key on catalog entry: set true after category form save. */
    const val KEY_CATEGORY_FORM_SAVED = "category_form_saved"

    fun cardCreate(categoryId: String): String = "card/create/$categoryId"

    fun cardEdit(categoryId: String, cardId: String): String =
        "card/edit/$categoryId/$cardId"

    fun categoryCreate(parentId: String): String = "category/create/$parentId"

    fun categoryEdit(categoryId: String): String = "category/edit/$categoryId"

    fun categoryMembers(categoryId: String): String = "category/$categoryId/members"
}
