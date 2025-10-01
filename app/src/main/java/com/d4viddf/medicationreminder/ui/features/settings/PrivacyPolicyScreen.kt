package com.d4viddf.medicationreminder.ui.features.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.privacy_policy_title)) },
                navigationIcon = {
                    FilledTonalIconButton (onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )

                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                PrivacyPolicyText()
            }
        }
    }
}

@Composable
fun PrivacyPolicyText() {
    val context = LocalContext.current
    val annotatedString = buildAnnotatedString {
        append(stringResource(id = R.string.privacy_policy_last_updated))
        append("\n\n")
        append(stringResource(id = R.string.privacy_policy_welcome))
        append("\n\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(id = R.string.privacy_policy_section_1_title))
            append("\n\n")
        }
        append(stringResource(id = R.string.privacy_policy_section_1_intro))
        append("\n\n")
        append(stringResource(id = R.string.privacy_policy_section_1_direct))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_1_direct_item_1))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_1_direct_item_2))
        append("\n\n")
        append(stringResource(id = R.string.privacy_policy_section_1_auto))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_1_auto_item_1))
        append("\n\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(id = R.string.privacy_policy_section_2_title))
            append("\n\n")
        }
        append(stringResource(id = R.string.privacy_policy_section_2_intro))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_2_item_1))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_2_item_2))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_2_item_3))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_2_item_4))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_2_item_5))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_2_item_6))
        append("\n\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(id = R.string.privacy_policy_section_3_title))
            append("\n\n")
        }
        append(stringResource(id = R.string.privacy_policy_section_3_intro))
        append("\n\n")
        append(stringResource(id = R.string.privacy_policy_section_3_data_collected_title))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_3_data_collected_item_1))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_3_data_collected_item_2))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_3_data_collected_item_3))
        append("\n\n")
        append(stringResource(id = R.string.privacy_policy_section_3_collection_method_title))
        append("\n")
        append(stringResource(id = R.string.privacy_policy_section_3_collection_method_item_1))
        append("\n\n")
        append(stringResource(id = R.string.privacy_policy_section_3_purpose_title))
        append("\n")
        append(stringResource(id = R.string.privacy_policy_section_3_purpose_item_1))
        append("\n\n")
        append(stringResource(id = R.string.privacy_policy_section_3_storage_title))
        append("\n")
        append(stringResource(id = R.string.privacy_policy_section_3_storage_item_1))
        append("\n\n")
        append(stringResource(id = R.string.privacy_policy_section_3_access_title))
        append("\n")
        append(stringResource(id = R.string.privacy_policy_section_3_access_item_1))
        append("\n\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(id = R.string.privacy_policy_section_4_title))
            append("\n\n")
        }
        append(stringResource(id = R.string.privacy_policy_section_4_intro))
        append("\n\n")
        append(stringResource(id = R.string.privacy_policy_section_4_item_1))
        append("\n\n")
        append(stringResource(id = R.string.privacy_policy_section_4_item_2))
        append("\n\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(id = R.string.privacy_policy_section_5_title))
            append("\n\n")
        }
        append(stringResource(id = R.string.privacy_policy_section_5_intro))
        append("\n\n")
        append(stringResource(id = R.string.privacy_policy_section_5_item_1))
        append("\n\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(id = R.string.privacy_policy_section_6_title))
            append("\n\n")
        }
        append(stringResource(id = R.string.privacy_policy_section_6_intro))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_6_item_1))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_6_item_2))
        append("\n")
        append(" - ")
        append(stringResource(id = R.string.privacy_policy_section_6_item_3))
        append("\n")
        append(stringResource(id = R.string.privacy_policy_section_6_item_4))
        append("\n\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(id = R.string.privacy_policy_section_7_title))
            append("\n\n")
        }
        append(stringResource(id = R.string.privacy_policy_section_7_intro))
        append("\n\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(id = R.string.privacy_policy_section_8_title))
            append("\n\n")
        }
        append(stringResource(id = R.string.privacy_policy_section_8_intro))
        append("\n\n")
        append(stringResource(id = R.string.privacy_policy_section_8_item_1))
        append("\n\n")
    }
    Column {
        Text(text = annotatedString)
        Spacer(modifier = Modifier.height(16.dp))
        with(buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(stringResource(id = R.string.privacy_policy_section_9_title))
                append("\n\n")
            }
            append(stringResource(id = R.string.privacy_policy_section_9_intro))
        }) {
            Text(text = this)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { openEmail(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.privacy_policy_contact_email_button))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { openWebsite(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.privacy_policy_contact_website_button))
        }
    }
}

private fun openEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:d4viddf@d4viddf.com".toUri()
    }
    context.startActivity(intent)
}

private fun openWebsite(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = "https://d4viddf.github.io/privacy/".toUri()
    }
    context.startActivity(intent)
}

@Preview(showBackground = true)
@Composable
fun PrivacyPolicyScreenPreview() {
    AppTheme {
        PrivacyPolicyScreen(onBack = {})
    }
}
