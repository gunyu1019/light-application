package kr.yhs.lightbluetooth.ui.pages

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.animation.defaultDecayAnimationSpec
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import kotlinx.coroutines.launch


@SuppressLint("MissingPermission")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConnectionList(
    pairedDevices: Set<BluetoothDevice>?,
    discoveredDevices: Set<BluetoothDevice>,
    onSearch: () -> Unit,
    onConnect: (BluetoothDevice) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val pairedDevicesAddress = mutableListOf<String>()
    if (pairedDevices != null)
        for (device in pairedDevices) {
            pairedDevicesAddress.add(device.address)
        }
    val scalingLazyListState: ScalingLazyListState = rememberScalingLazyListState()
    Scaffold(
        positionIndicator = {
            PositionIndicator(
                scalingLazyListState = scalingLazyListState
            )
        }
    ) {
        ScalingLazyColumn(
            state = scalingLazyListState,
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent {
                    coroutineScope.launch {
                        scalingLazyListState.animateScrollBy(it.horizontalScrollPixels)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Text(
                    text = "연결 가능한 블루투스 목록",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 32.dp, bottom = 32.dp)
                        .fillMaxWidth()
                )
            }
            if (discoveredDevices.isNotEmpty()) {
                items(discoveredDevices.toList()) { discoveredDevice ->
                    DetectDevices(
                        deviceName = discoveredDevice.name,
                        deviceAddress = discoveredDevice.address,
                        pairedDevices = discoveredDevice.address in pairedDevicesAddress
                    ) {
                        Log.i("Bluetooth-Device", discoveredDevice.address)
                        onConnect(discoveredDevice)
                    }
                }
            } else {
                item {
                    DeviceEmpty()
                }
            }
            item {
                DeviceUpdateButton(onClick = onSearch)
            }
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}


@Composable
fun DeviceEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(.4f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.SearchOff,
            contentDescription = "Not Found",
            modifier = Modifier
                .height(30.dp)
                .height(30.dp)
        )
        Text(
            text = "Device Not Found",
            fontSize = 14.sp
        )
    }
}


@Composable
fun DetectDevices(
    deviceName: String?, deviceAddress: String, pairedDevices: Boolean, onClick: () -> Unit
) {
    return Chip(
        modifier = Modifier
            .height(54.dp)
            .padding(top = 2.dp, bottom = 2.dp)
            .fillMaxWidth(),
        colors = ChipDefaults.chipColors(
            backgroundColor = Color.White,
            contentColor = Color.Black
        ),
        label = {
            Text(
                text = deviceName?: "Unknown Device",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                text = deviceAddress,
                maxLines = 1
            )
            if (pairedDevices) {
                Text(
                    text = "(paired)",
                    maxLines = 1,
                    modifier = Modifier.padding(start = 3.dp)
                )
            }
        },
        onClick = onClick
    )
}

@Composable
fun DeviceUpdateButton (onClick: () -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
        Button(
            modifier = Modifier.size(
                width = ButtonDefaults.LargeButtonSize,
                height = ButtonDefaults.ExtraSmallButtonSize
            ),
            onClick = onClick
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "refresh",
                modifier = Modifier.size(16.dp),
            )
        }
    }
    return
}