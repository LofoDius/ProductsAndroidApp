package lofod.products.view

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lofod.products.R
import lofod.products.api.RetrofitInstance
import lofod.products.api.request.CreateCategoryRequest
import lofod.products.api.response.CategoryResponse
import lofod.products.findCategoryById
import lofod.products.handleApiError
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import kotlin.collections.forEach

@Composable
fun CreateCategoryView(
    category: CategoryResponse?,
    onConfirmation: (CategoryResponse) -> Unit,
    onDismiss: () -> Unit
) {
    var isTreeExpanded by remember { mutableStateOf(false) }
    var parentId by remember { mutableStateOf<String?>(category?.parentId) }
    var categories by remember { mutableStateOf(emptyList<CategoryResponse>()) }
    var parentName by remember { mutableStateOf("Изменить родительскую категорию") }
    var newName by remember { mutableStateOf(category?.name ?: "") }
    var image by remember { mutableStateOf<Uri?>(null) }
    var initImage by remember { mutableStateOf<ImageBitmap?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            image = it
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val categoryApi = RetrofitInstance.categoryApi

    val scrollState = rememberScrollState()
    val contentResolver = LocalContext.current.contentResolver
    val context = LocalContext.current

    fun showException(e: Exception) {
        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
    }

    LaunchedEffect("apiCall") {
        categories = categoryApi.getCategories()
        category?.parentId?.let {
            parentId = it
            parentName = findCategoryById(it, categories)!!.name
        }

        category?.imageId?.let {
            val base64Image = RetrofitInstance.categoryApi.getCardImage(it).image
            val byteArray = Base64.decode(base64Image, Base64.DEFAULT)
            initImage = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                .asImageBitmap()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth(),
            onClick = { isTreeExpanded = false }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(
                        state = scrollState,
                        enabled = true
                    )
            ) {
                Text(text = if (category != null) "Редактирование категории" else "Добавление категории")
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Text(text = "Название:")
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Родительская категория:")
                TreeView(
                    isTreeExpanded,
                    parentName,
                    categories,
                    onExpand = {
                        isTreeExpanded = true
                    },
                    onChoose = {
                        parentId = it.categoryId
                        parentName = it.name
                        isTreeExpanded = false
                    }
                )

                image?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = "Картинка категории",
                        modifier = Modifier
                            .height(48.dp)
                            .clickable {
                                launcher.launch("image/*")
                            }
                    )
                } ?: Image(
                    bitmap = initImage ?: ImageBitmap.imageResource(R.drawable.placeholder),
                    contentDescription = "Картинка категории",
                    modifier = Modifier
                        .height(48.dp)
                        .clickable {
                            launcher.launch("image/*")
                        }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = {
                        coroutineScope.launch {
                            var multipart = image?.let {
                                val inputStream: InputStream? = contentResolver.openInputStream(it)
                                inputStream?.let {
                                    val byteArray = it.readBytes()
                                    it.close()

                                    val body = MultipartBody.Part.createFormData(
                                        "image",
                                        "image.jpg",
                                        byteArray.toRequestBody("image/*".toMediaTypeOrNull())
                                    )
                                    return@let body
                                }
                            }

                            val createCategoryRequest = CreateCategoryRequest(
                                parentId = parentId,
                                name = newName,
                                imageId = category?.imageId
                            )
                            val response = withContext(Dispatchers.IO) {
                                if (category != null)
                                    categoryApi.updateCategory(category.categoryId, createCategoryRequest)
                                        .execute()
                                else categoryApi.createCategory(createCategoryRequest)
                            }
                            if (response.isSuccessful) {
                                onConfirmation(response.body()!!)
                                multipart?.let {
                                    try {
                                        val res = categoryApi.uploadImage(multipart)
                                        if (res.isSuccessful && res.body()?.imageId != null) {
                                            createCategoryRequest.imageId = res.body()?.imageId
                                            categoryApi.updateCategory(category!!.categoryId, createCategoryRequest)
                                                .execute()
                                        } else {
                                            handleApiError(Exception(res.raw().body?.string()))
                                            showException(Exception(res.raw().body?.string()))
                                        }
                                    } catch (e: Exception) {
                                        handleApiError(e)
                                        showException(e)
                                    }
                                }
                            } else {
                                showException(Exception(response.raw().body?.string()))
                                onDismiss()
                            }
                        }
                    }) {
                        Text(text = "Сохранить")
                    }

                    Button(onClick = { onDismiss() }) {
                        Text(text = "Отмена")
                    }
                }
            }
        }
    }
}

@Composable
fun TreeView(
    expanded: Boolean,
    currentParentName: String,
    categories: List<CategoryResponse>,
    onExpand: () -> Unit,
    onChoose: (CategoryResponse) -> Unit,
    padding: Int = 0
) {
    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 300))
        },
        label = "category tree",
        modifier = Modifier.fillMaxHeight()
    ) { targetState ->
        if (targetState) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                categories.forEach {
                    TextButton(onClick = { onChoose(it) }) {
                        Text(
                            text = it.name,
                            modifier = Modifier.padding(start = (4 + padding).dp, bottom = 4.dp, top = 4.dp, end = 4.dp)
                        )
                    }

                    if (it.subcategories.isNotEmpty()) {
                        TreeView(
                            expanded = true,
                            currentParentName = it.name,
                            categories = it.subcategories,
                            onExpand = onExpand,
                            onChoose = onChoose,
                            padding = padding + 4
                        )
                    }
                }
            }
        } else {
            TextButton(onClick = { onExpand() }) {
                Text(text = currentParentName)
            }
        }
    }
}