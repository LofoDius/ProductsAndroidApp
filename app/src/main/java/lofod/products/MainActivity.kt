package lofod.products

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults.textFieldColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.coroutineScope
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
            var category by remember { mutableStateOf<CategoryResponse?>(null) }
            val categoryApi = RetrofitInstance.categoryApi

            LaunchedEffect("apiCall") {
                coroutineScope {
                    categoryApi.getCategories().enqueue(object : Callback<List<CategoryResponse>> {
                        override fun onResponse(
                            call: Call<List<CategoryResponse>?>,
                            response: Response<List<CategoryResponse>?>
                        ) {
                            category = CategoryResponse(
                                name = "Все категории",
                                categoryId = "-1",
                                parentId = null,
                                subcategoriesAmount = 0,
                                cardsAmount = 0,
                                subcategories = response.body()!!,
                                imageId = null
                            )
                        }

                        override fun onFailure(
                            call: Call<List<CategoryResponse>?>,
                            t: Throwable
                        ) {
                        }
                    })
                }
            }

            if (category != null)
                MainScreen(category!!)
            else
                CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(category: CategoryResponse) {
    var cards by remember { mutableStateOf(emptyList<CardResponse>()) }
    var searchCards by remember { mutableStateOf(emptyList<CardResponse>()) }
    var isSearching by remember { mutableStateOf(false) }
    var isEditCategoryMode by remember { mutableStateOf(false) }
    var editCategoryId: String? = null
    var category by remember { mutableStateOf<CategoryResponse?>(category) }

    val coroutineScope = rememberCoroutineScope()
    val categoryApi = RetrofitInstance.categoryApi

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)

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

    fun getCategory(categoryId: String?) {
        coroutineScope.launch {
            categoryApi.getCategories().enqueue(object : Callback<List<CategoryResponse>> {
                override fun onResponse(
                    call: Call<List<CategoryResponse>?>,
                    response: Response<List<CategoryResponse>?>
                ) {
                    if (categoryId == null) {
                        category = CategoryResponse(
                            name = "Все категории",
                            categoryId = "-1",
                            parentId = null,
                            subcategoriesAmount = 0,
                            cardsAmount = 0,
                            subcategories = response.body()!!,
                            imageId = null
                        )
                    } else {
                        category = findCategoryById(categoryId, response.body()!!)
                        getCards(categoryId)
                    }
                }

                override fun onFailure(
                    call: Call<List<CategoryResponse>?>,
                    t: Throwable
                ) {

                }

            })
        }
    }

    ProductsTheme {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.fillMaxHeight()) {
                    Spacer(
                        Modifier
                            .height(Dp(8f))
                            .fillMaxWidth()
                    )
                    Text(
                        text = "Выберите категорию",
                        fontSize = TextUnit(20f, TextUnitType.Sp),
                    )
                    HorizontalDivider()
                    if (category?.parentId != null || category?.categoryId != "-1") {
                        NavigationDrawerItem(
                            label = { Text(text = "Назад") },
                            selected = false,
                            onClick = {
                                getCategory(category!!.parentId)
                            }
                        )
                    }
                    if (category?.subcategories?.isNotEmpty() == true) {
                        category!!.subcategories.forEach {
                            NavigationDrawerItem(
                                label = {
                                    CategoryView(it, onCategoryEdit = {
                                        isEditCategoryMode = true
                                        editCategoryId = it
                                    })
                                },
                                selected = false,
                                onClick = {
                                    getCategory(it.categoryId)
                                }
                            )
                        }
                    } else {
                        Text("У этой категории нет подкатегорий")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                isEditCategoryMode = true
                                editCategoryId = null
                            }
                        }
                    ) {
                        Text("Добавить категорию")
                    }
                }
            },
            drawerState = drawerState,
        ) {
            Scaffold(
                topBar = {
                    SearchableTopAppBar(
                        navigationDrawerState = drawerState,
                        onOpenSearch = { isSearching = true },
                        onCloseSearch = { isSearching = false },
                        onSearch = { searchText ->
                            println("searchText: $searchText")
                        },
                    )
                }
            ) { innerPadding ->
                if (cards.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(cards) { card ->
                            CardView(card = card)
                        }
                    }
                } else {
                    Text(
                        text = "Выберите категорию в боковом меню",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
//
//                if (isEditCategoryMode) {
//
//                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableTopAppBar(
    navigationDrawerState: DrawerState,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearch: (String) -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf(TextFieldValue("")) }


    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

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
            IconButton(onClick = {
                coroutineScope.launch {
                    navigationDrawerState.apply {
                        if (isOpen) close() else open()
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Список категорий"
                )
            }
        },
    )
}

@Composable
fun CategoryView(category: CategoryResponse, onCategoryEdit: (String) -> Unit) {
    ProductsTheme {
        Row {
            Text(text = category.name)
            IconButton(onClick = { onCategoryEdit(category.categoryId) }) {
                Icon(Icons.Filled.Create, contentDescription = "Редактировать")
            }
        }
    }
}

@Composable
fun CardView(card: CardResponse) {
    val maxImageHeight = 160.dp
    val placeholder = ImageBitmap.imageResource(R.drawable.placeholder)
    var image by remember { mutableStateOf<ImageBitmap>(placeholder) }

    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect("apiCall") {
        coroutineScope.launch {
            card.imageId?.let {
                val base64Image = RetrofitInstance.categoryApi.getCardImage(card.imageId).image
                val byteArray = Base64.decode(base64Image, Base64.DEFAULT)
                image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    .asImageBitmap()
            }
        }
    }

    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp, 8.dp)
        ) {
            Image(
                bitmap = if (card.imageId != null) image else placeholder,
                contentDescription = "Картинка карточки",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        if (image.height > maxImageHeight.value.toInt())
                            maxImageHeight
                        else
                            image.height.dp
                    )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = card.name)
            HorizontalDivider()
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Стоимость: ${mapPriceLevel(card.priceLevel)}")
                VerticalDivider()
                Text("Качество: ${mapQualityLevel(card.qualityLevel)}")
            }
        }
    }
}

@Composable
fun EditCategoryDialog(categoryId: String?, onConfirmation: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
//                if (categoryId != null)

            }
        }
    }
}

fun findCategoryById(categoryId: String, categories: List<CategoryResponse>): CategoryResponse? {
    categories.forEach {
        if (it.categoryId == categoryId) {
            return it
        } else if (it.subcategories.isNotEmpty()) {
            return findCategoryById(categoryId, it.subcategories)
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

