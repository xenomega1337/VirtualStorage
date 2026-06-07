package com.example.virtualstorage

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.virtualstorage.data.Category
import com.example.virtualstorage.data.InventoryItem
import com.example.virtualstorage.data.InventoryState
import com.example.virtualstorage.data.InventoryTab
import com.example.virtualstorage.data.StockHistory
import com.example.virtualstorage.data.categoryPath
import com.example.virtualstorage.ui.theme.VirtualStorageTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: InventoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[InventoryViewModel::class.java]
        enableEdgeToEdge()
        setContent {
            VirtualStorageTheme {
                VirtualStorageApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualStorageApp(viewModel: InventoryViewModel) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Виртуальный склад", fontWeight = FontWeight.Bold)
                        Text(
                            "Учет вещей, остатков и докупки",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                InventoryTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Text(tabIcon(tab)) },
                        label = { Text(tab.title, maxLines = 1) }
                    )
                }
            }
        },
        floatingActionButton = {
            when {
                state.userEmail == null -> Unit
                state.selectedTab == InventoryTab.Items -> FloatingActionButton(onClick = { viewModel.startItemEdit() }) {
                    Text("+")
                }

                state.selectedTab == InventoryTab.Categories -> FloatingActionButton(onClick = { viewModel.startCategoryEdit() }) {
                    Text("+")
                }

                else -> Unit
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (state.selectedTab) {
                InventoryTab.Dashboard -> DashboardScreen(state, viewModel)
                InventoryTab.Items -> ItemsScreen(state, viewModel)
                InventoryTab.Categories -> CategoriesScreen(state, viewModel)
                InventoryTab.Restock -> RestockScreen(state, viewModel)
            }
        }
    }

    if (state.isItemDialogOpen) {
        ItemDialog(
            item = state.editingItem,
            categories = state.categories,
            onDismiss = viewModel::closeItemEdit,
            onSave = viewModel::saveItem
        )
    }
}

private fun tabIcon(tab: InventoryTab): String = when (tab) {
    InventoryTab.Dashboard -> "H"
    InventoryTab.Items -> "I"
    InventoryTab.Categories -> "C"
    InventoryTab.Restock -> "!"
}

@Composable
private fun DashboardScreen(state: InventoryState, viewModel: InventoryViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AuthCard(state, viewModel)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Товаров", state.items.size.toString(), Modifier.weight(1f))
                StatCard("Категорий", state.categories.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Нужно докупить", state.restockItems.size.toString(), Modifier.weight(1f))
                StatCard("Всего единиц", state.items.sumOf { it.quantity }.toString(), Modifier.weight(1f))
            }
        }
        item {
            SectionTitle("Последние изменения")
        }
        if (state.history.isEmpty()) {
            item { EmptyText("История появится после добавления или изменения товаров.") }
        } else {
            items(state.history) { history ->
                HistoryRow(history)
            }
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.selectTab(InventoryTab.Items) }
            ) {
                Text("Открыть товары")
            }
        }
    }
}

@Composable
private fun AuthCard(state: InventoryState, viewModel: InventoryViewModel) {
    var email by remember { mutableStateOf("test@mail.com") }
    var password by remember { mutableStateOf("123456") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Firebase Auth", fontWeight = FontWeight.Bold)
            if (state.userEmail != null) {
                Text("Вы вошли как ${state.userEmail}")
                OutlinedButton(onClick = viewModel::signOut) {
                    Text("Выйти")
                }
            } else {
                if (!state.firebaseConfigured) {
                    Text(
                        "Firebase не настроен. Добавьте app/google-services.json, чтобы вход и регистрация работали на эмуляторе.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.signIn(email, password) }) {
                        Text("Войти")
                    }
                    OutlinedButton(onClick = { viewModel.register(email, password) }) {
                        Text("Регистрация")
                    }
                }
            }
            if (state.authMessage.isNotBlank()) {
                Text(state.authMessage, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ItemsScreen(state: InventoryState, viewModel: InventoryViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::search,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Поиск по названию или описанию") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        CategoryFilter(state, viewModel)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.filteredItems.isEmpty()) {
                item {
                    EmptyText(
                        if (state.userEmail == null) {
                            "Войдите в аккаунт, чтобы увидеть товары этого склада."
                        } else {
                            "Пока нет товаров. Нажмите +, чтобы добавить первую вещь."
                        }
                    )
                }
            }
            items(state.filteredItems, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    categoryName = state.categories.categoryPath(item.categoryId),
                    onEdit = { viewModel.startItemEdit(item) },
                    onDelete = { viewModel.deleteItem(item.id) },
                    onMinus = { viewModel.changeQuantity(item, -1) },
                    onPlus = { viewModel.changeQuantity(item, 1) }
                )
            }
        }
    }
}

@Composable
private fun CategoriesScreen(state: InventoryState, viewModel: InventoryViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionTitle("Категории и подкатегории") }
        if (state.userEmail == null) {
            item { EmptyText("Войдите в аккаунт, чтобы увидеть категории этого склада.") }
        }
        items(state.categories, key = { it.id }) { category ->
            val count = state.items.count { it.categoryId == category.id }
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(category.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            state.categories.categoryPath(category.id),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("$count товаров", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = { viewModel.startCategoryEdit(category) }) {
                        Text("Изм.")
                    }
                    TextButton(onClick = { viewModel.deleteCategory(category.id) }) {
                        Text("Удал.")
                    }
                }
            }
        }
    }
    if (state.isCategoryDialogOpen) {
        CategoryDialog(
            category = state.editingCategory,
            categories = state.categories,
            onDismiss = viewModel::closeCategoryEdit,
            onSave = viewModel::saveCategory
        )
    }
}

