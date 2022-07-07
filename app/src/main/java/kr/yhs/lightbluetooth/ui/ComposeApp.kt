package kr.yhs.lightbluetooth.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerInput
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.widget.ConfirmationOverlay
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.yhs.lightbluetooth.MainActivity
import kr.yhs.lightbluetooth.R
import kr.yhs.lightbluetooth.ui.pages.ConnectionList
import kr.yhs.lightbluetooth.ui.pages.Message
import kr.yhs.lightbluetooth.ui.pages.Screen
import java.io.InputStream
import java.io.OutputStream
import java.util.*

@OptIn(
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class
)
@Composable
fun ComposeApp(activity: MainActivity) {
    var discoveredDevices: Set<BluetoothDevice> by remember {
        mutableStateOf(emptySet())
    }

    // var lightLevel by remember { mutableStateOf(4f) }
    var lightPower by remember { mutableStateOf(true) }
    val navigationController = rememberSwipeDismissableNavController()

    val bluetoothAdapter = activity.bluetoothAdapter
    var deviceSocket: BluetoothSocket? = null
    var deviceOutputStream: OutputStream? = null
    var deviceInputStream: InputStream? = null

    if (!bluetoothAdapter.isEnabled) {
        Message(
            title = activity.getString(R.string.bluetooth_adapter_topic),
            message = activity.getString(R.string.bluetooth_adapter_message)
        )
        return
    }
    SwipeDismissableNavHost(
        modifier = Modifier.fillMaxSize(),
        navController = navigationController,
        startDestination = Screen.ConnectionPage.route
    ) {
        composable(Screen.ConnectionPage.route) {
            Message(
                title = activity.getString(R.string.connection_topic),
                message = activity.getString(R.string.connection_message)
            ) {
                Button(onClick = {
                    navigationController.navigate(Screen.ConnectionList.route)
                }) {
                    Icon(Icons.Filled.Bluetooth, contentDescription = "Bluetooth Connect")
                }
            }
        }
        composable(Screen.ConnectionList.route) {
            val permissionList = mutableListOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionList.add(Manifest.permission.BLUETOOTH_CONNECT)
                permissionList.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            val permissionResult = rememberMultiplePermissionsState(
                permissionList
            )
            LaunchedEffect(true) {
                if (
                    ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.BLUETOOTH
                    ) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionResult.launchMultiplePermissionRequest()
                    if (!permissionResult.allPermissionsGranted) {
                        ConfirmationOverlay()
                            .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                            // .setMessage(activity.getText(androidx.compose.runtime.R.string.gps_permission))
                            .setMessage("필요한 권한을 모두 부여해주세요.")
                            .showOn(activity)
                        navigationController.popBackStack()
                        return@LaunchedEffect
                    }
                }
            }
            val pairedDevices = bluetoothAdapter.bondedDevices
            ConnectionList(pairedDevices, discoveredDevices, onSearch = {
                discoveredDevices = activity.scan()
            }, onConnect = {
                // HC-06 UUID
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

                deviceSocket = it.createRfcommSocketToServiceRecord(uuid)
                activity.bluetoothAdapter.cancelDiscovery()
                navigationController.navigate(Screen.ControlPage.route)
            })
        }
        composable(Screen.ControlPage.route) {
            LaunchedEffect(true) {
                try {
                    deviceSocket?.connect()

                    deviceOutputStream = deviceSocket?.outputStream
                    deviceInputStream = deviceSocket?.inputStream
                } catch (e: Exception) {
                    Toast.makeText(activity, "연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    navigationController.popBackStack()
                    return@LaunchedEffect
                }
                Toast.makeText(activity, "연결에 성공했습니다.", Toast.LENGTH_SHORT).show()
            }
            deviceOutputStream?.write(6)
            // For Debug?

            // deviceOutputStream?.write(lightLevel.toInt())
            // val sendValue = Thread {
            //     deviceOutputStream?.write(lightLevel.toInt())
            // }
            // InlineSlider(
            //     value = lightLevel,
            //     onValueChange = {
            //         lightLevel = it
            //         sendValue.run()
            //     },
            //     increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
            //     decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
            //     valueRange = 1f..6f,
            //     steps = 5,
            //     segmented = true
            // )
            Message(
                title = "무드등",
                message = "더 이상 할 수 있는것이 없다..?"
            ) {
                Button(onClick = {
                    lightPower = when (lightPower) {
                        true -> {
                            deviceOutputStream?.write(9)
                            Toast.makeText(activity, "불을 끕니다.", Toast.LENGTH_SHORT).show()
                            false
                        }
                        false -> {
                            deviceOutputStream?.write(8)
                            Toast.makeText(activity, "불을 킵니다.", Toast.LENGTH_SHORT).show()
                            true
                        }
                    }
                }, enabled = true) {
                    when(lightPower) {
                        false -> Icon(Icons.Filled.PowerOff, contentDescription = "Power Input")
                        true -> Icon (Icons.Filled.Power, contentDescription = "Power")
                    }
                }
            }
        }
    }
}