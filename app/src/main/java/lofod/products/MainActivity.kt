package lofod.products

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults.textFieldColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lofod.products.api.RetrofitInstance
import lofod.products.api.model.PriceLevel
import lofod.products.api.model.QualityLevel
import lofod.products.api.response.CardResponse
import lofod.products.api.response.CategoryResponse
import lofod.products.ui.theme.ProductsTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun MainScreen() {
    var cards by remember { mutableStateOf(emptyList<CardResponse>()) }
    var searchCards by remember { mutableStateOf(emptyList<CardResponse>()) }
    var isSearching by remember { mutableStateOf(false) }
    var isEditCategoryMode by remember { mutableStateOf(false) }
    var editCategoryId: String? = null
    var categories by remember { mutableStateOf<List<CategoryResponse>?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val categoryApi = RetrofitInstance.categoryApi


    fun getCards(categoryId: String) {
        categoryApi.getCategoryCards(categoryId).enqueue(object : Callback<List<CardResponse>> {
            override fun onResponse(
                call: Call<List<CardResponse>?>,
                response: Response<List<CardResponse>?>
            ) {
                cards = response.body()!!
            }

            override fun onFailure(
                call: Call<List<CardResponse>?>,
                t: Throwable
            ) {
                cards = emptyList()
            }

        })
    }

    fun getCategories(parentId: String?) {
        coroutineScope.launch {
            categoryApi.getCategories().enqueue(object : Callback<List<CategoryResponse>> {
                override fun onResponse(
                    call: Call<List<CategoryResponse>?>,
                    response: Response<List<CategoryResponse>?>
                ) {
                    categories = getCategoriesByParentId(parentId, response.body()!!)
                    if (parentId != null)
                        getCards(parentId)
                }

                override fun onFailure(
                    call: Call<List<CategoryResponse>?>,
                    t: Throwable
                ) {

                }

            })
        }
    }

    LaunchedEffect("apiCall") {
        getCategories(null)
    }

    ProductsTheme {
        Column {
            SearchableTopAppBar(
                categories = emptyList(),
                onCategoryEdit = { isEditCategoryMode = true; editCategoryId = it },
                onCategoryChoose = {
                    getCategories(it)
                },
                onOpenSearch = { isSearching = true },
                onCloseSearch = { isSearching = false },
                onSearch = { searchText ->
                    println("searchText: $searchText")
                },
            )

            if (cards.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(cards) { card ->
                        CardView(card = card)
                    }
                }
            } else {
                Text(
                    text = "Выберите категорию в боковом меню"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableTopAppBar(
    categories: List<CategoryResponse>,
    onCategoryChoose: (String?) -> Unit,
    onCategoryEdit: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearch: (String) -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    var isNavigationOpen by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    CenterAlignedTopAppBar(
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    slideInVertically(
                        animationSpec = tween(250),
                        initialOffsetY = { -400 }
                    ) togetherWith slideOutVertically(animationSpec = tween(100)) + fadeOut(tween(100))
                },
                label = "Строка поиска"
            ) { targetState ->
                if (targetState) {
                    TextField(
                        colors = textFieldColors(
                            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Введите текст") },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                onSearch(searchText.text)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true
                    )

                    LaunchedEffect(Unit) {
                        delay(100)
                        focusRequester.requestFocus()
                    }
                } else {
                    Text(
                        text = "Product App!",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        actions = {
            if (isSearchActive) {
                IconButton(onClick = {
                    isSearchActive = false
                    searchText = TextFieldValue("")
                    focusManager.clearFocus()
                    onCloseSearch()
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                }
            } else {
                IconButton(onClick = {
                    isSearchActive = true
                    onOpenSearch()
                }) {
                    Icon(Icons.Filled.Search, contentDescription = "Поиск")
                }
            }
        },
        navigationIcon = {
            AnimatedContent(
                targetState = isNavigationOpen,
                transitionSpec = {
                    slideInVertically(
                        animationSpec = tween(250),
                        initialOffsetY = { -400 }
                    ) togetherWith slideOutVertically(animationSpec = tween(100)) + fadeOut(tween(100))
                },
                label = "Строка поиска"
            ) {
                if (it) {
                    DropdownMenu(
                        expanded = isNavigationOpen,
                        onDismissRequest = { isNavigationOpen = false },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CategoryNavigator(
                            categories = categories,
                            onCategoryChoose = onCategoryChoose,
                            onCategoryEdit = onCategoryEdit
                        )
                    }
                } else {
                    IconButton(onClick = { isNavigationOpen = true }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Список категорий"
                        )
                    }
                }
            }
        },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryNavigator(
    categories: List<CategoryResponse>,
    onCategoryChoose: (String) -> Unit,
    onCategoryEdit: (String) -> Unit
) {

    ProductsTheme {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(categories) { category ->
                CategoryView(
                    category = category,
                    onCategoryEdit = onCategoryEdit,
                    onCategoryChoose = onCategoryChoose
                )
            }
        }
    }
}

@Composable
fun CategoryView(category: CategoryResponse, onCategoryEdit: (String) -> Unit, onCategoryChoose: (String) -> Unit) {
    ProductsTheme {
        Row(modifier = Modifier.clickable(
            enabled = true,
            onClick = { onCategoryChoose(category.categoryId) }
        )) {
            Text(text = category.name)
            IconButton(onClick = { onCategoryEdit(category.categoryId) }) {
                Icon(Icons.Filled.Create, contentDescription = "Редактировать")
            }
        }
    }
}

@Composable
fun CardView(card: CardResponse) {
    ProductsTheme {
        Column {
            Text(text = card.name)
            Row {
                Text(text = mapPriceLevel(card.priceLevel))
                Text(text = mapQualityLevel(card.qualityLevel))
            }
        }
    }
}

fun getCategoriesByParentId(parentId: String?, categories: List<CategoryResponse>): List<CategoryResponse> {
    return if (parentId == null) {
        categories
    } else {
        findParentByCategoryId(parentId, categories)?.subcategories ?: emptyList()
    }
}

fun findParentByCategoryId(categoryId: String, categories: List<CategoryResponse>): CategoryResponse? {
    categories.forEach {
        return if (it.categoryId == categoryId) {
            it
        } else {
            findParentByCategoryId(categoryId, categories)
        }
    }

    return null
}

fun mapPriceLevel(priceLevel: PriceLevel): String {
    return when (priceLevel) {
        PriceLevel.LOW_PRICE -> "Дешево"
        PriceLevel.MEDIUM_PRICE -> "Средненько"
        PriceLevel.HIGH_PRICE -> "Дорого"
    }
}

fun mapQualityLevel(qualityLevel: QualityLevel): String {
    return when (qualityLevel) {
        QualityLevel.LOW_QUALITY -> "Такое себе"
        QualityLevel.MEDIUM_QUALITY -> "Нормас"
        QualityLevel.HIGH_QUALITY -> "Лухари"
    }
}
