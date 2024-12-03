package lofod.products

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
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
import lofod.products.api.request.CreateSessionRequest
import lofod.products.api.response.CardResponse
import lofod.products.api.response.CategoryResponse
import lofod.products.ui.theme.ProductsTheme
import lofod.products.view.CreateCardView
import lofod.products.view.CreateCategoryView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

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
                    category = CategoryResponse(
                        name = "Все категории",
                        categoryId = "-1",
                        parentId = null,
                        subcategoriesAmount = 0,
                        cardsAmount = 0,
                        subcategories = categoryApi.getCategories(),
                        imageId = null
                    )

                    RetrofitInstance.authApi.createSession(CreateSessionRequest("1231"))
                        .enqueue(object : Callback<Void> {
                            override fun onResponse(
                                call: Call<Void?>,
                                response: Response<Void?>
                            ) {
                                if (response.code() == 201)
                                    Storage.token = response.headers()["Authorization"]
                            }

                            override fun onFailure(call: Call<Void?>, t: Throwable) {

                            }

                        })
                }
            }

            if (category != null)
                MainScreen(category!!)
            else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(category: CategoryResponse) {
    var isLoading by remember { mutableStateOf(false) }
    var cards by remember { mutableStateOf(emptyList<CardResponse>()) }
    var category by remember { mutableStateOf<CategoryResponse?>(category) }
    var isShowDeleteCategoryDialog by remember { mutableStateOf(false) }

    var searchCards by remember { mutableStateOf(emptyList<CardResponse>()) }
    var isSearching by remember { mutableStateOf(false) }

    var isEditCategoryMode by remember { mutableStateOf(false) }
    var editCategory: CategoryResponse? = null

    var isEditCardMode by remember { mutableStateOf(false) }
    var editCard: CardResponse? = null

    var isShowDeleteCardDialog by remember { mutableStateOf(false) }
    var cardToDelete: CardResponse? = null

    val coroutineScope = rememberCoroutineScope()
    val categoryApi = RetrofitInstance.categoryApi

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val drawerWidth = LocalConfiguration.current.screenWidthDp * 0.8

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
            val categories = categoryApi.getCategories()
            if (categoryId == null) {
                category = CategoryResponse(
                    name = "Все категории",
                    categoryId = "-1",
                    parentId = null,
                    subcategoriesAmount = 0,
                    cardsAmount = 0,
                    subcategories = categories,
                    imageId = null
                )
            } else {
                category = findCategoryById(categoryId, categories)
                getCards(categoryId)
            }
        }
    }

    ProductsTheme {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .fillMaxHeight()
                        .requiredWidth(drawerWidth.dp)
                ) {
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
                                isLoading = true
                                getCategory(category?.parentId)
                                isLoading = false
                            }
                        )
                    }
                    if (category?.subcategories?.isNotEmpty() == true) {
                        category!!.subcategories.forEach {
                            NavigationDrawerItem(
                                label = {
                                    CategoryView(it, onCategoryEdit = {
                                        isEditCategoryMode = true
                                        editCategory = it
                                    })
                                },
                                selected = false,
                                onClick = {
                                    isLoading = true
                                    getCategory(it.categoryId)
                                    isLoading = false
                                }
                            )
                        }
                    } else {
                        Text("У этой категории нет подкатегорий")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    isEditCategoryMode = true
                                    editCategory =
                                        CategoryResponse("", "-1", category?.categoryId, 0, 0, emptyList(), null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp, 4.dp)
                        ) {
                            Text("Добавить категорию")
                        }

                        if (category?.categoryId != "-1") {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        drawerState.close()
                                        isShowDeleteCategoryDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp, 4.dp)
                            ) {
                                Text("Удалить эту категорию")
                            }
                        }
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
                            coroutineScope.launch {
                                categoryApi.search(searchText).enqueue(object : Callback<List<CardResponse>> {
                                    override fun onResponse(
                                        call: Call<List<CardResponse>?>,
                                        response: Response<List<CardResponse>?>
                                    ) {
                                        if (response.isSuccessful) {
                                            searchCards = response.body()!!
                                        } else {
                                            handleApiError(HttpException(response))
                                        }
                                    }

                                    override fun onFailure(
                                        call: Call<List<CardResponse>?>,
                                        t: Throwable
                                    ) {

                                    }
                                })
                            }
                        },
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = {
                        isEditCardMode = true
                        editCard = null
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Добавить категорию")
                    }
                }
            ) { innerPadding ->
                if (isSearching) {
                    if (searchCards.isEmpty())
                        Text(text = "Ничего не найдено")
                    else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(searchCards) { card ->
                                CardView(
                                    card = card,
                                    onCardEdit = {
                                        editCard = it
                                        isEditCardMode = true
                                    },
                                    onCardDelete = {
                                        cardToDelete = it
                                        isShowDeleteCardDialog = true
                                    }
                                )
                            }
                        }
                    }
                } else if (cards.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(cards) { card ->
                            CardView(
                                card = card,
                                onCardEdit = {
                                    editCard = it
                                    isEditCardMode = true
                                },
                                onCardDelete = {
                                    cardToDelete = it
                                    isShowDeleteCardDialog = true
                                }
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Text(
                            text = if (category?.categoryId == "-1") "Выберите категорию в боковом меню"
                            else "Карточек нет",
                        )
                    }
                }

                // Диалог редактирования категории
                if (isEditCategoryMode) {
                    CreateCategoryView(
                        category = editCategory,
                        onConfirmation = {
                            coroutineScope.launch {
                                val categories = categoryApi.getCategories()
                                if (category?.categoryId == "-1") {
                                    category = CategoryResponse(
                                        name = "Все категории",
                                        categoryId = "-1",
                                        parentId = null,
                                        subcategoriesAmount = 0,
                                        cardsAmount = 0,
                                        subcategories = categories,
                                        imageId = null
                                    )
                                } else {
                                    category = findCategoryById(category!!.categoryId, categories)
                                    getCards(category!!.categoryId)
                                }
                                isEditCategoryMode = false
                            }
                        },
                        onDismiss = {
                            isEditCategoryMode = false
                        }
                    )
                }

                // Диалог редактирования карточки
                if (isEditCardMode) {
                    CreateCardView(
                        category = category!!,
                        editCard,
                        onConfirmation = {
                            cards = it
                            isEditCardMode = false
                        },
                        onDismiss = {
                            isEditCardMode = false
                        }
                    )
                }

                // Диалог подтверждения удаления карточки
                if (isShowDeleteCardDialog) {
                    Dialog(onDismissRequest = { isShowDeleteCardDialog = false }) {
                        Card(
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(8.dp)
                            ) {
                                Text(
                                    "Удалить карточку?",
                                    fontSize = TextUnit(24f, TextUnitType.Sp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                categoryApi.deleteCard(cardToDelete!!.categoryId, cardToDelete!!.cardId)
                                                    .enqueue(object : Callback<Void> {
                                                        override fun onResponse(
                                                            call: Call<Void?>,
                                                            response: Response<Void?>
                                                        ) {
                                                            if (response.isSuccessful) {
                                                                cards = cards.filter { it != cardToDelete }
                                                                searchCards = searchCards.filter { it != cardToDelete }
                                                            } else {
                                                                handleApiError(HttpException(response))
                                                                getCards(category!!.categoryId)
                                                            }
                                                            isShowDeleteCardDialog = false
                                                        }

                                                        override fun onFailure(
                                                            call: Call<Void?>,
                                                            t: Throwable
                                                        ) {
                                                            isShowDeleteCardDialog = false
                                                        }
                                                    })

                                            }
                                        }
                                    ) {
                                        Text(text = "Да")
                                    }

                                    TextButton(
                                        onClick = {
                                            isShowDeleteCardDialog = false
                                        }
                                    ) {
                                        Text(text = "Нет")
                                    }
                                }
                            }
                        }
                    }
                }

                // Диалог подтверждения удаления категории
                if (isShowDeleteCategoryDialog) {
                    Dialog(onDismissRequest = { isShowDeleteCategoryDialog = false }) {
                        Card(
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(8.dp)
                            ) {
                                Text(
                                    "Удалить эту категорию?",
                                    fontSize = TextUnit(24f, TextUnitType.Sp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                categoryApi.deleteCategory(cardToDelete!!.categoryId)
                                                    .enqueue(object : Callback<String> {
                                                        override fun onResponse(
                                                            call: Call<String?>,
                                                            response: Response<String?>
                                                        ) {
                                                            if (response.isSuccessful)
                                                                getCategory(category!!.parentId)
                                                            else
                                                                handleApiError(HttpException(response))
                                                            isShowDeleteCategoryDialog = false
                                                        }

                                                        override fun onFailure(
                                                            call: Call<String?>,
                                                            t: Throwable
                                                        ) {
                                                            isShowDeleteCategoryDialog = false
                                                        }
                                                    })

                                            }
                                        }
                                    ) {
                                        Text(text = "Да")
                                    }

                                    TextButton(
                                        onClick = {
                                            isShowDeleteCategoryDialog = false
                                        }
                                    ) {
                                        Text(text = "Нет")
                                    }
                                }
                            }
                        }
                    }
                }
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
fun CategoryView(category: CategoryResponse, onCategoryEdit: (CategoryResponse) -> Unit) {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect("apiCall") {
        if (category.imageId != null) {
            coroutineScope.launch {
                val base64Image = RetrofitInstance.categoryApi.getCardImage(category.imageId).image
                val byteArray = Base64.decode(base64Image, Base64.DEFAULT)
                image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    .asImageBitmap()
                    .asAndroidBitmap()
                    .let {
                        Bitmap.createScaledBitmap(it, 24, 24, false).asImageBitmap()
                    }
            }
        }
    }

    ProductsTheme {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (image != null) {
                Image(
                    bitmap = image!!,
                    contentDescription = "Картинка категории",
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 4.dp)
                )
            }

            Text(text = category.name)
            IconButton(onClick = { onCategoryEdit(category) }) {
                Icon(Icons.Filled.Create, contentDescription = "Редактировать")
            }
        }
    }
}

