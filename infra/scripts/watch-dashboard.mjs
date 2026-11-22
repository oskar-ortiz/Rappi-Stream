const url = process.argv[2] ?? "ws://localhost:8080/ws/dashboard";
const socket = new WebSocket(url);

socket.addEventListener("open", () => console.log(`Connected to ${url}`));
socket.addEventListener("message", event => console.log(event.data));
socket.addEventListener("error", error => {
  console.error("WebSocket connection failed", error);
  process.exitCode = 1;
});
socket.addEventListener("close", () => process.exit());

setTimeout(() => {
  if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
    socket.close(1000, "Smoke test complete");
  }
}, 20000);
