package com.suvojeet.notenext.ui.lock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.settings.SettingsRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(onPinSet: () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.pin_setup_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(id = R.string.create_pin_message), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.take(8) },
                label = { Text(stringResource(id = R.string.enter_pin)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { confirmPin = it.take(8) },
                label = { Text(stringResource(id = R.string.confirm_pin)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (pin.length < 4) {
                        error = context.getString(R.string.pin_too_short)
                        return@Button
                    }
                    if (pin != confirmPin) {
                        error = context.getString(R.string.pins_do_not_match)
                        return@Button
                    }
                    scope.launch {
                        settingsRepository.saveAppLockPin(pin)
                        settingsRepository.saveEnableAppLock(true)
                        onPinSet()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.save_pin))
            }
        }
    }
}
