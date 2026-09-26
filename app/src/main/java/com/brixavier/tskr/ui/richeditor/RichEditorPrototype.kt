package com.brixavier.tskr.ui.richeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brixavier.tskr.model.document.PlanDocument
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichEditorPrototype(
    document: PlanDocument,
    onDocumentChanged: (PlanDocument) -> Unit,
    modifier: Modifier = Modifier,
    sectionId: String = ""
) {
    val state = rememberRichTextState()
    
    val history = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var isUndoRedoAction by remember { mutableStateOf(false) }
    
    var loadedSectionId by remember { mutableStateOf<String?>(null) }
    
    // Synchronize RichTextState whenever sectionId changes or initial document loads
    LaunchedEffect(sectionId, document) {
        if (loadedSectionId != sectionId) {
            val html = RichEditorAdapter.documentToHtml(document)
            state.setHtml(html)
            history.clear()
            history.add(html)
            historyIndex = 0
            loadedSectionId = sectionId
        }
    }
    
    // Save to document on text change within the currently active section
    LaunchedEffect(state.annotatedString) {
        if (loadedSectionId == sectionId) {
            val html = state.toHtml()
            
            if (!isUndoRedoAction) {
                if (historyIndex >= 0 && historyIndex < history.size && history[historyIndex] != html) {
                    while (history.size > historyIndex + 1) {
                        history.removeAt(history.lastIndex)
                    }
                    history.add(html)
                    historyIndex = history.size - 1
                }
            }
            isUndoRedoAction = false
            
            val newDoc = RichEditorAdapter.htmlToDocument(html)
            onDocumentChanged(newDoc)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        RichEditorToolbar(
            state = state,
            canUndo = historyIndex > 0,
            canRedo = historyIndex < history.size - 1,
            onUndo = {
                if (historyIndex > 0) {
                    isUndoRedoAction = true
                    historyIndex--
                    state.setHtml(history[historyIndex])
                }
            },
            onRedo = {
                if (historyIndex < history.size - 1) {
                    isUndoRedoAction = true
                    historyIndex++
                    state.setHtml(history[historyIndex])
                }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        RichTextEditor(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        )
    }
}

@Composable
fun RichEditorToolbar(
    state: RichTextState,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bold
        IconButton(
            onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (state.currentSpanStyle.fontWeight == FontWeight.Bold) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
        ) {
            Icon(Icons.Default.FormatBold, contentDescription = "Bold")
        }
        
        // Italic
        IconButton(
            onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (state.currentSpanStyle.fontStyle == FontStyle.Italic) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
        ) {
            Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Bullet List
        IconButton(
            onClick = { state.toggleUnorderedList() },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (state.isUnorderedList) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
        ) {
            Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List")
        }
        
        // Numbered List
        IconButton(
            onClick = { state.toggleOrderedList() },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (state.isOrderedList) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
        ) {
            Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List")
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Link
        var showLinkDialog by remember { mutableStateOf(false) }
        IconButton(
            onClick = { showLinkDialog = true },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (state.isLink) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
        ) {
            Icon(Icons.Default.Link, contentDescription = "Link")
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Undo
        IconButton(
            onClick = onUndo,
            enabled = canUndo
        ) {
            Icon(Icons.Default.Undo, contentDescription = "Undo")
        }
        
        // Redo
        IconButton(
            onClick = onRedo,
            enabled = canRedo
        ) {
            Icon(Icons.Default.Redo, contentDescription = "Redo")
        }
        
        if (showLinkDialog) {
            var url by remember { mutableStateOf("https://") }
            AlertDialog(
                onDismissRequest = { showLinkDialog = false },
                title = { Text("Insert Link") },
                text = {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (url.isNotBlank()) {
                            val selectedText = state.annotatedString.substring(state.selection.min, state.selection.max)
                            val textToLink = if (selectedText.isEmpty()) url else selectedText
                            state.addLink(
                                text = textToLink,
                                url = url
                            )
                        }
                        showLinkDialog = false
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showLinkDialog = false 
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
