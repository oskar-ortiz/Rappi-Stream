$ErrorActionPreference = "Stop"

$container = if ($env:KAFKA_CONTAINER) { $env:KAFKA_CONTAINER } else { "rappi-stream-kafka-1" }
$topic = if ($env:KAFKA_TOPIC) { $env:KAFKA_TOPIC } else { "order_events" }
$prefix = if ($env:SMOKE_PREFIX) { $env:SMOKE_PREFIX } else { "smoke-order" }
$producer = "kafka-console-producer"
$arguments = @(
    "exec", "-i", $container, $producer,
    "--bootstrap-server", "localhost:29092",
    "--topic", $topic,
    "--property", "parse.key=true",
    "--property", "key.separator=:"
)

$process = [System.Diagnostics.Process]::new()
$process.StartInfo = [System.Diagnostics.ProcessStartInfo]::new()
$process.StartInfo.FileName = "docker"
$process.StartInfo.UseShellExecute = $false
$process.StartInfo.RedirectStandardInput = $true
$process.StartInfo.Arguments = $arguments -join " "

$baseTime = [DateTimeOffset]::UtcNow
$events = [System.Collections.Generic.List[string]]::new()
for ($index = 1; $index -le 3; $index++) {
    $orderId = "$prefix-$index"
    $driverId = "smoke-driver-$index"
    $sequence = @(
        @{ event_type = "DRIVER_ASSIGNED"; driver_id = $driverId },
        @{ event_type = "DRIVER_ARRIVING" },
        @{ event_type = "ORDER_DELIVERED" }
    )
    for ($step = 0; $step -lt $sequence.Count; $step++) {
        $event = @{
            event_type = $sequence[$step].event_type
            order_id = $orderId
            occurred_at = $baseTime.AddSeconds(($index * 3) + $step).ToString("yyyy-MM-ddTHH:mm:ssZ")
        }
        if ($sequence[$step].ContainsKey("driver_id")) {
            $event.driver_id = $sequence[$step].driver_id
        }
        $json = $event | ConvertTo-Json -Compress
        $events.Add("${orderId}:$json")
    }
}

if (-not $process.Start()) {
    throw "Could not start docker Kafka producer."
}
foreach ($eventLine in $events) {
    $process.StandardInput.WriteLine($eventLine)
}
$process.StandardInput.Close()
$process.WaitForExit()
if ($process.ExitCode -ne 0) {
    throw "Kafka producer exited with code $($process.ExitCode)."
}
Write-Output "Published DRIVER_ASSIGNED -> DRIVER_ARRIVING -> ORDER_DELIVERED for 3 smoke orders."