@Composable
fun CardView(card: CardResponse, onCardEdit: (CardResponse) -> Unit, onCardDelete: (CardResponse) -> Unit) {
    val maxImageHeight = 160.dp
    val placeholder = ImageBitmap.imageResource(R.drawable.placeholder)
    var image by remember { mutableStateOf<ImageBitmap>(placeholder) }
    var expanded by remember { mutableStateOf(false) }

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
            .animateContentSize()
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { expanded = !expanded }
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

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = card.description ?: "Описания нет",
                    color = if (card.description == null) Color.Gray else Color.Unspecified
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Стоимость: ${card.priceLevel.text()}")
                VerticalDivider()
                Text("Качество: ${card.qualityLevel.text()}")
            }

            if (expanded) {
                HorizontalDivider()
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 4.dp)
                ) {
                    TextButton(
                        onClick = {
                            onCardDelete(card)
                        }
                    ) {
                        Text(text = "Удалить")
                    }

                    TextButton(
                        onClick = {
                            onCardEdit(card)
                        }
                    ) {
                        Text(text = "Редактировать")
                    }
                }
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

fun handleApiError(e: Exception) {
    when (e) {
        is HttpException -> {
            println("HTTP ошибка: ${e.code()}, сообщение: ${e.message()}")
        }

        is IOException -> {
            println("Сетевая ошибка: ${e.message}")
        }

        else -> {
            println("Неизвестная ошибка: ${e.message}")
        }
    }
}