@Composable
private fun RestockScreen(state: InventoryState, viewModel: InventoryViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionTitle("Автоматический список докупки") }
        if (state.restockItems.isEmpty()) {
            item {
                EmptyText(
                    if (state.userEmail == null) {
                        "Войдите в аккаунт, чтобы увидеть список докупки."
                    } else {
                        "Все остатки в норме."
                    }
                )
            }
        }
        items(state.restockItems, key = { it.id }) { item ->
            ItemCard(
                item = item,
                categoryName = state.categories.categoryPath(item.categoryId),
                onEdit = { viewModel.startItemEdit(item) },
                onDelete = { viewModel.deleteItem(item.id) },
                onMinus = { viewModel.changeQuantity(item, -1) },
                onPlus = { viewModel.changeQuantity(item, 1) }
            )
        }
        item {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.selectTab(InventoryTab.Items) }
            ) {
                Text("Вернуться к товарам")
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CategoryFilter(state: InventoryState, viewModel: InventoryViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = state.selectedCategoryId == null,
            onClick = { viewModel.filterByCategory(null) },
            label = { Text("Все") }
        )
        state.categories.forEach { category ->
            FilterChip(
                selected = state.selectedCategoryId == category.id,
                onClick = { viewModel.filterByCategory(category.id) },
                label = { Text(category.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

@Composable
private fun ItemCard(
    item: InventoryItem,
    categoryName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemPhoto(item.imageUri, Modifier.size(82.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    if (item.needsRestock) {
                        AssistChip(onClick = {}, label = { Text("Докупить") })
                    }
                }
                Text(categoryName, style = MaterialTheme.typography.bodySmall)
                if (item.description.isNotBlank()) {
                    Text(
                        item.description,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text("Остаток: ${item.quantity} / минимум: ${item.minQuantity}")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onMinus) { Text("-1") }
                    OutlinedButton(onClick = onPlus) { Text("+1") }
                    TextButton(onClick = onEdit) { Text("Изм.") }
                    TextButton(onClick = onDelete) { Text("Удал.") }
                }
            }
        }
    }
}

@Composable
private fun ItemPhoto(uri: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState(initialValue = null as androidx.compose.ui.graphics.ImageBitmap?, uri) {
        value = uri?.let {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(it)).use { stream ->
                    BitmapFactory.decodeStream(stream).asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap == null) {
            Text("Фото", style = MaterialTheme.typography.labelMedium)
        } else {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun ItemDialog(
    item: InventoryItem?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Int, Long?, String?, Long?) -> Unit
) {
    val context = LocalContext.current
    var name by remember(item) { mutableStateOf(item?.name ?: "") }
    var description by remember(item) { mutableStateOf(item?.description ?: "") }
    var quantity by remember(item) { mutableStateOf((item?.quantity ?: 1).toString()) }
    var minQuantity by remember(item) { mutableStateOf((item?.minQuantity ?: 1).toString()) }
    var categoryId by remember(item) { mutableStateOf(item?.categoryId) }
    var imageUri by remember(item) { mutableStateOf(item?.imageUri) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            imageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Добавить товар" else "Редактировать товар") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ItemPhoto(imageUri, Modifier.fillMaxWidth().aspectRatio(2.5f))
                OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }) {
                    Text("Выбрать фото")
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter(Char::isDigit) },
                        label = { Text("Кол-во") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minQuantity,
                        onValueChange = { minQuantity = it.filter(Char::isDigit) },
                        label = { Text("Мин.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Категория", fontWeight = FontWeight.SemiBold)
                CategoryRadioRow(
                    title = "Без категории",
                    selected = categoryId == null,
                    onClick = { categoryId = null }
                )
                categories.forEach { category ->
                    CategoryRadioRow(
                        title = categories.categoryPath(category.id),
                        selected = categoryId == category.id,
                        onClick = { categoryId = category.id }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        description,
                        quantity.toIntOrNull() ?: 0,
                        minQuantity.toIntOrNull() ?: 0,
                        categoryId,
                        imageUri,
                        item?.id
                    )
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun CategoryDialog(
    category: Category?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (String, Long?, Long?) -> Unit
) {
    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var parentId by remember(category) { mutableStateOf(category?.parentId) }
    val availableParents = categories.filter { it.id != category?.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Добавить категорию" else "Редактировать категорию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Родительская категория", fontWeight = FontWeight.SemiBold)
                CategoryRadioRow("Нет, верхний уровень", parentId == null) { parentId = null }
                availableParents.forEach { parent ->
                    CategoryRadioRow(categories.categoryPath(parent.id), parentId == parent.id) {
                        parentId = parent.id
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, parentId, category?.id) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun CategoryRadioRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HistoryRow(history: StockHistory) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(history.action, fontWeight = FontWeight.SemiBold)
            Text(history.details, style = MaterialTheme.typography.bodySmall)
            Text(
                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(history.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
