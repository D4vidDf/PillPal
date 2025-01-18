package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.d4viddf.medicationreminder.data.MedicationType
import com.d4viddf.medicationreminder.viewmodel.MedicationTypeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationTypeSelector(
    selectedTypeId: Int,
    onTypeSelected: (Int) -> Unit,
    viewModel: MedicationTypeViewModel = hiltViewModel()
) {
    val medicationTypes by viewModel.medicationTypes.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    if (medicationTypes.isEmpty()) {
        // Display loading indicator
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading medication types...",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
    } else {
        // Use a Material3 Carousel
        val lazyListState = rememberLazyListState()

        // Center-align the first item when the carousel is first displayed
        LaunchedEffect(Unit) {
            if (medicationTypes.isNotEmpty()) {
                lazyListState.scrollToItem(0)
            }
        }

         LazyRow(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .padding(horizontal = 0.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(medicationTypes) { type ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (type.id == selectedTypeId) Color(
                            0xFFF0BF70
                        ) else Color(0xFF264443)
                    ),
                    onClick = { onTypeSelected(type.id) }
                ) {
                    MedicationTypeItem(type = type)
                }
            }
        }

        /*val carouselState = rememberCarouselState { medicationTypes.count() }

        HorizontalUncontainedCarousel(
            state = carouselState,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .padding(horizontal = 0.dp, vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 0.dp),
            itemWidth = 230.dp,
            itemSpacing = 8.dp
        ) {
            index->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (medicationTypes.get(index = index).id == selectedTypeId) Color(
                            0xFFF0BF70
                        ) else Color(0xFF264443)
                    ),
                    onClick = {
                        onTypeSelected(medicationTypes[index].id)
                    }
                ) {
                    MedicationTypeItem(type = medicationTypes[index])
                }
            }
        }*/

        /*val coroutineScope = rememberCoroutineScope()
        val pagerState = rememberPagerState()

        HorizontalPager(
            count = medicationTypes.count(),
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .padding(horizontal = 0.dp, vertical = 8.dp),
            itemSpacing = 8.dp, // Adjust spacing as needed
        ) { index ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (medicationTypes[index].id == selectedTypeId) Color(
                        0xFFF0BF70
                    ) else Color(0xFF264443)
                ),
                onClick = { onTypeSelected(medicationTypes[index].id) }
            ) {
                MedicationTypeItem(type = medicationTypes[index])
            }
        }

        // Center-align the first item when the carousel is first displayed
        LaunchedEffect(Unit) {
            if (medicationTypes.isNotEmpty()) {
                pagerState.scrollToPage(0)
            }
        }*/
    }
}


@Composable
fun MedicationTypeItem(
    type: MedicationType
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 0.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(50.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = type.imageUrl),
                contentDescription = type.name,
                modifier = Modifier.size(70.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = type.name,
            fontSize = 18.sp,
            color = Color.White
        )
    }
}