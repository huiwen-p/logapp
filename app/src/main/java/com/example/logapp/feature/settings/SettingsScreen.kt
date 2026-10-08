package com.example.logapp.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val dayBoundary by viewModel.dayBoundary.collectAsState()
    val isDriveSignedIn by viewModel.isDriveSignedIn.collectAsState()
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle result, update auth state
        viewModel.updateAuthState()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            
            Column(Modifier.selectableGroup()) {
                listOf("system" to "System Default", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (value == themeMode),
                                onClick = { viewModel.setThemeMode(value) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (value == themeMode),
                            onClick = null
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            Divider()

            Text("Day Boundary", style = MaterialTheme.typography.titleMedium)
            Text(
                "When does a new day start? (e.g. 00:00 or 04:00)",
                style = MaterialTheme.typography.bodySmall
            )
            
            OutlinedTextField(
                value = dayBoundary,
                onValueChange = { viewModel.setDayBoundary(it) },
                label = { Text("Time (HH:mm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            Text("Google Drive Backup", style = MaterialTheme.typography.titleMedium)
            
            if (isDriveSignedIn) {
                Text("Connected to Google Drive", color = MaterialTheme.colorScheme.primary)
                
                Button(
                    onClick = { viewModel.triggerBackupNow(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Backup to Drive Now")
                }
                
                var isAutoBackupEnabled by remember { mutableStateOf(false) } // This should ideally come from preferences
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Automatic Background Backup")
                    Switch(
                        checked = isAutoBackupEnabled,
                        onCheckedChange = { 
                            isAutoBackupEnabled = it
                            viewModel.setPeriodicBackupEnabled(context, it)
                        }
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.signOutDrive() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Out of Google Drive")
                }
            } else {
                Button(
                    onClick = { 
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                            .requestEmail()
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(client.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect to Google Drive")
                }
            }
        }
    }
}
