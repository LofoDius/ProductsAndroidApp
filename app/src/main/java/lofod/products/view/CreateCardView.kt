package lofod.products.view

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import lofod.products.R
import lofod.products.api.RetrofitInstance
import lofod.products.api.model.PriceLevel
import lofod.products.api.model.QualityLevel
import lofod.products.api.request.CreateCardRequest
import lofod.products.api.response.CardResponse
import lofod.products.api.response.CategoryResponse
import lofod.products.handleApiError
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.InputStream

@Composable
@Preview
fun CreateCardPreview() {
    CreateCardView(
        category = CategoryResponse("", "", null, 0, 0, emptyList(), null),
        card = null,
        onConfirmation = {},
        onDismiss = {}
    )
}

@Composable
fun CreateCardView(
    category: CategoryResponse,
    card: CardResponse?,
    onConfirmation: (List<CardResponse>) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current.contentResolver
    val categoryApi = RetrofitInstance.categoryApi

    var newName by remember { mutableStateOf(card?.name ?: "") }
    var image by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { image = it }
    }
    var initImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var priceLevel by remember { mutableStateOf(card?.priceLevel ?: PriceLevel.LOW_PRICE) }
    var qualityLevel by remember { mutableStateOf(card?.qualityLevel ?: QualityLevel.LOW_QUALITY) }
    var description by remember { mutableStateOf(card?.description ?: "") }

    var isPriceDropdownExpanded by remember { mutableStateOf(false) }
    var isQualityDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect("apiCall") {
        card?.imageId?.let {
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
                Text(text = if (card != null) "Редактирование карточки" else "Создание карточки")
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Введите название") },
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Введите описание") },
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 0.dp)
                ) {
                    Row {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    isPriceDropdownExpanded = true
                                    isQualityDropdownExpanded = false
                                }
                        ) {
                            Text(text = priceLevel.text())
                            Icon(Icons.Default.KeyboardArrowDown, "Раскрыть список уровня цены")
                        }

                        DropdownMenu(
                            expanded = isPriceDropdownExpanded,
                            onDismissRequest = {
                                isPriceDropdownExpanded = false
                            }
                        ) {
                            PriceLevel.entries.forEach { price ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = price.text())
                                    },
                                    onClick = {
                                        isPriceDropdownExpanded = false
                                        priceLevel = price
                                    }
                                )
                            }
                        }
                    }

                    Row {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    isPriceDropdownExpanded = false
                                    isQualityDropdownExpanded = true
                                }
                        ) {
                            Text(text = qualityLevel.text())
                            Icon(Icons.Default.KeyboardArrowDown, "Раскрыть список уровня цены")
                        }

                        DropdownMenu(
                            expanded = isQualityDropdownExpanded,
                            onDismissRequest = {
                                isQualityDropdownExpanded = false
                            }
                        ) {
                            QualityLevel.entries.forEach { quality ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = quality.text())
                                    },
                                    onClick = {
                                        isQualityDropdownExpanded = false
                                        qualityLevel = quality
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                        .height(64.dp)
                        .clickable {
                            launcher.launch("image/*")
                        }
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 8.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                var imageId: String? = image?.let {
                                    val inputStream: InputStream? = context.openInputStream(it)
                                    inputStream?.let {
                                        val byteArray = it.readBytes()
                                        it.close()

                                        val body = MultipartBody.Part.createFormData(
                                            "image",
                                            "image.jpg",
                                            byteArray.toRequestBody("image/*".toMediaTypeOrNull())
                                        )
                                        try {
                                            val res = categoryApi.uploadImage(body)
                                            if (res.isSuccessful && res.body()?.imageId != null)
                                                return@let res.body()?.imageId
                                            else {
                                                handleApiError(HttpException(res))
                                                return@let null
                                            }
                                        } catch (e: Exception) {
                                            handleApiError(e)
                                            return@let null
                                        }
                                    }
                                }

                                coroutineScope.launch {
                                    val createCardRequest = CreateCardRequest(
                                        name = newName,
                                        imageId = imageId,
                                        priceLevel = priceLevel,
                                        qualityLevel = qualityLevel,
                                        description = description
                                    )
                                    if (card != null) {
                                        categoryApi.updateCard(category.categoryId, card.cardId, createCardRequest)
                                            .enqueue(object : Callback<List<CardResponse>> {
                                                override fun onResponse(
                                                    call: Call<List<CardResponse>?>,
                                                    response: Response<List<CardResponse>?>
                                                ) {
                                                    if (response.isSuccessful)
                                                        onConfirmation(response.body()!!)
                                                    else onDismiss()
                                                }

                                                override fun onFailure(
                                                    call: Call<List<CardResponse>?>,
                                                    t: Throwable
                                                ) {
                                                    t.printStackTrace()
                                                    onDismiss()
                                                }

                                            })
                                    } else {
                                        categoryApi.createCard(category.categoryId, createCardRequest)
                                            .enqueue(object : Callback<List<CardResponse>> {
                                                override fun onResponse(
                                                    call: Call<List<CardResponse>?>,
                                                    response: Response<List<CardResponse>?>
                                                ) {
                                                    if (response.isSuccessful)
                                                        onConfirmation(response.body()!!)
                                                    else onDismiss()
                                                }

                                                override fun onFailure(
                                                    call: Call<List<CardResponse>?>,
                                                    t: Throwable
                                                ) {
                                                    t.printStackTrace()
                                                    onDismiss()
                                                }

                                            })
                                    }
                                }
                            }
                        }
                    ) {
                        Text(text = "Сохранить")
                    }

                    Button(
                        onClick = {
                            onDismiss()
                        }
                    ) {
                        Text(text = "Отмена")
                    }
                }
            }
        }
    }
}
